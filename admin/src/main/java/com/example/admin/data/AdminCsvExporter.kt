package com.example.admin.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.admin.util.AdminJsonHelper
import java.io.File

object AdminCsvExporter {
  private fun escapeCsv(value: String): String {
    val clean = AdminJsonHelper.orDash(value)
    return if (clean.contains(",") || clean.contains("\"") || clean.contains("\n")) {
      "\"${clean.replace("\"", "\"\"")}\""
    } else clean
  }

  fun buildRegisterCsv(detail: AdminRegisterDetail): String {
    val sb = StringBuilder()
    sb.appendLine("Register No,Title,Class,Course,Code,Lecturer,Venue,Date,Time,Status,Present,Total")
    sb.appendLine(
      listOf(
        detail.registerNo, detail.title, detail.className, detail.courseName,
        detail.courseCode, detail.lecturerName, detail.venue, detail.dateText,
        detail.timeText, detail.status, detail.presentCount.toString(), detail.total.toString()
      ).joinToString(",") { escapeCsv(it) }
    )
    sb.appendLine()
    sb.appendLine("#,Full Name,Admission Number,Role,Status,Time In,Time Out")
    detail.rows.forEach { row ->
      sb.appendLine(
        listOf(
          row.n.toString(),
          row.fullName,
          row.admissionNumber,
          if (row.isPrefect) "Class Rep" else "Student",
          if (row.present) "Present" else "Absent",
          row.timeInText,
          row.timeOutText
        ).joinToString(",") { escapeCsv(it) }
      )
    }
    return sb.toString()
  }

  fun shareRegisterCsv(context: Context, detail: AdminRegisterDetail) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val safeNo = detail.registerNo.ifBlank { detail.sessionId.take(8) }.replace(Regex("[^A-Za-z0-9_-]"), "_")
    val file = File(dir, "register_$safeNo.csv")
    file.writeText(buildRegisterCsv(detail))
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/csv"
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, "Attendance Register ${AdminJsonHelper.orDash(detail.registerNo)}")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Register CSV"))
  }
}
