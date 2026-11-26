package com.example.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.SuccessColor
import kotlinx.coroutines.delay

@Composable
fun GeminiChatScreen(
  uiState: GeminiChatUiState,
  userId: String?,
  isOnline: Boolean,
  onEnterChat: () -> Unit,
  onInputChanged: (String) -> Unit,
  onSendMessage: (String) -> Unit,
  onStopStreaming: () -> Unit,
  onNewChat: () -> Unit,
  onDismissError: () -> Unit,
  onBack: () -> Unit
) {
  LaunchedEffect(userId) {
    onEnterChat()
  }

  val listState = rememberLazyListState()
  val lastMessage = uiState.messages.lastOrNull()
  LaunchedEffect(uiState.messages.size, lastMessage?.text?.length) {
    if (uiState.messages.isNotEmpty()) {
      listState.animateScrollToItem(uiState.messages.lastIndex)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .imePadding()
      .navigationBarsPadding()
      .testTag("gemini_chat_screen")
  ) {
    GeminiTopBar(
      showNewChat = uiState.messages.isNotEmpty() || uiState.isStreaming,
      onBack = onBack,
      onNewChat = onNewChat
    )

    if (!isOnline) {
      StatusNoticeBanner(
        icon = Icons.Default.WifiOff,
        text = "You're offline. Connect to the internet to chat with Gemini.",
        borderColor = DarkBorder,
        backgroundColor = RoyalSurfaceHigh,
        contentColor = Lavender,
        testTag = "gemini_offline_banner"
      )
    }

    if (!uiState.isConfigured) {
      StatusNoticeBanner(
        icon = Icons.Default.Key,
        text = GeminiConfig.MISSING_API_KEY_MESSAGE,
        borderColor = MetallicGold.copy(alpha = 0.65f),
        backgroundColor = RoyalSurfaceHigh,
        contentColor = ChampagneGold,
        testTag = "gemini_missing_key_banner"
      )
    }

    AnimatedVisibility(
      visible = !uiState.errorMessage.isNullOrBlank(),
      enter = fadeIn(tween(180)),
      exit = fadeOut(tween(150))
    ) {
      StatusNoticeBanner(
        icon = Icons.Default.ErrorOutline,
        text = uiState.errorMessage.orEmpty(),
        borderColor = ErrorColor.copy(alpha = 0.7f),
        backgroundColor = RoyalErrorContainer,
        contentColor = ErrorColor,
        onDismiss = onDismissError,
        testTag = "gemini_error_banner"
      )
    }

    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentAlignment = Alignment.TopCenter
    ) {
      if (uiState.messages.isEmpty()) {
        GeminiWelcomeState(
          modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
          onStarterClick = onSendMessage
        )
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .testTag("gemini_message_list"),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(uiState.messages, key = { it.id }) { msg ->
            GeminiMessageBubble(message = msg)
          }
        }
      }
    }

    GeminiComposer(
      inputText = uiState.inputText,
      isStreaming = uiState.isStreaming,
      canSend = uiState.inputText.isNotBlank() && isOnline && uiState.isConfigured,
      onInputChanged = onInputChanged,
      onSend = { onSendMessage(uiState.inputText) },
      onStop = onStopStreaming
    )
  }
}

