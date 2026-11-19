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

data class PendingLecturerItem(
  val userId: String,
  val fullName: String,
  val email: String,
  val staffNumber: String = ""
)

sealed class BecomeLecturerResult {
  object Success : BecomeLecturerResult()
  object WrongPasscode : BecomeLecturerResult()
  data class Error(val message: String) : BecomeLecturerResult()
}

class LecturerService(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()
) {

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun becomeLecturer(
    passcode: String,
    accessToken: String
  ): BecomeLecturerResult = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext BecomeLecturerResult.Error(SupabaseConfig.MISSING_CONFIG_ERROR)
    }

    val body = JSONObject().apply {
      put("passcode", passcode.trim())
    }

    val url = "${SupabaseConfig.url}/rest/v1/rpc/become_lecturer"
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
        parseBecomeLecturerResponse(response.code, bodyString)
      }
    } catch (e: Exception) {
      BecomeLecturerResult.Error(SupabaseConfig.formatExceptionMessage(e, "Connection failed"))
    }
  }

  companion object {
    fun parseBecomeLecturerResponse(statusCode: Int, rawBody: String): BecomeLecturerResult {
      val trimmed = rawBody.trim()
      if (statusCode in 200..299) {
        val unquoted = trimmed.removeSurrounding("\"").removeSurrounding("'").trim().lowercase()
        if (unquoted == "false" || unquoted == "0" || unquoted.contains("wrong") || unquoted.contains("invalid passcode")) {
          return BecomeLecturerResult.WrongPasscode
        }
        if (trimmed.startsWith("{")) {
          val obj = runCatching { JSONObject(trimmed) }.getOrNull()
          if (obj != null) {
            if (obj.has("success") && !obj.optBoolean("success", true)) return BecomeLecturerResult.WrongPasscode
            if (obj.has("ok") && !obj.optBoolean("ok", true)) return BecomeLecturerResult.WrongPasscode
            if (obj.optString("role").equals("student", ignoreCase = true)) return BecomeLecturerResult.WrongPasscode
            val err = obj.optString("error").ifBlank { obj.optString("message") }
            if (err.isNotBlank() && (err.contains("passcode", ignoreCase = true) || err.contains("wrong", ignoreCase = true))) {
              return BecomeLecturerResult.WrongPasscode
            }
          }
        }
        return BecomeLecturerResult.Success
      }
      val errMsg = SupabaseConfig.extractErrorMessage(trimmed, statusCode)
      val mentionsPasscode = errMsg.contains("passcode", ignoreCase = true)
      val indicatesWrong = errMsg.contains("invalid", ignoreCase = true) ||
        errMsg.contains("wrong", ignoreCase = true) ||
        errMsg.contains("incorrect", ignoreCase = true) ||
        statusCode == 401 || statusCode == 403
      return if (mentionsPasscode && indicatesWrong) {
        BecomeLecturerResult.WrongPasscode
      } else {
        BecomeLecturerResult.Error(errMsg)
      }
    }
  }

  suspend fun checkClassMemberStatus(
    classId: String,
    userId: String,
    accessToken: String
  ): Result<String> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val url = "${SupabaseConfig.url}/rest/v1/class_members?select=status&class_id=eq.$classId&user_id=eq.$userId"
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
        if (array.length() > 0) {
          val status = array.getJSONObject(0).optString("status", "active")
          Result.success(status.ifBlank { "active" })
        } else {
          Result.success("active")
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to check member status"))
    }
  }

  suspend fun listPendingLecturers(
    classId: String,
    accessToken: String
  ): Result<List<PendingLecturerItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val body = JSONObject().apply {
      put("target_class_id", classId)
    }

    val url = "${SupabaseConfig.url}/rest/v1/rpc/list_pending_lecturers"
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
        val list = mutableListOf<PendingLecturerItem>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val uid = obj.optString("user_id").ifBlank { obj.optString("id") }
          val name = obj.optString("full_name").ifBlank { obj.optString("name", "Lecturer") }
          val email = obj.optString("email").ifBlank { "No email" }
          val staffNo = obj.optString("staff_number").ifBlank {
            obj.optString("admission_number", obj.optString("staff_no", ""))
          }
          list.add(
            PendingLecturerItem(
              userId = uid,
              fullName = name,
              email = email,
              staffNumber = staffNo
            )
          )
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to load pending lecturers"))
    }
  }

  suspend fun approvePendingLecturer(
    classId: String,
    userId: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val url = "${SupabaseConfig.url}/rest/v1/class_members?class_id=eq.$classId&user_id=eq.$userId&status=eq.pending"
    val body = JSONObject().apply {
      put("status", "active")
    }

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
      Result.failure(Exception(e.localizedMessage ?: "Failed to approve lecturer"))
    }
  }

  suspend fun rejectPendingLecturer(
    classId: String,
    userId: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val url = "${SupabaseConfig.url}/rest/v1/class_members?class_id=eq.$classId&user_id=eq.$userId&status=eq.pending"
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
      Result.failure(Exception(e.localizedMessage ?: "Failed to reject lecturer request"))
    }
  }
}
