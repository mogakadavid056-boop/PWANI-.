package com.example.admin.data

import com.example.admin.util.AdminJsonHelper
import com.example.admin.util.AdminRoleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class AdminAuthService(
  private val client: OkHttpClient = SupabaseAdminConfig.sharedHttpClient
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  data class AuthPayload(
    val userId: String,
    val email: String,
    val fullName: String,
    val accessToken: String,
    val refreshToken: String?
  )

  suspend fun signIn(email: String, password: String): Result<AuthPayload> = withContext(Dispatchers.IO) {
    if (!SupabaseAdminConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseAdminConfig.MISSING_CONFIG_ERROR))
    }
    val body = JSONObject().apply {
      put("email", email.trim())
      put("password", password)
    }
    val req = Request.Builder()
      .url("${SupabaseAdminConfig.url}/auth/v1/token?grant_type=password")
      .addHeader("apikey", SupabaseAdminConfig.anonKey)
      .addHeader("Authorization", "Bearer ${SupabaseAdminConfig.anonKey}")
      .addHeader("Content-Type", "application/json")
      .post(body.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(req).execute().use { resp ->
        val raw = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(raw, resp.code)))
        }
        val json = JSONObject(raw)
        val token = AdminJsonHelper.str(json, "access_token")
        val refresh = AdminJsonHelper.strOrNull(json, "refresh_token")
        val userObj = json.optJSONObject("user")
        val uid = AdminJsonHelper.str(userObj, "id").ifBlank { AdminJsonHelper.str(json, "id") }
        val userEmail = AdminJsonHelper.str(userObj, "email").ifBlank { email.trim() }
        val fullName = AdminJsonHelper.str(userObj?.optJSONObject("user_metadata"), "full_name")
        if (uid.isBlank() || token.isBlank()) {
          return@withContext Result.failure(Exception("Authentication response missing session token."))
        }
        Result.success(AuthPayload(uid, userEmail, fullName, token, refresh))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, "Sign in failed")))
    }
  }

  suspend fun refreshSession(refreshToken: String): Result<AuthPayload> = withContext(Dispatchers.IO) {
    if (!SupabaseAdminConfig.isConfigured() || refreshToken.isBlank()) {
      return@withContext Result.failure(IllegalStateException("Missing refresh token"))
    }
    val body = JSONObject().apply { put("refresh_token", refreshToken) }
    val req = Request.Builder()
      .url("${SupabaseAdminConfig.url}/auth/v1/token?grant_type=refresh_token")
      .addHeader("apikey", SupabaseAdminConfig.anonKey)
      .addHeader("Authorization", "Bearer ${SupabaseAdminConfig.anonKey}")
      .addHeader("Content-Type", "application/json")
      .post(body.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(req).execute().use { resp ->
        val raw = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(raw, resp.code)))
        }
        val json = JSONObject(raw)
        val token = AdminJsonHelper.str(json, "access_token")
        val nextRefresh = AdminJsonHelper.strOrNull(json, "refresh_token") ?: refreshToken
        val userObj = json.optJSONObject("user")
        val uid = AdminJsonHelper.str(userObj, "id")
        val userEmail = AdminJsonHelper.str(userObj, "email")
        val fullName = AdminJsonHelper.str(userObj?.optJSONObject("user_metadata"), "full_name")
        if (uid.isBlank() || token.isBlank()) {
          return@withContext Result.failure(Exception("Session refresh incomplete"))
        }
        Result.success(AuthPayload(uid, userEmail, fullName, token, nextRefresh))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, "Session refresh failed")))
    }
  }

  suspend fun fetchUserRole(userId: String, accessToken: String): Result<String> = withContext(Dispatchers.IO) {
    if (!SupabaseAdminConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseAdminConfig.MISSING_CONFIG_ERROR))
    }
    val req = Request.Builder()
      .url("${SupabaseAdminConfig.url}/rest/v1/user_roles?select=role,verified&user_id=eq.$userId")
      .addHeader("apikey", SupabaseAdminConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Accept", "application/json")
      .addHeader("Cache-Control", "no-cache")
      .get()
      .build()

    try {
      client.newCall(req).execute().use { resp ->
        val raw = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(raw, resp.code)))
        }
        Result.success(parseRole(raw))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, "Failed to verify role")))
    }
  }

  suspend fun fetchDisplayName(userId: String, accessToken: String): String = withContext(Dispatchers.IO) {
    val req = Request.Builder()
      .url("${SupabaseAdminConfig.url}/rest/v1/profiles?id=eq.$userId&select=full_name")
      .addHeader("apikey", SupabaseAdminConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Accept", "application/json")
      .get()
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) return@withContext ""
        val arr = JSONArray(resp.body?.string().orEmpty())
        if (arr.length() == 0) "" else AdminJsonHelper.str(arr.getJSONObject(0), "full_name")
      }
    } catch (_: Exception) {
      ""
    }
  }

  companion object {
    fun parseRole(body: String): String {
      return try {
        val arr = JSONArray(body)
        if (arr.length() == 0) return AdminRoleHelper.ROLE_STUDENT
        AdminRoleHelper.normalizeRole(AdminJsonHelper.str(arr.getJSONObject(0), "role"))
          .ifBlank { AdminRoleHelper.ROLE_STUDENT }
      } catch (_: Exception) {
        AdminRoleHelper.ROLE_STUDENT
      }
    }
  }
}
