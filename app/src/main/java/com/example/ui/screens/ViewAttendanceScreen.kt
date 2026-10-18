package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceRecordItem
import com.example.ui.components.AttendanceRecordRow
import com.example.ui.components.ClassRepPullToRefresh
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LoadingSkeleton
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.data.UserSession
import com.example.export.AttendanceExportHelper
import kotlinx.coroutines.launch

@Composable
fun ViewAttendanceScreen(
  attendanceList: List<AttendanceRecordItem>,
  isLoading: Boolean,
  errorMessage: String?,
  sessionTitle: String = "Session",
  sessionDate: String = "",
  classId: String = "",
  session: UserSession? = null,
  onDismissError: () -> Unit,
  onRefresh: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  var isExportingPdf by remember { mutableStateOf(false) }
  var isExportingCsv by remember { mutableStateOf(false) }
  var exportError by remember { mutableStateOf<String?>(null) }
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
      .testTag("view_attendance_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
      // Top Navigation
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.size(40.dp).testTag("view_attendance_back_btn")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text("Attendance List", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GoldLight)
          Text("Verified sign-ins for this session", fontSize = 12.sp, color = TextSecondary)
        }

        IconButton(
          onClick = onRefresh,
          modifier = Modifier.size(40.dp).testTag("refresh_attendance_list_btn")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh list", tint = GoldLight)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Summary Card
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(DarkSurface)
          .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
          .padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Group, contentDescription = null, tint = GoldLight, modifier = Modifier.size(24.dp))
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text("Total Signed", fontSize = 12.sp, color = TextSecondary)
            Text(
              text = "${attendanceList.size} ${if (attendanceList.size == 1) "student" else "students"}",
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              color = GoldPrimary,
              modifier = Modifier.testTag("attendance_total_count")
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Export Buttons: Export CSV and Export PDF side by side
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = {
            isExportingCsv = true
            exportError = null
            coroutineScope.launch {
              val res = AttendanceExportHelper.exportSessionCsv(context, sessionTitle, attendanceList)
              isExportingCsv = false
              if (res.isFailure) {
                exportError = res.exceptionOrNull()?.message ?: "Failed to export CSV"
              }
            }
          },
          enabled = !isExportingCsv && !isExportingPdf && attendanceList.isNotEmpty(),
          modifier = Modifier.weight(1f).height(48.dp).testTag("view_attendance_export_csv_btn"),
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
                val res = AttendanceExportHelper.exportSessionPdf(
                  context = context,
                  classId = classId,
                  sessionTitle = sessionTitle,
                  sessionDate = sessionDate,
                  attendanceList = attendanceList,
                  session = session
                )
                isExportingPdf = false
                if (res.isFailure) {
                  exportError = res.exceptionOrNull()?.message ?: "Failed to generate PDF"
                }
              }
            }
          },
          enabled = !isExportingCsv && !isExportingPdf && attendanceList.isNotEmpty(),
          modifier = Modifier.weight(1f).height(48.dp).testTag("view_attendance_export_pdf_btn"),
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
        ErrorBanner(message = activeErr, onDismiss = { exportError = null; onDismissError() })
      }

      Spacer(modifier = Modifier.height(10.dp))

      ClassRepPullToRefresh(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxWidth().weight(1f)
      ) {
        if (isLoading && attendanceList.isEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LoadingSkeleton(height = 65)
            LoadingSkeleton(height = 65)
            LoadingSkeleton(height = 65)
          }
        } else if (attendanceList.isEmpty()) {
          com.example.ui.components.EmptyAttendanceState(
            message = "No signatures yet. Students who sign attendance will appear here in real time."
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(attendanceList) { record ->
              AttendanceRecordRow(record)
            }
          }
        }
      }
    }
  }
}
