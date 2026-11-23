package com.example.util

import java.util.Locale

enum class DurationUnit(
  val label: String,
  val shortLabel: String,
  val secondsMultiplier: Long
) {
  SECONDS("Seconds", "sec", 1L),
  MINUTES("Minutes", "min", 60L),
  HOURS("Hours", "hr", 3_600L),
  DAYS("Days", "days", 86_400L),
  WEEKS("Weeks", "wks", 604_800L),
  MONTHS("Months", "mos", 2_592_000L);

  companion object {
    fun fromSuffix(suffix: String): DurationUnit? = when (suffix.trim().lowercase(Locale.ROOT)) {
      "s", "sec", "secs", "second", "seconds" -> SECONDS
      "m", "min", "mins", "minute", "minutes" -> MINUTES
      "h", "hr", "hrs", "hour", "hours" -> HOURS
      "d", "day", "days" -> DAYS
      "w", "wk", "wks", "week", "weeks" -> WEEKS
      "mo", "mos", "mon", "month", "months" -> MONTHS
      else -> null
    }
  }
}

object DurationHelper {
  fun toSeconds(value: Long?, unit: DurationUnit): Long? {
    if (value == null || value <= 0L) return null
    val raw = runCatching { Math.multiplyExact(value, unit.secondsMultiplier) }.getOrNull() ?: return null
    return raw.coerceAtLeast(1L)
  }

  fun parseDurationExpressionToSeconds(raw: String?): Long? {
    val clean = raw?.trim().orEmpty()
    if (clean.isBlank()) return null
    clean.toLongOrNull()?.let { return it.takeIf { v -> v > 0L } }
    val match = Regex("^(\\d+)\\s*([A-Za-z]+)$").matchEntire(clean) ?: return null
    val amount = match.groupValues[1].toLongOrNull() ?: return null
    val unit = DurationUnit.fromSuffix(match.groupValues[2]) ?: return null
    return toSeconds(amount, unit)
  }

  fun decomposeBestUnit(totalSeconds: Long?): Pair<Long, DurationUnit> {
    if (totalSeconds == null || totalSeconds <= 0L) return 1L to DurationUnit.MINUTES
    val ordered = listOf(
      DurationUnit.MONTHS,
      DurationUnit.WEEKS,
      DurationUnit.DAYS,
      DurationUnit.HOURS,
      DurationUnit.MINUTES,
      DurationUnit.SECONDS
    )
    for (unit in ordered) {
      if (totalSeconds >= unit.secondsMultiplier && totalSeconds % unit.secondsMultiplier == 0L) {
        return (totalSeconds / unit.secondsMultiplier) to unit
      }
    }
    return totalSeconds to DurationUnit.SECONDS
  }

  fun formatSummary(totalSeconds: Long?): String {
    if (totalSeconds == null || totalSeconds <= 0L) return "No limit"
    val (value, unit) = decomposeBestUnit(totalSeconds)
    return "$value ${unit.shortLabel}"
  }

  fun formatCountdown(totalSeconds: Long): String {
    val clamped = totalSeconds.coerceAtLeast(0L)
    if (clamped >= 2_592_000L) {
      val mos = clamped / 2_592_000L
      val remAfterMo = clamped % 2_592_000L
      val days = remAfterMo / 86_400L
      val hrs = (remAfterMo % 86_400L) / 3_600L
      val mins = (remAfterMo % 3_600L) / 60L
      val secs = remAfterMo % 60L
      return String.format(Locale.US, "%dmo %dd %02d:%02d:%02d", mos, days, hrs, mins, secs)
    }
    if (clamped >= 86_400L) {
      val days = clamped / 86_400L
      val rem = clamped % 86_400L
      val hrs = rem / 3_600L
      val mins = (rem % 3_600L) / 60L
      val secs = rem % 60L
      return String.format(Locale.US, "%dd %02d:%02d:%02d", days, hrs, mins, secs)
    }
    if (clamped >= 3_600L) {
      val hrs = clamped / 3_600L
      val mins = (clamped % 3_600L) / 60L
      val secs = clamped % 60L
      return String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    }
    val mins = clamped / 60L
    val secs = clamped % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
  }
}
