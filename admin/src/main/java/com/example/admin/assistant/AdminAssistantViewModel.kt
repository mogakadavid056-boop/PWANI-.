package com.example.admin.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.admin.data.AdminSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminAssistantUiState(
  val isCheckingConsent: Boolean = false,
  val isAcceptingConsent: Boolean = false,
  val hasConsent: Boolean = false,
  val consentError: String? = null,
  val messages: List<AdminAssistantMessage> = emptyList(),
  val isSending: Boolean = false,
  val inputText: String = "",
  val errorMessage: String? = null
)

class AdminAssistantViewModel(application: Application) : AndroidViewModel(application) {
  private val api = AdminAssistantApi()
  private val _uiState = MutableStateFlow(AdminAssistantUiState())
  val uiState: StateFlow<AdminAssistantUiState> = _uiState.asStateFlow()

  private var loadedUserId: String = ""

  fun onEnterAssistant(session: AdminSession?) {
    val userId = session?.userId.orEmpty()
    val token = session?.accessToken.orEmpty()
    if (userId.isBlank() || token.isBlank()) return

    val ctx = getApplication<Application>()
    val saved = if (loadedUserId != userId) {
      loadedUserId = userId
      AdminAssistantStorage.loadMessages(ctx, userId)
    } else {
      _uiState.value.messages.ifEmpty { AdminAssistantStorage.loadMessages(ctx, userId) }
    }
    val localAccepted = AdminAssistantStorage.hasLocalConsent(ctx, userId)

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
            if (accepted) AdminAssistantStorage.setLocalConsent(ctx, userId, true)
            _uiState.update { it.copy(hasConsent = accepted, isCheckingConsent = false) }
          },
          onFailure = { err ->
            _uiState.update { it.copy(isCheckingConsent = false, consentError = err.message) }
          }
        )
      }
    }
  }

  fun acceptTerms(session: AdminSession?) {
    val userId = session?.userId.orEmpty()
    val token = session?.accessToken.orEmpty()
    if (userId.isBlank() || token.isBlank() || _uiState.value.isAcceptingConsent) return

    _uiState.update { it.copy(isAcceptingConsent = true, consentError = null) }
    viewModelScope.launch {
      api.acceptAiTerms(token).fold(
        onSuccess = {
          AdminAssistantStorage.setLocalConsent(getApplication(), userId, true)
          _uiState.update { it.copy(isAcceptingConsent = false, hasConsent = true, consentError = null) }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(isAcceptingConsent = false, consentError = err.message ?: "Could not save consent. Please try again.")
          }
        }
      )
    }
  }

  fun onInputChanged(text: String) {
    _uiState.update { it.copy(inputText = text.take(2000), errorMessage = null) }
  }

  fun newChat(session: AdminSession?) {
    val userId = session?.userId ?: loadedUserId
    if (userId.isNotBlank()) AdminAssistantStorage.clearMessages(getApplication(), userId)
    _uiState.update { it.copy(messages = emptyList(), errorMessage = null) }
  }

  fun sendMessage(
    rawText: String,
    session: AdminSession?,
    context: AdminAssistantContext,
    isOnline: Boolean
  ) {
    val text = rawText.trim().take(2000)
    if (text.isBlank() || _uiState.value.isSending) return
    if (!isOnline) {
      _uiState.update { it.copy(errorMessage = "The assistant needs internet") }
      return
    }
    val token = session?.accessToken.orEmpty()
    val userId = session?.userId.orEmpty()
    if (token.isBlank() || userId.isBlank()) return

    val userMsg = AdminAssistantMessage(role = AdminAssistantRole.USER, content = text)
    val updatedWithUser = (_uiState.value.messages + userMsg).takeLast(AdminAssistantStorage.MAX_LOCAL_MESSAGES)
    AdminAssistantStorage.saveMessages(getApplication(), userId, updatedWithUser)

    _uiState.update {
      it.copy(
        messages = updatedWithUser,
        inputText = if (rawText == it.inputText) "" else it.inputText,
        isSending = true,
        errorMessage = null
      )
    }

    viewModelScope.launch {
      api.sendChat(updatedWithUser, context, token).fold(
        onSuccess = { res ->
          val assistantMsg = AdminAssistantMessage(
            role = AdminAssistantRole.ASSISTANT,
            content = res.reply,
            actions = res.actions
          )
          val updated = (_uiState.value.messages + assistantMsg).takeLast(AdminAssistantStorage.MAX_LOCAL_MESSAGES)
          AdminAssistantStorage.saveMessages(getApplication(), userId, updated)
          _uiState.update { it.copy(messages = updated, isSending = false, errorMessage = null) }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(isSending = false, errorMessage = err.message ?: "Unable to reach assistant.")
          }
        }
      )
    }
  }

  fun updateActionArgs(messageId: String, actionId: String, newArgs: Map<String, Any?>, userId: String) {
    val updated = mutateAction(messageId, actionId) {
      it.copy(args = newArgs, status = AdminActionCardStatus.PENDING, resultMessage = null)
    }
    if (userId.isNotBlank()) AdminAssistantStorage.saveMessages(getApplication(), userId, updated)
  }

  fun cancelAction(messageId: String, actionId: String, userId: String) {
    val updated = mutateAction(messageId, actionId) {
      it.copy(status = AdminActionCardStatus.CANCELLED, resultMessage = null)
    }
    if (userId.isNotBlank()) AdminAssistantStorage.saveMessages(getApplication(), userId, updated)
  }

  fun confirmAction(
    messageId: String,
    actionId: String,
    fallbackClassId: String?,
    session: AdminSession?,
    executor: AdminActionExecutor
  ) {
    val token = session?.accessToken.orEmpty()
    val userId = session?.userId.orEmpty()
    val target = _uiState.value.messages
      .firstOrNull { it.id == messageId }
      ?.actions?.firstOrNull { it.id == actionId } ?: return
    if (target.status == AdminActionCardStatus.EXECUTING || target.status == AdminActionCardStatus.DONE) return

    mutateAction(messageId, actionId) { it.copy(status = AdminActionCardStatus.EXECUTING, resultMessage = null) }
    viewModelScope.launch {
      val res = executor.execute(target, fallbackClassId, token)
      val next = res.fold(
        onSuccess = { ok -> mutateAction(messageId, actionId) { it.copy(status = AdminActionCardStatus.DONE, resultMessage = ok) } },
        onFailure = { err -> mutateAction(messageId, actionId) { it.copy(status = AdminActionCardStatus.FAILED, resultMessage = err.message ?: "Action failed.") } }
      )
      if (userId.isNotBlank()) AdminAssistantStorage.saveMessages(getApplication(), userId, next)
    }
  }

  fun clearError() = _uiState.update { it.copy(errorMessage = null, consentError = null) }

  fun clearOnLogout() {
    loadedUserId = ""
    AdminAssistantStorage.clearOnLogout(getApplication())
    _uiState.value = AdminAssistantUiState()
  }

  private fun mutateAction(
    messageId: String,
    actionId: String,
    transform: (AdminAssistantAction) -> AdminAssistantAction
  ): List<AdminAssistantMessage> {
    var result: List<AdminAssistantMessage> = emptyList()
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
}
