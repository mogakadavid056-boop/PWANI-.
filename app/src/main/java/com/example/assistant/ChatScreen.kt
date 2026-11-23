package com.example.assistant

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserSession
import com.example.ui.components.MetallicGoldText
import com.example.ui.components.PrimaryButton
import com.example.ui.components.RoyalCard
import com.example.ui.components.royalOutlinedTextFieldColors
import com.example.ui.theme.*

@Composable
fun ChatScreen(
  uiState: AssistantUiState,
  session: UserSession?,
  isOnline: Boolean,
  isRepInClass: Boolean,
  activeClassId: String?,
  screenContextName: String,
  executor: ActionExecutor,
  onEnterAssistant: () -> Unit,
  onAcceptTerms: () -> Unit,
  onInputChanged: (String) -> Unit,
  onToggleSpeechLanguage: () -> Unit,
  onDictationResult: (String) -> Unit,
  onNewChat: () -> Unit,
  onSendMessage: (String, AssistantContext) -> Unit,
  onConfirmAction: (messageId: String, actionId: String) -> Unit,
  onUpdateActionArgs: (messageId: String, actionId: String, args: Map<String, Any?>) -> Unit,
  onCancelAction: (messageId: String, actionId: String) -> Unit,
  onDismissError: () -> Unit,
  onBack: () -> Unit
) {
  LaunchedEffect(session?.userId) { onEnterAssistant() }

  val clipboard = LocalClipboardManager.current
  val context = LocalContext.current
  val listState = rememberLazyListState()
  val chips = remember(session?.role, isRepInClass) {
    AssistantSuggestions.forMainRole(session?.role, isRepInClass)
  }
  val chatContext = remember(screenContextName, activeClassId) {
    AssistantContext(app = "main", screen = screenContextName, classId = activeClassId)
  }

  val speechLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val spoken = result.data
        ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        ?.firstOrNull()
        .orEmpty()
      if (spoken.isNotBlank()) onDictationResult(spoken)
    }
  }

  LaunchedEffect(uiState.messages.size, uiState.isSending) {
    val count = uiState.messages.size + if (uiState.isSending) 1 else 0
    if (count > 0) listState.animateScrollToItem(count - 1)
  }

  Column(modifier = Modifier.fillMaxSize().testTag("assistant_chat_screen")) {
    AssistantTopBar(
      showNewChat = uiState.hasConsent && isOnline,
      onNewChat = onNewChat,
      onBack = onBack
    )

    if (!isOnline) {
      AssistantOfflineState(onBack = onBack)
      return@Column
    }

    if (uiState.isCheckingConsent && !uiState.hasConsent) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ChampagneGold)
      }
      return@Column
    }

    if (!uiState.hasConsent) {
      AssistantConsentScreen(
        isAccepting = uiState.isAcceptingConsent,
        errorMessage = uiState.consentError,
        onAgree = onAcceptTerms,
        onNotNow = onBack
      )
      return@Column
    }

    if (!uiState.errorMessage.isNullOrBlank()) {
      Box(modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 6.dp)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ButtonRadius))
            .background(ErrorContainer)
            .border(1.dp, ErrorRed.copy(alpha = 0.55f), RoundedCornerShape(ButtonRadius))
            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
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
              text = uiState.errorMessage,
              style = CaptionStyle.copy(color = SilverWhite, fontWeight = FontWeight.Medium)
            )
          }
          IconButton(onClick = onDismissError, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Dismiss error", tint = Lavender, modifier = Modifier.size(18.dp))
          }
        }
      }
    }

    LazyColumn(
      state = listState,
      modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = ScreenPadding),
      contentPadding = PaddingValues(vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (uiState.messages.isEmpty()) {
        item {
          RoyalCard(isHero = true) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(20.dp))
              Text("Ask ClassRep Assistant", style = CardTitleStyle)
            }
            Text(
              text = "Ask about your timetable, attendance, points, notices, polls, or draft class updates. Any changes require your Confirm tap.",
              style = CaptionStyle.copy(color = Lavender)
            )
          }
        }
      }

      items(uiState.messages, key = { it.id }) { msg ->
        AssistantBubbleItem(
          message = msg,
          onCopy = {
            clipboard.setText(AnnotatedString(msg.content))
            Toast.makeText(context, "Copied reply", Toast.LENGTH_SHORT).show()
          },
          onConfirmAction = { actionId -> onConfirmAction(msg.id, actionId) },
          onSaveActionEdit = { actionId, args -> onUpdateActionArgs(msg.id, actionId, args) },
          onCancelAction = { actionId -> onCancelAction(msg.id, actionId) }
        )
      }

      if (uiState.isSending) {
        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(RoyalCardSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ChampagneGold, strokeWidth = 2.dp)
              Text("Thinking...", style = CaptionStyle.copy(color = ChampagneGold))
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
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        chips.forEach { chip ->
          AssistChip(
            onClick = {
              if (chip.autoSend) onSendMessage(chip.label, chatContext)
              else onInputChanged(chip.label)
            },
            label = { Text(chip.label, fontSize = 12.sp, color = ChampagneGold) },
            colors = AssistChipDefaults.assistChipColors(containerColor = RoyalSurfaceHigh),
            border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.45f)),
            modifier = Modifier.defaultMinSize(minHeight = 40.dp)
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        TextButton(
          onClick = onToggleSpeechLanguage,
          modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).testTag("assistant_lang_toggle")
        ) {
          Text(uiState.speechLanguage.shortLabel, style = FieldLabelStyle.copy(color = ChampagneGold, fontSize = 12.sp))
        }

        IconButton(
          onClick = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
              putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
              putExtra(RecognizerIntent.EXTRA_LANGUAGE, uiState.speechLanguage.localeTag)
              putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your question")
            }
            runCatching { speechLauncher.launch(intent) }.onFailure {
              Toast.makeText(context, "Voice input is not available on this device.", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.size(48.dp).testTag("assistant_mic_button")
        ) {
          Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = ChampagneGold)
        }

        OutlinedTextField(
          value = uiState.inputText,
          onValueChange = onInputChanged,
          placeholder = { Text("Ask anything about your classes...") },
          colors = royalOutlinedTextFieldColors(),
          maxLines = 4,
          modifier = Modifier.weight(1f).testTag("assistant_input_field")
        )

        IconButton(
          onClick = { onSendMessage(uiState.inputText, chatContext) },
          enabled = uiState.inputText.isNotBlank() && !uiState.isSending,
          modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (uiState.inputText.isNotBlank() && !uiState.isSending) MetallicGold else RoyalSurfaceHigh)
            .testTag("assistant_send_button")
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
          text = "AI can make mistakes. Check important dates, grades and rules.",
          style = CaptionStyle.copy(fontSize = 11.sp, lineHeight = 14.sp, color = Lavender.copy(alpha = 0.8f)),
          modifier = Modifier.weight(1f)
        )
        Text(
          text = "${uiState.inputText.length}/2000",
          style = CaptionStyle.copy(fontSize = 11.sp, lineHeight = 14.sp, color = Lavender)
        )
      }
    }
  }
}

