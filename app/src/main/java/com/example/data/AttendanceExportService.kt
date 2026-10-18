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
        val name = obj.optString("name", "Class")
        val year = if (obj.has("year_of_study") && !obj.isNull("year_of_study")) obj.optInt("year_of_study") else null
        val uniObj = obj.optJSONObject("universities")
        val schoolObj = obj.optJSONObject("schools")
        val progObj = obj.optJSONObject("programmes")

        Result.success(
          ClassPdfMetadata(
            className = name,
            yearOfStudy = year,
            universityName = uniObj?.optString("name")?.ifBlank { null },
            schoolName = schoolObj?.optString("name")?.ifBlank { null },
            programmeName = progObj?.optString("name")?.ifBlank { null }
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
              val name = obj.optString("full_name").ifBlank { obj.optString("name", "Student") }
              val adm = obj.optString("admission_number").ifBlank { obj.optString("admission_no", "N/A") }
              val attended = obj.optInt("attended", obj.optInt("attended_count", obj.optInt("present_count", 0)))
              val total = obj.optInt("total", obj.optInt("total_sessions", obj.optInt("total_count", 0)))
              val pct = when {
                obj.has("percentage") && !obj.isNull("percentage") -> obj.optDouble("percentage")
                obj.has("attendance_percentage") -> obj.optDouble("attendance_percentage")
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
    if (firstAttempt.isSuccess) {
      firstAttempt
    } else {
      executeRpc("class_id")
    }
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
                val title = obj.optString("session_title").ifBlank {
                  obj.optString("title", obj.optString("session_name", "Session ${i + 1}"))
                }
                val rawDate = obj.optString("date").ifBlank {
                  obj.optString("starts_at", obj.optString("created_at", ""))
                }
                val displayDate = rawDate.replace("T", " ").substringBefore(".")
                val statusStr = obj.optString("status")
                val isPresent = statusStr.equals("Present", ignoreCase = true) ||
                  obj.optBoolean("present", obj.optBoolean("is_present", false)) ||
                  obj.optBoolean("attended", false)

                if (isPresent) attendedCount++
                records.add(MyAttendanceRecordItem(title, displayDate, isPresent))
              }
            } else if (bodyString.trim().startsWith("{")) {
              val root = JSONObject(bodyString)
              studentName = root.optString("student_name", studentName)
              admNo = root.optString("admission_number", admNo)
              className = root.optString("class_name", className)
              if (root.has("percentage")) overallPercentage = root.optDouble("percentage")
              attendedCount = root.optInt("attended", 0)
              totalCount = root.optInt("total", 0)
              val arr = root.optJSONArray("records") ?: root.optJSONArray("sessions") ?: JSONArray()
              for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val title = obj.optString("session_title", "Session ${i + 1}")
                val date = obj.optString("date", "").replace("T", " ").substringBefore(".")
                val isPresent = obj.optString("status").equals("Present", ignoreCase = true) ||
                  obj.optBoolean("present", false)
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
