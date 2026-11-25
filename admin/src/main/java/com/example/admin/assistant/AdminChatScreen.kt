package com.example.admin.assistant

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.AdminSession
import com.example.admin.ui.components.AdminFeedbackBanner
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminGoldButton
import com.example.admin.ui.components.AdminMetallicGoldText
import com.example.admin.ui.components.royalAdminTextFieldColors
import com.example.admin.ui.theme.*

@Composable
fun AdminAssistantSparkleButton(
  isOnline: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (!isOnline) return
  IconButton(
    onClick = onClick,
    modifier = modifier.size(48.dp).testTag("admin_assistant_button")
  ) {
    Icon(Icons.Default.AutoAwesome, contentDescription = "Assistant", tint = ChampagneGold)
  }
}

@Composable
fun AdminChatScreen(
  uiState: AdminAssistantUiState,
  session: AdminSession?,
  isOnline: Boolean,
  activeClassId: String?,
  screenContextName: String,
  onEnterAssistant: () -> Unit,
  onAcceptTerms: () -> Unit,
  onInputChanged: (String) -> Unit,
  onNewChat: () -> Unit,
  onSendMessage: (String, AdminAssistantContext) -> Unit,
  onConfirmAction: (messageId: String, actionId: String) -> Unit,
  onSaveActionEdit: (messageId: String, actionId: String, args: Map<String, Any?>) -> Unit,
  onCancelAction: (messageId: String, actionId: String) -> Unit,
  onDismissError: () -> Unit,
  onBack: () -> Unit
) {
  LaunchedEffect(session?.userId) { onEnterAssistant() }

  val clipboard = LocalClipboardManager.current
  val context = LocalContext.current
  val listState = rememberLazyListState()
  val chatContext = remember(screenContextName, activeClassId) {
    AdminAssistantContext(app = "admin", screen = screenContextName, classId = activeClassId)
  }

  LaunchedEffect(uiState.messages.size, uiState.isSending) {
    val count = uiState.messages.size + if (uiState.isSending) 1 else 0
    if (count > 0) listState.animateScrollToItem(count - 1)
  }

  Column(modifier = Modifier.fillMaxSize().testTag("admin_assistant_chat_screen")) {
    AdminAssistantTopBar(showNewChat = uiState.hasConsent && isOnline, onNewChat = onNewChat, onBack = onBack)

    if (!isOnline) {
      AdminAssistantOfflineState(onBack = onBack)
      return@Column
    }

    if (uiState.isCheckingConsent && !uiState.hasConsent) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ChampagneGold)
      }
      return@Column
    }

    if (!uiState.hasConsent) {
      AdminAssistantConsentScreen(
        isAccepting = uiState.isAcceptingConsent,
        errorMessage = uiState.consentError,
        onAgree = onAcceptTerms,
        onNotNow = onBack
      )
      return@Column
    }

    uiState.errorMessage?.let { err ->
      Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        AdminFeedbackBanner(message = err, isError = true, onDismiss = onDismissError)
      }
    }

    LazyColumn(
      state = listState,
      modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
      contentPadding = PaddingValues(vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (uiState.messages.isEmpty()) {
        item {
          AdminGlassCard(isHero = true) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(20.dp))
              Text("Ask ClassRep Assistant", style = AdminCardTitleStyle)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              "Query suspended lecturers, classes without a prefect, system summaries, or draft notices cleanly. Nothing changes without your Confirm tap.",
              style = AdminCaptionStyle.copy(color = Lavender)
            )
          }
        }
      }

      items(uiState.messages, key = { it.id }) { msg ->
        AdminBubbleItem(
          message = msg,
          onCopy = {
            clipboard.setText(AnnotatedString(msg.content))
            Toast.makeText(context, "Copied reply", Toast.LENGTH_SHORT).show()
          },
          onConfirmAction = { onConfirmAction(msg.id, it) },
          onSaveActionEdit = { id, args -> onSaveActionEdit(msg.id, id, args) },
          onCancelAction = { onCancelAction(msg.id, it) }
        )
      }

      if (uiState.isSending) {
        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(RoyalCardSurface)
                .border(1.dp, AdminBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ChampagneGold, strokeWidth = 2.dp)
              Text("Thinking...", style = AdminCaptionStyle.copy(color = ChampagneGold))
            }
          }
        }
      }
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(RoyalNavSurface)
        .border(1.dp, MetallicGold.copy(alpha = 0.25f))
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AdminAssistantSuggestions.chips.forEach { chip ->
          AssistChip(
            onClick = { onSendMessage(chip.label, chatContext) },
            label = { Text(chip.label, fontSize = 12.sp, color = ChampagneGold) },
            colors = AssistChipDefaults.assistChipColors(containerColor = RoyalSurfaceHigh),
            border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.45f)),
            modifier = Modifier.defaultMinSize(minHeight = 40.dp)
          )
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = uiState.inputText,
          onValueChange = onInputChanged,
          placeholder = { Text("Ask about governance, risk, classes...") },
          colors = royalAdminTextFieldColors(),
          maxLines = 4,
          modifier = Modifier.weight(1f).testTag("admin_assistant_input_field")
        )
        IconButton(
          onClick = { onSendMessage(uiState.inputText, chatContext) },
          enabled = uiState.inputText.isNotBlank() && !uiState.isSending,
          modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (uiState.inputText.isNotBlank() && !uiState.isSending) MetallicGold else RoyalSurfaceHigh)
            .testTag("admin_assistant_send_button")
        ) {
          Icon(
            Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
            tint = if (uiState.inputText.isNotBlank() && !uiState.isSending) Midnight else Lavender
          )
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          "AI can make mistakes. Check important dates, grades and rules.",
          style = AdminCaptionStyle.copy(fontSize = 11.sp, lineHeight = 14.sp, color = Lavender.copy(alpha = 0.8f)),
          modifier = Modifier.weight(1f)
        )
        Text("${uiState.inputText.length}/2000", style = AdminCaptionStyle.copy(fontSize = 11.sp, lineHeight = 14.sp, color = Lavender))
      }
    }
  }
}