@Composable
private fun GeminiTopBar(
  showNewChat: Boolean,
  onBack: () -> Unit,
  onNewChat: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Midnight.copy(alpha = 0.88f))
      .border(width = 0.5.dp, color = DarkBorder)
      .padding(horizontal = 8.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    IconButton(
      onClick = onBack,
      modifier = Modifier
        .size(48.dp)
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
        .background(RoyalPurple.copy(alpha = 0.7f))
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
        text = "General assistant for students & lecturers",
        style = CaptionStyle.copy(fontSize = 12.sp, lineHeight = 16.sp, color = Lavender),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    if (showNewChat) {
      TextButton(
        onClick = onNewChat,
        modifier = Modifier
          .height(48.dp)
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

@Composable
private fun StatusNoticeBanner(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  text: String,
  borderColor: Color,
  backgroundColor: Color,
  contentColor: Color,
  onDismiss: (() -> Unit)? = null,
  testTag: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(backgroundColor)
      .border(1.dp, borderColor, RoundedCornerShape(14.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
    Text(
      text = text,
      style = CaptionStyle.copy(fontSize = 13.sp, lineHeight = 18.sp, color = contentColor),
      modifier = Modifier.weight(1f)
    )
    if (onDismiss != null) {
      IconButton(
        onClick = onDismiss,
        modifier = Modifier.size(48.dp)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeminiWelcomeState(
  modifier: Modifier = Modifier,
  onStarterClick: (String) -> Unit
) {
  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 28.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = RoyalSurface.copy(alpha = 0.92f),
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorderHighlight),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
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
          text = "Study explanations, note summaries, writing help, quiz practice, or lecture planning — ready for students and lecturers.",
          style = CaptionStyle.copy(color = SilverWhite.copy(alpha = 0.85f), lineHeight = 20.sp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          GeminiStarterPrompts.items.forEachIndexed { idx, starter ->
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = RoyalSurfaceHigh,
              border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
              modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable { onStarterClick(starter.prompt) }
                .testTag("gemini_starter_chip_$idx")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = ChampagneGold,
                  modifier = Modifier.size(15.dp)
                )
                Text(
                  text = starter.label,
                  style = FieldLabelStyle.copy(fontSize = 13.sp, color = ChampagneGold)
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
private fun GeminiMessageBubble(message: GeminiMessage) {
  val clipboard = LocalClipboardManager.current
  var copied by remember(message.id) { mutableStateOf(false) }

  LaunchedEffect(copied) {
    if (copied) {
      delay(1600)
      copied = false
    }
  }

  if (message.role == ChatRole.USER) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End
    ) {
      Box(
        modifier = Modifier
          .widthIn(max = 320.dp)
          .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
          .background(MetallicGold.copy(alpha = 0.16f))
          .border(
            1.dp,
            DarkBorderHighlight,
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
          )
          .padding(horizontal = 14.dp, vertical = 11.dp)
      ) {
        Text(
          text = message.text,
          style = BodyStyle.copy(fontSize = 15.sp, lineHeight = 22.sp, color = SilverWhite)
        )
      }
    }
  } else {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp))
        .background(RoyalSurface.copy(alpha = 0.94f))
        .border(
          1.dp,
          DarkBorder,
          RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp)
        )
        .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = ChampagneGold,
            modifier = Modifier.size(15.dp)
          )
          Text(
            text = if (message.isStreaming) "Gemini • Streaming..." else "Gemini",
            style = FieldLabelStyle.copy(fontSize = 12.sp, color = ChampagneGold)
          )
        }

        if (!message.isStreaming && message.text.isNotBlank()) {
          IconButton(
            onClick = {
              clipboard.setText(AnnotatedString(message.text))
              copied = true
            },
            modifier = Modifier
              .size(48.dp)
              .testTag("gemini_copy_button_${message.id}")
          ) {
            Icon(
              imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
              contentDescription = if (copied) "Copied reply" else "Copy reply",
              tint = if (copied) SuccessColor else Lavender,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      if (message.isStreaming && message.text.isBlank()) {
        StreamingDotsIndicator(modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp))
      } else {
        AssistantMarkdownText(
          markdown = message.text,
          modifier = Modifier
            .fillMaxWidth()
            .padding(end = 6.dp, bottom = 4.dp)
        )
      }
    }
  }
}

@Composable
private fun StreamingDotsIndicator(modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "gemini_typing")
  val a1 by transition.animateFloat(
    initialValue = 0.25f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
    label = "dot1"
  )
  val a2 by transition.animateFloat(
    initialValue = 0.25f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(450, delayMillis = 150), RepeatMode.Reverse),
    label = "dot2"
  )
  val a3 by transition.animateFloat(
    initialValue = 0.25f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(450, delayMillis = 300), RepeatMode.Reverse),
    label = "dot3"
  )

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
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
}

@Composable
private fun GeminiComposer(
  inputText: String,
  isStreaming: Boolean,
  canSend: Boolean,
  onInputChanged: (String) -> Unit,
  onSend: () -> Unit,
  onStop: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Midnight.copy(alpha = 0.92f))
      .border(width = 0.5.dp, color = DarkBorder)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    OutlinedTextField(
      value = inputText,
      onValueChange = onInputChanged,
      placeholder = {
        Text(
          text = "Ask Gemini anything...",
          style = BodyStyle.copy(fontSize = 15.sp, color = Lavender.copy(alpha = 0.65f))
        )
      },
      textStyle = BodyStyle.copy(fontSize = 15.sp, lineHeight = 21.sp, color = SilverWhite),
      maxLines = 5,
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
        .testTag("gemini_input_field")
    )

    if (isStreaming) {
      IconButton(
        onClick = onStop,
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(RoyalErrorContainer)
          .border(1.dp, ErrorColor.copy(alpha = 0.7f), CircleShape)
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
