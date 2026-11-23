package com.example.admin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.ui.theme.AdminButtonTextStyle
import com.example.admin.ui.theme.AdminCaptionStyle
import com.example.admin.ui.theme.AdminCardTitleStyle
import com.example.admin.ui.theme.AdminScreenTitleStyle
import com.example.admin.ui.theme.AdminSectionHeadingStyle
import com.example.admin.ui.theme.ChampagneGold
import com.example.admin.ui.theme.CrimsonContainer
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.ElectricViolet
import com.example.admin.ui.theme.EmeraldContainer
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldHighlight
import com.example.admin.ui.theme.GoldShadow
import com.example.admin.ui.theme.Lavender
import com.example.admin.ui.theme.MetallicGold
import com.example.admin.ui.theme.Midnight
import com.example.admin.ui.theme.MontserratFamily
import com.example.admin.ui.theme.PlayfairDisplayFamily
import com.example.admin.ui.theme.RoyalCardSurface
import com.example.admin.ui.theme.RoyalPurple
import com.example.admin.ui.theme.RoyalSurface
import com.example.admin.ui.theme.SilverWhite
import kotlin.random.Random

private data class AdminStarSpec(
  val xFraction: Float,
  val yFraction: Float,
  val radiusDp: Float,
  val isGold: Boolean,
  val alpha: Float
)

val AdminRoyalGoldBrush = Brush.linearGradient(
  colors = listOf(GoldHighlight, ChampagneGold, MetallicGold, GoldShadow)
)

val AdminButtonGradientBrush = Brush.horizontalGradient(
  colors = listOf(ChampagneGold, MetallicGold, GoldShadow)
)

val AdminCardBorderBrush = Brush.linearGradient(
  colors = listOf(
    ChampagneGold.copy(alpha = 0.45f),
    MetallicGold.copy(alpha = 0.40f),
    ElectricViolet.copy(alpha = 0.25f),
    MetallicGold.copy(alpha = 0.40f)
  )
)

@Composable
fun AdminBackdrop(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  val stars = remember {
    val rng = Random(202511)
    List(72) { index ->
      AdminStarSpec(
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

      drawRect(
        brush = Brush.verticalGradient(
          0.0f to Midnight,
          0.74f to Midnight,
          1.0f to RoyalPurple
        ),
        size = size
      )

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

      stars.forEach { star ->
        val starColor = (if (star.isGold) ChampagneGold else Lavender).copy(alpha = star.alpha)
        drawCircle(
          color = starColor,
          radius = star.radiusDp.dp.toPx(),
          center = Offset(star.xFraction * w, star.yFraction * h)
        )
      }
    }

    content()
  }
}

@Composable
fun AdminMetallicGoldText(
  text: String,
  modifier: Modifier = Modifier,
  style: TextStyle = AdminScreenTitleStyle,
  textAlign: TextAlign? = null,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip
) {
  Text(
    text = text,
    modifier = modifier,
    textAlign = textAlign,
    maxLines = maxLines,
    overflow = overflow,
    style = style.copy(
      brush = AdminRoyalGoldBrush,
      shadow = Shadow(
        color = Midnight.copy(alpha = 0.9f),
        offset = Offset(0f, 3f),
        blurRadius = 4f
      )
    )
  )
}

@Composable
fun AdminCrownIcon(
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
fun AdminRoyalDivider(modifier: Modifier = Modifier) {
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
fun AdminGoldButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  leadingIcon: ImageVector? = null,
  enabled: Boolean = true,
  isLoading: Boolean = false
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val shape = RoundedCornerShape(14.dp)

  Button(
    onClick = onClick,
    enabled = enabled && !isLoading,
    interactionSource = interactionSource,
    contentPadding = PaddingValues(0.dp),
    shape = shape,
    modifier = modifier
      .defaultMinSize(minHeight = 52.dp)
      .then(if (isPressed && enabled) Modifier.shadow(14.dp, shape, ambientColor = ElectricViolet, spotColor = ElectricViolet) else Modifier),
    colors = ButtonDefaults.buttonColors(
      containerColor = Color.Transparent,
      contentColor = Midnight,
      disabledContainerColor = GoldShadow.copy(alpha = 0.35f),
      disabledContentColor = Midnight.copy(alpha = 0.6f)
    )
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 52.dp)
        .background(
          if (enabled && !isLoading) AdminButtonGradientBrush
          else Brush.horizontalGradient(listOf(GoldShadow.copy(alpha = 0.35f), GoldShadow.copy(alpha = 0.35f)))
        )
        .padding(horizontal = 16.dp),
      contentAlignment = Alignment.Center
    ) {
      if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = RoyalPurple, strokeWidth = 2.dp)
      } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
          if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = Midnight, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
          }
          Text(text = text, style = AdminButtonTextStyle.copy(color = Midnight))
        }
      }
    }
  }
}

