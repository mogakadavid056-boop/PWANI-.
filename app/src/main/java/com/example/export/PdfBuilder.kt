package com.example.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.example.data.ClassPdfMetadata

object PdfBuilder {
  const val PAGE_WIDTH = 595f
  const val PAGE_HEIGHT = 842f
  const val MARGIN = 36f
  const val USABLE_WIDTH = PAGE_WIDTH - (2 * MARGIN) // 523f
  const val FOOTER_LINE_Y = 800f
  const val MAX_CONTENT_Y = 792f

  val COLOR_DARK_GREEN = Color.parseColor("#0B2A1E")
  val COLOR_GOLD = Color.parseColor("#D4A017")
  val COLOR_GOLD_HEADER = Color.parseColor("#EEDB98")
  val COLOR_ZEBRA_LIGHT = Color.parseColor("#F4F1E6")
  val COLOR_ZEBRA_WHITE = Color.parseColor("#FFFFFF")
  val COLOR_BORDER = Color.parseColor("#D0CBC0")
  val COLOR_TEXT_PRIMARY = Color.parseColor("#1A1A1A")
  val COLOR_TEXT_SECONDARY = Color.parseColor("#555555")
  val COLOR_GREEN = Color.parseColor("#1B5E20")
  val COLOR_RED = Color.parseColor("#C62828")

  data class PdfColumn(
    val title: String,
    val width: Float,
    val align: Paint.Align = Paint.Align.LEFT
  )

  sealed class PdfCell {
    data class Text(
      val text: String,
      val color: Int = COLOR_TEXT_PRIMARY,
      val isBold: Boolean = false,
      val fontSize: Float = 8.5f
    ) : PdfCell()

    data class Image(val bitmap: Bitmap?) : PdfCell()
  }

  fun drawBackground(canvas: Canvas) {
    val paint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
    canvas.drawRect(0f, 0f, PAGE_WIDTH, PAGE_HEIGHT, paint)
  }

  fun drawHeaderBand(canvas: Canvas, context: Context, documentTitle: String) {
    val bandPaint = Paint().apply { color = COLOR_DARK_GREEN; style = Paint.Style.FILL }
    canvas.drawRect(0f, 0f, PAGE_WIDTH, 48f, bandPaint)

    val linePaint = Paint().apply {
      color = COLOR_GOLD
      strokeWidth = 1.5f
      style = Paint.Style.STROKE
    }
    canvas.drawLine(0f, 48f, PAGE_WIDTH, 48f, linePaint)

    var leftX = MARGIN
    val logoResId = context.resources.getIdentifier("logo_classrep", "drawable", context.packageName)
    if (logoResId != 0) {
      val drawable = ContextCompat.getDrawable(context, logoResId)
      if (drawable != null) {
        drawable.setBounds(MARGIN.toInt(), 12, (MARGIN + 24).toInt(), 36)
        drawable.draw(canvas)
        leftX += 30f
      }
    }

    val brandPaint = Paint().apply {
      color = Color.WHITE
      textSize = 14f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }
    canvas.drawText("ClassRep", leftX, 23f, brandPaint)

    val subBrandPaint = Paint().apply {
      color = Color.parseColor("#D4D4D4")
      textSize = 7.5f
      typeface = Typeface.DEFAULT
      isAntiAlias = true
    }
    canvas.drawText("University Class Coordination", leftX, 36f, subBrandPaint)

    val titlePaint = Paint().apply {
      color = COLOR_GOLD
      textSize = 12f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textAlign = Paint.Align.RIGHT
      isAntiAlias = true
    }
    canvas.drawText(documentTitle, PAGE_WIDTH - MARGIN, 29f, titlePaint)
  }

  fun drawInfoBlock(
    canvas: Canvas,
    startY: Float,
    metadata: ClassPdfMetadata,
    generatedBy: String,
    generatedAt: String,
    sessionTitle: String? = null,
    sessionDateTime: String? = null
  ): Float {
    var curY = startY + 4f
    val bgPaint = Paint().apply { color = Color.parseColor("#FAF8F2"); style = Paint.Style.FILL }
    val borderPaint = Paint().apply { color = COLOR_BORDER; style = Paint.Style.STROKE; strokeWidth = 0.8f }
    val blockHeight = if (sessionTitle != null) 74f else 62f
    canvas.drawRoundRect(RectF(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + blockHeight), 4f, 4f, bgPaint)
    canvas.drawRoundRect(RectF(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + blockHeight), 4f, 4f, borderPaint)

    val labelPaint = Paint().apply {
      color = COLOR_TEXT_SECONDARY
      textSize = 8f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }
    val valuePaint = Paint().apply {
      color = COLOR_TEXT_PRIMARY
      textSize = 8.5f
      typeface = Typeface.DEFAULT
      isAntiAlias = true
    }

    val col1X = MARGIN + 10f
    val col2X = MARGIN + 260f
    curY += 12f

    // Row 1
    canvas.drawText("Institution:", col1X, curY, labelPaint)
    canvas.drawText(metadata.universityName ?: "University", col1X + 50f, curY, valuePaint)

    canvas.drawText("School / Faculty:", col2X, curY, labelPaint)
    canvas.drawText(metadata.schoolName ?: "Not specified", col2X + 68f, curY, valuePaint)
    curY += 13f

    // Row 2
    canvas.drawText("Programme:", col1X, curY, labelPaint)
    canvas.drawText(metadata.programmeName ?: "General", col1X + 50f, curY, valuePaint)

    val yearStr = metadata.yearOfStudy?.let { "Year $it" } ?: "All Years"
    canvas.drawText("Year of Study:", col2X, curY, labelPaint)
    canvas.drawText(yearStr, col2X + 68f, curY, valuePaint)
    curY += 13f

    // Row 3: Class name & Session (if session)
    canvas.drawText("Class:", col1X, curY, labelPaint)
    canvas.drawText(metadata.className, col1X + 50f, curY, valuePaint)

    if (sessionTitle != null) {
      canvas.drawText("Session:", col2X, curY, labelPaint)
      val dtStr = if (sessionDateTime != null) " ($sessionDateTime)" else ""
      canvas.drawText("$sessionTitle$dtStr", col2X + 68f, curY, valuePaint)
      curY += 13f
    }

    // Row 4: Generated by
    val genPaint = Paint().apply {
      color = COLOR_TEXT_SECONDARY
      textSize = 7.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
      isAntiAlias = true
    }
    canvas.drawText("Generated by $generatedBy on $generatedAt", col1X, curY + 2f, genPaint)

    return startY + blockHeight + 12f
  }

