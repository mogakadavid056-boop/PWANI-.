package com.example.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.ScheduleItem
import java.util.Calendar

object AlarmScheduler {

  fun getOffsetsToSchedule(reminderMinutes: Int): List<Int> {
    return if (reminderMinutes > 0) listOf(reminderMinutes, 0) else listOf(0)
  }

  fun computeNextTriggerMillis(
    dayOfWeek: Int,
    startTime: String,
    offsetMinutes: Int,
    nowMillis: Long = System.currentTimeMillis()
  ): Long? {
    val cleanTime = startTime.trim()
    val parts = cleanTime.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null

    val targetCalDay = when (dayOfWeek) {
      1 -> Calendar.MONDAY
      2 -> Calendar.TUESDAY
      3 -> Calendar.WEDNESDAY
      4 -> Calendar.THURSDAY
      5 -> Calendar.FRIDAY
      6 -> Calendar.SATURDAY
      7 -> Calendar.SUNDAY
      else -> return null
    }

    val cal = Calendar.getInstance().apply {
      timeInMillis = nowMillis
      set(Calendar.DAY_OF_WEEK, targetCalDay)
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      add(Calendar.MINUTE, -offsetMinutes)
    }

    while (cal.timeInMillis <= nowMillis) {
      cal.add(Calendar.DAY_OF_YEAR, 7)
    }
    return cal.timeInMillis
  }

  fun computeAlarmRequestCode(
    scheduleId: String,
    offsetMinutes: Int,
    usedCodes: Set<Int> = emptySet()
  ): Int {
    var code = ("${scheduleId}_${offsetMinutes}".hashCode() and 0x3FFFFFFF)
    if (code == 0) code = 1
    while (usedCodes.contains(code)) {
      code = (code + 1) and 0x3FFFFFFF
      if (code == 0) code = 1
    }
    return code
  }

  fun computeSnoozeRequestCode(scheduleId: String, offsetMinutes: Int): Int {
    val base = ("SNOOZE_${scheduleId}_${offsetMinutes}".hashCode() and 0x3FFFFFFF)
    return base or 0x40000000
  }

