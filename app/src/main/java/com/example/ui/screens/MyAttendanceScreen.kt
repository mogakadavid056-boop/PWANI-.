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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
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
import com.example.data.MyAttendanceRecordItem
import com.example.data.MyAttendanceSummaryData
import com.example.data.UserSession
import com.example.export.AttendanceExportHelper
import com.example.ui.components.ClassRepPullToRefresh
import com.example.ui.components.EmptyAttendanceState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LoadingSkeleton
import com.example.ui.components.MetallicGoldText
import com.example.ui.components.PrimaryButton
import com.example.ui.components.OutlinedGoldButton
import com.example.ui.components.RoyalCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MyAttendanceScreen(
  classId: String,
  session: UserSession?,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val exportService = remember { AttendanceExportService() }

  var reportData by remember { mutableStateOf<MyAttendanceSummaryData?>(null) }
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
        val result = exportService.fetchMyAttendance(classId, session)
        isLoading = false
        result.fold(
          onSuccess = { reportData = it },
          onFailure = { errorMessage = it.message ?: "Failed to load attendance report." }
        )
      }
    }
  }

  LaunchedEffect(classId, session?.userId) {
    loadData()
  }

  Box(modifier = Modifier.fillMaxSize().testTag("my_attendance_screen")) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("my_attendance_back_btn")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
          MetallicGoldText("My Attendance", style = ScreenTitleStyle, maxLines = 1)
          Text("Personal attendance record", style = CaptionStyle.copy(color = Lavender))
        }
        IconButton(onClick = loadData, modifier = Modifier.size(48.dp)) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ChampagneGold)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      val currentReport = reportData
      if (currentReport != null) {
        // Summary Header Card
        RoyalCard {
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
              Text(currentReport.studentName, style = CardTitleStyle)
              if (currentReport.admissionNumber.isNotBlank()) {
                Text("Adm: ${currentReport.admissionNumber}", style = CaptionStyle.copy(color = ChampagneGold))
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text("Attended ${currentReport.attended} of ${currentReport.total} sessions", style = CaptionStyle.copy(color = Lavender))
            }
            val pctStr = String.format(Locale.US, "%.1f%%", currentReport.percentage)
            val pctColor = if (currentReport.percentage < 75.0) ErrorRed else SuccessColor
            Box(
              modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(pctColor.copy(alpha = 0.16f))
                .border(1.dp, pctColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text(pctStr, fontFamily = MontserratFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = pctColor)
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Export Buttons side by side
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedGoldButton(
            text = "Export CSV",
            leadingIcon = Icons.Default.FileDownload,
            onClick = {
              isExportingCsv = true
              exportError = null
              coroutineScope.launch {
                val res = AttendanceExportHelper.exportMyAttendanceCsv(context, currentReport)
                isExportingCsv = false
                if (res.isFailure) exportError = res.exceptionOrNull()?.message ?: "Failed to export CSV"
              }
            },
            enabled = !isExportingCsv && !isExportingPdf,
            modifier = Modifier.weight(1f).testTag("my_attendance_export_csv_btn")
          )

          PrimaryButton(
            text = if (isExportingPdf) "PDF..." else "Export PDF",
            leadingIcon = Icons.Default.PictureAsPdf,
            onClick = {
              isExportingPdf = true
              exportError = null
              coroutineScope.launch {
                val res = AttendanceExportHelper.exportMyAttendancePdf(context, currentReport)
                isExportingPdf = false
                if (res.isFailure) exportError = res.exceptionOrNull()?.message ?: "Failed to generate PDF"
              }
            },
            enabled = !isExportingCsv && !isExportingPdf,
            isLoading = isExportingPdf,
            modifier = Modifier.weight(1f).testTag("my_attendance_export_pdf_btn")
          )
        }
      }

      val activeErr = errorMessage ?: exportError
      if (!activeErr.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        ErrorBanner(message = activeErr, onDismiss = { errorMessage = null; exportError = null })
      }

      Spacer(modifier = Modifier.height(10.dp))

      ClassRepPullToRefresh(isRefreshing = isLoading, onRefresh = loadData, modifier = Modifier.weight(1f)) {
        val records = currentReport?.records.orEmpty()
        if (isLoading && records.isEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LoadingSkeleton(height = 60)
            LoadingSkeleton(height = 60)
          }
        } else if (records.isEmpty()) {
          EmptyAttendanceState(message = "No attendance sessions logged for you in this class yet.")
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(records) { index, item ->
              StudentSessionRecordRow(index + 1, item)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StudentSessionRecordRow(index: Int, record: MyAttendanceRecordItem) {
  RoyalCard {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text("$index.", style = CaptionStyle.copy(color = Lavender, fontWeight = FontWeight.Bold), modifier = Modifier.width(26.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(record.sessionTitle, fontFamily = PlayfairDisplayFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SilverWhite)
        if (record.date.isNotBlank()) {
          Text(record.date, style = CaptionStyle.copy(color = Lavender))
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (record.isPresent) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Present", fontFamily = MontserratFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SuccessColor)
        } else {
          Icon(Icons.Default.Cancel, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Absent", fontFamily = MontserratFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
        }
      }
    }
  }
}
