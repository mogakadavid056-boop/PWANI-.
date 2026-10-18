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
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()
) {

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun fetchSchedules(
    classId: String,
    accessToken: String
  ): Result<List<ScheduleItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val url = "${SupabaseConfig.url}/rest/v1/schedules?class_id=eq.$classId&select=id,title,lecturer_name,day_of_week,start_time,end_time,room&order=day_of_week.asc,start_time.asc"
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
          list.add(
            ScheduleItem(
              id = obj.optString("id"),
              title = obj.optString("title"),
              lecturerName = obj.optString("lecturer_name"),
              dayOfWeek = obj.optInt("day_of_week", 1),
              startTime = obj.optString("start_time"),
              endTime = obj.optString("end_time"),
              room = obj.optString("room")
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
}
