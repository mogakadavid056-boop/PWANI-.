package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarms.AlarmNavigationHelper
import com.example.alarms.AlarmNotificationHelper
import com.example.alarms.AlarmPreferences
import com.example.alarms.AlarmScheduler
import com.example.alarms.TimetableAlarmSync
import com.example.data.ScheduleItem
import com.example.data.UserSession
import com.example.ui.components.ClassRepCard
import com.example.ui.theme.*
import com.example.util.JsonHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmSettingsScreen(
  currentClassSchedules: List<ScheduleItem>,
  session: UserSession?,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val settingsState by AlarmPreferences.state.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) {
    AlarmPreferences.refreshState(context)
  }

  var isSyncing by remember { mutableStateOf(false) }
  var statusBanner by remember { mutableStateOf<String?>(null) }

  var hasNotifPermission by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
      } else true
    )
  }
  val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    hasNotifPermission = granted
  }

  val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
  val canExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    alarmManager?.canScheduleExactAlarms() == true
  } else true

  val mergedCourses = remember(settingsState.cachedSchedules, currentClassSchedules) {
    val all = (settingsState.cachedSchedules + currentClassSchedules)
      .filter { it.title.isNotBlank() || it.courseCode.isNotBlank() }
    all.groupBy { it.courseKey }.mapNotNull { (key, list) ->
      if (key.isBlank()) null else key to list
    }
  }

  Column(
    modifier = modifier.fillMaxWidth().testTag("alarm_settings_screen"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    ClassRepCard {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Default.Alarm, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text("Class Alarms", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
              text = if (settingsState.masterEnabled) "Ringing alarms enabled (works offline & silent)" else "All class alarms paused",
              fontSize = 12.sp,
              color = TextSecondary
            )
          }
        }
        Switch(
          checked = settingsState.masterEnabled,
          onCheckedChange = { enabled ->
            AlarmPreferences.setMasterEnabled(context, enabled)
            AlarmScheduler.rescheduleAll(context)
          },
          modifier = Modifier.testTag("alarm_master_switch"),
          colors = SwitchDefaults.colors(checkedThumbColor = DarkTextOnGold, checkedTrackColor = GoldPrimary)
        )
      }
    }

    ClassRepCard {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Schedule, contentDescription = null, tint = GoldLight, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Reminder Time", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = GoldLight)
      }
      Text(
        text = if (settingsState.reminderMinutes > 0) {
          "Alarms ring ${settingsState.reminderMinutes} min before and at class start."
        } else {
          "Alarms ring at class start time."
        },
        fontSize = 12.sp,
        color = TextSecondary
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        AlarmPreferences.REMINDER_OPTIONS.forEach { mins ->
          val selected = settingsState.reminderMinutes == mins
          val label = if (mins == 0) "At start" else "$mins min before"
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(if (selected) GoldPrimary else DarkSurfaceVariant)
              .border(1.dp, if (selected) GoldPrimary else DarkBorder, RoundedCornerShape(10.dp))
              .clickable(enabled = settingsState.masterEnabled) {
                AlarmPreferences.setReminderMinutes(context, mins)
                AlarmScheduler.rescheduleAll(context)
              }
              .padding(horizontal = 12.dp, vertical = 8.dp)
              .testTag("alarm_offset_$mins")
          ) {
            Text(
              text = label,
              fontSize = 12.sp,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              color = if (selected) DarkTextOnGold else TextPrimary
            )
          }
        }
      }
    }

    ClassRepCard {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Course Alarms (${mergedCourses.size})", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = GoldLight)
        TextButton(
          onClick = {
            if (session != null && !isSyncing) {
              isSyncing = true
              scope.launch {
                val res = TimetableAlarmSync.syncAndReschedule(context, session)
                isSyncing = false
                statusBanner = if (res.isSuccess) "Synced timetable & rescheduled alarms" else "Rescheduled from offline cache"
              }
            }
          },
          enabled = !isSyncing
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (isSyncing) "Syncing..." else "Sync", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      if (!statusBanner.isNullOrBlank()) {
        Text(statusBanner!!, fontSize = 12.sp, color = SuccessGreen)
      }

      if (mergedCourses.isEmpty()) {
        Text("No timetable courses found yet. Refresh your timetable to populate courses.", fontSize = 12.sp, color = TextSecondary)
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          mergedCourses.forEach { (courseKey, items) ->
            val first = items.first()
            val code = AlarmNotificationHelper.sanitizeField(first.courseCode)
            val title = AlarmNotificationHelper.sanitizeField(first.title)
            val header = if (code.isNotBlank() && !code.equals(title, true)) "$code • $title" else JsonHelper.orDash(title.ifBlank { code })
            val summary = items.joinToString(", ") { "${it.dayName.take(3)} ${it.startTime.take(5)}" }
            val enabled = !settingsState.disabledCourses.contains(courseKey)

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(header, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(summary, fontSize = 11.sp, color = TextSecondary)
                if (first.lecturerName.isNotBlank() || first.room.isNotBlank()) {
                  val meta = listOfNotNull(first.room.ifBlank { null }, first.lecturerName.ifBlank { null }).joinToString(" • ")
                  Text(meta, fontSize = 11.sp, color = GoldLight)
                }
              }
              Switch(
                checked = enabled && settingsState.masterEnabled,
                enabled = settingsState.masterEnabled,
                onCheckedChange = { isChecked ->
                  AlarmPreferences.setCourseEnabled(context, courseKey, isChecked)
                  AlarmScheduler.rescheduleAll(context)
                },
                modifier = Modifier.testTag("alarm_course_switch_$courseKey"),
                colors = SwitchDefaults.colors(checkedThumbColor = DarkTextOnGold, checkedTrackColor = GoldPrimary)
              )
            }
          }
        }
      }
    }

    ClassRepCard {
      Text("Alarm Reliability", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GoldLight)
      if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        OutlinedButton(
          onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
          modifier = Modifier.fillMaxWidth(),
          border = BorderStroke(1.dp, GoldPrimary)
        ) {
          Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Allow Notification Permission", color = GoldLight, fontSize = 12.sp)
        }
      }
      if (!canExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        OutlinedButton(
          onClick = { AlarmNavigationHelper.openExactAlarmSettings(context) },
          modifier = Modifier.fillMaxWidth(),
          border = BorderStroke(1.dp, GoldPrimary)
        ) {
          Icon(Icons.Default.Alarm, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Allow Exact Alarms", color = GoldLight, fontSize = 12.sp)
        }
      }
      OutlinedButton(
        onClick = { AlarmNavigationHelper.openBatteryOptimizationSettings(context) },
        modifier = Modifier.fillMaxWidth().testTag("alarm_battery_settings_btn"),
        border = BorderStroke(1.dp, DarkBorder)
      ) {
        Icon(Icons.Default.BatterySaver, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Battery Optimization Settings", color = TextPrimary, fontSize = 12.sp)
      }
    }
  }
}
