package com.example.admin.notifications

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AdminNotificationItem(
  val id: String,
  val classId: String?,
  val kind: String,
  val title: String,
  val body: String,
  val data: Map<String, String> = emptyMap(),
  val readAt: String? = null,
  val createdAt: String = ""
) {
  val isUnread: Boolean
    get() = readAt.isNullOrBlank()

  val targetClassId: String
    get() = classId?.takeIf { it.isNotBlank() && !it.equals("null", true) }
      ?: data["class_id"]?.takeIf { it.isNotBlank() && !it.equals("null", true) }
      ?: ""

  val isUrgentKind: Boolean
    get() = kind in setOf("attendance_alert", "attendance_warning", "notice_urgent")

  val isApprovalKind: Boolean
    get() = kind in setOf("lecturer_registered", "unit_pending", "lecturer_join_request", "unit_approved", "unit_rejected", "lecturer_verified")
}

data class AdminNotificationDeepLink(
  val kind: String,
  val classId: String,
  val data: Map<String, String> = emptyMap()
)

object AdminNotificationTimeFormatter {
  private val dateFallbackFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.US)

  fun formatRelative(isoString: String?, nowMillis: Long = System.currentTimeMillis()): String {
    val raw = isoString?.trim().orEmpty()
    if (raw.isBlank() || raw.equals("null", ignoreCase = true)) return ""
    return try {
      val instant = Instant.parse(raw)
      val diffSec = ((nowMillis - instant.toEpochMilli()) / 1000L).coerceAtLeast(0L)
      when {
        diffSec < 60L -> "Just now"
        diffSec < 3600L -> "${diffSec / 60L} min ago"
        diffSec < 86_400L -> {
          val hrs = diffSec / 3600L
          if (hrs == 1L) "1 hr ago" else "$hrs hr ago"
        }
        diffSec < 7L * 86_400L -> {
          val days = diffSec / 86_400L
          if (days == 1L) "Yesterday" else "$days days ago"
        }
        else -> instant.atZone(ZoneId.systemDefault()).format(dateFallbackFormatter)
      }
    } catch (_: Throwable) {
      raw.replace("T", " ").take(16)
    }
  }
}
