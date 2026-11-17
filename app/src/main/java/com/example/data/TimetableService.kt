package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TimetableService(
  private val client: OkHttpClient = OfflineCache.getClient()
) {

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun fetchSchedules(
    classId: String,
    accessToken: String
  ): Result<List<ScheduleItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val url = "${SupabaseConfig.url}/rest/v1/schedules?class_id=eq.$classId&select=*&order=day_of_week.asc,start_time.asc"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Accept", "application/json")
      .get()
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val bodyString = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
        }

        val array = JSONArray(bodyString)
        val list = mutableListOf<ScheduleItem>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val courseObj = obj.optJSONObject("courses")
          val parsedCourseCode = com.example.util.JsonHelper.str(obj, "course_code")
            .ifBlank { com.example.util.JsonHelper.str(courseObj, "code") }
          list.add(
            ScheduleItem(
              id = com.example.util.JsonHelper.str(obj, "id"),
              title = com.example.util.JsonHelper.str(obj, "title"),
              lecturerName = com.example.util.JsonHelper.str(obj, "lecturer_name"),
              dayOfWeek = obj.optInt("day_of_week", 1),
              startTime = com.example.util.JsonHelper.str(obj, "start_time"),
              endTime = com.example.util.JsonHelper.str(obj, "end_time"),
              room = com.example.util.JsonHelper.str(obj, "room"),
              status = com.example.util.JsonHelper.str(obj, "status").ifBlank { "scheduled" },
              statusNote = com.example.util.JsonHelper.firstStr(obj, "status_note", "note", "reason"),
              classId = com.example.util.JsonHelper.str(obj, "class_id").ifBlank { classId },
              courseId = com.example.util.JsonHelper.str(obj, "course_id")
                .ifBlank { com.example.util.JsonHelper.str(courseObj, "id") },
              courseCode = parsedCourseCode
            )
          )
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to load timetable"))
    }
  }

  suspend fun addSchedule(
    classId: String,
    title: String,
    dayOfWeek: Int,
    startTime: String,
    endTime: String,
    room: String,
    lecturerName: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val body = JSONObject().apply {
      put("class_id", classId)
      put("title", title.trim())
      put("day_of_week", dayOfWeek)
      put("start_time", startTime.trim())
      put("end_time", endTime.trim())
      put("room", room.trim())
      put("lecturer_name", lecturerName.trim())
    }

    val url = "${SupabaseConfig.url}/rest/v1/schedules"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .post(body.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val bodyString = response.body?.string().orEmpty()
        if (response.isSuccessful) {
          Result.success(Unit)
        } else {
          Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to add timetable class"))
    }
  }

  suspend fun updateSchedule(
    scheduleId: String,
    title: String,
    dayOfWeek: Int,
    startTime: String,
    endTime: String,
    room: String,
    lecturerName: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val body = JSONObject().apply {
      put("title", title.trim())
      put("day_of_week", dayOfWeek)
      put("start_time", startTime.trim())
      put("end_time", endTime.trim())
      put("room", room.trim())
      put("lecturer_name", lecturerName.trim())
    }

    val url = "${SupabaseConfig.url}/rest/v1/schedules?id=eq.$scheduleId"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .patch(body.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val bodyString = response.body?.string().orEmpty()
        if (response.isSuccessful) {
          Result.success(Unit)
        } else {
          Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to update timetable class"))
    }
  }

  suspend fun deleteSchedule(
    scheduleId: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val url = "${SupabaseConfig.url}/rest/v1/schedules?id=eq.$scheduleId"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .delete()
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val bodyString = response.body?.string().orEmpty()
        if (response.isSuccessful) {
          Result.success(Unit)
        } else {
          Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to delete timetable class"))
    }
  }

  suspend fun updateScheduleStatus(
    scheduleId: String,
    status: String,
    statusNote: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val body = JSONObject().apply {
      put("status", status.trim().lowercase())
      put("status_note", statusNote.trim())
    }

    val url = "${SupabaseConfig.url}/rest/v1/schedules?id=eq.$scheduleId"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .patch(body.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val bodyString = response.body?.string().orEmpty()
        if (response.isSuccessful) {
          Result.success(Unit)
        } else {
          Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to update class status"))
    }
  }
}
