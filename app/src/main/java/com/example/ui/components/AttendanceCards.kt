package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AttendanceSessionItem
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardRadius
import com.example.ui.theme.CardTitleStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.MontserratFamily
import com.example.ui.theme.RoyalSurface
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SilverWhite
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

  RoyalCard(
    borderColor = MetallicGold,
    modifier = Modifier.testTag("active_session_banner")
  ) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        "ACTIVE ATTENDANCE CODE",
        style = FieldLabelStyle.copy(color = ChampagneGold, letterSpacing = 1.4.sp)
      )
      Spacer(modifier = Modifier.height(10.dp))

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(14.dp))
          .background(RoyalSurface)
          .border(1.5.dp, MetallicGold, RoundedCornerShape(14.dp))
          .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = sessionCode,
          fontFamily = MontserratFamily,
          fontSize = 56.sp,
          fontWeight = FontWeight.Bold,
          color = ChampagneGold,
          letterSpacing = 6.sp,
          modifier = Modifier.testTag("large_attendance_code")
        )
      }
      Spacer(modifier = Modifier.height(8.dp))

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(
          Icons.Default.HourglassBottom,
          contentDescription = null,
          tint = if (remainingSeconds > 0) ChampagneGold else ErrorRed,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (remainingSeconds > 0) "$timeStr remaining" else "Session expired",
          style = CaptionStyle.copy(
            fontWeight = FontWeight.Medium,
            color = if (remainingSeconds > 0) Lavender else ErrorRed
          )
        )
      }
      Spacer(modifier = Modifier.height(14.dp))

      GoldButton(
        text = "View Records",
        leadingIcon = Icons.Default.Visibility,
        onClick = onViewAttendance,
        height = 48.dp,
        testTag = "view_active_session_attendance_btn"
      )

      if (onCloseSession != null || onVoidSession != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (onCloseSession != null) {
            OutlinedButton(
              onClick = onCloseSession,
              modifier = Modifier.weight(1f).height(48.dp).testTag("active_banner_close_btn"),
              shape = RoundedCornerShape(ButtonRadius),
              border = BorderStroke(1.5.dp, MetallicGold),
              colors = ButtonDefaults.outlinedButtonColors(containerColor = RoyalSurface, contentColor = ChampagneGold)
            ) {
              Text("Close", style = ButtonTextStyle.copy(color = ChampagneGold), maxLines = 1, softWrap = false)
            }
          }
          if (onVoidSession != null) {
            OutlinedButton(
              onClick = onVoidSession,
              modifier = Modifier.weight(1f).height(48.dp).testTag("active_banner_void_btn"),
              shape = RoundedCornerShape(ButtonRadius),
              border = BorderStroke(1.5.dp, ErrorRed),
              colors = ButtonDefaults.outlinedButtonColors(containerColor = RoyalSurface, contentColor = ErrorRed)
            ) {
              Text("Void", style = ButtonTextStyle.copy(color = ErrorRed), maxLines = 1, softWrap = false)
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
  RoyalCard(modifier = Modifier.testTag("staff_session_item")) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(RoyalSurfaceHigh),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.People, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          val startedDisplay = NairobiTimeHelper.formatIsoToNairobi(session.startsAt)
          Text("Session: $startedDisplay", style = CardTitleStyle.copy(fontSize = 16.sp, color = SilverWhite))
          Text(
            if (session.active) "Active session" else "Completed",
            style = CaptionStyle.copy(color = if (session.active) ChampagneGold else Lavender)
          )
        }
        if (onVoidSession != null) {
          IconButton(onClick = onVoidSession, modifier = Modifier.size(44.dp).testTag("void_session_btn_${session.id}")) {
            Icon(Icons.Default.Block, contentDescription = "Void Session", tint = ErrorRed, modifier = Modifier.size(18.dp))
          }
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (session.active && onShowCode != null) {
          Button(
            onClick = onShowCode,
            modifier = Modifier.weight(1f).height(44.dp).testTag("show_code_btn_${session.id}"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight)
          ) { Text("Code", style = ButtonTextStyle.copy(color = Midnight, fontSize = 14.sp), maxLines = 1, softWrap = false) }
        }
        if (session.active && onCloseSession != null) {
          OutlinedButton(
            onClick = onCloseSession,
            modifier = Modifier.weight(1f).height(44.dp).testTag("close_session_btn_${session.id}"),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.5.dp, MetallicGold),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = RoyalSurface, contentColor = ChampagneGold)
          ) { Text("Close", style = ButtonTextStyle.copy(color = ChampagneGold, fontSize = 14.sp), maxLines = 1, softWrap = false) }
        }
        OutlinedButton(
          onClick = onViewAttendance,
          modifier = Modifier.weight(1f).height(44.dp).testTag("view_session_btn_${session.id}"),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.5.dp, DarkBorder),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = RoyalSurface, contentColor = ChampagneGold)
        ) { Text("View", style = ButtonTextStyle.copy(color = ChampagneGold, fontSize = 14.sp), maxLines = 1, softWrap = false) }
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

  RoyalCard(
    borderColor = MetallicGold.copy(alpha = 0.7f),
    modifier = Modifier
      .then(if (!isOnline) Modifier.alpha(0.6f) else Modifier)
      .testTag("student_open_session_item")
  ) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(RoyalSurfaceHigh),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.TouchApp, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text("Active Attendance Session", style = CardTitleStyle.copy(color = ChampagneGold))
          Text("Enter code to sign in or sign out", style = CaptionStyle.copy(color = Lavender))
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
          onClick = { if (!isOnline) OfflineToastHelper.show(context) else onSign() },
          modifier = Modifier.weight(1f).height(48.dp).testTag("sign_open_session_btn"),
          shape = RoundedCornerShape(ButtonRadius),
          colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight)
        ) {
          Text("Sign", style = ButtonTextStyle.copy(color = Midnight), maxLines = 1, softWrap = false)
        }
        if (onSignOut != null) {
          OutlinedButton(
            onClick = { if (!isOnline) OfflineToastHelper.show(context) else onSignOut() },
            modifier = Modifier.weight(1f).height(48.dp).testTag("student_sign_out_btn"),
            shape = RoundedCornerShape(ButtonRadius),
            border = BorderStroke(1.5.dp, MetallicGold),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = RoyalSurface, contentColor = ChampagneGold)
          ) {
            Text("Sign out", style = ButtonTextStyle.copy(color = ChampagneGold), maxLines = 1, softWrap = false)
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