  fun rescheduleAll(
    context: Context,
    schedules: List<ScheduleItem>? = null,
    nowMillis: Long = System.currentTimeMillis()
  ) {
    val appCtx = context.applicationContext
    val settings = AlarmPreferences.refreshState(appCtx)
    val sourceList = if (schedules != null) {
      AlarmPreferences.saveCachedSchedules(appCtx, schedules)
      schedules
    } else {
      settings.cachedSchedules
    }

    cancelAllAlarms(appCtx)

    if (!settings.masterEnabled || sourceList.isEmpty()) return

    val offsets = getOffsetsToSchedule(settings.reminderMinutes)
    val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    val usedCodes = mutableSetOf<Int>()
    val scheduledRecords = mutableListOf<ScheduledAlarmRecord>()

    sourceList.forEach { item ->
      if (item.id.isBlank() || item.isCancelled) return@forEach
      val cKey = item.courseKey
      if (!settings.isCourseEnabled(cKey)) return@forEach

      offsets.forEach { offset ->
        val triggerAt = computeNextTriggerMillis(
          dayOfWeek = item.dayOfWeek,
          startTime = item.startTime,
          offsetMinutes = offset,
          nowMillis = nowMillis
        ) ?: return@forEach

        val reqCode = computeAlarmRequestCode(item.id, offset, usedCodes)
        usedCodes.add(reqCode)

        val record = ScheduledAlarmRecord(
          requestCode = reqCode,
          scheduleId = item.id,
          classId = item.classId,
          courseKey = cKey,
          courseCode = item.courseCode.ifBlank { item.title },
          title = item.title,
          lecturerName = item.lecturerName,
          room = item.room,
          dayOfWeek = item.dayOfWeek,
          startTime = item.startTime,
          endTime = item.endTime,
          offsetMinutes = offset,
          triggerAtMillis = triggerAt,
          isSnooze = false
        )

        val intent = AlarmReceiver.buildAlarmIntent(appCtx, record)
        val pending = PendingIntent.getBroadcast(
          appCtx,
          reqCode,
          intent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactOrFallback(alarmManager, triggerAt, pending)
        scheduledRecords.add(record)
      }
    }

    AlarmPreferences.saveScheduledRegistry(appCtx, scheduledRecords)
  }

  fun rescheduleNextWeekOccurrence(
    context: Context,
    record: ScheduledAlarmRecord,
    nowMillis: Long = System.currentTimeMillis()
  ) {
    val appCtx = context.applicationContext
    if (!AlarmPreferences.isMasterEnabled(appCtx)) return
    if (!AlarmPreferences.isCourseEnabled(appCtx, record.courseKey)) return

    val nextTrigger = computeNextTriggerMillis(
      dayOfWeek = record.dayOfWeek,
      startTime = record.startTime,
      offsetMinutes = record.offsetMinutes,
      nowMillis = nowMillis + 65_000L
    ) ?: return

    val updated = record.copy(triggerAtMillis = nextTrigger, isSnooze = false)
    val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = AlarmReceiver.buildAlarmIntent(appCtx, updated)
    val pending = PendingIntent.getBroadcast(
      appCtx,
      updated.requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    setExactOrFallback(alarmManager, nextTrigger, pending)

    val existing = AlarmPreferences.getScheduledRegistry(appCtx).toMutableList()
    val idx = existing.indexOfFirst { it.requestCode == updated.requestCode }
    if (idx >= 0) existing[idx] = updated else existing.add(updated)
    AlarmPreferences.saveScheduledRegistry(appCtx, existing)
  }

  fun scheduleSnooze(
    context: Context,
    record: ScheduledAlarmRecord,
    snoozeMinutes: Int = 5
  ) {
    val appCtx = context.applicationContext
    val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val snoozeCode = computeSnoozeRequestCode(record.scheduleId, record.offsetMinutes)
    val triggerAt = System.currentTimeMillis() + snoozeMinutes * 60_000L
    val snoozeRecord = record.copy(
      requestCode = snoozeCode,
      triggerAtMillis = triggerAt,
      isSnooze = true
    )
    val intent = AlarmReceiver.buildAlarmIntent(appCtx, snoozeRecord)
    val pending = PendingIntent.getBroadcast(
      appCtx,
      snoozeCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    setExactOrFallback(alarmManager, triggerAt, pending)
  }

  fun cancelAllAlarms(context: Context) {
    val appCtx = context.applicationContext
    val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val records = AlarmPreferences.getScheduledRegistry(appCtx)

    records.forEach { rec ->
      val alarmPending = PendingIntent.getBroadcast(
        appCtx,
        rec.requestCode,
        AlarmReceiver.buildAlarmIntent(appCtx, rec),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
      )
      alarmPending?.let {
        alarmManager.cancel(it)
        it.cancel()
      }

      val snoozeCode = computeSnoozeRequestCode(rec.scheduleId, rec.offsetMinutes)
      val snoozePending = PendingIntent.getBroadcast(
        appCtx,
        snoozeCode,
        AlarmReceiver.buildAlarmIntent(appCtx, rec.copy(requestCode = snoozeCode, isSnooze = true)),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
      )
      snoozePending?.let {
        alarmManager.cancel(it)
        it.cancel()
      }
    }

    AlarmPreferences.saveScheduledRegistry(appCtx, emptyList())
  }

  private fun setExactOrFallback(
    alarmManager: AlarmManager,
    triggerAtMillis: Long,
    pendingIntent: PendingIntent
  ) {
    val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      alarmManager.canScheduleExactAlarms()
    } else {
      true
    }
    try {
      if (canExact) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      } else {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      }
    } catch (_: SecurityException) {
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
  }
}
