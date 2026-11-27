package com.example.assistant

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.ui.theme.ClassRepTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiChatScreenTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun screen_rendersMarkdownBlocksCopyRegenerateAndStopControls() {
    var regenerated = false
    var stopped = false

    val sampleMarkdown = """
      ## Newton's Second Law
      Step 1: Relate force and acceleration
      
      $$ F = ma $$
      
      | Quantity | Symbol | Unit |
      | :--- | :---: | ---: |
      | Force | F | N |
      
      ```python
      force = mass * acceleration
      ```
    """.trimIndent()

    val uiState = GeminiChatUiState(
      messages = listOf(
        GeminiMessage(id = "u1", role = ChatRole.USER, text = "Explain Newton's second law"),
        GeminiMessage(id = "m1", role = ChatRole.MODEL, text = sampleMarkdown, isStreaming = false)
      ),
      isStreaming = false,
      isConfigured = true
    )

    composeTestRule.setContent {
      ClassRepTheme {
        GeminiChatScreen(
          uiState = uiState,
          userId = "user1",
          isOnline = true,
          onEnterChat = {},
          onInputChanged = {},
          onSendMessage = {},
          onStopStreaming = { stopped = true },
          onRegenerateResponse = { regenerated = true },
          onRetryLast = {},
          onNewChat = {},
          onDismissError = {},
          onBack = {}
        )
      }
    }

    composeTestRule.onNodeWithText("Newton's Second Law", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Step 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("EQUATION").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("PYTHON").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("gemini_copy_button_m1").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.onNodeWithTag("gemini_regenerate_button").performScrollTo().assertIsDisplayed().performClick()
    assertTrue(regenerated)
    assertTrue(!stopped)
  }

  @Test
  fun screen_showsStopButtonAndThinkingWhileStreaming() {
    var stopped = false
    val uiState = GeminiChatUiState(
      messages = listOf(
        GeminiMessage(id = "u1", role = ChatRole.USER, text = "Hello"),
        GeminiMessage(id = "m1", role = ChatRole.MODEL, text = "", isStreaming = true)
      ),
      isStreaming = true,
      isConfigured = true
    )

    composeTestRule.setContent {
      ClassRepTheme {
        GeminiChatScreen(
          uiState = uiState,
          userId = "user1",
          isOnline = true,
          onEnterChat = {},
          onInputChanged = {},
          onSendMessage = {},
          onStopStreaming = { stopped = true },
          onRegenerateResponse = {},
          onRetryLast = {},
          onNewChat = {},
          onDismissError = {},
          onBack = {}
        )
      }
    }

    composeTestRule.onNodeWithText("Thinking...").assertIsDisplayed()
    composeTestRule.onNodeWithTag("gemini_stop_button").assertIsDisplayed().performClick()
    assertTrue(stopped)
  }
}
