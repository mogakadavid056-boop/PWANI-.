package com.example.admin.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.AdminAttendancePdfExporter
import com.example.admin.data.AdminAttendanceSession
import com.example.admin.data.AdminCourseUnit
import com.example.admin.ui.AdminMonitorExtraState
import com.example.admin.ui.AdminMonitorExtraViewModel
import com.example.admin.ui.components.AdminConfirmDialog
import com.example.admin.ui.components.AdminEmptyState
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminOptionPickerDialog
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.AdminBorder
import com.example.admin.ui.theme.AdminCardElevated
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.TextMuted
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary
import com.example.admin.util.AdminJsonHelper

@Composable
fun AdminMonitorAttendanceTab(
  classId: String,
  className: String,
  units: List<AdminCourseUnit>,
  extraState: AdminMonitorExtraState,
  extraVm: AdminMonitorExtraViewModel,
  onOpenClassRisk: () -> Unit
) {
  val context = LocalContext.current
  var showStartDialog by remember { mutableStateOf(false) }
  var deleteTarget by remember { mutableStateOf<AdminAttendanceSession?>(null) }

  val selectedSession = extraState.selectedSession
  if (selectedSession != null) {
    BackHandler(onBack = extraVm::closeSessionDetail)
    val title = selectedSession.sessionTitle.ifBlank { selectedSession.courseTitle.ifBlank { "Attendance" } }
    Column(modifier = Modifier.fillMaxSize()) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = extraVm::closeSessionDetail) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldPrimary)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
          Text("${extraState.attendanceRecords.size} signed • ${AdminJsonHelper.orDash(selectedSession.venueText)}", fontSize = 12.sp, color = TextMuted)
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = { AdminAttendancePdfExporter.shareAttendanceListCsv(context, title, extraState.attendanceRecords) },
          modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Download, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Export CSV", fontSize = 12.sp, color = GoldLight)
        }
        Button(
          onClick = { AdminAttendancePdfExporter.shareAttendanceListPdf(context, title, className, extraState.attendanceRecords) },
          modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
        ) {
          Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Export PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      if (extraState.attendanceRecords.isEmpty()) {
        AdminEmptyState(Icons.Default.FactCheck, "No signatures yet", "Students who sign in will appear here.")
      } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
          itemsIndexed(
            items = extraState.attendanceRecords,
            key = { idx, rec -> "${rec.admissionNumber}_${rec.timeSigned}_$idx" }
          ) { _, rec ->
            AdminGlassCard {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(AdminJsonHelper.orDash(rec.fullName), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                  Text("Adm: ${AdminJsonHelper.orDash(rec.admissionNumber)}", fontSize = 12.sp, color = TextMuted)
                }
                Text(AdminJsonHelper.orDash(rec.timeSigned.replace("T", " ").take(16)), fontSize = 11.sp, color = GoldLight)
              }
            }
          }
        }
      }
    }
    return
  }

  val openSession = extraState.sessions.firstOrNull { it.active || it.status.equals("open", true) }
  Column(modifier = Modifier.fillMaxSize()) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(onClick = onOpenClassRisk, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
        Icon(Icons.Default.Analytics, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Attendance Tracker", fontSize = 12.sp, color = GoldLight)
      }
      Button(
        onClick = { showStartDialog = true },
        enabled = !extraState.isActionLoading,
        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Start Session", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }

    if (openSession != null) {
      Spacer(modifier = Modifier.height(10.dp))
      AdminGlassCard(borderColor = GoldPrimary) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Column(modifier = Modifier.weight(1f)) {
            AdminStatusPill("LIVE SESSION", EmeraldSuccess)
            Spacer(modifier = Modifier.height(4.dp))
            Text(openSession.sessionTitle.ifBlank { openSession.courseTitle.ifBlank { "Active Session" } }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Code refreshes every 60s • Signed: ${extraState.attendanceRecords.size}", fontSize = 12.sp, color = TextSecondary)
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(extraState.liveCode.ifBlank { "----" }, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GoldPrimary, letterSpacing = 2.sp)
            TextButton(onClick = { extraVm.closeAttendanceSession(openSession.id) }) {
              Icon(Icons.Default.Lock, contentDescription = null, tint = CrimsonError, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("Close", color = CrimsonError, fontSize = 12.sp)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    if (extraState.sessions.isEmpty()) {
      AdminEmptyState(Icons.Default.FactCheck, "No attendance sessions", "Tap Start Session to open attendance for $className.")
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        items(extraState.sessions, key = { it.id }) { s ->
          val open = s.active || s.status.equals("open", true)
          AdminGlassCard(onClick = { extraVm.openSession(s) }) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text(s.sessionTitle.ifBlank { s.courseTitle.ifBlank { "Attendance Session" } }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                val sub = listOf(s.courseCode, s.lecturerName, s.venueText).filter { it.isNotBlank() }.joinToString(" • ")
                if (sub.isNotBlank()) Text(sub, fontSize = 12.sp, color = TextMuted)
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                AdminStatusPill(if (open) "OPEN" else s.status.uppercase(), if (open) EmeraldSuccess else GoldLight)
                IconButton(onClick = { deleteTarget = s }) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete session", tint = CrimsonError, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      }
    }
  }

  if (showStartDialog) {
    StartSessionDialog(units, extraState.isActionLoading, { cid, title, mins -> extraVm.startSession(cid, title, mins); showStartDialog = false }) { showStartDialog = false }
  }
  deleteTarget?.let { target ->
    AdminConfirmDialog("Delete Attendance Session?", "Provide a reason for deleting this session:", "Delete",
      isDestructive = true, isActionLoading = extraState.isActionLoading, requireReason = true, reasonLabel = "Reason for deletion",
      onConfirm = { reason -> extraVm.deleteAttendanceSession(target.id, reason); deleteTarget = null }, onDismiss = { deleteTarget = null })
  }
}

@Composable
private fun StartSessionDialog(units: List<AdminCourseUnit>, loading: Boolean, onStart: (String?, String, Int) -> Unit, onDismiss: () -> Unit) {
  val approved = remember(units) { units.filter { it.isApproved } }
  var selectedUnit by remember { mutableStateOf(approved.firstOrNull()) }
  var title by remember { mutableStateOf("") }
  var minutes by remember { mutableIntStateOf(60) }
  var pickUnit by remember { mutableStateOf(false) }
  val fc = OutlinedTextFieldDefaults.colors(focusedContainerColor = AdminSurface, unfocusedContainerColor = AdminSurface, focusedBorderColor = GoldPrimary, unfocusedBorderColor = AdminBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)

  AlertDialog(
    onDismissRequest = { if (!loading) onDismiss() }, containerColor = AdminCardElevated, shape = RoundedCornerShape(16.dp),
    title = { Text("Start Attendance Session", fontWeight = FontWeight.Bold, color = GoldLight) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (approved.isNotEmpty()) {
          OutlinedButton(onClick = { pickUnit = true }, modifier = Modifier.fillMaxWidth()) { Text("Unit: ${selectedUnit?.displayLabel ?: "None"}", color = GoldLight) }
        }
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Session Title") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(15, 30, 60, 90).forEach { m ->
            FilterChip(selected = minutes == m, onClick = { minutes = m }, label = { Text("${m}m", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPrimary, selectedLabelColor = DarkTextOnGold, containerColor = AdminSurface, labelColor = TextSecondary))
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = { onStart(selectedUnit?.courseId, title.ifBlank { selectedUnit?.displayLabel ?: "Lecture" }, minutes) },
        enabled = !loading && (title.isNotBlank() || selectedUnit != null),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)) { Text("Start") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
  )
  if (pickUnit) AdminOptionPickerDialog("Select Unit", approved, { it.courseId == selectedUnit?.courseId }, { it.displayLabel }, onSelect = { selectedUnit = it; pickUnit = false }) { pickUnit = false }
}
