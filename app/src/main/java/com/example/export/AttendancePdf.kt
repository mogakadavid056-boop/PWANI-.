package com.example.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Base64
import com.example.data.AttendanceRecordItem
import com.example.data.ClassPdfMetadata
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AttendancePdf {

  fun generateSessionAttendancePdf(
    context: Context,
    classMetadata: ClassPdfMetadata,
    sessionTitle: String,
    sessionDate: String,
    attendanceList: List<AttendanceRecordItem>,
    generatedBy: String
  ): File {
    val document = PdfDocument()
    val now = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date())
    val dateForFile = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val columns = listOf(
      PdfBuilder.PdfColumn("No.", 32f, Paint.Align.CENTER),
      PdfBuilder.PdfColumn("Full Name", 168f, Paint.Align.LEFT),
      PdfBuilder.PdfColumn("Admission No.", 110f, Paint.Align.LEFT),
      PdfBuilder.PdfColumn("Time Signed", 110f, Paint.Align.CENTER),
      PdfBuilder.PdfColumn("Signature", 103f, Paint.Align.CENTER)
    )

    val rowHeight = 34f
    val headerHeight = 22f
    val summaryHeight = 65f

    // Pre-decode bitmaps for smooth layout
    val decodedRows = attendanceList.mapIndexed { index, record ->
      val timeText = if (record.timeSigned.isNotBlank()) {
        record.timeSigned.replace("T", " ").substringBefore(".").takeLast(8)
      } else "-"
      val bmp = decodeSignature(record.signatureData)
      val cells = listOf(
        PdfBuilder.PdfCell.Text("${index + 1}", isBold = true),
        PdfBuilder.PdfCell.Text(record.fullName, isBold = false),
        PdfBuilder.PdfCell.Text(record.admissionNumber.ifBlank { "-" }, isBold = false),
        PdfBuilder.PdfCell.Text(timeText, isBold = false),
        PdfBuilder.PdfCell.Image(bmp)
      )
      cells
    }

    // Pagination planning
    val pages = mutableListOf<List<List<PdfBuilder.PdfCell>>>()
    var currentRowIndex = 0
    val totalRows = decodedRows.size

    // Page 1 calculation
    val p1StartY = 48f + 10f
    val p1TableStartY = p1StartY + 74f + 12f // info block takes ~74f + 12f margin
    var p1AvailHeight = PdfBuilder.MAX_CONTENT_Y - p1TableStartY - headerHeight
    val p1Rows = mutableListOf<List<PdfBuilder.PdfCell>>()

    while (currentRowIndex < totalRows && p1AvailHeight >= rowHeight) {
      // Check if this is the last row and if summary fits
      if (currentRowIndex == totalRows - 1) {
        if (p1AvailHeight >= rowHeight + summaryHeight) {
          p1Rows.add(decodedRows[currentRowIndex])
          currentRowIndex++
          break
        } else if (p1AvailHeight >= rowHeight) {
          p1Rows.add(decodedRows[currentRowIndex])
          currentRowIndex++
          break
        }
      }
      p1Rows.add(decodedRows[currentRowIndex])
      currentRowIndex++
      p1AvailHeight -= rowHeight
    }
    pages.add(p1Rows)

    // Subsequent pages
    val subPageTableStartY = 48f + 14f
    val subPageAvailHeightBase = PdfBuilder.MAX_CONTENT_Y - subPageTableStartY - headerHeight

    while (currentRowIndex < totalRows) {
      var avail = subPageAvailHeightBase
      val pageRows = mutableListOf<List<PdfBuilder.PdfCell>>()
      while (currentRowIndex < totalRows && avail >= rowHeight) {
        pageRows.add(decodedRows[currentRowIndex])
        currentRowIndex++
        avail -= rowHeight
      }
      pages.add(pageRows)
    }

    // Check if summary fits on last page, otherwise add dedicated final summary page
    val lastPageAvail = if (pages.size == 1) p1AvailHeight else {
      val lastCount = pages.last().size
      subPageAvailHeightBase - (lastCount * rowHeight)
    }
    val needExtraSummaryPage = lastPageAvail < summaryHeight && totalRows > 0
    if (needExtraSummaryPage) {
      pages.add(emptyList())
    }

    val totalPages = pages.size

    // Draw pages
    for (pageIdx in 0 until totalPages) {
      val pageInfo = PdfDocument.PageInfo.Builder(PdfBuilder.PAGE_WIDTH.toInt(), PdfBuilder.PAGE_HEIGHT.toInt(), pageIdx + 1).create()
      val page = document.startPage(pageInfo)
      val canvas = page.canvas

      PdfBuilder.drawBackground(canvas)
      PdfBuilder.drawHeaderBand(canvas, context, "Attendance Sheet")

      var currentY: Float
      if (pageIdx == 0) {
        currentY = PdfBuilder.drawInfoBlock(
          canvas = canvas,
          startY = 48f + 10f,
          metadata = classMetadata,
          generatedBy = generatedBy,
          generatedAt = now,
          sessionTitle = sessionTitle,
          sessionDateTime = sessionDate
        )
      } else {
        currentY = 48f + 14f
      }

      val rowsOnThisPage = pages[pageIdx]
      if (rowsOnThisPage.isNotEmpty()) {
        PdfBuilder.drawTableHeader(canvas, currentY, columns, headerHeight)
        currentY += headerHeight

        rowsOnThisPage.forEachIndexed { rIdx, cells ->
          val isEven = (rIdx % 2 == 0)
          PdfBuilder.drawTableRow(canvas, currentY, rowHeight, isEven, columns, cells)
          currentY += rowHeight
        }
      }

      // If last page, draw summary and signature lines
      if (pageIdx == totalPages - 1) {
        currentY += 16f
        drawSummaryAndSignatures(canvas, currentY, attendanceList.size)
      }

      PdfBuilder.drawFooter(canvas, pageIdx + 1, totalPages, now)
      document.finishPage(page)
    }

    val cleanTitle = ExportShareHelper.sanitizeFileName(sessionTitle)
    val fileName = "Attendance_${cleanTitle}_$dateForFile.pdf"
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val outFile = File(exportDir, fileName)
    FileOutputStream(outFile).use { out ->
      document.writeTo(out)
    }
    document.close()
    return outFile
  }

  private fun drawSummaryAndSignatures(canvas: Canvas, y: Float, totalCount: Int) {
    val countPaint = Paint().apply {
      color = PdfBuilder.COLOR_TEXT_PRIMARY
      textSize = 10f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }
    canvas.drawText("Total present: $totalCount", PdfBuilder.MARGIN, y, countPaint)

    val lineY = y + 36f
    val sigLinePaint = Paint().apply {
      color = Color.parseColor("#777777")
      strokeWidth = 0.8f
    }
    val labelPaint = Paint().apply {
      color = PdfBuilder.COLOR_TEXT_SECONDARY
      textSize = 8.5f
      typeface = Typeface.DEFAULT
      isAntiAlias = true
    }

    // Signature 1: Class Rep / Lecturer
    val col1X = PdfBuilder.MARGIN
    canvas.drawLine(col1X, lineY, col1X + 180f, lineY, sigLinePaint)
    canvas.drawText("Class Rep / Lecturer", col1X, lineY + 12f, labelPaint)

    // Signature 2: Date
    val col2X = PdfBuilder.PAGE_WIDTH - PdfBuilder.MARGIN - 140f
    canvas.drawLine(col2X, lineY, col2X + 140f, lineY, sigLinePaint)
    canvas.drawText("Date", col2X, lineY + 12f, labelPaint)
  }

  private fun decodeSignature(base64: String?): Bitmap? {
    if (base64.isNullOrBlank()) return null
    return try {
      val clean = if (base64.contains(",")) base64.substringAfter(",") else base64
      val bytes = Base64.decode(clean, Base64.DEFAULT)
      val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
      scaleBitmap(bmp, 90f, 30f)
    } catch (_: Exception) {
      null
    }
  }

  private fun scaleBitmap(src: Bitmap, maxW: Float, maxH: Float): Bitmap {
    val w = src.width.toFloat()
    val h = src.height.toFloat()
    if (w <= 0f || h <= 0f) return src
    val factor = minOf(maxW / w, maxH / h)
    val destW = (w * factor).toInt().coerceAtLeast(1)
    val destH = (h * factor).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(src, destW, destH, true)
  }
}
