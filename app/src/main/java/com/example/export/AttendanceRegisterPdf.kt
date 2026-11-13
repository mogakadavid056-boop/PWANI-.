package com.example.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.AttendanceRegisterDetail
import com.example.data.AttendanceRegisterRowItem
import com.example.util.JsonHelper
import java.io.File
import java.io.FileOutputStream

object AttendanceRegisterPdf {
  const val PAGE_WIDTH = 595
  const val PAGE_HEIGHT = 842
  const val MARGIN = 36f
  const val ROW_HEIGHT = 58f
  const val HEADER_HEIGHT = 22f
  const val SIG_BOX_HEIGHT = 96f
  const val MAX_CONTENT_Y = 806f

  val COLOR_DARK_GREEN = Color.parseColor("#0B2A1E")
  val COLOR_GOLD = Color.parseColor("#E6B422")
  val COLOR_ZEBRA = Color.parseColor("#F4F7F5")
  val COLOR_GRID = Color.parseColor("#DCDCDC")
  val COLOR_TEXT = Color.parseColor("#1A1A1A")
  val COLOR_MUTED = Color.parseColor("#666666")

  fun generatePdfFile(context: Context, detail: AttendanceRegisterDetail): File {
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val cleanRegNo = detail.registerNo.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "register" }
    val file = File(exportDir, "$cleanRegNo.pdf")

    val pages = paginateRows(detail.rows)
    val totalPages = pages.size
    val document = PdfDocument()

    pages.forEachIndexed { pageIndex, pageRows ->
      val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
      val page = document.startPage(pageInfo)
      drawPage(context, page.canvas, detail, pageIndex, totalPages, pageRows)
      document.finishPage(page)
    }

