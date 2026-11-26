package com.example.assistant

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiChatServiceTest {

  @Test
  fun sanitizeApiKey_rejectsBlankAndPlaceholder() {
    assertEquals("", GeminiConfig.sanitizeApiKey(null))
    assertEquals("", GeminiConfig.sanitizeApiKey(""))
    assertEquals("", GeminiConfig.sanitizeApiKey("   "))
    assertEquals("", GeminiConfig.sanitizeApiKey(GeminiConfig.DEFAULT_API_KEY_PLACEHOLDER))
    assertEquals("", GeminiConfig.sanitizeApiKey("\"${GeminiConfig.DEFAULT_API_KEY_PLACEHOLDER}\""))
    assertEquals("AIzaTestKey123", GeminiConfig.sanitizeApiKey("  \"AIzaTestKey123\"  "))
  }

  @Test
  fun buildRequestBody_includesSystemInstructionAndRecentTurns() {
    val history = (1..25).map { idx ->
      GeminiMessage(
        id = "m_$idx",
        role = if (idx % 2 == 1) ChatRole.USER else ChatRole.MODEL,
        text = "Message $idx",
        isStreaming = false
      )
    }

    val payload = GeminiChatService.buildRequestBody(history, "New question")
    val sysParts = payload.getJSONObject("systemInstruction").getJSONArray("parts")
    assertTrue(sysParts.getJSONObject(0).getString("text").contains("students and lecturers"))

    val contents = payload.getJSONArray("contents")
    // 20 history messages + 1 new user prompt
    assertEquals(21, contents.length())
    assertEquals("Message 6", contents.getJSONObject(0).getJSONArray("parts").getJSONObject(0).getString("text"))
    assertEquals("user", contents.getJSONObject(20).getString("role"))
    assertEquals("New question", contents.getJSONObject(20).getJSONArray("parts").getJSONObject(0).getString("text"))
  }

  @Test
  fun extractChunkText_parsesTextAndSkipsThoughtParts() {
    val json = """
      {
        "candidates": [
          {
            "content": {
              "role": "model",
              "parts": [
                {"thought": true, "text": "Internal reasoning"},
                {"text": "Hello "},
                {"text": "world!"}
              ]
            }
          }
        ]
      }
    """.trimIndent()

    assertEquals("Hello world!", GeminiChatService.extractChunkText(json))
    assertEquals("", GeminiChatService.extractChunkText("{}"))
    assertEquals("", GeminiChatService.extractChunkText("not-json"))
  }

  @Test
  fun extractApiErrorMessage_handlesAuthQuotaAndCustomErrors() {
    val authErr = GeminiChatService.extractApiErrorMessage(
      """{"error":{"code":403,"message":"API key not valid"}}""",
      403
    )
    assertTrue(authErr.contains("GEMINI_API_KEY"))

    val quotaErr = GeminiChatService.extractApiErrorMessage(
      """{"error":{"code":429,"message":"Resource exhausted"}}""",
      429
    )
    assertTrue(quotaErr.contains("rate limit", ignoreCase = true))
  }

  @Test
  fun storage_savesLoadsAndClearsOnLogout() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    GeminiChatStorage.clearOnLogout(context)

    val messages = listOf(
      GeminiMessage(id = "1", role = ChatRole.USER, text = "Explain photosynthesis"),
      GeminiMessage(id = "2", role = ChatRole.MODEL, text = "Photosynthesis converts light energy..."),
      GeminiMessage(id = "3", role = ChatRole.MODEL, text = "", isStreaming = true)
    )
    GeminiChatStorage.saveMessages(context, "user-42", messages)

    val loaded = GeminiChatStorage.loadMessages(context, "user-42")
    assertEquals(2, loaded.size)
    assertEquals(ChatRole.USER, loaded[0].role)
    assertEquals("Explain photosynthesis", loaded[0].text)
    assertEquals(ChatRole.MODEL, loaded[1].role)
    assertFalse(loaded[1].isStreaming)

    GeminiChatStorage.clearOnLogout(context)
    assertTrue(GeminiChatStorage.loadMessages(context, "user-42").isEmpty())
  }

  @Test
  fun starterPrompts_areGeneralAndReady() {
    assertEquals(4, GeminiStarterPrompts.items.size)
    assertTrue(GeminiStarterPrompts.items.all { it.label.isNotBlank() && it.prompt.isNotBlank() })
  }
}
