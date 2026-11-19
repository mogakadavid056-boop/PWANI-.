package com.example.admin.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.admin.util.AdminJsonHelper
import java.io.File
import java.util.Locale

object AdminAttendancePdfExporter {
  fun shareAttendanceListCsv(context: Context, title: String, records: List<AdminAttendanceRecord>) {
    val sb = StringBuilder("No.,Full Name,Admission Number,Time Signed\n")
    records.forEachIndexed { idx, r ->
      sb.appendLine("${idx + 1},${esc(r.fullName)},${esc(r.admissionNumber)},${esc(r.timeSigned)}")
    }
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val safe = title.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "attendance" }
    val file = File(dir, "attendance_$safe.csv").apply { writeText(sb.toString()) }
    shareFile(context, file, "text/csv", "Share Attendance CSV")
  }

  fun shareAttendanceListPdf(context: Context, title: String, subtitle: String, records: List<AdminAttendanceRecord>) {
    val doc = PdfDocument()
    val pageW = 595; val pageH = 842
    val headPaint = Paint().apply { color = Color.rgb(11, 42, 30); textSize = 15f; isFakeBoldText = true }
    val subPaint = Paint().apply { color = Color.DKGRAY; textSize = 10.5f }
    val rowPaint = Paint().apply { color = Color.BLACK; textSize = 10f }
    val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

    var pageNum = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
    var y = 44f
    page.canvas.drawText("ClassRep Admin - $title", 32f, y, headPaint); y += 16f
    page.canvas.drawText("$subtitle • Signed: ${records.size}", 32f, y, subPaint); y += 16f
    page.canvas.drawLine(32f, y, pageW - 32f, y, linePaint); y += 16f

    records.forEachIndexed { idx, r ->
      if (y > pageH - 42f) {
        doc.finishPage(page)
        pageNum++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
        y = 44f
      }
      page.canvas.drawText("${idx + 1}.", 32f, y, rowPaint)
      page.canvas.drawText(AdminJsonHelper.orDash(r.fullName).take(28), 60f, y, rowPaint)
      page.canvas.drawText(AdminJsonHelper.orDash(r.admissionNumber).take(18), 290f, y, rowPaint)
      page.canvas.drawText(AdminJsonHelper.orDash(r.timeSigned).take(22), 430f, y, rowPaint)
      y += 16f
    }
    doc.finishPage(page)

    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val safe = title.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "attendance" }
    val file = File(dir, "attendance_$safe.pdf")
    file.outputStream().use { doc.writeTo(it) }
    doc.close()
    shareFile(context, file, "application/pdf", "Share Attendance PDF")
  }

  fun shareRiskListPdf(context: Context, className: String, students: List<AdminAtRiskStudentItem>) {
    val doc = PdfDocument()
    val pageW = 595; val pageH = 842
    val headPaint = Paint().apply { color = Color.rgb(11, 42, 30); textSize = 15f; isFakeBoldText = true }
    val rowPaint = Paint().apply { color = Color.BLACK; textSize = 9.5f }
    var pageNum = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
    var y = 44f
    page.canvas.drawText("Attendance Risk Report - $className", 32f, y, headPaint); y += 22f
    students.forEachIndexed { idx, s ->
      if (y > pageH - 42f) {
        doc.finishPage(page); pageNum++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create()); y = 44f
      }
      val pct = String.format(Locale.US, "%.1f%%", s.percentage)
      val line = "${idx + 1}. ${s.fullName} (${s.admissionNumber}) • ${s.attended}/${s.totalSessions} ($pct) • Streak ${s.missStreak} • ${s.riskLevel.label}"
      page.canvas.drawText(line.take(92), 32f, y, rowPaint); y += 16f
    }
    doc.finishPage(page)
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val safe = className.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "risk" }
    val file = File(dir, "risk_$safe.pdf")
    file.outputStream().use { doc.writeTo(it) }
    doc.close()
    shareFile(context, file, "application/pdf", "Share Risk PDF")
  }

  private fun esc(v: String): String {
    val c = AdminJsonHelper.orDash(v)
    return if (c.contains(",") || c.contains("\"")) "\"${c.replace("\"", "\"\"")}\"" else c
  }

  private fun shareFile(context: Context, file: File, mime: String, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = mime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
  }
}
