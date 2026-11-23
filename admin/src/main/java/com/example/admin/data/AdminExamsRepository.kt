package com.example.admin.data

import com.example.admin.util.AdminJsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject

class AdminExamsRepository(
  private val client: OkHttpClient = SupabaseAdminConfig.sharedHttpClient
) {
  suspend fun listQuizzes(classId: String, accessToken: String): Result<List<AdminQuizSummary>> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("target_class_id", classId) }
      AdminHttpHelper.postRpc(client, "list_quizzes", body, accessToken).mapCatching { raw ->
        val arr = JSONArray(raw.ifBlank { "[]" })
        List(arr.length()) { i ->
          val o = arr.getJSONObject(i)
          val secs = o.optLong("time_limit_seconds", -1L).takeIf { it > 0L }
            ?: o.optInt("time_limit_minutes", -1).takeIf { it > 0 }?.toLong()?.times(60L)
          AdminQuizSummary(
            id = AdminJsonHelper.firstStr(o, "quiz_id", "id"),
            title = AdminJsonHelper.str(o, "title").ifBlank { "Assessment" },
            unitCode = AdminJsonHelper.str(o, "unit_code"),
            status = AdminJsonHelper.str(o, "status").ifBlank { "open" },
            isExam = AdminJsonHelper.bool(o, "is_exam", false),
            answerFormat = AdminJsonHelper.str(o, "answer_format").ifBlank { "choices" },
            questionCount = AdminJsonHelper.intVal(o, "question_count", 0),
            totalMarks = o.optDouble("total_marks", 0.0).takeIf { !it.isNaN() } ?: 0.0,
            attemptsCount = AdminJsonHelper.intVal(o, "attempts_count", 0),
            timeLimitSeconds = secs,
            oneWayNavigation = AdminJsonHelper.bool(o, "one_way_navigation", false),
            noCopyPaste = AdminJsonHelper.bool(o, "no_copy_paste", false),
            opensAt = AdminJsonHelper.str(o, "opens_at"),
            closesAt = AdminJsonHelper.str(o, "closes_at")
          )
        }
      }
    }

  suspend fun getQuizResults(quizId: String, accessToken: String): Result<List<AdminQuizResultRow>> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("target_quiz_id", quizId) }
      AdminHttpHelper.postRpc(client, "quiz_results", body, accessToken).mapCatching { raw ->
        val arr = JSONArray(raw.ifBlank { "[]" })
        List(arr.length()) { i ->
          val o = arr.getJSONObject(i)
          val rawScore = if (o.isNull("score")) null else o.optDouble("score", Double.NaN).takeIf { !it.isNaN() }
          val mcq = if (o.isNull("mcq_marks")) null else o.optDouble("mcq_marks", Double.NaN).takeIf { !it.isNaN() }
          val written = if (o.isNull("written_marks")) null else o.optDouble("written_marks", Double.NaN).takeIf { !it.isNaN() }
          AdminQuizResultRow(
            studentId = AdminJsonHelper.firstStr(o, "student_id", "user_id"),
            fullName = AdminJsonHelper.firstStr(o, "full_name", "name").ifBlank { "Student" },
            admissionNumber = AdminJsonHelper.firstStr(o, "admission_number", "admission_no").ifBlank { "-" },
            score = rawScore,
            total = o.optDouble("total", 0.0).takeIf { !it.isNaN() } ?: 0.0,
            submittedAt = AdminJsonHelper.str(o, "submitted_at"),
            markingStatus = AdminJsonHelper.str(o, "marking_status").ifBlank { "auto" },
            late = AdminJsonHelper.bool(o, "late", false),
            mcqMarks = mcq,
            writtenMarks = written,
            focusLossCount = AdminJsonHelper.intVal(o, "focus_loss_count", 0),
            autoSubmittedReason = AdminJsonHelper.str(o, "auto_submitted_reason")
          )
        }
      }
    }
}
