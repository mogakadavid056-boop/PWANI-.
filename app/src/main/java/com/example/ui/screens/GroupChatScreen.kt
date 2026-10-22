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
import com.example.data.DiscussionGroupItem
import com.example.data.GroupMessageItem
import com.example.ui.GroupsUiState
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.ErrorBanner
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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
  val listState = rememberLazyListState()

  LaunchedEffect(uiState.messages.size) {
    if (uiState.messages.isNotEmpty()) {
      listState.animateScrollToItem(uiState.messages.size - 1)
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
      .testTag("group_chat_screen")
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkSurface)
          .border(1.dp, DarkBorder)
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.size(38.dp).testTag("chat_back_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = group.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight,
            maxLines = 1
          )
          Text(
            text = "${group.memberCount} members",
            fontSize = 11.sp,
            color = TextSecondary
          )
        }
        IconButton(
          onClick = onOpenInfo,
          modifier = Modifier.size(38.dp).testTag("chat_info_button")
        ) {
          Icon(Icons.Default.Info, contentDescription = "Group Info", tint = GoldLight)
        }
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
            CircularProgressIndicator(color = GoldPrimary)
          }
        } else if (uiState.messages.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No messages yet. Say hello!", color = TextSecondary, fontSize = 13.sp)
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
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .imePadding()
          .background(DarkSurface)
          .border(1.dp, DarkBorder)
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = { if (it.length <= 2000) inputText = it },
          placeholder = { Text("Type a message...", color = TextSecondary, fontSize = 14.sp) },
          maxLines = 4,
          modifier = Modifier.weight(1f).testTag("chat_message_input"),
          shape = RoundedCornerShape(22.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = DarkBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = DarkSurfaceVariant,
            unfocusedContainerColor = DarkSurfaceVariant
          )
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
            .size(44.dp)
            .clip(CircleShape)
            .background(if (inputText.trim().isNotBlank() && isOnline) GoldPrimary else DarkSurfaceVariant)
            .then(if (!isOnline) Modifier.alpha(0.5f) else Modifier)
            .testTag("chat_send_button")
        ) {
          if (uiState.isSendingMessage) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DarkTextOnGold, strokeWidth = 2.dp)
          } else {
            Icon(
              Icons.AutoMirrored.Filled.Send,
              contentDescription = "Send",
              tint = if (inputText.trim().isNotBlank()) DarkTextOnGold else TextSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }

    uiState.messagePendingDelete?.let {
      ConfirmActionDialog(
        title = "Delete Message",
        message = "Are you sure you want to delete this message? This action cannot be undone.",
        confirmButtonText = "Delete",
        confirmButtonColor = ErrorRed,
        isLoading = uiState.isActionLoading,
        onConfirm = onConfirmDeleteMessage,
        onDismiss = onDismissPromptDeleteMessage
      )
    }
  }
}
