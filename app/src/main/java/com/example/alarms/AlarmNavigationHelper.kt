package com.example.alarms

import android.app.NotificationManager
import android.content.Context
import android.content.Intent

data class AlarmNavigationTarget(
  val classId: String,
  val dayOfWeek: Int,
  val notificationId: Int,
  val nonce: Int
)

object AlarmNavigationHelper {
  fun parseIntent(intent: Intent?): AlarmNavigationTarget? {
    if (intent == null) return null
    if (!intent.getBooleanExtra(AlarmNotificationHelper.EXTRA_OPEN_TIMETABLE, false)) return null
    val classId = AlarmNotificationHelper.sanitizeField(intent.getStringExtra(AlarmNotificationHelper.EXTRA_CLASS_ID))
    val dayOfWeek = intent.getIntExtra(AlarmNotificationHelper.EXTRA_DAY_OF_WEEK, 1).coerceIn(1, 7)
    val notificationId = intent.getIntExtra(AlarmNotificationHelper.EXTRA_NOTIFICATION_ID, 0)
    intent.removeExtra(AlarmNotificationHelper.EXTRA_OPEN_TIMETABLE)
    return AlarmNavigationTarget(
      classId = classId,
      dayOfWeek = dayOfWeek,
      notificationId = notificationId,
      nonce = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
    )
  }

  fun dismissTriggeredAlarm(context: Context, notificationId: Int) {
    val appCtx = context.applicationContext
    AlarmSoundPlayer.stopRinging(appCtx)
    if (notificationId != 0) {
      val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      nm?.cancel(notificationId)
    }
  }

  fun openExactAlarmSettings(context: Context) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return
    runCatching {
      context.startActivity(
        Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
          data = android.net.Uri.parse("package:${context.packageName}")
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      )
    }.onFailure { openAppDetailsSettings(context) }
  }

  fun openBatteryOptimizationSettings(context: Context) {
    runCatching {
      context.startActivity(
        Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      )
    }.onFailure { openAppDetailsSettings(context) }
  }

  private fun openAppDetailsSettings(context: Context) {
    runCatching {
      context.startActivity(
        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
          data = android.net.Uri.parse("package:${context.packageName}")
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      )
    }
  }
}
