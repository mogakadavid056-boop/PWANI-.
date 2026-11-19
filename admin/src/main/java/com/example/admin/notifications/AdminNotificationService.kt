package com.example.admin.notifications

import com.example.admin.data.SupabaseAdminConfig
import com.example.admin.util.AdminJsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class AdminNotificationService(
  private val client: OkHttpClient = SupabaseAdminConfig.sharedHttpClient
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun fetchUnreadCount(accessToken: String): Result<Int> = withContext(Dispatchers.IO) {
    if (!SupabaseAdminConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseAdminConfig.MISSING_CONFIG_ERROR))
    }
    val req = Request.Builder()
      .url("${SupabaseAdminConfig.url}/rest/v1/rpc/unread_notification_count")
      .addHeader("apikey", SupabaseAdminConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post("{}".toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(body, resp.code)))
        }
        val count = body.trim().removeSurrounding("\"").toIntOrNull() ?: 0
        Result.success(count.coerceAtLeast(0))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, "Failed to load unread count")))
    }
  }

  suspend fun fetchNotifications(
    accessToken: String,
    limit: Int = 30,
    offset: Int = 0
  ): Result<List<AdminNotificationItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseAdminConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseAdminConfig.MISSING_CONFIG_ERROR))
    }
    val url = "${SupabaseAdminConfig.url}/rest/v1/notifications" +
      "?select=id,class_id,kind,title,body,data,read_at,created_at" +
      "&order=created_at.desc&limit=$limit&offset=$offset"
    val req = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseAdminConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Accept", "application/json")
      .get()
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(body, resp.code)))
        }
        Result.success(parseNotificationsJson(body))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, "Failed to load notifications")))
    }
  }

  suspend fun markRead(
    notificationIds: List<String>?,
    accessToken: String
  ): Result<Int> = withContext(Dispatchers.IO) {
    if (!SupabaseAdminConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseAdminConfig.MISSING_CONFIG_ERROR))
    }
    val payload = JSONObject()
    if (!notificationIds.isNullOrEmpty()) {
      val arr = JSONArray()
      notificationIds.forEach { arr.put(it) }
      payload.put("notification_ids", arr)
    }
    val req = Request.Builder()
      .url("${SupabaseAdminConfig.url}/rest/v1/rpc/mark_notifications_read")
      .addHeader("apikey", SupabaseAdminConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(payload.toString().toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(body, resp.code)))
        }
        Result.success(body.trim().removeSurrounding("\"").toIntOrNull() ?: 0)
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, "Failed to update notifications")))
    }
  }

  companion object {
    fun parseNotificationsJson(body: String): List<AdminNotificationItem> {
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
                val v = AdminJsonHelper.str(dataObj, k)
                if (v.isNotBlank()) put(k, v)
              }
            }
          }
          add(
            AdminNotificationItem(
              id = AdminJsonHelper.str(obj, "id"),
              classId = AdminJsonHelper.strOrNull(obj, "class_id"),
              kind = AdminJsonHelper.str(obj, "kind"),
              title = AdminJsonHelper.str(obj, "title").ifBlank { "Notification" },
              body = AdminJsonHelper.str(obj, "body"),
              data = dataMap,
              readAt = AdminJsonHelper.strOrNull(obj, "read_at"),
              createdAt = AdminJsonHelper.str(obj, "created_at")
            )
          )
        }
      }
    }
  }
}
