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

  Box(modifier = Modifier.fillMaxSize().background(DarkBackground).testTag("attendance_summary_screen")) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp).testTag("summary_back_btn")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text("Attendance Summary", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = GoldLight)
          Text(className.ifBlank { "Class Attendance" }, fontSize = 12.sp, color = TextSecondary)
        }
        IconButton(onClick = loadData, modifier = Modifier.size(40.dp)) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GoldLight)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Stats Card
      Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DarkSurface)
          .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)).padding(14.dp)
      ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Students", fontSize = 11.sp, color = TextSecondary)
            Text("${summaryList.size}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Class Average", fontSize = 11.sp, color = TextSecondary)
            val avgStr = String.format(Locale.US, "%.1f%%", classAverage)
            Text(avgStr, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (classAverage >= 75.0) GoldPrimary else ErrorRed)
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Below 75%", fontSize = 11.sp, color = TextSecondary)
            Text("$below75Count", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (below75Count > 0) ErrorRed else GoldLight)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Export Buttons: Export CSV and Export PDF side by side
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
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
          modifier = Modifier.weight(1f).height(48.dp).testTag("summary_export_csv_btn"),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = com.example.ui.theme.DarkSurface, contentColor = GoldPrimary),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary)
        ) {
          Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp), tint = GoldPrimary)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Export CSV", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
        }

        Button(
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
          modifier = Modifier.weight(1f).height(48.dp).testTag("summary_export_pdf_btn"),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = com.example.ui.theme.DarkTextOnGold)
        ) {
          if (isExportingPdf) {
            CircularProgressIndicator(color = com.example.ui.theme.DarkTextOnGold, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Generating PDF...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.DarkTextOnGold)
          } else {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = com.example.ui.theme.DarkTextOnGold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Export PDF", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.DarkTextOnGold)
          }
        }
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
  val pctColor = if (isBelow75) ErrorRed else SuccessGreen
  Box(
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(DarkSurface)
      .border(1.dp, if (isBelow75) ErrorRed.copy(alpha = 0.4f) else DarkBorder, RoundedCornerShape(10.dp)).padding(12.dp)
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text("$index.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.width(26.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(item.fullName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        if (item.admissionNumber.isNotBlank()) {
          Text("Adm: ${item.admissionNumber}", fontSize = 13.sp, color = GoldLight)
        }
      }
      Column(horizontalAlignment = Alignment.End) {
        val pctStr = String.format(Locale.US, "%.1f%%", item.percentage)
        Text(pctStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = pctColor)
        Text("${item.attended} / ${item.total} sessions", fontSize = 13.sp, color = TextSecondary)
      }
    }
  }
}
