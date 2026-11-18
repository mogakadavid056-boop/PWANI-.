package com.example.admin.data

import com.example.admin.BuildConfig
import com.example.admin.util.AdminJsonHelper
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseAdminConfig {
  const val DEFAULT_SUPABASE_URL = "https://zftrsxyqixnmqpcxrbyo.supabase.co"
  const val DEFAULT_SUPABASE_ANON_KEY = "sb_publishable_PzdmF_ouJmxKgmVhvYq-nQ_Jgy6o9lY"
  const val NETWORK_UNREACHABLE_MESSAGE = "Cannot reach Supabase. Check your internet connection and try again."
  const val MISSING_CONFIG_ERROR = "Supabase credentials not configured. Set SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets."

  val sharedHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(18, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(20, TimeUnit.SECONDS)
      .build()
  }

  val url: String
    get() {
      val raw = try {
        val bc = BuildConfig.SUPABASE_URL
        if (bc.isNotBlank() && !bc.contains("your-project.supabase.co")) bc else DEFAULT_SUPABASE_URL
      } catch (_: Throwable) {
        DEFAULT_SUPABASE_URL
      }
      return sanitizeUrl(raw)
    }

  val anonKey: String
    get() {
      val raw = try {
        val bc = BuildConfig.SUPABASE_ANON_KEY
        if (bc.isNotBlank() && !bc.contains("your-supabase-anon-key")) bc else DEFAULT_SUPABASE_ANON_KEY
      } catch (_: Throwable) {
        DEFAULT_SUPABASE_ANON_KEY
      }
      return raw.trim().removeSurrounding("\"").removeSurrounding("'").trim()
    }

  val hostLabel: String
    get() = url.removePrefix("https://").removePrefix("http://").substringBefore("/")

  fun sanitizeUrl(rawUrl: String?): String {
    if (rawUrl == null) return ""
    var cleaned = rawUrl.trim().removeSurrounding("\"").removeSurrounding("'").trim()
    while (cleaned.endsWith("/")) cleaned = cleaned.dropLast(1).trim()
    for (suffix in listOf("/rest/v1", "/auth/v1", "/rest", "/auth")) {
      if (cleaned.endsWith(suffix, ignoreCase = true)) {
        cleaned = cleaned.substring(0, cleaned.length - suffix.length).trim()
      }
    }
    while (cleaned.endsWith("/")) cleaned = cleaned.dropLast(1).trim()
    return cleaned
  }

  fun isConfigured(): Boolean =
    url.isNotEmpty() && anonKey.isNotEmpty() &&
      !url.contains("your-project.supabase.co") &&
      !anonKey.contains("your-supabase-anon-key")

  fun formatExceptionMessage(throwable: Throwable?, defaultMessage: String = "Request failed"): String {
    var cur = throwable
    while (cur != null) {
      if (cur is java.net.UnknownHostException ||
        cur is java.net.ConnectException ||
        cur is java.net.SocketTimeoutException ||
        cur is java.net.SocketException ||
        cur.message?.contains("Unable to resolve host", ignoreCase = true) == true
      ) {
        return NETWORK_UNREACHABLE_MESSAGE
      }
      cur = cur.cause
    }
    return throwable?.localizedMessage?.takeIf { it.isNotBlank() } ?: defaultMessage
  }

  fun extractErrorMessage(responseBody: String, statusCode: Int): String {
    if (statusCode in 502..504) return NETWORK_UNREACHABLE_MESSAGE
    if (responseBody.isBlank()) return "Server responded with status code $statusCode"
    return try {
      val json = JSONObject(responseBody)
      val msg = AdminJsonHelper.firstStr(json, "message", "msg", "error_description", "error", "details", "hint")
      msg.ifBlank { responseBody.take(180) }
    } catch (_: Throwable) {
      responseBody.take(180)
    }
  }
}
