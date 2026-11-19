package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RiskLevel
import com.example.data.StudentRiskItem
import com.example.data.UserSession
import com.example.export.AttendanceRiskPdfExporter
import com.example.ui.AttendanceTrackerUiState
import com.example.ui.components.ClassRepPullToRefresh
import com.example.ui.components.EmptyAttendanceState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LoadingSkeleton
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AttendanceTrackerScreen(
  classId: String,
  className: String,
  session: UserSession?,
  uiState: AttendanceTrackerUiState,
  onRefresh: () -> Unit,
  onSelectLevel: (RiskLevel) -> Unit,
  onSearchChange: (String) -> Unit,
  onStudentClick: (StudentRiskItem) -> Unit,
  onCloseStudentDetail: () -> Unit,
  onExcuseSession: (sessionId: String, reason: String) -> Unit,
  onRemoveExcuse: (sessionId: String) -> Unit,
  onDismissError: () -> Unit,
  onDismissSuccess: () -> Unit,
  onBack: () -> Unit
) {
  val selectedStudent = uiState.selectedStudent
  if (selectedStudent != null) {
    StudentAttendanceDetailScreen(
      student = selectedStudent,
      timeline = uiState.timeline,
      isLoading = uiState.isDetailLoading,
      isActionLoading = uiState.isActionLoading,
      errorMessage = uiState.errorMessage,
      successMessage = uiState.successMessage,
      onExcuseSession = onExcuseSession,
      onRemoveExcuse = onRemoveExcuse,
      onDismissError = onDismissError,
      onDismissSuccess = onDismissSuccess,
      onBack = onCloseStudentDetail
    )
    return
  }

  BackHandler(onBack = onBack)
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isExportingPdf by remember { mutableStateOf(false) }
  var exportError by remember { mutableStateOf<String?>(null) }

  Box(modifier = Modifier.fillMaxSize().background(DarkBackground).testTag("attendance_tracker_screen")) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
          }
          Column {
            Text("Attendance Tracker", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldLight)
            Text(className, fontSize = 13.sp, color = TextSecondary)
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(onClick = onRefresh, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GoldLight)
          }
          Button(
            onClick = {
              isExportingPdf = true
              exportError = null
              scope.launch {
                val res = AttendanceRiskPdfExporter.exportAtRiskPdf(
                  context = context,
                  className = className,
                  generatedBy = session?.fullName.orEmpty(),
                  students = uiState.students
                )
                isExportingPdf = false
                if (res.isFailure) exportError = res.exceptionOrNull()?.message ?: "Export failed"
              }
            },
            enabled = !isExportingPdf && uiState.students.isNotEmpty(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.testTag("tracker_export_pdf_btn")
          ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = DarkTextOnGold, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isExportingPdf) "PDF..." else "Export PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold)
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        RiskLevel.entries.forEach { lvl ->
          val selected = uiState.selectedLevel == lvl
          val chipColor = when (lvl) {
            RiskLevel.CRITICAL -> ErrorRed
            RiskLevel.HIGH -> Color(0xFFFF9800)
            RiskLevel.WATCH -> Color(0xFFFFD54F)
            RiskLevel.OK -> SuccessGreen
            RiskLevel.ALL -> GoldPrimary
          }
          FilterChip(
            selected = selected,
            onClick = { onSelectLevel(lvl) },
            label = {
              Text(
                text = "${lvl.label} (${uiState.countFor(lvl)})",
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) DarkTextOnGold else chipColor
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              containerColor = DarkCard,
              selectedContainerColor = chipColor
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = onSearchChange,
        placeholder = { Text("Search student name or admission no.") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldLight) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("tracker_search_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = GoldPrimary,
          unfocusedBorderColor = DarkBorder,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        )
      )

      val activeErr = uiState.errorMessage ?: exportError
      if (!activeErr.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        ErrorBanner(message = activeErr, onDismiss = { onDismissError(); exportError = null })
      }

      Spacer(modifier = Modifier.height(10.dp))

      ClassRepPullToRefresh(isRefreshing = uiState.isLoading, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
        val rows = uiState.filteredStudents
        if (uiState.isLoading && uiState.students.isEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LoadingSkeleton(height = 84)
            LoadingSkeleton(height = 84)
          }
        } else if (rows.isEmpty()) {
          EmptyAttendanceState(message = "No students match this filter.")
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { it.studentId }) { item ->
              StudentRiskCard(item = item, onClick = { onStudentClick(item) })
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
          }
        }
      }
    }
  }
}

@Composable
private fun StudentRiskCard(item: StudentRiskItem, onClick: () -> Unit) {
  val pctStr = String.format(Locale.US, "%.1f%%", item.percentage)
  val attendedTotal = item.attended + item.excused
  val lastDate = item.lastAttendedAt.takeIf { it.isNotBlank() && !it.equals("null", true) }
    ?.take(10) ?: "Never"

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(DarkCard)
      .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(14.dp)
      .testTag("tracker_student_card_${item.studentId}"),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(item.fullName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Adm: ${item.admissionNumber}", fontSize = 13.sp, color = GoldLight)
      }
      RiskBadgeChip(item.riskLevel)
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("$attendedTotal of ${item.totalSessions} ($pctStr)", fontSize = 13.sp, color = TextPrimary)
      if (item.missStreak >= 2) {
        Box(
          modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ErrorRed.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
          Text("Missed ${item.missStreak} in a row", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
        }
      }
    }
    Text("Last attended: $lastDate", fontSize = 13.sp, color = TextSecondary)
  }
}
