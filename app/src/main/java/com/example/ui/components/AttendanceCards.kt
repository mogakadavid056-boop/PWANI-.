package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceSessionItem
import com.example.ui.theme.CardRadius
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ActiveSessionBanner(
  sessionCode: String,
  remainingSeconds: Long,
  onViewAttendance: () -> Unit
) {
  val mins = remainingSeconds / 60
  val secs = remainingSeconds % 60
  val timeStr = String.format("%02d:%02d", mins, secs)

  ClassRepCard(
    modifier = Modifier
      .border(2.dp, GoldPrimary, RoundedCornerShape(CardRadius))
      .testTag("active_session_banner")
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "ACTIVE ATTENDANCE CODE",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = GoldLight,
        letterSpacing = 1.sp
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Very large monospace code
      Text(
        text = sessionCode,
        fontSize = 46.sp,
        fontWeight = FontWeight.ExtraBold,
        color = GoldPrimary,
        letterSpacing = 8.sp,
        modifier = Modifier.testTag("large_attendance_code")
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Countdown below the code
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(
          Icons.Default.HourglassBottom,
          contentDescription = null,
          tint = if (remainingSeconds > 0) GoldPrimary else ErrorRed,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (remainingSeconds > 0) "$timeStr remaining" else "Session expired",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = if (remainingSeconds > 0) GoldLight else ErrorRed
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      PrimaryButton(
        text = "View Attendance List",
        leadingIcon = Icons.Default.Visibility,
        onClick = onViewAttendance,
        modifier = Modifier.fillMaxWidth().testTag("view_active_session_attendance_btn")
      )
    }
  }
}

@Composable
fun StaffSessionCard(
  session: AttendanceSessionItem,
  onViewAttendance: () -> Unit,
  onDeleteSession: (() -> Unit)? = null
) {
  ClassRepCard(
    modifier = Modifier.testTag("staff_session_item")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.People, contentDescription = null, tint = GoldLight, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        val startedDisplay = session.startsAt.replace("T", " ").substringBefore(".")
        Text(text = "Session: $startedDisplay", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(
          text = if (session.active) "Active session" else "Completed",
          fontSize = 11.sp,
          color = if (session.active) GoldPrimary else TextSecondary
        )
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
          onClick = onViewAttendance,
          modifier = Modifier.height(34.dp).testTag("view_session_btn_${session.id}"),
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = GoldLight)
        ) {
          Text("View", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (onDeleteSession != null) {
          Spacer(modifier = Modifier.width(6.dp))
          IconButton(
            onClick = onDeleteSession,
            modifier = Modifier.size(32.dp).testTag("delete_session_btn_${session.id}")
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Session", tint = ErrorRed, modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }
}

@Composable
fun StudentOpenSessionCard(session: AttendanceSessionItem, onSign: () -> Unit) {
  ClassRepCard(
    modifier = Modifier
      .border(1.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(CardRadius))
      .testTag("student_open_session_item"),
    onClick = onSign
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.TouchApp, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text("Active Attendance Session", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldLight)
        Text("Tap here to enter code and sign", fontSize = 11.sp, color = TextSecondary)
      }
      PrimaryButton(
        text = "Sign",
        onClick = onSign,
        modifier = Modifier.height(36.dp).testTag("sign_open_session_btn")
      )
    }
  }
}

@Composable
fun EmptyAttendanceState(message: String) {
  EmptyState(
    icon = Icons.Default.Schedule,
    title = "No Attendance Sessions",
    message = message
  )
}
