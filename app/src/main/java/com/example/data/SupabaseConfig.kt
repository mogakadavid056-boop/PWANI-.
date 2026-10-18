package com.example.data

import com.example.BuildConfig

object SupabaseConfig {

  const val NETWORK_UNREACHABLE_MESSAGE = "Cannot reach the server. Check your internet and try again."

  val url: String
    get() {
      val raw = try {
        val bc = BuildConfig.SUPABASE_URL
        if (bc.isNotBlank() && !bc.contains("your-project.supabase.co")) bc else Constants.DEFAULT_SUPABASE_URL
      } catch (_: Throwable) {
        Constants.DEFAULT_SUPABASE_URL
      }
      return sanitizeUrl(raw)
    }

  val anonKey: String
    get() {
      val raw = try {
        val bc = BuildConfig.SUPABASE_ANON_KEY
        if (bc.isNotBlank() && !bc.contains("your-supabase-anon-key")) bc else Constants.DEFAULT_SUPABASE_ANON_KEY
      } catch (_: Throwable) {
        Constants.DEFAULT_SUPABASE_ANON_KEY
      }
      return sanitizeKey(raw)
    }

  fun sanitizeUrl(rawUrl: String?): String {
    if (rawUrl == null) return ""
    var cleaned = rawUrl.trim()
      .removeSurrounding("\"")
      .removeSurrounding("'")
      .trim()

    // Strip trailing slashes
    while (cleaned.endsWith("/")) {
      cleaned = cleaned.dropLast(1).trim()
    }

    // Strip /rest/v1 or /auth/v1 or /rest or /auth suffix
    val suffixes = listOf("/rest/v1", "/auth/v1", "/rest", "/auth")
    for (suffix in suffixes) {
      if (cleaned.endsWith(suffix, ignoreCase = true)) {
        cleaned = cleaned.substring(0, cleaned.length - suffix.length).trim()
      }
    }

    while (cleaned.endsWith("/")) {
      cleaned = cleaned.dropLast(1).trim()
    }

    return cleaned
  }

  fun sanitizeKey(rawKey: String?): String {
    if (rawKey == null) return ""
    return rawKey.trim()
      .removeSurrounding("\"")
      .removeSurrounding("'")
      .trim()
  }

  fun isConfigured(): Boolean {
    val currentUrl = url
    val currentKey = anonKey
    if (currentUrl.isEmpty() || currentKey.isEmpty()) {
      return false
    }
    if (currentUrl.contains("your-project.supabase.co") ||
        currentKey.contains("your-supabase-anon-key")) {
      return false
    }
    return true
  }

  const val MISSING_CONFIG_ERROR =
    "Supabase credentials not configured. Please set SUPABASE_URL and SUPABASE_ANON_KEY in the AI Studio Secrets panel."

  fun isNetworkError(throwable: Throwable?): Boolean {
    if (throwable == null) return false
    var current: Throwable? = throwable
    while (current != null) {
      if (current is java.net.UnknownHostException ||
          current is java.net.ConnectException ||
          current is java.net.SocketTimeoutException ||
          current is java.net.NoRouteToHostException ||
          current is java.net.PortUnreachableException ||
          current is java.net.SocketException ||
          (current.message?.contains("Unable to resolve host", ignoreCase = true) == true) ||
          (current.message?.contains("Failed to connect", ignoreCase = true) == true) ||
          (current.message?.contains("Network is unreachable", ignoreCase = true) == true) ||
          (current.message?.contains("Connection refused", ignoreCase = true) == true) ||
          (current.message?.contains("Software caused connection abort", ignoreCase = true) == true) ||
          (current.message?.contains("timeout", ignoreCase = true) == true && current is java.io.IOException)
      ) {
        return true
      }
      current = current.cause
    }
    return false
  }

  fun formatExceptionMessage(throwable: Throwable?, defaultMessage: String = ""): String {
    if (isNetworkError(throwable)) {
      return NETWORK_UNREACHABLE_MESSAGE
    }
    val msg = throwable?.localizedMessage ?: throwable?.message
    return if (!msg.isNullOrBlank()) msg else defaultMessage
  }

  fun extractErrorMessage(responseBody: String, statusCode: Int): String {
    if (statusCode in 502..504) {
      return NETWORK_UNREACHABLE_MESSAGE
    }
    if (responseBody.isBlank()) return "Server responded with status code $statusCode"
    return try {
      val json = org.json.JSONObject(responseBody)
      when {
        json.has("message") -> json.getString("message")
        json.has("msg") -> json.getString("msg")
        json.has("error_description") -> json.getString("error_description")
        json.has("error") -> json.getString("error")
        json.has("details") -> json.getString("details")
        json.has("hint") -> json.getString("hint")
        else -> responseBody
      }
    } catch (_: Throwable) {
      val keys = listOf("message", "msg", "error_description", "error", "details", "hint")
      for (key in keys) {
        val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"")
        val match = regex.find(responseBody)
        if (match != null) {
          return match.groupValues[1]
        }
      }
      responseBody.take(150)
    }
  }
}
