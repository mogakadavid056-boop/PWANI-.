package com.example.admin.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.AdminAtRiskStudentItem
import com.example.admin.data.AdminAttendancePdfExporter
import com.example.admin.data.AdminClassRiskSummary
import com.example.admin.data.AdminRiskLevel
import com.example.admin.ui.AdminRiskState
import com.example.admin.ui.components.AdminConfirmDialog
import com.example.admin.ui.components.AdminEmptyState
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminNotificationBellButton
import com.example.admin.ui.components.AdminSearchField
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.AmberWarning
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.InfoBlue
import com.example.admin.ui.theme.TextMuted
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun AdminRiskScreen(
  state: AdminRiskState,
  unreadNotifications: Int,
  onRefresh: () -> Unit,
  onSetMinLevel: (AdminRiskLevel) -> Unit,
  onSearchChanged: (String) -> Unit,
  onSelectClassFilter: (AdminClassRiskSummary?) -> Unit,
  onLoadMoreClasses: () -> Unit,
  onLoadMoreStudents: () -> Unit,
  onOpenStudent: (AdminAtRiskStudentItem) -> Unit,
  onCloseStudent: () -> Unit,
  onSetExcuse: (String, Boolean, String?) -> Unit,
  onRunWarningsNow: () -> Unit,
  onOpenNotifications: () -> Unit
) {
  state.selectedStudent?.let { student ->
    AdminStudentTimelineScreen(
      student = student,
      timeline = state.timeline,
      isLoading = state.isTimelineLoading,
      isActionLoading = state.isActionLoading,
      onBack = onCloseStudent,
      onSetExcuse = onSetExcuse
    )
    return
  }

  val context = LocalContext.current
  var showRules by remember { mutableStateOf(false) }
  var confirmWarnings by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp).testTag("admin_risk_screen")) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Attendance Risk", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Early intervention & absence tracking", fontSize = 12.sp, color = TextSecondary)
      }
      Row {
        IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = "Refresh risk", tint = GoldPrimary) }
        AdminNotificationBellButton(unreadCount = unreadNotifications, onClick = onOpenNotifications)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
    AdminGlassCard(onClick = { showRules = !showRules }) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("How risk levels work", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GoldLight)
        Icon(if (showRules) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = GoldLight)
      }
      AnimatedVisibility(visible = showRules) {
        Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text("• Critical: < 60% attendance or 4+ consecutive misses", fontSize = 12.sp, color = CrimsonError)
          Text("• High: < 75% attendance or 3 consecutive misses", fontSize = 12.sp, color = AmberWarning)
          Text("• Watch: < 85% attendance or 2 consecutive misses", fontSize = 12.sp, color = InfoBlue)
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(
        onClick = { confirmWarnings = true },
        enabled = !state.isActionLoading,
        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) {
        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text("Run Warnings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
      OutlinedButton(
        onClick = {
          val label = state.selectedClassNameFilter.ifBlank { "All_Classes" }
          AdminAttendancePdfExporter.shareRiskListPdf(context, label, state.students)
        },
        enabled = state.students.isNotEmpty(),
        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
      ) {
        Icon(Icons.Default.Download, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text("Export PDF", fontSize = 12.sp, color = GoldLight)
      }
    }

    if (state.isLoadingOverview || state.isLoadingStudents) {
      Spacer(modifier = Modifier.height(6.dp))
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = GoldPrimary)
    }

    Spacer(modifier = Modifier.height(10.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
      if (state.classSummaries.isNotEmpty()) {
        item {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.classSummaries, key = { it.classId }) { cls ->
              val selected = state.selectedClassIdFilter == cls.classId
              AdminGlassCard(
                modifier = Modifier.width(220.dp),
                borderColor = if (selected) GoldPrimary else GoldPrimary.copy(alpha = 0.25f),
                onClick = { onSelectClassFilter(if (selected) null else cls) }
              ) {
                Text(cls.className, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                Text("${cls.studentCount} students • ${cls.totalSessions} sessions", fontSize = 11.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(8.dp))
                RiskDistributionBar(cls)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Crit ${cls.criticalCount} • High ${cls.highCount} • Watch ${cls.watchCount}", fontSize = 11.sp, color = TextSecondary)
              }
            }
            if (state.hasMoreClasses) {
              item {
                OutlinedButton(onClick = onLoadMoreClasses, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(92.dp)) {
                  Text("Load\nMore", color = GoldLight, fontSize = 12.sp)
                }
              }
            }
          }
        }
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (state.selectedClassIdFilter != null) {
              FilterChip(
                selected = true,
                onClick = { onSelectClassFilter(null) },
                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Clear class filter", modifier = Modifier.size(14.dp)) },
                label = { Text(state.selectedClassNameFilter.ifBlank { "Filtered Class" }, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = InfoBlue, selectedLabelColor = DarkTextOnGold)
              )
            }
            listOf(AdminRiskLevel.WATCH, AdminRiskLevel.HIGH, AdminRiskLevel.CRITICAL, AdminRiskLevel.ALL).forEach { lvl ->
              FilterChip(
                selected = state.minLevel == lvl,
                onClick = { onSetMinLevel(lvl) },
                label = { Text(if (lvl == AdminRiskLevel.ALL) "All" else "${lvl.label}+", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPrimary, selectedLabelColor = DarkTextOnGold, containerColor = AdminSurface, labelColor = TextSecondary)
              )
            }
          }
          AdminSearchField(value = state.searchQuery, onValueChange = onSearchChanged, placeholder = "Search student name or admission no...", tag = "admin_risk_search")
        }
      }

      if (state.students.isEmpty() && !state.isLoadingStudents) {
        item { AdminEmptyState(Icons.Default.Analytics, "No students in this risk tier", "Students matching this filter appear here.") }
      } else {
        items(state.students, key = { "${it.classId}_${it.studentId}" }) { stu ->
          val lvlColor = riskColor(stu.riskLevel)
          val pct = String.format(Locale.US, "%.1f%%", stu.percentage)
          AdminGlassCard(borderColor = lvlColor.copy(alpha = 0.5f), onClick = { onOpenStudent(stu) }) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text(stu.fullName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("${stu.className} • Adm: ${stu.admissionNumber}", fontSize = 12.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text("${stu.attended}/${stu.totalSessions} ($pct) • Excused: ${stu.excused} • Streak: ${stu.missStreak}", fontSize = 12.sp, color = TextSecondary)
              }
              AdminStatusPill(stu.riskLevel.label.uppercase(), lvlColor)
            }
          }
        }
        if (state.hasMoreStudents) {
          item {
            OutlinedButton(onClick = onLoadMoreStudents, enabled = !state.isLoadingMore, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(10.dp)) {
              Text(if (state.isLoadingMore) "Loading..." else "Load more students", color = GoldLight)
            }
          }
        }
      }
    }
  }

  if (confirmWarnings) {
    AdminConfirmDialog(
      title = "Run Attendance Alerts?",
      message = "Scan all classes and send automatic notifications for watch, high, and critical attendance levels?",
      confirmLabel = "Run Now",
      isActionLoading = state.isActionLoading,
      onConfirm = { onRunWarningsNow(); confirmWarnings = false },
      onDismiss = { confirmWarnings = false }
    )
  }
}

@Composable
private fun RiskDistributionBar(cls: AdminClassRiskSummary) {
  val total = (cls.criticalCount + cls.highCount + cls.watchCount + cls.okCount).coerceAtLeast(1)
  Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(AdminSurface)) {
    if (cls.criticalCount > 0) Box(modifier = Modifier.weight(cls.criticalCount.toFloat() / total).fillMaxHeight().background(CrimsonError))
    if (cls.highCount > 0) Box(modifier = Modifier.weight(cls.highCount.toFloat() / total).fillMaxHeight().background(AmberWarning))
    if (cls.watchCount > 0) Box(modifier = Modifier.weight(cls.watchCount.toFloat() / total).fillMaxHeight().background(InfoBlue))
    if (cls.okCount > 0) Box(modifier = Modifier.weight(cls.okCount.toFloat() / total).fillMaxHeight().background(EmeraldSuccess))
  }
}

fun riskColor(level: AdminRiskLevel) = when (level) {
  AdminRiskLevel.CRITICAL -> CrimsonError
  AdminRiskLevel.HIGH -> AmberWarning
  AdminRiskLevel.WATCH -> InfoBlue
  else -> EmeraldSuccess
}
