package com.example.assistant

import com.example.data.DraftQuizQuestion
import com.example.util.RoleHelper
import java.util.UUID

enum class AssistantRole(val apiValue: String) {
  USER("user"),
  ASSISTANT("assistant")
}

enum class ActionCardStatus {
  PENDING,
  EXECUTING,
  DONE,
  FAILED,
  CANCELLED
}

enum class SpeechLanguage(val localeTag: String, val shortLabel: String) {
  ENGLISH("en-KE", "EN"),
  KISWAHILI("sw-KE", "SW")
}

data class AssistantAction(
  val id: String = UUID.randomUUID().toString(),
  val tool: String,
  val args: Map<String, Any?> = emptyMap(),
  val summary: String = "",
  val status: ActionCardStatus = ActionCardStatus.PENDING,
  val resultMessage: String? = null
)

data class AssistantMessage(
  val id: String = UUID.randomUUID().toString(),
  val role: AssistantRole,
  val content: String,
  val actions: List<AssistantAction> = emptyList(),
  val timestampMs: Long = System.currentTimeMillis()
)

data class AssistantContext(
  val app: String = "main",
  val screen: String = "home",
  val classId: String? = null
)

data class AssistantChatResponse(
  val reply: String,
  val actions: List<AssistantAction> = emptyList()
)

data class ChallengeDraftPrefill(
  val classId: String,
  val title: String,
  val description: String,
  val questions: List<DraftQuizQuestion>
)

data class AssistantSuggestionChip(
  val label: String,
  val autoSend: Boolean
)

object AssistantSuggestions {
  fun forMainRole(role: String?, isRepInClass: Boolean = false): List<AssistantSuggestionChip> {
    val isStaff = RoleHelper.isStaffRole(role)
    val isLecturerOrAdmin = RoleHelper.isLecturer(role) || RoleHelper.isAdmin(role)
    val canDraftStaffTools = isStaff || RoleHelper.isClassRep(role) || isRepInClass
    val isStudentOrPrefect = !isStaff || RoleHelper.isClassRep(role) || isRepInClass

    val list = mutableListOf(
      AssistantSuggestionChip("What is my timetable today?", autoSend = true),
      AssistantSuggestionChip("Is attendance open now?", autoSend = true),
      AssistantSuggestionChip("Explain a topic simply", autoSend = false)
    )
    if (isStudentOrPrefect) {
      list.add(AssistantSuggestionChip("How is my attendance?", autoSend = true))
      list.add(AssistantSuggestionChip("How many points do I have?", autoSend = true))
    }
    if (canDraftStaffTools) {
      list.add(AssistantSuggestionChip("Draft a notice about...", autoSend = false))
      list.add(AssistantSuggestionChip("Create a poll about...", autoSend = false))
      list.add(AssistantSuggestionChip("Make a 5 question quiz on...", autoSend = false))
    }
    if (isLecturerOrAdmin) {
      list.add(AssistantSuggestionChip("How is attendance in my class?", autoSend = true))
    }
    return list
  }
}