  fun drawTableHeader(canvas: Canvas, y: Float, columns: List<PdfColumn>, headerHeight: Float = 22f) {
    val bgPaint = Paint().apply { color = COLOR_GOLD_HEADER; style = Paint.Style.FILL }
    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + headerHeight, bgPaint)

    val borderPaint = Paint().apply { color = COLOR_BORDER; style = Paint.Style.STROKE; strokeWidth = 0.8f }
    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + headerHeight, borderPaint)

    val textPaint = Paint().apply {
      color = COLOR_TEXT_PRIMARY
      textSize = 8.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }

    var currentX = MARGIN
    columns.forEachIndexed { index, col ->
      val textX = when (col.align) {
        Paint.Align.LEFT -> currentX + 6f
        Paint.Align.CENTER -> currentX + (col.width / 2f)
        Paint.Align.RIGHT -> currentX + col.width - 6f
      }
      textPaint.textAlign = col.align
      canvas.drawText(col.title, textX, y + 14.5f, textPaint)

      if (index > 0) {
        canvas.drawLine(currentX, y, currentX, y + headerHeight, borderPaint)
      }
      currentX += col.width
    }
  }

  fun drawTableRow(
    canvas: Canvas,
    y: Float,
    rowHeight: Float,
    isEven: Boolean,
    columns: List<PdfColumn>,
    cells: List<PdfCell>
  ) {
    val bgPaint = Paint().apply {
      color = if (isEven) COLOR_ZEBRA_WHITE else COLOR_ZEBRA_LIGHT
      style = Paint.Style.FILL
    }
    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, bgPaint)

    val borderPaint = Paint().apply { color = COLOR_BORDER; style = Paint.Style.STROKE; strokeWidth = 0.5f }
    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)

    var currentX = MARGIN
    columns.forEachIndexed { index, col ->
      if (index > 0) {
        canvas.drawLine(currentX, y, currentX, y + rowHeight, borderPaint)
      }

      val cell = cells.getOrNull(index)
      if (cell is PdfCell.Text) {
        val paint = Paint().apply {
          color = cell.color
          textSize = cell.fontSize
          typeface = if (cell.isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
          textAlign = col.align
          isAntiAlias = true
        }
        val textX = when (col.align) {
          Paint.Align.LEFT -> currentX + 6f
          Paint.Align.CENTER -> currentX + (col.width / 2f)
          Paint.Align.RIGHT -> currentX + col.width - 6f
        }
        val textY = y + (rowHeight / 2f) + (cell.fontSize / 3f)
        canvas.drawText(cell.text, textX, textY, paint)
      } else if (cell is PdfCell.Image && cell.bitmap != null) {
        val bmp = cell.bitmap
        val destX = currentX + ((col.width - bmp.width) / 2f).coerceAtLeast(0f)
        val destY = y + ((rowHeight - bmp.height) / 2f).coerceAtLeast(0f)
        canvas.drawBitmap(bmp, destX, destY, null)
      }

      currentX += col.width
    }
  }

  fun drawFooter(canvas: Canvas, pageNumber: Int, totalPages: Int, generatedDate: String) {
    val linePaint = Paint().apply { color = Color.parseColor("#E0E0E0"); strokeWidth = 0.7f }
    canvas.drawLine(MARGIN, FOOTER_LINE_Y, PAGE_WIDTH - MARGIN, FOOTER_LINE_Y, linePaint)

    val footerPaint = Paint().apply {
      color = COLOR_TEXT_SECONDARY
      textSize = 7.5f
      typeface = Typeface.DEFAULT
      isAntiAlias = true
    }
    footerPaint.textAlign = Paint.Align.LEFT
    canvas.drawText("ClassRep · Generated $generatedDate", MARGIN, FOOTER_LINE_Y + 16f, footerPaint)

    footerPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText("Page $pageNumber of $totalPages", PAGE_WIDTH - MARGIN, FOOTER_LINE_Y + 16f, footerPaint)
  }
}
