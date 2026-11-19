package com.example.admin.notifications

import android.content.Context

object AdminNotificationPrefs {
  private const val PREFS_NAME = "classrep_admin_notif_prefs"
  private const val KEY_SHOWN_IDS = "shown_notification_ids"
  private const val KEY_PERMISSION_ASKED = "notification_permission_asked"
  private const val KEY_FCM_TOKEN = "admin_fcm_device_token"
  private const val MAX_STORED_IDS = 300

  private fun prefs(context: Context) =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun isAlreadyShown(context: Context, notificationId: String): Boolean {
    if (notificationId.isBlank()) return true
    val stored = prefs(context).getString(KEY_SHOWN_IDS, "").orEmpty()
    if (stored.isBlank()) return false
    return stored.split(",").contains(notificationId)
  }

  fun markShown(context: Context, notificationIds: List<String>) {
    if (notificationIds.isEmpty()) return
    val p = prefs(context)
    val existing = p.getString(KEY_SHOWN_IDS, "").orEmpty()
      .split(",")
      .filter { it.isNotBlank() }
      .toMutableList()
    notificationIds.forEach { id ->
      if (id.isNotBlank() && !existing.contains(id)) existing.add(id)
    }
    val trimmed = if (existing.size > MAX_STORED_IDS) existing.takeLast(MAX_STORED_IDS) else existing
    p.edit().putString(KEY_SHOWN_IDS, trimmed.joinToString(",")).apply()
  }

  fun saveFcmToken(context: Context, token: String) {
    val clean = token.trim()
    if (clean.isNotBlank()) prefs(context).edit().putString(KEY_FCM_TOKEN, clean).apply()
  }

  fun getFcmToken(context: Context): String? =
    prefs(context).getString(KEY_FCM_TOKEN, null)?.takeIf { it.isNotBlank() }

  fun clearFcmToken(context: Context) {
    prefs(context).edit().remove(KEY_FCM_TOKEN).apply()
  }

  fun wasPermissionPromptDismissed(context: Context): Boolean =
    prefs(context).getBoolean(KEY_PERMISSION_ASKED, false)

  fun setPermissionPromptDismissed(context: Context, dismissed: Boolean) {
    prefs(context).edit().putBoolean(KEY_PERMISSION_ASKED, dismissed).apply()
  }

  fun clearOnLogout(context: Context) {
    prefs(context).edit().remove(KEY_SHOWN_IDS).apply()
  }
}
