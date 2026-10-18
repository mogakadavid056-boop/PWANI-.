package com.example.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.ClassPdfMetadata
import com.example.data.StudentAttendanceSummaryItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SummaryPdf {

  fun generateClassSummaryPdf(
    context: Context,
    classMetadata: ClassPdfMetadata,
    summaryList: List<StudentAttendanceSummaryItem>,
    generatedBy: String
  ): File {
    val document = PdfDocument()
    val now = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date())
    val dateForFile = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val columns = listOf(
      PdfBuilder.PdfColumn("No.", 32f, Paint.Align.CENTER),
      PdfBuilder.PdfColumn("Full Name", 171f, Paint.Align.LEFT),
      PdfBuilder.PdfColumn("Admission No.", 120f, Paint.Align.LEFT),
      PdfBuilder.PdfColumn("Attended", 60f, Paint.Align.CENTER),
      PdfBuilder.PdfColumn("Total", 60f, Paint.Align.CENTER),
      PdfBuilder.PdfColumn("Percentage", 80f, Paint.Align.RIGHT)
    )

    val rowHeight = 22f
    val headerHeight = 22f
    val summaryBoxHeight = 44f

    val decodedRows = summaryList.mapIndexed { index, item ->
      val isBelow75 = item.percentage < 75.0
      val pctColor = if (isBelow75) PdfBuilder.COLOR_RED else PdfBuilder.COLOR_GREEN
      val pctStr = String.format(Locale.US, "%.1f%%", item.percentage)

      listOf(
        PdfBuilder.PdfCell.Text("${index + 1}", isBold = true),
        PdfBuilder.PdfCell.Text(item.fullName, isBold = false),
        PdfBuilder.PdfCell.Text(item.admissionNumber.ifBlank { "-" }, isBold = false),
        PdfBuilder.PdfCell.Text("${item.attended}", isBold = false),
        PdfBuilder.PdfCell.Text("${item.total}", isBold = false),
        PdfBuilder.PdfCell.Text(pctStr, color = pctColor, isBold = true)
      )
    }

    // Pagination
    val pages = mutableListOf<List<List<PdfBuilder.PdfCell>>>()
    var currentRowIndex = 0
    val totalRows = decodedRows.size

    val p1StartY = 48f + 10f
    val p1TableStartY = p1StartY + 62f + 12f
    var p1AvailHeight = PdfBuilder.MAX_CONTENT_Y - p1TableStartY - headerHeight
    val p1Rows = mutableListOf<List<PdfBuilder.PdfCell>>()

    while (currentRowIndex < totalRows && p1AvailHeight >= rowHeight) {
      if (currentRowIndex == totalRows - 1 && p1AvailHeight < rowHeight + summaryBoxHeight) {
        // If summary wouldn't fit, don't leave orphaned summary alone if we can push
      }
      p1Rows.add(decodedRows[currentRowIndex])
      currentRowIndex++
      p1AvailHeight -= rowHeight
    }
    pages.add(p1Rows)

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

    val lastPageAvail = if (pages.size == 1) p1AvailHeight else {
      subPageAvailHeightBase - (pages.last().size * rowHeight)
    }
    if (lastPageAvail < summaryBoxHeight && totalRows > 0) {
      pages.add(emptyList())
    }

    val totalPages = pages.size
    val classAverage = if (summaryList.isNotEmpty()) summaryList.map { it.percentage }.average() else 0.0
    val below75Count = summaryList.count { it.percentage < 75.0 }

    for (pageIdx in 0 until totalPages) {
      val pageInfo = PdfDocument.PageInfo.Builder(PdfBuilder.PAGE_WIDTH.toInt(), PdfBuilder.PAGE_HEIGHT.toInt(), pageIdx + 1).create()
      val page = document.startPage(pageInfo)
      val canvas = page.canvas

      PdfBuilder.drawBackground(canvas)
      PdfBuilder.drawHeaderBand(canvas, context, "Attendance Summary")

      var currentY: Float
      if (pageIdx == 0) {
        currentY = PdfBuilder.drawInfoBlock(
          canvas = canvas,
          startY = 48f + 10f,
          metadata = classMetadata,
          generatedBy = generatedBy,
          generatedAt = now
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

      if (pageIdx == totalPages - 1) {
        currentY += 14f
        drawSummaryStats(canvas, currentY, classAverage, below75Count)
      }

      PdfBuilder.drawFooter(canvas, pageIdx + 1, totalPages, now)
      document.finishPage(page)
    }

    val cleanClass = ExportShareHelper.sanitizeFileName(classMetadata.className)
    val fileName = "Attendance_Summary_${cleanClass}_$dateForFile.pdf"
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val outFile = File(exportDir, fileName)
    FileOutputStream(outFile).use { out ->
      document.writeTo(out)
    }
    document.close()
    return outFile
  }

  private fun drawSummaryStats(canvas: Canvas, y: Float, classAverage: Double, below75Count: Int) {
    val bgPaint = Paint().apply { color = Color.parseColor("#FAF8F2"); style = Paint.Style.FILL }
    val borderPaint = Paint().apply { color = PdfBuilder.COLOR_BORDER; style = Paint.Style.STROKE; strokeWidth = 0.8f }
    canvas.drawRoundRect(RectF(PdfBuilder.MARGIN, y, PdfBuilder.PAGE_WIDTH - PdfBuilder.MARGIN, y + 36f), 4f, 4f, bgPaint)
    canvas.drawRoundRect(RectF(PdfBuilder.MARGIN, y, PdfBuilder.PAGE_WIDTH - PdfBuilder.MARGIN, y + 36f), 4f, 4f, borderPaint)

    val labelPaint = Paint().apply {
      color = PdfBuilder.COLOR_TEXT_PRIMARY
      textSize = 9.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }
    val avgStr = String.format(Locale.US, "%.1f%%", classAverage)
    canvas.drawText("Class average: $avgStr", PdfBuilder.MARGIN + 16f, y + 22f, labelPaint)

    val countPaint = Paint().apply {
      color = if (below75Count > 0) PdfBuilder.COLOR_RED else PdfBuilder.COLOR_GREEN
      textSize = 9.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
      textAlign = Paint.Align.RIGHT
    }
    canvas.drawText("Students below 75%: $below75Count", PdfBuilder.PAGE_WIDTH - PdfBuilder.MARGIN - 16f, y + 22f, countPaint)
  }
}
