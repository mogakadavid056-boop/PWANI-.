package com.example.alarms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object AlarmNotificationHelper {
  const val CHANNEL_ID = "class_alarms_channel"
  const val CHANNEL_NAME = "Class Alarms"

  const val EXTRA_OPEN_TIMETABLE = "extra_open_timetable"
  const val EXTRA_CLASS_ID = "extra_class_id"
  const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
  const val EXTRA_DAY_OF_WEEK = "extra_day_of_week"
  const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

  private val VIBRATION_PATTERN = longArrayOf(0L, 600L, 300L, 600L, 300L, 800L)

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
      ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
      ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val attrs = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_ALARM)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()
    val channel = NotificationChannel(
      CHANNEL_ID,
      CHANNEL_NAME,
      NotificationManager.IMPORTANCE_HIGH
    ).apply {
      description = "Loud ringing alarms before and at the start of registered classes"
      setSound(soundUri, attrs)
      enableVibration(true)
      vibrationPattern = VIBRATION_PATTERN
      setBypassDnd(true)
      lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    }
    nm.createNotificationChannel(channel)
  }

  fun sanitizeField(raw: String?): String {
    val clean = raw?.trim().orEmpty()
    return if (clean.equals("null", ignoreCase = true)) "" else clean
  }

  fun formatAlarmText(
    courseCode: String?,
    title: String?,
    startTime: String?,
    endTime: String?,
    room: String?,
    lecturerName: String?,
    offsetMinutes: Int,
    isSnooze: Boolean
  ): Pair<String, String> {
    val cleanCode = sanitizeField(courseCode)
    val cleanTitle = sanitizeField(title)
    val label = cleanCode.ifBlank { cleanTitle }.ifBlank { "Class" }

    val startShort = sanitizeField(startTime).take(5)
    val endShort = sanitizeField(endTime).take(5)
    val timeRange = when {
      startShort.isNotBlank() && endShort.isNotBlank() -> "$startShort - $endShort"
      startShort.isNotBlank() -> startShort
      else -> ""
    }

    val timing = when {
      isSnooze -> "snooze reminder"
      offsetMinutes > 0 -> "starts in $offsetMinutes min"
      else -> "starts now"
    }

    val cleanRoom = sanitizeField(room)
    val cleanLecturer = sanitizeField(lecturerName)
    val trailing = listOfNotNull(
      cleanRoom.ifBlank { null },
      cleanLecturer.ifBlank { null }
    )
    val body = buildString {
      append("$label $timing")
      if (trailing.isNotEmpty()) {
        append(" - ")
        append(trailing.joinToString(" - "))
      }
    }

    val notifTitle = buildString {
      append(label)
      if (cleanCode.isNotBlank() && cleanTitle.isNotBlank() && !cleanTitle.equals(cleanCode, ignoreCase = true)) {
        append(" • $cleanTitle")
      }
      if (timeRange.isNotBlank()) {
        append(" ($timeRange)")
      }
    }
    return notifTitle to body
  }

  fun showAlarmNotification(
    context: Context,
    record: ScheduledAlarmRecord,
    notificationId: Int
  ) {
    ensureChannel(context)
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    val (notifTitle, notifBody) = formatAlarmText(
      courseCode = record.courseCode,
      title = record.title,
      startTime = record.startTime,
      endTime = record.endTime,
      room = record.room,
      lecturerName = record.lecturerName,
      offsetMinutes = record.offsetMinutes,
      isSnooze = record.isSnooze
    )

    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(EXTRA_OPEN_TIMETABLE, true)
      putExtra(EXTRA_CLASS_ID, record.classId)
      putExtra(EXTRA_SCHEDULE_ID, record.scheduleId)
      putExtra(EXTRA_DAY_OF_WEEK, record.dayOfWeek)
      putExtra(EXTRA_NOTIFICATION_ID, notificationId)
    }
    val openPending = PendingIntent.getActivity(
      context,
      notificationId,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val dismissIntent = AlarmReceiver.buildActionIntent(context, AlarmReceiver.ACTION_DISMISS, record, notificationId)
    val dismissPending = PendingIntent.getBroadcast(
      context,
      notificationId + 100_000,
      dismissIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val snoozeIntent = AlarmReceiver.buildActionIntent(context, AlarmReceiver.ACTION_SNOZE, record, notificationId)
    val snoozePending = PendingIntent.getBroadcast(
      context,
      notificationId + 200_000,
      snoozeIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(notifTitle)
      .setContentText(notifBody)
      .setStyle(NotificationCompat.BigTextStyle().bigText(notifBody))
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setContentIntent(openPending)
      .setFullScreenIntent(openPending, true)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPending)
      .addAction(android.R.drawable.ic_popup_reminder, "Snooze (5 min)", snoozePending)

    try {
      nm.notify(notificationId, builder.build())
    } catch (_: SecurityException) {
      // Notification permission denied on API 33+
    }
  }
}

object AlarmSoundPlayer {
  private var mediaPlayer: MediaPlayer? = null
  private var activeVibrator: Vibrator? = null
  private val handler = Handler(Looper.getMainLooper())
  private var autoStopRunnable: Runnable? = null

  @Synchronized
  fun startRinging(context: Context) {
    stopRinging(context)
    val appCtx = context.applicationContext

    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
      ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
      ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val alarmAttrs = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_ALARM)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()

    if (soundUri != null) {
      runCatching {
        mediaPlayer = MediaPlayer().apply {
          setAudioAttributes(alarmAttrs)
          setDataSource(appCtx, soundUri)
          isLooping = true
          prepare()
          start()
        }
      }
    }

    runCatching {
      val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (appCtx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        appCtx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }
      activeVibrator = vibrator
      if (vibrator != null && vibrator.hasVibrator()) {
        val pattern = longArrayOf(0L, 600L, 300L, 600L, 300L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0), alarmAttrs)
        } else {
          @Suppress("DEPRECATION")
          vibrator.vibrate(pattern, 0)
        }
      }
    }

    val stopTask = Runnable { stopRinging(appCtx) }
    autoStopRunnable = stopTask
    handler.postDelayed(stopTask, 60_000L)
  }

  @Synchronized
  fun stopRinging(context: Context? = null) {
    autoStopRunnable?.let { handler.removeCallbacks(it) }
    autoStopRunnable = null
    runCatching {
      mediaPlayer?.stop()
      mediaPlayer?.release()
    }
    mediaPlayer = null
    runCatching { activeVibrator?.cancel() }
    activeVibrator = null
  }
}
