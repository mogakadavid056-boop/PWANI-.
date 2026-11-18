package com.example.data

import com.example.util.JsonHelper
import com.example.util.RoleHelper
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
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Network error")))
    }
  }

  private fun parseUserObject(obj: JSONObject): AdminUserItem {
    val id = JsonHelper.firstStr(obj, "id", "user_id")
    val fullName = JsonHelper.firstStr(obj, "full_name", "name")
    val email = JsonHelper.str(obj, "email")
    val admissionNumber = JsonHelper.str(obj, "admission_number")
    val role = RoleHelper.normalizeRole(JsonHelper.str(obj, "role"))
      .ifBlank { RoleHelper.ROLE_STUDENT }
    val verified = JsonHelper.bool(obj, "verified", false)
    return AdminUserItem(
      id = id,
      fullName = fullName,
      email = email,
      admissionNumber = admissionNumber,
      role = role,
      isVerified = verified
    )
  }

  suspend fun listUsers(
    searchText: String,
    maxRows: Int = 50,
    accessToken: String
  ): Result<List<AdminUserItem>> {
    val payload = JSONObject().apply {
      put("search_text", searchText)
      put("max_rows", maxRows)
    }
    return postRpc("list_users", payload, accessToken).mapCatching { body ->
      val arr = JSONArray(body)
      buildList {
        for (i in 0 until arr.length()) {
          add(parseUserObject(arr.getJSONObject(i)))
        }
      }
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
    return postRpc("admin_assign_role", payload, accessToken).map { }
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
    return postRpc("admin_set_lecturer_verified", payload, accessToken).map { }
  }
}
