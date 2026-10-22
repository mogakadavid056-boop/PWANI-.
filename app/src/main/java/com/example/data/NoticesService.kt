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

class NoticesService(
  private val client: OkHttpClient = OfflineCache.getClient()
) {

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  private fun buildGet(url: String, token: String): Request =
    Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $token")
      .addHeader("Accept", "application/json")
      .get()
      .build()

  suspend fun fetchNotices(
    classId: String,
    accessToken: String
  ): Result<List<NoticeItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))

    val url = "${SupabaseConfig.url}/rest/v1/notices?class_id=eq.$classId&select=id,title,content,category,is_urgent,is_pinned,created_at&order=is_pinned.desc,created_at.desc"
    try {
      client.newCall(buildGet(url, accessToken)).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, response.code)))
        val array = JSONArray(body)
        val list = (0 until array.length()).map { i ->
          val obj = array.getJSONObject(i)
          NoticeItem(
            id = obj.optString("id"),
            title = obj.optString("title"),
            content = obj.optString("content"),
            category = obj.optString("category", "general"),
            isUrgent = obj.optBoolean("is_urgent", false),
            isPinned = obj.optBoolean("is_pinned", false),
            createdAt = obj.optString("created_at")
          )
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to load notices"))
    }
  }

  suspend fun fetchLatestAnnouncements(
    accessToken: String
  ): Result<List<NoticeItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))

    val url = "${SupabaseConfig.url}/rest/v1/notices?select=id,title,content,category,is_urgent,created_at,classes(name)&order=created_at.desc&limit=5"
    try {
      client.newCall(buildGet(url, accessToken)).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, response.code)))
        val array = JSONArray(body)
        val list = (0 until array.length()).map { i ->
          val obj = array.getJSONObject(i)
          val clsName = obj.optJSONObject("classes")?.optString("name")
          NoticeItem(
            id = obj.optString("id"),
            title = obj.optString("title"),
            content = obj.optString("content"),
            category = obj.optString("category", "general"),
            isUrgent = obj.optBoolean("is_urgent", false),
            isPinned = false,
            createdAt = obj.optString("created_at"),
            className = clsName
          )
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to load announcements"))
    }
  }

  suspend fun fetchReadNoticeIds(
    userId: String,
    accessToken: String
  ): Result<Set<String>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))

    val url = "${SupabaseConfig.url}/rest/v1/notice_reads?select=notice_id&user_id=eq.$userId"
    try {
      client.newCall(buildGet(url, accessToken)).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, response.code)))
        val array = JSONArray(body)
        val set = (0 until array.length()).mapNotNull { i ->
          val id = array.getJSONObject(i).optString("notice_id")
          if (id.isNotBlank()) id else null
        }.toSet()
        Result.success(set)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to check read notices"))
    }
  }

  suspend fun markNoticeAsRead(
    noticeId: String,
    userId: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.success(Unit)

    val body = JSONObject().apply {
      put("notice_id", noticeId)
      put("user_id", userId)
    }
    val url = "${SupabaseConfig.url}/rest/v1/notice_reads"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Prefer", "resolution=ignore-duplicates")
      .post(body.toString().toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(request).execute().use { Result.success(Unit) }
    } catch (_: Exception) {
      Result.success(Unit)
    }
  }

  suspend fun checkIsStudentRep(
    classId: String,
    userId: String,
    accessToken: String
  ): Result<Boolean> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))

    val url = "${SupabaseConfig.url}/rest/v1/student_representatives?class_id=eq.$classId&user_id=eq.$userId&active=eq.true"
    try {
      client.newCall(buildGet(url, accessToken)).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, response.code)))
        Result.success(JSONArray(body).length() > 0)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to verify representative role"))
    }
  }

  suspend fun createNotice(
    classId: String,
    title: String,
    content: String,
    category: String,
    urgent: Boolean,
    pinned: Boolean,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))

    val body = JSONObject().apply {
      put("target_class_id", classId)
      put("target_course_id", JSONObject.NULL)
      put("notice_title", title)
      put("notice_content", content)
      put("notice_category", category)
      put("urgent", urgent)
      put("pinned", pinned)
    }
    val url = "${SupabaseConfig.url}/rest/v1/rpc/create_notice"
    val req = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .post(body.toString().toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { response ->
        val b = response.body?.string().orEmpty()
        if (response.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(SupabaseConfig.extractErrorMessage(b, response.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to post notice"))
    }
  }

  suspend fun editNotice(
    noticeId: String,
    title: String,
    content: String,
    category: String,
    urgent: Boolean,
    pinned: Boolean,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))

    val body = JSONObject().apply {
      put("title", title)
      put("content", content)
      put("category", category)
      put("is_urgent", urgent)
      put("is_pinned", pinned)
    }
    val url = "${SupabaseConfig.url}/rest/v1/notices?id=eq.$noticeId"
    val req = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .patch(body.toString().toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { response ->
        val b = response.body?.string().orEmpty()
        if (response.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(SupabaseConfig.extractErrorMessage(b, response.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to update notice"))
    }
  }

  suspend fun deleteNotice(
    noticeId: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))

    val url = "${SupabaseConfig.url}/rest/v1/notices?id=eq.$noticeId"
    val req = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .delete()
      .build()
    try {
      client.newCall(req).execute().use { response ->
        val b = response.body?.string().orEmpty()
        if (response.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(SupabaseConfig.extractErrorMessage(b, response.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to delete notice"))
    }
  }
}