    FileOutputStream(file).use { out -> document.writeTo(out) }
    document.close()
    return file
  }

  private fun paginateRows(rows: List<AttendanceRegisterRowItem>): List<List<AttendanceRegisterRowItem>> {
    val pages = mutableListOf<List<AttendanceRegisterRowItem>>()
    val p1TableStartY = 66f + (4 * 28f) + 10f // ~188f
    val subTableStartY = 72f

    var idx = 0
    val p1Rows = mutableListOf<AttendanceRegisterRowItem>()
    var y = p1TableStartY + HEADER_HEIGHT
    while (idx < rows.size && y + ROW_HEIGHT <= MAX_CONTENT_Y) {
      p1Rows.add(rows[idx])
      idx++
      y += ROW_HEIGHT
    }
    pages.add(p1Rows)

    while (idx < rows.size) {
      val pageRows = mutableListOf<AttendanceRegisterRowItem>()
      y = subTableStartY + HEADER_HEIGHT
      while (idx < rows.size && y + ROW_HEIGHT <= MAX_CONTENT_Y) {
        pageRows.add(rows[idx])
        idx++
        y += ROW_HEIGHT
      }
      pages.add(pageRows)
    }

    val lastRowsCount = pages.last().size
    val lastBaseY = if (pages.size == 1) p1TableStartY else subTableStartY
    val lastTableEndY = if (lastRowsCount > 0) lastBaseY + HEADER_HEIGHT + (lastRowsCount * ROW_HEIGHT) else lastBaseY
    if (lastTableEndY + 14f + SIG_BOX_HEIGHT > MAX_CONTENT_Y) {
      pages.add(emptyList())
    }
    return pages
  }

  private fun drawPage(
    context: Context,
    canvas: Canvas,
    detail: AttendanceRegisterDetail,
    pageIndex: Int,
    totalPages: Int,
    pageRows: List<AttendanceRegisterRowItem>
  ) {
    canvas.drawColor(Color.WHITE)
    AttendanceRegisterPdfElements.drawStatusWatermark(
      canvas, detail.status, detail.voidReason, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat()
    )
    AttendanceRegisterPdfElements.drawHeaderBand(context, canvas, detail, PAGE_WIDTH.toFloat(), MARGIN)

    var curY = if (pageIndex == 0) {
      AttendanceRegisterPdfElements.drawInfoGrid(canvas, detail, 66f, PAGE_WIDTH.toFloat(), MARGIN) + 10f
    } else {
      72f
    }

    if (pageRows.isNotEmpty()) {
      curY = drawTable(canvas, pageRows, curY)
    }

    if (pageIndex == totalPages - 1) {
      val sigY = (MAX_CONTENT_Y - SIG_BOX_HEIGHT - 4f).coerceAtLeast(curY + 14f)
      AttendanceRegisterPdfElements.drawSignatureBoxes(canvas, detail, sigY, PAGE_WIDTH.toFloat(), MARGIN)
    }

    AttendanceRegisterPdfElements.drawFooter(
      canvas, detail, pageIndex + 1, totalPages, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), MARGIN
    )
  }

  private fun drawTable(canvas: Canvas, rows: List<AttendanceRegisterRowItem>, startY: Float): Float {
    var curY = startY
    val colW = floatArrayOf(28f, 169f, 98f, 54f, 54f, 120f)

    val hPaint = Paint().apply { color = COLOR_DARK_GREEN; style = Paint.Style.FILL }
    canvas.drawRect(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + HEADER_HEIGHT, hPaint)

    val thText = Paint().apply {
      color = Color.WHITE; textSize = 8f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
    }
    val titles = arrayOf("No.", "Student name", "Admission no.", "Time in", "Time out", "Signature")
    var x = MARGIN
    for (i in titles.indices) {
      canvas.drawText(titles[i], x + 5f, curY + 14f, thText)
      x += colW[i]
    }
    curY += HEADER_HEIGHT

    val bgPaint = Paint().apply { style = Paint.Style.FILL }
    val borderPaint = Paint().apply { color = COLOR_GRID; strokeWidth = 0.6f; style = Paint.Style.STROKE }
    val textPaint = Paint().apply { color = COLOR_TEXT; textSize = 8.5f; isAntiAlias = true }
    val manualPaint = Paint().apply { color = COLOR_MUTED; textSize = 7.2f; isAntiAlias = true }
    val absentPaint = Paint().apply { color = Color.GRAY; textSize = 8.5f; isAntiAlias = true }

    rows.forEachIndexed { idx, row ->
      bgPaint.color = if (idx % 2 == 1) COLOR_ZEBRA else Color.WHITE
      canvas.drawRect(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + ROW_HEIGHT, bgPaint)
      canvas.drawRect(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + ROW_HEIGHT, borderPaint)

      val centerY = curY + (ROW_HEIGHT / 2f) + 3f
      var rx = MARGIN
      canvas.drawText("${row.n}", rx + 8f, centerY, textPaint)
      rx += colW[0]

      val nameMaxW = if (row.isPrefect) colW[1] - 48f else colW[1] - 8f
      val nameText = AttendanceRegisterPdfElements.ellipsize(JsonHelper.orDash(row.fullName), textPaint, nameMaxW)
      val nameBaseline = if (row.present && row.manual) centerY - 5f else centerY
      canvas.drawText(nameText, rx + 4f, nameBaseline, textPaint)
      if (row.present && row.manual) {
        canvas.drawText("(marked by staff)", rx + 4f, nameBaseline + 11f, manualPaint)
      }
      if (row.isPrefect) {
        drawPrefectPill(canvas, rx + 8f + textPaint.measureText(nameText), nameBaseline - 9f)
      }
      rx += colW[1]

      canvas.drawText(AttendanceRegisterPdfElements.ellipsize(JsonHelper.orDash(row.admissionNumber), textPaint, colW[2] - 8f), rx + 4f, centerY, textPaint)
      rx += colW[2]

      val tIn = if (row.present) JsonHelper.orDash(row.timeInText) else "-"
      canvas.drawText(tIn, rx + 8f, centerY, if (row.present) textPaint else absentPaint)
      rx += colW[3]

      val tOut = if (row.present) JsonHelper.orDash(row.timeOutText) else "-"
      canvas.drawText(tOut, rx + 8f, centerY, if (row.present) textPaint else absentPaint)
      rx += colW[4]

      if (!row.present) {
        canvas.drawText("Absent", rx + 18f, centerY, absentPaint)
      } else if (!row.signature.isNullOrBlank()) {
        val drawn = AttendanceRegisterPdfElements.drawSig(canvas, row.signature, rx + 4f, curY + 3f, colW[5] - 8f, ROW_HEIGHT - 6f)
        if (!drawn) {
          AttendanceRegisterPdfElements.drawSignedFallback(canvas, rx + 18f, centerY)
        }
      } else {
        AttendanceRegisterPdfElements.drawSignedFallback(canvas, rx + 18f, centerY)
      }

      curY += ROW_HEIGHT
    }
    return curY
  }

  private fun drawPrefectPill(canvas: Canvas, x: Float, y: Float) {
    val pillPaint = Paint().apply { color = COLOR_GOLD; style = Paint.Style.FILL; isAntiAlias = true }
    canvas.drawRoundRect(RectF(x, y, x + 36f, y + 12f), 6f, 6f, pillPaint)
    val textP = Paint().apply {
      color = COLOR_DARK_GREEN; textSize = 6.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
    }
    canvas.drawText("Prefect", x + 5f, y + 9f, textP)
  }
}
