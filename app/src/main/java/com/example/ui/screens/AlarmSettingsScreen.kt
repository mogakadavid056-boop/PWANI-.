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
import com.example.ui.components.OutlinedGoldButton
import com.example.ui.components.RoyalButtonGradientBrush
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
          Icon(Icons.Default.Alarm, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text("Class Alarms", style = CardTitleStyle)
            Text(
              text = if (settingsState.masterEnabled) "Ringing alarms enabled (works offline & silent)" else "All class alarms paused",
              style = CaptionStyle.copy(color = Lavender)
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
          colors = SwitchDefaults.colors(checkedThumbColor = Midnight, checkedTrackColor = GoldPrimary)
        )
      }
    }

    ClassRepCard {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Schedule, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Reminder Time", style = CardTitleStyle.copy(color = ChampagneGold))
      }
      Text(
        text = if (settingsState.reminderMinutes > 0) {
          "Alarms ring ${settingsState.reminderMinutes} min before and at class start."
        } else {
          "Alarms ring at class start time."
        },
        style = CaptionStyle.copy(color = Lavender)
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
              .defaultMinSize(minHeight = 48.dp)
              .clip(RoundedCornerShape(12.dp))
              .then(
                if (selected) Modifier.background(RoyalButtonGradientBrush)
                else Modifier.background(RoyalSurfaceHigh)
              )
              .border(1.dp, if (selected) ChampagneGold else DarkBorder, RoundedCornerShape(12.dp))
              .clickable(enabled = settingsState.masterEnabled) {
                AlarmPreferences.setReminderMinutes(context, mins)
                AlarmScheduler.rescheduleAll(context)
              }
              .padding(horizontal = 14.dp, vertical = 10.dp)
              .testTag("alarm_offset_$mins"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = label,
              fontFamily = MontserratFamily,
              fontSize = 13.sp,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
              color = if (selected) RoyalPurple else Lavender
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
        Text("Course Alarms (${mergedCourses.size})", style = CardTitleStyle.copy(color = ChampagneGold))
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
          enabled = !isSyncing,
          modifier = Modifier.defaultMinSize(minHeight = 48.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (isSyncing) "Syncing..." else "Sync", style = FieldLabelStyle.copy(color = ChampagneGold))
        }
      }

      if (!statusBanner.isNullOrBlank()) {
        Text(statusBanner!!, style = CaptionStyle.copy(color = SuccessColor))
      }

      if (mergedCourses.isEmpty()) {
        Text("No timetable courses found yet. Refresh your timetable to populate courses.", style = CaptionStyle.copy(color = Lavender))
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
                .clip(RoundedCornerShape(12.dp))
                .background(RoyalSurfaceHigh)
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(header, style = CardTitleStyle.copy(fontSize = 16.sp))
                Text(summary, style = CaptionStyle.copy(color = Lavender))
                if (first.lecturerName.isNotBlank() || first.room.isNotBlank()) {
                  val meta = listOfNotNull(first.room.ifBlank { null }, first.lecturerName.ifBlank { null }).joinToString(" • ")
                  Text(meta, style = CaptionStyle.copy(color = ChampagneGold))
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
                colors = SwitchDefaults.colors(checkedThumbColor = Midnight, checkedTrackColor = GoldPrimary)
              )
            }
          }
        }
      }
    }

    ClassRepCard {
      Text("Alarm Reliability", style = CardTitleStyle.copy(color = ChampagneGold))
      if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        OutlinedGoldButton(
          text = "Allow Notification Permission",
          leadingIcon = Icons.Default.NotificationsActive,
          onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
          modifier = Modifier.fillMaxWidth()
        )
      }
      if (!canExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        OutlinedGoldButton(
          text = "Allow Exact Alarms",
          leadingIcon = Icons.Default.Alarm,
          onClick = { AlarmNavigationHelper.openExactAlarmSettings(context) },
          modifier = Modifier.fillMaxWidth()
        )
      }
      OutlinedGoldButton(
        text = "Battery Optimization Settings",
        leadingIcon = Icons.Default.BatterySaver,
        onClick = { AlarmNavigationHelper.openBatteryOptimizationSettings(context) },
        modifier = Modifier.fillMaxWidth().testTag("alarm_battery_settings_btn")
      )
    }
  }
}