@Composable
private fun AssistantTopBar(showNewChat: Boolean, onNewChat: () -> Unit, onBack: () -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().background(RoyalNavSurface)) {
    Row(
      modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("assistant_back_button")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
        }
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        MetallicGoldText(text = "Assistant", style = ScreenTitleStyle.copy(fontSize = 20.sp))
      }
      if (showNewChat) {
        TextButton(onClick = onNewChat, modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("assistant_new_chat_button")) {
          Icon(Icons.Default.AddComment, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("New chat", style = FieldLabelStyle.copy(color = ChampagneGold, fontSize = 13.sp))
        }
      }
    }
    HorizontalDivider(thickness = 1.dp, color = MetallicGold.copy(alpha = 0.45f))
  }
}

@Composable
private fun AssistantConsentScreen(
  isAccepting: Boolean,
  errorMessage: String?,
  onAgree: () -> Unit,
  onNotNow: () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(ScreenPadding).testTag("assistant_consent_screen"),
    verticalArrangement = Arrangement.Center
  ) {
    RoyalCard(isHero = true) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChampagneGold)
        Text("ClassRep Assistant", style = SectionHeadingStyle)
      }
      Text(
        text = "The assistant uses Google's Gemini AI. Your questions and the class information needed to answer them are sent to Google to produce the answer. It cannot change anything without your tap. Do not type passwords or private secrets.",
        style = BodyStyle
      )
      if (!errorMessage.isNullOrBlank()) {
        Text(text = errorMessage, style = CaptionStyle.copy(color = ErrorColor))
      }
      Spacer(modifier = Modifier.height(6.dp))
      PrimaryButton(
        text = "I agree",
        onClick = onAgree,
        isLoading = isAccepting,
        modifier = Modifier.fillMaxWidth().testTag("assistant_consent_agree_button")
      )
      OutlinedButton(
        onClick = onNotNow,
        enabled = !isAccepting,
        shape = RoundedCornerShape(ButtonRadius),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("assistant_consent_decline_button")
      ) {
        Text("Not now", color = Lavender)
      }
    }
  }
}

@Composable
private fun AssistantOfflineState(onBack: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize().padding(ScreenPadding),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    RoyalCard {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.WifiOff, contentDescription = null, tint = ChampagneGold)
        Text("The assistant needs internet", style = CardTitleStyle)
      }
      Text("Reconnect to the internet to ask questions or draft updates with ClassRep Assistant.", style = CaptionStyle)
      OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
        Text("Go back", color = ChampagneGold)
      }
    }
  }
}

@Composable
private fun AssistantBubbleItem(
  message: AssistantMessage,
  onCopy: () -> Unit,
  onConfirmAction: (String) -> Unit,
  onSaveActionEdit: (String, Map<String, Any?>) -> Unit,
  onCancelAction: (String) -> Unit
) {
  val isUser = message.role == AssistantRole.USER
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
        .border(1.dp, if (isUser) MetallicGold.copy(alpha = 0.65f) else DarkBorder, RoundedCornerShape(16.dp))
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      if (isUser) {
        Text(text = message.content, style = BodyStyle.copy(fontSize = 15.sp, color = SilverWhite))
      } else {
        AssistantMarkdownText(markdown = message.content)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable { onCopy() }
              .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy reply", tint = Lavender, modifier = Modifier.size(14.dp))
            Text("Copy", style = CaptionStyle.copy(fontSize = 12.sp, color = Lavender))
          }
        }
      }
    }
    message.actions.forEach { act ->
      ActionProposalCard(
        action = act,
        onConfirm = { onConfirmAction(act.id) },
        onSaveEdit = { newArgs -> onSaveActionEdit(act.id, newArgs) },
        onCancel = { onCancelAction(act.id) }
      )
    }
  }
}
