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

class AttendanceTrackerService(
  private val client: OkHttpClient = OfflineCache.getClient()
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun getClassAttendanceRisk(classId: String, accessToken: String): Result<List<StudentRiskItem>> =
    withContext(Dispatchers.IO) {
      val payload = JSONObject().apply { put("target_class_id", classId) }
      postRpc("class_attendance_risk", payload, accessToken) { body ->
        parseStudentRiskList(body)
      }
    }

  suspend fun getStudentAttendanceDetail(
    classId: String,
    studentId: String,
    accessToken: String
  ): Result<List<StudentSessionTimelineItem>> = withContext(Dispatchers.IO) {
    val payload = JSONObject().apply {
      put("target_class_id", classId)
      put("target_student_id", studentId)
    }
    postRpc("student_attendance_detail", payload, accessToken) { body ->
      parseStudentTimeline(body)
    }
  }

  suspend fun setAbsenceExcused(
    sessionId: String,
    studentId: String,
    excused: Boolean,
    reason: String?,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val payload = JSONObject().apply {
      put("target_session_id", sessionId)
      put("target_student_id", studentId)
      put("excused", excused)
      if (!reason.isNullOrBlank()) put("reason", reason.trim())
    }
    postRpc("set_absence_excused", payload, accessToken) { }
  }

  suspend fun getMyAttendanceStatus(classId: String, accessToken: String): Result<MyAttendanceStatus> =
    withContext(Dispatchers.IO) {
      val payload = JSONObject().apply { put("target_class_id", classId) }
      postRpc("my_attendance_status", payload, accessToken) { body ->
        parseMyAttendanceStatus(body)
      }
    }

  private fun <T> postRpc(
    rpcName: String,
    payload: JSONObject,
    accessToken: String,
    parser: (String) -> T
  ): Result<T> {
    if (!SupabaseConfig.isConfigured()) {
      return Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val req = Request.Builder()
      .url("${SupabaseConfig.url}/rest/v1/rpc/$rpcName")
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(payload.toString().toRequestBody(jsonMediaType))
      .build()
    return try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          return Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
        }
        Result.success(parser(body))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Request failed")))
    }
  }

  companion object {
    fun parseStudentRiskList(body: String): List<StudentRiskItem> {
      val trimmed = body.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyList()
      val arr = JSONArray(trimmed)
      return buildList {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          val total = JsonHelper.intVal(o, "total_sessions", 0)
          val pct = o.optDouble("percentage", 100.0).let { if (it.isNaN()) 100.0 else it }
          val streak = JsonHelper.intVal(o, "miss_streak", 0)
          val levelRaw = JsonHelper.str(o, "risk_level")
          add(
            StudentRiskItem(
              studentId = JsonHelper.str(o, "student_id"),
              fullName = JsonHelper.str(o, "full_name").ifBlank { "Student" },
              admissionNumber = JsonHelper.orDash(o.optString("admission_number")),
              attended = JsonHelper.intVal(o, "attended", 0),
              excused = JsonHelper.intVal(o, "excused", 0),
              totalSessions = total,
              percentage = pct,
              missStreak = streak,
              lastAttendedAt = JsonHelper.str(o, "last_attended_at"),
              riskLevel = if (levelRaw.isNotBlank()) RiskLevel.fromKey(levelRaw) else RiskLevel.classify(pct, streak, total)
            )
          )
        }
      }
    }

    fun parseStudentTimeline(body: String): List<StudentSessionTimelineItem> {
      val trimmed = body.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyList()
      val arr = JSONArray(trimmed)
      return buildList {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          add(
            StudentSessionTimelineItem(
              sessionId = JsonHelper.str(o, "session_id"),
              title = JsonHelper.str(o, "title").ifBlank { "Class Session" },
              sessionType = JsonHelper.str(o, "session_type"),
              startsAt = JsonHelper.str(o, "starts_at"),
              status = JsonHelper.str(o, "status").ifBlank { "absent" }.lowercase(),
              excuseReason = JsonHelper.str(o, "excuse_reason")
            )
          )
        }
      }
    }

    fun parseMyAttendanceStatus(body: String): MyAttendanceStatus {
      val o = runCatching { JSONObject(body) }.getOrDefault(JSONObject())
      val total = JsonHelper.intVal(o, "total_sessions", 0)
      val pct = o.optDouble("percentage", 100.0).let { if (it.isNaN()) 100.0 else it }
      val streak = JsonHelper.intVal(o, "miss_streak", 0)
      val levelRaw = JsonHelper.str(o, "risk_level")
      return MyAttendanceStatus(
        attended = JsonHelper.intVal(o, "attended", 0),
        excused = JsonHelper.intVal(o, "excused", 0),
        totalSessions = total,
        percentage = pct,
        missStreak = streak,
        lastAttendedAt = JsonHelper.str(o, "last_attended_at"),
        riskLevel = if (levelRaw.isNotBlank()) RiskLevel.fromKey(levelRaw) else RiskLevel.classify(pct, streak, total)
      )
    }
  }
}
