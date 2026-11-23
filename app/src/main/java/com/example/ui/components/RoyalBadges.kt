package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.GoldShadow
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SilverWhite
import com.example.util.RoleHelper

@Composable
fun VioletGemIcon(
  modifier: Modifier = Modifier,
  size: Dp = 14.dp,
  tint: Color = ElectricViolet
) {
  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val gem = Path().apply {
      moveTo(w * 0.50f, h * 0.06f)
      lineTo(w * 0.94f, h * 0.50f)
      lineTo(w * 0.50f, h * 0.94f)
      lineTo(w * 0.06f, h * 0.50f)
      close()
    }
    drawPath(
      path = gem,
      brush = Brush.linearGradient(listOf(Lavender, tint, RoyalPurple))
    )
    drawPath(
      path = gem,
      color = Lavender.copy(alpha = 0.85f),
      style = Stroke(width = 1.dp.toPx())
    )
  }
}

@Composable
fun GoldGemIcon(
  modifier: Modifier = Modifier,
  size: Dp = 14.dp
) {
  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val gem = Path().apply {
      moveTo(w * 0.50f, h * 0.08f)
      lineTo(w * 0.92f, h * 0.50f)
      lineTo(w * 0.50f, h * 0.92f)
      lineTo(w * 0.08f, h * 0.50f)
      close()
    }
    drawPath(
      path = gem,
      brush = Brush.linearGradient(listOf(GoldHighlight, ChampagneGold, MetallicGold))
    )
  }
}

@Composable
fun RoleBadge(
  role: String?,
  modifier: Modifier = Modifier,
  testTag: String = "role_badge"
) {
  val normalized = RoleHelper.normalizeRole(role)
  val label = RoleHelper.formatDisplayName(role)

  val (bgBrush, borderColor, textColor) = when (normalized) {
    RoleHelper.ROLE_ADMIN -> Triple(
      Brush.horizontalGradient(listOf(ChampagneGold, MetallicGold)),
      GoldHighlight.copy(alpha = 0.7f),
      RoyalPurple
    )
    RoleHelper.ROLE_LECTURER -> Triple(
      Brush.horizontalGradient(listOf(RoyalSurfaceHigh, RoyalSurfaceHigh)),
      MetallicGold.copy(alpha = 0.7f),
      ChampagneGold
    )
    RoleHelper.ROLE_CLASS_REP -> Triple(
      Brush.horizontalGradient(listOf(RoyalPurple.copy(alpha = 0.85f), RoyalSurfaceHigh)),
      ElectricViolet.copy(alpha = 0.8f),
      Lavender
    )
    else -> Triple(
      Brush.horizontalGradient(listOf(RoyalSurfaceHigh.copy(alpha = 0.7f), RoyalSurfaceHigh.copy(alpha = 0.7f))),
      SilverWhite.copy(alpha = 0.55f),
      SilverWhite
    )
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(bgBrush)
      .border(1.dp, borderColor, RoundedCornerShape(10.dp))
      .padding(horizontal = 10.dp, vertical = 4.dp)
      .testTag(testTag)
  ) {
    when (normalized) {
      RoleHelper.ROLE_ADMIN -> {
        RoyalCrownIcon(size = 14.dp)
        Spacer(modifier = Modifier.width(6.dp))
      }
      RoleHelper.ROLE_LECTURER -> {
        Icon(
          imageVector = Icons.Default.School,
          contentDescription = null,
          tint = MetallicGold,
          modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
      }
      RoleHelper.ROLE_CLASS_REP -> {
        VioletGemIcon(size = 13.dp)
        Spacer(modifier = Modifier.width(6.dp))
      }
      else -> {
        Box(
          modifier = Modifier
            .size(8.dp)
            .border(1.2.dp, SilverWhite, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
      }
    }

    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium.copy(
        fontSize = 12.sp,
        color = textColor
      )
    )
  }
}

@Composable
fun CornerFlourishOverlay(
  modifier: Modifier = Modifier,
  color: Color = ChampagneGold.copy(alpha = 0.45f)
) {
  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    val arm = 18.dp.toPx()
    val pad = 8.dp.toPx()
    val stroke = 1.3.dp.toPx()

    // Top-left L ornament
    drawLine(color, Offset(pad, pad), Offset(pad + arm, pad), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(pad, pad), Offset(pad, pad + arm), strokeWidth = stroke, cap = StrokeCap.Round)
    drawCircle(color, radius = 2.dp.toPx(), center = Offset(pad + arm * 0.45f, pad + arm * 0.45f))

    // Bottom-right L ornament
    drawLine(color, Offset(w - pad, h - pad), Offset(w - pad - arm, h - pad), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(w - pad, h - pad), Offset(w - pad, h - pad - arm), strokeWidth = stroke, cap = StrokeCap.Round)
    drawCircle(color, radius = 2.dp.toPx(), center = Offset(w - pad - arm * 0.45f, h - pad - arm * 0.45f))
  }
}
