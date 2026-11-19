package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RiskLevel
import com.example.data.StudentRiskItem
import com.example.data.StudentSessionTimelineItem
import com.example.ui.components.EmptyAttendanceState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LoadingSkeleton
import com.example.ui.theme.*
import com.example.util.NairobiTimeHelper
import java.util.Locale

@Composable
fun StudentAttendanceDetailScreen(
  student: StudentRiskItem,
  timeline: List<StudentSessionTimelineItem>,
  isLoading: Boolean,
  isActionLoading: Boolean,
  errorMessage: String?,
  successMessage: String?,
  onExcuseSession: (sessionId: String, reason: String) -> Unit,
  onRemoveExcuse: (sessionId: String) -> Unit,
  onDismissError: () -> Unit,
  onDismissSuccess: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)
  var sessionPendingExcuse by remember { mutableStateOf<StudentSessionTimelineItem?>(null) }

  Box(modifier = Modifier.fillMaxSize().background(DarkBackground).testTag("student_attendance_detail_screen")) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(student.fullName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldLight)
          Text("Adm: ${student.admissionNumber}", fontSize = 13.sp, color = TextSecondary)
        }
        RiskBadgeChip(student.riskLevel)
      }

      Spacer(modifier = Modifier.height(10.dp))

      val pctStr = String.format(Locale.US, "%.1f%%", student.percentage)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(DarkCard)
          .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
          .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("Attended ${student.attended + student.excused} of ${student.totalSessions} ($pctStr)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        if (student.missStreak >= 2) {
          Text("Missed ${student.missStreak} in a row", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
        }
      }

      if (!errorMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        ErrorBanner(message = errorMessage, onDismiss = onDismissError)
      }
      if (!successMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SuccessContainer)
            .border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(successMessage, fontSize = 13.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
          TextButton(onClick = onDismissSuccess) { Text("OK", color = GoldLight) }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (isLoading && timeline.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          LoadingSkeleton(height = 68)
          LoadingSkeleton(height = 68)
        }
      } else if (timeline.isEmpty()) {
        EmptyAttendanceState(message = "No recorded sessions for this class yet.")
      } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          items(timeline, key = { it.sessionId }) { row ->
            TimelineSessionCard(
              row = row,
              isActionLoading = isActionLoading,
              onExcuse = { sessionPendingExcuse = row },
              onRemoveExcuse = { onRemoveExcuse(row.sessionId) }
            )
          }
          item { Spacer(modifier = Modifier.height(16.dp)) }
        }
      }
    }

    sessionPendingExcuse?.let { target ->
      ExcuseAbsenceDialog(
        sessionTitle = target.title,
        isActionLoading = isActionLoading,
        onDismiss = { sessionPendingExcuse = null },
        onSubmit = { reason ->
          onExcuseSession(target.sessionId, reason)
          sessionPendingExcuse = null
        }
      )
    }
  }
}

@Composable
private fun TimelineSessionCard(
  row: StudentSessionTimelineItem,
  isActionLoading: Boolean,
  onExcuse: () -> Unit,
  onRemoveExcuse: () -> Unit
) {
  val statusColor = when (row.status) {
    "present" -> SuccessGreen
    "excused" -> Color(0xFF42A5F5)
    else -> ErrorRed
  }
  val statusLabel = when (row.status) {
    "present" -> "Present"
    "excused" -> "Excused"
    else -> "Absent"
  }
  val formatted = NairobiTimeHelper.formatIsoToNairobi(row.startsAt)
  val dateStr = if (formatted == "-" || formatted.isBlank()) row.startsAt.take(16).replace("T", " ") else formatted

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(DarkCard)
      .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(row.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        if (dateStr.isNotBlank()) Text(dateStr, fontSize = 13.sp, color = TextSecondary)
      }
      Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(statusColor.copy(alpha = 0.16f))
          .border(1.dp, statusColor.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
          .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Text(statusLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
      }
    }
    if (row.status == "excused" && row.excuseReason.isNotBlank()) {
      Spacer(modifier = Modifier.height(6.dp))
      Text("Reason: ${row.excuseReason}", fontSize = 13.sp, color = Color(0xFF90CAF9))
    }
    if (row.status == "absent") {
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedButton(
        onClick = onExcuse,
        enabled = !isActionLoading,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, GoldPrimary),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
      ) {
        Text("Excuse this absence", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GoldLight)
      }
    } else if (row.status == "excused") {
      Spacer(modifier = Modifier.height(8.dp))
      TextButton(onClick = onRemoveExcuse, enabled = !isActionLoading) {
        Text("Remove excuse", fontSize = 13.sp, color = ErrorRed)
      }
    }
  }
}

@Composable
fun RiskBadgeChip(level: RiskLevel) {
  val color = when (level) {
    RiskLevel.CRITICAL -> ErrorRed
    RiskLevel.HIGH -> Color(0xFFFF9800)
    RiskLevel.WATCH -> Color(0xFFFFD54F)
    else -> SuccessGreen
  }
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(color.copy(alpha = 0.18f))
      .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
      .padding(horizontal = 10.dp, vertical = 4.dp)
  ) {
    Text(level.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
  }
}
