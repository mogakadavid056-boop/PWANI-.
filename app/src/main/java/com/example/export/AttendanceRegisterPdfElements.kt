package com.example.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.example.data.AttendanceRegisterDetail
import com.example.util.JsonHelper
import com.example.util.SignatureBitmapHelper

object AttendanceRegisterPdfElements {
  val COLOR_DARK_GREEN = Color.parseColor("#0B2A1E")
  val COLOR_GOLD = Color.parseColor("#E6B422")
  val COLOR_LIGHT_GOLD = Color.parseColor("#F7D97B")
  val COLOR_GRID = Color.parseColor("#DCDCDC")
  val COLOR_GRAY_LABEL = Color.parseColor("#666666")
  val COLOR_TEXT = Color.parseColor("#1A1A1A")
  val COLOR_GREEN = Color.parseColor("#1B5E20")
  val COLOR_RED = Color.parseColor("#D32F2F")
  val COLOR_AMBER = Color.parseColor("#B45309")

  fun drawHeaderBand(context: Context, canvas: Canvas, detail: AttendanceRegisterDetail, width: Float, margin: Float) {
    val bandPaint = Paint().apply { color = COLOR_DARK_GREEN; style = Paint.Style.FILL }
    canvas.drawRect(0f, 0f, width, 56f, bandPaint)
    val stripePaint = Paint().apply { color = COLOR_GOLD; style = Paint.Style.FILL }
    canvas.drawRect(0f, 56f, width, 59f, stripePaint)

    drawLogoInGoldRing(context, canvas, margin, 8f, 38f)

    val subPaint = Paint().apply {
      color = COLOR_LIGHT_GOLD; textSize = 8.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
    }
    canvas.drawText("Pwani University", margin + 46f, 21f, subPaint)

    val titlePaint = Paint().apply {
      color = Color.WHITE; textSize = 13f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
    }
    canvas.drawText("Class Attendance Register", margin + 46f, 38f, titlePaint)

    val regPaint = Paint().apply {
      color = COLOR_LIGHT_GOLD; textSize = 10f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT; isAntiAlias = true
    }
    canvas.drawText(JsonHelper.orDash(detail.registerNo), width - margin, 24f, regPaint)

    val statusPaint = Paint().apply {
      color = Color.WHITE; textSize = 8.5f; textAlign = Paint.Align.RIGHT; isAntiAlias = true
    }
    canvas.drawText("STATUS: ${detail.status.uppercase()}", width - margin, 39f, statusPaint)
  }

  private fun drawLogoInGoldRing(context: Context, canvas: Canvas, x: Float, y: Float, diameter: Float) {
    val radius = diameter / 2f
    val cx = x + radius
    val cy = y + radius
    val ringPaint = Paint().apply { color = COLOR_GOLD; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }
    canvas.drawCircle(cx, cy, radius, ringPaint)

    val logoId = context.resources.getIdentifier("ic_classrep_logo", "drawable", context.packageName)
    if (logoId != 0) {
      ContextCompat.getDrawable(context, logoId)?.let { d ->
        val save = canvas.save()
        canvas.clipPath(Path().apply { addCircle(cx, cy, radius - 1.5f, Path.Direction.CCW) })
        d.setBounds((x + 2).toInt(), (y + 2).toInt(), (x + diameter - 2).toInt(), (y + diameter - 2).toInt())
        d.draw(canvas)
        canvas.restoreToCount(save)
      }
    }
  }

  fun drawInfoGrid(canvas: Canvas, d: AttendanceRegisterDetail, startY: Float, width: Float, margin: Float): Float {
    val rowH = 26f
    val usableW = width - (2 * margin)
    val halfW = usableW / 2f
    val col1X = margin
    val col2X = margin + halfW + 12f
    val linePaint = Paint().apply { color = COLOR_GRID; strokeWidth = 0.6f }
    val labelPaint = Paint().apply { color = COLOR_GRAY_LABEL; textSize = 7.5f; isAntiAlias = true }
    val valuePaint = Paint().apply {
      color = COLOR_TEXT; textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
    }

    val gridData = listOf(
      Pair("COURSE TITLE", JsonHelper.orDash(d.courseName)) to Pair("LECTURER IN CHARGE", JsonHelper.orDash(d.lecturerName)),
      Pair("COURSE CODE", JsonHelper.orDash(d.courseCode)) to Pair("DATE", JsonHelper.orDash(d.dateText)),
      Pair("CLASS", JsonHelper.orDash(d.className)) to Pair("TIME", JsonHelper.orDash(d.timeText)),
      Pair("VENUE", JsonHelper.orDash(d.venue)) to Pair("PRESENT", "${d.presentCount} of ${d.total}")
    )

    var y = startY
    for ((c1, c2) in gridData) {
      canvas.drawText(c1.first, col1X, y + 9f, labelPaint)
      canvas.drawText(ellipsize(c1.second, valuePaint, halfW - 12f), col1X, y + 21f, valuePaint)
      canvas.drawLine(col1X, y + rowH, col1X + halfW - 10f, y + rowH, linePaint)

      canvas.drawText(c2.first, col2X, y + 9f, labelPaint)
      canvas.drawText(ellipsize(c2.second, valuePaint, width - margin - col2X - 4f), col2X, y + 21f, valuePaint)
      canvas.drawLine(col2X, y + rowH, width - margin, y + rowH, linePaint)
      y += rowH + 2f
    }
    return y
  }

