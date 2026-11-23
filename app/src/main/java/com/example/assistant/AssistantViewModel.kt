package com.example.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssistantUiState(
  val isCheckingConsent: Boolean = false,
  val isAcceptingConsent: Boolean = false,
  val hasConsent: Boolean = false,
  val consentError: String? = null,
  val messages: List<AssistantMessage> = emptyList(),
  val isSending: Boolean = false,
  val inputText: String = "",
  val speechLanguage: SpeechLanguage = SpeechLanguage.ENGLISH,
  val errorMessage: String? = null
)

class AssistantViewModel(
  application: Application
) : AndroidViewModel(application) {

  private val api = AssistantApi()
  private val firebaseAssistant = FirebaseAssistantService()
  private val _uiState = MutableStateFlow(AssistantUiState())
  val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

  private var loadedUserId: String = ""

  fun onEnterAssistant(session: UserSession?) {
    val userId = session?.userId.orEmpty()
    val token = session?.accessToken.orEmpty()
    if (userId.isBlank() || token.isBlank()) return

    val appCtx = getApplication<Application>()
    val saved = if (loadedUserId != userId) {
      loadedUserId = userId
      AssistantStorage.loadMessages(appCtx, userId)
    } else {
      _uiState.value.messages.ifEmpty { AssistantStorage.loadMessages(appCtx, userId) }
    }
    val localAccepted = AssistantStorage.hasLocalConsent(appCtx, userId)

    _uiState.update {
      it.copy(
        messages = saved,
        hasConsent = localAccepted,
        isCheckingConsent = !localAccepted,
        consentError = null,
        errorMessage = null
      )
    }

    if (!localAccepted) {
      viewModelScope.launch {
        api.hasAiConsent(token).fold(
          onSuccess = { accepted ->
            if (accepted) AssistantStorage.setLocalConsent(appCtx, userId, true)
            _uiState.update { it.copy(hasConsent = accepted, isCheckingConsent = false) }
          },
          onFailure = {
            _uiState.update { it.copy(isCheckingConsent = false, consentError = null) }
          }
        )
      }
    }
  }

  fun acceptTerms(session: UserSession?) {
    val userId = session?.userId.orEmpty()
    val token = session?.accessToken.orEmpty()
    if (userId.isBlank() || token.isBlank() || _uiState.value.isAcceptingConsent) return

    _uiState.update { it.copy(isAcceptingConsent = true, consentError = null) }
    viewModelScope.launch {
      // Sync consent flag to Supabase if RPC is available, always persist locally so user can chat cleanly.
      runCatching { api.acceptAiTerms(token) }
      AssistantStorage.setLocalConsent(getApplication(), userId, true)
      _uiState.update { it.copy(isAcceptingConsent = false, hasConsent = true, consentError = null) }
    }
  }

  fun onInputChanged(text: String) {
    _uiState.update { it.copy(inputText = text.take(2000), errorMessage = null) }
  }

  fun toggleSpeechLanguage() {
    _uiState.update {
      val next = if (it.speechLanguage == SpeechLanguage.ENGLISH) {
        SpeechLanguage.KISWAHILI
      } else {
        SpeechLanguage.ENGLISH
      }
      it.copy(speechLanguage = next)
    }
  }

  fun appendDictation(spoken: String) {
    val clean = spoken.trim()
    if (clean.isBlank()) return
    _uiState.update { st ->
      val combined = if (st.inputText.isBlank()) clean else "${st.inputText.trimEnd()} $clean"
      st.copy(inputText = combined.take(2000), errorMessage = null)
    }
  }

  fun newChat(session: UserSession?) {
    val userId = session?.userId ?: loadedUserId
    if (userId.isNotBlank()) {
      AssistantStorage.clearMessages(getApplication(), userId)
    }
    _uiState.update { it.copy(messages = emptyList(), errorMessage = null) }
  }

  fun sendMessage(
    rawText: String,
    session: UserSession?,
    context: AssistantContext,
    isOnline: Boolean,
    isRepInClass: Boolean = false
  ) {
    val text = rawText.trim().take(2000)
    if (text.isBlank() || _uiState.value.isSending) return
    if (!isOnline) {
      _uiState.update { it.copy(errorMessage = "The assistant needs internet") }
      return
    }
    val validSession = session?.takeIf { it.accessToken.isNotBlank() && it.userId.isNotBlank() } ?: return

    val userMsg = AssistantMessage(role = AssistantRole.USER, content = text)
    val updatedWithUser = (_uiState.value.messages + userMsg).takeLast(AssistantStorage.MAX_LOCAL_MESSAGES)
    AssistantStorage.saveMessages(getApplication(), validSession.userId, updatedWithUser)

    _uiState.update {
      it.copy(
        messages = updatedWithUser,
        inputText = if (rawText == it.inputText) "" else it.inputText,
        isSending = true,
        errorMessage = null
      )
    }

    viewModelScope.launch {
      firebaseAssistant.sendChat(
        messages = updatedWithUser,
        context = context,
        session = validSession,
        isRepInClass = isRepInClass
      ).fold(
        onSuccess = { response ->
          val assistantMsg = AssistantMessage(
            role = AssistantRole.ASSISTANT,
            content = response.reply,
            actions = response.actions
          )
          val updated = (_uiState.value.messages + assistantMsg).takeLast(AssistantStorage.MAX_LOCAL_MESSAGES)
          AssistantStorage.saveMessages(getApplication(), validSession.userId, updated)
          _uiState.update { it.copy(messages = updated, isSending = false, errorMessage = null) }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isSending = false,
              errorMessage = err.message ?: "Unable to reach assistant."
            )
          }
        }
      )
    }
  }

  fun updateActionArgs(
    messageId: String,
    actionId: String,
    newArgs: Map<String, Any?>,
    userId: String
  ) {
    val updated = mutateAction(messageId, actionId) {
      it.copy(args = newArgs, status = ActionCardStatus.PENDING, resultMessage = null)
    }
    persistIfUser(userId, updated)
  }

  fun cancelAction(messageId: String, actionId: String, userId: String) {
    val updated = mutateAction(messageId, actionId) {
      it.copy(status = ActionCardStatus.CANCELLED, resultMessage = null)
    }
    persistIfUser(userId, updated)
  }

  fun confirmAction(
    messageId: String,
    actionId: String,
    fallbackClassId: String?,
    session: UserSession?,
    executor: ActionExecutor
  ) {
    val token = session?.accessToken.orEmpty()
    val userId = session?.userId.orEmpty()
    val targetAction = _uiState.value.messages
      .firstOrNull { it.id == messageId }
      ?.actions
      ?.firstOrNull { it.id == actionId } ?: return
    if (targetAction.status == ActionCardStatus.EXECUTING || targetAction.status == ActionCardStatus.DONE) return

    mutateAction(messageId, actionId) {
      it.copy(status = ActionCardStatus.EXECUTING, resultMessage = null)
    }

    viewModelScope.launch {
      val result = executor.execute(targetAction, fallbackClassId, token)
      val nextMessages = result.fold(
        onSuccess = { okMsg ->
          mutateAction(messageId, actionId) {
            it.copy(status = ActionCardStatus.DONE, resultMessage = okMsg)
          }
        },
        onFailure = { err ->
          mutateAction(messageId, actionId) {
            it.copy(
              status = ActionCardStatus.FAILED,
              resultMessage = err.message ?: "Action failed."
            )
          }
        }
      )
      persistIfUser(userId, nextMessages)
    }
  }

  fun clearError() = _uiState.update { it.copy(errorMessage = null, consentError = null) }

  fun clearOnLogout() {
    loadedUserId = ""
    AssistantStorage.clearOnLogout(getApplication())
    _uiState.value = AssistantUiState()
  }

  private fun mutateAction(
    messageId: String,
    actionId: String,
    transform: (AssistantAction) -> AssistantAction
  ): List<AssistantMessage> {
    var result: List<AssistantMessage> = emptyList()
    _uiState.update { st ->
      val next = st.messages.map { msg ->
        if (msg.id != messageId) msg
        else msg.copy(actions = msg.actions.map { act -> if (act.id == actionId) transform(act) else act })
      }
      result = next
      st.copy(messages = next)
    }
    return result
  }

  private fun persistIfUser(userId: String, messages: List<AssistantMessage>) {
    if (userId.isNotBlank()) {
      AssistantStorage.saveMessages(getApplication(), userId, messages)
    }
  }
}
