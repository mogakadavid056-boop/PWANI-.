package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.notifications.NotificationPreferences
import com.example.ui.theme.*

@Composable
fun NotificationBellButton(
  unreadCount: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(modifier = modifier.size(48.dp), contentAlignment = Alignment.Center) {
    IconButton(
      onClick = onClick,
      modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(DarkSurfaceVariant)
        .border(1.dp, DarkBorder, CircleShape)
        .testTag("notification_bell_button")
    ) {
      Icon(
        imageVector = if (unreadCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
        contentDescription = "Notifications",
        tint = if (unreadCount > 0) GoldPrimary else GoldLight,
        modifier = Modifier.size(20.dp)
      )
    }
    if (unreadCount > 0) {
      val label = if (unreadCount > 99) "99+" else unreadCount.toString()
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(top = 2.dp, end = 2.dp)
          .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
          .clip(RoundedCornerShape(9.dp))
          .background(ErrorRed)
          .padding(horizontal = 4.dp, vertical = 1.dp)
          .testTag("notification_bell_badge"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = label,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          lineHeight = 11.sp
        )
      }
    }
  }
}

@Composable
fun NotificationPermissionRationaleDialog(
  isAuthenticated: Boolean
) {
  if (!isAuthenticated || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
  val context = LocalContext.current
  val alreadyGranted = remember {
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
  }
  var showPrompt by remember(isAuthenticated) {
    mutableStateOf(!alreadyGranted && !NotificationPreferences.wasPermissionPromptDismissed(context))
  }
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
    NotificationPreferences.setPermissionPromptDismissed(context, true)
    showPrompt = false
  }

  if (showPrompt) {
    AlertDialog(
      onDismissRequest = {
        NotificationPreferences.setPermissionPromptDismissed(context, true)
        showPrompt = false
      },
      containerColor = Color(0xFF0E3324),
      title = {
        Text(
          text = "Stay updated with class alerts",
          fontWeight = FontWeight.Bold,
          color = GoldLight
        )
      },
      text = {
        Text(
          text = "Allow notifications so ClassRep can alert you when attendance registers open, urgent notices are posted, or timetable changes happen.",
          fontSize = 14.sp,
          color = TextPrimary,
          lineHeight = 20.sp
        )
      },
      confirmButton = {
        Button(
          onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
        ) {
          Text("Allow notifications", fontWeight = FontWeight.Bold, color = DarkTextOnGold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            NotificationPreferences.setPermissionPromptDismissed(context, true)
            showPrompt = false
          }
        ) {
          Text("Not now", color = TextSecondary)
        }
      }
    )
  }
}
