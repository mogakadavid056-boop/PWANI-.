package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppBackgroundManager
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CardRadius
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.GoldShadow
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.MontserratFamily
import com.example.ui.theme.RoyalCardSurface
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.RoyalSurface
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.ScreenTitleStyle
import com.example.ui.theme.SilverWhite
import kotlin.random.Random

private data class StarSpec(
  val xFraction: Float,
  val yFraction: Float,
  val radiusDp: Float,
  val isGold: Boolean,
  val alpha: Float
)

val RoyalGoldGradientBrush = Brush.linearGradient(
  colors = listOf(GoldHighlight, ChampagneGold, MetallicGold, GoldShadow)
)

val RoyalButtonGradientBrush = Brush.horizontalGradient(
  colors = listOf(ChampagneGold, MetallicGold, GoldShadow)
)

val RoyalCardBorderBrush = Brush.linearGradient(
  colors = listOf(
    ChampagneGold.copy(alpha = 0.45f),
    MetallicGold.copy(alpha = 0.40f),
    ElectricViolet.copy(alpha = 0.25f),
    MetallicGold.copy(alpha = 0.40f)
  )
)

@Composable
fun StarfieldBackground(
  modifier: Modifier = Modifier,
  useAuthBackground: Boolean = false,
  content: @Composable BoxScope.() -> Unit
) {
  val bgState by AppBackgroundManager.state.collectAsStateWithLifecycle()
  val dynamicBitmap = if (useAuthBackground) bgState.bg001 else bgState.bg002

  val stars = remember {
    val rng = Random(202509)
    List(72) { index ->
      StarSpec(
        xFraction = rng.nextFloat(),
        yFraction = rng.nextFloat() * 0.92f,
        radiusDp = 0.8f + rng.nextFloat() * 1.4f,
        isGold = index % 3 == 0,
        alpha = 0.15f + rng.nextFloat() * 0.45f
      )
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      // Vertical gradient from Midnight down to RoyalPurple at about 25% from the bottom
      drawRect(
        brush = Brush.verticalGradient(
          0.0f to Midnight,
          0.72f to Midnight,
          1.0f to RoyalPurple
        ),
        size = size
      )

      // Faint ElectricViolet radial glow top-right
      val glowCenter = Offset(w * 0.88f, h * 0.10f)
      val glowRadius = maxOf(w, h) * 0.55f
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            ElectricViolet.copy(alpha = 0.22f),
            ElectricViolet.copy(alpha = 0.08f),
            Color.Transparent
          ),
          center = glowCenter,
          radius = glowRadius
        ),
        radius = glowRadius,
        center = glowCenter
      )

      // Fixed-seed stars (60-80 tiny stars, Lavender & Champagne, alpha 0.15..0.60)
      stars.forEach { star ->
        val starColor = (if (star.isGold) ChampagneGold else Lavender).copy(alpha = star.alpha)
        drawCircle(
          color = starColor,
          radius = star.radiusDp.dp.toPx(),
          center = Offset(star.xFraction * w, star.yFraction * h)
        )
      }
    }

    Crossfade(
      targetState = dynamicBitmap,
      animationSpec = tween(durationMillis = 450),
      label = "supabase_app_background"
    ) { bmp ->
      if (bmp != null) {
        Box(modifier = Modifier.fillMaxSize()) {
          Image(
            bitmap = bmp,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
          // Protective translucent Royal gradient scrim so SilverWhite & Gold UI stays legible
          val scrimColors = if (useAuthBackground) {
            listOf(
              Midnight.copy(alpha = 0.25f),
              Midnight.copy(alpha = 0.42f),
              Midnight.copy(alpha = 0.56f)
            )
          } else {
            listOf(
              Midnight.copy(alpha = 0.44f),
              Midnight.copy(alpha = 0.52f),
              RoyalPurple.copy(alpha = 0.48f)
            )
          }
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Brush.verticalGradient(scrimColors))
          )
        }
      }
    }

    content()
  }
}

@Composable
fun rememberAnimationsEnabled(): Boolean {
  val context = LocalContext.current
  return remember(context) {
    try {
      Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
      ) > 0f
    } catch (_: Throwable) {
      true
    }
  }
}

