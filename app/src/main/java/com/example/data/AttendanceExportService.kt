package com.example.data

import com.example.util.JsonHelper
import com.example.util.NairobiTimeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AttendanceExportService(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun fetchClassMetadata(
    classId: String,
    accessToken: String
  ): Result<ClassPdfMetadata> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val url = "${SupabaseConfig.url}/rest/v1/classes?id=eq.$classId&select=name,year_of_study,universities(name),schools(name),programmes(name)"
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
        if (array.length() == 0) {
          return@withContext Result.success(ClassPdfMetadata(className = "Class"))
        }
        val obj = array.getJSONObject(0)
        val name = JsonHelper.str(obj, "name").ifBlank { "Class" }
        val year = if (obj.has("year_of_study") && !obj.isNull("year_of_study")) obj.optInt("year_of_study") else null
        val uniObj = obj.optJSONObject("universities")
        val schoolObj = obj.optJSONObject("schools")
        val progObj = obj.optJSONObject("programmes")

        Result.success(
          ClassPdfMetadata(
            className = name,
            yearOfStudy = year,
            universityName = JsonHelper.strOrNull(uniObj, "name"),
            schoolName = JsonHelper.strOrNull(schoolObj, "name"),
            programmeName = JsonHelper.strOrNull(progObj, "name")
          )
        )
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to fetch class details"))
    }
  }

  suspend fun fetchClassAttendanceSummary(
    classId: String,
    accessToken: String
  ): Result<List<StudentAttendanceSummaryItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    suspend fun executeRpc(argKey: String): Result<List<StudentAttendanceSummaryItem>> {
      val body = JSONObject().apply { put(argKey, classId) }
      val url = "${SupabaseConfig.url}/rest/v1/rpc/get_class_attendance_summary"
      val request = Request.Builder()
        .url(url)
        .addHeader("apikey", SupabaseConfig.anonKey)
        .addHeader("Authorization", "Bearer $accessToken")
        .addHeader("Content-Type", "application/json")
        .post(body.toString().toRequestBody(jsonMediaType))
        .build()

      return try {
        client.newCall(request).execute().use { response ->
          val bodyString = response.body?.string().orEmpty()
          if (!response.isSuccessful) {
            Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
          } else {
            val array = JSONArray(bodyString)
            val list = mutableListOf<StudentAttendanceSummaryItem>()
            for (i in 0 until array.length()) {
              val obj = array.getJSONObject(i)
              val name = JsonHelper.firstStr(obj, "full_name", "name").ifBlank { "Student" }
              val adm = JsonHelper.firstStr(obj, "admission_number", "admission_no").ifBlank { "-" }
              val attended = JsonHelper.intVal(
                obj, "attended",
                JsonHelper.intVal(obj, "attended_count", JsonHelper.intVal(obj, "present_count", 0))
              )
              val total = JsonHelper.intVal(
                obj, "total",
                JsonHelper.intVal(obj, "total_sessions", JsonHelper.intVal(obj, "total_count", 0))
              )
              val pct = when {
                obj.has("percentage") && !obj.isNull("percentage") -> obj.optDouble("percentage")
                obj.has("attendance_percentage") && !obj.isNull("attendance_percentage") -> obj.optDouble("attendance_percentage")
                total > 0 -> (attended.toDouble() / total) * 100.0
                else -> 0.0
              }
              list.add(
                StudentAttendanceSummaryItem(
                  fullName = name,
                  admissionNumber = adm,
                  attended = attended,
                  total = total,
                  percentage = pct
                )
              )
            }
            Result.success(list)
          }
        }
      } catch (e: Exception) {
        Result.failure(Exception(e.localizedMessage ?: "Failed to load summary"))
      }
    }

    val firstAttempt = executeRpc("target_class_id")
    if (firstAttempt.isSuccess) firstAttempt else executeRpc("class_id")
  }

  suspend fun fetchMyAttendance(
    classId: String,
    session: UserSession
  ): Result<MyAttendanceSummaryData> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    suspend fun executeRpc(bodyJson: JSONObject): Result<MyAttendanceSummaryData> {
      val url = "${SupabaseConfig.url}/rest/v1/rpc/get_my_attendance"
      val request = Request.Builder()
        .url(url)
        .addHeader("apikey", SupabaseConfig.anonKey)
        .addHeader("Authorization", "Bearer ${session.accessToken}")
        .addHeader("Content-Type", "application/json")
        .post(bodyJson.toString().toRequestBody(jsonMediaType))
        .build()

      return try {
        client.newCall(request).execute().use { response ->
          val bodyString = response.body?.string().orEmpty()
          if (!response.isSuccessful) {
            Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
          } else {
            val records = mutableListOf<MyAttendanceRecordItem>()
            var attendedCount = 0
            var totalCount = 0
            var studentName = session.fullName
            var admNo = session.admissionNumber
            var className = "Class"
            var overallPercentage: Double? = null

            if (bodyString.trim().startsWith("[")) {
              val array = JSONArray(bodyString)
              totalCount = array.length()
              for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val title = JsonHelper.firstStr(obj, "session_title", "title", "session_name")
                  .ifBlank { "Session ${i + 1}" }
                val displayDate = JsonHelper.str(obj, "date_text").ifBlank {
                  val rawDate = JsonHelper.firstStr(obj, "date", "starts_at", "created_at")
                  if (rawDate.isBlank()) "-" else NairobiTimeHelper.formatIsoToNairobi(rawDate)
                }
                val statusStr = JsonHelper.str(obj, "status")
                val isPresent = statusStr.equals("Present", ignoreCase = true) ||
                  JsonHelper.bool(obj, "present", JsonHelper.bool(obj, "is_present", false)) ||
                  JsonHelper.bool(obj, "attended", false)

                if (isPresent) attendedCount++
                records.add(MyAttendanceRecordItem(title, displayDate, isPresent))
              }
            } else if (bodyString.trim().startsWith("{")) {
              val root = JSONObject(bodyString)
              studentName = JsonHelper.str(root, "student_name").ifBlank { studentName }
              admNo = JsonHelper.str(root, "admission_number").ifBlank { admNo }
              className = JsonHelper.str(root, "class_name").ifBlank { className }
              if (root.has("percentage") && !root.isNull("percentage")) overallPercentage = root.optDouble("percentage")
              attendedCount = JsonHelper.intVal(root, "attended", 0)
              totalCount = JsonHelper.intVal(root, "total", 0)
              val arr = root.optJSONArray("records") ?: root.optJSONArray("sessions") ?: JSONArray()
              for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val title = JsonHelper.firstStr(obj, "session_title", "title").ifBlank { "Session ${i + 1}" }
                val date = JsonHelper.str(obj, "date_text").ifBlank {
                  val raw = JsonHelper.firstStr(obj, "date", "starts_at")
                  if (raw.isBlank()) "-" else NairobiTimeHelper.formatIsoToNairobi(raw)
                }
                val isPresent = JsonHelper.str(obj, "status").equals("Present", ignoreCase = true) ||
                  JsonHelper.bool(obj, "present", false)
                records.add(MyAttendanceRecordItem(title, date, isPresent))
              }
            }

            val finalPct = overallPercentage ?: if (totalCount > 0) (attendedCount.toDouble() / totalCount) * 100.0 else 0.0
            Result.success(
              MyAttendanceSummaryData(
                studentName = studentName,
                admissionNumber = admNo,
                className = className,
                attended = attendedCount,
                total = totalCount,
                percentage = finalPct,
                records = records
              )
            )
          }
        }
      } catch (e: Exception) {
        Result.failure(Exception(e.localizedMessage ?: "Failed to load attendance report"))
      }
    }

    val first = executeRpc(JSONObject().apply { put("target_class_id", classId) })
    if (first.isSuccess) first else executeRpc(JSONObject())
  }
}
