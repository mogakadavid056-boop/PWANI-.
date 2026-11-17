package com.example.alarms

import android.content.Context
import android.content.SharedPreferences
import com.example.data.ScheduleItem
import com.example.util.JsonHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class ScheduledAlarmRecord(
  val requestCode: Int,
  val scheduleId: String,
  val classId: String,
  val courseKey: String,
  val courseCode: String,
  val title: String,
  val lecturerName: String,
  val room: String,
  val dayOfWeek: Int,
  val startTime: String,
  val endTime: String,
  val offsetMinutes: Int,
  val triggerAtMillis: Long,
  val isSnooze: Boolean = false
)

data class AlarmSettingsState(
  val masterEnabled: Boolean = true,
  val reminderMinutes: Int = 15,
  val disabledCourses: Set<String> = emptySet(),
  val cachedSchedules: List<ScheduleItem> = emptyList(),
  val onboardingCompleted: Boolean = false
) {
  fun isCourseEnabled(courseKey: String): Boolean {
    return courseKey.isBlank() || !disabledCourses.contains(courseKey)
  }
}

object AlarmPreferences {
  private const val PREFS_NAME = "classrep_alarm_prefs"
  private const val KEY_MASTER_ENABLED = "master_enabled"
  private const val KEY_REMINDER_MINUTES = "reminder_minutes"
  private const val KEY_DISABLED_COURSES = "disabled_courses"
  private const val KEY_CACHED_SCHEDULES = "cached_schedules"
  private const val KEY_SCHEDULED_REGISTRY = "scheduled_registry"
  private const val KEY_ONBOARDING_DONE = "onboarding_done"

  val REMINDER_OPTIONS = listOf(0, 5, 10, 15, 30)

  private val _state = MutableStateFlow(AlarmSettingsState())
  val state: StateFlow<AlarmSettingsState> = _state.asStateFlow()

  private fun prefs(context: Context): SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun refreshState(context: Context): AlarmSettingsState {
    val p = prefs(context)
    val snapshot = AlarmSettingsState(
      masterEnabled = p.getBoolean(KEY_MASTER_ENABLED, true),
      reminderMinutes = p.getInt(KEY_REMINDER_MINUTES, 15),
      disabledCourses = p.getStringSet(KEY_DISABLED_COURSES, emptySet())?.toSet() ?: emptySet(),
      cachedSchedules = decodeSchedules(p.getString(KEY_CACHED_SCHEDULES, "[]").orEmpty()),
      onboardingCompleted = p.getBoolean(KEY_ONBOARDING_DONE, false)
    )
    _state.value = snapshot
    return snapshot
  }

