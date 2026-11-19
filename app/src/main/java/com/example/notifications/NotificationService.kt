package com.example.notifications

import com.example.data.OfflineCache
import com.example.data.SupabaseConfig
import com.example.util.JsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class NotificationService(
  private val client: OkHttpClient = OfflineCache.getClient()
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun fetchUnreadCount(accessToken: String): Result<Int> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val req = Request.Builder()
      .url("${SupabaseConfig.url}/rest/v1/rpc/unread_notification_count")
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post("{}".toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
        }
        val count = body.trim().removeSurrounding("\"").toIntOrNull() ?: 0
        Result.success(count.coerceAtLeast(0))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to load unread count")))
    }
  }

  suspend fun fetchNotifications(
    accessToken: String,
    limit: Int = 30,
    offset: Int = 0
  ): Result<List<AppNotificationItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val url = "${SupabaseConfig.url}/rest/v1/notifications" +
      "?select=id,class_id,kind,title,body,data,read_at,created_at" +
      "&order=created_at.desc&limit=$limit&offset=$offset"
    val req = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Accept", "application/json")
      .get()
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
        }
        Result.success(parseNotificationsJson(body))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to load notifications")))
    }
  }

  suspend fun markRead(
    notificationIds: List<String>?,
    accessToken: String
  ): Result<Int> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val payload = JSONObject()
    if (!notificationIds.isNullOrEmpty()) {
      val arr = JSONArray()
      notificationIds.forEach { arr.put(it) }
      payload.put("notification_ids", arr)
    }
    val req = Request.Builder()
      .url("${SupabaseConfig.url}/rest/v1/rpc/mark_notifications_read")
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(payload.toString().toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
        }
        Result.success(body.trim().removeSurrounding("\"").toIntOrNull() ?: 0)
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to update notifications")))
    }
  }

  companion object {
    fun parseNotificationsJson(body: String): List<AppNotificationItem> {
      val trimmed = body.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyList()
      val arr = JSONArray(trimmed)
      return buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val dataObj = obj.optJSONObject("data")
          val dataMap = buildMap<String, String> {
            if (dataObj != null) {
              val keys = dataObj.keys()
              while (keys.hasNext()) {
                val k = keys.next()
                val v = JsonHelper.str(dataObj, k)
                if (v.isNotBlank()) put(k, v)
              }
            }
          }
          add(
            AppNotificationItem(
              id = JsonHelper.str(obj, "id"),
              classId = JsonHelper.strOrNull(obj, "class_id"),
              kind = JsonHelper.str(obj, "kind"),
              title = JsonHelper.str(obj, "title").ifBlank { "Notification" },
              body = JsonHelper.str(obj, "body"),
              data = dataMap,
              readAt = JsonHelper.strOrNull(obj, "read_at"),
              createdAt = JsonHelper.str(obj, "created_at")
            )
          )
        }
      }
    }
  }
}
