package com.example.admin.assistant

import java.util.UUID

enum class AdminAssistantRole(val apiValue: String) {
  USER("user"),
  ASSISTANT("assistant")
}

enum class AdminActionCardStatus {
  PENDING,
  EXECUTING,
  DONE,
  FAILED,
  CANCELLED
}

data class AdminAssistantAction(
  val id: String = UUID.randomUUID().toString(),
  val tool: String,
  val args: Map<String, Any?> = emptyMap(),
  val summary: String = "",
  val status: AdminActionCardStatus = AdminActionCardStatus.PENDING,
  val resultMessage: String? = null
)

data class AdminAssistantMessage(
  val id: String = UUID.randomUUID().toString(),
  val role: AdminAssistantRole,
  val content: String,
  val actions: List<AdminAssistantAction> = emptyList(),
  val timestampMs: Long = System.currentTimeMillis()
)

data class AdminAssistantContext(
  val app: String = "admin",
  val screen: String = "overview",
  val classId: String? = null
)

data class AdminAssistantChatResponse(
  val reply: String,
  val actions: List<AdminAssistantAction> = emptyList()
)

data class AdminSuggestionChip(
  val label: String,
  val autoSend: Boolean
)

object AdminAssistantSuggestions {
  val chips: List<AdminSuggestionChip> = listOf(
    AdminSuggestionChip("Unverified lecturers", autoSend = true),
    AdminSuggestionChip("Classes without a prefect", autoSend = true),
    AdminSuggestionChip("System summary", autoSend = true),
    AdminSuggestionChip("Students without a class", autoSend = true)
  )
}
