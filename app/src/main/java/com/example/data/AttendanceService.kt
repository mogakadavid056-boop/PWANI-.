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
    classId: String, courseId: String? = null, sessionExpiryIso: String,
    sessionTitle: String = "Class Attendance", accessToken: String
  ): Result<CreateSessionV2Result> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    val body = JSONObject().apply {
      put("target_class_id", classId); put("target_course_id", courseId ?: JSONObject.NULL)
      put("session_expiry", sessionExpiryIso); put("session_title", sessionTitle)
    }
    postRpc("create_attendance_session_v2", body, accessToken).mapCatching { bodyStr ->
      val json = if (bodyStr.startsWith("[")) JSONArray(bodyStr).optJSONObject(0) ?: JSONObject() else JSONObject(bodyStr)
      CreateSessionV2Result(
        sessionId = json.optString("session_id", json.optString("id")),
        sessionCode = json.optString("session_code", json.optString("code")),
        expiresAt = json.optString("expires_at", sessionExpiryIso)
      )
    }
  }

  suspend fun getAttendanceCode(sessionId: String, accessToken: String): Result<String> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    val body = JSONObject().apply { put("target_session_id", sessionId) }
    postRpc("get_attendance_code", body, accessToken).mapCatching { bodyStr ->
      when {
        bodyStr.startsWith("[") -> {
          val arr = JSONArray(bodyStr)
          val first = if (arr.length() > 0) arr.opt(0) else ""
          if (first is JSONObject) first.optString("session_code", first.optString("code", "")) else first.toString().removeSurrounding("\"")
        }
        bodyStr.startsWith("{") -> JSONObject(bodyStr).optString("session_code", JSONObject(bodyStr).optString("code", ""))
        bodyStr.startsWith("\"") && bodyStr.endsWith("\"") -> bodyStr.removeSurrounding("\"")
        else -> bodyStr
      }.trim()
    }
  }

  suspend fun signAttendanceV2(
    sessionId: String, providedCode: String, signatureBase64: String, accessToken: String
  ): Result<SignAttendanceResult> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    val body = JSONObject().apply {
      put("target_session_id", sessionId); put("provided_code", providedCode.trim()); put("signature", signatureBase64)
    }
    postRpc("sign_attendance_v2", body, accessToken).mapCatching { bodyStr ->
      val json = if (bodyStr.startsWith("[")) JSONArray(bodyStr).optJSONObject(0) ?: JSONObject()
      else if (bodyStr.startsWith("{")) JSONObject(bodyStr) else JSONObject()
      SignAttendanceResult(ok = json.optBoolean("ok", true), message = json.optString("message", bodyStr))
    }
  }

  @Deprecated("Use createAttendanceSessionV2 instead")
  suspend fun createAttendanceSession(
    classId: String, sessionCode: String, sessionExpiryIso: String, accessToken: String
  ): Result<String> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    val body = JSONObject().apply {
      put("target_class_id", classId); put("target_course_id", JSONObject.NULL)
      put("session_code", sessionCode); put("session_expiry", sessionExpiryIso)
    }
    postRpc("create_attendance_session", body, accessToken).mapCatching { b ->
      if (b.startsWith("{")) JSONObject(b).optString("id", JSONObject(b).optString("session_id", b))
      else b.removeSurrounding("\"")
    }
  }

  @Deprecated("Use signAttendanceV2 instead")
  suspend fun signAttendance(
    sessionId: String, providedCode: String, signatureBase64: String, accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    val body = JSONObject().apply {
      put("target_session_id", sessionId); put("provided_code", providedCode.trim()); put("signature", signatureBase64)
    }
    postRpc("sign_attendance", body, accessToken).map { Unit }
  }

  suspend fun fetchStaffSessions(classId: String, accessToken: String): Result<List<AttendanceSessionItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    val url = "${SupabaseConfig.url}/rest/v1/attendance_sessions?class_id=eq.$classId&select=id,starts_at,expires_at,active&order=starts_at.desc"
    getQuery(url, accessToken).mapCatching { bodyStr ->
      val array = JSONArray(bodyStr)
      List(array.length()) { i ->
        val obj = array.getJSONObject(i)
        AttendanceSessionItem(obj.optString("id"), obj.optString("starts_at"), obj.optString("expires_at"), obj.optBoolean("active", true))
      }
    }
  }

  suspend fun fetchOpenSessions(classId: String, nowIso: String, accessToken: String): Result<List<AttendanceSessionItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    val encNow = try { URLEncoder.encode(nowIso, "UTF-8") } catch (_: Exception) { nowIso }
    val url = "${SupabaseConfig.url}/rest/v1/attendance_sessions?class_id=eq.$classId&active=eq.true&expires_at=gt.$encNow&select=id,starts_at,expires_at"
    getQuery(url, accessToken).mapCatching { bodyStr ->
      val array = JSONArray(bodyStr)
      List(array.length()) { i ->
        val obj = array.getJSONObject(i)
        AttendanceSessionItem(obj.optString("id"), obj.optString("starts_at"), obj.optString("expires_at"), true)
      }
    }
  }

  suspend fun getAttendanceList(sessionId: String, accessToken: String): Result<List<AttendanceRecordItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    val body = JSONObject().apply { put("target_session_id", sessionId) }
    postRpc("get_attendance_list", body, accessToken).mapCatching { bodyStr ->
      val array = JSONArray(bodyStr)
      List(array.length()) { i ->
        val obj = array.getJSONObject(i)
        AttendanceRecordItem(
          fullName = obj.optString("full_name").ifBlank { obj.optString("name", "Student") },
          admissionNumber = obj.optString("admission_number").ifBlank { obj.optString("admission_no", "N/A") },
          timeSigned = obj.optString("signed_at").ifBlank { obj.optString("created_at", "") },
          signatureData = obj.optString("signature_data").ifBlank { obj.optString("signature", "") }.ifBlank { null }
        )
      }
    }
  }

  suspend fun deleteAttendanceSession(sessionId: String, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    val url = "${SupabaseConfig.url}/rest/v1/attendance_sessions?id=eq.$sessionId"
    val req = Request.Builder().url(url).addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken").delete().build()
    try {
      client.newCall(req).execute().use { res ->
        val body = res.body?.string().orEmpty()
        if (res.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, res.code)))
      }
    } catch (e: Exception) { Result.failure(Exception(e.localizedMessage ?: "Failed to delete session")) }
  }
}
