package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.focus.onFocusChanged
import com.example.data.DiscussionGroupItem
import com.example.data.GroupMessageItem
import com.example.ui.GroupsUiState
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.ErrorBanner
import com.example.ui.components.royalOutlinedTextFieldColors
import com.example.ui.components.violetGlowShadow
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardTitleStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.RoyalNavSurface
import com.example.ui.theme.RoyalSurfaceHigh

@Composable
fun GroupChatScreen(
  group: DiscussionGroupItem,
  currentUserId: String,
  isStaff: Boolean,
  uiState: GroupsUiState,
  onSendMessage: (String) -> Unit,
  onPromptDeleteMessage: (GroupMessageItem) -> Unit,
  onDismissPromptDeleteMessage: () -> Unit,
  onConfirmDeleteMessage: () -> Unit,
  onDismissError: () -> Unit,
  onOpenInfo: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  var inputText by remember { mutableStateOf("") }
  var isInputFocused by remember { mutableStateOf(false) }
  val listState = rememberLazyListState()

  LaunchedEffect(uiState.messages.size) {
    if (uiState.messages.isNotEmpty()) {
      listState.animateScrollToItem(uiState.messages.size - 1)
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .testTag("group_chat_screen")
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top bar
      Column(modifier = Modifier.fillMaxWidth().background(RoyalNavSurface)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onBack,
            modifier = Modifier.size(48.dp).testTag("chat_back_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
          }
          Spacer(modifier = Modifier.width(4.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = group.name,
              style = CardTitleStyle.copy(color = ChampagneGold),
              maxLines = 1
            )
            Text(
              text = "${group.memberCount} members",
              style = CaptionStyle
            )
          }
          IconButton(
            onClick = onOpenInfo,
            modifier = Modifier.size(48.dp).testTag("chat_info_button")
          ) {
            Icon(Icons.Default.Info, contentDescription = "Group Info", tint = ChampagneGold)
          }
        }
        HorizontalDivider(color = MetallicGold.copy(alpha = 0.25f), thickness = 1.dp)
      }

      if (!uiState.errorMessage.isNullOrBlank()) {
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
          ErrorBanner(message = uiState.errorMessage, onDismiss = onDismissError)
        }
      }

      // Messages list
      Box(modifier = Modifier.weight(1f)) {
        if (uiState.isLoadingMessages && uiState.messages.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MetallicGold)
          }
        } else if (uiState.messages.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No messages yet. Say hello!", style = BodyStyle.copy(color = com.example.ui.theme.Lavender))
          }
        } else {
          LazyColumn(
            state = listState,
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 14.dp, vertical = 8.dp)
          ) {
            items(uiState.messages, key = { it.id }) { message ->
              val isOwn = message.senderId == currentUserId
              val canDelete = isOwn || isStaff
              ChatMessageBubble(
                message = message,
                isOwn = isOwn,
                canDelete = canDelete,
                onLongClick = { onPromptDeleteMessage(message) }
              )
            }
          }
        }
      }

      // Input row
      Column(modifier = Modifier.fillMaxWidth().imePadding().background(RoyalNavSurface)) {
        HorizontalDivider(color = MetallicGold.copy(alpha = 0.25f), thickness = 1.dp)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = inputText,
            onValueChange = { if (it.length <= 2000) inputText = it },
            placeholder = { Text("Type a message...", style = CaptionStyle) },
            maxLines = 4,
            modifier = Modifier
              .weight(1f)
              .then(violetGlowShadow(isInputFocused))
              .onFocusChanged { isInputFocused = it.isFocused }
              .testTag("chat_message_input"),
            shape = RoundedCornerShape(22.dp),
            colors = royalOutlinedTextFieldColors()
          )

          Spacer(modifier = Modifier.width(8.dp))

          val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
          val context = androidx.compose.ui.platform.LocalContext.current
          IconButton(
            onClick = {
              if (!isOnline) {
                com.example.ui.components.OfflineToastHelper.show(context)
                return@IconButton
              }
              val textToSend = inputText.trim()
              if (textToSend.isNotBlank()) {
                inputText = ""
                onSendMessage(textToSend)
              }
            },
            enabled = if (!isOnline) true else (inputText.trim().isNotBlank() && !uiState.isSendingMessage),
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(if (inputText.trim().isNotBlank() && isOnline) MetallicGold else RoyalSurfaceHigh)
              .then(if (!isOnline) Modifier.alpha(0.5f) else Modifier)
              .testTag("chat_send_button")
          ) {
            if (uiState.isSendingMessage) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Midnight, strokeWidth = 2.dp)
            } else {
              Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (inputText.trim().isNotBlank()) Midnight else com.example.ui.theme.Lavender,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }

    uiState.messagePendingDelete?.let {
      ConfirmActionDialog(
        title = "Delete Message",
        message = "Are you sure you want to delete this message? This action cannot be undone.",
        confirmButtonText = "Delete",
        confirmButtonColor = ErrorColor,
        isLoading = uiState.isActionLoading,
        onConfirm = onConfirmDeleteMessage,
        onDismiss = onDismissPromptDeleteMessage
      )
    }
  }
}
