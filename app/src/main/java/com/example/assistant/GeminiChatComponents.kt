package com.example.assistant

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderHighlight
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.RoyalErrorContainer
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.RoyalSurface
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.ScreenTitleStyle
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.SilverWhite

@Composable
internal fun GeminiTopBar(
  showNewChat: Boolean,
  isStreaming: Boolean,
  maxContentWidth: Dp,
  onBack: () -> Unit,
  onNewChat: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(Midnight.copy(alpha = 0.90f))
      .border(width = 0.5.dp, color = DarkBorder),
    contentAlignment = Alignment.Center
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = maxContentWidth)
        .padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(48.dp)
          .pointerHoverIcon(PointerIcon.Hand)
          .testTag("gemini_back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = ChampagneGold
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(RoyalPurple.copy(alpha = 0.72f))
          .border(1.dp, DarkBorderHighlight, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = ChampagneGold,
          modifier = Modifier.size(18.dp)
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Gemini AI",
          style = ScreenTitleStyle.copy(fontSize = 20.sp, lineHeight = 24.sp),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = if (isStreaming) "Generating response..." else "General assistant for students & lecturers",
          style = CaptionStyle.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = if (isStreaming) ChampagneGold else Lavender
          ),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      if (showNewChat) {
        TextButton(
          onClick = onNewChat,
          modifier = Modifier
            .height(48.dp)
            .pointerHoverIcon(PointerIcon.Hand)
            .testTag("gemini_new_chat_button")
        ) {
          Icon(
            imageVector = Icons.Default.AddComment,
            contentDescription = null,
            tint = ChampagneGold,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("New", style = FieldLabelStyle.copy(color = ChampagneGold))
        }
      }
    }
  }
}

