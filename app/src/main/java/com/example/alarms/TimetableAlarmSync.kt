package com.example.alarms

import android.content.Context
import com.example.data.AttendanceRegisterService
import com.example.data.ClassesService
import com.example.data.OfflineCache
import com.example.data.RegisteredCourseItem
import com.example.data.ScheduleItem
import com.example.data.SupabaseConfig
import com.example.data.TimetableService
import com.example.data.UserSession
import com.example.util.JsonHelper
import com.example.util.RoleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder

object TimetableAlarmSync {
  private val client: OkHttpClient = OfflineCache.getClient()

  suspend fun syncAndReschedule(
    context: Context,
    session: UserSession?
  ): Result<List<ScheduleItem>> = withContext(Dispatchers.IO) {
    val appCtx = context.applicationContext
    if (session == null || session.userId.isBlank() || session.accessToken.isBlank()) {
      return@withContext Result.failure(IllegalStateException("Missing session"))
    }
    if (RoleHelper.isStaffRole(session.role)) {
      AlarmScheduler.cancelAllAlarms(appCtx)
      return@withContext Result.success(emptyList())
    }

    val fetched = fetchStudentRegisteredSchedules(session)
    fetched.fold(
      onSuccess = { schedules ->
        AlarmScheduler.rescheduleAll(appCtx, schedules)
        Result.success(schedules)
      },
      onFailure = { err ->
        AlarmScheduler.rescheduleAll(appCtx, schedules = null)
        Result.failure(err)
      }
    )
  }

  suspend fun onClassTimetableRefreshed(
    context: Context,
    session: UserSession?,
    classId: String,
    classSchedules: List<ScheduleItem>
  ) = withContext(Dispatchers.IO) {
    val appCtx = context.applicationContext
    if (session == null || session.userId.isBlank() || session.accessToken.isBlank()) return@withContext
    if (RoleHelper.isStaffRole(session.role)) return@withContext

    val syncResult = fetchStudentRegisteredSchedules(session)
    if (syncResult.isSuccess) {
      AlarmScheduler.rescheduleAll(appCtx, syncResult.getOrDefault(emptyList()))
      return@withContext
    }

    val existing = AlarmPreferences.getCachedSchedules(appCtx)
      .filterNot { it.classId == classId }
    val updatedClassItems = classSchedules.map {
      if (it.classId.isBlank()) it.copy(classId = classId) else it
    }
    AlarmScheduler.rescheduleAll(appCtx, existing + updatedClassItems)
  }

  suspend fun onAuthenticated(context: Context, session: UserSession) {
    val appCtx = context.applicationContext
    AlarmPreferences.refreshState(appCtx)
    AlarmNotificationHelper.ensureChannel(appCtx)
    SyncWorker.enqueuePeriodicSync(appCtx)
    syncAndReschedule(appCtx, session)
  }

  fun onLoggedOut(context: Context) {
    val appCtx = context.applicationContext
    SyncWorker.cancelPeriodicSync(appCtx)
    AlarmSoundPlayer.stopRinging(appCtx)
    AlarmScheduler.cancelAllAlarms(appCtx)
    AlarmPreferences.clearTimetableData(appCtx)
  }