@Composable
fun MetallicGoldText(
  text: String,
  modifier: Modifier = Modifier,
  style: TextStyle = ScreenTitleStyle,
  textAlign: TextAlign? = null,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip,
  shimmerFraction: Float? = null
) {
  val brush = remember(shimmerFraction) {
    if (shimmerFraction == null) {
      RoyalGoldGradientBrush
    } else {
      val s = shimmerFraction.coerceIn(0f, 1f)
      Brush.linearGradient(
        colorStops = arrayOf(
          0.0f to ChampagneGold,
          (s * 0.6f + 0.15f).coerceIn(0.05f, 0.90f) to GoldHighlight,
          0.75f to MetallicGold,
          1.0f to GoldShadow
        )
      )
    }
  }
  Text(
    text = text,
    modifier = modifier,
    textAlign = textAlign,
    maxLines = maxLines,
    overflow = overflow,
    style = style.copy(
      brush = brush,
      shadow = Shadow(
        color = Midnight.copy(alpha = 0.9f),
        offset = Offset(0f, 3f),
        blurRadius = 4f
      )
    )
  )
}

@Composable
fun RoyalCard(
  modifier: Modifier = Modifier,
  isHero: Boolean = false,
  borderColor: Color? = null,
  backgroundColor: Color = RoyalCardSurface,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  val shape = RoundedCornerShape(CardRadius)
  val borderModifier = if (borderColor != null) {
    Modifier.border(1.dp, borderColor, shape)
  } else {
    Modifier.border(1.dp, RoyalCardBorderBrush, shape)
  }

  val cardModifier = modifier
    .fillMaxWidth()
    .clip(shape)
    .background(backgroundColor)
    .then(borderModifier)
    .let {
      if (isHero) {
        it.drawWithContent {
          drawContent()
          val inset = 8.dp.toPx()
          val leg = 14.dp.toPx()
          val stroke = 1.5.dp.toPx()
          val flourishColor = ChampagneGold.copy(alpha = 0.65f)
          // Top-left corner flourish
          drawLine(flourishColor, Offset(inset, inset), Offset(inset + leg, inset), stroke, cap = StrokeCap.Round)
          drawLine(flourishColor, Offset(inset, inset), Offset(inset, inset + leg), stroke, cap = StrokeCap.Round)
          // Top-right corner flourish
          drawLine(flourishColor, Offset(size.width - inset, inset), Offset(size.width - inset - leg, inset), stroke, cap = StrokeCap.Round)
          drawLine(flourishColor, Offset(size.width - inset, inset), Offset(size.width - inset, inset + leg), stroke, cap = StrokeCap.Round)
          // Bottom-left corner flourish
          drawLine(flourishColor, Offset(inset, size.height - inset), Offset(inset + leg, size.height - inset), stroke, cap = StrokeCap.Round)
          drawLine(flourishColor, Offset(inset, size.height - inset), Offset(inset, size.height - inset - leg), stroke, cap = StrokeCap.Round)
          // Bottom-right corner flourish
          drawLine(flourishColor, Offset(size.width - inset, size.height - inset), Offset(size.width - inset - leg, size.height - inset), stroke, cap = StrokeCap.Round)
          drawLine(flourishColor, Offset(size.width - inset, size.height - inset), Offset(size.width - inset, size.height - inset - leg), stroke, cap = StrokeCap.Round)
        }
      } else {
        it
      }
    }
    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    .padding(16.dp)

  Column(
    modifier = cardModifier,
    verticalArrangement = Arrangement.spacedBy(8.dp),
    content = content
  )
}

@Composable
fun RoyalCrownIcon(
  modifier: Modifier = Modifier,
  size: Dp = 28.dp,
  tint: Color = ChampagneGold
) {
  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val path = Path().apply {
      moveTo(w * 0.14f, h * 0.76f)
      lineTo(w * 0.10f, h * 0.34f)
      lineTo(w * 0.33f, h * 0.54f)
      lineTo(w * 0.50f, h * 0.22f)
      lineTo(w * 0.67f, h * 0.54f)
      lineTo(w * 0.90f, h * 0.34f)
      lineTo(w * 0.86f, h * 0.76f)
      close()
    }
    drawPath(
      path = path,
      brush = Brush.linearGradient(listOf(GoldHighlight, tint, MetallicGold)),
      style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    )
    drawLine(
      color = MetallicGold,
      start = Offset(w * 0.16f, h * 0.84f),
      end = Offset(w * 0.84f, h * 0.84f),
      strokeWidth = 2.dp.toPx(),
      cap = StrokeCap.Round
    )
    val jewelR = 1.8.dp.toPx()
    drawCircle(ChampagneGold, radius = jewelR, center = Offset(w * 0.10f, h * 0.30f))
    drawCircle(GoldHighlight, radius = jewelR * 1.15f, center = Offset(w * 0.50f, h * 0.18f))
    drawCircle(ChampagneGold, radius = jewelR, center = Offset(w * 0.90f, h * 0.30f))
  }
}

