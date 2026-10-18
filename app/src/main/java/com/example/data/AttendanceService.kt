package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class AttendanceService(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()
) {

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun createAttendanceSession(
    classId: String,
    sessionCode: String,
    sessionExpiryIso: String,
    accessToken: String
  ): Result<String> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val body = JSONObject().apply {
      put("target_class_id", classId)
      put("target_course_id", JSONObject.NULL)
      put("session_code", sessionCode)
      put("session_expiry", sessionExpiryIso)
    }

    val url = "${SupabaseConfig.url}/rest/v1/rpc/create_attendance_session"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .post(body.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val bodyString = response.body?.string().orEmpty().trim()
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
        }

        val sessionId = when {
          bodyString.startsWith("\"") && bodyString.endsWith("\"") -> bodyString.removeSurrounding("\"")
          bodyString.startsWith("{") -> {
            val json = JSONObject(bodyString)
            json.optString("id", json.optString("session_id", bodyString))
          }
          else -> bodyString
        }
        Result.success(sessionId)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to start attendance session"))
    }
  }

  suspend fun fetchStaffSessions(
    classId: String,
    accessToken: String
  ): Result<List<AttendanceSessionItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val url = "${SupabaseConfig.url}/rest/v1/attendance_sessions?class_id=eq.$classId&select=id,starts_at,expires_at,active&order=starts_at.desc"
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
        val list = mutableListOf<AttendanceSessionItem>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          list.add(
            AttendanceSessionItem(
              id = obj.optString("id"),
              startsAt = obj.optString("starts_at"),
              expiresAt = obj.optString("expires_at"),
              active = obj.optBoolean("active", true)
            )
          )
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to load past sessions"))
    }
  }

  suspend fun fetchOpenSessions(
    classId: String,
    nowIso: String,
    accessToken: String
  ): Result<List<AttendanceSessionItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val encodedNow = try { URLEncoder.encode(nowIso, "UTF-8") } catch (_: Exception) { nowIso }
    val url = "${SupabaseConfig.url}/rest/v1/attendance_sessions?class_id=eq.$classId&active=eq.true&expires_at=gt.$encodedNow&select=id,starts_at,expires_at"
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
        val list = mutableListOf<AttendanceSessionItem>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          list.add(
            AttendanceSessionItem(
              id = obj.optString("id"),
              startsAt = obj.optString("starts_at"),
              expiresAt = obj.optString("expires_at"),
              active = true
            )
          )
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to load open sessions"))
    }
  }

  suspend fun getAttendanceList(
    sessionId: String,
    accessToken: String
  ): Result<List<AttendanceRecordItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val body = JSONObject().apply { put("target_session_id", sessionId) }
    val url = "${SupabaseConfig.url}/rest/v1/rpc/get_attendance_list"
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
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
        }

        val array = JSONArray(bodyString)
        val list = mutableListOf<AttendanceRecordItem>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val fullName = obj.optString("full_name").ifBlank { obj.optString("name", "Student") }
          val admNo = obj.optString("admission_number").ifBlank { obj.optString("admission_no", "N/A") }
          val timeSigned = obj.optString("signed_at").ifBlank { obj.optString("created_at", "") }
          val sigData = obj.optString("signature_data").ifBlank { obj.optString("signature", "") }
          list.add(AttendanceRecordItem(fullName = fullName, admissionNumber = admNo, timeSigned = timeSigned, signatureData = sigData.ifBlank { null }))
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to load attendance records"))
    }
  }

  suspend fun signAttendance(
    sessionId: String,
    providedCode: String,
    signatureBase64: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val body = JSONObject().apply {
      put("target_session_id", sessionId)
      put("provided_code", providedCode.trim())
      put("signature", signatureBase64)
    }

    val url = "${SupabaseConfig.url}/rest/v1/rpc/sign_attendance"
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
      Result.failure(Exception(e.localizedMessage ?: "Failed to sign attendance"))
    }
  }

  suspend fun deleteAttendanceSession(
    sessionId: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val url = "${SupabaseConfig.url}/rest/v1/attendance_sessions?id=eq.$sessionId"
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
      Result.failure(Exception(e.localizedMessage ?: "Failed to delete attendance session"))
    }
  }
}
