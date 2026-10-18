package com.example.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.AttendanceRecordItem
import com.example.data.MyAttendanceSummaryData
import com.example.data.StudentAttendanceSummaryItem
import java.io.File
import java.io.FileOutputStream

object ExportShareHelper {

  fun sanitizeFileName(name: String): String {
    return name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").trim('_').ifBlank { "Document" }
  }

  fun saveAndShareFile(
    context: Context,
    fileName: String,
    mimeType: String,
    chooserTitle: String,
    writeBytes: (File) -> Unit
  ) {
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(exportDir, fileName)
    writeBytes(file)

    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)

    val intent = Intent(Intent.ACTION_SEND).apply {
      type = mimeType
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(intent, chooserTitle)
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
  }

  fun shareCsv(
    context: Context,
    fileName: String,
    csvContent: String,
    title: String
  ) {
    saveAndShareFile(context, fileName, "text/csv", "Share $title") { file ->
      FileOutputStream(file).use { out ->
        out.write(csvContent.toByteArray(Charsets.UTF_8))
      }
    }
  }

  fun buildSessionAttendanceCsv(
    sessionTitle: String,
    attendanceList: List<AttendanceRecordItem>
  ): String {
    val sb = StringBuilder()
    sb.appendLine("No.,Full Name,Admission No.,Time Signed")
    attendanceList.forEachIndexed { index, record ->
      val cleanName = escapeCsv(record.fullName)
      val cleanAdm = escapeCsv(record.admissionNumber)
      val cleanTime = escapeCsv(record.timeSigned)
      sb.appendLine("${index + 1},$cleanName,$cleanAdm,$cleanTime")
    }
    return sb.toString()
  }

  fun buildClassSummaryCsv(
    summaryList: List<StudentAttendanceSummaryItem>,
    classAverage: Double,
    below75Count: Int
  ): String {
    val sb = StringBuilder()
    sb.appendLine("No.,Full Name,Admission No.,Attended,Total,Percentage")
    summaryList.forEachIndexed { index, item ->
      val name = escapeCsv(item.fullName)
      val adm = escapeCsv(item.admissionNumber)
      val pctStr = String.format(java.util.Locale.US, "%.1f%%", item.percentage)
      sb.appendLine("${index + 1},$name,$adm,${item.attended},${item.total},$pctStr")
    }
    sb.appendLine()
    val avgStr = String.format(java.util.Locale.US, "%.1f%%", classAverage)
    sb.appendLine("Class average,$avgStr")
    sb.appendLine("Students below 75%,$below75Count")
    return sb.toString()
  }

  fun buildMyAttendanceCsv(
    report: MyAttendanceSummaryData
  ): String {
    val sb = StringBuilder()
    val name = escapeCsv(report.studentName)
    val adm = escapeCsv(report.admissionNumber)
    val cls = escapeCsv(report.className)
    val pctStr = String.format(java.util.Locale.US, "%.1f%%", report.percentage)

    sb.appendLine("Student Name,$name")
    sb.appendLine("Admission No.,$adm")
    sb.appendLine("Class,$cls")
    sb.appendLine("Overall Percentage,$pctStr")
    sb.appendLine("Attended,${report.attended} of ${report.total}")
    sb.appendLine()
    sb.appendLine("No.,Session,Date,Status")
    report.records.forEachIndexed { index, rec ->
      val sTitle = escapeCsv(rec.sessionTitle)
      val sDate = escapeCsv(rec.date)
      val status = if (rec.isPresent) "Present" else "Absent"
      sb.appendLine("${index + 1},$sTitle,$sDate,$status")
    }
    return sb.toString()
  }

  private fun escapeCsv(value: String): String {
    val v = value.trim()
    return if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
      "\"" + v.replace("\"", "\"\"") + "\""
    } else {
      v
    }
  }
}