  fun isMasterEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_MASTER_ENABLED, true)

  fun setMasterEnabled(context: Context, enabled: Boolean) {
    prefs(context).edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
    refreshState(context)
  }

  fun getReminderMinutes(context: Context): Int = prefs(context).getInt(KEY_REMINDER_MINUTES, 15)

  fun setReminderMinutes(context: Context, minutes: Int) {
    val safe = if (minutes in REMINDER_OPTIONS) minutes else 15
    prefs(context).edit().putInt(KEY_REMINDER_MINUTES, safe).apply()
    refreshState(context)
  }

  fun getDisabledCourses(context: Context): Set<String> =
    prefs(context).getStringSet(KEY_DISABLED_COURSES, emptySet())?.toSet() ?: emptySet()

  fun isCourseEnabled(context: Context, courseKey: String): Boolean {
    if (courseKey.isBlank()) return true
    return !getDisabledCourses(context).contains(courseKey)
  }

  fun setCourseEnabled(context: Context, courseKey: String, enabled: Boolean) {
    if (courseKey.isBlank()) return
    val current = getDisabledCourses(context).toMutableSet()
    if (enabled) current.remove(courseKey) else current.add(courseKey)
    prefs(context).edit().putStringSet(KEY_DISABLED_COURSES, current).apply()
    refreshState(context)
  }

  fun isOnboardingCompleted(context: Context): Boolean =
    prefs(context).getBoolean(KEY_ONBOARDING_DONE, false)

  fun setOnboardingCompleted(context: Context, completed: Boolean = true) {
    prefs(context).edit().putBoolean(KEY_ONBOARDING_DONE, completed).apply()
    refreshState(context)
  }

  fun saveCachedSchedules(context: Context, schedules: List<ScheduleItem>) {
    prefs(context).edit().putString(KEY_CACHED_SCHEDULES, encodeSchedules(schedules)).apply()
    refreshState(context)
  }

  fun getCachedSchedules(context: Context): List<ScheduleItem> =
    decodeSchedules(prefs(context).getString(KEY_CACHED_SCHEDULES, "[]").orEmpty())

  fun saveScheduledRegistry(context: Context, records: List<ScheduledAlarmRecord>) {
    prefs(context).edit().putString(KEY_SCHEDULED_REGISTRY, encodeRegistry(records)).apply()
  }

  fun getScheduledRegistry(context: Context): List<ScheduledAlarmRecord> =
    decodeRegistry(prefs(context).getString(KEY_SCHEDULED_REGISTRY, "[]").orEmpty())

  fun clearTimetableData(context: Context) {
    prefs(context).edit()
      .remove(KEY_CACHED_SCHEDULES)
      .remove(KEY_SCHEDULED_REGISTRY)
      .apply()
    refreshState(context)
  }

  fun encodeSchedules(schedules: List<ScheduleItem>): String {
    val arr = JSONArray()
    schedules.forEach { s ->
      arr.put(JSONObject().apply {
        put("id", s.id)
        put("title", s.title)
        put("lecturer_name", s.lecturerName)
        put("day_of_week", s.dayOfWeek)
        put("start_time", s.startTime)
        put("end_time", s.endTime)
        put("room", s.room)
        put("status", s.status)
        put("status_note", s.statusNote)
        put("class_id", s.classId)
        put("course_id", s.courseId)
        put("course_code", s.courseCode)
      })
    }
    return arr.toString()
  }

  fun decodeSchedules(json: String): List<ScheduleItem> {
    if (json.isBlank()) return emptyList()
    return runCatching {
      val arr = JSONArray(json)
      List(arr.length()) { i ->
        val obj = arr.getJSONObject(i)
        ScheduleItem(
          id = JsonHelper.str(obj, "id"),
          title = JsonHelper.str(obj, "title"),
          lecturerName = JsonHelper.str(obj, "lecturer_name"),
          dayOfWeek = JsonHelper.intVal(obj, "day_of_week", 1),
          startTime = JsonHelper.str(obj, "start_time"),
          endTime = JsonHelper.str(obj, "end_time"),
          room = JsonHelper.str(obj, "room"),
          status = JsonHelper.str(obj, "status").ifBlank { "scheduled" },
          statusNote = JsonHelper.str(obj, "status_note"),
          classId = JsonHelper.str(obj, "class_id"),
          courseId = JsonHelper.str(obj, "course_id"),
          courseCode = JsonHelper.str(obj, "course_code")
        )
      }
    }.getOrDefault(emptyList())
  }

  private fun encodeRegistry(records: List<ScheduledAlarmRecord>): String {
    val arr = JSONArray()
    records.forEach { r ->
      arr.put(JSONObject().apply {
        put("requestCode", r.requestCode)
        put("scheduleId", r.scheduleId)
        put("classId", r.classId)
        put("courseKey", r.courseKey)
        put("courseCode", r.courseCode)
        put("title", r.title)
        put("lecturerName", r.lecturerName)
        put("room", r.room)
        put("dayOfWeek", r.dayOfWeek)
        put("startTime", r.startTime)
        put("endTime", r.endTime)
        put("offsetMinutes", r.offsetMinutes)
        put("triggerAtMillis", r.triggerAtMillis)
        put("isSnooze", r.isSnooze)
      })
    }
    return arr.toString()
  }

  private fun decodeRegistry(json: String): List<ScheduledAlarmRecord> {
    if (json.isBlank()) return emptyList()
    return runCatching {
      val arr = JSONArray(json)
      List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        ScheduledAlarmRecord(
          requestCode = o.optInt("requestCode", 0),
          scheduleId = JsonHelper.str(o, "scheduleId"),
          classId = JsonHelper.str(o, "classId"),
          courseKey = JsonHelper.str(o, "courseKey"),
          courseCode = JsonHelper.str(o, "courseCode"),
          title = JsonHelper.str(o, "title"),
          lecturerName = JsonHelper.str(o, "lecturerName"),
          room = JsonHelper.str(o, "room"),
          dayOfWeek = JsonHelper.intVal(o, "dayOfWeek", 1),
          startTime = JsonHelper.str(o, "startTime"),
          endTime = JsonHelper.str(o, "endTime"),
          offsetMinutes = JsonHelper.intVal(o, "offsetMinutes", 0),
          triggerAtMillis = o.optLong("triggerAtMillis", 0L),
          isSnooze = JsonHelper.bool(o, "isSnooze", false)
        )
      }
    }.getOrDefault(emptyList())
  }
}
