package com.example.assistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class ChatRole {
  USER,
  MODEL
}

data class GeminiMessage(
  val id: String = UUID.randomUUID().toString(),
  val role: ChatRole,
  val text: String,
  val timestampMs: Long = System.currentTimeMillis(),
  val isStreaming: Boolean = false
)

data class GeminiStarterPrompt(
  val label: String,
  val prompt: String
)

object GeminiStarterPrompts {
  val items: List<GeminiStarterPrompt> = listOf(
    GeminiStarterPrompt(
      label = "Explain a topic",
      prompt = "Explain a complex topic clearly using a simple real-world example and key takeaways. Ask me which topic I want to explore."
    ),
    GeminiStarterPrompt(
      label = "Summarize notes",
      prompt = "I will paste my lecture notes next. Please organize them into a concise summary with key points and definitions."
    ),
    GeminiStarterPrompt(
      label = "Draft announcement",
      prompt = "Help me draft a clear, polite class announcement. Ask me what details to include."
    ),
    GeminiStarterPrompt(
      label = "Practice quiz",
      prompt = "Ask me 5 short practice questions on any subject I choose, one step at a time, and explain each answer."
    )
  )
}

data class GeminiChatUiState(
  val messages: List<GeminiMessage> = emptyList(),
  val inputText: String = "",
  val isStreaming: Boolean = false,
  val errorMessage: String? = null,
  val lastFailedPrompt: String? = null,
  val isConfigured: Boolean = GeminiConfig.isConfigured()
) {
  val canRegenerate: Boolean
    get() = !isStreaming && messages.any { it.role == ChatRole.USER && it.text.isNotBlank() }
}

object GeminiChatStorage {
  private const val PREFS_NAME = "gemini_chat_prefs"
  private const val MAX_STORED_MESSAGES = 40

  private fun prefs(context: Context) =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private fun messagesKey(userId: String) = "messages_${userId.ifBlank { "guest" }}"

  fun loadMessages(context: Context, userId: String): List<GeminiMessage> {
    val raw = prefs(context).getString(messagesKey(userId), null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val text = obj.optString("text", "")
          if (text.isBlank()) continue
          val role = if (obj.optString("role").equals("USER", ignoreCase = true)) {
            ChatRole.USER
          } else {
            ChatRole.MODEL
          }
          add(
            GeminiMessage(
              id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
              role = role,
              text = text,
              timestampMs = obj.optLong("timestampMs", System.currentTimeMillis()),
              isStreaming = false
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  fun saveMessages(context: Context, userId: String, messages: List<GeminiMessage>) {
    val finalized = messages
      .filter { !it.isStreaming && it.text.isNotBlank() }
      .takeLast(MAX_STORED_MESSAGES)
    val arr = JSONArray()
    for (msg in finalized) {
      arr.put(
        JSONObject().apply {
          put("id", msg.id)
          put("role", msg.role.name)
          put("text", msg.text)
          put("timestampMs", msg.timestampMs)
        }
      )
    }
    prefs(context).edit().putString(messagesKey(userId), arr.toString()).apply()
  }

  fun clearMessages(context: Context, userId: String) {
    prefs(context).edit().remove(messagesKey(userId)).apply()
  }

  fun clearOnLogout(context: Context) {
    prefs(context).edit().clear().apply()
  }
}
