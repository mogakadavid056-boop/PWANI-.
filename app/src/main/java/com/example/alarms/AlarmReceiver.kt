package com.example.alarms

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

  companion object {
    const val ACTION_CLASS_ALARM = "com.example.classrep.ACTION_CLASS_ALARM"
    const val ACTION_DISMISS = "com.example.classrep.ACTION_ALARM_DISMISS"
    const val ACTION_SNOZE = "com.example.classrep.ACTION_ALARM_SNOZE"

    const val EXTRA_REQUEST_CODE = "extra_request_code"
    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    const val EXTRA_CLASS_ID = "extra_class_id"
    const val EXTRA_COURSE_KEY = "extra_course_key"
    const val EXTRA_COURSE_CODE = "extra_course_code"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_LECTURER = "extra_lecturer"
    const val EXTRA_ROOM = "extra_room"
    const val EXTRA_DAY_OF_WEEK = "extra_day_of_week"
    const val EXTRA_START_TIME = "extra_start_time"
    const val EXTRA_END_TIME = "extra_end_time"
    const val EXTRA_OFFSET_MINUTES = "extra_offset_minutes"
    const val EXTRA_IS_SNOZE = "extra_is_snooze"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    fun buildAlarmIntent(context: Context, record: ScheduledAlarmRecord): Intent {
      return Intent(context, AlarmReceiver::class.java).apply {
        action = ACTION_CLASS_ALARM
        putExtrasFromRecord(this, record)
      }
    }

    fun buildActionIntent(
      context: Context,
      actionName: String,
      record: ScheduledAlarmRecord,
      notificationId: Int
    ): Intent {
      return Intent(context, AlarmReceiver::class.java).apply {
        action = actionName
        putExtrasFromRecord(this, record)
        putExtra(EXTRA_NOTIFICATION_ID, notificationId)
      }
    }

    private fun putExtrasFromRecord(intent: Intent, record: ScheduledAlarmRecord) {
      intent.putExtra(EXTRA_REQUEST_CODE, record.requestCode)
      intent.putExtra(EXTRA_SCHEDULE_ID, record.scheduleId)
      intent.putExtra(EXTRA_CLASS_ID, record.classId)
      intent.putExtra(EXTRA_COURSE_KEY, record.courseKey)
      intent.putExtra(EXTRA_COURSE_CODE, record.courseCode)
      intent.putExtra(EXTRA_TITLE, record.title)
      intent.putExtra(EXTRA_LECTURER, record.lecturerName)
      intent.putExtra(EXTRA_ROOM, record.room)
      intent.putExtra(EXTRA_DAY_OF_WEEK, record.dayOfWeek)
      intent.putExtra(EXTRA_START_TIME, record.startTime)
      intent.putExtra(EXTRA_END_TIME, record.endTime)
      intent.putExtra(EXTRA_OFFSET_MINUTES, record.offsetMinutes)
      intent.putExtra(EXTRA_IS_SNOZE, record.isSnooze)
    }

    fun recordFromIntent(intent: Intent): ScheduledAlarmRecord {
      val reqCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0)
      return ScheduledAlarmRecord(
        requestCode = reqCode,
        scheduleId = AlarmNotificationHelper.sanitizeField(intent.getStringExtra(EXTRA_SCHEDULE_ID)),
        classId = AlarmNotificationHelper.sanitizeField(intent.getStringExtra(EXTRA_CLASS_ID)),
        courseKey = AlarmNotificationHelper.sanitizeField(intent.getStringExtra(EXTRA_COURSE_KEY)),
        courseCode = AlarmNotificationHelper.sanitizeField(intent.getStringExtra(EXTRA_COURSE_CODE)),
        title = AlarmNotificationHelper.sanitizeField(intent.getStringExtra(EXTRA_TITLE)),
        lecturerName = AlarmNotificationHelper.sanitizeField(intent.getStringExtra(EXTRA_LECTURER)),
        room = AlarmNotificationHelper.sanitizeField(intent.getStringExtra(EXTRA_ROOM)),
        dayOfWeek = intent.getIntExtra(EXTRA_DAY_OF_WEEK, 1),
        startTime = AlarmNotificationHelper.sanitizeField(intent.getStringExtra(EXTRA_START_TIME)),
        endTime = AlarmNotificationHelper.sanitizeField(intent.getStringExtra(EXTRA_END_TIME)),
        offsetMinutes = intent.getIntExtra(EXTRA_OFFSET_MINUTES, 0),
        triggerAtMillis = System.currentTimeMillis(),
        isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOZE, false)
      )
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    val appCtx = context.applicationContext
    val record = recordFromIntent(intent)
    val fallbackNotifId = if (record.requestCode != 0) record.requestCode else 9001
    val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, fallbackNotifId)

    when (intent.action) {
      ACTION_CLASS_ALARM -> {
        if (!AlarmPreferences.isMasterEnabled(appCtx)) return
        if (!AlarmPreferences.isCourseEnabled(appCtx, record.courseKey)) return

        if (!record.isSnooze) {
          AlarmScheduler.rescheduleNextWeekOccurrence(appCtx, record)
        }
        AlarmNotificationHelper.ensureChannel(appCtx)
        AlarmSoundPlayer.startRinging(appCtx)
        AlarmNotificationHelper.showAlarmNotification(appCtx, record, notificationId)
      }

      ACTION_DISMISS -> {
        AlarmSoundPlayer.stopRinging(appCtx)
        cancelNotification(appCtx, notificationId)
      }

      ACTION_SNOZE -> {
        AlarmSoundPlayer.stopRinging(appCtx)
        cancelNotification(appCtx, notificationId)
        AlarmScheduler.scheduleSnooze(appCtx, record, snoozeMinutes = 5)
      }
    }
  }

  private fun cancelNotification(context: Context, notificationId: Int) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    nm?.cancel(notificationId)
  }
}
