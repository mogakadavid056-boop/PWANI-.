package com.example.ui.components

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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import com.example.ui.theme.DarkSurfaceVariant
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceSessionItem
import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AttendanceSection(
  isStaff: Boolean,
  isLoading: Boolean,
  staffSessions: List<AttendanceSessionItem>,
  openSessions: List<AttendanceSessionItem>,
  activeSessionCode: String?,
  activeSessionId: String?,
  activeRemainingSeconds: Long,
  onStartAttendanceClick: () -> Unit,
  onRefreshSessions: () -> Unit,
  onOpenViewAttendance: (sessionId: String) -> Unit,
  onOpenSignAttendance: (sessionId: String) -> Unit,
  onOpenAttendanceSummary: () -> Unit = {},
  onOpenMyAttendance: () -> Unit = {},
  onDeleteSession: (AttendanceSessionItem) -> Unit = {}
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "Attendance",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          modifier = Modifier.testTag("attendance_section_title")
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isStaff) "(${staffSessions.size})" else "(${openSessions.size} active)",
          fontSize = 14.sp,
          color = TextSecondary
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onRefreshSessions,
          modifier = Modifier.size(36.dp).testTag("refresh_attendance_btn")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GoldLight, modifier = Modifier.size(20.dp))
        }

        if (isStaff) {
          Spacer(modifier = Modifier.width(4.dp))
          Button(
            onClick = onOpenAttendanceSummary,
            modifier = Modifier.height(36.dp).testTag("view_attendance_summary_btn"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = GoldLight)
          ) {
            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldLight)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.width(6.dp))
          val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
          val context = LocalContext.current
          Button(
            onClick = {
              if (!isOnline) {
                OfflineToastHelper.show(context)
              } else {
                onStartAttendanceClick()
              }
            },
            modifier = Modifier.height(36.dp)
              .then(if (!isOnline) Modifier.alpha(0.5f) else Modifier)
              .testTag("start_attendance_btn"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = DarkTextOnGold)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Start", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (isStaff && !activeSessionCode.isNullOrBlank()) {
      ActiveSessionBanner(
        sessionCode = activeSessionCode,
        remainingSeconds = activeRemainingSeconds,
        onViewAttendance = {
          if (!activeSessionId.isNullOrBlank()) onOpenViewAttendance(activeSessionId)
        }
      )
      Spacer(modifier = Modifier.height(16.dp))
    }

    if (isLoading) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LoadingSkeleton(height = 85)
        LoadingSkeleton(height = 85)
      }
    } else if (isStaff) {
      if (staffSessions.isEmpty()) {
        EmptyAttendanceState(message = "No attendance sessions created yet. Tap Start Attendance above to begin.")
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          staffSessions.forEach { session ->
            StaffSessionCard(
              session = session,
              onViewAttendance = { onOpenViewAttendance(session.id) },
              onDeleteSession = { onDeleteSession(session) }
            )
          }
        }
      }
    } else {
      ClassRepCard(
        modifier = Modifier.fillMaxWidth().testTag("student_my_attendance_card"),
        onClick = onOpenMyAttendance
      ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Assessment, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text("My Attendance Report", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("View your record and export PDF / CSV", fontSize = 11.sp, color = TextSecondary)
          }
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (openSessions.isEmpty()) {
        EmptyAttendanceState(message = if (isStaff) "No open attendance sessions right now. Tap Start Attendance above to create one." else "Nothing here yet. Waiting for your class prefect to update this.")
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          openSessions.forEach { session ->
            StudentOpenSessionCard(session = session, onSign = { onOpenSignAttendance(session.id) })
          }
        }
      }
    }
  }
}
