package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InkStroke
import com.example.data.MarkingStamp
import com.example.data.NormalizedPoint
import com.example.data.QuestionMarkingDraft
import com.example.data.StampKind
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.RoyalSurface
import com.example.ui.theme.SilverWhite
import com.example.ui.theme.SuccessColor

enum class MarkingTool(val label: String) {
  PAN("Scroll"),
  PEN("Pen"),
  TICK("Tick \u2713"),
  CROSS("Cross \u2717")
}

@Composable
fun InkMarkingAnswerSheet(
  studentAnswer: String,
  draft: QuestionMarkingDraft,
  activeTool: MarkingTool,
  penColorHex: Long,
  isEditable: Boolean,
  onAddStroke: (InkStroke) -> Unit,
  onAddStamp: (MarkingStamp) -> Unit,
  modifier: Modifier = Modifier
) {
  val textMeasurer = rememberTextMeasurer()
  val activePoints = remember { mutableStateListOf<NormalizedPoint>() }
  val latestTool by rememberUpdatedState(activeTool)
  val latestPenHex by rememberUpdatedState(penColorHex)
  val latestOnAddStroke by rememberUpdatedState(onAddStroke)
  val latestOnAddStamp by rememberUpdatedState(onAddStamp)

  val borderColor = if (isEditable && activeTool != MarkingTool.PAN) ElectricViolet else DarkBorder
  val shape = RoundedCornerShape(12.dp)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 160.dp)
      .clip(shape)
      .background(RoyalSurface)
      .border(if (activeTool != MarkingTool.PAN && isEditable) 1.5.dp else 1.dp, borderColor, shape)
  ) {
    Text(
      text = studentAnswer.ifBlank { "(No answer submitted)" },
      style = BodyStyle.copy(color = SilverWhite),
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    )

    val interactionMod = when {
      !isEditable || activeTool == MarkingTool.PAN -> Modifier
      activeTool == MarkingTool.PEN -> Modifier.pointerInput(activeTool, penColorHex) {
        detectDragGestures(
          onDragStart = { offset ->
            activePoints.clear()
            val w = size.width.toFloat().coerceAtLeast(1f)
            val h = size.height.toFloat().coerceAtLeast(1f)
            activePoints.add(NormalizedPoint((offset.x / w).coerceIn(0f, 1f), (offset.y / h).coerceIn(0f, 1f)))
          },
          onDrag = { change, _ ->
            change.consume()
            val w = size.width.toFloat().coerceAtLeast(1f)
            val h = size.height.toFloat().coerceAtLeast(1f)
            activePoints.add(NormalizedPoint((change.position.x / w).coerceIn(0f, 1f), (change.position.y / h).coerceIn(0f, 1f)))
          },
          onDragEnd = {
            if (activePoints.isNotEmpty()) {
              latestOnAddStroke(InkStroke(points = activePoints.toList(), colorHex = latestPenHex, strokeWidth = 3.5f))
              activePoints.clear()
            }
          },
          onDragCancel = {
            activePoints.clear()
          }
        )
      }
      else -> Modifier.pointerInput(activeTool) {
        detectTapGestures { offset ->
          val w = size.width.toFloat().coerceAtLeast(1f)
          val h = size.height.toFloat().coerceAtLeast(1f)
          val kind = if (latestTool == MarkingTool.TICK) StampKind.TICK else StampKind.CROSS
          latestOnAddStamp(
            MarkingStamp(
              kind = kind,
              x = (offset.x / w).coerceIn(0.02f, 0.96f),
              y = (offset.y / h).coerceIn(0.05f, 0.95f)
            )
          )
        }
      }
    }

    Canvas(
      modifier = Modifier
        .matchParentSize()
        .then(interactionMod)
    ) {
      val w = size.width
      val h = size.height

      draft.strokes.forEach { stroke ->
        drawInkStroke(stroke.points, Color(stroke.colorHex.toInt()), stroke.strokeWidth, w, h)
      }
      if (activePoints.isNotEmpty()) {
        drawInkStroke(activePoints, Color(penColorHex.toInt()), 3.5f, w, h)
      }

      draft.stamps.forEach { stamp ->
        drawStampGlyph(textMeasurer, stamp, w, h)
      }
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawInkStroke(
  points: List<NormalizedPoint>,
  color: Color,
  strokeWidth: Float,
  w: Float,
  h: Float
) {
  if (points.isEmpty()) return
  val strokePx = strokeWidth.dp.toPx() * 0.7f
  if (points.size == 1) {
    val pt = points.first()
    drawCircle(color = color, radius = strokePx / 1.5f, center = Offset(pt.x * w, pt.y * h))
  } else {
    val path = Path()
    points.forEachIndexed { idx, pt ->
      val px = pt.x * w
      val py = pt.y * h
      if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    drawPath(
      path = path,
      color = color,
      style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStampGlyph(
  textMeasurer: TextMeasurer,
  stamp: MarkingStamp,
  w: Float,
  h: Float
) {
  val color = if (stamp.kind == StampKind.TICK) SuccessColor else ErrorColor
  val text = if (stamp.label.isNotBlank()) "${stamp.kind.symbol} ${stamp.label}" else stamp.kind.symbol
  val result = textMeasurer.measure(
    text = text,
    style = TextStyle(color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
  )
  val x = (stamp.x * w - result.size.width / 2f).coerceIn(0f, (w - result.size.width).coerceAtLeast(0f))
  val y = (stamp.y * h - result.size.height / 2f).coerceIn(0f, (h - result.size.height).coerceAtLeast(0f))
  drawText(result, topLeft = Offset(x, y))
}
