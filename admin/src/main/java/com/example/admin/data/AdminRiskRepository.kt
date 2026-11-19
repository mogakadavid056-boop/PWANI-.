package com.example.admin.data

import com.example.admin.util.AdminJsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject

class AdminRiskRepository(
  private val client: OkHttpClient = SupabaseAdminConfig.sharedHttpClient
) {
  suspend fun getRiskOverview(maxRows: Int = 30, offsetRows: Int = 0, accessToken: String): Result<List<AdminClassRiskSummary>> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("max_rows", maxRows); put("offset_rows", offsetRows) }
      AdminHttpHelper.postRpc(client, "admin_attendance_risk_overview", body, accessToken).mapCatching {
        parseRiskOverview(it)
      }
    }

  suspend fun getAtRiskStudents(
    classId: String?,
    minLevel: String,
    searchText: String,
    maxRows: Int = 30,
    offsetRows: Int = 0,
    accessToken: String
  ): Result<List<AdminAtRiskStudentItem>> = withContext(Dispatchers.IO) {
    val body = JSONObject().apply {
      put("target_class_id", if (classId.isNullOrBlank()) JSONObject.NULL else classId)
      put("min_level", minLevel.lowercase())
      put("search_text", searchText.trim())
      put("max_rows", maxRows)
      put("offset_rows", offsetRows)
    }
    AdminHttpHelper.postRpc(client, "admin_at_risk_students", body, accessToken).mapCatching {
      parseAtRiskStudents(it)
    }
  }

  suspend fun getClassAttendanceRisk(classId: String, accessToken: String): Result<List<AdminAtRiskStudentItem>> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("target_class_id", classId) }
      AdminHttpHelper.postRpc(client, "class_attendance_risk", body, accessToken).mapCatching { raw ->
        parseAtRiskStudents(raw, fallbackClassId = classId)
      }
    }

  suspend fun getStudentTimeline(classId: String, studentId: String, accessToken: String): Result<List<AdminStudentTimelineItem>> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("target_class_id", classId); put("target_student_id", studentId) }
      AdminHttpHelper.postRpc(client, "student_attendance_detail", body, accessToken).mapCatching {
        parseTimeline(it)
      }
    }

  suspend fun setAbsenceExcused(sessionId: String, studentId: String, excused: Boolean, reason: String?, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply {
        put("target_session_id", sessionId); put("target_student_id", studentId); put("excused", excused)
        if (!reason.isNullOrBlank()) put("reason", reason.trim())
      }
      AdminHttpHelper.postRpc(client, "set_absence_excused", body, accessToken).map { }
    }

  suspend fun runAttendanceAlerts(accessToken: String): Result<AdminRunAlertsResult> =
    withContext(Dispatchers.IO) {
      AdminHttpHelper.postRpc(client, "admin_run_attendance_alerts", JSONObject(), accessToken).mapCatching { raw ->
        val o = runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
        AdminRunAlertsResult(
          processedClasses = AdminJsonHelper.intVal(o, "processed_classes", 0),
          warningsSent = AdminJsonHelper.intVal(o, "warnings_sent", 0),
          alertsSent = AdminJsonHelper.intVal(o, "alerts_sent", 0)
        )
      }
    }

  companion object {
    fun parseRiskOverview(raw: String): List<AdminClassRiskSummary> {
      val trimmed = raw.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyList()
      val arr = JSONArray(trimmed)
      return buildList {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          add(
            AdminClassRiskSummary(
              classId = AdminJsonHelper.str(o, "class_id"),
              className = AdminJsonHelper.str(o, "class_name").ifBlank { "Class" },
              schoolName = AdminJsonHelper.str(o, "school_name"),
              studentCount = AdminJsonHelper.intVal(o, "student_count", 0),
              totalSessions = AdminJsonHelper.intVal(o, "total_sessions", 0),
              criticalCount = AdminJsonHelper.intVal(o, "critical_count", 0),
              highCount = AdminJsonHelper.intVal(o, "high_count", 0),
              watchCount = AdminJsonHelper.intVal(o, "watch_count", 0),
              okCount = AdminJsonHelper.intVal(o, "ok_count", 0)
            )
          )
        }
      }
    }

    fun parseAtRiskStudents(raw: String, fallbackClassId: String = "", fallbackClassName: String = ""): List<AdminAtRiskStudentItem> {
      val trimmed = raw.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyList()
      val arr = JSONArray(trimmed)
      return buildList {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          val total = AdminJsonHelper.intVal(o, "total_sessions", 0)
          val pct = o.optDouble("percentage", 100.0).let { if (it.isNaN()) 100.0 else it }
          val streak = AdminJsonHelper.intVal(o, "miss_streak", 0)
          val lvlRaw = AdminJsonHelper.str(o, "risk_level")
          add(
            AdminAtRiskStudentItem(
              classId = AdminJsonHelper.str(o, "class_id").ifBlank { fallbackClassId },
              className = AdminJsonHelper.str(o, "class_name").ifBlank { fallbackClassName },
              studentId = AdminJsonHelper.str(o, "student_id"),
              fullName = AdminJsonHelper.str(o, "full_name").ifBlank { "Student" },
              admissionNumber = AdminJsonHelper.orDash(o.optString("admission_number")),
              attended = AdminJsonHelper.intVal(o, "attended", 0),
              excused = AdminJsonHelper.intVal(o, "excused", 0),
              totalSessions = total,
              percentage = pct,
              missStreak = streak,
              lastAttendedAt = AdminJsonHelper.str(o, "last_attended_at"),
              riskLevel = if (lvlRaw.isNotBlank()) AdminRiskLevel.fromKey(lvlRaw) else AdminRiskLevel.classify(pct, streak, total)
            )
          )
        }
      }
    }

    fun parseTimeline(raw: String): List<AdminStudentTimelineItem> {
      val trimmed = raw.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyList()
      val arr = JSONArray(trimmed)
      return buildList {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          add(
            AdminStudentTimelineItem(
              sessionId = AdminJsonHelper.str(o, "session_id"),
              title = AdminJsonHelper.str(o, "title").ifBlank { "Class Session" },
              sessionType = AdminJsonHelper.str(o, "session_type"),
              startsAt = AdminJsonHelper.str(o, "starts_at"),
              status = AdminJsonHelper.str(o, "status").ifBlank { "absent" }.lowercase(),
              excuseReason = AdminJsonHelper.str(o, "excuse_reason")
            )
          )
        }
      }
    }
  }
}
