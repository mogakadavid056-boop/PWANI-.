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

class MembersService(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()
) {

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun listClassMembers(
    classId: String,
    accessToken: String
  ): Result<List<ClassMemberItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val body = JSONObject().apply {
      put("target_class_id", classId)
    }

    val url = "${SupabaseConfig.url}/rest/v1/rpc/list_class_members"
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
        val list = mutableListOf<ClassMemberItem>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val userId = obj.optString("user_id").ifBlank {
            obj.optString("id", obj.optString("student_id", obj.optString("target_user_id")))
          }
          val fullName = obj.optString("full_name").ifBlank {
            obj.optString("name", obj.optString("student_name", "Student"))
          }
          val admNo = obj.optString("admission_number").ifBlank {
            obj.optString("admission_no", obj.optString("reg_no", "N/A"))
          }
          val isRep = obj.optBoolean("is_rep", obj.optBoolean("is_representative", obj.optBoolean("rep", false))) ||
            obj.optString("role").equals("rep", ignoreCase = true) ||
            obj.optString("role").equals("representative", ignoreCase = true)

          list.add(
            ClassMemberItem(
              userId = userId,
              fullName = fullName,
              admissionNumber = admNo,
              isRep = isRep,
              role = if (obj.has("role")) obj.optString("role") else null
            )
          )
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to load class members"))
    }
  }

  suspend fun appointClassRep(
    classId: String,
    targetUserId: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val body = JSONObject().apply {
      put("target_class_id", classId)
      put("target_user_id", targetUserId)
    }

    val url = "${SupabaseConfig.url}/rest/v1/rpc/appoint_class_rep"
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
      Result.failure(Exception(e.localizedMessage ?: "Failed to appoint class representative"))
    }
  }

  suspend fun removeClassRep(
    classId: String,
    targetUserId: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val body = JSONObject().apply {
      put("target_class_id", classId)
      put("target_user_id", targetUserId)
    }

    val url = "${SupabaseConfig.url}/rest/v1/rpc/remove_class_rep"
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
      Result.failure(Exception(e.localizedMessage ?: "Failed to remove class representative"))
    }
  }

  suspend fun removeFromClass(
    classId: String,
    targetUserId: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val url = "${SupabaseConfig.url}/rest/v1/class_members?class_id=eq.$classId&user_id=eq.$targetUserId"
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
      Result.failure(Exception(e.localizedMessage ?: "Failed to remove member from class"))
    }
  }
}
