package com.example.assistant

import android.util.Log
import com.example.data.AttendanceService
import com.example.data.AttendanceTrackerService
import com.example.data.ChallengesService
import com.example.data.ClassesService
import com.example.data.NoticesService
import com.example.data.PollsService
import com.example.data.RiskLevel
import com.example.data.TimetableService
import com.example.data.UserSession
import com.example.util.NairobiTimeHelper
import com.example.util.RoleHelper
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone

class FirebaseAssistantService(
  private val classesService: ClassesService = ClassesService(),
  private val noticesService: NoticesService = NoticesService(),
  private val timetableService: TimetableService = TimetableService(),
  private val attendanceService: AttendanceService = AttendanceService(),
  private val attendanceTrackerService: AttendanceTrackerService = AttendanceTrackerService(),
  private val pollsService: PollsService = PollsService(),
  private val challengesService: ChallengesService = ChallengesService()
) {

  private val questionSchema = Schema.obj(
    properties = mapOf(
      "question" to Schema.string(description = "Multiple-choice question text"),
      "options" to Schema.array(
        items = Schema.string(),
        description = "2 to 5 answer options"
      ),
      "correct_index" to Schema.integer(description = "Zero-based index of the correct option"),
      "explanation" to Schema.string(description = "Short explanation of the correct answer")
    ),
    optionalProperties = listOf("explanation")
  )

  private val actionSchema = Schema.obj(
    properties = mapOf(
      "tool" to Schema.enumeration(
        values = listOf(
          ActionExecutor.TOOL_POST_NOTICE,
          ActionExecutor.TOOL_CREATE_POLL,
          ActionExecutor.TOOL_START_ATTENDANCE,
          ActionExecutor.TOOL_TIMETABLE_ENTRY,
          ActionExecutor.TOOL_CHALLENGE_DRAFT,
          ActionExecutor.TOOL_OPEN_SCREEN
        ),
        description = "Action tool identifier"
      ),
      "summary" to Schema.string(description = "Short human-readable summary shown on the confirmation card"),
      "screen" to Schema.string(description = "Target screen name for open_screen: home, classes, profile, notifications, notices, timetable, attendance, groups, materials, polls, play, challenges, leaderboard"),
      "class_id" to Schema.string(description = "Class UUID when relevant"),
      "title" to Schema.string(description = "Notice, attendance, timetable, or challenge title"),
      "content" to Schema.string(description = "Notice body text"),
      "category" to Schema.string(description = "Notice category: general, exam, or assignment"),
      "urgent" to Schema.boolean(description = "Whether the notice is urgent"),
      "pinned" to Schema.boolean(description = "Whether the notice is pinned"),
      "question" to Schema.string(description = "Poll question text"),
      "options" to Schema.array(items = Schema.string(), description = "Poll options list"),
      "hours_open" to Schema.integer(description = "Hours the poll stays open (default 24)"),
      "session_type" to Schema.string(description = "Attendance session type: lecture, cat, exam, lab, or tutorial"),
      "venue" to Schema.string(description = "Room or venue name"),
      "minutes" to Schema.integer(description = "Attendance session duration in minutes"),
      "day_of_week" to Schema.integer(description = "ISO weekday 1 (Monday) to 7 (Sunday)"),
      "start_time" to Schema.string(description = "24h start time HH:MM"),
      "end_time" to Schema.string(description = "24h end time HH:MM"),
      "lecturer_name" to Schema.string(description = "Lecturer name"),
      "description" to Schema.string(description = "Challenge description"),
      "questions" to Schema.array(items = questionSchema, description = "Draft challenge questions")
    ),
    optionalProperties = listOf(
      "screen", "class_id", "title", "content", "category", "urgent", "pinned",
      "question", "options", "hours_open", "session_type", "venue", "minutes",
      "day_of_week", "start_time", "end_time", "lecturer_name", "description", "questions"
    )
  )

  private val responseSchema = Schema.obj(
    properties = mapOf(
      "reply" to Schema.string(
        description = "Short phone-friendly answer in plain text or lightweight markdown (**bold**, '- ' bullets, '1.' numbered steps). First sentence answers directly."
      ),
      "actions" to Schema.array(
        items = actionSchema,
        description = "Optional proposed actions the user can review and confirm. Leave empty unless the user asks to navigate or create/draft something."
      )
    ),
    optionalProperties = listOf("actions")
  )

  private val generativeModel by lazy {
    val config = generationConfig {
      responseMimeType = "application/json"
      responseSchema = this@FirebaseAssistantService.responseSchema
      temperature = 0.4f
    }
    Firebase.ai.generativeModel(
      modelName = MODEL_NAME,
      generationConfig = config,
      systemInstruction = content { text(SYSTEM_PROMPT) }
    )
  }

  suspend fun sendChat(
    messages: List<AssistantMessage>,
    context: AssistantContext,
    session: UserSession,
    isRepInClass: Boolean
  ): Result<AssistantChatResponse> {
    return try {
      val trimmedMessages = messages
        .filter { it.content.isNotBlank() }
        .takeLast(AssistantStorage.MAX_SENT_MESSAGES)
      if (trimmedMessages.isEmpty()) {
        return Result.failure(IllegalArgumentException("Please type a message first."))
      }

      val liveContextText = withContext(Dispatchers.IO) {
        buildLiveContextSummary(context, session, isRepInClass)
      }

      val canDraftStaffTools = RoleHelper.isStaffRole(session.role) ||
        RoleHelper.isClassRep(session.role) ||
        isRepInClass

      val historyContents = trimmedMessages.dropLast(1).map { msg ->
        content(if (msg.role == AssistantRole.USER) "user" else "model") {
          text(msg.content.take(2000))
        }
      }

      val latestUserPrompt = buildTurnPrompt(
        userMessage = trimmedMessages.last().content.take(2000),
        liveContextText = liveContextText,
        canDraftStaffTools = canDraftStaffTools
      )

      val response = withTimeout(TIMEOUT_MS) {
        val chat = generativeModel.startChat(history = historyContents)
        chat.sendMessage(latestUserPrompt)
      }

      val rawJson = response.text?.trim().orEmpty()
      if (rawJson.isBlank()) {
        return Result.failure(IllegalStateException("Assistant returned an empty response. Please try again."))
      }

      val parsed = parseFirebaseResponse(
        jsonString = rawJson,
        allowStaffProposals = canDraftStaffTools,
        fallbackClassId = context.classId
      )
      Result.success(parsed)
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Firebase AI chat request timed out", e)
      Result.failure(IllegalStateException("Assistant request timed out after 60 seconds. Please try again."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Firebase AI chat request failed", e)
      val friendly = formatFirebaseException(e)
      Result.failure(IllegalStateException(friendly, e))
    }
  }

  private suspend fun buildLiveContextSummary(
    context: AssistantContext,
    session: UserSession,
    isRepInClass: Boolean
  ): String = withTimeoutOrNull(4_500L) {
    val token = session.accessToken
    val roleLabel = when {
      RoleHelper.isAdmin(session.role) -> "admin"
      RoleHelper.isLecturer(session.role) -> "lecturer"
      RoleHelper.isClassRep(session.role) || isRepInClass -> "class prefect"
      else -> "student"
    }

    val classesDeferred = async {
      classesService.fetchClasses(session.userId, session.role ?: "student", token).getOrNull().orEmpty()
    }
    val classes = classesDeferred.await()
    val activeClass = classes.firstOrNull { it.id == context.classId } ?: classes.firstOrNull()
    val classId = activeClass?.id.orEmpty()

    val sb = StringBuilder()
    sb.appendLine("User: ${session.fullName.ifBlank { "Signed-in user" }} (role: $roleLabel)")
    sb.appendLine("Current Nairobi time: ${NairobiTimeHelper.nowNairobi("yyyy-MM-dd HH:mm")} (ISO weekday: ${currentNairobiIsoDayOfWeek()})")
    sb.appendLine("Current screen: ${context.screen}")
    if (classes.isEmpty()) {
      sb.appendLine("Enrolled classes: none yet")
    } else {
      sb.appendLine("Enrolled classes: " + classes.take(8).joinToString("; ") { "${it.name} (${it.code}, id=${it.id})" })
    }

    if (classId.isNotBlank()) {
      sb.appendLine("Active class: ${activeClass?.name ?: "Class"} (${activeClass?.code.orEmpty()}, id=$classId)")
      val schedulesDef = async { timetableService.fetchSchedules(classId, token).getOrNull().orEmpty() }
      val noticesDef = async { noticesService.fetchNotices(classId, token).getOrNull().orEmpty() }
      val pollsDef = async { pollsService.listPolls(classId, token).getOrNull().orEmpty() }
      val openAttendanceDef = async {
        attendanceService.fetchOpenSessions(classId, NairobiTimeHelper.toIsoUtc(), token).getOrNull().orEmpty()
      }
      val myAttendanceDef = async {
        if (!RoleHelper.isStaffRole(session.role)) {
          attendanceTrackerService.getMyAttendanceStatus(classId, token).getOrNull()
        } else null
      }
      val myPointsDef = async {
        if (!RoleHelper.isStaffRole(session.role)) {
          challengesService.getMyActivityStats(classId, token).getOrNull()
        } else null
      }
      val staffRiskTotalsDef = async {
        if (RoleHelper.isLecturer(session.role) || RoleHelper.isAdmin(session.role)) {
          attendanceTrackerService.getClassAttendanceRisk(classId, token).getOrNull()
        } else null
      }

      val schedules = schedulesDef.await()
      val todayDay = currentNairobiIsoDayOfWeek()
      val todaySchedules = schedules.filter { it.dayOfWeek == todayDay }
      if (todaySchedules.isNotEmpty()) {
        sb.appendLine("Today's classes (${todaySchedules.first().dayName}):")
        todaySchedules.take(10).forEach { s ->
          sb.appendLine("- ${s.formattedTimeRange}: ${s.title} | room: ${s.room} | lecturer: ${s.lecturerName} | status: ${s.normalizedStatus}")
        }
      } else if (schedules.isNotEmpty()) {
        sb.appendLine("Today has no scheduled sessions. Full week entries: ${schedules.size} (${schedules.take(6).joinToString("; ") { "${it.dayName} ${it.formattedTimeRange} ${it.title}" }})")
      } else {
        sb.appendLine("Timetable: no entries yet")
      }

      val openAtt = openAttendanceDef.await()
      if (openAtt.isNotEmpty()) {
        sb.appendLine("Open attendance right now: ${openAtt.size} session(s), earliest expires ${NairobiTimeHelper.formatIsoToNairobi(openAtt.first().expiresAt)}")
      } else {
        sb.appendLine("Open attendance right now: none")
      }

      val notices = noticesDef.await().take(5)
      if (notices.isNotEmpty()) {
        sb.appendLine("Recent notices:")
        notices.forEach { n ->
          val urgentTag = if (n.isUrgent) " [URGENT]" else ""
          sb.appendLine("- ${n.title}$urgentTag (${n.category}): ${n.content.take(120)}")
        }
      }

      val openPolls = pollsDef.await().filter { it.isOpen() }.take(4)
      if (openPolls.isNotEmpty()) {
        sb.appendLine("Open polls: " + openPolls.joinToString("; ") { "${it.question} (${it.statusBadge().second}, ${it.totalVotes} votes)" })
      }

      myAttendanceDef.await()?.let { att ->
        sb.appendLine("My attendance: ${att.attended}/${att.totalSessions} attended (${att.percentage.toInt()}%, excused=${att.excused}, streak missed=${att.missStreak}, risk=${att.riskLevel.label})")
      }

      myPointsDef.await()?.let { pts ->
        val rankStr = pts.weekRank?.let { "#$it/${pts.rankedStudents}" } ?: "unranked"
        sb.appendLine("My points: total=${pts.totalPoints}, this week=${pts.weekPoints}, streak=${pts.streakDays}d, week rank=$rankStr")
      }

      staffRiskTotalsDef.await()?.let { riskList ->
        val crit = riskList.count { it.riskLevel == RiskLevel.CRITICAL }
        val high = riskList.count { it.riskLevel == RiskLevel.HIGH }
        val watch = riskList.count { it.riskLevel == RiskLevel.WATCH }
        val ok = riskList.count { it.riskLevel == RiskLevel.OK }
        sb.appendLine("Class attendance summary (totals only): total=${riskList.size} students (OK=$ok, Watch=$watch, High=$high, Critical=$crit)")
      }
    }

    sb.toString().trim()
  } ?: "User role: ${session.role ?: "student"}, screen: ${context.screen}, classId: ${context.classId ?: "none"}"

  companion object {
    private const val TAG = "FirebaseAssistantService"
    const val MODEL_NAME = "gemini-3.8-flash"
    private const val TIMEOUT_MS = 60_000L

    private val STAFF_WRITE_TOOLS = setOf(
      ActionExecutor.TOOL_POST_NOTICE,
      ActionExecutor.TOOL_CREATE_POLL,
      ActionExecutor.TOOL_START_ATTENDANCE,
      ActionExecutor.TOOL_TIMETABLE_ENTRY,
      ActionExecutor.TOOL_CHALLENGE_DRAFT
    )

    private const val SYSTEM_PROMPT = """You are ClassRep Assistant, the helper inside the ClassRep app used by Pwani University students, class prefects, lecturers and administrators. You are an AI, and you say so if asked.

YOUR GOAL
Give the answer a smart, kind senior student or tutor would give: correct, clear, short and easy to read on a phone.

HOW TO WRITE
- The first sentence is the answer or the direct next step. No "Sure!", no "Great question", no restating the question, no closing recap.
- Layout for a phone chat: short paragraphs of 1 to 3 sentences with a blank line between them. Use "- " bullets for 3 or more parallel items and "1." numbered lists for steps in order. Use **bold** only for key terms, dates and times. No headings, no tables, no emojis, no LaTeX. Write maths in plain text, for example x^2.
- Never invent class facts, dates, scores, or attendance numbers. If something is missing from the live context provided, say so clearly in one sentence.
- Privacy: Never ask for passwords or secrets. Never share individual student risk details unless they belong to the asking student.
- Actions safety: You CANNOT change anything directly in the database. You can only propose actions in the `actions` array so the user sees a Confirm/Edit/Cancel card:
  - Always allowed: `open_screen` (valid screens: home, classes, profile, notifications, notices, timetable, attendance, groups, materials, polls, play, challenges, leaderboard).
  - Allowed ONLY when `can_draft_staff_tools` is true: `propose_post_notice`, `propose_create_poll`, `propose_start_attendance`, `propose_timetable_entry`, `propose_challenge_draft`.
  - Never put actions in `actions` unless the user asks to navigate or create/draft something."""

    fun buildTurnPrompt(
      userMessage: String,
      liveContextText: String,
      canDraftStaffTools: Boolean
    ): String {
      return buildString {
        appendLine("[APP CONTEXT]")
        appendLine("can_draft_staff_tools: $canDraftStaffTools")
        if (liveContextText.isNotBlank()) {
          appendLine(liveContextText)
        }
        appendLine("[/APP CONTEXT]")
        appendLine()
        append(userMessage)
      }
    }

    fun parseFirebaseResponse(
      jsonString: String,
      allowStaffProposals: Boolean,
      fallbackClassId: String? = null
    ): AssistantChatResponse {
      val clean = jsonString.trim()
      if (clean.isEmpty()) {
        throw IllegalStateException("Assistant returned an empty response.")
      }
      val root = runCatching { JSONObject(clean) }.getOrNull()
        ?: return AssistantChatResponse(reply = clean, actions = emptyList())

      val reply = root.optString("reply", "").trim()
        .takeUnless { it.equals("null", ignoreCase = true) || it.equals("undefined", ignoreCase = true) }
        .orEmpty()

      val rawActions = root.optJSONArray("actions")
      val actions = buildList {
        if (rawActions != null) {
          for (i in 0 until rawActions.length()) {
            val obj = rawActions.optJSONObject(i) ?: continue
            val tool = obj.optString("tool", "").trim()
            if (!ActionExecutor.isSupportedMainTool(tool)) continue
            if (tool in STAFF_WRITE_TOOLS && !allowStaffProposals) continue

            val argsMap = linkedMapOf<String, Any?>()
            val keys = obj.keys()
            while (keys.hasNext()) {
              val key = keys.next()
              if (key == "tool" || key == "summary") continue
              val value = jsonValToKotlin(obj.opt(key))
              if (value != null) argsMap[key] = value
            }
            if (!argsMap.containsKey("class_id") && !fallbackClassId.isNullOrBlank()) {
              argsMap["class_id"] = fallbackClassId
            }

            val summary = obj.optString("summary", "").trim()
              .takeUnless { it.equals("null", ignoreCase = true) }
              .orEmpty()
              .ifBlank { tool.replace('_', ' ') }

            add(
              AssistantAction(
                id = "fb_act_$i",
                tool = tool,
                args = argsMap,
                summary = summary
              )
            )
          }
        }
      }

      val finalReply = when {
        reply.isNotBlank() -> reply
        actions.isNotEmpty() -> "Here is a suggested action you can review and confirm:"
        else -> throw IllegalStateException("Assistant returned no reply text. Please try asking again.")
      }
      return AssistantChatResponse(reply = finalReply, actions = actions)
    }

    private fun jsonValToKotlin(value: Any?): Any? = when (value) {
      null, JSONObject.NULL -> null
      is JSONObject -> {
        val m = linkedMapOf<String, Any?>()
        val k = value.keys()
        while (k.hasNext()) {
          val key = k.next()
          val v = jsonValToKotlin(value.opt(key))
          if (v != null) m[key] = v
        }
        m
      }
      is JSONArray -> List(value.length()) { idx -> jsonValToKotlin(value.opt(idx)) }.filterNotNull()
      else -> value
    }

    private fun currentNairobiIsoDayOfWeek(): Int {
      val cal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Nairobi"))
      return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
      }
    }

    private fun formatFirebaseException(e: Exception): String {
      val raw = e.localizedMessage?.trim().orEmpty()
      val lower = raw.lowercase()
      return when {
        lower.contains("network") || lower.contains("unable to resolve host") || lower.contains("failed to connect") ->
          "Unable to reach Gemini AI. Check your internet connection and try again."
        lower.contains("403") || lower.contains("app check") || lower.contains("attestation") ->
          "Firebase App Check verification failed (${raw.take(140)})."
        lower.contains("429") || lower.contains("quota") || lower.contains("resource_exhausted") ->
          "AI service is temporarily busy. Please wait a moment and try again."
        raw.isNotBlank() -> raw
        else -> "Assistant request failed. Please try again."
      }
    }
  }
}
