package com.example.data

import com.example.util.JsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class AttendanceService(
  private val client: OkHttpClient = OfflineCache.getClient()
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  private fun postRpc(rpcName: String, body: JSONObject, accessToken: String): Result<String> {
    val url = "${SupabaseConfig.url}/rest/v1/rpc/$rpcName"
    val req = Request.Builder().url(url).addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken").addHeader("Content-Type", "application/json")
      .post(body.toString().toRequestBody(jsonMediaType)).build()
    return try {
      client.newCall(req).execute().use { res ->
        val bodyStr = res.body?.string().orEmpty().trim()
        if (res.isSuccessful) Result.success(bodyStr)
        else Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyStr, res.code)))
      }
    } catch (e: Exception) { Result.failure(Exception(e.localizedMessage ?: "Request failed")) }
  }

  private fun getQuery(url: String, accessToken: String): Result<String> {
    val req = Request.Builder().url(url).addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken").addHeader("Accept", "application/json").get().build()
    return try {
      client.newCall(req).execute().use { res ->
        val bodyStr = res.body?.string().orEmpty()
        if (res.isSuccessful) Result.success(bodyStr)
        else Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyStr, res.code)))
      }
    } catch (e: Exception) { Result.failure(Exception(e.localizedMessage ?: "Request failed")) }
  }

  suspend fun createAttendanceSessionV2(
    classId: String,
    courseId: String? = null,
    sessionExpiryIso: String,
    sessionTitle: String = "Class Attendance",
    accessToken: String
  ): Result<CreateSessionV2Result> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val body = JSONObject().apply {
      put("target_class_id", classId)
      put("target_course_id", if (courseId.isNullOrBlank()) JSONObject.NULL else courseId)
      put("session_expiry", sessionExpiryIso)
      put("session_title", sessionTitle.trim().ifBlank { "Class Attendance" })
    }
    postRpc("create_attendance_session_v2", body, accessToken).mapCatching { bodyStr ->
      val json = if (bodyStr.startsWith("[")) JSONArray(bodyStr).optJSONObject(0) ?: JSONObject() else JSONObject(bodyStr)
      CreateSessionV2Result(
        sessionId = JsonHelper.firstStr(json, "session_id", "id"),
        sessionCode = JsonHelper.firstStr(json, "session_code", "code"),
        expiresAt = JsonHelper.str(json, "expires_at").ifBlank { sessionExpiryIso }
      )
    }
  }

  suspend fun getAttendanceCode(sessionId: String, accessToken: String): Result<String> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val body = JSONObject().apply { put("target_session_id", sessionId) }
    postRpc("get_attendance_code", body, accessToken).mapCatching { bodyStr ->
      val extracted = when {
        bodyStr.startsWith("[") -> {
          val arr = JSONArray(bodyStr)
          val first = if (arr.length() > 0) arr.opt(0) else null
          if (first is JSONObject) JsonHelper.firstStr(first, "session_code", "code")
          else first?.toString()?.removeSurrounding("\"")?.takeIf { !it.equals("null", true) }.orEmpty()
        }
        bodyStr.startsWith("{") -> JsonHelper.firstStr(JSONObject(bodyStr), "session_code", "code")
        bodyStr.startsWith("\"") && bodyStr.endsWith("\"") -> bodyStr.removeSurrounding("\"")
        else -> bodyStr
      }.trim()
      if (extracted.equals("null", ignoreCase = true)) "" else extracted
    }
  }

  suspend fun signAttendanceV2(
    sessionId: String,
    providedCode: String,
    signatureBase64: String,
    accessToken: String,
    memberFullName: String? = null,
    memberAdmissionNo: String? = null
  ): Result<SignAttendanceResult> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val body = JSONObject().apply {
      put("target_session_id", sessionId)
      put("provided_code", providedCode.trim())
      put("signature", signatureBase64)
      if (!memberFullName.isNullOrBlank()) put("member_full_name", memberFullName.trim())
      if (!memberAdmissionNo.isNullOrBlank()) put("member_admission_no", memberAdmissionNo.trim())
    }
    postRpc("sign_attendance_v2", body, accessToken).mapCatching { bodyStr ->
      val json = when {
        bodyStr.startsWith("[") -> JSONArray(bodyStr).optJSONObject(0) ?: JSONObject()
        bodyStr.startsWith("{") -> JSONObject(bodyStr)
        else -> JSONObject()
      }
      SignAttendanceResult(
        ok = JsonHelper.bool(json, "ok", true),
        message = JsonHelper.str(json, "message").ifBlank { bodyStr },
        needsDetails = JsonHelper.bool(json, "needs_details", false)
      )
    }
  }

  suspend fun fetchStaffSessions(classId: String, accessToken: String): Result<List<AttendanceSessionItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val url = "${SupabaseConfig.url}/rest/v1/attendance_sessions?class_id=eq.$classId&select=id,starts_at,expires_at,active&order=starts_at.desc"
    getQuery(url, accessToken).mapCatching { bodyStr ->
      val array = JSONArray(bodyStr)
      List(array.length()) { i ->
        val obj = array.getJSONObject(i)
        AttendanceSessionItem(
          id = JsonHelper.str(obj, "id"),
          startsAt = JsonHelper.str(obj, "starts_at"),
          expiresAt = JsonHelper.str(obj, "expires_at"),
          active = JsonHelper.bool(obj, "active", true)
        )
      }
    }
  }

  suspend fun fetchOpenSessions(classId: String, nowIso: String, accessToken: String): Result<List<AttendanceSessionItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val encNow = try { URLEncoder.encode(nowIso, "UTF-8") } catch (_: Exception) { nowIso }
    val url = "${SupabaseConfig.url}/rest/v1/attendance_sessions?class_id=eq.$classId&active=eq.true&expires_at=gt.$encNow&select=id,starts_at,expires_at"
    getQuery(url, accessToken).mapCatching { bodyStr ->
      val array = JSONArray(bodyStr)
      List(array.length()) { i ->
        val obj = array.getJSONObject(i)
        AttendanceSessionItem(
          id = JsonHelper.str(obj, "id"),
          startsAt = JsonHelper.str(obj, "starts_at"),
          expiresAt = JsonHelper.str(obj, "expires_at"),
          active = true
        )
      }
    }
  }

  suspend fun getAttendanceList(sessionId: String, accessToken: String): Result<List<AttendanceRecordItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val body = JSONObject().apply { put("target_session_id", sessionId) }
    postRpc("get_attendance_list", body, accessToken).mapCatching { bodyStr ->
      val array = JSONArray(bodyStr)
      List(array.length()) { i ->
        val obj = array.getJSONObject(i)
        AttendanceRecordItem(
          fullName = JsonHelper.firstStr(obj, "full_name", "name").ifBlank { "Student" },
          admissionNumber = JsonHelper.firstStr(obj, "admission_number", "admission_no").ifBlank { "-" },
          timeSigned = JsonHelper.firstStr(obj, "time_in_text", "signed_at", "created_at"),
          signatureData = JsonHelper.firstStr(obj, "signature_data", "signature").ifBlank { null }
        )
      }
    }
  }
}
