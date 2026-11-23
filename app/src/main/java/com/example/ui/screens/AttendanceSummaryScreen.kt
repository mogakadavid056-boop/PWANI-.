package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceExportService
import com.example.data.StudentAttendanceSummaryItem
import com.example.data.UserSession
import com.example.export.AttendanceExportHelper
import com.example.ui.components.ClassRepPullToRefresh
import com.example.ui.components.EmptyAttendanceState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LoadingSkeleton
import com.example.ui.components.MetallicGoldText
import com.example.ui.components.OutlinedGoldButton
import com.example.ui.components.PrimaryButton
import com.example.ui.components.RoyalCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AttendanceSummaryScreen(
  classId: String,
  className: String,
  session: UserSession?,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val exportService = remember { AttendanceExportService() }

  var summaryList by remember { mutableStateOf<List<StudentAttendanceSummaryItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isExportingPdf by remember { mutableStateOf(false) }
  var isExportingCsv by remember { mutableStateOf(false) }
  var exportError by remember { mutableStateOf<String?>(null) }

  val loadData: () -> Unit = {
    if (session != null && classId.isNotBlank()) {
      isLoading = true
      errorMessage = null
      coroutineScope.launch {
        val result = exportService.fetchClassAttendanceSummary(classId, session.accessToken)
        isLoading = false
        result.fold(
          onSuccess = { summaryList = it },
          onFailure = { errorMessage = it.message ?: "Failed to load summary." }
        )
      }
    }
  }

  LaunchedEffect(classId, session?.userId) {
    loadData()
  }

  val classAverage = if (summaryList.isNotEmpty()) summaryList.map { it.percentage }.average() else 0.0
  val below75Count = summaryList.count { it.percentage < 75.0 }

  Box(modifier = Modifier.fillMaxSize().testTag("attendance_summary_screen")) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("summary_back_btn")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
          MetallicGoldText("Attendance", style = ScreenTitleStyle, maxLines = 1)
          Text(className.ifBlank { "Class Attendance Summary" }, style = CaptionStyle.copy(color = Lavender))
        }
        IconButton(onClick = loadData, modifier = Modifier.size(48.dp)) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ChampagneGold)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Stats Card
      RoyalCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Students", style = CaptionStyle.copy(color = Lavender))
            Text("${summaryList.size}", style = CardTitleStyle.copy(color = SilverWhite))
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Class Average", style = CaptionStyle.copy(color = Lavender))
            val avgStr = String.format(Locale.US, "%.1f%%", classAverage)
            Text(avgStr, style = CardTitleStyle.copy(color = if (classAverage >= 75.0) ChampagneGold else ErrorRed))
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Below 75%", style = CaptionStyle.copy(color = Lavender))
            Text("$below75Count", style = CardTitleStyle.copy(color = if (below75Count > 0) ErrorRed else ChampagneGold))
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Export Buttons: Export CSV and Export PDF side by side
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedGoldButton(
          text = "Export CSV",
          leadingIcon = Icons.Default.FileDownload,
          onClick = {
            isExportingCsv = true
            exportError = null
            coroutineScope.launch {
              val res = AttendanceExportHelper.exportSummaryCsv(context, className, summaryList)
              isExportingCsv = false
              if (res.isFailure) exportError = res.exceptionOrNull()?.message ?: "Failed to export CSV"
            }
          },
          enabled = !isExportingCsv && !isExportingPdf && summaryList.isNotEmpty(),
          modifier = Modifier.weight(1f).testTag("summary_export_csv_btn")
        )

        PrimaryButton(
          text = if (isExportingPdf) "PDF..." else "Export PDF",
          leadingIcon = Icons.Default.PictureAsPdf,
          onClick = {
            if (session != null) {
              isExportingPdf = true
              exportError = null
              coroutineScope.launch {
                val res = AttendanceExportHelper.exportSummaryPdf(
                  context = context,
                  classId = classId,
                  summaryList = summaryList,
                  session = session,
                  fallbackClassName = className
                )
                isExportingPdf = false
                if (res.isFailure) exportError = res.exceptionOrNull()?.message ?: "Failed to generate PDF"
              }
            }
          },
          enabled = !isExportingCsv && !isExportingPdf && summaryList.isNotEmpty(),
          isLoading = isExportingPdf,
          modifier = Modifier.weight(1f).testTag("summary_export_pdf_btn")
        )
      }

      val activeErr = errorMessage ?: exportError
      if (!activeErr.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        ErrorBanner(message = activeErr, onDismiss = { errorMessage = null; exportError = null })
      }

      Spacer(modifier = Modifier.height(10.dp))

      ClassRepPullToRefresh(isRefreshing = isLoading, onRefresh = loadData, modifier = Modifier.weight(1f)) {
        if (isLoading && summaryList.isEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LoadingSkeleton(height = 60)
            LoadingSkeleton(height = 60)
            LoadingSkeleton(height = 60)
          }
        } else if (summaryList.isEmpty()) {
          EmptyAttendanceState(message = "No attendance summary data found for this class yet.")
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(summaryList) { index, item ->
              SummaryStudentRow(index + 1, item)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SummaryStudentRow(index: Int, item: StudentAttendanceSummaryItem) {
  val isBelow75 = item.percentage < 75.0
  val pctColor = if (isBelow75) ErrorRed else SuccessColor
  RoyalCard(borderColor = if (isBelow75) ErrorRed.copy(alpha = 0.5f) else null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text("$index.", style = CaptionStyle.copy(color = Lavender, fontWeight = FontWeight.Bold), modifier = Modifier.width(26.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(item.fullName, style = CardTitleStyle.copy(fontSize = 16.sp))
        if (item.admissionNumber.isNotBlank()) {
          Text("Adm: ${item.admissionNumber}", style = CaptionStyle.copy(color = ChampagneGold))
        }
      }
      Column(horizontalAlignment = Alignment.End) {
        val pctStr = String.format(Locale.US, "%.1f%%", item.percentage)
        Text(pctStr, fontFamily = MontserratFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = pctColor)
        Text("${item.attended} / ${item.total} sessions", style = CaptionStyle.copy(color = Lavender))
      }
    }
  }
}
