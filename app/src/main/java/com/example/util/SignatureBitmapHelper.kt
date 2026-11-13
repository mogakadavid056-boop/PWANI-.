package com.example.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.Base64

object SignatureBitmapHelper {
  const val NAVY_INK_COLOR = 0xFF1C348C.toInt()

  fun decodeTrimmedNavyBitmap(rawBase64: String?): Bitmap? {
    val trimmed = rawBase64?.trim().orEmpty()
    if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) return null
    return try {
      val clean = when {
        trimmed.startsWith("data:", ignoreCase = true) && trimmed.contains(",") ->
          trimmed.substringAfter(",")
        trimmed.contains("base64,", ignoreCase = true) ->
          trimmed.substringAfter("base64,")
        trimmed.contains(",") ->
          trimmed.substringAfter(",")
        else -> trimmed
      }.trim()
      val bytes = Base64.decode(clean, Base64.DEFAULT)
      val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
      val cropped = cropToInkBounds(src) ?: return null
      tintToNavy(cropped)
    } catch (_: Exception) {
      null
    }
  }

  fun cropToInkBounds(src: Bitmap, paddingPx: Int = 4): Bitmap? {
    val w = src.width
    val h = src.height
    if (w <= 0 || h <= 0) return null

    val pixels = IntArray(w * h)
    src.getPixels(pixels, 0, w, 0, 0, w, h)

    var hasTransparent = false
    for (p in pixels) {
      if ((p ushr 24) < 250) {
        hasTransparent = true
        break
      }
    }

    var minX = w
    var minY = h
    var maxX = -1
    var maxY = -1

    for (y in 0 until h) {
      val rowOffset = y * w
      for (x in 0 until w) {
        val px = pixels[rowOffset + x]
        val alpha = px ushr 24
        val isInk = if (hasTransparent) {
          alpha > 10
        } else {
          val r = (px ushr 16) and 0xFF
          val g = (px ushr 8) and 0xFF
          val b = px and 0xFF
          r < 245 || g < 245 || b < 245
        }
        if (isInk) {
          if (x < minX) minX = x
          if (y < minY) minY = y
          if (x > maxX) maxX = x
          if (y > maxY) maxY = y
        }
      }
    }

    if (maxX < minX || maxY < minY) return null

    val left = (minX - paddingPx).coerceAtLeast(0)
    val top = (minY - paddingPx).coerceAtLeast(0)
    val right = (maxX + paddingPx + 1).coerceAtMost(w)
    val bottom = (maxY + paddingPx + 1).coerceAtMost(h)
    return Bitmap.createBitmap(src, left, top, (right - left).coerceAtLeast(1), (bottom - top).coerceAtLeast(1))
  }

  private fun tintToNavy(src: Bitmap): Bitmap {
    val out = Bitmap.createBitmap(src.width.coerceAtLeast(1), src.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    out.eraseColor(Color.TRANSPARENT)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
      colorFilter = PorterDuffColorFilter(NAVY_INK_COLOR, PorterDuff.Mode.SRC_IN)
    }
    canvas.drawBitmap(src, 0f, 0f, paint)
    return out
  }
}
