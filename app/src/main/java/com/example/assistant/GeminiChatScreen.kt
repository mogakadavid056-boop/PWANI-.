package com.example.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderHighlight
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RoyalErrorContainer
import com.example.ui.theme.RoyalSurface
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SilverWhite
import com.example.ui.theme.SuccessColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GeminiChatScreen(
  uiState: GeminiChatUiState,
  userId: String?,
  isOnline: Boolean,
  onEnterChat: () -> Unit,
  onInputChanged: (String) -> Unit,
  onSendMessage: (String) -> Unit,
  onStopStreaming: () -> Unit,
  onRegenerateResponse: () -> Unit = {},
  onRetryLast: () -> Unit = onRegenerateResponse,
  onNewChat: () -> Unit,
  onDismissError: () -> Unit,
  onBack: () -> Unit
) {
  LaunchedEffect(userId) {
    onEnterChat()
  }

  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  val isNearBottom by remember {
    derivedStateOf {
      val info = listState.layoutInfo
      if (info.totalItemsCount == 0) true
      else {
        val lastVisible = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
        lastVisible.index >= info.totalItemsCount - 1 && !listState.canScrollForward
      }
    }
  }

  var autoFollow by remember { mutableStateOf(true) }
  LaunchedEffect(listState.isScrollInProgress, isNearBottom) {
    if (listState.isScrollInProgress && !isNearBottom) {
      autoFollow = false
    } else if (isNearBottom) {
      autoFollow = true
    }
  }

  val messageCount = uiState.messages.size
  val lastMessage = uiState.messages.lastOrNull()
  LaunchedEffect(messageCount) {
    if (messageCount > 0) {
      autoFollow = true
      listState.animateScrollToItem(messageCount - 1)
    }
  }
  LaunchedEffect(lastMessage?.text?.length, uiState.isStreaming) {
    if (uiState.isStreaming && autoFollow && messageCount > 0) {
      listState.scrollToItem(messageCount - 1)
    }
  }

  val lastModelIndex = remember(uiState.messages) {
    uiState.messages.indexOfLast { it.role == ChatRole.MODEL }
  }
  val hasTrailingUnansweredUserTurn = remember(uiState.messages, uiState.isStreaming) {
    !uiState.isStreaming && uiState.messages.lastOrNull()?.role == ChatRole.USER
  }
  val canRegenerateNow = uiState.canRegenerate && isOnline && uiState.isConfigured

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .imePadding()
      .navigationBarsPadding()
      .testTag("gemini_chat_screen")
  ) {
    val isTabletOrDesktop = maxWidth >= 600.dp
    val maxContentWidth = when {
      maxWidth >= 900.dp -> 840.dp
      maxWidth >= 600.dp -> 720.dp
      else -> maxWidth
    }
    val userBubbleMaxWidth = if (isTabletOrDesktop) 520.dp else 310.dp

    Column(modifier = Modifier.fillMaxSize()) {
      GeminiTopBar(
        showNewChat = uiState.messages.isNotEmpty() || uiState.isStreaming,
        isStreaming = uiState.isStreaming,
        maxContentWidth = maxContentWidth,
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
          maxContentWidth = maxContentWidth,
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
          maxContentWidth = maxContentWidth,
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
          maxContentWidth = maxContentWidth,
          onRetry = if (!uiState.isStreaming && isOnline && uiState.isConfigured &&
            (uiState.lastFailedPrompt != null || uiState.canRegenerate)
          ) {
            onRetryLast
          } else {
            null
          },
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
            isWideLayout = isTabletOrDesktop,
            modifier = Modifier
              .fillMaxSize()
              .widthIn(max = maxContentWidth),
            onStarterClick = onSendMessage
          )
        } else {
          LazyColumn(
            state = listState,
            modifier = Modifier
              .fillMaxSize()
              .widthIn(max = maxContentWidth)
              .testTag("gemini_message_list"),
            contentPadding = PaddingValues(
              horizontal = if (isTabletOrDesktop) 24.dp else 14.dp,
              vertical = 14.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            itemsIndexed(
              items = uiState.messages,
              key = { _, msg -> msg.id }
            ) { index, msg ->
              GeminiMessageBubble(
                message = msg,
                userBubbleMaxWidth = userBubbleMaxWidth,
                showRegenerate = index == lastModelIndex && !uiState.isStreaming,
                canRegenerate = canRegenerateNow,
                onRegenerate = onRegenerateResponse
              )
            }

            if (hasTrailingUnansweredUserTurn) {
              item(key = "retry_unanswered_turn") {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.Start
                ) {
                  ResponseActionPill(
                    icon = Icons.Default.Refresh,
                    label = "Regenerate response",
                    contentDesc = "Regenerate response",
                    enabled = canRegenerateNow,
                    onClick = onRegenerateResponse,
                    testTag = "gemini_regenerate_button"
                  )
                }
              }
            }
          }
        }

        // Floating controls above composer (Stop generating while streaming OR Jump to bottom)
        Column(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 10.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          AnimatedVisibility(
            visible = uiState.messages.isNotEmpty() && !isNearBottom,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
          ) {
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = RoyalSurfaceHigh.copy(alpha = 0.95f),
              border = BorderStroke(1.dp, DarkBorderHighlight),
              modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .minimumInteractiveComponentSize()
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable {
                  autoFollow = true
                  coroutineScope.launch {
                    if (uiState.messages.isNotEmpty()) {
                      listState.animateScrollToItem(uiState.messages.lastIndex)
                    }
                  }
                }
                .testTag("gemini_scroll_bottom_button")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.KeyboardArrowDown,
                  contentDescription = "Jump to bottom",
                  tint = ChampagneGold,
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = "Latest",
                  style = FieldLabelStyle.copy(fontSize = 12.5.sp, color = ChampagneGold)
                )
              }
            }
          }

          AnimatedVisibility(
            visible = uiState.isStreaming,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
          ) {
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = RoyalErrorContainer,
              border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.75f)),
              modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .minimumInteractiveComponentSize()
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(onClick = onStopStreaming)
                .testTag("gemini_stop_pill_button")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Stop,
                  contentDescription = null,
                  tint = ErrorColor,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "Stop generating",
                  style = FieldLabelStyle.copy(fontSize = 12.5.sp, color = ErrorColor)
                )
              }
            }
          }
        }
      }

      GeminiComposer(
        inputText = uiState.inputText,
        isStreaming = uiState.isStreaming,
        canSend = uiState.inputText.isNotBlank() && uiState.isConfigured,
        isWideLayout = isTabletOrDesktop,
        maxContentWidth = maxContentWidth,
        onInputChanged = onInputChanged,
        onSend = { onSendMessage(uiState.inputText) },
        onStop = onStopStreaming
      )
    }
  }
}

