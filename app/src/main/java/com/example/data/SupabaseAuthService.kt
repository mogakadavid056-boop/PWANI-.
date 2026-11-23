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

data class UserRoleInfo(
  val role: String = RoleHelper.ROLE_STUDENT,
  val isVerified: Boolean = false
)

class SupabaseAuthService(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()
) {

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  data class AuthResult(
    val userId: String,
    val email: String,
    val fullName: String,
    val admissionNumber: String = "",
    val accessToken: String,
    val refreshToken: String?,
    val hasSession: Boolean = true
  )

  companion object {
    fun buildSignUpMetadata(
      fullName: String,
      admissionNumber: String? = null
    ): JSONObject = JSONObject().apply {
      put("full_name", fullName.trim())
      if (!admissionNumber.isNullOrBlank()) {
        put("admission_number", admissionNumber.trim())
      }
    }

    fun parseUserRoleInfo(body: String): UserRoleInfo {
      return try {
        val array = JSONArray(body)
        if (array.length() == 0) {
          return UserRoleInfo(role = RoleHelper.ROLE_STUDENT, isVerified = false)
        }
        val first = array.getJSONObject(0)
        val role = RoleHelper.normalizeRole(JsonHelper.str(first, "role"))
          .ifBlank { RoleHelper.ROLE_STUDENT }
        val isVerified = if (first.has("verified") && !first.isNull("verified")) {
          first.optBoolean("verified", false)
        } else {
          false
        }
        UserRoleInfo(role = role, isVerified = isVerified)
      } catch (_: Exception) {
        UserRoleInfo(role = RoleHelper.ROLE_STUDENT, isVerified = false)
      }
    }
  }

  suspend fun signUp(
    fullName: String,
    email: String,
    password: String,
    admissionNumber: String? = null
  ): Result<AuthResult> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val metadata = buildSignUpMetadata(fullName, admissionNumber)
    val requestBody = JSONObject().apply {
      put("email", email)
      put("password", password)
      put("data", metadata)
    }

    val url = "${SupabaseConfig.url}/auth/v1/signup"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
      .addHeader("Content-Type", "application/json")
      .post(requestBody.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val bodyString = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
        }

        val json = JSONObject(bodyString)
        val userObj = json.optJSONObject("user") ?: json
        val userId = JsonHelper.str(userObj, "id")
        val userEmail = JsonHelper.str(userObj, "email").ifBlank { email }
        val metaObj = userObj.optJSONObject("user_metadata")
        val parsedName = JsonHelper.str(metaObj, "full_name").ifBlank { fullName }
        val parsedAdm = JsonHelper.str(metaObj, "admission_number").ifBlank { admissionNumber.orEmpty() }
        val token = JsonHelper.str(json, "access_token")
        val hasSession = token.isNotBlank() && token != SupabaseConfig.anonKey
        val refreshToken = JsonHelper.strOrNull(json, "refresh_token")

        Result.success(
          AuthResult(
            userId = userId,
            email = userEmail,
            fullName = parsedName,
            admissionNumber = parsedAdm,
            accessToken = if (hasSession) token else "",
            refreshToken = refreshToken,
            hasSession = hasSession
          )
        )
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Registration failed")))
    }
  }

  suspend fun login(
    email: String,
    password: String
  ): Result<AuthResult> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val requestBody = JSONObject().apply {
      put("email", email)
      put("password", password)
    }

    val url = "${SupabaseConfig.url}/auth/v1/token?grant_type=password"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
      .addHeader("Content-Type", "application/json")
      .post(requestBody.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val bodyString = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
        }

        val json = JSONObject(bodyString)
        val accessToken = JsonHelper.str(json, "access_token")
        val refreshToken = JsonHelper.strOrNull(json, "refresh_token")
        val userObj = json.optJSONObject("user")
        val userId = JsonHelper.str(userObj, "id").ifBlank { JsonHelper.str(json, "id") }
        val userEmail = JsonHelper.str(userObj, "email").ifBlank { email }
        val userMeta = userObj?.optJSONObject("user_metadata")
        val fullName = JsonHelper.str(userMeta, "full_name")
        val admissionNumber = JsonHelper.str(userMeta, "admission_number")

        if (userId.isBlank() || accessToken.isBlank()) {
          return@withContext Result.failure(Exception("Authentication response missing session token."))
        }

        Result.success(
          AuthResult(
            userId = userId,
            email = userEmail,
            fullName = fullName,
            admissionNumber = admissionNumber,
            accessToken = accessToken,
            refreshToken = refreshToken,
            hasSession = true
          )
        )
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Network connection failed")))
    }
  }
  suspend fun patchProfile(
    userId: String,
    accessToken: String,
    payload: JSONObject
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured() || userId.isBlank() || accessToken.isBlank()) {
      return@withContext Result.failure(IllegalStateException("Missing session for profile update"))
    }
    val upsertBody = JSONObject(payload.toString()).apply { put("id", userId) }
    val upsertUrl = "${SupabaseConfig.url}/rest/v1/profiles?on_conflict=id"
    val upsertRequest = Request.Builder()
      .url(upsertUrl)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
      .post(upsertBody.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(upsertRequest).execute().use { response ->
        if (response.isSuccessful) {
          return@withContext Result.success(Unit)
        }
      }
      val patchUrl = "${SupabaseConfig.url}/rest/v1/profiles?id=eq.$userId"
      val patchRequest = Request.Builder()
        .url(patchUrl)
        .addHeader("apikey", SupabaseConfig.anonKey)
        .addHeader("Authorization", "Bearer $accessToken")
        .addHeader("Content-Type", "application/json")
        .addHeader("Prefer", "return=minimal")
        .patch(payload.toString().toRequestBody(jsonMediaType))
        .build()

      client.newCall(patchRequest).execute().use { response ->
        if (response.isSuccessful) {
          Result.success(Unit)
        } else {
          val body = response.body?.string().orEmpty()
          Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, response.code)))
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to save profile")))
    }
  }
  suspend fun fetchUserRole(
    userId: String,
    accessToken: String
  ): Result<UserRoleInfo> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    if (userId.isBlank() || accessToken.isBlank()) {
      return@withContext Result.failure(IllegalStateException("Authentication session is missing."))
    }
    val url = "${SupabaseConfig.url}/rest/v1/user_roles?select=role,verified&user_id=eq.$userId"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .addHeader("Cache-Control", "no-cache")
      .get()
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, response.code)))
        }
        Result.success(parseUserRoleInfo(body))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to load user role")))
    }
  }
  suspend fun resendSignUpEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val payload = JSONObject().apply {
      put("type", "signup")
      put("email", email.trim().lowercase())
    }
    val req = Request.Builder()
      .url("${SupabaseConfig.url}/auth/v1/resend")
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer ${SupabaseConfig.anonKey}")
      .addHeader("Content-Type", "application/json")
      .post(payload.toString().toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        if (resp.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(SupabaseConfig.extractErrorMessage(resp.body?.string().orEmpty(), resp.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to resend confirmation email")))
    }
  }

  suspend fun fetchBasicProfile(userId: String, accessToken: String): Pair<String, String> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured() || userId.isBlank() || accessToken.isBlank()) return@withContext "" to ""
    val req = Request.Builder()
      .url("${SupabaseConfig.url}/rest/v1/profiles?id=eq.$userId&select=full_name,admission_number")
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Accept", "application/json")
      .get().build()
    try {
      client.newCall(req).execute().use { response ->
        if (!response.isSuccessful) return@withContext "" to ""
        val arr = JSONArray(response.body?.string().orEmpty())
        if (arr.length() == 0) return@withContext "" to ""
        val obj = arr.getJSONObject(0)
        JsonHelper.str(obj, "full_name") to JsonHelper.str(obj, "admission_number")
      }
    } catch (_: Exception) { "" to "" }
  }
}
