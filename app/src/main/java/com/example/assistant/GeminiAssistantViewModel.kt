package com.example.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SupabaseConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GeminiAssistantViewModel(application: Application) : AndroidViewModel(application) {
  private val _uiState = MutableStateFlow(
    GeminiChatUiState(isConfigured = GeminiConfig.isConfigured())
  )
  val uiState: StateFlow<GeminiChatUiState> = _uiState.asStateFlow()

  private var loadedUserId: String? = null
  private var streamJob: Job? = null

  fun onEnterChat(userId: String?) {
    val resolvedId = userId.orEmpty().ifBlank { "guest" }
    val configured = GeminiConfig.isConfigured()
    if (loadedUserId != resolvedId) {
      loadedUserId = resolvedId
      val saved = GeminiChatStorage.loadMessages(getApplication(), resolvedId)
      _uiState.update {
        it.copy(
          messages = saved,
          isConfigured = configured,
          errorMessage = null
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
    _uiState.update { it.copy(errorMessage = null) }
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
        isConfigured = GeminiConfig.isConfigured()
      )
    }
  }

  fun sendMessage(rawPrompt: String, userId: String?, isOnline: Boolean) {
    val prompt = rawPrompt.trim()
    if (prompt.isEmpty() || _uiState.value.isStreaming) return

    if (!GeminiConfig.isConfigured()) {
      _uiState.update {
        it.copy(
          isConfigured = false,
          errorMessage = GeminiConfig.MISSING_API_KEY_MESSAGE
        )
      }
      return
    }
    if (!isOnline) {
      _uiState.update {
        it.copy(errorMessage = "You're offline. Connect to the internet to chat with Gemini.")
      }
      return
    }

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
        isConfigured = true
      )
    }

    streamJob = viewModelScope.launch {
      val buffer = StringBuilder()
      try {
        GeminiChatService.streamReply(historyBeforeTurn, prompt).collect { chunk ->
          buffer.append(chunk)
          val updatedText = buffer.toString()
          _uiState.update { state ->
            state.copy(
              messages = state.messages.map { msg ->
                if (msg.id == modelPlaceholder.id) msg.copy(text = updatedText, isStreaming = true)
                else msg
              }
            )
          }
        }

        _uiState.update { state ->
          val finalized = state.messages.map { msg ->
            if (msg.id == modelPlaceholder.id) msg.copy(isStreaming = false) else msg
          }
          GeminiChatStorage.saveMessages(getApplication(), resolvedId, finalized)
          state.copy(messages = finalized, isStreaming = false)
        }
      } catch (e: CancellationException) {
        _uiState.update { state ->
          val finalized = state.messages.mapNotNull { msg ->
            when {
              msg.id == modelPlaceholder.id && msg.text.isBlank() -> null
              msg.id == modelPlaceholder.id -> msg.copy(isStreaming = false)
              else -> msg
            }
          }
          GeminiChatStorage.saveMessages(getApplication(), resolvedId, finalized)
          state.copy(messages = finalized, isStreaming = false)
        }
        throw e
      } catch (e: Throwable) {
        val friendly = if (SupabaseConfig.isNetworkError(e)) {
          "Connection interrupted. Check your internet and try again."
        } else {
          e.message?.takeIf { it.isNotBlank() } ?: "Could not get a response from Gemini."
        }
        _uiState.update { state ->
          val finalized = state.messages.mapNotNull { msg ->
            when {
              msg.id == modelPlaceholder.id && msg.text.isBlank() -> null
              msg.id == modelPlaceholder.id -> msg.copy(isStreaming = false)
              else -> msg
            }
          }
          GeminiChatStorage.saveMessages(getApplication(), resolvedId, finalized)
          state.copy(
            messages = finalized,
            isStreaming = false,
            errorMessage = friendly
          )
        }
      } finally {
        streamJob = null
      }
    }
  }

  fun clearOnLogout() {
    streamJob?.cancel()
    streamJob = null
    loadedUserId = null
    GeminiChatStorage.clearOnLogout(getApplication())
    _uiState.value = GeminiChatUiState(isConfigured = GeminiConfig.isConfigured())
  }
}