@Composable
private fun AdminAssistantTopBar(showNewChat: Boolean, onNewChat: () -> Unit, onBack: () -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().background(RoyalNavSurface)) {
    Row(
      modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("admin_assistant_back_button")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
        }
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        AdminMetallicGoldText(text = "Assistant", style = AdminScreenTitleStyle.copy(fontSize = 20.sp))
      }
      if (showNewChat) {
        TextButton(onClick = onNewChat, modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("admin_assistant_new_chat_button")) {
          Icon(Icons.Default.AddComment, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("New chat", style = AdminFieldLabelStyle.copy(color = ChampagneGold, fontSize = 13.sp))
        }
      }
    }
    HorizontalDivider(thickness = 1.dp, color = MetallicGold.copy(alpha = 0.45f))
  }
}

@Composable
private fun AdminAssistantConsentScreen(isAccepting: Boolean, errorMessage: String?, onAgree: () -> Unit, onNotNow: () -> Unit) {
  Column(modifier = Modifier.fillMaxSize().padding(16.dp).testTag("admin_assistant_consent_screen"), verticalArrangement = Arrangement.Center) {
    AdminGlassCard(isHero = true) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChampagneGold)
        Text("ClassRep Assistant", style = AdminSectionHeadingStyle)
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        "The assistant uses Google's Gemini AI. Your questions and the class information needed to answer them are sent to Google to produce the answer. It cannot change anything without your tap. Do not type passwords or private secrets.",
        style = AdminBodyStyle
      )
      if (!errorMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(errorMessage, style = AdminCaptionStyle.copy(color = CrimsonError))
      }
      Spacer(modifier = Modifier.height(10.dp))
      AdminGoldButton("I agree", onClick = onAgree, isLoading = isAccepting, modifier = Modifier.fillMaxWidth().testTag("admin_assistant_consent_agree"))
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedButton(
        onClick = onNotNow,
        enabled = !isAccepting,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AdminBorder),
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
      ) { Text("Not now", color = Lavender) }
    }
  }
}

@Composable
private fun AdminAssistantOfflineState(onBack: () -> Unit) {
  Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
    AdminGlassCard {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.WifiOff, contentDescription = null, tint = ChampagneGold)
        Text("The assistant needs internet", style = AdminCardTitleStyle)
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text("Reconnect to the internet to use ClassRep Assistant.", style = AdminCaptionStyle)
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
        Text("Go back", color = ChampagneGold)
      }
    }
  }
}

@Composable
private fun AdminBubbleItem(
  message: AdminAssistantMessage,
  onCopy: () -> Unit,
  onConfirmAction: (String) -> Unit,
  onSaveActionEdit: (String, Map<String, Any?>) -> Unit,
  onCancelAction: (String) -> Unit
) {
  val isUser = message.role == AdminAssistantRole.USER
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 320.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(if (isUser) RoyalSurfaceHigh else RoyalCardSurface)
        .border(1.dp, if (isUser) MetallicGold.copy(alpha = 0.65f) else AdminBorder, RoundedCornerShape(16.dp))
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      if (isUser) {
        Text(message.content, style = AdminBodyStyle.copy(fontSize = 15.sp, color = SilverWhite))
      } else {
        AdminMarkdownText(message.content)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          Row(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onCopy() }.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy reply", tint = Lavender, modifier = Modifier.size(14.dp))
            Text("Copy", style = AdminCaptionStyle.copy(fontSize = 12.sp, color = Lavender))
          }
        }
      }
    }
    message.actions.forEach { act ->
      AdminActionProposalCard(
        action = act,
        onConfirm = { onConfirmAction(act.id) },
        onSaveEdit = { newArgs -> onSaveActionEdit(act.id, newArgs) },
        onCancel = { onCancelAction(act.id) }
      )
    }
  }
}