  fun drawSignatureBoxes(canvas: Canvas, detail: AttendanceRegisterDetail, startY: Float, width: Float, margin: Float) {
    val boxW = 248f
    val boxH = 96f
    val leftX = margin
    val rightX = width - margin - boxW

    drawBox(canvas, leftX, startY, boxW, boxH, "Class Prefect", JsonHelper.orDash(detail.prefectName), detail.prefectSignature, detail.prefectSignedText)
    drawBox(canvas, rightX, startY, boxW, boxH, "Lecturer in charge", JsonHelper.orDash(detail.lecturerName), detail.lecturerSignature, detail.lecturerSignedText)
  }

  private fun drawBox(
    canvas: Canvas, x: Float, y: Float, w: Float, h: Float,
    title: String, name: String, sig: String?, signedText: String
  ) {
    val bg = Paint().apply { color = Color.parseColor("#FAFAFA"); style = Paint.Style.FILL }
    val border = Paint().apply { color = COLOR_GRID; style = Paint.Style.STROKE; strokeWidth = 0.8f }
    canvas.drawRoundRect(RectF(x, y, x + w, y + h), 4f, 4f, bg)
    canvas.drawRoundRect(RectF(x, y, x + w, y + h), 4f, 4f, border)

    val labelP = Paint().apply { color = COLOR_GRAY_LABEL; textSize = 7.5f; isAntiAlias = true }
    val nameP = Paint().apply { color = COLOR_TEXT; textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
    canvas.drawText(title, x + 10f, y + 13f, labelP)
    canvas.drawText(ellipsize(name, nameP, w - 20f), x + 10f, y + 26f, nameP)

    val sigAreaY = y + 30f
    val sigAreaH = h - 46f
    if (!sig.isNullOrBlank()) {
      val drawn = drawSig(canvas, sig, x + 10f, sigAreaY, w - 20f, sigAreaH)
      if (!drawn) {
        drawSignedFallback(canvas, x + 10f, sigAreaY + (sigAreaH / 2f) + 4f)
      }
    }

    val cleanSigned = signedText.trim().takeIf { it.isNotBlank() && !it.equals("null", true) }
    val dateStr = if (cleanSigned != null) "Signed: $cleanSigned" else "Not signed yet"
    canvas.drawText(ellipsize(dateStr, labelP, w - 20f), x + 10f, y + h - 7f, labelP)
  }

  fun drawSignedFallback(canvas: Canvas, x: Float, baselineY: Float) {
    val p = Paint().apply {
      color = COLOR_GREEN
      textSize = 9.5f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      isAntiAlias = true
    }
    canvas.drawText("Signed", x, baselineY, p)
  }

  fun drawSig(canvas: Canvas, base64Str: String, x: Float, y: Float, maxW: Float, maxH: Float): Boolean {
    val bmp = SignatureBitmapHelper.decodeTrimmedNavyBitmap(base64Str) ?: return false
    val availW = maxW * 0.85f
    val availH = maxH * 0.80f
    val scale = (availW / bmp.width.toFloat()).coerceAtMost(availH / bmp.height.toFloat())
    val dw = (bmp.width * scale).coerceAtLeast(1f)
    val dh = (bmp.height * scale).coerceAtLeast(1f)
    val left = x + (maxW - dw) / 2f
    val top = y + (maxH - dh) / 2f
    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
      colorFilter = PorterDuffColorFilter(SignatureBitmapHelper.NAVY_INK_COLOR, PorterDuff.Mode.SRC_IN)
    }
    canvas.drawBitmap(bmp, null, RectF(left, top, left + dw, top + dh), paint)
    return true
  }

  fun drawStatusWatermark(canvas: Canvas, status: String, voidReason: String?, width: Float, height: Float) {
    val st = status.trim().lowercase()
    if (st != "voided" && st != "open") return
    val save = canvas.save()
    canvas.rotate(-32f, width / 2f, height / 2f)
    if (st == "voided") {
      val wmPaint = Paint().apply {
        color = COLOR_RED; alpha = 36; textSize = 72f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER; isAntiAlias = true
      }
      canvas.drawText("VOIDED", width / 2f, height / 2f, wmPaint)
      if (!voidReason.isNullOrBlank()) {
        val rPaint = Paint().apply {
          color = COLOR_RED; alpha = 48; textSize = 16f; textAlign = Paint.Align.CENTER; isAntiAlias = true
        }
        canvas.drawText("Reason: $voidReason", width / 2f, (height / 2f) + 34f, rPaint)
      }
    } else {
      val wmPaint = Paint().apply {
        color = COLOR_AMBER; alpha = 28; textSize = 46f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER; isAntiAlias = true
      }
      canvas.drawText("OPEN - NOT FINAL", width / 2f, height / 2f, wmPaint)
    }
    canvas.restoreToCount(save)
  }

  fun drawFooter(canvas: Canvas, detail: AttendanceRegisterDetail, curPage: Int, totalPages: Int, width: Float, height: Float, margin: Float) {
    val y = height - 16f
    val fPaint = Paint().apply { color = COLOR_GRAY_LABEL; textSize = 7.5f; isAntiAlias = true }
    canvas.drawText("Generated by ClassRep on ${JsonHelper.orDash(detail.generatedText)}", margin, y, fPaint)
    val pagePaint = Paint().apply { color = COLOR_GRAY_LABEL; textSize = 7.5f; textAlign = Paint.Align.RIGHT; isAntiAlias = true }
    canvas.drawText("Page $curPage of $totalPages", width - margin, y, pagePaint)
  }

  fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    var end = text.length
    while (end > 0 && paint.measureText(text.substring(0, end) + "...") > maxWidth) end--
    return if (end <= 0) "" else text.substring(0, end) + "..."
  }
}
