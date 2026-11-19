package com.example.admin.notifications

import android.content.Context
import com.example.admin.data.SupabaseAdminConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.coroutines.resume

object AdminPushTokenManager {
  const val APP_NAME_ADMIN = "admin"
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
  private val logoutScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  fun buildRegisterPayload(token: String): String = JSONObject().apply {
    put("device_token", token.trim())
    put("app_name", APP_NAME_ADMIN)
  }.toString()

  fun buildUnregisterPayload(token: String): String = JSONObject().apply {
    put("device_token", token.trim())
  }.toString()

  suspend fun fetchCurrentTokenSafely(context: Context): String? {
    val initialized = runCatching {
      FirebaseApp.getApps(context.applicationContext).isNotEmpty() ||
        FirebaseApp.initializeApp(context.applicationContext) != null
    }.getOrDefault(false)
    if (!initialized) return AdminNotificationPrefs.getFcmToken(context)

    val fetched = runCatching {
      suspendCancellableCoroutine<String?> { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
          if (!cont.isActive) return@addOnCompleteListener
          val token = if (task.isSuccessful) task.result?.trim()?.takeIf { it.isNotBlank() } else null
          cont.resume(token)
        }
      }
    }.getOrNull()

    return fetched ?: AdminNotificationPrefs.getFcmToken(context)
  }

  suspend fun syncCurrentToken(context: Context, accessToken: String): Result<Unit> {
    if (accessToken.isBlank()) return Result.success(Unit)
    val token = fetchCurrentTokenSafely(context) ?: return Result.success(Unit)
    return registerToken(context, accessToken, token)
  }

  suspend fun registerToken(
    context: Context,
    accessToken: String,
    fcmToken: String,
    client: OkHttpClient = SupabaseAdminConfig.sharedHttpClient
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val cleanToken = fcmToken.trim()
    if (cleanToken.isBlank() || accessToken.isBlank()) return@withContext Result.success(Unit)
    AdminNotificationPrefs.saveFcmToken(context, cleanToken)
    if (!SupabaseAdminConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseAdminConfig.MISSING_CONFIG_ERROR))
    }
    val req = Request.Builder()
      .url("${SupabaseAdminConfig.url}/rest/v1/rpc/register_device_token")
      .addHeader("apikey", SupabaseAdminConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(buildRegisterPayload(cleanToken).toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(body, resp.code)))
        } else {
          Result.success(Unit)
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, "Failed to register admin push token")))
    }
  }

  fun unregisterOnLogout(
    context: Context,
    accessToken: String,
    client: OkHttpClient = SupabaseAdminConfig.sharedHttpClient
  ) {
    val appCtx = context.applicationContext
    val savedToken = AdminNotificationPrefs.getFcmToken(appCtx)
    AdminNotificationPrefs.clearFcmToken(appCtx)

    logoutScope.launch {
      val activeToken = fetchCurrentTokenSafely(appCtx)?.takeIf { it.isNotBlank() } ?: savedToken
      if (accessToken.isNotBlank() && !activeToken.isNullOrBlank()) {
        unregisterTokenRpc(accessToken, activeToken, client)
      }
      runCatching {
        if (FirebaseApp.getApps(appCtx).isNotEmpty()) {
          FirebaseMessaging.getInstance().deleteToken()
        }
      }
    }
  }

  suspend fun unregisterTokenRpc(
    accessToken: String,
    fcmToken: String,
    client: OkHttpClient = SupabaseAdminConfig.sharedHttpClient
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val cleanToken = fcmToken.trim()
    if (cleanToken.isBlank() || accessToken.isBlank()) return@withContext Result.success(Unit)
    if (!SupabaseAdminConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseAdminConfig.MISSING_CONFIG_ERROR))
    }
    val req = Request.Builder()
      .url("${SupabaseAdminConfig.url}/rest/v1/rpc/unregister_device_token")
      .addHeader("apikey", SupabaseAdminConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(buildUnregisterPayload(cleanToken).toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(body, resp.code)))
        } else {
          Result.success(Unit)
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, "Failed to unregister admin push token")))
    }
  }
}
