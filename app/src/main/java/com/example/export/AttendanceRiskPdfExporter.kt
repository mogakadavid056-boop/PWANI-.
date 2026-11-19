package com.example.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.ClassPdfMetadata
import com.example.data.RiskLevel
import com.example.data.StudentRiskItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AttendanceRiskPdfExporter {
  private val columns = listOf(
    PdfBuilder.PdfColumn("No.", 28f, Paint.Align.CENTER),
    PdfBuilder.PdfColumn("Name", 145f, Paint.Align.LEFT),
    PdfBuilder.PdfColumn("Admission No.", 92f, Paint.Align.LEFT),
    PdfBuilder.PdfColumn("Attended", 48f, Paint.Align.CENTER),
    PdfBuilder.PdfColumn("Total", 40f, Paint.Align.CENTER),
    PdfBuilder.PdfColumn("Pct", 52f, Paint.Align.RIGHT),
    PdfBuilder.PdfColumn("Missed", 54f, Paint.Align.CENTER),
    PdfBuilder.PdfColumn("Level", 64f, Paint.Align.CENTER)
  )

  suspend fun exportAtRiskPdf(
    context: Context,
    className: String,
    generatedBy: String,
    students: List<StudentRiskItem>
  ): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      val atRisk = students.filter {
        it.riskLevel == RiskLevel.CRITICAL || it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.WATCH
      }
      val generatedAt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date())
      val doc = PdfDocument()
      var pageNum = 1
      val rowHeight = 22f
      val rowsPerPage = 24
      val chunks = if (atRisk.isEmpty()) listOf(emptyList()) else atRisk.chunked(rowsPerPage)
      val totalPages = chunks.size

      chunks.forEachIndexed { chunkIdx, chunk ->
        val pageInfo = PdfDocument.PageInfo.Builder(PdfBuilder.PAGE_WIDTH.toInt(), PdfBuilder.PAGE_HEIGHT.toInt(), pageNum).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        PdfBuilder.drawBackground(canvas)
        PdfBuilder.drawHeaderBand(canvas, context, "Attendance Risk Report")

        var y = 60f
        if (chunkIdx == 0) {
          y = PdfBuilder.drawInfoBlock(
            canvas = canvas,
            startY = y,
            metadata = ClassPdfMetadata(className = className.ifBlank { "Class" }),
            generatedBy = generatedBy.ifBlank { "Lecturer" },
            generatedAt = generatedAt
          )
        }

        PdfBuilder.drawTableHeader(canvas, y, columns)
        y += 22f

        if (chunk.isEmpty()) {
          val paint = Paint().apply { color = PdfBuilder.COLOR_TEXT_SECONDARY; textSize = 10f; isAntiAlias = true }
          canvas.drawText("No students in Critical, High, or Watch levels.", PdfBuilder.MARGIN + 10f, y + 20f, paint)
        } else {
          chunk.forEachIndexed { idx, s ->
            val globalIdx = (chunkIdx * rowsPerPage) + idx + 1
            val lvlColor = when (s.riskLevel) {
              RiskLevel.CRITICAL -> PdfBuilder.COLOR_RED
              RiskLevel.HIGH -> android.graphics.Color.parseColor("#E65100")
              RiskLevel.WATCH -> android.graphics.Color.parseColor("#9A6700")
              else -> PdfBuilder.COLOR_GREEN
            }
            val pctStr = String.format(Locale.US, "%.1f%%", s.percentage)
            PdfBuilder.drawTableRow(
              canvas = canvas,
              y = y,
              rowHeight = rowHeight,
              isEven = idx % 2 == 0,
              columns = columns,
              cells = listOf(
                PdfBuilder.PdfCell.Text(globalIdx.toString()),
                PdfBuilder.PdfCell.Text(s.fullName.take(24), isBold = true),
                PdfBuilder.PdfCell.Text(s.admissionNumber.take(15)),
                PdfBuilder.PdfCell.Text("${s.attended + s.excused}"),
                PdfBuilder.PdfCell.Text(s.totalSessions.toString()),
                PdfBuilder.PdfCell.Text(pctStr, color = lvlColor, isBold = true),
                PdfBuilder.PdfCell.Text("${s.missStreak} row"),
                PdfBuilder.PdfCell.Text(s.riskLevel.label, color = lvlColor, isBold = true)
              )
            )
            y += rowHeight
          }
        }
        PdfBuilder.drawFooter(canvas, pageNum, totalPages, generatedAt)
        doc.finishPage(page)
        pageNum++
      }

      val safeName = ExportShareHelper.sanitizeFileName(className)
      ExportShareHelper.saveAndShareFile(
        context = context,
        fileName = "Attendance_Risk_$safeName.pdf",
        mimeType = "application/pdf",
        chooserTitle = "Share Attendance Risk PDF"
      ) { file ->
        FileOutputStream(file).use { out -> doc.writeTo(out) }
      }
      doc.close()
    }
  }
}