@Composable
private fun GeminiMessageBubble(
  message: GeminiMessage,
  userBubbleMaxWidth: Dp,
  showRegenerate: Boolean,
  canRegenerate: Boolean,
  onRegenerate: () -> Unit
) {
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
      SelectionContainer {
        Box(
          modifier = Modifier
            .widthIn(max = userBubbleMaxWidth)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
            .background(MetallicGold.copy(alpha = 0.17f))
            .border(
              1.dp,
              DarkBorderHighlight,
              RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
            )
            .padding(horizontal = 15.dp, vertical = 11.dp)
        ) {
          Text(
            text = message.text,
            style = BodyStyle.copy(fontSize = 15.5.sp, lineHeight = 22.5.sp, color = SilverWhite)
          )
        }
      }
    }
  } else {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp))
        .background(RoyalSurface.copy(alpha = 0.95f))
        .border(
          1.dp,
          if (message.isStreaming) DarkBorderHighlight else DarkBorder,
          RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp)
        )
        .padding(start = 15.dp, end = 15.dp, top = 12.dp, bottom = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
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
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = when {
              message.isStreaming && message.text.isBlank() -> "Gemini • Thinking"
              message.isStreaming -> "Gemini • Streaming"
              else -> "Gemini"
            },
            style = FieldLabelStyle.copy(fontSize = 12.5.sp, color = ChampagneGold)
          )
        }
      }

      if (message.isStreaming && message.text.isBlank()) {
        GeminiThinkingIndicator(modifier = Modifier.padding(vertical = 8.dp))
      } else {
        AssistantMarkdownText(
          markdown = message.text,
          messageKey = message.id,
          modifier = Modifier.fillMaxWidth()
        )
      }

      if (message.text.isNotBlank()) {
        HorizontalDivider(thickness = 0.75.dp, color = DarkBorder.copy(alpha = 0.65f))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ResponseActionPill(
            icon = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
            label = if (copied) "Copied" else "Copy",
            contentDesc = if (copied) "Copied reply" else "Copy reply",
            tintOverride = if (copied) SuccessColor else Lavender,
            onClick = {
              clipboard.setText(AnnotatedString(message.text))
              copied = true
            },
            testTag = "gemini_copy_button_${message.id}"
          )

          if (showRegenerate) {
            ResponseActionPill(
              icon = Icons.Default.Refresh,
              label = "Regenerate",
              contentDesc = "Regenerate response",
              enabled = canRegenerate,
              onClick = onRegenerate,
              testTag = "gemini_regenerate_button"
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ResponseActionPill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  contentDesc: String,
  enabled: Boolean = true,
  tintOverride: androidx.compose.ui.graphics.Color? = null,
  onClick: () -> Unit,
  testTag: String
) {
  val tint = when {
    !enabled -> Lavender.copy(alpha = 0.4f)
    tintOverride != null -> tintOverride
    else -> ChampagneGold
  }
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = RoyalSurfaceHigh.copy(alpha = if (enabled) 0.78f else 0.35f),
    border = BorderStroke(0.75.dp, if (enabled) DarkBorder else DarkBorder.copy(alpha = 0.35f)),
    modifier = Modifier
      .minimumInteractiveComponentSize()
      .clip(RoundedCornerShape(10.dp))
      .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
      .clickable(enabled = enabled, onClick = onClick)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDesc,
        tint = tint,
        modifier = Modifier.size(15.dp)
      )
      Text(
        text = label,
        style = FieldLabelStyle.copy(fontSize = 12.sp, lineHeight = 16.sp, color = tint)
      )
    }
  }
}
