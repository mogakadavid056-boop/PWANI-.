package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ButtonMinHeight
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardRadius
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldShadow
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.SilverWhite

@Composable
fun ClassRepCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  RoyalCard(
    modifier = modifier,
    onClick = onClick,
    content = content
  )
}

@Composable
fun PrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  leadingIcon: ImageVector? = null,
  enabled: Boolean = true,
  isLoading: Boolean = false,
  containerColor: Color = GoldPrimary,
  contentColor: Color = DarkTextOnGold
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val shape = RoundedCornerShape(ButtonRadius)
  val isDefaultGold = containerColor == GoldPrimary || containerColor == MetallicGold

  Button(
    onClick = onClick,
    enabled = enabled && !isLoading,
    interactionSource = interactionSource,
    contentPadding = if (isDefaultGold) PaddingValues(0.dp) else ButtonDefaults.ContentPadding,
    modifier = modifier
      .defaultMinSize(minHeight = ButtonMinHeight)
      .then(
        if (isPressed && enabled) Modifier.shadow(12.dp, shape, ambientColor = ElectricViolet, spotColor = ElectricViolet)
        else Modifier
      ),
    shape = shape,
    colors = ButtonDefaults.buttonColors(
      containerColor = if (isDefaultGold) Color.Transparent else containerColor,
      contentColor = contentColor,
      disabledContainerColor = if (isDefaultGold) Color.Transparent else containerColor.copy(alpha = 0.4f),
      disabledContentColor = contentColor.copy(alpha = 0.5f)
    )
  ) {
    val innerModifier = if (isDefaultGold) {
      Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = ButtonMinHeight)
        .background(
          if (enabled && !isLoading) {
            RoyalButtonGradientBrush
          } else {
            Brush.horizontalGradient(listOf(GoldShadow.copy(alpha = 0.35f), GoldShadow.copy(alpha = 0.35f)))
          }
        )
        .padding(horizontal = 16.dp, vertical = 10.dp)
    } else {
      Modifier
    }

    Box(modifier = innerModifier, contentAlignment = Alignment.Center) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          color = if (isDefaultGold) RoyalPurple else contentColor,
          strokeWidth = 2.dp
        )
      } else {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          if (leadingIcon != null) {
            Icon(
              leadingIcon,
              contentDescription = null,
              tint = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
          }
          Text(
            text = text,
            style = ButtonTextStyle.copy(
              color = if (enabled) contentColor else contentColor.copy(alpha = 0.6f)
            )
          )
        }
      }
    }
  }
}

@Composable
fun ClassRepOutlinedButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  leadingIcon: ImageVector? = null,
  enabled: Boolean = true,
  isLoading: Boolean = false
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val shape = RoundedCornerShape(ButtonRadius)

  OutlinedButton(
    onClick = onClick,
    enabled = enabled && !isLoading,
    interactionSource = interactionSource,
    modifier = modifier
      .defaultMinSize(minHeight = ButtonMinHeight)
      .then(
        if (isPressed && enabled) Modifier.shadow(12.dp, shape, ambientColor = ElectricViolet, spotColor = ElectricViolet)
        else Modifier
      ),
    shape = shape,
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
          Icon(leadingIcon, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, style = ButtonTextStyle.copy(color = ChampagneGold))
      }
    }
  }
}

@Composable
fun EmptyState(
  icon: ImageVector,
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null
) {
  RoyalCard(modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(RoyalSurfaceHigh)
          .border(1.dp, DarkBorder, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(28.dp))
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = title,
        style = SectionHeadingStyle.copy(textAlign = TextAlign.Center)
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = message,
        style = CaptionStyle.copy(color = Lavender, textAlign = TextAlign.Center),
        modifier = Modifier.padding(horizontal = 16.dp)
      )
      if (actionLabel != null && onActionClick != null) {
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryButton(
          text = actionLabel,
          onClick = onActionClick,
          modifier = Modifier.fillMaxWidth(0.7f)
        )
      }
    }
  }
}

@Composable
fun LoadingSkeleton(
  modifier: Modifier = Modifier,
  height: Int = 80
) {
  val transition = rememberInfiniteTransition(label = "shimmer")
  val alpha by transition.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.75f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "shimmer_alpha"
  )

  val shape = RoundedCornerShape(CardRadius)
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height.dp)
      .clip(shape)
      .background(DarkCard.copy(alpha = alpha))
      .border(1.dp, DarkBorder, shape)
  )
}

fun toFriendlyErrorMessage(raw: String): String {
  val lower = raw.lowercase()
  return when {
    lower.contains("jwt") || lower.contains("401") || lower.contains("unauthorized") ->
      "Session expired. Please log in again."
    lower.contains("vote first to see results") ->
      "Vote first to see results."
    lower.contains("already voted") ->
      "You have already cast your vote."
    lower.contains("network") || lower.contains("timeout") || lower.contains("connect") || lower.contains("socket") ->
      "Unable to connect. Please check your internet connection."
    lower.contains("duplicate") || lower.contains("unique") ->
      "This item already exists."
    lower.contains("not found") || lower.contains("404") ->
      "The requested item was not found."
    lower.contains("permission") || lower.contains("403") || lower.contains("forbidden") ->
      "You do not have permission for this action."
    raw.length > 80 -> "Something went wrong. Please try again."
    else -> raw
  }
}

@Composable
fun FriendlyErrorSnackbar(
  errorMessage: String,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val friendly = toFriendlyErrorMessage(errorMessage)
  val shape = RoundedCornerShape(ButtonRadius)

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(ErrorContainer)
      .border(1.dp, ErrorRed.copy(alpha = 0.55f), shape)
      .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f)
    ) {
      Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = friendly,
        style = CaptionStyle.copy(color = SilverWhite, fontWeight = FontWeight.Medium)
      )
    }
    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
      Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Lavender, modifier = Modifier.size(18.dp))
    }
  }
}
