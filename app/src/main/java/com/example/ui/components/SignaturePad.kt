package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.TextSecondary
import java.io.ByteArrayOutputStream

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
  val canvasSize = remember { androidx.compose.runtime.mutableStateOf(IntSize(1, 1)) }

  fun exportBase64(): String? {
    if (paths.isEmpty() && currentPoints.isEmpty()) return null
    val targetWidth = 300
    val targetHeight = 150
    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val scaleX = if (canvasSize.value.width > 0) targetWidth.toFloat() / canvasSize.value.width else 1f
    val scaleY = if (canvasSize.value.height > 0) targetHeight.toFloat() / canvasSize.value.height else 1f

    val paint = Paint().apply {
      color = android.graphics.Color.WHITE
      strokeWidth = 3.5f
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.ROUND
      isAntiAlias = true
    }

    val allPaths = paths.toList() + if (currentPoints.isNotEmpty()) listOf(SignaturePath(currentPoints.toList())) else emptyList()
    for (sp in allPaths) {
      if (sp.points.size > 1) {
        val path = android.graphics.Path()
        val first = sp.points.first()
        path.moveTo(first.x * scaleX, first.y * scaleY)
        for (i in 1 until sp.points.size) {
          val pt = sp.points[i]
          path.lineTo(pt.x * scaleX, pt.y * scaleY)
        }
        canvas.drawPath(path, paint)
      } else if (sp.points.size == 1) {
        val pt = sp.points.first()
        canvas.drawCircle(pt.x * scaleX, pt.y * scaleY, 2f, paint)
      }
    }

    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    val byteArray = stream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
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
        Text("Draw Signature", fontSize = 13.sp, color = GoldLight)
      }
      OutlinedButton(
        onClick = {
          paths.clear()
          currentPoints.clear()
          onSignatureChanged(false) { null }
        },
        modifier = Modifier.height(30.dp).testTag("clear_signature_button"),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
        shape = RoundedCornerShape(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
      ) {
        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Clear", fontSize = 11.sp)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(150.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(DarkSurface)
        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
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
                onSignatureChanged(true) { exportBase64() }
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
          fontSize = 12.sp,
          color = TextSecondary.copy(alpha = 0.6f),
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
              color = Color.White,
              style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
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
            color = Color.White,
            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
          )
        }
      }
    }
  }
}
