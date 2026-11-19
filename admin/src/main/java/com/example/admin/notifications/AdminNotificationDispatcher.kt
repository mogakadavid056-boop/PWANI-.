package com.example.admin.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.admin.MainActivity
import com.example.admin.R

object AdminNotificationDispatcher {
  const val CHANNEL_URGENT = "admin_urgent"
  const val CHANNEL_APPROVALS = "admin_approvals"
  const val CHANNEL_UPDATES = "admin_updates"
  const val PUSH_CHANNEL_URGENT = "urgent"
  const val PUSH_CHANNEL_UPDATES = "updates"

  private const val EXTRA_NOTIF_ID = "admin_notif_id"
  private const val EXTRA_NOTIF_KIND = "admin_notif_kind"
  private const val EXTRA_NOTIF_CLASS_ID = "admin_notif_class_id"
  private const val EXTRA_NOTIF_COURSE_ID = "admin_notif_course_id"
  private const val EXTRA_NOTIF_USER_ID = "admin_notif_user_id"

  fun ensureChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val channels = listOf(
      buildUrgentChannel(CHANNEL_URGENT, "Urgent", "Urgent attendance risk alerts and escalations", defaultSound),
      buildUrgentChannel(PUSH_CHANNEL_URGENT, "Urgent Push Alerts", "Real-time urgent notices and attendance alerts", defaultSound),
      NotificationChannel(CHANNEL_APPROVALS, "Approvals", NotificationManager.IMPORTANCE_DEFAULT).apply {
        description = "Pending lecturer verifications, unit approvals, and join requests"
      },
      NotificationChannel(CHANNEL_UPDATES, "Updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
        description = "General ClassRep Admin updates"
      },
      NotificationChannel(PUSH_CHANNEL_UPDATES, "Real-time Updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
        description = "Real-time administrative updates and approvals"
      }
    )
    nm.createNotificationChannels(channels)
  }

  private fun buildUrgentChannel(
    id: String,
    name: String,
    desc: String,
    soundUri: android.net.Uri?
  ): NotificationChannel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
    description = desc
    enableVibration(true)
    vibrationPattern = longArrayOf(0L, 250L, 150L, 250L)
    if (soundUri != null) setSound(soundUri, android.app.Notification.AUDIO_ATTRIBUTES_DEFAULT)
  }

  fun channelForKind(kind: String): String = when (kind) {
    "attendance_alert", "notice_urgent", "attendance_warning" -> CHANNEL_URGENT
    "lecturer_registered", "unit_pending", "lecturer_join_request" -> CHANNEL_APPROVALS
    else -> CHANNEL_UPDATES
  }

  fun pushChannelForKind(kind: String): String = when (kind) {
    "notice_urgent", "attendance_open", "attendance_warning", "attendance_alert" -> PUSH_CHANNEL_URGENT
    else -> PUSH_CHANNEL_UPDATES
  }

  fun dispatchUnseen(context: Context, items: List<AdminNotificationItem>) {
    val unread = items.filter { it.isUnread && it.id.isNotBlank() }
    val unseen = unread.filterNot { AdminNotificationPrefs.isAlreadyShown(context, it.id) }
    if (unseen.isEmpty()) return

    ensureChannels(context)
    val newlyShown = mutableListOf<String>()
    unseen.take(6).forEach { item ->
      postSystemNotification(
        context = context,
        notifId = item.id,
        kind = item.kind,
        title = item.title,
        body = item.body.ifBlank { item.title },
        classId = item.targetClassId,
        courseId = item.data["course_id"].orEmpty(),
        userId = item.data["user_id"].orEmpty(),
        channelId = channelForKind(item.kind)
      )
      newlyShown.add(item.id)
    }
    AdminNotificationPrefs.markShown(context, newlyShown)
  }

  fun dispatchRemoteMessage(
    context: Context,
    title: String,
    body: String,
    data: Map<String, String>,
    explicitChannelId: String? = null
  ): Boolean {
    val notifId = cleanExtra(data["notification_id"])
    if (notifId.isNotBlank() && AdminNotificationPrefs.isAlreadyShown(context, notifId)) {
      return false
    }
    val kind = cleanExtra(data["kind"]).ifBlank { "update" }
    val classId = cleanExtra(data["class_id"])
    val courseId = cleanExtra(data["course_id"])
    val userId = cleanExtra(data["user_id"])
    val channelId = if (explicitChannelId == PUSH_CHANNEL_URGENT) {
      PUSH_CHANNEL_URGENT
    } else {
      pushChannelForKind(kind)
    }

    ensureChannels(context)
    val effectiveId = notifId.ifBlank { "admin_fcm:${System.currentTimeMillis()}:${kind.hashCode()}" }
    val resolvedTitle = title.ifBlank { "ClassRep Admin" }
    val resolvedBody = body.ifBlank { resolvedTitle }
    postSystemNotification(
      context = context,
      notifId = effectiveId,
      kind = kind,
      title = resolvedTitle,
      body = resolvedBody,
      classId = classId,
      courseId = courseId,
      userId = userId,
      channelId = channelId
    )
    if (notifId.isNotBlank()) {
      AdminNotificationPrefs.markShown(context, listOf(notifId))
    }
    return true
  }

  private fun postSystemNotification(
    context: Context,
    notifId: String,
    kind: String,
    title: String,
    body: String,
    classId: String,
    courseId: String,
    userId: String,
    channelId: String
  ) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(EXTRA_NOTIF_ID, notifId)
      putExtra(EXTRA_NOTIF_KIND, kind)
      putExtra(EXTRA_NOTIF_CLASS_ID, classId)
      if (courseId.isNotBlank()) putExtra(EXTRA_NOTIF_COURSE_ID, courseId)
      if (userId.isNotBlank()) putExtra(EXTRA_NOTIF_USER_ID, userId)
    }
    val notifHashId = 600_000 + (notifId.hashCode() and 0x7FFF)
    val pending = PendingIntent.getActivity(
      context, notifHashId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val isHigh = channelId == CHANNEL_URGENT || channelId == PUSH_CHANNEL_URGENT
    val priority = if (isHigh) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
    val builder = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(priority)
      .setAutoCancel(true)
      .setContentIntent(pending)

    runCatching { nm.notify(notifHashId, builder.build()) }
  }

  fun parseIntentDeepLink(intent: Intent?, context: Context? = null): AdminNotificationDeepLink? {
    if (intent == null) return null
    val kind = cleanExtra(intent.getStringExtra(EXTRA_NOTIF_KIND))
      .ifBlank { cleanExtra(intent.getStringExtra("kind")) }
    if (kind.isBlank()) return null

    val notifId = cleanExtra(intent.getStringExtra(EXTRA_NOTIF_ID))
      .ifBlank { cleanExtra(intent.getStringExtra("notification_id")) }
    val classId = cleanExtra(intent.getStringExtra(EXTRA_NOTIF_CLASS_ID))
      .ifBlank { cleanExtra(intent.getStringExtra("class_id")) }
    val courseId = cleanExtra(intent.getStringExtra(EXTRA_NOTIF_COURSE_ID))
      .ifBlank { cleanExtra(intent.getStringExtra("course_id")) }
    val userId = cleanExtra(intent.getStringExtra(EXTRA_NOTIF_USER_ID))
      .ifBlank { cleanExtra(intent.getStringExtra("user_id")) }

    val data = buildMap {
      if (notifId.isNotBlank()) put("notification_id", notifId)
      if (courseId.isNotBlank()) put("course_id", courseId)
      if (userId.isNotBlank()) put("user_id", userId)
    }
    if (context != null && notifId.isNotBlank()) {
      AdminNotificationPrefs.markShown(context, listOf(notifId))
    }
    listOf(
      EXTRA_NOTIF_ID, EXTRA_NOTIF_KIND, EXTRA_NOTIF_CLASS_ID, EXTRA_NOTIF_COURSE_ID, EXTRA_NOTIF_USER_ID,
      "notification_id", "kind", "class_id", "course_id", "user_id"
    ).forEach { intent.removeExtra(it) }

    return AdminNotificationDeepLink(kind = kind, classId = classId, data = data)
  }

  private fun cleanExtra(raw: String?): String {
    val trimmed = raw?.trim().orEmpty()
    return if (trimmed.equals("null", ignoreCase = true)) "" else trimmed
  }
}
