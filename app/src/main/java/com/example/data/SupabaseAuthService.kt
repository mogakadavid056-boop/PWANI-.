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

data class ProfileRoleInfo(
  val role: String,
  val roleStatus: String,
  val fullName: String = "",
  val admissionNumber: String = "",
  val staffNumber: String = "",
  val department: String = "",
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
    val accessToken: String,
    val refreshToken: String?,
    val hasSession: Boolean = true
  )

  companion object {
    fun buildSignUpMetadata(
      fullName: String,
      requestedRole: String,
      admissionNumber: String? = null,
      staffNumber: String? = null,
      department: String? = null
    ): JSONObject = JSONObject().apply {
      put("full_name", fullName.trim())
      put("requested_role", RoleHelper.normalizeRole(requestedRole))
      if (!admissionNumber.isNullOrBlank()) {
        put("admission_number", admissionNumber.trim())
      }
      if (!staffNumber.isNullOrBlank()) {
        put("staff_number", staffNumber.trim())
      }
      if (!department.isNullOrBlank()) {
        put("department", department.trim())
      }
    }

    fun parseProfileRoleInfo(body: String): Result<ProfileRoleInfo> {
      return try {
        val array = JSONArray(body)
        if (array.length() == 0) {
          return Result.failure(Exception("Account profile not found. Please contact an administrator."))
        }
        val first = array.getJSONObject(0)
        val role = RoleHelper.normalizeRole(JsonHelper.str(first, "role"))
        if (role.isBlank()) {
          return Result.failure(Exception("Your account role is not configured yet. Please contact an administrator."))
        }
        val rawStatus = JsonHelper.firstStr(first, "role_status", "status")
        val roleStatus = RoleHelper.normalizeRoleStatus(rawStatus).ifBlank { RoleHelper.STATUS_APPROVED }
        val isVerified = when {
          RoleHelper.isPendingApproval(roleStatus) -> false
          RoleHelper.isRejectedStatus(roleStatus) -> false
          first.has("verified") && !first.isNull("verified") -> first.optBoolean("verified", true)
          else -> roleStatus == RoleHelper.STATUS_APPROVED
        }
        Result.success(
          ProfileRoleInfo(
            role = role,
            roleStatus = roleStatus,
            fullName = JsonHelper.str(first, "full_name"),
            admissionNumber = JsonHelper.str(first, "admission_number"),
            staffNumber = JsonHelper.str(first, "staff_number"),
            department = JsonHelper.str(first, "department"),
            isVerified = isVerified
          )
        )
      } catch (e: Exception) {
        Result.failure(Exception("Invalid profile response from server."))
      }
    }
  }

  suspend fun signUp(
    fullName: String,
    email: String,
    password: String,
    requestedRole: String,
    admissionNumber: String? = null,
    staffNumber: String? = null,
    department: String? = null
  ): Result<AuthResult> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val metadata = buildSignUpMetadata(fullName, requestedRole, admissionNumber, staffNumber, department)
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
        val token = JsonHelper.str(json, "access_token")
        val hasSession = token.isNotBlank() && token != SupabaseConfig.anonKey
        val refreshToken = JsonHelper.strOrNull(json, "refresh_token")

        Result.success(
          AuthResult(
            userId = userId,
            email = userEmail,
            fullName = parsedName,
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

        if (userId.isBlank() || accessToken.isBlank()) {
          return@withContext Result.failure(Exception("Authentication response missing session token."))
        }

        Result.success(
          AuthResult(
            userId = userId,
            email = userEmail,
            fullName = fullName,
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
    val safePayload = JSONObject(payload.toString()).apply {
      remove("role")
      remove("role_status")
    }
    val url = "${SupabaseConfig.url}/rest/v1/profiles?id=eq.$userId"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Prefer", "return=minimal")
      .patch(safePayload.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
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

  suspend fun fetchProfileRoleInfo(
    userId: String,
    accessToken: String
  ): Result<ProfileRoleInfo> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    if (userId.isBlank() || accessToken.isBlank()) {
      return@withContext Result.failure(IllegalStateException("Authentication session is missing."))
    }

    val url = "${SupabaseConfig.url}/rest/v1/profiles?id=eq.$userId&select=*"
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
        parseProfileRoleInfo(body)
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to load user profile")))
    }
  }
}
