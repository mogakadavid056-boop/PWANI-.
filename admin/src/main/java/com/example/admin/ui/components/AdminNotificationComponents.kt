package com.example.admin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.ui.theme.AdminCaptionStyle
import com.example.admin.ui.theme.AdminCardElevated
import com.example.admin.ui.theme.AdminSectionHeadingStyle
import com.example.admin.ui.theme.ChampagneGold
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.Lavender
import com.example.admin.ui.theme.MontserratFamily
import com.example.admin.ui.theme.SilverWhite

@Composable
fun AdminNotificationBellButton(
  unreadCount: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(modifier = modifier) {
    IconButton(
      onClick = onClick,
      modifier = Modifier.size(48.dp).testTag("admin_notification_bell")
    ) {
      Icon(
        imageVector = Icons.Default.Notifications,
        contentDescription = if (unreadCount > 0) "$unreadCount unread notifications" else "Notifications",
        tint = if (unreadCount > 0) GoldPrimary else ChampagneGold
      )
    }
    if (unreadCount > 0) {
      val label = if (unreadCount > 99) "99+" else unreadCount.toString()
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .offset(x = (-4).dp, y = 4.dp)
          .size(width = if (unreadCount > 9) 22.dp else 18.dp, height = 18.dp)
          .clip(CircleShape)
          .background(CrimsonError)
          .padding(horizontal = 3.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = label,
          fontFamily = MontserratFamily,
          fontSize = 10.sp,
          fontWeight = FontWeight.SemiBold,
          color = SilverWhite,
          maxLines = 1
        )
      }
    }
  }
}

@Composable
fun AdminPermissionRationaleDialog(
  onAllow: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = AdminCardElevated,
    shape = RoundedCornerShape(18.dp),
    title = {
      Text("Enable Admin Alerts?", style = AdminSectionHeadingStyle)
    },
    text = {
      Text(
        "Allow notifications so ClassRep Admin can alert you immediately when lecturers register, request to join a class, submit new units, or when students reach critical attendance risk.",
        style = AdminCaptionStyle.copy(color = SilverWhite.copy(alpha = 0.9f))
      )
    },
    confirmButton = {
      Button(
        onClick = onAllow,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) { Text("Allow Notifications", fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold) }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Not now", fontFamily = MontserratFamily, color = Lavender) }
    }
  )
}
