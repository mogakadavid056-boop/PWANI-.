package com.example.notifications

import android.content.Context
import com.example.data.OfflineCache
import com.example.data.SupabaseConfig
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

object PushTokenManager {
  const val APP_NAME_MAIN = "main"
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
  private val logoutScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  fun buildRegisterPayload(token: String): String = JSONObject().apply {
    put("device_token", token.trim())
    put("app_name", APP_NAME_MAIN)
  }.toString()

  fun buildUnregisterPayload(token: String): String = JSONObject().apply {
    put("device_token", token.trim())
  }.toString()

  suspend fun fetchCurrentTokenSafely(context: Context): String? {
    val initialized = runCatching {
      FirebaseApp.getApps(context.applicationContext).isNotEmpty() ||
        FirebaseApp.initializeApp(context.applicationContext) != null
    }.getOrDefault(false)
    if (!initialized) return NotificationPreferences.getFcmToken(context)

    val fetched = runCatching {
      suspendCancellableCoroutine<String?> { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
          if (!cont.isActive) return@addOnCompleteListener
          val token = if (task.isSuccessful) task.result?.trim()?.takeIf { it.isNotBlank() } else null
          cont.resume(token)
        }
      }
    }.getOrNull()

    return fetched ?: NotificationPreferences.getFcmToken(context)
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
    client: OkHttpClient = OfflineCache.getClient()
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val cleanToken = fcmToken.trim()
    if (cleanToken.isBlank() || accessToken.isBlank()) return@withContext Result.success(Unit)
    NotificationPreferences.saveFcmToken(context, cleanToken)
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val req = Request.Builder()
      .url("${SupabaseConfig.url}/rest/v1/rpc/register_device_token")
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(buildRegisterPayload(cleanToken).toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
        } else {
          Result.success(Unit)
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to register push token")))
    }
  }

  fun unregisterOnLogout(
    context: Context,
    accessToken: String,
    client: OkHttpClient = OfflineCache.getClient()
  ) {
    val appCtx = context.applicationContext
    val savedToken = NotificationPreferences.getFcmToken(appCtx)
    NotificationPreferences.clearFcmToken(appCtx)

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
    client: OkHttpClient = OfflineCache.getClient()
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val cleanToken = fcmToken.trim()
    if (cleanToken.isBlank() || accessToken.isBlank()) return@withContext Result.success(Unit)
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val req = Request.Builder()
      .url("${SupabaseConfig.url}/rest/v1/rpc/unregister_device_token")
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(buildUnregisterPayload(cleanToken).toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
        } else {
          Result.success(Unit)
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to unregister push token")))
    }
  }
}
