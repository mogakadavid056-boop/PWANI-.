package com.example.export

import android.content.Context
import com.example.data.AttendanceExportService
import com.example.data.AttendanceRecordItem
import com.example.data.ClassPdfMetadata
import com.example.data.MyAttendanceSummaryData
import com.example.data.StudentAttendanceSummaryItem
import com.example.data.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AttendanceExportHelper {

  private val exportService = AttendanceExportService()

  suspend fun exportSessionPdf(
    context: Context,
    classId: String,
    sessionTitle: String,
    sessionDate: String,
    attendanceList: List<AttendanceRecordItem>,
    session: UserSession
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val meta = exportService.fetchClassMetadata(classId, session.accessToken).getOrDefault(
        ClassPdfMetadata(className = "Class")
      )
      val pdfFile = withContext(Dispatchers.Default) {
        AttendancePdf.generateSessionAttendancePdf(
          context = context,
          classMetadata = meta,
          sessionTitle = sessionTitle,
          sessionDate = sessionDate,
          attendanceList = attendanceList,
          generatedBy = session.fullName
        )
      }
      withContext(Dispatchers.Main) {
        ExportShareHelper.saveAndShareFile(
          context = context,
          fileName = pdfFile.name,
          mimeType = "application/pdf",
          chooserTitle = "Share Attendance PDF"
        ) { /* file already written */ }
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to generate session PDF"))
    }
  }

  suspend fun exportSessionCsv(
    context: Context,
    sessionTitle: String,
    attendanceList: List<AttendanceRecordItem>
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val csvText = ExportShareHelper.buildSessionAttendanceCsv(sessionTitle, attendanceList)
      val cleanTitle = ExportShareHelper.sanitizeFileName(sessionTitle)
      val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
      val fileName = "Attendance_${cleanTitle}_$dateStr.csv"
      withContext(Dispatchers.Main) {
        ExportShareHelper.shareCsv(context, fileName, csvText, "Session Attendance CSV")
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to export session CSV"))
    }
  }

  suspend fun exportSummaryPdf(
    context: Context,
    classId: String,
    summaryList: List<StudentAttendanceSummaryItem>,
    session: UserSession,
    fallbackClassName: String = ""
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      var meta = exportService.fetchClassMetadata(classId, session.accessToken).getOrDefault(
        ClassPdfMetadata(className = fallbackClassName.ifBlank { "Class" })
      )
      if (meta.className == "Class" && fallbackClassName.isNotBlank()) {
        meta = meta.copy(className = fallbackClassName)
      }
      val pdfFile = withContext(Dispatchers.Default) {
        SummaryPdf.generateClassSummaryPdf(
          context = context,
          classMetadata = meta,
          summaryList = summaryList,
          generatedBy = session.fullName
        )
      }
      withContext(Dispatchers.Main) {
        ExportShareHelper.saveAndShareFile(
          context = context,
          fileName = pdfFile.name,
          mimeType = "application/pdf",
          chooserTitle = "Share Summary PDF"
        ) { /* file already written */ }
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to generate summary PDF"))
    }
  }

  suspend fun exportSummaryCsv(
    context: Context,
    className: String,
    summaryList: List<StudentAttendanceSummaryItem>
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val avg = if (summaryList.isNotEmpty()) summaryList.map { it.percentage }.average() else 0.0
      val below75 = summaryList.count { it.percentage < 75.0 }
      val csvText = ExportShareHelper.buildClassSummaryCsv(summaryList, avg, below75)
      val cleanClass = ExportShareHelper.sanitizeFileName(className)
      val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
      val fileName = "Attendance_Summary_${cleanClass}_$dateStr.csv"
      withContext(Dispatchers.Main) {
        ExportShareHelper.shareCsv(context, fileName, csvText, "Class Attendance Summary CSV")
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to export summary CSV"))
    }
  }

  suspend fun exportMyAttendancePdf(
    context: Context,
    reportData: MyAttendanceSummaryData
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val pdfFile = withContext(Dispatchers.Default) {
        MyAttendancePdf.generateMyAttendancePdf(
          context = context,
          reportData = reportData
        )
      }
      withContext(Dispatchers.Main) {
        ExportShareHelper.saveAndShareFile(
          context = context,
          fileName = pdfFile.name,
          mimeType = "application/pdf",
          chooserTitle = "Share My Attendance PDF"
        ) { /* file already written */ }
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to generate My Attendance PDF"))
    }
  }

  suspend fun exportMyAttendanceCsv(
    context: Context,
    reportData: MyAttendanceSummaryData
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val csvText = ExportShareHelper.buildMyAttendanceCsv(reportData)
      val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
      val fileName = "My_Attendance_$dateStr.csv"
      withContext(Dispatchers.Main) {
        ExportShareHelper.shareCsv(context, fileName, csvText, "My Attendance CSV")
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to export My Attendance CSV"))
    }
  }
}
