package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AttendanceSessionItem
import com.example.ui.theme.*
import com.example.util.NairobiTimeHelper

@Composable
fun ActiveSessionBanner(
  sessionCode: String,
  remainingSeconds: Long,
  onViewAttendance: () -> Unit,
  onCloseSession: (() -> Unit)? = null,
  onVoidSession: (() -> Unit)? = null
) {
  val mins = remainingSeconds / 60
  val secs = remainingSeconds % 60
  val timeStr = String.format("%02d:%02d", mins, secs)

  ClassRepCard(
    modifier = Modifier
      .border(2.dp, GoldPrimary, RoundedCornerShape(CardRadius))
      .testTag("active_session_banner")
  ) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
      Text("ACTIVE ATTENDANCE CODE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldLight, letterSpacing = 1.sp)
      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = sessionCode,
        fontSize = 44.sp,
        fontWeight = FontWeight.ExtraBold,
        color = GoldPrimary,
        letterSpacing = 8.sp,
        modifier = Modifier.testTag("large_attendance_code")
      )
      Spacer(modifier = Modifier.height(6.dp))

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = if (remainingSeconds > 0) GoldPrimary else ErrorRed, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (remainingSeconds > 0) "$timeStr remaining" else "Session expired",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = if (remainingSeconds > 0) GoldLight else ErrorRed
        )
      }
      Spacer(modifier = Modifier.height(14.dp))

      Button(
        onClick = onViewAttendance,
        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("view_active_session_attendance_btn"),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) {
        Icon(Icons.Default.Visibility, contentDescription = null, tint = DarkTextOnGold, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("View Records", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold, maxLines = 1, softWrap = false)
      }

      if (onCloseSession != null || onVoidSession != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (onCloseSession != null) {
            OutlinedButton(
              onClick = onCloseSession,
              modifier = Modifier.weight(1f).height(42.dp).testTag("active_banner_close_btn"),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.5.dp, GoldPrimary),
              colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldLight)
            ) {
              Text("Close", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldLight, maxLines = 1, softWrap = false)
            }
          }
          if (onVoidSession != null) {
            OutlinedButton(
              onClick = onVoidSession,
              modifier = Modifier.weight(1f).height(42.dp).testTag("active_banner_void_btn"),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.5.dp, ErrorRed),
              colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = ErrorRed)
            ) {
              Text("Void", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ErrorRed, maxLines = 1, softWrap = false)
            }
          }
        }
      }
    }
  }
}

@Composable
fun StaffSessionCard(
  session: AttendanceSessionItem,
  onViewAttendance: () -> Unit,
  onShowCode: (() -> Unit)? = null,
  onCloseSession: (() -> Unit)? = null,
  onVoidSession: (() -> Unit)? = null
) {
  ClassRepCard(modifier = Modifier.testTag("staff_session_item")) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant), contentAlignment = Alignment.Center) {
          Icon(Icons.Default.People, contentDescription = null, tint = GoldLight, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          val startedDisplay = NairobiTimeHelper.formatIsoToNairobi(session.startsAt)
          Text("Session: $startedDisplay", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
          Text(if (session.active) "Active session" else "Completed", fontSize = 12.sp, color = if (session.active) GoldPrimary else TextSecondary)
        }
        if (onVoidSession != null) {
          IconButton(onClick = onVoidSession, modifier = Modifier.size(36.dp).testTag("void_session_btn_${session.id}")) {
            Icon(Icons.Default.Block, contentDescription = "Void Session", tint = ErrorRed, modifier = Modifier.size(18.dp))
          }
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (session.active && onShowCode != null) {
          Button(
            onClick = onShowCode,
            modifier = Modifier.weight(1f).height(40.dp).testTag("show_code_btn_${session.id}"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
          ) { Text("Code", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold, maxLines = 1, softWrap = false) }
        }
        if (session.active && onCloseSession != null) {
          OutlinedButton(
            onClick = onCloseSession,
            modifier = Modifier.weight(1f).height(40.dp).testTag("close_session_btn_${session.id}"),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.5.dp, GoldPrimary),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldLight)
          ) { Text("Close", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldLight, maxLines = 1, softWrap = false) }
        }
        OutlinedButton(
          onClick = onViewAttendance,
          modifier = Modifier.weight(1f).height(40.dp).testTag("view_session_btn_${session.id}"),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.5.dp, GoldPrimary),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldLight)
        ) { Text("View", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldLight, maxLines = 1, softWrap = false) }
      }
    }
  }
}

@Composable
fun StudentOpenSessionCard(
  session: AttendanceSessionItem,
  onSign: () -> Unit,
  onSignOut: (() -> Unit)? = null
) {
  val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
  val context = LocalContext.current

  ClassRepCard(
    modifier = Modifier
      .border(1.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(CardRadius))
      .then(if (!isOnline) Modifier.alpha(0.6f) else Modifier)
      .testTag("student_open_session_item")
  ) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant), contentAlignment = Alignment.Center) {
          Icon(Icons.Default.TouchApp, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text("Active Attendance Session", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldLight)
          Text("Enter code to sign in or sign out", fontSize = 12.5.sp, color = TextSecondary)
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
          onClick = { if (!isOnline) OfflineToastHelper.show(context) else onSign() },
          modifier = Modifier.weight(1f).height(44.dp).testTag("sign_open_session_btn"),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
        ) {
          Text("Sign", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold, maxLines = 1, softWrap = false)
        }
        if (onSignOut != null) {
          OutlinedButton(
            onClick = { if (!isOnline) OfflineToastHelper.show(context) else onSignOut() },
            modifier = Modifier.weight(1f).height(44.dp).testTag("student_sign_out_btn"),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.5.dp, GoldPrimary),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldLight)
          ) {
            Text("Sign out", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldLight, maxLines = 1, softWrap = false)
          }
        }
      }
    }
  }
}

@Composable
fun EmptyAttendanceState(message: String) {
  EmptyState(icon = Icons.Default.Schedule, title = "No Attendance Sessions", message = message)
}
