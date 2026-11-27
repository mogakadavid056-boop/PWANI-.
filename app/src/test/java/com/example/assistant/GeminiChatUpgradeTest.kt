package com.example.assistant

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiChatUpgradeTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUpDispatcher() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDownDispatcher() {
    Dispatchers.resetMain()
  }

  @Test
  fun parser_handlesHeadingsTablesEquationsStepsCodeAndDividers() {
    val markdown = """
      # Quadratic Solver Guide
      Here is how to solve $ ax^2 + bx + c = 0 $ when `a != 0` and x < 10.
      
      ## Step-by-Step Method
      Step 1: Compute the Discriminant
      1. Identify coefficients `a`, `b`, and `c`.
         - Ensure `a` is non-zero.
      
      $$
      x = \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}
      $$
      
      | Case | Condition | Roots |
      | :--- | :---: | ---: |
      | Two Real | \Delta > 0 | 2 |
      | Repeated | \Delta = 0 | 1 |
      
      > Always verify your roots by substitution.
      
      ---
      
      ```kotlin
      fun discriminant(a: Double, b: Double, c: Double) = b * b - 4 * a * c
      ```
    """.trimIndent()

    val blocks = SafeMarkdownParser.parseBlocks(markdown)

    val h1 = blocks.filterIsInstance<MarkdownBlock.Heading>().first { it.level == 1 }
    assertEquals("Quadratic Solver Guide", h1.text)
    assertNotNull(h1.sectionCopyText)
    assertTrue(h1.sectionCopyText!!.contains("Step 1"))

    val step = blocks.filterIsInstance<MarkdownBlock.StepBlock>().first()
    assertEquals("1", step.stepNumber)
    assertEquals("Compute the Discriminant", step.title)

    val bullet = blocks.filterIsInstance<MarkdownBlock.Bullet>().first()
    assertTrue(bullet.indentLevel >= 1)

    val eq = blocks.filterIsInstance<MarkdownBlock.EquationBlock>().first()
    assertTrue(eq.rawLatex.contains("\\frac"))
    assertTrue(eq.formattedMath.contains("±"))
    assertTrue(eq.formattedMath.contains("√"))

    val table = blocks.filterIsInstance<MarkdownBlock.Table>().first()
    assertEquals(listOf("Case", "Condition", "Roots"), table.headers)
    assertEquals(listOf(TableAlignment.LEFT, TableAlignment.CENTER, TableAlignment.RIGHT), table.alignments)
    assertEquals(2, table.rows.size)
    assertTrue(table.rawMarkdown.contains("Two Real"))

    val quote = blocks.filterIsInstance<MarkdownBlock.Blockquote>().first()
    assertEquals("Always verify your roots by substitution.", quote.rawText)

    assertTrue(blocks.any { it is MarkdownBlock.Divider })

    val code = blocks.filterIsInstance<MarkdownBlock.CodeBlock>().first()
    assertEquals("kotlin", code.language)
    assertTrue(code.code.contains("fun discriminant"))
  }

  @Test
  fun sanitizer_stripsHtmlTagsWhilePreservingMathComparisons() {
    val cleaned = SafeMarkdownParser.sanitizeText("Check <script>alert(1)</script>x < y and a >= b")
    assertFalse(cleaned.contains("<script>"))
    assertTrue(cleaned.contains("x < y"))
    assertTrue(cleaned.contains("a >= b"))
  }

  @Test
  fun inlineParser_preservesSnakeCaseAndFormatsMathAndCode() {
    val formatted = SafeMarkdownParser.parseInlineMarkdown(
      text = "Use `my_var_name` with **bold** and *italic* for \$x^2 + \\alpha\$",
      boldColor = Color.Yellow,
      codeColor = Color.Yellow,
      codeBgColor = Color.Black,
      linkColor = Color.Cyan,
      mathColor = Color.White
    )
    val plain = formatted.text
    assertTrue(plain.contains("my_var_name"))
    assertTrue(plain.contains("bold"))
    assertTrue(plain.contains("italic"))
    assertTrue(plain.contains("x²"))
    assertTrue(plain.contains("α"))
  }

  @Test
  fun viewModel_sendRegenerateStopAndRetryWorkCleanly() = runTest(testDispatcher) {
    val app = ApplicationProvider.getApplicationContext<Application>()
    GeminiChatStorage.clearOnLogout(app)

    val vm = GeminiAssistantViewModel(app).apply {
      isConfiguredProvider = { true }
    }

    var callCount = 0
    vm.streamProvider = { _, prompt ->
      callCount++
      flow {
        emit("Answer #$callCount for: ")
        delay(25)
        emit(prompt)
      }
    }

    vm.sendMessage("Explain entropy", userId = "u1", isOnline = true)
    advanceUntilIdle()

    val afterFirst = vm.uiState.value
    assertFalse(afterFirst.isStreaming)
    assertEquals(2, afterFirst.messages.size)
    assertEquals(ChatRole.USER, afterFirst.messages[0].role)
    assertEquals("Answer #1 for: Explain entropy", afterFirst.messages[1].text)
    assertTrue(afterFirst.canRegenerate)

    // Regenerate replaces model reply without duplicating user turn
    vm.regenerateLastResponse(userId = "u1", isOnline = true)
    advanceUntilIdle()

    val afterRegen = vm.uiState.value
    assertFalse(afterRegen.isStreaming)
    assertEquals(2, afterRegen.messages.size)
    assertEquals("Explain entropy", afterRegen.messages[0].text)
    assertEquals("Answer #2 for: Explain entropy", afterRegen.messages[1].text)

    // Error handling sets errorMessage and preserves retry capability
    vm.streamProvider = { _, _ ->
      flow {
        throw IllegalStateException("Temporary upstream error")
      }
    }
    vm.sendMessage("Second question", userId = "u1", isOnline = true)
    advanceUntilIdle()

    val afterError = vm.uiState.value
    assertFalse(afterError.isStreaming)
    assertEquals("Temporary upstream error", afterError.errorMessage)
    assertEquals("Second question", afterError.lastFailedPrompt)

    // Retry recovers cleanly without duplicating user turn
    vm.streamProvider = { _, prompt ->
      flow { emit("Recovered: $prompt") }
    }
    vm.retryLastError(userId = "u1", isOnline = true)
    advanceUntilIdle()

    val afterRetry = vm.uiState.value
    assertNull(afterRetry.errorMessage)
    assertNull(afterRetry.lastFailedPrompt)
    assertEquals(4, afterRetry.messages.size)
    assertEquals("Recovered: Second question", afterRetry.messages.last().text)
  }

  @Test
  fun viewModel_stopStreamingPreservesPartialReply() = runTest(testDispatcher) {
    val app = ApplicationProvider.getApplicationContext<Application>()
    GeminiChatStorage.clearOnLogout(app)

    val vm = GeminiAssistantViewModel(app).apply {
      isConfiguredProvider = { true }
      streamProvider = { _, _ ->
        flow {
          emit("First part ")
          delay(2000)
          emit("Second part")
        }
      }
    }

    vm.sendMessage("Long explanation", userId = "u2", isOnline = true)
    advanceTimeBy(100)
    vm.stopStreaming()
    advanceUntilIdle()

    val state = vm.uiState.value
    assertFalse(state.isStreaming)
    assertEquals(2, state.messages.size)
    assertEquals("First part ", state.messages[1].text)
    assertFalse(state.messages[1].isStreaming)
  }
}