  private suspend fun fetchStudentRegisteredSchedules(
    session: UserSession
  ): Result<List<ScheduleItem>> {
    if (!SupabaseConfig.isConfigured()) {
      return Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val regCourses = queryRegisteredCourses(session.userId, session.accessToken)
    if (regCourses != null && regCourses.isNotEmpty()) {
      val byCourse = fetchSchedulesByCourseIds(regCourses, session.accessToken)
      if (byCourse.isSuccess) return byCourse
    }

    val classesRes = ClassesService(client).fetchClasses(session.userId, session.role, session.accessToken)
    if (classesRes.isFailure) {
      return Result.failure(classesRes.exceptionOrNull() ?: Exception("Failed to load classes"))
    }
    val classes = classesRes.getOrDefault(emptyList())
    val timetableService = TimetableService(client)
    val registerService = AttendanceRegisterService(client)

    val combined = mutableListOf<ScheduleItem>()
    val registeredIds = regCourses?.map { it.courseId }?.filter { it.isNotBlank() }?.toSet().orEmpty()

    for (cls in classes) {
      val units = registerService.getClassUnits(cls.id, session.accessToken).getOrDefault(emptyList())
      val schedulesRes = timetableService.fetchSchedules(cls.id, session.accessToken)
      if (schedulesRes.isSuccess) {
        val items = schedulesRes.getOrDefault(emptyList()).map { item ->
          val matchedUnit = units.firstOrNull { u ->
            (item.courseId.isNotBlank() && u.courseId == item.courseId) ||
              (item.courseCode.isNotBlank() && u.courseCode.equals(item.courseCode, ignoreCase = true)) ||
              (item.title.isNotBlank() && u.courseName.equals(item.title, ignoreCase = true))
          }
          item.copy(
            classId = item.classId.ifBlank { cls.id },
            courseId = item.courseId.ifBlank { matchedUnit?.courseId.orEmpty() },
            courseCode = item.courseCode.ifBlank { matchedUnit?.courseCode.orEmpty() }
          )
        }
        val filtered = if (registeredIds.isNotEmpty()) {
          items.filter { it.courseId.isNotBlank() && registeredIds.contains(it.courseId) }
        } else {
          items
        }
        combined.addAll(filtered)
      }
    }
    return Result.success(combined.distinctBy { it.id })
  }

  private fun queryRegisteredCourses(
    userId: String,
    accessToken: String
  ): List<RegisteredCourseItem>? {
    val encodedUid = URLEncoder.encode(userId, "UTF-8")
    val endpoints = listOf(
      "student_courses?user_id=eq.$encodedUid&select=course_id,courses(id,code,name)",
      "course_registrations?user_id=eq.$encodedUid&select=course_id,courses(id,code,name)",
      "course_registrations?student_id=eq.$encodedUid&select=course_id,courses(id,code,name)",
      "student_courses?user_id=eq.$encodedUid&select=*",
      "course_registrations?student_id=eq.$encodedUid&select=*"
    )
    for (path in endpoints) {
      val body = executeGet(path, accessToken) ?: continue
      val parsed = parseRegisteredCourses(body)
      if (parsed.isNotEmpty()) return parsed
    }
    return null
  }

  private fun parseRegisteredCourses(body: String): List<RegisteredCourseItem> {
    return runCatching {
      val arr = JSONArray(body)
      List(arr.length()) { i ->
        val obj = arr.getJSONObject(i)
        val cObj = obj.optJSONObject("courses")
        RegisteredCourseItem(
          courseId = JsonHelper.firstStr(obj, "course_id", "id").ifBlank { JsonHelper.str(cObj, "id") },
          courseCode = JsonHelper.str(obj, "course_code").ifBlank { JsonHelper.str(cObj, "code") },
          courseName = JsonHelper.firstStr(obj, "course_name", "name").ifBlank { JsonHelper.str(cObj, "name") },
          classId = JsonHelper.str(obj, "class_id")
        )
      }.filter { it.courseId.isNotBlank() || it.courseCode.isNotBlank() }
    }.getOrDefault(emptyList())
  }

  private fun fetchSchedulesByCourseIds(
    courses: List<RegisteredCourseItem>,
    accessToken: String
  ): Result<List<ScheduleItem>> {
    val ids = courses.map { it.courseId }.filter { it.isNotBlank() }.distinct()
    if (ids.isEmpty()) return Result.failure(IllegalStateException("No course IDs"))
    val courseMap = courses.associateBy { it.courseId }
    val inFilter = ids.joinToString(",")
    val queries = listOf(
      "schedules?course_id=in.($inFilter)&select=*,courses(id,code,name)&order=day_of_week.asc,start_time.asc",
      "schedules?course_id=in.($inFilter)&select=*&order=day_of_week.asc,start_time.asc"
    )
    for (q in queries) {
      val body = executeGet(q, accessToken) ?: continue
      val list = parseSchedulesArray(body, courseMap)
      return Result.success(list)
    }
    return Result.failure(Exception("Unable to query course schedules"))
  }

  fun parseSchedulesArray(
    body: String,
    courseMap: Map<String, RegisteredCourseItem> = emptyMap()
  ): List<ScheduleItem> {
    val arr = JSONArray(body)
    return List(arr.length()) { i ->
      val obj = arr.getJSONObject(i)
      val cObj = obj.optJSONObject("courses")
      val cid = JsonHelper.str(obj, "course_id").ifBlank { JsonHelper.str(cObj, "id") }
      val matched = courseMap[cid]
      val code = JsonHelper.str(obj, "course_code")
        .ifBlank { JsonHelper.str(cObj, "code") }
        .ifBlank { matched?.courseCode.orEmpty() }
      val title = JsonHelper.str(obj, "title")
        .ifBlank { JsonHelper.str(cObj, "name") }
        .ifBlank { matched?.courseName.orEmpty() }
      ScheduleItem(
        id = JsonHelper.str(obj, "id"),
        title = title.ifBlank { code.ifBlank { "Class" } },
        lecturerName = JsonHelper.str(obj, "lecturer_name"),
        dayOfWeek = JsonHelper.intVal(obj, "day_of_week", 1),
        startTime = JsonHelper.str(obj, "start_time"),
        endTime = JsonHelper.str(obj, "end_time"),
        room = JsonHelper.str(obj, "room"),
        status = JsonHelper.str(obj, "status").ifBlank { "scheduled" },
        statusNote = JsonHelper.firstStr(obj, "status_note", "note", "reason"),
        classId = JsonHelper.str(obj, "class_id").ifBlank { matched?.classId.orEmpty() },
        courseId = cid,
        courseCode = code
      )
    }.filter { it.id.isNotBlank() }
  }

  private fun executeGet(pathAndQuery: String, accessToken: String): String? {
    val req = Request.Builder()
      .url("${SupabaseConfig.url}/rest/v1/$pathAndQuery")
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Accept", "application/json")
      .get()
      .build()
    return try {
      client.newCall(req).execute().use { resp ->
        if (resp.isSuccessful) resp.body?.string().orEmpty() else null
      }
    } catch (_: Exception) {
      null
    }
  }
}
