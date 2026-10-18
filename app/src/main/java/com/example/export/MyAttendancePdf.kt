package com.example.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.MyAttendanceSummaryData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MyAttendancePdf {

  fun generateMyAttendancePdf(
    context: Context,
    reportData: MyAttendanceSummaryData
  ): File {
    val document = PdfDocument()
    val now = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date())
    val dateForFile = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val columns = listOf(
      PdfBuilder.PdfColumn("No.", 35f, Paint.Align.CENTER),
      PdfBuilder.PdfColumn("Session", 238f, Paint.Align.LEFT),
      PdfBuilder.PdfColumn("Date", 140f, Paint.Align.CENTER),
      PdfBuilder.PdfColumn("Status", 110f, Paint.Align.CENTER)
    )

    val rowHeight = 22f
    val headerHeight = 22f

    val decodedRows = reportData.records.mapIndexed { index, record ->
      val statusColor = if (record.isPresent) PdfBuilder.COLOR_GREEN else PdfBuilder.COLOR_RED
      val statusText = if (record.isPresent) "Present" else "Absent"

      listOf(
        PdfBuilder.PdfCell.Text("${index + 1}", isBold = true),
        PdfBuilder.PdfCell.Text(record.sessionTitle, isBold = false),
        PdfBuilder.PdfCell.Text(record.date.ifBlank { "-" }, isBold = false),
        PdfBuilder.PdfCell.Text(statusText, color = statusColor, isBold = true)
      )
    }

    // Pagination
    val pages = mutableListOf<List<List<PdfBuilder.PdfCell>>>()
    var currentRowIndex = 0
    val totalRows = decodedRows.size

    val p1StartY = 48f + 10f
    val p1HeaderBlockHeight = 68f
    val p1TableStartY = p1StartY + p1HeaderBlockHeight + 12f
    var p1AvailHeight = PdfBuilder.MAX_CONTENT_Y - p1TableStartY - headerHeight
    val p1Rows = mutableListOf<List<PdfBuilder.PdfCell>>()

    while (currentRowIndex < totalRows && p1AvailHeight >= rowHeight) {
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

    val totalPages = pages.size

    for (pageIdx in 0 until totalPages) {
      val pageInfo = PdfDocument.PageInfo.Builder(PdfBuilder.PAGE_WIDTH.toInt(), PdfBuilder.PAGE_HEIGHT.toInt(), pageIdx + 1).create()
      val page = document.startPage(pageInfo)
      val canvas = page.canvas

      PdfBuilder.drawBackground(canvas)
      PdfBuilder.drawHeaderBand(canvas, context, "My Attendance Report")

      var currentY: Float
      if (pageIdx == 0) {
        currentY = drawStudentHeaderBlock(canvas, p1StartY, p1HeaderBlockHeight, reportData, now)
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

      PdfBuilder.drawFooter(canvas, pageIdx + 1, totalPages, now)
      document.finishPage(page)
    }

    val fileName = "My_Attendance_$dateForFile.pdf"
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val outFile = File(exportDir, fileName)
    FileOutputStream(outFile).use { out ->
      document.writeTo(out)
    }
    document.close()
    return outFile
  }

  private fun drawStudentHeaderBlock(
    canvas: Canvas,
    startY: Float,
    blockHeight: Float,
    data: MyAttendanceSummaryData,
    generatedAt: String
  ): Float {
    val bgPaint = Paint().apply { color = Color.parseColor("#FAF8F2"); style = Paint.Style.FILL }
    val borderPaint = Paint().apply { color = PdfBuilder.COLOR_BORDER; style = Paint.Style.STROKE; strokeWidth = 0.8f }
    canvas.drawRoundRect(RectF(PdfBuilder.MARGIN, startY, PdfBuilder.PAGE_WIDTH - PdfBuilder.MARGIN, startY + blockHeight), 4f, 4f, bgPaint)
    canvas.drawRoundRect(RectF(PdfBuilder.MARGIN, startY, PdfBuilder.PAGE_WIDTH - PdfBuilder.MARGIN, startY + blockHeight), 4f, 4f, borderPaint)

    val labelPaint = Paint().apply {
      color = PdfBuilder.COLOR_TEXT_SECONDARY
      textSize = 8f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }
    val valuePaint = Paint().apply {
      color = PdfBuilder.COLOR_TEXT_PRIMARY
      textSize = 8.5f
      typeface = Typeface.DEFAULT
      isAntiAlias = true
    }

    val col1X = PdfBuilder.MARGIN + 10f
    val col2X = PdfBuilder.MARGIN + 260f
    var curY = startY + 16f

    // Row 1
    canvas.drawText("Student Name:", col1X, curY, labelPaint)
    canvas.drawText(data.studentName, col1X + 70f, curY, valuePaint)

    canvas.drawText("Class:", col2X, curY, labelPaint)
    canvas.drawText(data.className, col2X + 60f, curY, valuePaint)
    curY += 14f

    // Row 2
    canvas.drawText("Admission No.:", col1X, curY, labelPaint)
    canvas.drawText(data.admissionNumber.ifBlank { "Not set" }, col1X + 70f, curY, valuePaint)

    canvas.drawText("Attendance:", col2X, curY, labelPaint)
    val pctStr = String.format(Locale.US, "%.1f%%", data.percentage)
    val pctColor = if (data.percentage < 75.0) PdfBuilder.COLOR_RED else PdfBuilder.COLOR_GREEN
    val statPaint = Paint().apply {
      color = pctColor
      textSize = 8.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }
    canvas.drawText("${data.attended} of ${data.total} ($pctStr)", col2X + 60f, curY, statPaint)
    curY += 14f

    // Row 3: Generated note
    val genPaint = Paint().apply {
      color = PdfBuilder.COLOR_TEXT_SECONDARY
      textSize = 7.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
      isAntiAlias = true
    }
    canvas.drawText("Generated on $generatedAt", col1X, curY + 2f, genPaint)

    return startY + blockHeight + 12f
  }
}
