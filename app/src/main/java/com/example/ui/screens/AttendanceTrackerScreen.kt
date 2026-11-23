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
import androidx.compose.ui.focus.onFocusChanged
import com.example.ui.components.ClassRepPullToRefresh
import com.example.ui.components.EmptyAttendanceState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LoadingSkeleton
import com.example.ui.components.MetallicGoldText
import com.example.ui.components.RoyalCard
import com.example.ui.components.royalOutlinedTextFieldColors
import com.example.ui.components.violetGlowShadow
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

  Box(modifier = Modifier.fillMaxSize().testTag("attendance_tracker_screen")) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
          }
          Column {
            MetallicGoldText("Attendance Tracker", style = ScreenTitleStyle.copy(fontSize = 20.sp))
            Text(className, style = SubtitleItalicStyle.copy(fontSize = 14.sp))
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ChampagneGold)
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
            shape = RoundedCornerShape(ButtonRadius),
            colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("tracker_export_pdf_btn")
          ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Midnight, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isExportingPdf) "PDF..." else "Export PDF", style = ButtonTextStyle.copy(fontSize = 13.sp, color = Midnight))
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
            RiskLevel.CRITICAL -> ErrorColor
            RiskLevel.HIGH -> ElectricViolet
            RiskLevel.WATCH -> ChampagneGold
            RiskLevel.OK -> SuccessColor
            RiskLevel.ALL -> MetallicGold
          }
          FilterChip(
            selected = selected,
            onClick = { onSelectLevel(lvl) },
            label = {
              Text(
                text = "${lvl.label} (${uiState.countFor(lvl)})",
                style = FieldLabelStyle.copy(
                  fontSize = 13.sp,
                  color = if (selected) Midnight else chipColor
                )
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              containerColor = RoyalSurfaceHigh,
              selectedContainerColor = chipColor
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      var searchFocused by remember { mutableStateOf(false) }
      OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = onSearchChange,
        placeholder = { Text("Search student name or admission no.", style = CaptionStyle) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ChampagneGold) },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .then(violetGlowShadow(searchFocused))
          .onFocusChanged { searchFocused = it.isFocused }
          .testTag("tracker_search_input"),
        shape = RoundedCornerShape(ButtonRadius),
        colors = royalOutlinedTextFieldColors()
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

  RoyalCard(
    onClick = onClick,
    modifier = Modifier.testTag("tracker_student_card_${item.studentId}")
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(item.fullName, style = CardTitleStyle)
        Text("Adm: ${item.admissionNumber}", style = CaptionStyle.copy(color = ChampagneGold))
      }
      RiskBadgeChip(item.riskLevel)
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("$attendedTotal of ${item.totalSessions} ($pctStr)", style = BodyStyle.copy(fontSize = 14.sp))
      if (item.missStreak >= 2) {
        Box(
          modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ErrorColor.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
          Text("Missed ${item.missStreak} in a row", style = FieldLabelStyle.copy(fontSize = 12.sp, color = ErrorColor))
        }
      }
    }
    Text("Last attended: $lastDate", style = CaptionStyle)
  }
}
