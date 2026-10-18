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
    val accessToken: String,
    val refreshToken: String?
  )

  suspend fun signUp(
    fullName: String,
    admissionNumber: String,
    email: String,
    password: String
  ): Result<AuthResult> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val requestBody = JSONObject().apply {
      put("email", email)
      put("password", password)
      put("data", JSONObject().apply { put("full_name", fullName) })
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
          return@withContext Result.failure(Exception(extractErrorMessage(bodyString, response.code)))
        }

        val json = JSONObject(bodyString)
        val userObj = json.optJSONObject("user") ?: json
        val userId = userObj.optString("id")
        val userEmail = userObj.optString("email", email)
        val metaObj = userObj.optJSONObject("user_metadata")
        val parsedName = metaObj?.optString("full_name")?.ifBlank { fullName } ?: fullName
        val accessToken = json.optString("access_token").ifBlank { SupabaseConfig.anonKey }
        val refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }

        Result.success(
          AuthResult(
            userId = userId,
            email = userEmail,
            fullName = parsedName,
            accessToken = accessToken,
            refreshToken = refreshToken
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
          return@withContext Result.failure(Exception(extractErrorMessage(bodyString, response.code)))
        }

        val json = JSONObject(bodyString)
        val accessToken = json.optString("access_token")
        val refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
        val userObj = json.optJSONObject("user")
        val userId = userObj?.optString("id") ?: json.optString("id")
        val userEmail = userObj?.optString("email") ?: email
        val userMeta = userObj?.optJSONObject("user_metadata")
        val fullName = userMeta?.optString("full_name").orEmpty()

        Result.success(
          AuthResult(
            userId = userId,
            email = userEmail,
            fullName = fullName,
            accessToken = accessToken,
            refreshToken = refreshToken
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
    if (!SupabaseConfig.isConfigured() || userId.isBlank()) {
      return@withContext Result.success(Unit)
    }

    val url = "${SupabaseConfig.url}/rest/v1/profiles?id=eq.$userId"
    val tokenHeader = if (accessToken.isNotBlank() && accessToken != SupabaseConfig.anonKey) accessToken else SupabaseConfig.anonKey

    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $tokenHeader")
      .addHeader("Content-Type", "application/json")
      .addHeader("Prefer", "return=minimal")
      .patch(payload.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          Result.success(Unit)
        } else {
          val body = response.body?.string().orEmpty()
          Result.failure(Exception(extractErrorMessage(body, response.code)))
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to save profile"))
    }
  }

  suspend fun patchAdmissionNumber(
    userId: String,
    accessToken: String,
    admissionNumber: String
  ): Result<Unit> = patchProfile(userId, accessToken, JSONObject().apply { put("admission_number", admissionNumber) })

  suspend fun fetchAdmissionNumber(
    userId: String,
    accessToken: String
  ): Result<String> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured() || userId.isBlank()) return@withContext Result.success("")

    val url = "${SupabaseConfig.url}/rest/v1/profiles?id=eq.$userId&select=admission_number"
    val tokenHeader = if (accessToken.isNotBlank() && accessToken != SupabaseConfig.anonKey) accessToken else SupabaseConfig.anonKey

    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $tokenHeader")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .get()
      .build()

    try {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return@withContext Result.success("")
        val body = response.body?.string().orEmpty()
        val array = JSONArray(body)
        if (array.length() > 0) {
          Result.success(array.getJSONObject(0).optString("admission_number", ""))
        } else {
          Result.success("")
        }
      }
    } catch (_: Exception) {
      Result.success("")
    }
  }

  suspend fun fetchUserRole(
    userId: String,
    accessToken: String
  ): Result<String> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured() || userId.isBlank()) return@withContext Result.success("Student")

    val url = "${SupabaseConfig.url}/rest/v1/user_roles?user_id=eq.$userId&select=*"
    val tokenHeader = if (accessToken.isNotBlank() && accessToken != SupabaseConfig.anonKey) accessToken else SupabaseConfig.anonKey

    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $tokenHeader")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .get()
      .build()

    try {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          val body = response.body?.string().orEmpty()
          return@withContext Result.failure(Exception(extractErrorMessage(body, response.code)))
        }

        val body = response.body?.string().orEmpty()
        val array = JSONArray(body)
        if (array.length() > 0) {
          val rawRole = array.getJSONObject(0).optString("role", "Student")
          Result.success(rawRole.ifBlank { "Student" })
        } else {
          Result.success("Student")
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to fetch user role"))
    }
  }

  private fun extractErrorMessage(responseBody: String, statusCode: Int): String {
    return SupabaseConfig.extractErrorMessage(responseBody, statusCode)
  }
}
