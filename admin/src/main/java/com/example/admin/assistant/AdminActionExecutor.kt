package com.example.admin.assistant

import com.example.admin.data.AdminMonitorContentRepository
import com.example.admin.ui.AdminTab
import com.example.admin.ui.ApprovalsSubTab
import com.example.admin.ui.MonitorTab

sealed interface AdminNavigationTarget {
  data class Tab(val tab: AdminTab) : AdminNavigationTarget
  data object Notifications : AdminNavigationTarget
  data class ApprovalsSubTabTarget(val subTab: ApprovalsSubTab) : AdminNavigationTarget
  data class ClassDetailTab(val classId: String?, val monitorTab: MonitorTab) : AdminNavigationTarget
}

class AdminActionExecutor(
  private val contentRepo: AdminMonitorContentRepository = AdminMonitorContentRepository(),
  private val onNoticePosted: (String) -> Unit = {},
  private val onNavigate: (AdminNavigationTarget) -> Unit = {}
) {
  suspend fun execute(
    action: AdminAssistantAction,
    fallbackClassId: String?,
    accessToken: String
  ): Result<String> {
    if (!isSupportedAdminTool(action.tool)) {
      return Result.failure(IllegalArgumentException("Action is not permitted in the Admin app."))
    }
    val args = action.args
    val classId = strArg(args, "class_id", "target_class_id").ifBlank { fallbackClassId.orEmpty() }

    return when (action.tool) {
      TOOL_OPEN_SCREEN -> executeOpenScreen(args, fallbackClassId)
      TOOL_POST_NOTICE -> executePostNotice(args, classId, accessToken)
      else -> Result.failure(IllegalArgumentException("Action is not permitted in the Admin app."))
    }
  }

  private fun executeOpenScreen(
    args: Map<String, Any?>,
    fallbackClassId: String?
  ): Result<String> {
    val screenName = strArg(args, "screen", "destination", "target").ifBlank { "overview" }
    val classId = strArg(args, "class_id", "target_class_id")
      .ifBlank { fallbackClassId.orEmpty() }
      .ifBlank { null }
    val target = mapOpenScreenDestination(screenName, classId)
      ?: return Result.failure(IllegalArgumentException("Unknown screen: $screenName"))
    onNavigate(target)
    return Result.success("Opened ${screenName.replace('_', ' ')}.")
  }

  private suspend fun executePostNotice(
    args: Map<String, Any?>,
    classId: String,
    accessToken: String
  ): Result<String> {
    if (classId.isBlank()) {
      return Result.failure(IllegalArgumentException("Open a class first to post a notice."))
    }
    val title = strArg(args, "title", "notice_title")
    val content = strArg(args, "content", "notice_content", "body")
    if (title.isBlank() || content.isBlank()) {
      return Result.failure(IllegalArgumentException("Notice title and message are required."))
    }
    val category = strArg(args, "category", "notice_category").lowercase().ifBlank { "general" }
    val urgent = boolArg(args, "is_urgent", "urgent", defaultValue = false)
    val pinned = boolArg(args, "is_pinned", "pinned", defaultValue = false)
    return contentRepo.createNotice(
      classId = classId,
      title = title,
      content = content,
      category = category,
      urgent = urgent,
      pinned = pinned,
      accessToken = accessToken
    ).map {
      onNoticePosted(classId)
      "Notice posted to class."
    }
  }

  companion object {
    const val TOOL_OPEN_SCREEN = "open_screen"
    const val TOOL_POST_NOTICE = "propose_post_notice"

    private val SUPPORTED_ADMIN_TOOLS = setOf(TOOL_OPEN_SCREEN, TOOL_POST_NOTICE)

    fun isSupportedAdminTool(tool: String): Boolean = tool.trim() in SUPPORTED_ADMIN_TOOLS

    fun mapOpenScreenDestination(rawScreen: String, classId: String?): AdminNavigationTarget? {
      return when (rawScreen.trim().lowercase()) {
        "dashboard", "overview", "home" -> AdminNavigationTarget.Tab(AdminTab.OVERVIEW)
        "classes", "class_search", "class_monitor" -> AdminNavigationTarget.Tab(AdminTab.CLASSES)
        "approvals", "units_pending", "pending_units" -> AdminNavigationTarget.ApprovalsSubTabTarget(ApprovalsSubTab.UNITS)
        "unverified_lecturers", "lecturers", "verify_lecturers" -> AdminNavigationTarget.ApprovalsSubTabTarget(ApprovalsSubTab.LECTURERS)
        "join_requests", "class_requests" -> AdminNavigationTarget.ApprovalsSubTabTarget(ApprovalsSubTab.JOIN_REQUESTS)
        "risk", "attendance_risk" -> AdminNavigationTarget.Tab(AdminTab.RISK)
        "users", "users_roles" -> AdminNavigationTarget.Tab(AdminTab.USERS)
        "university", "schools", "programmes", "academic_years" -> AdminNavigationTarget.Tab(AdminTab.UNIVERSITY)
        "notifications", "notification" -> AdminNavigationTarget.Notifications
        "notices", "announcements" -> AdminNavigationTarget.ClassDetailTab(classId, MonitorTab.NOTICES)
        "members" -> AdminNavigationTarget.ClassDetailTab(classId, MonitorTab.MEMBERS)
        "timetable", "schedule" -> AdminNavigationTarget.ClassDetailTab(classId, MonitorTab.TIMETABLE)
        "units" -> AdminNavigationTarget.ClassDetailTab(classId, MonitorTab.UNITS)
        "materials", "notes" -> AdminNavigationTarget.ClassDetailTab(classId, MonitorTab.MATERIALS)
        "attendance" -> AdminNavigationTarget.ClassDetailTab(classId, MonitorTab.ATTENDANCE)
        "exams", "quizzes", "play", "leaderboard" -> AdminNavigationTarget.ClassDetailTab(classId, MonitorTab.EXAMS)
        "polls" -> AdminNavigationTarget.ClassDetailTab(classId, MonitorTab.POLLS)
        "groups" -> AdminNavigationTarget.ClassDetailTab(classId, MonitorTab.GROUPS)
        "profile" -> AdminNavigationTarget.Tab(AdminTab.OVERVIEW)
        else -> null
      }
    }

    fun strArg(args: Map<String, Any?>, vararg keys: String): String {
      for (k in keys) {
        val v = args[k]?.toString()?.trim().orEmpty()
        if (v.isNotBlank() && !v.equals("null", ignoreCase = true)) return v
      }
      return ""
    }

    fun boolArg(args: Map<String, Any?>, vararg keys: String, defaultValue: Boolean): Boolean {
      for (k in keys) {
        val v = args[k] ?: continue
        when (v) {
          is Boolean -> return v
          is String -> if (v.equals("true", true)) return true else if (v.equals("false", true)) return false
        }
      }
      return defaultValue
    }
  }
}
