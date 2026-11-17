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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.components.ClassRepCard
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.*

@Composable
fun AlarmOnboardingDialog(
  onComplete: () -> Unit
) {
  val context = LocalContext.current
  val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

  var notifGranted by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
      } else true
    )
  }
  var exactGranted by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager?.canScheduleExactAlarms() == true
      } else true
    )
  }
  var requestedInitialPermissions by remember { mutableStateOf(false) }

  val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    notifGranted = granted
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      exactGranted = alarmManager?.canScheduleExactAlarms() == true
    }
  }

  LaunchedEffect(Unit) {
    if (!requestedInitialPermissions) {
      requestedInitialPermissions = true
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifGranted) {
        runCatching { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
      }
    }
  }

  Dialog(
    onDismissRequest = {},
    properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(DarkBackground)
        .safeDrawingPadding()
        .testTag("alarm_onboarding_screen")
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(GoldPrimary.copy(alpha = 0.14f))
            .border(1.5.dp, GoldPrimary.copy(alpha = 0.5f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Alarm, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(38.dp))
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = "Enable Reliable Class Alarms",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "ClassRep rings alarms 15 minutes before and at the start of every registered class, even offline or on silent.",
          fontSize = 14.sp,
          color = TextSecondary,
          textAlign = TextAlign.Center,
          lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        ClassRepCard {
          PermissionStepRow(
            icon = Icons.Default.NotificationsActive,
            title = "1. Notification Permission",
            subtitle = if (notifGranted) "Allowed" else "Needed so class alarm alerts can appear",
            granted = notifGranted,
            actionLabel = if (notifGranted) null else "Allow",
            onAction = {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
              }
            }
          )
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            HorizontalDivider(color = DarkBorder)
            PermissionStepRow(
              icon = Icons.Default.Alarm,
              title = "2. Exact Alarm Permission",
              subtitle = if (exactGranted) "Allowed" else "Needed so alarms fire on the exact minute",
              granted = exactGranted,
              actionLabel = if (exactGranted) null else "Allow Exact",
              onAction = {
                com.example.alarms.AlarmNavigationHelper.openExactAlarmSettings(context)
                exactGranted = alarmManager?.canScheduleExactAlarms() == true
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        ClassRepCard {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Turn Off Battery Optimization",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = GoldLight
            )
          }
          Text(
            text = "Android phones may pause background alarms to save battery. Open Battery Optimization settings, find ClassRep, and select Unrestricted / Don't optimize so your morning and afternoon classes always ring on time.",
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 19.sp
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedButton(
            onClick = { com.example.alarms.AlarmNavigationHelper.openBatteryOptimizationSettings(context) },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("onboarding_battery_btn"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, GoldPrimary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight)
          ) {
            Text("Open Battery Optimization Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
          text = "Continue to ClassRep",
          onClick = onComplete,
          modifier = Modifier.fillMaxWidth().testTag("onboarding_continue_btn")
        )
      }
    }
  }
}

@Composable
private fun PermissionStepRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  granted: Boolean,
  actionLabel: String?,
  onAction: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Icon(icon, contentDescription = null, tint = if (granted) SuccessGreen else GoldPrimary, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(subtitle, fontSize = 12.sp, color = if (granted) SuccessGreen else TextSecondary)
      }
    }
    if (granted) {
      Icon(Icons.Default.CheckCircle, contentDescription = "Granted", tint = SuccessGreen, modifier = Modifier.size(20.dp))
    } else if (actionLabel != null) {
      TextButton(onClick = onAction) {
        Text(actionLabel, color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    }
  }
}
