package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object NairobiTimeHelper {
  private val nairobiTz = TimeZone.getTimeZone("Africa/Nairobi")

  fun formatIsoToNairobi(isoString: String?, pattern: String = "yyyy-MM-dd HH:mm"): String {
    val raw = isoString?.trim().orEmpty()
    if (raw.isBlank() || raw.equals("null", ignoreCase = true)) return "-"
    val clean = raw.replace("Z", "").substringBefore("+")
    val patterns = listOf(
      "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
      "yyyy-MM-dd'T'HH:mm:ss.SSS",
      "yyyy-MM-dd'T'HH:mm:ss",
      "yyyy-MM-dd HH:mm:ss",
      "yyyy-MM-dd"
    )
    for (p in patterns) {
      try {
        val parser = SimpleDateFormat(p, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val date = parser.parse(clean)
        if (date != null) {
          val formatter = SimpleDateFormat(pattern, Locale.US).apply { timeZone = nairobiTz }
          return formatter.format(date)
        }
      } catch (_: Exception) {}
    }
    return raw
  }

  fun formatIsoToTimeOnly(isoString: String?): String {
    return formatIsoToNairobi(isoString, "HH:mm")
  }

  fun formatIsoToDateOnly(isoString: String?): String {
    return formatIsoToNairobi(isoString, "yyyy-MM-dd")
  }

  fun nowNairobi(pattern: String = "yyyy-MM-dd HH:mm"): String {
    val formatter = SimpleDateFormat(pattern, Locale.US).apply { timeZone = nairobiTz }
    return formatter.format(Date())
  }

  fun toIsoUtc(date: Date = Date()): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    return sdf.format(date)
  }

  fun calculateRemainingSeconds(expiresAtIso: String): Long {
    if (expiresAtIso.isBlank()) return 0L
    val clean = expiresAtIso.replace("Z", "").substringBefore("+")
    val patterns = listOf(
      "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
      "yyyy-MM-dd'T'HH:mm:ss.SSS",
      "yyyy-MM-dd'T'HH:mm:ss",
      "yyyy-MM-dd HH:mm:ss"
    )
    for (pat in patterns) {
      try {
        val date = SimpleDateFormat(pat, Locale.US).apply {
          timeZone = TimeZone.getTimeZone("UTC")
        }.parse(clean)
        if (date != null) return ((date.time - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
      } catch (_: Exception) {}
    }
    return 0L
  }
}
