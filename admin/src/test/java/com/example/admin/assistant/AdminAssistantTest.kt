package com.example.admin.assistant

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.admin.ui.AdminTab
import com.example.admin.ui.ApprovalsSubTab
import com.example.admin.ui.MonitorTab
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
class AdminAssistantTest {

  @Test
  fun buildChatRequestBody_setsAppAdminAndCapsAtTenMessages() {
    val msgs = (1..14).map { i ->
      AdminAssistantMessage(
        id = "am_$i",
        role = if (i % 2 == 1) AdminAssistantRole.USER else AdminAssistantRole.ASSISTANT,
        content = "Admin msg $i"
      )
    }
    val json = AdminAssistantApi.buildChatRequestBody(
      messages = msgs,
      context = AdminAssistantContext(app = "admin", screen = "overview", classId = null)
    )
    val sent = json.getJSONArray("messages")
    assertEquals(10, sent.length())
    assertEquals("Admin msg 5", sent.getJSONObject(0).getString("text"))
    assertEquals("Admin msg 14", sent.getJSONObject(9).getString("text"))
    assertFalse(sent.getJSONObject(0).has("content"))

    val ctx = json.getJSONObject("context")
    assertEquals("admin", ctx.getString("app"))
    assertEquals("overview", ctx.getString("screen"))
    assertTrue(ctx.isNull("class_id"))
  }

  @Test
  fun parseChatResponse_strictlyAllowsOnlyOpenScreenAndPostNotice() {
    val raw = """
      {
        "reply": "**2** unverified lecturers found.",
        "actions": [
          {"id":"1","tool":"open_screen","summary":"Review unverified lecturers","args":{"screen":"unverified_lecturers"}},
          {"id":"2","tool":"propose_post_notice","summary":"Post notice","args":{"title":"Alert","content":"Meeting"}},
          {"id":"3","tool":"propose_create_poll","summary":"Create poll","args":{"question":"Q"}},
          {"id":"4","tool":"propose_challenge_draft","summary":"Quiz","args":{}},
          {"id":"5","tool":"verify_lecturer","summary":"Verify","args":{}}
        ]
      }
    """.trimIndent()
    val parsed = AdminAssistantApi.parseChatResponse(raw)
    assertEquals(2, parsed.actions.size)
    assertEquals("open_screen", parsed.actions[0].tool)
    assertEquals("propose_post_notice", parsed.actions[1].tool)

    assertTrue(AdminActionExecutor.isSupportedAdminTool("open_screen"))
    assertTrue(AdminActionExecutor.isSupportedAdminTool("propose_post_notice"))
    assertFalse(AdminActionExecutor.isSupportedAdminTool("propose_create_poll"))
    assertFalse(AdminActionExecutor.isSupportedAdminTool("delete_user"))
  }

  @Test
  fun storage_capsAtThirtyMessagesAndClearsOnLogout() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AdminAssistantStorage.clearOnLogout(context)

    val msgs = (1..35).map { i ->
      AdminAssistantMessage(id = "aid_$i", role = AdminAssistantRole.USER, content = "Note $i")
    }
    AdminAssistantStorage.setLocalConsent(context, "admin-1", true)
    AdminAssistantStorage.saveMessages(context, "admin-1", msgs)

    assertTrue(AdminAssistantStorage.hasLocalConsent(context, "admin-1"))
    val loaded = AdminAssistantStorage.loadMessages(context, "admin-1")
    assertEquals(30, loaded.size)
    assertEquals("Note 6", loaded.first().content)
    assertEquals("Note 35", loaded.last().content)

    AdminAssistantStorage.clearOnLogout(context)
    assertFalse(AdminAssistantStorage.hasLocalConsent(context, "admin-1"))
    assertTrue(AdminAssistantStorage.loadMessages(context, "admin-1").isEmpty())
  }

  @Test
  fun adminMarkdownAndSuggestions_workSafely() {
    val chipLabels = AdminAssistantSuggestions.chips.map { it.label }
    assertEquals(
      listOf("Unverified lecturers", "Classes without a prefect", "System summary", "Students without a class"),
      chipLabels
    )

    val blocks = AdminSafeMarkdownParser.parseBlocks("- **Classes**: 4\n1. Action needed\n<img src=x>javascript:alert(1)")
    assertEquals(3, blocks.size)
    val sanitized = (blocks[2] as AdminMarkdownBlock.Paragraph).text
    assertFalse(sanitized.contains("<"))
    assertFalse(sanitized.contains("javascript:", ignoreCase = true))

    val bold = AdminSafeMarkdownParser.parseBoldInline("**System** summary", Color.Yellow)
    assertEquals("System summary", bold.text)
    assertTrue(bold.getStringAnnotations(0, bold.length).isEmpty())
  }

  @Test
  fun adminActionExecutor_mapsAllowedDestinationsOnly() {
    assertEquals(
      AdminNavigationTarget.Tab(AdminTab.OVERVIEW),
      AdminActionExecutor.mapOpenScreenDestination("dashboard", null)
    )
    assertEquals(
      AdminNavigationTarget.ApprovalsSubTabTarget(ApprovalsSubTab.LECTURERS),
      AdminActionExecutor.mapOpenScreenDestination("unverified_lecturers", null)
    )
    assertEquals(
      AdminNavigationTarget.ClassDetailTab("cls-8", MonitorTab.ATTENDANCE),
      AdminActionExecutor.mapOpenScreenDestination("attendance", "cls-8")
    )
    assertNull(AdminActionExecutor.mapOpenScreenDestination("delete_everything", null))
  }

  @Test
  fun parseChatResponse_handlesAlternativeShapesErrorsAndRejectsBlankDone() {
    val alt = AdminAssistantApi.parseChatResponse(
      """{"data":{"answer":"3 classes have no prefect.","proposed_actions":[{"function":{"name":"open_screen","arguments":{"screen":"classes"}}},{"name":"propose_create_poll","arguments":{}}]}}"""
    )
    assertEquals("3 classes have no prefect.", alt.reply)
    assertEquals(1, alt.actions.size)
    assertEquals("open_screen", alt.actions[0].tool)

    val err = runCatching {
      AdminAssistantApi.parseChatResponse("""{"ok":false,"message":"Daily limit reached"}""")
    }.exceptionOrNull()
    assertTrue(err?.message?.contains("Daily limit reached") == true)

    val blankErr = runCatching {
      AdminAssistantApi.parseChatResponse("""{"reply":""}""")
    }.exceptionOrNull()
    assertTrue(blankErr != null && blankErr.message != "Done.")

    val starterErr = runCatching {
      AdminAssistantApi.parseChatResponse("""{"message":"Hello undefined!"}""")
    }.exceptionOrNull()
    assertTrue(starterErr?.message?.contains("starter template") == true)

    val undefinedErr = runCatching {
      AdminAssistantApi.parseChatResponse("""{"reply":"undefined"}""")
    }.exceptionOrNull()
    assertTrue(undefinedErr != null)
  }
}
