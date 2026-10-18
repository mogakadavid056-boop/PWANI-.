package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ButtonMinHeight
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardRadius
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ClassRepCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  val shape = RoundedCornerShape(CardRadius)
  val baseModifier = modifier
    .fillMaxWidth()
    .clip(shape)
    .background(DarkCard)
    .border(1.dp, DarkBorder, shape)
    .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    .padding(16.dp)

  Column(
    modifier = baseModifier,
    verticalArrangement = Arrangement.spacedBy(8.dp),
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
  Button(
    onClick = onClick,
    enabled = enabled && !isLoading,
    modifier = modifier
      .defaultMinSize(minHeight = ButtonMinHeight),
    shape = RoundedCornerShape(ButtonRadius),
    colors = ButtonDefaults.buttonColors(
      containerColor = containerColor,
      contentColor = contentColor,
      disabledContainerColor = containerColor.copy(alpha = 0.4f),
      disabledContentColor = contentColor.copy(alpha = 0.5f)
    )
  ) {
    if (isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        color = contentColor,
        strokeWidth = 2.dp
      )
    } else {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        if (leadingIcon != null) {
          Icon(leadingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = text,
          style = ButtonTextStyle.copy(color = contentColor, fontWeight = FontWeight.Bold)
        )
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
  androidx.compose.material3.OutlinedButton(
    onClick = onClick,
    enabled = enabled && !isLoading,
    modifier = modifier.defaultMinSize(minHeight = ButtonMinHeight),
    shape = RoundedCornerShape(ButtonRadius),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = DarkSurface,
      contentColor = GoldPrimary
    )
  ) {
    if (isLoading) {
      CircularProgressIndicator(modifier = Modifier.size(20.dp), color = GoldPrimary, strokeWidth = 2.dp)
    } else {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (leadingIcon != null) {
          Icon(leadingIcon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, style = ButtonTextStyle.copy(color = GoldPrimary, fontWeight = FontWeight.Bold))
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
  ClassRepCard(modifier = modifier) {
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
          .background(DarkBorder.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = title,
        style = SectionHeadingStyle.copy(textAlign = TextAlign.Center)
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = message,
        style = CaptionStyle.copy(textAlign = TextAlign.Center),
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
      .border(1.dp, ErrorRed.copy(alpha = 0.5f), shape)
      .padding(horizontal = 14.dp, vertical = 10.dp),
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
        style = CaptionStyle.copy(color = TextPrimary, fontWeight = FontWeight.Medium)
      )
    }
    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
      Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextSecondary, modifier = Modifier.size(16.dp))
    }
  }
}
