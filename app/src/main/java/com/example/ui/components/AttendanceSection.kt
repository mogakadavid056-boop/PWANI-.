package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
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
import com.example.data.AttendanceRegisterListItem
import com.example.data.AttendanceSessionItem
import com.example.ui.theme.*

@Composable
fun AttendanceSection(
  isStaff: Boolean,
  canSignAttendance: Boolean = !isStaff,
  isLoading: Boolean,
  staffSessions: List<AttendanceSessionItem>,
  openSessions: List<AttendanceSessionItem>,
  registers: List<AttendanceRegisterListItem> = emptyList(),
  activeSessionCode: String?,
  activeSessionId: String?,
  activeRemainingSeconds: Long,
  canManageActiveSession: Boolean = false,
  onStartAttendanceClick: () -> Unit,
  onRefreshSessions: () -> Unit,
  onOpenViewAttendance: (sessionId: String) -> Unit,
  onOpenSignAttendance: (sessionId: String) -> Unit,
  onOpenAttendanceSummary: () -> Unit = {},
  onOpenMyAttendance: () -> Unit = {},
  onOpenRegister: (sessionId: String) -> Unit = {},
  onCloseSession: (sessionId: String) -> Unit = {},
  onVoidSessionPrompt: (AttendanceSessionItem) -> Unit = {},
  onDeleteRegisterPrompt: (AttendanceRegisterListItem) -> Unit = {},
  onSignOutSession: (sessionId: String) -> Unit = {},
  onShowActiveCode: ((sessionId: String, expiresAt: String) -> Unit)? = null
) {
  val context = LocalContext.current
  val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
  val hasManageRights = isStaff || registers.any { it.canManage }

  Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Attendance", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.testTag("attendance_section_title"))
        Text(
          text = if (registers.isNotEmpty()) "${registers.size} registers"
          else if (hasManageRights) "${staffSessions.size} sessions"
          else "${openSessions.size} active",
          fontSize = 13.sp,
          color = TextSecondary
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        IconButton(onClick = onRefreshSessions, modifier = Modifier.size(38.dp).testTag("refresh_attendance_btn")) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GoldLight, modifier = Modifier.size(20.dp))
        }

        if (hasManageRights) {
          OutlinedButton(
            onClick = onOpenAttendanceSummary,
            modifier = Modifier.height(38.dp).testTag("view_attendance_summary_btn"),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.5.dp, GoldPrimary),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldLight),
            contentPadding = PaddingValues(horizontal = 10.dp)
          ) {
            Text("Summary", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldLight, maxLines = 1, softWrap = false)
          }

          Button(
            onClick = { if (!isOnline) OfflineToastHelper.show(context) else onStartAttendanceClick() },
            modifier = Modifier.height(38.dp).then(if (!isOnline) Modifier.alpha(0.5f) else Modifier).testTag("start_attendance_btn"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
            contentPadding = PaddingValues(horizontal = 12.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp), tint = DarkTextOnGold)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Start", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold, maxLines = 1, softWrap = false)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    val activeRegCanManage = isStaff && (canManageActiveSession ||
      (!activeSessionId.isNullOrBlank() && registers.any { it.sessionId == activeSessionId && it.canManage }))
    if (activeRegCanManage && !activeSessionCode.isNullOrBlank()) {
      ActiveSessionBanner(
        sessionCode = activeSessionCode,
        remainingSeconds = activeRemainingSeconds,
        onViewAttendance = { if (!activeSessionId.isNullOrBlank()) onOpenViewAttendance(activeSessionId) },
        onCloseSession = { if (!activeSessionId.isNullOrBlank()) onCloseSession(activeSessionId) },
        onVoidSession = {
          val cur = staffSessions.find { it.id == activeSessionId } ?: AttendanceSessionItem(activeSessionId.orEmpty(), "", "")
          onVoidSessionPrompt(cur)
        }
      )
      Spacer(modifier = Modifier.height(16.dp))
    }

    if (canSignAttendance && openSessions.isNotEmpty()) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        openSessions.forEach { session ->
          StudentOpenSessionCard(
            session = session,
            onSign = { onOpenSignAttendance(session.id) },
            onSignOut = { onSignOutSession(session.id) }
          )
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
    }

    if (canSignAttendance) {
      ClassRepCard(modifier = Modifier.fillMaxWidth().testTag("student_my_attendance_card"), onClick = onOpenMyAttendance) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(DarkSurfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Assessment, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text("My Attendance Report", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("View your record and export PDF / CSV", fontSize = 12.sp, color = TextSecondary)
          }
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }

    if (isLoading && registers.isEmpty() && staffSessions.isEmpty() && openSessions.isEmpty()) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LoadingSkeleton(height = 85)
        LoadingSkeleton(height = 85)
      }
    } else if (registers.isNotEmpty()) {
      Text("ATTENDANCE REGISTERS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldLight, letterSpacing = 0.8.sp)
      Spacer(modifier = Modifier.height(8.dp))
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        registers.forEach { reg ->
          AttendanceRegisterCard(
            item = reg,
            onClick = { onOpenRegister(reg.sessionId) },
            onDelete = if (hasManageRights && reg.canDelete) { { onDeleteRegisterPrompt(reg) } } else null
          )
        }
      }
    } else if (hasManageRights && staffSessions.isNotEmpty()) {
      Text("PAST SESSIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldLight)
      Spacer(modifier = Modifier.height(8.dp))
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        staffSessions.forEach { session ->
          StaffSessionCard(
            session = session,
            onViewAttendance = { onOpenViewAttendance(session.id) },
            onShowCode = if (onShowActiveCode != null) { { onShowActiveCode(session.id, session.expiresAt) } } else null,
            onCloseSession = { onCloseSession(session.id) },
            onVoidSession = { onVoidSessionPrompt(session) }
          )
        }
      }
    } else if (!hasManageRights && openSessions.isEmpty()) {
      EmptyAttendanceState(message = "No active attendance sessions right now.")
    } else {
      EmptyAttendanceState(message = "No attendance sessions created yet. Tap Start above to begin.")
    }
  }
}
