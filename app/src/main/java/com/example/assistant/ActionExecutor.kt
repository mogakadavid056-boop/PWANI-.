package com.example.assistant

import com.example.data.AttendanceRegisterService
import com.example.data.AttendanceService
import com.example.data.DraftQuizQuestion
import com.example.data.NoticesService
import com.example.data.PollsService
import com.example.data.QuestionType
import com.example.data.TimetableService
import com.example.ui.components.ClassTab
import com.example.ui.screens.MoreSubView
import com.example.util.NairobiTimeHelper
import java.time.Instant
import java.util.Date

sealed interface MainNavigationTarget {
  data object Home : MainNavigationTarget
  data object Profile : MainNavigationTarget
  data object Notifications : MainNavigationTarget
  data class ClassDestination(
    val classId: String?,
    val tab: ClassTab,
    val moreSubView: MoreSubView = MoreSubView.None
  ) : MainNavigationTarget
}

class ActionExecutor(
  private val noticesService: NoticesService = NoticesService(),
  private val pollsService: PollsService = PollsService(),
  private val attendanceService: AttendanceService = AttendanceService(),
  private val attendanceRegisterService: AttendanceRegisterService = AttendanceRegisterService(),
  private val timetableService: TimetableService = TimetableService(),
  private val onNoticesChanged: (String) -> Unit = {},
  private val onPollsChanged: (String) -> Unit = {},
  private val onAttendanceStarted: (classId: String, sessionId: String, sessionCode: String, expiresAtIso: String, remainingSeconds: Long) -> Unit = { _, _, _, _, _ -> },
  private val onTimetableChanged: (String) -> Unit = {},
  private val onOpenChallengeDraft: (ChallengeDraftPrefill) -> Unit = {},
  private val onNavigate: (MainNavigationTarget) -> Unit = {}
) {

  suspend fun execute(
    action: AssistantAction,
    fallbackClassId: String?,
    accessToken: String
  ): Result<String> {
    if (!isSupportedMainTool(action.tool)) {
      return Result.failure(IllegalArgumentException("Unsupported action: ${action.tool}"))
    }
    val args = action.args
    val classId = strArg(args, "class_id", "target_class_id")
      .ifBlank { fallbackClassId.orEmpty() }

    return when (action.tool) {
      TOOL_POST_NOTICE -> executePostNotice(args, classId, accessToken)
      TOOL_CREATE_POLL -> executeCreatePoll(args, classId, accessToken)
      TOOL_START_ATTENDANCE -> executeStartAttendance(args, classId, accessToken)
      TOOL_TIMETABLE_ENTRY -> executeTimetableEntry(args, classId, accessToken)
      TOOL_CHALLENGE_DRAFT -> executeChallengeDraft(args, classId)
      TOOL_OPEN_SCREEN -> executeOpenScreen(args, fallbackClassId)
      else -> Result.failure(IllegalArgumentException("Action is not permitted."))
    }
  }

  private suspend fun executePostNotice(
    args: Map<String, Any?>,
    classId: String,
    accessToken: String
  ): Result<String> {
    if (classId.isBlank()) return Result.failure(IllegalArgumentException("Select a class first to post a notice."))
    val title = strArg(args, "title", "notice_title")
    val content = strArg(args, "content", "notice_content", "body")
    if (title.isBlank() || content.isBlank()) {
      return Result.failure(IllegalArgumentException("Notice title and message are required."))
    }
    val category = strArg(args, "category", "notice_category").lowercase().ifBlank { "general" }
    val urgent = boolArg(args, "is_urgent", "urgent", defaultValue = false)
    val pinned = boolArg(args, "is_pinned", "pinned", defaultValue = false)
    return noticesService.createNotice(
      classId = classId,
      title = title,
      content = content,
      category = category,
      urgent = urgent,
      pinned = pinned,
      accessToken = accessToken
    ).map {
      onNoticesChanged(classId)
      "Notice posted to class."
    }
  }

  private suspend fun executeCreatePoll(
    args: Map<String, Any?>,
    classId: String,
    accessToken: String
  ): Result<String> {
    if (classId.isBlank()) return Result.failure(IllegalArgumentException("Select a class first to create a poll."))
    val question = strArg(args, "question", "poll_question", "title")
    val options = stringListArg(args, "options", "poll_options")
      .map { it.trim() }
      .filter { it.isNotBlank() }
    if (question.isBlank() || options.size < 2) {
      return Result.failure(IllegalArgumentException("Poll requires a question and at least 2 options."))
    }
    val explicitExpiry = strArg(args, "expires_at", "poll_expiry")
    val durationHours = intArg(args, "hours_open", "duration_hours", "hours", defaultValue = 24).coerceIn(1, 168)
    val expiryIso = explicitExpiry.ifBlank {
      Instant.now().plusSeconds(durationHours * 3600L).toString()
    }
    return pollsService.createPoll(
      classId = classId,
      question = question,
      options = options.take(10),
      expiryIso = expiryIso,
      accessToken = accessToken
    ).map {
      onPollsChanged(classId)
      "Poll created and published."
    }
  }

  private suspend fun executeStartAttendance(
    args: Map<String, Any?>,
    classId: String,
    accessToken: String
  ): Result<String> {
    if (classId.isBlank()) return Result.failure(IllegalArgumentException("Select a class first to start attendance."))
    val rawTitle = strArg(args, "session_title", "title")
    val sessionType = strArg(args, "session_type", "type").lowercase()
    val sessionTitle = rawTitle.ifBlank {
      when (sessionType) {
        "cat" -> "CAT Attendance"
        "exam" -> "Exam Attendance"
        "lab" -> "Lab Attendance"
        "tutorial" -> "Tutorial Attendance"
        else -> "Class Attendance"
      }
    }
    val courseId = strArg(args, "course_id", "target_course_id").ifBlank { null }
    val durationMinutes = intArg(args, "duration_minutes", "minutes", defaultValue = 15).coerceIn(1, 180)
    val expiryIso = NairobiTimeHelper.toIsoUtc(Date(System.currentTimeMillis() + durationMinutes * 60_000L))
    return attendanceService.createAttendanceSessionV2(
      classId = classId,
      courseId = courseId,
      sessionExpiryIso = expiryIso,
      sessionTitle = sessionTitle,
      accessToken = accessToken
    ).map { res ->
      val courseTitle = strArg(args, "course_title", "course_name")
      val courseCode = strArg(args, "course_code")
      val lecturer = strArg(args, "lecturer_name", "lecturer")
      val venue = strArg(args, "venue", "venue_text", "room")
      if (courseTitle.isNotBlank() || courseCode.isNotBlank() || lecturer.isNotBlank() || venue.isNotBlank()) {
        attendanceRegisterService.updateRegisterDetails(
          sessionId = res.sessionId,
          courseTitle = courseTitle,
          courseCode = courseCode,
          lecturerName = lecturer,
          venueText = venue,
          sessionTitle = sessionTitle,
          accessToken = accessToken
        )
      }
      val rem = NairobiTimeHelper.calculateRemainingSeconds(res.expiresAt).let {
        if (it > 0L) it else durationMinutes * 60L
      }
      onAttendanceStarted(classId, res.sessionId, res.sessionCode, res.expiresAt, rem)
      onNavigate(MainNavigationTarget.ClassDestination(classId, ClassTab.Attendance))
      if (res.sessionCode.isNotBlank()) "Attendance opened! Code: ${res.sessionCode}" else "Attendance session started!"
    }
  }

  private suspend fun executeTimetableEntry(
    args: Map<String, Any?>,
    classId: String,
    accessToken: String
  ): Result<String> {
    if (classId.isBlank()) return Result.failure(IllegalArgumentException("Select a class first to add timetable entry."))
    val title = strArg(args, "title", "course_title", "subject")
    val dayOfWeek = parseDayOfWeek(args["day_of_week"] ?: args["day"])
    val startTime = strArg(args, "start_time", "start")
    val endTime = strArg(args, "end_time", "end")
    val room = strArg(args, "room", "venue").ifBlank { "TBA" }
    val lecturer = strArg(args, "lecturer_name", "lecturer").ifBlank { "TBA" }
    if (title.isBlank() || startTime.isBlank() || endTime.isBlank()) {
      return Result.failure(IllegalArgumentException("Title, start time, and end time are required."))
    }
    return timetableService.addSchedule(
      classId = classId,
      title = title,
      dayOfWeek = dayOfWeek,
      startTime = startTime,
      endTime = endTime,
      room = room,
      lecturerName = lecturer,
      accessToken = accessToken
    ).map {
      onTimetableChanged(classId)
      "Timetable entry \"$title\" added."
    }
  }

  private fun executeChallengeDraft(
    args: Map<String, Any?>,
    classId: String
  ): Result<String> {
    if (classId.isBlank()) {
      return Result.failure(IllegalArgumentException("Select a class first to open the challenge draft."))
    }
    val prefill = parseChallengeDraftPrefill(args, classId)
    onOpenChallengeDraft(prefill)
    return Result.success("Draft opened in Create Challenge editor.")
  }

  private fun executeOpenScreen(
    args: Map<String, Any?>,
    fallbackClassId: String?
  ): Result<String> {
    val screenName = strArg(args, "screen", "destination", "target").ifBlank { "home" }
    val classId = strArg(args, "class_id", "target_class_id").ifBlank { fallbackClassId.orEmpty() }.ifBlank { null }
    val target = mapOpenScreenDestination(screenName, classId)
      ?: return Result.failure(IllegalArgumentException("Unknown screen: $screenName"))
    onNavigate(target)
    return Result.success("Opened ${screenName.replace('_', ' ')}.")
  }

  companion object {
    const val TOOL_POST_NOTICE = "propose_post_notice"
    const val TOOL_CREATE_POLL = "propose_create_poll"
    const val TOOL_START_ATTENDANCE = "propose_start_attendance"
    const val TOOL_TIMETABLE_ENTRY = "propose_timetable_entry"
    const val TOOL_CHALLENGE_DRAFT = "propose_challenge_draft"
    const val TOOL_OPEN_SCREEN = "open_screen"

    private val SUPPORTED_MAIN_TOOLS = setOf(
      TOOL_POST_NOTICE,
      TOOL_CREATE_POLL,
      TOOL_START_ATTENDANCE,
      TOOL_TIMETABLE_ENTRY,
      TOOL_CHALLENGE_DRAFT,
      TOOL_OPEN_SCREEN
    )

    fun isSupportedMainTool(tool: String): Boolean = tool.trim() in SUPPORTED_MAIN_TOOLS

    fun mapOpenScreenDestination(rawScreen: String, classId: String?): MainNavigationTarget? {
      return when (rawScreen.trim().lowercase()) {
        "home", "classes", "dashboard" -> MainNavigationTarget.Home
        "profile" -> MainNavigationTarget.Profile
        "notifications", "notification" -> MainNavigationTarget.Notifications
        "notices", "announcements" -> MainNavigationTarget.ClassDestination(classId, ClassTab.Notices)
        "timetable", "schedule" -> MainNavigationTarget.ClassDestination(classId, ClassTab.Timetable)
        "attendance" -> MainNavigationTarget.ClassDestination(classId, ClassTab.Attendance)
        "groups", "group_chat", "discussions" -> MainNavigationTarget.ClassDestination(classId, ClassTab.Groups)
        "materials", "notes", "units" -> MainNavigationTarget.ClassDestination(classId, ClassTab.More, MoreSubView.Materials)
        "polls" -> MainNavigationTarget.ClassDestination(classId, ClassTab.More, MoreSubView.Polls)
        "play", "challenges", "quizzes" -> MainNavigationTarget.ClassDestination(classId, ClassTab.More, MoreSubView.Challenges)
        "leaderboard" -> MainNavigationTarget.ClassDestination(classId, ClassTab.More, MoreSubView.Leaderboard)
        else -> null
      }
    }

    fun parseChallengeDraftPrefill(args: Map<String, Any?>, classId: String): ChallengeDraftPrefill {
      val title = strArg(args, "title", "quiz_title").ifBlank { "Draft Challenge" }
      val desc = strArg(args, "description", "summary")
      val rawQuestions = args["questions"] as? List<*> ?: emptyList<Any?>()
      val parsedQuestions = rawQuestions.mapNotNull { item ->
        val map = item as? Map<*, *> ?: return@mapNotNull null
        val qText = map["question"]?.toString()?.trim().orEmpty()
        if (qText.isBlank()) return@mapNotNull null
        val rawOpts = (map["options"] as? List<*>)?.map { it?.toString()?.trim().orEmpty() }.orEmpty()
        val paddedOpts = if (rawOpts.size >= 2) {
          (rawOpts + List((4 - rawOpts.size).coerceAtLeast(0)) { "" }).take(5)
        } else {
          listOf("", "", "", "")
        }
        val correctIdx = (map["correct_index"] as? Number)?.toInt()
          ?: (map["correctIndex"] as? Number)?.toInt()
          ?: 0
        val explanation = map["explanation"]?.toString()?.trim().orEmpty()
        DraftQuizQuestion(
          questionType = QuestionType.MCQ,
          question = qText,
          options = paddedOpts,
          correctIndex = correctIdx.coerceIn(0, (paddedOpts.size - 1).coerceAtLeast(0)),
          explanation = explanation
        )
      }
      return ChallengeDraftPrefill(
        classId = classId,
        title = title,
        description = desc,
        questions = parsedQuestions.ifEmpty {
          listOf(DraftQuizQuestion(question = "", options = listOf("", "", "", ""), correctIndex = 0))
        }
      )
    }

    fun parseDayOfWeek(raw: Any?): Int = when (raw) {
      is Number -> raw.toInt().coerceIn(1, 7)
      is String -> when (raw.trim().lowercase()) {
        "monday", "mon", "1" -> 1
        "tuesday", "tue", "2" -> 2
        "wednesday", "wed", "3" -> 3
        "thursday", "thu", "4" -> 4
        "friday", "fri", "5" -> 5
        "saturday", "sat", "6" -> 6
        "sunday", "sun", "7" -> 7
        else -> 1
      }
      else -> 1
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

    fun intArg(args: Map<String, Any?>, vararg keys: String, defaultValue: Int): Int {
      for (k in keys) {
        val v = args[k] ?: continue
        when (v) {
          is Number -> return v.toInt()
          is String -> v.trim().toIntOrNull()?.let { return it }
        }
      }
      return defaultValue
    }

    fun stringListArg(args: Map<String, Any?>, vararg keys: String): List<String> {
      for (k in keys) {
        val v = args[k] as? List<*> ?: continue
        return v.mapNotNull { it?.toString()?.trim() }
      }
      return emptyList()
    }
  }
}
