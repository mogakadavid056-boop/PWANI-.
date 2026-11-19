package com.example.admin.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.AdminAtRiskStudentItem
import com.example.admin.data.AdminStudentTimelineItem
import com.example.admin.notifications.AdminNotificationTimeFormatter
import com.example.admin.ui.components.AdminConfirmDialog
import com.example.admin.ui.components.AdminEmptyState
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.AmberWarning
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.InfoBlue
import com.example.admin.ui.theme.TextMuted
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun AdminStudentTimelineScreen(
  student: AdminAtRiskStudentItem,
  timeline: List<AdminStudentTimelineItem>,
  isLoading: Boolean,
  isActionLoading: Boolean,
  onBack: () -> Unit,
  onSetExcuse: (String, Boolean, String?) -> Unit
) {
  BackHandler(onBack = onBack)
  var excuseTarget by remember { mutableStateOf<AdminStudentTimelineItem?>(null) }
  var removeExcuseTarget by remember { mutableStateOf<AdminStudentTimelineItem?>(null) }

  val lvlColor = riskColor(student.riskLevel)
  val pct = String.format(Locale.US, "%.1f%%", student.percentage)

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to risk list", tint = GoldPrimary)
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(student.fullName, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("${student.className} • Adm: ${student.admissionNumber}", fontSize = 12.sp, color = GoldLight)
      }
      AdminStatusPill(student.riskLevel.label.uppercase(), lvlColor)
    }

    Spacer(modifier = Modifier.height(8.dp))
    AdminGlassCard(borderColor = lvlColor.copy(alpha = 0.6f)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
          Text("Attendance Rate", fontSize = 11.sp, color = TextMuted)
          Text("$pct (${student.attended}/${student.totalSessions})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Column(horizontalAlignment = Alignment.End) {
          Text("Excused • Miss Streak", fontSize = 11.sp, color = TextMuted)
          Text("${student.excused} excused • ${student.missStreak} streak", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
        }
      }
    }

    if (isLoading) {
      Spacer(modifier = Modifier.height(6.dp))
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = GoldPrimary)
    }

    Spacer(modifier = Modifier.height(10.dp))
    if (timeline.isEmpty() && !isLoading) {
      AdminEmptyState(Icons.Default.FactCheck, "No session history", "Attendance sessions for this student appear here.")
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        items(timeline, key = { it.sessionId }) { row ->
          val stColor = when (row.status) {
            "present" -> EmeraldSuccess
            "excused" -> InfoBlue
            else -> CrimsonError
          }
          AdminGlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text(row.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                val ts = AdminNotificationTimeFormatter.formatRelative(row.startsAt)
                if (ts.isNotBlank()) Text(ts, fontSize = 12.sp, color = TextMuted)
                if (row.excuseReason.isNotBlank()) {
                  Spacer(modifier = Modifier.height(3.dp))
                  Text("Reason: ${row.excuseReason}", fontSize = 12.sp, color = InfoBlue)
                }
              }
              AdminStatusPill(row.status.uppercase(), stColor)
            }
            if (row.status == "absent") {
              Spacer(modifier = Modifier.height(8.dp))
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                  onClick = { excuseTarget = row },
                  enabled = !isActionLoading,
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, GoldPrimary),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                  Text("Excuse Absence", fontSize = 12.sp, color = GoldLight, fontWeight = FontWeight.SemiBold)
                }
              }
            } else if (row.status == "excused") {
              Spacer(modifier = Modifier.height(8.dp))
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                  onClick = { removeExcuseTarget = row },
                  enabled = !isActionLoading,
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, AmberWarning),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                  Text("Remove Excuse", fontSize = 12.sp, color = AmberWarning)
                }
              }
            }
          }
        }
      }
    }
  }

  excuseTarget?.let { item ->
    AdminConfirmDialog(
      title = "Excuse Absence",
      message = "Mark ${student.fullName} as excused for \"${item.title}\"?",
      confirmLabel = "Excuse",
      isActionLoading = isActionLoading,
      optionalReason = true,
      reasonLabel = "Reason (e.g. Medical note)",
      onConfirm = { r -> onSetExcuse(item.sessionId, true, r.ifBlank { null }); excuseTarget = null },
      onDismiss = { excuseTarget = null }
    )
  }

  removeExcuseTarget?.let { item ->
    AdminConfirmDialog(
      title = "Remove Excuse?",
      message = "Revert \"${item.title}\" back to Absent?",
      confirmLabel = "Remove",
      isDestructive = true,
      isActionLoading = isActionLoading,
      onConfirm = { onSetExcuse(item.sessionId, false, null); removeExcuseTarget = null },
      onDismiss = { removeExcuseTarget = null }
    )
  }
}