@Composable
fun royalAdminTextFieldColors() = OutlinedTextFieldDefaults.colors(
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

@Composable
fun AdminGlassCard(
  modifier: Modifier = Modifier,
  borderColor: Color? = null,
  isHero: Boolean = false,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  val shape = RoundedCornerShape(18.dp)
  val borderMod = if (borderColor != null) {
    Modifier.border(1.dp, borderColor, shape)
  } else {
    Modifier.border(1.dp, AdminCardBorderBrush, shape)
  }

  val base = modifier
    .fillMaxWidth()
    .clip(shape)
    .background(RoyalCardSurface)
    .then(borderMod)
    .let {
      if (isHero) {
        it.drawWithContent {
          drawContent()
          val inset = 8.dp.toPx()
          val leg = 14.dp.toPx()
          val stroke = 1.5.dp.toPx()
          val flourishColor = ChampagneGold.copy(alpha = 0.65f)
          drawLine(flourishColor, Offset(inset, inset), Offset(inset + leg, inset), stroke, cap = StrokeCap.Round)
          drawLine(flourishColor, Offset(inset, inset), Offset(inset, inset + leg), stroke, cap = StrokeCap.Round)
          drawLine(flourishColor, Offset(size.width - inset, inset), Offset(size.width - inset - leg, inset), stroke, cap = StrokeCap.Round)
          drawLine(flourishColor, Offset(size.width - inset, inset), Offset(size.width - inset, inset + leg), stroke, cap = StrokeCap.Round)
          drawLine(flourishColor, Offset(inset, size.height - inset), Offset(inset + leg, size.height - inset), stroke, cap = StrokeCap.Round)
          drawLine(flourishColor, Offset(inset, size.height - inset), Offset(inset, size.height - inset - leg), stroke, cap = StrokeCap.Round)
          drawLine(flourishColor, Offset(size.width - inset, size.height - inset), Offset(size.width - inset - leg, size.height - inset), stroke, cap = StrokeCap.Round)
          drawLine(flourishColor, Offset(size.width - inset, size.height - inset), Offset(size.width - inset, size.height - inset - leg), stroke, cap = StrokeCap.Round)
        }
      } else {
        it
      }
    }
    .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    .padding(16.dp)
  Column(modifier = base, content = content)
}

@Composable
fun KpiBentoCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null
) {
  AdminGlassCard(
    modifier = modifier,
    borderColor = accentColor.copy(alpha = 0.45f),
    onClick = onClick
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(accentColor.copy(alpha = 0.16f))
          .border(1.dp, accentColor.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(20.dp))
      }
      Text(
        text = value,
        fontFamily = PlayfairDisplayFamily,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = SilverWhite
      )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Text(text = title, style = AdminCardTitleStyle.copy(color = ChampagneGold))
    Text(text = subtitle, style = AdminCaptionStyle.copy(color = Lavender), maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
fun AdminStatusPill(
  text: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(color.copy(alpha = 0.16f))
      .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
      .padding(horizontal = 10.dp, vertical = 4.dp)
  ) {
    Text(
      text = text,
      fontFamily = MontserratFamily,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      color = color,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
fun AdminSearchField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  tag: String = "admin_search",
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    interactionSource = interactionSource,
    modifier = modifier
      .fillMaxWidth()
      .then(if (isFocused) Modifier.shadow(10.dp, RoundedCornerShape(14.dp), ambientColor = ElectricViolet, spotColor = ElectricViolet) else Modifier)
      .testTag(tag),
    placeholder = { Text(placeholder, style = AdminCaptionStyle.copy(color = Lavender.copy(alpha = 0.7f))) },
    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = if (isFocused) ChampagneGold else MetallicGold, modifier = Modifier.size(18.dp)) },
    trailingIcon = {
      if (value.isNotEmpty()) {
        IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(48.dp)) {
          Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Lavender, modifier = Modifier.size(18.dp))
        }
      }
    },
    singleLine = true,
    shape = RoundedCornerShape(14.dp),
    colors = royalAdminTextFieldColors()
  )
}

@Composable
fun AdminFeedbackBanner(
  message: String,
  isError: Boolean,
  onDismiss: () -> Unit
) {
  val bg = if (isError) CrimsonContainer else EmeraldContainer
  val fg = if (isError) CrimsonError else EmeraldSuccess
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(bg)
      .border(1.dp, fg.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
      .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = message, style = AdminCaptionStyle.copy(color = SilverWhite), modifier = Modifier.weight(1f))
    Spacer(modifier = Modifier.width(6.dp))
    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
      Icon(Icons.Default.Clear, contentDescription = "Dismiss", tint = Lavender, modifier = Modifier.size(18.dp))
    }
  }
}

@Composable
fun AdminEmptyState(
  icon: ImageVector,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier
) {
  AdminGlassCard(modifier = modifier) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(icon, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(40.dp))
      Spacer(modifier = Modifier.height(10.dp))
      Text(text = title, style = AdminSectionHeadingStyle.copy(textAlign = TextAlign.Center))
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = subtitle, style = AdminCaptionStyle.copy(color = Lavender, textAlign = TextAlign.Center))
    }
  }
}