@Composable
fun RoyalDivider(
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .weight(1f)
        .height(1.dp)
        .background(
          Brush.horizontalGradient(
            listOf(Color.Transparent, MetallicGold.copy(alpha = 0.55f), ChampagneGold.copy(alpha = 0.8f))
          )
        )
    )
    Canvas(modifier = Modifier.padding(horizontal = 10.dp).size(8.dp)) {
      val c = this.size.width / 2f
      val p = Path().apply {
        moveTo(c, 0f)
        lineTo(this@Canvas.size.width, c)
        lineTo(c, this@Canvas.size.height)
        lineTo(0f, c)
        close()
      }
      drawPath(p, color = ChampagneGold)
    }
    Box(
      modifier = Modifier
        .weight(1f)
        .height(1.dp)
        .background(
          Brush.horizontalGradient(
            listOf(ChampagneGold.copy(alpha = 0.8f), MetallicGold.copy(alpha = 0.55f), Color.Transparent)
          )
        )
    )
  }
}

@Composable
fun RoleBadge(
  roleText: String,
  modifier: Modifier = Modifier,
  isGoldFilled: Boolean = false
) {
  val shape = RoundedCornerShape(10.dp)
  if (isGoldFilled) {
    Box(
      modifier = modifier
        .clip(shape)
        .background(RoyalButtonGradientBrush)
        .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
      Text(
        text = roleText,
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        color = RoyalPurple,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  } else {
    Box(
      modifier = modifier
        .clip(shape)
        .background(RoyalSurfaceHigh)
        .border(1.dp, MetallicGold.copy(alpha = 0.55f), shape)
        .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
      Text(
        text = roleText,
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        color = ChampagneGold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun OutlinedGoldButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  leadingIcon: ImageVector? = null,
  enabled: Boolean = true,
  isLoading: Boolean = false
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  OutlinedButton(
    onClick = onClick,
    enabled = enabled && !isLoading,
    interactionSource = interactionSource,
    modifier = modifier
      .defaultMinSize(minHeight = 52.dp)
      .then(
        if (isPressed) Modifier.shadow(12.dp, RoundedCornerShape(ButtonRadius), ambientColor = ElectricViolet, spotColor = ElectricViolet)
        else Modifier
      ),
    shape = RoundedCornerShape(ButtonRadius),
    border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(ChampagneGold, MetallicGold, GoldShadow))),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = Color.Transparent,
      contentColor = ChampagneGold
    )
  ) {
    if (isLoading) {
      CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ChampagneGold, strokeWidth = 2.dp)
    } else {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (leadingIcon != null) {
          androidx.compose.material3.Icon(
            leadingIcon,
            contentDescription = null,
            tint = ChampagneGold,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = text,
          style = ButtonTextStyle.copy(color = ChampagneGold)
        )
      }
    }
  }
}

@Composable
fun royalOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedContainerColor = RoyalSurface,
  unfocusedContainerColor = RoyalSurface,
  focusedBorderColor = ElectricViolet,
  unfocusedBorderColor = MetallicGold.copy(alpha = 0.40f),
  focusedLabelColor = ChampagneGold,
  unfocusedLabelColor = Lavender,
  focusedTextColor = SilverWhite,
  unfocusedTextColor = SilverWhite,
  focusedPlaceholderColor = Lavender.copy(alpha = 0.7f),
  unfocusedPlaceholderColor = Lavender.copy(alpha = 0.7f),
  cursorColor = ChampagneGold
)

fun violetGlowShadow(active: Boolean = true): Modifier =
  if (active) {
    Modifier.shadow(
      elevation = 10.dp,
      shape = RoundedCornerShape(ButtonRadius),
      ambientColor = ElectricViolet,
      spotColor = ElectricViolet
    )
  } else {
    Modifier
  }

fun focusTextShadow(active: Boolean = true): Shadow? =
  if (active) {
    Shadow(color = ElectricViolet.copy(alpha = 0.65f), offset = Offset.Zero, blurRadius = 10f)
  } else {
    null
  }
