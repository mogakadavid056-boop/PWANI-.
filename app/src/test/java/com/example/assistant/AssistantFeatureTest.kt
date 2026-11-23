package com.example.assistant

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.ClassTab
import com.example.ui.screens.MoreSubView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AssistantFeatureTest {

  @Test
  fun buildChatRequestBody_setsAppMainAndCapsMessagesAtTen() {
    val msgs = (1..15).map { i ->
      AssistantMessage(
        id = "m$i",
        role = if (i % 2 == 1) AssistantRole.USER else AssistantRole.ASSISTANT,
        content = "Message $i"
      )
    }
    val json = AssistantApi.buildChatRequestBody(
      messages = msgs,
      context = AssistantContext(app = "main", screen = "class_detail", classId = "cls-42")
    )
    val sentMsgs = json.getJSONArray("messages")
    assertEquals(10, sentMsgs.length())
    assertEquals("Message 6", sentMsgs.getJSONObject(0).getString("text"))
    assertEquals("Message 15", sentMsgs.getJSONObject(9).getString("text"))
    assertFalse(sentMsgs.getJSONObject(0).has("content"))

    val ctx = json.getJSONObject("context")
    assertEquals("main", ctx.getString("app"))
    assertEquals("class_detail", ctx.getString("screen"))
    assertEquals("cls-42", ctx.getString("class_id"))
  }

  @Test
  fun parseChatResponse_filtersToAllowedMainToolsAndParsesConsent() {
    val raw = """
      {
        "reply": "Here is your update.",
        "actions": [
          {"id":"a1","tool":"propose_post_notice","summary":"Post exam reminder","args":{"title":"Exam","content":"Starts 8am"}},
          {"id":"a2","tool":"delete_notice","summary":"Delete","args":{}},
          {"id":"a3","tool":"verify_lecturer","summary":"Verify","args":{}},
          {"id":"a4","tool":"open_screen","summary":"Open timetable","args":{"screen":"timetable","class_id":"c9"}}
        ]
      }
    """.trimIndent()
    val parsed = AssistantApi.parseChatResponse(raw)
    assertEquals("Here is your update.", parsed.reply)
    assertEquals(2, parsed.actions.size)
    assertEquals("propose_post_notice", parsed.actions[0].tool)
    assertEquals("open_screen", parsed.actions[1].tool)

    assertTrue(AssistantApi.parseConsentResponse("true"))
    assertFalse(AssistantApi.parseConsentResponse("false"))
    assertTrue(AssistantApi.parseConsentResponse("""{"has_ai_consent":true}"""))
    assertFalse(AssistantApi.parseConsentResponse("""{"accepted":false}"""))
  }

  @Test
  fun storage_capsAtThirtyMessagesAndClearsOnLogout() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AssistantStorage.clearOnLogout(context)

    val msgs = (1..36).map { i ->
      AssistantMessage(id = "id_$i", role = AssistantRole.USER, content = "Msg $i")
    }
    AssistantStorage.setLocalConsent(context, "user-1", true)
    AssistantStorage.saveMessages(context, "user-1", msgs)

    assertTrue(AssistantStorage.hasLocalConsent(context, "user-1"))
    val loaded = AssistantStorage.loadMessages(context, "user-1")
    assertEquals(30, loaded.size)
    assertEquals("Msg 7", loaded.first().content)
    assertEquals("Msg 36", loaded.last().content)

    AssistantStorage.clearOnLogout(context)
    assertFalse(AssistantStorage.hasLocalConsent(context, "user-1"))
    assertTrue(AssistantStorage.loadMessages(context, "user-1").isEmpty())
  }

  @Test
  fun safeMarkdownParser_formatsMarkdownAndStripsHtmlScriptsAndLinks() {
    val md = """
      # Today's Plan
      - Check **Timetable** now
      1. Attend **Lecture 1**
      <script>alert('xss')</script><b>Stay safe</b> javascript:void(0) https://example.com
    """.trimIndent()

    val blocks = SafeMarkdownParser.parseBlocks(md)
    assertEquals(4, blocks.size)
    assertTrue(blocks[0] is MarkdownBlock.Paragraph)
    assertTrue(blocks[1] is MarkdownBlock.Bullet)
    assertTrue(blocks[2] is MarkdownBlock.Numbered)

    val sanitized = (blocks[3] as MarkdownBlock.Paragraph).text
    assertFalse(sanitized.contains("<script>"))
    assertFalse(sanitized.contains("<"))
    assertFalse(sanitized.contains("javascript:", ignoreCase = true))
    assertTrue(sanitized.contains("https://example.com"))

    val styled = SafeMarkdownParser.parseBoldInline("**Timetable** update", Color.Yellow)
    assertEquals("Timetable update", styled.text)
    assertTrue(styled.getStringAnnotations(0, styled.length).isEmpty())
  }

  @Test
  fun suggestionsAndExecutorHelpers_matchRoleAndAllowedDestinations() {
    val studentChips = AssistantSuggestions.forMainRole("student", isRepInClass = false).map { it.label }
    assertTrue(studentChips.contains("How is my attendance?"))
    assertTrue(studentChips.contains("How many points do I have?"))
    assertFalse(studentChips.contains("Draft a notice about..."))

    val prefectChips = AssistantSuggestions.forMainRole("student", isRepInClass = true).map { it.label }
    assertTrue(prefectChips.contains("Draft a notice about..."))
    assertTrue(prefectChips.contains("Create a poll about..."))
    assertFalse(prefectChips.contains("How is attendance in my class?"))

    val lecturerChips = AssistantSuggestions.forMainRole("lecturer", isRepInClass = false).map { it.label }
    assertTrue(lecturerChips.contains("How is attendance in my class?"))
    assertTrue(lecturerChips.contains("Make a 5 question quiz on..."))

    val dest = ActionExecutor.mapOpenScreenDestination("polls", "cls-1") as? MainNavigationTarget.ClassDestination
    assertNotNull(dest)
    assertEquals(ClassTab.More, dest?.tab)
    assertEquals(MoreSubView.Polls, dest?.moreSubView)
    assertNull(ActionExecutor.mapOpenScreenDestination("admin_panel", "cls-1"))

    val prefill = ActionExecutor.parseChallengeDraftPrefill(
      mapOf(
        "title" to "Surveying Quiz",
        "questions" to listOf(
          mapOf("question" to "What is GPS?", "options" to listOf("Satellite nav", "Compass"), "correct_index" to 0)
        )
      ),
      classId = "cls-1"
    )
    assertEquals("Surveying Quiz", prefill.title)
    assertEquals(1, prefill.questions.size)
    assertEquals(4, prefill.questions[0].options.size)
  }

  @Test
  fun parseChatResponse_handlesAlternativeShapesErrorsAndNeverShowsBlankDone() {
    val nested = AssistantApi.parseChatResponse(
      """{"data":{"message":{"content":"Your attendance is 92%."},"proposed_actions":[{"name":"open_screen","arguments":"{\"screen\":\"attendance\"}"}]}}"""
    )
    assertEquals("Your attendance is 92%.", nested.reply)
    assertEquals(1, nested.actions.size)
    assertEquals("open_screen", nested.actions[0].tool)
    assertEquals("attendance", nested.actions[0].args["screen"])

    val gemini = AssistantApi.parseChatResponse(
      """{"candidates":[{"content":{"parts":[{"text":"internal","thought":true},{"text":"Class starts at 8:00 AM."}]}}]}"""
    )
    assertEquals("Class starts at 8:00 AM.", gemini.reply)

    val actionOnly = AssistantApi.parseChatResponse(
      """{"reply":null,"actions":[{"tool":"propose_post_notice","summary":"Post update","args":{"title":"T","content":"C"}}]}"""
    )
    assertTrue(actionOnly.reply.isNotBlank() && actionOnly.reply != "Done." && actionOnly.reply != "null")
    assertEquals(1, actionOnly.actions.size)

    val errThrown = runCatching {
      AssistantApi.parseChatResponse("""{"error":"Missing GEMINI_API_KEY secret"}""")
    }.exceptionOrNull()
    assertNotNull(errThrown)
    assertTrue(errThrown!!.message!!.contains("GEMINI_API_KEY"))

    val emptyThrown = runCatching {
      AssistantApi.parseChatResponse("""{"reply":""}""")
    }.exceptionOrNull()
    assertNotNull(emptyThrown)
    assertFalse(emptyThrown!!.message == "Done.")

    val templateThrown = runCatching {
      AssistantApi.parseChatResponse("""{"message":"Hello undefined!"}""")
    }.exceptionOrNull()
    assertNotNull(templateThrown)
    assertTrue(templateThrown!!.message!!.contains("starter template"))

    val undefinedThrown = runCatching {
      AssistantApi.parseChatResponse("""{"reply":"undefined"}""")
    }.exceptionOrNull()
    assertNotNull(undefinedThrown)

    val mathText = SafeMarkdownParser.sanitizeText("Attendance < 75% and points > 50")
    assertTrue(mathText.contains("75% and points"))

    val codeBlocks = SafeMarkdownParser.parseBlocks(
      """
      Here is an example:
      ```python
      for i in range(1, 6):
          print(i)
      ```
      """.trimIndent()
    )
    val code = codeBlocks.filterIsInstance<MarkdownBlock.CodeBlock>().firstOrNull()
    assertNotNull(code)
    assertEquals("python", code?.language)
    assertTrue(code?.code?.contains("    print(i)") == true)

    assertEquals(MainNavigationTarget.Home, ActionExecutor.mapOpenScreenDestination("classes", null))
    assertEquals(MainNavigationTarget.Home, ActionExecutor.mapOpenScreenDestination("dashboard", null))
    assertEquals(
      48,
      ActionExecutor.intArg(mapOf("hours_open" to 48), "hours_open", "duration_hours", "hours", defaultValue = 24)
    )
    val fiveOptDraft = ActionExecutor.parseChallengeDraftPrefill(
      mapOf(
        "title" to "5 Options",
        "questions" to listOf(
          mapOf("question" to "Q", "options" to listOf("A", "B", "C", "D", "E"), "correct_index" to 4)
        )
      ),
      classId = "cls-1"
    )
    assertEquals(5, fiveOptDraft.questions[0].options.size)
    assertEquals(4, fiveOptDraft.questions[0].correctIndex)
  }

  @Test
  fun firebaseAssistant_parsesStructuredJsonAndEnforcesRoleActionGating() {
    val raw = """
      {
        "reply": "Today you have **Algorithms** at 08:00 in LH3.",
        "actions": [
          {"tool":"open_screen","summary":"Open Timetable","screen":"timetable"},
          {"tool":"propose_post_notice","summary":"Post exam reminder","title":"Exam","content":"Starts 8am","category":"exam","urgent":true},
          {"tool":"propose_create_poll","summary":"Create poll","question":"Reschedule?","options":["Yes","No"],"hours_open":12}
        ]
      }
    """.trimIndent()

    val studentParsed = FirebaseAssistantService.parseFirebaseResponse(
      jsonString = raw,
      allowStaffProposals = false,
      fallbackClassId = "cls-9"
    )
    assertEquals("Today you have **Algorithms** at 08:00 in LH3.", studentParsed.reply)
    assertEquals(1, studentParsed.actions.size)
    assertEquals("open_screen", studentParsed.actions[0].tool)
    assertEquals("cls-9", studentParsed.actions[0].args["class_id"])

    val staffParsed = FirebaseAssistantService.parseFirebaseResponse(
      jsonString = raw,
      allowStaffProposals = true,
      fallbackClassId = "cls-9"
    )
    assertEquals(3, staffParsed.actions.size)
    assertEquals("propose_post_notice", staffParsed.actions[1].tool)
    assertEquals("propose_create_poll", staffParsed.actions[2].tool)
    assertEquals(12, staffParsed.actions[2].args["hours_open"])

    val prompt = FirebaseAssistantService.buildTurnPrompt(
      userMessage = "What classes do I have today?",
      liveContextText = "Active class: BSc CS (id=cls-9)",
      canDraftStaffTools = false
    )
    assertTrue(prompt.contains("can_draft_staff_tools: false"))
    assertTrue(prompt.contains("What classes do I have today?"))
  }
}
