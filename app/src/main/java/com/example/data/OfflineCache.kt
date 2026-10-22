package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class CacheEntry(
  val body: String,
  val savedAt: Long
)

object OfflineCache {
  var currentUserId: String = ""
  var appContext: Context? = null

  private val _lastUpdatedFlow = MutableStateFlow<String?>(null)
  val lastUpdatedFlow: StateFlow<String?> = _lastUpdatedFlow.asStateFlow()

  fun notifyFromCache(savedAt: Long) {
    _lastUpdatedFlow.value = formatRelativeTime(savedAt)
  }

  fun clearLastUpdated() {
    _lastUpdatedFlow.value = null
  }

  fun isCacheable(method: String, url: String): Boolean {
    val cleanUrl = url.lowercase()

    // NEVER cache
    if (cleanUrl.contains("/auth/v1/")) return false
    if (cleanUrl.contains("attendance_sessions")) return false
    if (cleanUrl.contains("attendance_codes")) return false
    if (cleanUrl.contains("/messages")) return false
    if (cleanUrl.contains("poll_votes") || cleanUrl.contains("vote_poll")) return false
    if (cleanUrl.contains("storage/v1/object/sign")) return false

    // Allowed RPCs (POST)
    if (method.equals("POST", ignoreCase = true)) {
      val allowedRpcs = listOf(
        "list_class_reps",
        "list_assignments",
        "get_my_attendance",
        "list_polls",
        "list_due_soon"
      )
      return allowedRpcs.any { cleanUrl.contains("rpc/$it") }
    }

    // Must be GET for table reads
    if (!method.equals("GET", ignoreCase = true)) return false

    // Staff-only member lists check: only cache if it's the user's own membership
    if (cleanUrl.contains("/class_members")) {
      return cleanUrl.contains("user_id=eq.")
    }

    // Allowed GET reads
    val allowedGets = listOf(
      "/classes",
      "/notices",
      "/notice_reads",
      "/schedules",
      "/materials",
      "/profiles",
      "/schools",
      "/programmes"
    )
    return allowedGets.any { cleanUrl.contains(it) }
  }

  private fun hashKey(method: String, url: String, body: String): String {
    val raw = "${method.uppercase()}_${url}_$body"
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(raw.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }

  fun getCacheDir(context: Context, userId: String): File {
    val safeUid = if (userId.isNotBlank()) userId else "anonymous"
    val dir = File(context.filesDir, "cache/$safeUid")
    if (!dir.exists()) dir.mkdirs()
    return dir
  }

  fun save(context: Context, userId: String, method: String, url: String, body: String, responseJson: String) {
    if (userId.isBlank() || responseJson.isBlank()) return
    try {
      val dir = getCacheDir(context, userId)
      val key = hashKey(method, url, body)
      val file = File(dir, "$key.cache")
      val timestamp = System.currentTimeMillis()
      file.writeText("$timestamp\n$responseJson")
      pruneCacheIfNeeded(dir)
    } catch (_: Exception) {}
  }

  fun get(context: Context, userId: String, method: String, url: String, body: String): CacheEntry? {
    if (userId.isBlank()) return null
    return try {
      val dir = getCacheDir(context, userId)
      val key = hashKey(method, url, body)
      val file = File(dir, "$key.cache")
      if (!file.exists()) return null
      val text = file.readText()
      val newlineIdx = text.indexOf('\n')
      if (newlineIdx == -1) null
      else {
        val savedAt = text.substring(0, newlineIdx).toLongOrNull() ?: file.lastModified()
        val json = text.substring(newlineIdx + 1)
        CacheEntry(body = json, savedAt = savedAt)
      }
    } catch (_: Exception) {
      null
    }
  }

  private fun pruneCacheIfNeeded(dir: File) {
    val files = dir.listFiles() ?: return
    if (files.size > 300) {
      val sorted = files.sortedBy { it.lastModified() }
      val toDelete = files.size - 300
      for (i in 0 until toDelete) {
        sorted[i].delete()
      }
    }
  }

  fun clearUserCache(context: Context, userId: String) {
    if (userId.isBlank()) return
    try {
      val dir = File(context.filesDir, "cache/$userId")
      if (dir.exists()) dir.deleteRecursively()
    } catch (_: Exception) {}
  }

  fun formatRelativeTime(millis: Long): String {
    if (millis <= 0) return "recently"
    val diff = System.currentTimeMillis() - millis
    if (diff < 0) return "just now"
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
      seconds < 60 -> "just now"
      minutes < 60 -> "$minutes min ago"
      hours < 24 -> "$hours hr${if (hours > 1) "s" else ""} ago"
      days == 1L -> "yesterday"
      days < 7 -> "$days days ago"
      else -> {
        val sdf = java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault())
        sdf.format(java.util.Date(millis))
      }
    }
  }

  val sharedClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(15, TimeUnit.SECONDS)
      .writeTimeout(15, TimeUnit.SECONDS)
      .addInterceptor(OfflineCacheInterceptor())
      .build()
  }

  fun getClient(): OkHttpClient = sharedClient
}

class OfflineCacheInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val method = request.method
    val url = request.url.toString()
    val context = OfflineCache.appContext
    val userId = OfflineCache.currentUserId

    val requestBodyString = if (method.equals("POST", ignoreCase = true)) {
      request.body?.let { b ->
        val buf = okio.Buffer()
        b.writeTo(buf)
        buf.readUtf8()
      } ?: ""
    } else ""

    val cacheable = context != null && userId.isNotBlank() && OfflineCache.isCacheable(method, url)

    if (!NetworkMonitor.isOnline.value && cacheable && context != null) {
      val cached = OfflineCache.get(context, userId, method, url, requestBodyString)
      if (cached != null) {
        OfflineCache.notifyFromCache(cached.savedAt)
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        return Response.Builder()
          .request(request)
          .protocol(okhttp3.Protocol.HTTP_1_1)
          .code(200)
          .message("OK (from cache)")
          .header("X-From-Cache", "true")
          .header("X-Cache-Time", cached.savedAt.toString())
          .body(cached.body.toResponseBody(mediaType))
          .build()
      } else {
        throw IOException("You are offline and this has not been saved yet.")
      }
    }

    try {
      val response = chain.proceed(request)
      if (response.isSuccessful && cacheable && context != null) {
        val responseBody = response.body
        if (responseBody != null) {
          val mediaType = responseBody.contentType()
          val bodyString = responseBody.string()
          OfflineCache.save(context, userId, method, url, requestBodyString, bodyString)
          return response.newBuilder()
            .body(bodyString.toResponseBody(mediaType))
            .build()
        }
      }
      return response
    } catch (e: Exception) {
      if (cacheable && context != null && (e is IOException || !NetworkMonitor.isOnline.value)) {
        val cached = OfflineCache.get(context, userId, method, url, requestBodyString)
        if (cached != null) {
          OfflineCache.notifyFromCache(cached.savedAt)
          val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
          return Response.Builder()
            .request(request)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(200)
            .message("OK (from cache)")
            .header("X-From-Cache", "true")
            .header("X-Cache-Time", cached.savedAt.toString())
            .body(cached.body.toResponseBody(mediaType))
            .build()
        } else {
          throw IOException("You are offline and this has not been saved yet.")
        }
      }
      throw e
    }
  }
}
