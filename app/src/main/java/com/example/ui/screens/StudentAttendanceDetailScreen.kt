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
import com.example.ui.components.RoyalCard
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

  Box(modifier = Modifier.fillMaxSize().testTag("student_attendance_detail_screen")) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(student.fullName, style = ProfileNameStyle.copy(fontSize = 20.sp))
          Text("Adm: ${student.admissionNumber}", style = CaptionStyle)
        }
        RiskBadgeChip(student.riskLevel)
      }

      Spacer(modifier = Modifier.height(10.dp))

      val pctStr = String.format(Locale.US, "%.1f%%", student.percentage)
      RoyalCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Attended ${student.attended + student.excused} of ${student.totalSessions} ($pctStr)", style = BodyStyle.copy(fontWeight = FontWeight.SemiBold))
          if (student.missStreak >= 2) {
            Text("Missed ${student.missStreak} in a row", style = CaptionStyle.copy(fontWeight = FontWeight.Bold, color = ErrorColor))
          }
        }
      }

      if (!errorMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        ErrorBanner(message = errorMessage, onDismiss = onDismissError)
      }
      if (!successMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(RoyalSuccessContainer)
            .border(1.dp, SuccessColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)).padding(10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(successMessage, style = CaptionStyle.copy(color = SuccessColor, fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
          TextButton(onClick = onDismissSuccess, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
            Text("OK", style = ButtonTextStyle.copy(color = ChampagneGold, fontSize = 14.sp))
          }
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
    "present" -> SuccessColor
    "excused" -> ElectricViolet
    else -> ErrorColor
  }
  val statusLabel = when (row.status) {
    "present" -> "Present"
    "excused" -> "Excused"
    else -> "Absent"
  }
  val formatted = NairobiTimeHelper.formatIsoToNairobi(row.startsAt)
  val dateStr = if (formatted == "-" || formatted.isBlank()) row.startsAt.take(16).replace("T", " ") else formatted

  RoyalCard {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(row.title, style = CardTitleStyle.copy(fontSize = 16.sp))
        if (dateStr.isNotBlank()) Text(dateStr, style = CaptionStyle)
      }
      Box(
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(statusColor.copy(alpha = 0.16f))
          .border(1.dp, statusColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
          .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Text(statusLabel, style = FieldLabelStyle.copy(fontSize = 12.sp, color = statusColor))
      }
    }
    if (row.status == "excused" && row.excuseReason.isNotBlank()) {
      Text("Reason: ${row.excuseReason}", style = CaptionStyle.copy(color = Lavender))
    }
    if (row.status == "absent") {
      OutlinedButton(
        onClick = onExcuse,
        enabled = !isActionLoading,
        shape = RoundedCornerShape(ButtonRadius),
        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        border = BorderStroke(1.dp, MetallicGold),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
      ) {
        Text("Excuse this absence", style = ButtonTextStyle.copy(fontSize = 14.sp, color = ChampagneGold))
      }
    } else if (row.status == "excused") {
      TextButton(onClick = onRemoveExcuse, enabled = !isActionLoading, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
        Text("Remove excuse", style = ButtonTextStyle.copy(fontSize = 14.sp, color = ErrorColor))
      }
    }
  }
}

@Composable
fun RiskBadgeChip(level: RiskLevel) {
  val color = when (level) {
    RiskLevel.CRITICAL -> ErrorColor
    RiskLevel.HIGH -> ElectricViolet
    RiskLevel.WATCH -> ChampagneGold
    else -> SuccessColor
  }
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(color.copy(alpha = 0.18f))
      .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
      .padding(horizontal = 10.dp, vertical = 4.dp)
  ) {
    Text(level.label, style = FieldLabelStyle.copy(fontSize = 12.sp, color = color))
  }
}