@Composable
internal fun StatusNoticeBanner(
  icon: ImageVector,
  text: String,
  borderColor: Color,
  backgroundColor: Color,
  contentColor: Color,
  maxContentWidth: Dp,
  onRetry: (() -> Unit)? = null,
  onDismiss: (() -> Unit)? = null,
  testTag: String
) {
  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = maxContentWidth)
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(backgroundColor)
        .border(1.dp, borderColor, RoundedCornerShape(14.dp))
        .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
        .testTag(testTag),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
      Text(
        text = text,
        style = CaptionStyle.copy(fontSize = 13.sp, lineHeight = 18.sp, color = contentColor),
        modifier = Modifier.weight(1f)
      )
      if (onRetry != null) {
        TextButton(
          onClick = onRetry,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .pointerHoverIcon(PointerIcon.Hand)
            .testTag("gemini_retry_button")
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            tint = ChampagneGold,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Retry", style = FieldLabelStyle.copy(fontSize = 12.5.sp, color = ChampagneGold))
        }
      }
      if (onDismiss != null) {
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .size(48.dp)
            .pointerHoverIcon(PointerIcon.Hand)
            .testTag("gemini_dismiss_error_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Dismiss error",
            tint = contentColor,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GeminiWelcomeState(
  isWideLayout: Boolean,
  modifier: Modifier = Modifier,
  onStarterClick: (String) -> Unit
) {
  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(horizontal = if (isWideLayout) 28.dp else 18.dp, vertical = 28.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = RoyalSurface.copy(alpha = 0.92f),
      border = BorderStroke(1.dp, DarkBorderHighlight),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(if (isWideLayout) 28.dp else 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(RoyalSurfaceHigh)
            .border(1.5.dp, MetallicGold, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = ChampagneGold,
            modifier = Modifier.size(28.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Ask Gemini Anything",
          style = SectionHeadingStyle.copy(color = ChampagneGold)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Step-by-step explanations, equations, tables, code help, lecture summaries, or study practice — ready for students and lecturers.",
          style = CaptionStyle.copy(color = SilverWhite.copy(alpha = 0.88f), lineHeight = 20.sp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          GeminiStarterPrompts.items.forEachIndexed { idx, starter ->
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = RoyalSurfaceHigh,
              border = BorderStroke(1.dp, DarkBorder),
              modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .minimumInteractiveComponentSize()
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable { onStarterClick(starter.prompt) }
                .testTag("gemini_starter_chip_$idx")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = ChampagneGold,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = starter.label,
                  style = FieldLabelStyle.copy(fontSize = 13.5.sp, color = ChampagneGold)
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun GeminiThinkingIndicator(modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "gemini_thinking")
  val a1 by transition.animateFloat(
    initialValue = 0.25f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
    label = "dot1"
  )
  val a2 by transition.animateFloat(
    initialValue = 0.25f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(420, delayMillis = 140), RepeatMode.Reverse),
    label = "dot2"
  )
  val a3 by transition.animateFloat(
    initialValue = 0.25f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(420, delayMillis = 280), RepeatMode.Reverse),
    label = "dot3"
  )

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
      listOf(a1, a2, a3).forEach { alphaVal ->
        Box(
          modifier = Modifier
            .size(8.dp)
            .alpha(alphaVal)
            .clip(CircleShape)
            .background(ChampagneGold)
        )
      }
    }
    Text(
      text = "Thinking...",
      style = CaptionStyle.copy(fontSize = 13.sp, color = Lavender)
    )
  }
}

@Composable
internal fun GeminiComposer(
  inputText: String,
  isStreaming: Boolean,
  canSend: Boolean,
  isWideLayout: Boolean,
  maxContentWidth: Dp,
  onInputChanged: (String) -> Unit,
  onSend: () -> Unit,
  onStop: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(Midnight.copy(alpha = 0.94f))
      .border(width = 0.5.dp, color = DarkBorder),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = maxContentWidth)
        .padding(horizontal = if (isWideLayout) 20.dp else 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = onInputChanged,
          placeholder = {
            Text(
              text = if (isWideLayout) {
                "Ask Gemini anything... (Enter to send, Shift+Enter for new line)"
              } else {
                "Ask Gemini anything..."
              },
              style = BodyStyle.copy(fontSize = 15.sp, color = Lavender.copy(alpha = 0.65f)),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          },
          textStyle = BodyStyle.copy(fontSize = 15.sp, lineHeight = 22.sp, color = SilverWhite),
          maxLines = if (isWideLayout) 7 else 5,
          shape = RoundedCornerShape(22.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = RoyalSurfaceHigh,
            unfocusedContainerColor = RoyalSurface,
            focusedBorderColor = ChampagneGold,
            unfocusedBorderColor = DarkBorder,
            cursorColor = ChampagneGold
          ),
          modifier = Modifier
            .weight(1f)
            .onPreviewKeyEvent { keyEvent ->
              val isEnter = keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter
              val plainEnter = isEnter &&
                !keyEvent.isShiftPressed &&
                !keyEvent.isCtrlPressed &&
                !keyEvent.isAltPressed &&
                !keyEvent.isMetaPressed
              if (plainEnter && keyEvent.type == KeyEventType.KeyDown) {
                if (canSend && !isStreaming) {
                  onSend()
                }
                true
              } else {
                false
              }
            }
            .testTag("gemini_input_field")
        )

        if (isStreaming) {
          IconButton(
            onClick = onStop,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(RoyalErrorContainer)
              .border(1.dp, ErrorColor.copy(alpha = 0.75f), CircleShape)
              .pointerHoverIcon(PointerIcon.Hand)
              .testTag("gemini_stop_button")
          ) {
            Icon(
              imageVector = Icons.Default.Stop,
              contentDescription = "Stop generating",
              tint = ErrorColor,
              modifier = Modifier.size(22.dp)
            )
          }
        } else {
          IconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(if (canSend) MetallicGold else RoyalSurfaceHigh)
              .border(
                1.dp,
                if (canSend) ChampagneGold else DarkBorder,
                CircleShape
              )
              .pointerHoverIcon(if (canSend) PointerIcon.Hand else PointerIcon.Default)
              .testTag("gemini_send_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = "Send message",
              tint = if (canSend) Midnight else Lavender.copy(alpha = 0.45f),
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }
}
