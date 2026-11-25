package com.example.data

import com.example.util.JsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AttendanceRegisterService(
  private val client: OkHttpClient = OfflineCache.getClient()
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  private fun postRpc(rpcName: String, body: JSONObject, accessToken: String): Result<String> {
    if (!SupabaseConfig.isConfigured()) {
      return Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val url = "${SupabaseConfig.url}/rest/v1/rpc/$rpcName"
    val req = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .post(body.toString().toRequestBody(jsonMediaType))
      .build()

    return try {
      client.newCall(req).execute().use { res ->
        val bodyStr = res.body?.string().orEmpty().trim()
        if (res.isSuccessful) Result.success(bodyStr)
        else Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyStr, res.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Request failed"))
    }
  }

  suspend fun listRegisters(
    classId: String,
    accessToken: String
  ): Result<List<AttendanceRegisterListItem>> = withContext(Dispatchers.IO) {
    val body = JSONObject().apply { put("target_class_id", classId) }
    postRpc("list_registers", body, accessToken).mapCatching {
      AttendanceRegisterParser.parseRegisterList(it)
    }
  }

  suspend fun getAttendanceRegister(
    sessionId: String,
    accessToken: String
  ): Result<AttendanceRegisterDetail> = withContext(Dispatchers.IO) {
    val body = JSONObject().apply { put("target_session_id", sessionId) }
    postRpc("get_attendance_register", body, accessToken).mapCatching {
      AttendanceRegisterParser.parseRegisterDetail(it, sessionId)
    }
  }

  suspend fun getClassUnits(
    classId: String,
    accessToken: String
  ): Result<List<ClassUnitItem>> = withContext(Dispatchers.IO) {
    val body = JSONObject().apply { put("target_class_id", classId) }
    postRpc("get_class_units", body, accessToken).mapCatching {
      AttendanceRegisterParser.parseClassUnits(it)
    }
  }

  suspend fun updateRegisterDetails(
    sessionId: String,
    courseTitle: String,
    courseCode: String,
    lecturerName: String,
    venueText: String,
    sessionTitle: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val body = JSONObject().apply {
      put("target_session_id", sessionId)
      put("course_title", courseTitle.trim())
      put("course_code", courseCode.trim())
      put("lecturer_name", lecturerName.trim())
      put("venue_text", venueText.trim())
      put("session_title", sessionTitle.trim().ifBlank { "Class Attendance" })
    }
    postRpc("update_register_details", body, accessToken).map { Unit }
  }

  suspend fun closeAttendanceSession(
    sessionId: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val body = JSONObject().apply { put("target_session_id", sessionId) }
    postRpc("close_attendance_session", body, accessToken).map { Unit }
  }

  suspend fun signOutAttendance(
    sessionId: String,
    accessToken: String
  ): Result<Pair<Boolean, String>> = withContext(Dispatchers.IO) {
    val body = JSONObject().apply { put("target_session_id", sessionId) }
    postRpc("sign_out_attendance", body, accessToken).mapCatching { bodyStr ->
      val obj = if (bodyStr.startsWith("{")) JSONObject(bodyStr) else JSONObject()
      val ok = JsonHelper.bool(obj, "ok", true)
      val msg = JsonHelper.str(obj, "message").ifBlank {
        if (ok) "Signed out successfully" else bodyStr
      }
      Pair(ok, msg)
    }
  }

  suspend fun signRegister(
    sessionId: String,
    signerRole: String,
    signature: String,
    accessToken: String
  ): Result<Pair<Boolean, String>> = withContext(Dispatchers.IO) {
    val body = JSONObject().apply {
      put("target_session_id", sessionId)
      put("signer_role", signerRole.lowercase())
      put("signature", signature)
    }
    postRpc("sign_register", body, accessToken).mapCatching { bodyStr ->
      val obj = if (bodyStr.startsWith("{")) JSONObject(bodyStr) else JSONObject()
      val ok = JsonHelper.bool(obj, "ok", true)
      val msg = JsonHelper.str(obj, "message").ifBlank {
        if (ok) "Register signed successfully" else bodyStr
      }
      Pair(ok, msg)
    }
  }

  suspend fun voidAttendanceSession(
    sessionId: String,
    reason: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val body = JSONObject().apply {
      put("target_session_id", sessionId)
      put("reason", reason.trim())
    }
    postRpc("void_attendance_session", body, accessToken).map { Unit }
  }

  suspend fun deleteAttendanceSession(
    sessionId: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val body = JSONObject().apply { put("target_session_id", sessionId) }
    postRpc("delete_attendance_session", body, accessToken).map { Unit }
  }

  suspend fun correctAttendanceRecord(
    recordId: String,
    newTimeIn: String,
    newTimeOut: String,
    reason: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val body = JSONObject().apply {
      put("target_record_id", recordId)
      put("new_time_in", newTimeIn.trim())
      put("new_time_out", if (newTimeOut.isBlank()) JSONObject.NULL else newTimeOut.trim())
      put("reason", reason.trim())
    }
    postRpc("correct_attendance_record", body, accessToken).map { Unit }
  }

  suspend fun markPresent(
    sessionId: String,
    studentId: String,
    newTimeIn: String,
    newTimeOut: String,
    reason: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val body = JSONObject().apply {
      put("target_session_id", sessionId)
      put("target_student_id", studentId)
      put("new_time_in", newTimeIn.trim())
      put("new_time_out", if (newTimeOut.isBlank()) JSONObject.NULL else newTimeOut.trim())
      put("reason", reason.trim())
    }
    postRpc("mark_present", body, accessToken).map { Unit }
  }

  suspend fun updateMyDetails(
    fullName: String,
    admissionNo: String,
    phone: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val body = JSONObject().apply {
      put("member_full_name", fullName.trim())
      put("member_admission_no", admissionNo.trim())
      put("member_phone", if (phone.isBlank()) JSONObject.NULL else phone.trim())
    }
    postRpc("update_my_details", body, accessToken).map { Unit }
  }
}
