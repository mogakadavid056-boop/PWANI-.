package com.example.data

import com.example.util.JsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class ChallengesService(
  private val client: OkHttpClient = OfflineCache.getClient()
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  private suspend fun postRpc(
    rpcName: String,
    payload: JSONObject,
    accessToken: String
  ): Result<String> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    if (accessToken.isBlank()) {
      return@withContext Result.failure(IllegalStateException("Session expired. Please log in again."))
    }
    val req = Request.Builder()
      .url("${SupabaseConfig.url}/rest/v1/rpc/$rpcName")
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(payload.toString().toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (resp.isSuccessful) Result.success(body)
        else Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Network request failed")))
    }
  }

  suspend fun listQuizzes(classId: String, accessToken: String): Result<List<ChallengeItem>> {
    val payload = JSONObject().apply { put("target_class_id", classId) }
    val rpcResult = postRpc("list_quizzes", payload, accessToken)
    if (rpcResult.isFailure) return Result.failure(rpcResult.exceptionOrNull()!!)

    val creatorMap = fetchQuizCreators(classId, accessToken).getOrDefault(emptyMap())
    return runCatching {
      parseQuizzes(rpcResult.getOrThrow()).map { item ->
        val creator = creatorMap[item.quizId]
        if (creator != null) item.copy(createdBy = creator) else item
      }
    }
  }

  private suspend fun fetchQuizCreators(classId: String, accessToken: String): Result<Map<String, String>> =
    withContext(Dispatchers.IO) {
      if (!SupabaseConfig.isConfigured() || classId.isBlank() || accessToken.isBlank()) {
        return@withContext Result.success(emptyMap())
      }
      val req = Request.Builder()
        .url("${SupabaseConfig.url}/rest/v1/quizzes?class_id=eq.$classId&select=id,created_by")
        .addHeader("apikey", SupabaseConfig.anonKey)
        .addHeader("Authorization", "Bearer $accessToken")
        .addHeader("Accept", "application/json")
        .get()
        .build()
      runCatching {
        client.newCall(req).execute().use { resp ->
          if (!resp.isSuccessful) return@use emptyMap()
          parseCreatedByMap(resp.body?.string().orEmpty())
        }
      }
    }

  suspend fun createQuiz(
    classId: String,
    title: String,
    description: String,
    opensAtIso: String,
    closesAtIso: String,
    timeLimitMinutes: Int?,
    questions: List<DraftQuizQuestion>,
    courseId: String?,
    asExam: Boolean = false,
    shuffleQuestions: Boolean = false,
    answerFormat: ExamAnswerFormat = ExamAnswerFormat.CHOICES,
    securityConfig: ExamSecurityConfig = ExamSecurityConfig(),
    accessToken: String
  ): Result<String> {
    val payload = buildCreateQuizPayload(
      classId = classId,
      title = title,
      description = description,
      opensAtIso = opensAtIso,
      closesAtIso = closesAtIso,
      timeLimitMinutes = timeLimitMinutes,
      questions = questions,
      courseId = courseId,
      asExam = asExam,
      shuffleQuestions = shuffleQuestions,
      answerFormat = answerFormat,
      securityConfig = securityConfig
    )
    return postRpc("create_quiz", payload, accessToken).map { body ->
      body.trim().trim('"')
    }
  }

  suspend fun addQuizQuestions(
    quizId: String,
    questions: List<DraftQuizQuestion>,
    accessToken: String
  ): Result<Int> {
    val payload = JSONObject().apply {
      put("target_quiz_id", quizId)
      put("questions", buildQuestionsJsonArray(questions))
    }
    return postRpc("add_quiz_questions", payload, accessToken).mapCatching { body ->
      body.trim().trim('"').toIntOrNull() ?: questions.size
    }
  }

  suspend fun startQuiz(quizId: String, accessToken: String): Result<List<PlayQuestion>> {
    val payload = JSONObject().apply { put("target_quiz_id", quizId) }
    return postRpc("start_quiz", payload, accessToken).mapCatching { parseStartQuizQuestions(it) }
  }

  suspend fun reportExamViolation(
    quizId: String,
    violationKind: String = "left_app",
    accessToken: String
  ): Result<SecurityEventResult> {
    val payload = JSONObject().apply {
      put("target_quiz_id", quizId)
      put("violation_kind", violationKind.ifBlank { "left_app" }.take(30))
    }
    return postRpc("report_exam_violation", payload, accessToken).mapCatching { body ->
      parseViolationResult(body)
    }
  }

  suspend fun saveExamProgress(
    quizId: String,
    questionsInOrder: List<PlayQuestion>,
    selectedAnswersByOrd: Map<Int, Int>,
    writtenAnswersByOrd: Map<Int, String>,
    accessToken: String
  ): Result<Unit> {
    val payload = buildSaveProgressPayload(
      quizId = quizId,
      questionsInOrder = questionsInOrder,
      selectedAnswersByOrd = selectedAnswersByOrd,
      writtenAnswersByOrd = writtenAnswersByOrd
    )
    return postRpc("save_exam_progress", payload, accessToken).map { }
  }

  suspend fun getExamProgress(quizId: String, accessToken: String): Result<SavedExamProgress?> {
    val payload = JSONObject().apply { put("target_quiz_id", quizId) }
    return postRpc("get_exam_progress", payload, accessToken).mapCatching { parseSavedExamProgress(it) }
  }

  suspend fun canMarkQuiz(quizId: String, accessToken: String): Result<Boolean> {
    val payload = JSONObject().apply { put("p_quiz", quizId) }
    return postRpc("can_mark_quiz", payload, accessToken).mapCatching { body ->
      val clean = body.trim().trim('"').lowercase()
      clean == "true"
    }
  }

  suspend fun listExamFlags(quizId: String, accessToken: String): Result<List<ExamFlagItem>> {
    val payload = JSONObject().apply { put("target_quiz_id", quizId) }
    return postRpc("list_exam_flags", payload, accessToken).mapCatching { parseExamFlags(it) }
  }

  suspend fun submitQuiz(
    quizId: String,
    questionsInOrder: List<PlayQuestion>,
    selectedAnswersByOrd: Map<Int, Int>,
    writtenAnswersByOrd: Map<Int, String> = emptyMap(),
    accessToken: String
  ): Result<QuizSubmissionResult> {
    val payload = buildSubmitQuizPayload(
      quizId = quizId,
      questionsInOrder = questionsInOrder,
      selectedAnswersByOrd = selectedAnswersByOrd,
      writtenAnswersByOrd = writtenAnswersByOrd
    )
    return postRpc("submit_quiz", payload, accessToken).mapCatching { parseSubmitQuizResult(it) }
  }

  suspend fun getQuizReview(quizId: String, accessToken: String): Result<List<QuizReviewItem>> {
    val payload = JSONObject().apply { put("target_quiz_id", quizId) }
    return postRpc("get_quiz_review", payload, accessToken).mapCatching { parseQuizReview(it) }
  }

  suspend fun getQuizResults(quizId: String, accessToken: String): Result<List<QuizResultRow>> {
    val payload = JSONObject().apply { put("target_quiz_id", quizId) }
    return postRpc("quiz_results", payload, accessToken).mapCatching { parseQuizResults(it) }
  }

  suspend fun deleteQuiz(quizId: String, accessToken: String): Result<Unit> {
    val payload = JSONObject().apply { put("target_quiz_id", quizId) }
    return postRpc("delete_quiz", payload, accessToken).map { }
  }

  suspend fun getMyActivityStats(classId: String, accessToken: String): Result<ActivityStats> {
    val payload = JSONObject().apply { put("target_class_id", classId) }
    return postRpc("my_activity_stats", payload, accessToken).mapCatching { parseActivityStats(it) }
  }

  suspend fun getClassLeaderboard(
    classId: String,
    period: LeaderboardPeriod,
    maxRows: Int = 10,
    accessToken: String
  ): Result<List<LeaderboardEntry>> {
    val payload = JSONObject().apply {
      put("target_class_id", classId)
      put("period", period.apiValue)
      put("max_rows", maxRows)
    }
    return postRpc("class_leaderboard", payload, accessToken).mapCatching { parseLeaderboard(it) }
  }

  suspend fun setLeaderboardVisibility(visible: Boolean, accessToken: String): Result<Unit> {
    val payload = JSONObject().apply { put("visible", visible) }
    return postRpc("set_leaderboard_visibility", payload, accessToken).map { }
  }

  companion object {
    fun buildQuestionsJsonArray(questions: List<DraftQuizQuestion>): JSONArray {
      return JSONArray().apply {
        questions.forEach { q ->
          val cleanMarks = q.marks.coerceIn(0.5, 100.0)
          put(
            JSONObject().apply {
              put("type", q.questionType.apiValue)
              put("question", q.question.trim())
              put("marks", cleanMarks)
              if (q.timeLimitSeconds != null && q.timeLimitSeconds >= 5L) {
                put("time_limit_seconds", q.timeLimitSeconds)
              }
              if (q.questionType == QuestionType.MCQ) {
                val cleanedOpts = q.options.map { it.trim() }.filter { it.isNotBlank() }
                val optsJson = JSONArray().apply { cleanedOpts.forEach { put(it) } }
                put("options", optsJson)
                put("correct_index", q.correctIndex.coerceIn(0, (cleanedOpts.size - 1).coerceAtLeast(0)))
              } else {
                if (q.modelAnswer.isNotBlank()) {
                  put("model_answer", q.modelAnswer.trim())
                }
              }
              if (q.explanation.isNotBlank()) put("explanation", q.explanation.trim())
            }
          )
        }
      }
    }

    fun buildCreateQuizPayload(
      classId: String,
      title: String,
      description: String,
      opensAtIso: String,
      closesAtIso: String,
      timeLimitMinutes: Int?,
      questions: List<DraftQuizQuestion>,
      courseId: String?,
      asExam: Boolean = false,
      shuffleQuestions: Boolean = false,
      answerFormat: ExamAnswerFormat = ExamAnswerFormat.CHOICES,
      securityConfig: ExamSecurityConfig = ExamSecurityConfig()
    ): JSONObject {
      val rawSecs = securityConfig.timeLimitSeconds?.takeIf { it > 0L }
        ?: timeLimitMinutes?.takeIf { it > 0 }?.toLong()?.times(60L)
      val totalSecs = rawSecs?.coerceIn(30L, 86_400L)
      val legacyMins = totalSecs?.let { kotlin.math.ceil(it / 60.0).toInt().coerceIn(1, 1440) }
      val maxViolations: Any = when (securityConfig.onExitAction) {
        ExamExitAction.OFF -> JSONObject.NULL
        ExamExitAction.AUTO_SUBMIT -> 1
        ExamExitAction.WARN -> securityConfig.maxExitViolations.coerceIn(1, 50)
      }
      return JSONObject().apply {
        put("target_class_id", classId)
        put("quiz_title", title.trim())
        put("quiz_description", description.trim())
        put("quiz_opens_at", opensAtIso)
        put("quiz_closes_at", closesAtIso)
        if (legacyMins != null) put("time_limit", legacyMins) else put("time_limit", JSONObject.NULL)
        put("questions", buildQuestionsJsonArray(questions))
        if (!courseId.isNullOrBlank()) put("target_course_id", courseId) else put("target_course_id", JSONObject.NULL)
        put("as_exam", asExam)
        put("shuffle", shuffleQuestions)
        put("answer_format", answerFormat.apiValue)
        if (totalSecs != null) put("p_time_limit_seconds", totalSecs) else put("p_time_limit_seconds", JSONObject.NULL)
        val perQ = securityConfig.perQuestionSeconds?.takeIf { it >= 5L }?.coerceIn(5L, 3600L)
        if (perQ != null) put("p_per_question_seconds", perQ) else put("p_per_question_seconds", JSONObject.NULL)
        put("p_countdown_mode", securityConfig.countdownMode.apiValue)
        put("p_countdown_style", securityConfig.countdownStyle.apiValue)
        put("p_one_way_navigation", securityConfig.oneWayNavigation)
        put("p_lock_on_answer", securityConfig.lockOnAnswer)
        put("p_no_copy_paste", securityConfig.noCopyPaste)
        put("p_on_exit_action", securityConfig.onExitAction.apiValue)
        put("p_max_exit_violations", maxViolations)
        put("p_strict_deadline", securityConfig.strictDeadline)
      }
    }

    fun buildSaveProgressPayload(
      quizId: String,
      questionsInOrder: List<PlayQuestion>,
      selectedAnswersByOrd: Map<Int, Int>,
      writtenAnswersByOrd: Map<Int, String> = emptyMap()
    ): JSONObject {
      val maxOrd = (questionsInOrder.maxOfOrNull { it.ord } ?: questionsInOrder.size).coerceAtMost(200)
      val ordered = JSONArray()
      for (ordIndex in 1..maxOrd) {
        ordered.put(selectedAnswersByOrd[ordIndex] ?: -1)
      }
      val writtenObj = JSONObject()
      writtenAnswersByOrd.forEach { (ord, text) ->
        val clean = text.take(8000)
        if (clean.isNotBlank()) {
          writtenObj.put(ord.toString(), clean)
        }
      }
      return JSONObject().apply {
        put("target_quiz_id", quizId)
        put("chosen_answers", ordered)
        put("written_answers", writtenObj)
      }
    }

    fun buildSubmitQuizPayload(
      quizId: String,
      questionsInOrder: List<PlayQuestion>,
      selectedAnswersByOrd: Map<Int, Int>,
      writtenAnswersByOrd: Map<Int, String> = emptyMap()
    ): JSONObject {
      val maxOrd = questionsInOrder.maxOfOrNull { it.ord } ?: questionsInOrder.size
      val ordered = JSONArray()
      for (ordIndex in 1..maxOrd) {
        ordered.put(selectedAnswersByOrd[ordIndex] ?: -1)
      }
      val writtenObj = JSONObject()
      writtenAnswersByOrd.forEach { (ord, text) ->
        val clean = text.take(8000)
        if (clean.isNotBlank()) {
          writtenObj.put(ord.toString(), clean)
        }
      }
      return JSONObject().apply {
        put("target_quiz_id", quizId)
        put("chosen_answers", ordered)
        put("written_answers", writtenObj)
      }
    }

    fun computeRemainingSeconds(
      startedAtIso: String?,
      closesAtIso: String?,
      timeLimitSeconds: Long?,
      nowMillis: Long = System.currentTimeMillis()
    ): Long? {
      if (timeLimitSeconds == null || timeLimitSeconds <= 0L) return null
      val startMillis = runCatching { Instant.parse(startedAtIso?.trim()).toEpochMilli() }.getOrNull() ?: return null
      val limitEnd = startMillis + timeLimitSeconds * 1000L
      val closeMillis = runCatching { Instant.parse(closesAtIso?.trim()).toEpochMilli() }.getOrNull()
      val effectiveEnd = if (closeMillis != null) minOf(limitEnd, closeMillis) else limitEnd
      return ((effectiveEnd - nowMillis) / 1000L).coerceAtLeast(0L)
    }

    fun computeClosingRemainingSeconds(
      closesAtIso: String?,
      nowMillis: Long = System.currentTimeMillis()
    ): Long? {
      val closeMillis = runCatching { Instant.parse(closesAtIso?.trim()).toEpochMilli() }.getOrNull() ?: return null
      return ((closeMillis - nowMillis) / 1000L).coerceAtLeast(0L)
    }

    fun parseQuizzes(body: String): List<ChallengeItem> {
      val trimmed = body.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyList()
      val arr = JSONArray(trimmed)
      return buildList {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          val myScore = JsonHelper.doubleOrNull(o, "my_score")
          val myTotal = JsonHelper.doubleOrNull(o, "my_total")
          val totalMarks = JsonHelper.doubleOrNull(o, "total_marks")
            ?: JsonHelper.doubleOrNull(o, "question_count")
            ?: 0.0
          val limitMins = if (o.has("time_limit_minutes") && !o.isNull("time_limit_minutes")) {
            o.optInt("time_limit_minutes").takeIf { it > 0 }
          } else null
          val limitSecs = if (o.has("time_limit_seconds") && !o.isNull("time_limit_seconds")) {
            o.optLong("time_limit_seconds").takeIf { it > 0L }
          } else limitMins?.toLong()?.times(60L)
          val perQSecs = if (o.has("per_question_seconds") && !o.isNull("per_question_seconds")) {
            o.optLong("per_question_seconds").takeIf { it > 0L }
          } else null
          add(
            ChallengeItem(
              quizId = JsonHelper.firstStr(o, "quiz_id", "id"),
              title = JsonHelper.str(o, "title").ifBlank { "Challenge" },
              description = JsonHelper.str(o, "description"),
              unitCode = JsonHelper.str(o, "unit_code").uppercase(),
              opensAt = JsonHelper.str(o, "opens_at"),
              closesAt = JsonHelper.str(o, "closes_at"),
              timeLimitMinutes = limitMins,
              timeLimitSeconds = limitSecs,
              perQuestionSeconds = perQSecs,
              countdownMode = CountdownMode.fromApi(JsonHelper.strOrNull(o, "countdown_mode")),
              countdownStyle = CountdownStyle.fromApi(JsonHelper.strOrNull(o, "countdown_style")),
              oneWayNavigation = JsonHelper.bool(o, "one_way_navigation", false),
              lockOnAnswer = JsonHelper.bool(o, "lock_on_answer", false),
              noCopyPaste = JsonHelper.bool(o, "no_copy_paste", false),
              onExitAction = ExamExitAction.fromApi(JsonHelper.strOrNull(o, "on_exit_action")),
              maxExitViolations = if (o.has("max_exit_violations") && !o.isNull("max_exit_violations")) {
                o.optInt("max_exit_violations", 2).coerceIn(1, 50)
              } else 2,
              strictDeadline = JsonHelper.bool(o, "strict_deadline", false),
              questionCount = JsonHelper.intVal(o, "question_count", 0),
              status = QuizStatus.fromApi(JsonHelper.str(o, "status")),
              attempted = JsonHelper.bool(o, "attempted", false),
              myScore = myScore,
              myTotal = myTotal,
              attemptsCount = JsonHelper.intVal(o, "attempts_count", 0),
              isExam = JsonHelper.bool(o, "is_exam", false),
              answerFormat = ExamAnswerFormat.fromApi(JsonHelper.str(o, "answer_format")),
              totalMarks = totalMarks,
              myMarkingStatus = MarkingStatus.fromApiOrNull(JsonHelper.strOrNull(o, "my_marking_status")),
              pdfDownloadedAt = JsonHelper.strOrNull(o, "pdf_downloaded_at")
            )
          )
        }
      }
    }

    fun parseCreatedByMap(body: String): Map<String, String> {
      val trimmed = body.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyMap()
      val arr = JSONArray(trimmed)
      return buildMap {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          val id = JsonHelper.str(o, "id")
          val cb = JsonHelper.str(o, "created_by")
          if (id.isNotBlank() && cb.isNotBlank()) put(id, cb)
        }
      }
    }

    fun parseStartQuizQuestions(body: String): List<PlayQuestion> {
      val trimmed = body.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyList()
      val arr = JSONArray(trimmed)
      return buildList {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          val limitMins = if (o.has("time_limit_minutes") && !o.isNull("time_limit_minutes")) {
            o.optInt("time_limit_minutes").takeIf { it > 0 }
          } else null
          val limitSecs = if (o.has("time_limit_seconds") && !o.isNull("time_limit_seconds")) {
            o.optLong("time_limit_seconds").takeIf { it > 0L }
          } else limitMins?.toLong()?.times(60L)
          val perQSecs = if (o.has("per_question_seconds") && !o.isNull("per_question_seconds")) {
            o.optLong("per_question_seconds").takeIf { it > 0L }
          } else null
          val qLimitSecs = if (o.has("question_time_limit_seconds") && !o.isNull("question_time_limit_seconds")) {
            o.optLong("question_time_limit_seconds").takeIf { it > 0L }
          } else perQSecs
          val lockedArr = o.optJSONArray("locked_question_ords")
          val lockedOrds = buildList {
            if (lockedArr != null) {
              for (k in 0 until lockedArr.length()) {
                val ordVal = lockedArr.optInt(k, -1)
                if (ordVal > 0) add(ordVal)
              }
            }
          }
          add(
            PlayQuestion(
              questionId = JsonHelper.str(o, "question_id"),
              ord = JsonHelper.intVal(o, "ord", i + 1),
              questionType = QuestionType.fromApi(JsonHelper.strOrNull(o, "question_type")),
              question = JsonHelper.str(o, "question"),
              options = parseStringList(o.optJSONArray("options")),
              marks = JsonHelper.doubleVal(o, "marks", 1.0),
              timeLimitMinutes = limitMins,
              timeLimitSeconds = limitSecs,
              perQuestionSeconds = perQSecs,
              questionTimeLimitSeconds = qLimitSecs,
              countdownMode = JsonHelper.strOrNull(o, "countdown_mode")?.let { CountdownMode.fromApi(it) },
              countdownStyle = JsonHelper.strOrNull(o, "countdown_style")?.let { CountdownStyle.fromApi(it) },
              oneWayNavigation = if (o.has("one_way_navigation") && !o.isNull("one_way_navigation")) o.optBoolean("one_way_navigation") else null,
              lockOnAnswer = if (o.has("lock_on_answer") && !o.isNull("lock_on_answer")) o.optBoolean("lock_on_answer") else null,
              noCopyPaste = if (o.has("no_copy_paste") && !o.isNull("no_copy_paste")) o.optBoolean("no_copy_paste") else null,
              onExitAction = JsonHelper.strOrNull(o, "on_exit_action")?.let { ExamExitAction.fromApi(it) },
              maxExitViolations = if (o.has("max_exit_violations") && !o.isNull("max_exit_violations")) {
                o.optInt("max_exit_violations").coerceIn(1, 50)
              } else null,
              strictDeadline = if (o.has("strict_deadline") && !o.isNull("strict_deadline")) o.optBoolean("strict_deadline") else null,
              startedAt = JsonHelper.strOrNull(o, "started_at"),
              serverNow = JsonHelper.strOrNull(o, "server_now"),
              lockedQuestionOrds = lockedOrds,
              initialFocusLossCount = JsonHelper.intVal(o, "focus_loss_count", 0)
            )
          )
        }
      }
    }

    fun parseViolationResult(body: String): SecurityEventResult {
      val trimmed = body.trim()
      val o = when {
        trimmed.startsWith("[") -> JSONArray(trimmed).optJSONObject(0) ?: JSONObject()
        trimmed.startsWith("{") -> JSONObject(trimmed)
        else -> JSONObject()
      }
      val maxViolations = when {
        o.has("max_violations") && !o.isNull("max_violations") -> o.optInt("max_violations", 2).coerceIn(1, 50)
        o.has("max_exit_violations") && !o.isNull("max_exit_violations") -> o.optInt("max_exit_violations", 2).coerceIn(1, 50)
        else -> 0
      }
      val violations = if (o.has("violations")) JsonHelper.intVal(o, "violations", 0)
      else JsonHelper.intVal(o, "focus_loss_count", 0)
      val autoSubmitted = if (o.has("auto_submitted")) JsonHelper.bool(o, "auto_submitted", false)
      else JsonHelper.bool(o, "should_auto_submit", false)
      return SecurityEventResult(
        focusLossCount = violations,
        maxExitViolations = maxViolations,
        shouldAutoSubmit = autoSubmitted
      )
    }

    fun parseSavedExamProgress(body: String): SavedExamProgress? {
      val trimmed = body.trim()
      val o = when {
        trimmed.startsWith("[") -> JSONArray(trimmed).optJSONObject(0)
        trimmed.startsWith("{") -> JSONObject(trimmed)
        else -> null
      } ?: return null
      val chosenMap = mutableMapOf<Int, Int>()
      val chosenArr = o.optJSONArray("chosen_answers")
      if (chosenArr != null) {
        for (idx in 0 until chosenArr.length()) {
          val v = chosenArr.optInt(idx, -1)
          if (v >= 0) chosenMap[idx + 1] = v
        }
      }
      val writtenMap = mutableMapOf<Int, String>()
      val writtenObj = o.optJSONObject("written_answers")
      if (writtenObj != null) {
        val keys = writtenObj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          val ord = k.toIntOrNull() ?: continue
          val txt = writtenObj.optString(k, "")
          if (txt.isNotBlank() && !txt.equals("null", ignoreCase = true)) {
            writtenMap[ord] = txt
          }
        }
      }
      return SavedExamProgress(
        chosenAnswersByOrd = chosenMap,
        writtenAnswersByOrd = writtenMap,
        savedAt = JsonHelper.strOrNull(o, "saved_at"),
        exitViolations = JsonHelper.intVal(o, "exit_violations", 0)
      )
    }

    fun parseExamFlags(body: String): List<ExamFlagItem> {
      val trimmed = body.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyList()
      val arr = JSONArray(trimmed)
      return buildList {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          add(
            ExamFlagItem(
              studentId = JsonHelper.str(o, "student_id"),
              fullName = JsonHelper.str(o, "full_name").ifBlank { "Student" },
              admissionNumber = JsonHelper.orDash(JsonHelper.str(o, "admission_number")),
              exitViolations = JsonHelper.intVal(o, "exit_violations", 0),
              autoSubmitted = JsonHelper.bool(o, "auto_submitted", false),
              autoSubmitReason = JsonHelper.str(o, "auto_submit_reason"),
              late = JsonHelper.bool(o, "late", false),
              submittedAt = JsonHelper.str(o, "submitted_at")
            )
          )
        }
      }
    }

    fun parseSubmitQuizResult(body: String): QuizSubmissionResult {
      val trimmed = body.trim()
      val o = when {
        trimmed.startsWith("[") -> JSONArray(trimmed).optJSONObject(0) ?: JSONObject()
        trimmed.startsWith("{") -> JSONObject(trimmed)
        else -> JSONObject()
      }
      return QuizSubmissionResult(
        score = JsonHelper.doubleVal(o, "score", 0.0),
        total = JsonHelper.doubleVal(o, "total", 0.0),
        pointsAwarded = JsonHelper.intVal(o, "points_awarded", 0),
        needsMarking = JsonHelper.bool(o, "needs_marking", false)
      )
    }

    fun parseQuizReview(body: String): List<QuizReviewItem> {
      val trimmed = body.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyList()
      val arr = JSONArray(trimmed)
      return buildList {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          add(
            QuizReviewItem(
              ord = JsonHelper.intVal(o, "ord", i + 1),
              questionType = QuestionType.fromApi(JsonHelper.strOrNull(o, "question_type")),
              question = JsonHelper.str(o, "question"),
              options = parseStringList(o.optJSONArray("options")),
              correctIndex = JsonHelper.intVal(o, "correct_index", 0),
              chosenIndex = JsonHelper.intVal(o, "chosen_index", -1),
              writtenAnswer = JsonHelper.str(o, "written_answer"),
              marks = JsonHelper.doubleVal(o, "marks", 1.0),
              awardedMarks = JsonHelper.doubleOrNull(o, "awarded_marks"),
              markerComment = JsonHelper.str(o, "marker_comment"),
              explanation = JsonHelper.str(o, "explanation")
            )
          )
        }
      }.sortedBy { it.ord }
    }

    fun parseQuizResults(body: String): List<QuizResultRow> {
      val trimmed = body.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyList()
      val arr = JSONArray(trimmed)
      return buildList {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          add(
            QuizResultRow(
              studentId = JsonHelper.str(o, "student_id"),
              fullName = JsonHelper.str(o, "full_name").ifBlank { "Student" },
              admissionNumber = JsonHelper.orDash(JsonHelper.str(o, "admission_number")),
              score = JsonHelper.doubleOrNull(o, "score"),
              total = JsonHelper.doubleVal(o, "total", 0.0),
              submittedAt = JsonHelper.str(o, "submitted_at"),
              markingStatus = MarkingStatus.fromApi(JsonHelper.strOrNull(o, "marking_status")),
              late = JsonHelper.bool(o, "late", false),
              mcqMarks = JsonHelper.doubleOrNull(o, "mcq_marks"),
              writtenMarks = JsonHelper.doubleOrNull(o, "written_marks"),
              focusLossCount = JsonHelper.intVal(o, "focus_loss_count", 0),
              autoSubmittedReason = JsonHelper.str(o, "auto_submitted_reason")
            )
          )
        }
      }
    }

    fun parseActivityStats(body: String): ActivityStats {
      val trimmed = body.trim()
      val o = when {
        trimmed.startsWith("[") -> JSONArray(trimmed).optJSONObject(0) ?: JSONObject()
        trimmed.startsWith("{") -> JSONObject(trimmed)
        else -> JSONObject()
      }
      val rank = if (o.has("week_rank") && !o.isNull("week_rank")) {
        o.optInt("week_rank").takeIf { it > 0 }
      } else null
      return ActivityStats(
        totalPoints = JsonHelper.intVal(o, "total_points", 0),
        weekPoints = JsonHelper.intVal(o, "week_points", 0),
        streakDays = JsonHelper.intVal(o, "streak_days", 0),
        bestStreak = JsonHelper.intVal(o, "best_streak", 0),
        weekRank = rank,
        rankedStudents = JsonHelper.intVal(o, "ranked_students", 0)
      )
    }

    fun parseLeaderboard(body: String): List<LeaderboardEntry> {
      val trimmed = body.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyList()
      val arr = JSONArray(trimmed)
      return buildList {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          add(
            LeaderboardEntry(
              rankNo = JsonHelper.intVal(o, "rank_no", i + 1),
              displayName = JsonHelper.str(o, "display_name").ifBlank { "Anonymous" },
              points = JsonHelper.intVal(o, "points", 0),
              isMe = JsonHelper.bool(o, "is_me", false)
            )
          )
        }
      }
    }

    private fun parseStringList(arr: JSONArray?): List<String> {
      if (arr == null) return emptyList()
      return buildList {
        for (i in 0 until arr.length()) {
          val s = arr.optString(i, "").trim()
          if (s.isNotBlank() && !s.equals("null", ignoreCase = true)) add(s)
        }
      }
    }
  }
}
