package com.example.assistant

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
  fun buildRequestTargets_prioritizesDeveloperEndpointWithVertexFallback() {
    val aqTargets = GeminiChatService.buildRequestTargets("AQ.Ab8RNTestKey")
    assertFalse(aqTargets.first().isVertexExpress)
    assertTrue(aqTargets.first().url.startsWith("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite"))
    assertEquals("AQ.Ab8RNTestKey", aqTargets.first().headerApiKey)
    assertTrue(aqTargets.any { it.isVertexExpress && it.headerApiKey == null })

    val devTargets = GeminiChatService.buildRequestTargets("AIzaTestKey123")
    assertFalse(devTargets.first().isVertexExpress)
    assertEquals("AIzaTestKey123", devTargets.first().headerApiKey)
    assertNull(devTargets.last().headerApiKey)
  }

  @Test
  fun shouldTryNextTarget_retriesOnOverload503Quota429And404() {
    val dev = GeminiRequestTarget("https://generativelanguage.googleapis.com/a", "key", isVertexExpress = false)
    val dev2 = GeminiRequestTarget("https://generativelanguage.googleapis.com/b", "key", isVertexExpress = false)
    val vertex = GeminiRequestTarget("https://aiplatform.googleapis.com/v", null, isVertexExpress = true)

    assertTrue(GeminiChatService.shouldTryNextTarget(503, dev, listOf(dev2, vertex)))
    assertTrue(GeminiChatService.shouldTryNextTarget(429, dev, listOf(dev2, vertex)))
    assertTrue(GeminiChatService.shouldTryNextTarget(404, dev, listOf(dev2, vertex)))
    assertTrue(GeminiChatService.shouldTryNextTarget(403, dev, listOf(dev2, vertex)))
    assertFalse(GeminiChatService.shouldTryNextTarget(403, dev, listOf(dev2)))
    assertFalse(GeminiChatService.shouldTryNextTarget(503, dev, emptyList()))
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
    val sysObj = payload.getJSONObject("systemInstruction")
    assertEquals("system", sysObj.getString("role"))
    val sysParts = sysObj.getJSONArray("parts")
    assertTrue(sysParts.getJSONObject(0).getString("text").contains("students and lecturers"))

    val contents = payload.getJSONArray("contents")
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
    assertTrue(GeminiChatServiceTest_isEmptyAfterLogout(context))
  }

  private fun GeminiChatServiceTest_isEmptyAfterLogout(context: Application): Boolean =
    GeminiChatStorage.loadMessages(context, "user-42").isEmpty()

  @Test
  fun starterPrompts_areGeneralAndReady() {
    assertEquals(4, GeminiStarterPrompts.items.size)
    assertTrue(GeminiStarterPrompts.items.all { it.label.isNotBlank() && it.prompt.isNotBlank() })
  }
}
