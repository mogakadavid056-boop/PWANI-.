package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.util.SignatureBitmapHelper
import java.io.ByteArrayOutputStream
import kotlin.math.hypot

data class SignaturePath(
  val points: List<Offset>
)

@Composable
fun SignaturePad(
  modifier: Modifier = Modifier,
  onSignatureChanged: (hasSignature: Boolean, base64Generator: () -> String?) -> Unit
) {
  val paths = remember { mutableStateListOf<SignaturePath>() }
  val currentPoints = remember { mutableStateListOf<Offset>() }
  val canvasSize = remember { mutableStateOf(IntSize(1, 1)) }
  val density = LocalDensity.current
  val strokeWidthPx = with(density) { 4.dp.toPx() }
  val navyInkColor = Color(SignatureBitmapHelper.NAVY_INK_COLOR)

  fun totalStrokeLength(allPaths: List<SignaturePath>): Float {
    var total = 0f
    for (sp in allPaths) {
      for (i in 1 until sp.points.size) {
        val p1 = sp.points[i - 1]
        val p2 = sp.points[i]
        total += hypot(p2.x - p1.x, p2.y - p1.y)
      }
    }
    return total
  }

  fun hasValidStroke(): Boolean {
    val allPaths = paths.toList() + if (currentPoints.isNotEmpty()) listOf(SignaturePath(currentPoints.toList())) else emptyList()
    return totalStrokeLength(allPaths) >= 30f
  }

  fun exportBase64(): String? {
    val allPaths = paths.toList() + if (currentPoints.isNotEmpty()) listOf(SignaturePath(currentPoints.toList())) else emptyList()
    if (allPaths.isEmpty() || totalStrokeLength(allPaths) < 30f) return null

    val w = canvasSize.value.width.coerceAtLeast(1)
    val h = canvasSize.value.height.coerceAtLeast(1)
    val rawBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    rawBitmap.eraseColor(android.graphics.Color.TRANSPARENT)
    val canvas = android.graphics.Canvas(rawBitmap)

    val paint = Paint().apply {
      color = SignatureBitmapHelper.NAVY_INK_COLOR
      strokeWidth = strokeWidthPx
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.ROUND
      isAntiAlias = true
    }

    for (sp in allPaths) {
      if (sp.points.size > 1) {
        val path = android.graphics.Path()
        val first = sp.points.first()
        path.moveTo(first.x, first.y)
        for (i in 1 until sp.points.size) {
          val pt = sp.points[i]
          path.lineTo(pt.x, pt.y)
        }
        canvas.drawPath(path, paint)
      } else if (sp.points.size == 1) {
        val pt = sp.points.first()
        canvas.drawCircle(pt.x, pt.y, strokeWidthPx / 2f, paint)
      }
    }

    val cropped = SignatureBitmapHelper.cropToInkBounds(rawBitmap, paddingPx = 12) ?: return null
    val finalBitmap = if (cropped.width > 500) {
      val scaledH = ((cropped.height.toFloat() / cropped.width.toFloat()) * 500f).toInt().coerceAtLeast(1)
      Bitmap.createScaledBitmap(cropped, 500, scaledH, true)
    } else {
      cropped
    }

    val stream = ByteArrayOutputStream()
    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
  }

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Draw, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Draw Signature", fontSize = 14.sp, color = GoldLight)
      }
      OutlinedButton(
        onClick = {
          paths.clear()
          currentPoints.clear()
          onSignatureChanged(false) { null }
        },
        modifier = Modifier.height(34.dp).testTag("clear_signature_button"),
        border = BorderStroke(1.5.dp, GoldPrimary),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldLight),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
      ) {
        Icon(Icons.Default.Clear, contentDescription = null, tint = GoldLight, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Clear", fontSize = 14.sp, color = GoldLight, maxLines = 1, softWrap = false)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(150.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(Color.White)
        .border(1.5.dp, DarkBorder, RoundedCornerShape(12.dp))
        .onSizeChanged { canvasSize.value = it }
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = { offset ->
              currentPoints.clear()
              currentPoints.add(offset)
            },
            onDrag = { change, _ ->
              change.consume()
              currentPoints.add(change.position)
            },
            onDragEnd = {
              if (currentPoints.isNotEmpty()) {
                paths.add(SignaturePath(currentPoints.toList()))
                currentPoints.clear()
                onSignatureChanged(hasValidStroke()) { exportBase64() }
              }
            },
            onDragCancel = {
              currentPoints.clear()
            }
          )
        }
        .testTag("signature_canvas")
    ) {
      if (paths.isEmpty() && currentPoints.isEmpty()) {
        Text(
          text = "Sign here with your finger",
          fontSize = 13.sp,
          color = Color(0xFF6B7280),
          modifier = Modifier.align(Alignment.Center)
        )
      }

      Canvas(modifier = Modifier.fillMaxSize()) {
        paths.forEach { sp ->
          if (sp.points.size > 1) {
            val composePath = Path().apply {
              moveTo(sp.points[0].x, sp.points[0].y)
              for (i in 1 until sp.points.size) {
                lineTo(sp.points[i].x, sp.points[i].y)
              }
            }
            drawPath(
              path = composePath,
              color = navyInkColor,
              style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
          }
        }
        if (currentPoints.size > 1) {
          val composePath = Path().apply {
            moveTo(currentPoints[0].x, currentPoints[0].y)
            for (i in 1 until currentPoints.size) {
              lineTo(currentPoints[i].x, currentPoints[i].y)
            }
          }
          drawPath(
            path = composePath,
            color = navyInkColor,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
          )
        }
      }
    }
  }
}
