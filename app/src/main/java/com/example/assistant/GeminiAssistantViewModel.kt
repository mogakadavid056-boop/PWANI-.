package com.example.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NetworkMonitor
import com.example.data.SupabaseConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GeminiAssistantViewModel(application: Application) : AndroidViewModel(application) {
  companion object {
    private const val STREAM_UI_THROTTLE_MS = 45L
  }

  private val _uiState = MutableStateFlow(
    GeminiChatUiState(isConfigured = GeminiConfig.isConfigured())
  )
  val uiState: StateFlow<GeminiChatUiState> = _uiState.asStateFlow()

  private var loadedUserId: String? = null
  private var streamJob: Job? = null

  /** Hook overridable in tests for deterministic configuration checks. */
  internal var isConfiguredProvider: () -> Boolean = { GeminiConfig.isConfigured() }

  /** Hook overridable in tests for deterministic stream simulation. */
  internal var streamProvider: (List<GeminiMessage>, String) -> Flow<String> = { history, prompt ->
    GeminiChatService.streamReply(history, prompt)
  }

  fun onEnterChat(userId: String?) {
    val resolvedId = userId.orEmpty().ifBlank { "guest" }
    val configured = isConfiguredProvider()
    if (loadedUserId != resolvedId) {
      loadedUserId = resolvedId
      val saved = GeminiChatStorage.loadMessages(getApplication(), resolvedId)
      _uiState.update {
        it.copy(
          messages = saved,
          isConfigured = configured,
          errorMessage = null,
          lastFailedPrompt = null
        )
      }
    } else {
      _uiState.update { it.copy(isConfigured = configured) }
    }
  }

  fun onInputChanged(text: String) {
    _uiState.update { it.copy(inputText = text) }
  }

  fun clearError() {
    _uiState.update { it.copy(errorMessage = null, lastFailedPrompt = null) }
  }

  fun stopStreaming() {
    val activeJob = streamJob ?: return
    streamJob = null
    activeJob.cancel()
    val uid = loadedUserId.orEmpty().ifBlank { "guest" }
    _uiState.update { state ->
      val finalized = state.messages.mapNotNull { msg ->
        when {
          msg.isStreaming && msg.text.isBlank() -> null
          msg.isStreaming -> msg.copy(isStreaming = false)
          else -> msg
        }
      }
      GeminiChatStorage.saveMessages(getApplication(), uid, finalized)
      state.copy(messages = finalized, isStreaming = false)
    }
  }

  fun newChat(userId: String?) {
    streamJob?.cancel()
    streamJob = null
    val resolvedId = userId.orEmpty().ifBlank { "guest" }
    loadedUserId = resolvedId
    GeminiChatStorage.clearMessages(getApplication(), resolvedId)
    _uiState.update {
      GeminiChatUiState(
        messages = emptyList(),
        inputText = "",
        isStreaming = false,
        errorMessage = null,
        lastFailedPrompt = null,
        isConfigured = isConfiguredProvider()
      )
    }
  }

  fun sendMessage(rawPrompt: String, userId: String?, isOnline: Boolean) {
    val prompt = rawPrompt.trim()
    if (prompt.isEmpty() || _uiState.value.isStreaming) return
    if (!validateCanStart(prompt, isOnline)) return

    val resolvedId = userId.orEmpty().ifBlank { "guest" }
    loadedUserId = resolvedId
    val historyBeforeTurn = _uiState.value.messages.filter { !it.isStreaming && it.text.isNotBlank() }

    val userMessage = GeminiMessage(
      role = ChatRole.USER,
      text = prompt
    )
    val modelPlaceholder = GeminiMessage(
      role = ChatRole.MODEL,
      text = "",
      isStreaming = true
    )

    _uiState.update { state ->
      state.copy(
        messages = state.messages + userMessage + modelPlaceholder,
        inputText = "",
        isStreaming = true,
        errorMessage = null,
        lastFailedPrompt = null,
        isConfigured = true
      )
    }

    launchStreamTurn(
      resolvedId = resolvedId,
      historyBeforeTurn = historyBeforeTurn,
      prompt = prompt,
      modelPlaceholderId = modelPlaceholder.id
    )
  }

  fun regenerateLastResponse(userId: String?, isOnline: Boolean) {
    if (_uiState.value.isStreaming) return
    val currentMessages = _uiState.value.messages
    val lastUserIndex = currentMessages.indexOfLast { it.role == ChatRole.USER && it.text.isNotBlank() }
    if (lastUserIndex < 0) return

    val lastUserMsg = currentMessages[lastUserIndex]
    if (!validateCanStart(lastUserMsg.text, isOnline)) return

    val resolvedId = userId.orEmpty().ifBlank { "guest" }
    loadedUserId = resolvedId
    val historyBeforeTurn = currentMessages.subList(0, lastUserIndex)
      .filter { !it.isStreaming && it.text.isNotBlank() }
    val preservedMessages = currentMessages.subList(0, lastUserIndex + 1)

    val modelPlaceholder = GeminiMessage(
      role = ChatRole.MODEL,
      text = "",
      isStreaming = true
    )

    _uiState.update { state ->
      state.copy(
        messages = preservedMessages + modelPlaceholder,
        isStreaming = true,
        errorMessage = null,
        lastFailedPrompt = null,
        isConfigured = true
      )
    }

    launchStreamTurn(
      resolvedId = resolvedId,
      historyBeforeTurn = historyBeforeTurn,
      prompt = lastUserMsg.text,
      modelPlaceholderId = modelPlaceholder.id
    )
  }

  fun retryLastError(userId: String?, isOnline: Boolean) {
    if (_uiState.value.isStreaming) return
    val lastUserMsg = _uiState.value.messages.lastOrNull { it.role == ChatRole.USER && it.text.isNotBlank() }
    val failedPrompt = _uiState.value.lastFailedPrompt
    if (failedPrompt != null && (lastUserMsg == null || lastUserMsg.text != failedPrompt)) {
      sendMessage(failedPrompt, userId, isOnline)
    } else if (lastUserMsg != null) {
      regenerateLastResponse(userId, isOnline)
    }
  }

  private fun validateCanStart(prompt: String, isOnline: Boolean): Boolean {
    if (!isConfiguredProvider()) {
      _uiState.update {
        it.copy(
          isConfigured = false,
          errorMessage = GeminiConfig.MISSING_API_KEY_MESSAGE,
          lastFailedPrompt = prompt
        )
      }
      return false
    }
    val effectivelyOnline = isOnline || NetworkMonitor.checkOnlineNow(getApplication())
    if (!effectivelyOnline) {
      _uiState.update {
        it.copy(
          errorMessage = "You're offline. Connect to the internet to chat with Gemini.",
          lastFailedPrompt = prompt
        )
      }
      return false
    }
    return true
  }

  private fun launchStreamTurn(
    resolvedId: String,
    historyBeforeTurn: List<GeminiMessage>,
    prompt: String,
    modelPlaceholderId: String
  ) {
    streamJob = viewModelScope.launch {
      val buffer = StringBuilder()
      var lastEmitTimeMs = 0L
      try {
        streamProvider(historyBeforeTurn, prompt).collect { chunk ->
          buffer.append(chunk)
          val now = System.currentTimeMillis()
          if (lastEmitTimeMs == 0L || now - lastEmitTimeMs >= STREAM_UI_THROTTLE_MS) {
            lastEmitTimeMs = now
            updateStreamingMessage(modelPlaceholderId, buffer.toString())
          }
        }

        // Flush full buffered response and finalize message
        val completeText = buffer.toString()
        _uiState.update { state ->
          val finalized = state.messages.mapNotNull { msg ->
            when {
              msg.id == modelPlaceholderId && completeText.isBlank() -> null
              msg.id == modelPlaceholderId -> msg.copy(text = completeText, isStreaming = false)
              else -> msg
            }
          }
          GeminiChatStorage.saveMessages(getApplication(), resolvedId, finalized)
          state.copy(messages = finalized, isStreaming = false, lastFailedPrompt = null)
        }
      } catch (e: CancellationException) {
        val partial = buffer.toString()
        _uiState.update { state ->
          val finalized = state.messages.mapNotNull { msg ->
            when {
              msg.id == modelPlaceholderId && partial.isBlank() -> null
              msg.id == modelPlaceholderId -> msg.copy(text = partial, isStreaming = false)
              else -> msg
            }
          }
          GeminiChatStorage.saveMessages(getApplication(), resolvedId, finalized)
          state.copy(messages = finalized, isStreaming = false)
        }
        throw e
      } catch (e: Throwable) {
        val partial = buffer.toString()
        val friendly = if (SupabaseConfig.isNetworkError(e)) {
          "Connection interrupted. Check your internet and try again."
        } else {
          e.message?.takeIf { it.isNotBlank() } ?: "Could not get a response from Gemini."
        }
        _uiState.update { state ->
          val finalized = state.messages.mapNotNull { msg ->
            when {
              msg.id == modelPlaceholderId && partial.isBlank() -> null
              msg.id == modelPlaceholderId -> msg.copy(text = partial, isStreaming = false)
              else -> msg
            }
          }
          GeminiChatStorage.saveMessages(getApplication(), resolvedId, finalized)
          state.copy(
            messages = finalized,
            isStreaming = false,
            errorMessage = friendly,
            lastFailedPrompt = prompt
          )
        }
      } finally {
        streamJob = null
      }
    }
  }

  private fun updateStreamingMessage(modelMessageId: String, latestText: String) {
    _uiState.update { state ->
      val idx = state.messages.indexOfLast { it.id == modelMessageId }
      if (idx < 0) return@update state
      val mutable = state.messages.toMutableList()
      mutable[idx] = mutable[idx].copy(text = latestText, isStreaming = true)
      state.copy(messages = mutable)
    }
  }

  fun clearOnLogout() {
    streamJob?.cancel()
    streamJob = null
    loadedUserId = null
    GeminiChatStorage.clearOnLogout(getApplication())
    _uiState.value = GeminiChatUiState(isConfigured = isConfiguredProvider())
  }
}
