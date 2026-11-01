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

class AdminService(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  private suspend fun postRpc(
    rpcName: String,
    body: JSONObject,
    accessToken: String
  ): Result<String> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val url = "${SupabaseConfig.url}/rest/v1/rpc/$rpcName"
    val req = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(body.toString().toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val bodyStr = resp.body?.string().orEmpty()
        if (resp.isSuccessful) {
          Result.success(bodyStr)
        } else {
          Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyStr, resp.code)))
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Network error"))
    }
  }

  suspend fun listUsers(
    searchText: String,
    maxRows: Int = 20,
    accessToken: String
  ): Result<List<AdminUserItem>> {
    val payload = JSONObject().apply {
      put("search_text", searchText)
      put("max_rows", maxRows)
    }
    val result = postRpc("list_users", payload, accessToken)
    return result.mapCatching { body ->
      val arr = JSONArray(body)
      val list = mutableListOf<AdminUserItem>()
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val id = obj.optString("id").ifEmpty { obj.optString("user_id") }
        val fullName = obj.optString("full_name").ifEmpty { obj.optString("name") }
        val email = obj.optString("email")
        val admissionNumber = obj.optString("admission_number")
        val role = obj.optString("role").ifBlank { "student" }
        val verified = obj.optBoolean("verified", false)
        list.add(
          AdminUserItem(
            id = id,
            fullName = fullName,
            email = email,
            admissionNumber = admissionNumber,
            role = role,
            isVerified = verified
          )
        )
      }
      list
    }
  }

  suspend fun assignRole(
    targetUserId: String,
    newRole: String,
    accessToken: String
  ): Result<Unit> {
    val payload = JSONObject().apply {
      put("target_user_id", targetUserId)
      put("new_role", newRole)
    }
    val result = postRpc("admin_assign_role", payload, accessToken)
    return result.map { }
  }

  suspend fun setLecturerVerified(
    targetUserId: String,
    isVerified: Boolean,
    accessToken: String
  ): Result<Unit> {
    val payload = JSONObject().apply {
      put("target_user_id", targetUserId)
      put("is_verified", isVerified)
    }
    val result = postRpc("admin_set_lecturer_verified", payload, accessToken)
    return result.map { }
  }
}
