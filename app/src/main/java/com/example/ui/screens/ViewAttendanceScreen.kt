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
import com.example.ui.components.MetallicGoldText
import com.example.ui.components.OutlinedGoldButton
import com.example.ui.components.PrimaryButton
import com.example.ui.components.RoyalCard
import com.example.ui.theme.*

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
          modifier = Modifier.size(48.dp).testTag("view_attendance_back_btn")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
        }

        Spacer(modifier = Modifier.width(6.dp))

        Column(modifier = Modifier.weight(1f)) {
          MetallicGoldText("Attendance List", style = ScreenTitleStyle, maxLines = 1)
          Text("Verified sign-ins for this session", style = CaptionStyle.copy(color = Lavender))
        }

        IconButton(
          onClick = onRefresh,
          modifier = Modifier.size(48.dp).testTag("refresh_attendance_list_btn")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh list", tint = ChampagneGold)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Summary Card
      RoyalCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(RoyalSurfaceHigh)
              .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Group, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(24.dp))
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text("Total Signed", style = CaptionStyle.copy(color = Lavender))
            Text(
              text = "${attendanceList.size} ${if (attendanceList.size == 1) "student" else "students"}",
              style = SectionHeadingStyle.copy(color = ChampagneGold),
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
        OutlinedGoldButton(
          text = "Export CSV",
          leadingIcon = Icons.Default.FileDownload,
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
          modifier = Modifier.weight(1f).testTag("view_attendance_export_csv_btn")
        )

        PrimaryButton(
          text = if (isExportingPdf) "PDF..." else "Export PDF",
          leadingIcon = Icons.Default.PictureAsPdf,
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
          isLoading = isExportingPdf,
          modifier = Modifier.weight(1f).testTag("view_attendance_export_pdf_btn")
        )
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
