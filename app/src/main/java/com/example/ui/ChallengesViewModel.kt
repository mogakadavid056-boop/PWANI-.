package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChallengeItem
import com.example.data.ChallengesService
import com.example.data.CountdownMode
import com.example.data.DraftQuizQuestion
import com.example.data.ExamAnswerFormat
import com.example.data.ExamExitAction
import com.example.data.ExamMarkingService
import com.example.data.ExamSecurityConfig
import com.example.data.InkStroke
import com.example.data.LeaderboardPeriod
import com.example.data.MarkingDraftData
import com.example.data.MarkingStamp
import com.example.data.MarkingStatus
import com.example.data.PlayQuestion
import com.example.data.QuestionMarkInput
import com.example.data.QuestionMarkingDraft
import com.example.data.QuestionType
import com.example.data.QuizResultRow
import com.example.data.QuizStatusFilter
import com.example.data.UserSession
import com.example.util.MarkedPaperPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

class ChallengesViewModel(
  private val service: ChallengesService = ChallengesService(),
  private val markingService: ExamMarkingService = ExamMarkingService()
) : ViewModel() {

  private val _uiState = MutableStateFlow(ChallengesUiState())
  val uiState: StateFlow<ChallengesUiState> = _uiState.asStateFlow()

  private var countdownJob: Job? = null
  private var activeSessionForTimer: UserSession? = null
  private var lastViolationEventAtMs: Long = 0L

  fun loadClassChallengesData(classId: String, session: UserSession?) {
    val token = session?.accessToken.orEmpty()
    if (classId.isBlank() || token.isBlank()) return
    _uiState.update { it.copy(classId = classId, errorMessage = null) }
    loadQuizzes(classId, session)
    loadActivityStats(classId, session)
    loadLeaderboard(classId, _uiState.value.leaderboardPeriod, session)
  }

  fun loadQuizzes(classId: String = _uiState.value.classId, session: UserSession?) {
    val token = session?.accessToken.orEmpty()
    if (classId.isBlank() || token.isBlank()) return
    _uiState.update { it.copy(classId = classId, isLoadingQuizzes = true, errorMessage = null) }
    viewModelScope.launch {
      service.listQuizzes(classId, token).fold(
        onSuccess = { list -> _uiState.update { it.copy(quizzes = list, isLoadingQuizzes = false) } },
        onFailure = { err -> _uiState.update { it.copy(isLoadingQuizzes = false, errorMessage = err.message) } }
      )
    }
  }

  fun loadActivityStats(classId: String = _uiState.value.classId, session: UserSession?) {
    val token = session?.accessToken.orEmpty()
    if (classId.isBlank() || token.isBlank()) return
    _uiState.update { it.copy(isLoadingStats = true) }
    viewModelScope.launch {
      service.getMyActivityStats(classId, token).fold(
        onSuccess = { stats -> _uiState.update { it.copy(activityStats = stats, isLoadingStats = false) } },
        onFailure = { _uiState.update { it.copy(isLoadingStats = false) } }
      )
    }
  }

  fun loadLeaderboard(
    classId: String = _uiState.value.classId,
    period: LeaderboardPeriod = _uiState.value.leaderboardPeriod,
    session: UserSession?
  ) {
    val token = session?.accessToken.orEmpty()
    if (classId.isBlank() || token.isBlank()) return
    _uiState.update { it.copy(classId = classId, leaderboardPeriod = period, isLoadingLeaderboard = true) }
    viewModelScope.launch {
      service.getClassLeaderboard(classId, period, 10, token).fold(
        onSuccess = { rows -> _uiState.update { it.copy(leaderboardRows = rows, isLoadingLeaderboard = false) } },
        onFailure = { err -> _uiState.update { it.copy(isLoadingLeaderboard = false, errorMessage = err.message) } }
      )
    }
  }

  fun loadPapersToMark(session: UserSession?) {
    val token = session?.accessToken.orEmpty()
    if (token.isBlank()) return
    _uiState.update { it.copy(isLoadingPapersToMark = true) }
    viewModelScope.launch {
      markingService.listPapersToMark(token).fold(
        onSuccess = { items -> _uiState.update { it.copy(papersToMark = items, isLoadingPapersToMark = false) } },
        onFailure = { _uiState.update { it.copy(isLoadingPapersToMark = false) } }
      )
    }
  }

  fun selectFilter(filter: QuizStatusFilter) = _uiState.update { it.copy(selectedFilter = filter) }
  fun selectSubmissionFilter(filter: SubmissionFilter) = _uiState.update { it.copy(submissionsFilter = filter) }
  fun selectSubmissionSortOrder(sortOrder: SubmissionSortOrder) = _uiState.update { it.copy(submissionsSortOrder = sortOrder) }
  fun toggleSubmissionsWaitingFirst() = _uiState.update { it.copy(submissionsWaitingFirst = !it.submissionsWaitingFirst) }
  fun updateSubmissionsSearchQuery(query: String) = _uiState.update { it.copy(submissionsSearchQuery = query) }

  fun selectLeaderboardPeriod(period: LeaderboardPeriod, session: UserSession?) {
    val cid = _uiState.value.classId
    if (cid.isNotBlank()) loadLeaderboard(cid, period, session)
  }

  fun setShowCreateDialog(show: Boolean) = _uiState.update {
    it.copy(
      showCreateDialog = show,
      challengeDraftPrefill = if (show) it.challengeDraftPrefill else null,
      errorMessage = null
    )
  }

  fun openCreateDialogWithDraft(prefill: com.example.assistant.ChallengeDraftPrefill) = _uiState.update {
    it.copy(
      classId = prefill.classId.ifBlank { it.classId },
      showCreateDialog = true,
      challengeDraftPrefill = prefill,
      errorMessage = null
    )
  }

  fun setAddQuestionsQuiz(quiz: ChallengeItem?) = _uiState.update { it.copy(addQuestionsForQuiz = quiz, errorMessage = null) }

  fun createQuiz(
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
    session: UserSession?,
    onSuccess: () -> Unit = {}
  ) {
    val cid = _uiState.value.classId
    val token = session?.accessToken.orEmpty()
    if (cid.isBlank() || token.isBlank()) return
    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      service.createQuiz(
        classId = cid,
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
        securityConfig = securityConfig,
        accessToken = token
      ).fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              isActionLoading = false,
              showCreateDialog = false,
              challengeDraftPrefill = null,
              successMessage = if (asExam) "Exam published!" else "Challenge published!"
            )
          }
          loadQuizzes(cid, session)
          onSuccess()
        },
        onFailure = { err ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) }
        }
      )
    }
  }

  fun addQuizQuestions(quizId: String, questions: List<DraftQuizQuestion>, session: UserSession?) {
    val token = session?.accessToken.orEmpty()
    if (quizId.isBlank() || token.isBlank()) return
    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      service.addQuizQuestions(quizId, questions, token).fold(
        onSuccess = { totalCount ->
          _uiState.update {
            it.copy(isActionLoading = false, addQuestionsForQuiz = null, successMessage = "Questions added ($totalCount total).")
          }
          val cid = _uiState.value.classId
          if (cid.isNotBlank()) loadQuizzes(cid, session)
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun startQuiz(quiz: ChallengeItem, session: UserSession?) {
    val token = session?.accessToken.orEmpty()
    if (token.isBlank()) return
    stopCountdown()
    activeSessionForTimer = session
    lastViolationEventAtMs = 0L
    _uiState.update {
      it.copy(
        playingQuiz = quiz,
        playQuestions = emptyList(),
        playCurrentIndex = 0,
        playSelectedAnswers = emptyMap(),
        playWrittenAnswers = emptyMap(),
        playLockedQuestionOrds = emptySet(),
        playStartedAt = null,
        playTimeLimitMinutes = quiz.timeLimitMinutes,
        playTimeLimitSeconds = quiz.effectiveTimeLimitSeconds,
        playRemainingSeconds = null,
        playQuestionTotalSeconds = null,
        playQuestionRemainingSeconds = null,
        playFocusLossCount = 0,
        playSecurityWarning = null,
        playAutoSubmittedReason = null,
        submissionResult = null,
        isLoadingPlay = true,
        errorMessage = null
      )
    }
    viewModelScope.launch {
      service.startQuiz(quiz.quizId, token).fold(
        onSuccess = { questions ->
          val first = questions.firstOrNull()
          val mergedQuiz = if (first != null) {
            quiz.copy(
              timeLimitSeconds = first.timeLimitSeconds ?: quiz.effectiveTimeLimitSeconds,
              perQuestionSeconds = first.perQuestionSeconds ?: quiz.perQuestionSeconds,
              countdownMode = first.countdownMode,
              countdownStyle = first.countdownStyle,
              oneWayNavigation = first.oneWayNavigation,
              lockOnAnswer = first.lockOnAnswer,
              noCopyPaste = first.noCopyPaste,
              onExitAction = first.onExitAction,
              maxExitViolations = first.maxExitViolations,
              strictDeadline = first.strictDeadline
            )
          } else quiz
          val lockedSet = first?.lockedQuestionOrds?.toSet().orEmpty()
          val firstUnlockedIdx = questions.indexOfFirst { it.ord !in lockedSet }.coerceAtLeast(0)
          val startQ = questions.getOrNull(firstUnlockedIdx)
          val serverSkewMs = first?.serverNow?.let { rawNow ->
            runCatching { Instant.parse(rawNow.trim()).toEpochMilli() - System.currentTimeMillis() }.getOrDefault(0L)
          } ?: 0L
          val initialOverall = computeOverallRemaining(mergedQuiz, first?.startedAt, serverSkewMs)
          val qSecs = startQ?.questionTimeLimitSeconds ?: mergedQuiz.perQuestionSeconds

          _uiState.update {
            it.copy(
              playingQuiz = mergedQuiz,
              playQuestions = questions,
              playCurrentIndex = firstUnlockedIdx,
              playLockedQuestionOrds = lockedSet,
              playStartedAt = first?.startedAt,
              playTimeLimitMinutes = first?.timeLimitMinutes ?: mergedQuiz.timeLimitMinutes,
              playTimeLimitSeconds = mergedQuiz.effectiveTimeLimitSeconds,
              playRemainingSeconds = initialOverall,
              playQuestionTotalSeconds = qSecs,
              playQuestionRemainingSeconds = if (mergedQuiz.countdownMode == CountdownMode.PER_QUESTION ||
                mergedQuiz.countdownMode == CountdownMode.BOTH || qSecs != null
              ) qSecs else null,
              playFocusLossCount = first?.initialFocusLossCount ?: 0,
              isLoadingPlay = false
            )
          }
          startCountdownLoop(mergedQuiz, first?.startedAt, serverSkewMs, session)
        },
        onFailure = { err ->
          _uiState.update { it.copy(isLoadingPlay = false, errorMessage = err.message) }
        }
      )
    }
  }

  private fun computeOverallRemaining(
    quiz: ChallengeItem,
    startedAt: String?,
    serverSkewMs: Long
  ): Long? {
    val nowMs = System.currentTimeMillis() + serverSkewMs
    return when {
      quiz.countdownMode == CountdownMode.CLOSING_WINDOW ->
        ChallengesService.computeClosingRemainingSeconds(quiz.closesAt, nowMs)
      quiz.effectiveTimeLimitSeconds != null ->
        ChallengesService.computeRemainingSeconds(startedAt, quiz.closesAt, quiz.effectiveTimeLimitSeconds, nowMs)
      else -> null
    }
  }

  private fun startCountdownLoop(
    quiz: ChallengeItem,
    startedAt: String?,
    serverSkewMs: Long,
    session: UserSession?
  ) {
    countdownJob?.cancel()
    val hasOverall = quiz.countdownMode == CountdownMode.CLOSING_WINDOW || quiz.effectiveTimeLimitSeconds != null
    val hasPerQ = quiz.perQuestionSeconds != null ||
      _uiState.value.playQuestions.any { it.questionTimeLimitSeconds != null }
    if (!hasOverall && !hasPerQ) return

    countdownJob = viewModelScope.launch {
      while (isActive) {
        delay(1000L)
        val st = _uiState.value
        if (st.playingQuiz?.quizId != quiz.quizId || st.submissionResult != null) break

        val overallRem = if (hasOverall) computeOverallRemaining(quiz, startedAt, serverSkewMs) else null
        val curQRem = st.playQuestionRemainingSeconds?.let { (it - 1L).coerceAtLeast(0L) }

        _uiState.update {
          it.copy(
            playRemainingSeconds = overallRem,
            playQuestionRemainingSeconds = curQRem
          )
        }

        if (overallRem != null && overallRem <= 0L) {
          if (!st.isActionLoading) submitQuiz(session, autoSubmittedReason = "TIME_EXPIRED")
          break
        }

        if (curQRem != null && curQRem <= 0L) {
          handleQuestionTimerExpired(session)
        }
      }
    }
  }

  private fun handleQuestionTimerExpired(session: UserSession?) {
    val st = _uiState.value
    val quiz = st.playingQuiz ?: return
    val questions = st.playQuestions
    val currentQ = questions.getOrNull(st.playCurrentIndex) ?: return
    val newLocked = st.playLockedQuestionOrds + currentQ.ord
    val nextIdx = (st.playCurrentIndex + 1 until questions.size)
      .firstOrNull { questions[it].ord !in newLocked }

    if (nextIdx != null) {
      val nextQ = questions[nextIdx]
      val nextQSecs = nextQ.questionTimeLimitSeconds ?: quiz.perQuestionSeconds
      _uiState.update {
        it.copy(
          playLockedQuestionOrds = newLocked,
          playCurrentIndex = nextIdx,
          playQuestionTotalSeconds = nextQSecs,
          playQuestionRemainingSeconds = nextQSecs
        )
      }
      syncProgressInBackground(session)
    } else {
      _uiState.update { it.copy(playLockedQuestionOrds = newLocked, playQuestionRemainingSeconds = null) }
      if (!st.isActionLoading) submitQuiz(session, autoSubmittedReason = "QUESTION_TIME_ENDED")
    }
  }

  private fun stopCountdown() {
    countdownJob?.cancel()
    countdownJob = null
  }

  fun selectAnswer(questionOrd: Int, optionIndex: Int) {
    val st = _uiState.value
    if (questionOrd in st.playLockedQuestionOrds) return
    _uiState.update { state ->
      state.copy(playSelectedAnswers = state.playSelectedAnswers + (questionOrd to optionIndex))
    }
  }

  fun updateWrittenAnswer(questionOrd: Int, text: String) {
    val st = _uiState.value
    if (questionOrd in st.playLockedQuestionOrds) return
    val clipped = text.take(8000)
    _uiState.update { state ->
      val next = state.playWrittenAnswers.toMutableMap()
      if (clipped.isBlank()) next.remove(questionOrd) else next[questionOrd] = clipped
      state.copy(playWrittenAnswers = next)
    }
  }

  fun goToQuestion(index: Int, session: UserSession? = activeSessionForTimer) {
    val st = _uiState.value
    val quiz = st.playingQuiz ?: return
    val questions = st.playQuestions
    if (questions.isEmpty()) return
    val targetIdx = index.coerceIn(0, questions.lastIndex)
    if (targetIdx == st.playCurrentIndex) return

    // Enforce "no turning back": cannot jump backwards or into locked questions
    if (quiz.oneWayNavigation && targetIdx < st.playCurrentIndex) return
    val targetQ = questions[targetIdx]
    if (targetQ.ord in st.playLockedQuestionOrds) return

    val currentQ = questions.getOrNull(st.playCurrentIndex)
    val shouldLockLeaving = quiz.oneWayNavigation || quiz.lockOnAnswer
    val nextLocked = if (shouldLockLeaving && currentQ != null) {
      st.playLockedQuestionOrds + currentQ.ord
    } else {
      st.playLockedQuestionOrds
    }

    val nextQSecs = targetQ.questionTimeLimitSeconds ?: quiz.perQuestionSeconds
    val resetQTimer = if (quiz.countdownMode == CountdownMode.PER_QUESTION ||
      quiz.countdownMode == CountdownMode.BOTH || nextQSecs != null
    ) nextQSecs else null

    _uiState.update {
      it.copy(
        playCurrentIndex = targetIdx,
        playLockedQuestionOrds = nextLocked,
        playQuestionTotalSeconds = nextQSecs,
        playQuestionRemainingSeconds = resetQTimer
      )
    }
    syncProgressInBackground(session)
  }

  private fun syncProgressInBackground(session: UserSession?) {
    val st = _uiState.value
    val quiz = st.playingQuiz ?: return
    val token = session?.accessToken.orEmpty()
    if (token.isBlank() || st.submissionResult != null) return
    val curOrd = st.playQuestions.getOrNull(st.playCurrentIndex)?.ord ?: 1
    viewModelScope.launch {
      service.syncExamProgress(
        quizId = quiz.quizId,
        questionsInOrder = st.playQuestions,
        selectedAnswersByOrd = st.playSelectedAnswers,
        writtenAnswersByOrd = st.playWrittenAnswers,
        lockedQuestionOrds = st.playLockedQuestionOrds,
        currentOrd = curOrd,
        accessToken = token
      )
    }
  }

  fun onExamSecurityViolation(eventType: String, detail: String, session: UserSession?) {
    val st = _uiState.value
    val quiz = st.playingQuiz ?: return
    if (st.submissionResult != null || st.playQuestions.isEmpty()) return
    if (!quiz.isExam && quiz.onExitAction == ExamExitAction.OFF) return
    if (quiz.onExitAction == ExamExitAction.OFF) return

    val now = System.currentTimeMillis()
    if (now - lastViolationEventAtMs < 1500L) return
    lastViolationEventAtMs = now

    val token = session?.accessToken.orEmpty()
    val localNext = st.playFocusLossCount + 1
    _uiState.update {
      it.copy(
        playFocusLossCount = localNext,
        playSecurityWarning = "Security Alert ($localNext/${quiz.maxExitViolations}): Leaving or switching apps during this exam is logged."
      )
    }

    if (token.isBlank()) {
      if (quiz.onExitAction == ExamExitAction.AUTO_SUBMIT || localNext >= quiz.maxExitViolations) {
        submitQuiz(session, autoSubmittedReason = "SECURITY_APP_EXIT")
      }
      return
    }

    viewModelScope.launch {
      syncProgressInBackground(session)
      service.recordExamSecurityEvent(quiz.quizId, eventType, detail, token).onSuccess { res ->
        _uiState.update {
          it.copy(
            playFocusLossCount = res.focusLossCount,
            playSecurityWarning = "Security Alert (${res.focusLossCount}/${res.maxExitViolations}): App exit/focus loss recorded."
          )
        }
        if (res.shouldAutoSubmit && !_uiState.value.isActionLoading && _uiState.value.submissionResult == null) {
          submitQuiz(session, autoSubmittedReason = "SECURITY_APP_EXIT")
        }
      }.onFailure {
        if (quiz.onExitAction == ExamExitAction.AUTO_SUBMIT || localNext >= quiz.maxExitViolations) {
          submitQuiz(session, autoSubmittedReason = "SECURITY_APP_EXIT")
        }
      }
    }
  }

  fun dismissSecurityWarning() = _uiState.update { it.copy(playSecurityWarning = null) }

  fun submitQuiz(session: UserSession?, autoSubmittedReason: String? = null) {
    val state = _uiState.value
    val quiz = state.playingQuiz ?: return
    val token = session?.accessToken.orEmpty()
    if (token.isBlank() || state.isActionLoading) return
    stopCountdown()
    _uiState.update {
      it.copy(
        isActionLoading = true,
        errorMessage = null,
        playAutoSubmittedReason = autoSubmittedReason
      )
    }
    viewModelScope.launch {
      service.submitQuiz(
        quizId = quiz.quizId,
        questionsInOrder = state.playQuestions,
        selectedAnswersByOrd = state.playSelectedAnswers,
        writtenAnswersByOrd = state.playWrittenAnswers,
        autoSubmittedReason = autoSubmittedReason,
        accessToken = token
      ).fold(
        onSuccess = { res ->
          _uiState.update {
            it.copy(
              isActionLoading = false,
              submissionResult = res,
              playRemainingSeconds = null,
              playQuestionRemainingSeconds = null
            )
          }
          val cid = _uiState.value.classId
          if (cid.isNotBlank()) {
            loadQuizzes(cid, session)
            loadActivityStats(cid, session)
            loadLeaderboard(cid, _uiState.value.leaderboardPeriod, session)
          }
        },
        onFailure = { err ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) }
        }
      )
    }
  }

  fun closePlayQuiz() {
    stopCountdown()
    activeSessionForTimer = null
    _uiState.update {
      it.copy(
        playingQuiz = null,
        playQuestions = emptyList(),
        playCurrentIndex = 0,
        playSelectedAnswers = emptyMap(),
        playWrittenAnswers = emptyMap(),
        playLockedQuestionOrds = emptySet(),
        playStartedAt = null,
        playTimeLimitMinutes = null,
        playTimeLimitSeconds = null,
        playRemainingSeconds = null,
        playQuestionTotalSeconds = null,
        playQuestionRemainingSeconds = null,
        playFocusLossCount = 0,
        playSecurityWarning = null,
        playAutoSubmittedReason = null,
        submissionResult = null,
        isLoadingPlay = false,
        errorMessage = null
      )
    }
  }

  fun openQuizReview(quiz: ChallengeItem, session: UserSession?) {
    val token = session?.accessToken.orEmpty()
    if (token.isBlank()) return
    stopCountdown()
    _uiState.update {
      it.copy(playingQuiz = null, submissionResult = null, reviewQuiz = quiz, reviewItems = emptyList(), isLoadingReview = true, errorMessage = null)
    }
    viewModelScope.launch {
      service.getQuizReview(quiz.quizId, token).fold(
        onSuccess = { items -> _uiState.update { it.copy(reviewItems = items, isLoadingReview = false) } },
        onFailure = { err -> _uiState.update { it.copy(isLoadingReview = false, errorMessage = err.message) } }
      )
    }
  }

  fun closeQuizReview() = _uiState.update { it.copy(reviewQuiz = null, reviewItems = emptyList(), isLoadingReview = false, errorMessage = null) }

  fun openQuizResults(quiz: ChallengeItem, session: UserSession?) {
    val token = session?.accessToken.orEmpty()
    if (token.isBlank()) return
    _uiState.update { it.copy(resultsQuiz = quiz, resultsRows = emptyList(), isLoadingResults = true, errorMessage = null) }
    viewModelScope.launch {
      service.getQuizResults(quiz.quizId, token).fold(
        onSuccess = { rows -> _uiState.update { it.copy(resultsRows = rows, isLoadingResults = false) } },
        onFailure = { err -> _uiState.update { it.copy(isLoadingResults = false, errorMessage = err.message) } }
      )
    }
  }

  fun closeQuizResults() = _uiState.update { it.copy(resultsQuiz = null, resultsRows = emptyList(), isLoadingResults = false, errorMessage = null) }

  fun exportRankedResultsPdf(
    context: Context,
    className: String,
    quiz: ChallengeItem,
    rows: List<QuizResultRow>,
    generatedBy: String
  ) {
    if (rows.isEmpty()) {
      _uiState.update { it.copy(errorMessage = "No submissions available yet to rank.") }
      return
    }
    _uiState.update { it.copy(isLoadingPdf = true, errorMessage = null) }
    viewModelScope.launch {
      runCatching {
        ChallengesBatchAndPdfHelper.buildRankedResultsPdfFile(
          context = context,
          quiz = quiz,
          className = className,
          rows = rows,
          generatedBy = generatedBy
        )
      }.fold(
        onSuccess = { file ->
          _uiState.update {
            it.copy(
              isLoadingPdf = false,
              pdfViewerFile = file,
              pdfViewerTitle = "Ranked Results • ${quiz.title}"
            )
          }
        },
        onFailure = { err ->
          _uiState.update { it.copy(isLoadingPdf = false, errorMessage = err.localizedMessage ?: "Failed to build ranked PDF") }
        }
      )
    }
  }

  fun exportRankedResultsPdfFromMarking(
    context: Context,
    className: String,
    quiz: ChallengeItem,
    session: UserSession?
  ) {
    val token = session?.accessToken.orEmpty()
    if (token.isBlank()) return
    _uiState.update { it.copy(isLoadingPdf = true, errorMessage = null) }
    viewModelScope.launch {
      service.getQuizResults(quiz.quizId, token).fold(
        onSuccess = { rows ->
          exportRankedResultsPdf(context, className, quiz, rows, session?.fullName?.ifBlank { "Lecturer" } ?: "Lecturer")
        },
        onFailure = { err ->
          _uiState.update { it.copy(isLoadingPdf = false, errorMessage = err.message) }
        }
      )
    }
  }

  fun promptDeleteQuiz(quiz: ChallengeItem) = _uiState.update { it.copy(quizPendingDelete = quiz) }
  fun dismissDeleteQuiz() = _uiState.update { it.copy(quizPendingDelete = null) }

  fun confirmDeleteQuiz(session: UserSession?) {
    val quiz = _uiState.value.quizPendingDelete ?: return
    val token = session?.accessToken.orEmpty()
    if (token.isBlank()) return
    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      service.deleteQuiz(quiz.quizId, token).fold(
        onSuccess = {
          _uiState.update { st ->
            st.copy(
              isActionLoading = false,
              quizPendingDelete = null,
              quizzes = st.quizzes.filterNot { it.quizId == quiz.quizId },
              resultsQuiz = if (st.resultsQuiz?.quizId == quiz.quizId) null else st.resultsQuiz,
              reviewQuiz = if (st.reviewQuiz?.quizId == quiz.quizId) null else st.reviewQuiz,
              markingQuiz = if (st.markingQuiz?.quizId == quiz.quizId) null else st.markingQuiz,
              successMessage = if (quiz.isExam) "Exam deleted" else "Challenge deleted"
            )
          }
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, quizPendingDelete = null, errorMessage = err.message) } }
      )
    }
  }

  // MARKING STUDIO WORKFLOW
  fun openExamSubmissions(quiz: ChallengeItem, session: UserSession?) {
    val token = session?.accessToken.orEmpty()
    if (token.isBlank()) return
    _uiState.update {
      it.copy(markingQuiz = quiz, submissions = emptyList(), submissionsFilter = SubmissionFilter.ALL, submissionsSearchQuery = "", isLoadingSubmissions = true, errorMessage = null)
    }
    viewModelScope.launch {
      markingService.listExamSubmissions(quiz.quizId, token).fold(
        onSuccess = { rows -> _uiState.update { it.copy(submissions = rows, isLoadingSubmissions = false) } },
        onFailure = { err -> _uiState.update { it.copy(isLoadingSubmissions = false, errorMessage = err.message) } }
      )
    }
  }

  fun closeExamSubmissions() = _uiState.update {
    it.copy(markingQuiz = null, submissions = emptyList(), submissionsSearchQuery = "", isLoadingSubmissions = false, errorMessage = null)
  }

  fun openAdjacentPaperForMarking(offset: Int, session: UserSession?) {
    val st = _uiState.value
    val target = if (offset < 0) st.prevPaperSubmission else st.nextPaperSubmission
    val quizId = st.markingQuiz?.quizId ?: st.activePaper?.quiz?.id ?: return
    if (target != null) openPaperForMarking(quizId, target.studentId, session)
  }

  fun openPaperForMarking(quizId: String, studentId: String, session: UserSession?) {
    val token = session?.accessToken.orEmpty()
    if (token.isBlank()) return
    _uiState.update { it.copy(activePaper = null, isLoadingPaper = true, errorMessage = null) }
    viewModelScope.launch {
      markingService.getExamPaper(quizId, studentId, token).fold(
        onSuccess = { detail ->
          val initialMarks = buildMap {
            detail.questions.filter { it.questionType == QuestionType.WRITTEN }.forEach { q ->
              put(q.ord, QuestionMarkInput(marks = q.awardedMarks, comment = q.comment))
            }
          }
          _uiState.update {
            it.copy(activePaper = detail, markingDraft = detail.draft, questionMarksMap = initialMarks, overallCommentInput = detail.attempt.overallComment, isLoadingPaper = false)
          }
        },
        onFailure = { err -> _uiState.update { it.copy(isLoadingPaper = false, errorMessage = err.message) } }
      )
    }
  }

  fun closePaperMarking() = _uiState.update {
    it.copy(activePaper = null, markingDraft = MarkingDraftData(), questionMarksMap = emptyMap(), overallCommentInput = "", isLoadingPaper = false, isSavingDraft = false, isReleasingPaper = false, errorMessage = null)
  }

  fun addStroke(questionOrd: Int, stroke: InkStroke) {
    if (stroke.points.isEmpty()) return
    _uiState.update { st ->
      val current = st.markingDraft.questions[questionOrd] ?: QuestionMarkingDraft()
      st.copy(markingDraft = st.markingDraft.copy(questions = st.markingDraft.questions + (questionOrd to current.copy(strokes = current.strokes + stroke))))
    }
  }

  fun addStamp(questionOrd: Int, stamp: MarkingStamp) {
    _uiState.update { st ->
      val current = st.markingDraft.questions[questionOrd] ?: QuestionMarkingDraft()
      st.copy(markingDraft = st.markingDraft.copy(questions = st.markingDraft.questions + (questionOrd to current.copy(stamps = current.stamps + stamp))))
    }
  }

  fun undoLastAnnotation(questionOrd: Int) {
    _uiState.update { st ->
      val current = st.markingDraft.questions[questionOrd] ?: return@update st
      val updatedQ = when {
        current.strokes.isNotEmpty() -> current.copy(strokes = current.strokes.dropLast(1))
        current.stamps.isNotEmpty() -> current.copy(stamps = current.stamps.dropLast(1))
        else -> current
      }
      st.copy(markingDraft = st.markingDraft.copy(questions = st.markingDraft.questions + (questionOrd to updatedQ)))
    }
  }

  fun clearQuestionAnnotations(questionOrd: Int) {
    _uiState.update { st ->
      st.copy(markingDraft = st.markingDraft.copy(questions = st.markingDraft.questions + (questionOrd to QuestionMarkingDraft())))
    }
  }

  fun updateQuestionMark(questionOrd: Int, marks: Double?) {
    _uiState.update { st ->
      val cur = st.questionMarksMap[questionOrd] ?: QuestionMarkInput()
      st.copy(questionMarksMap = st.questionMarksMap + (questionOrd to cur.copy(marks = marks)))
    }
  }

  fun updateQuestionComment(questionOrd: Int, comment: String) {
    val clipped = comment.take(2000)
    _uiState.update { st ->
      val cur = st.questionMarksMap[questionOrd] ?: QuestionMarkInput()
      st.copy(questionMarksMap = st.questionMarksMap + (questionOrd to cur.copy(comment = clipped)))
    }
  }

  fun updateOverallComment(comment: String) = _uiState.update { it.copy(overallCommentInput = comment.take(2000)) }

  fun saveMarkingDraft(session: UserSession?) {
    val st = _uiState.value
    val paper = st.activePaper ?: return
    val token = session?.accessToken.orEmpty()
    if (token.isBlank() || st.isSavingDraft) return
    _uiState.update { it.copy(isSavingDraft = true, errorMessage = null) }
    viewModelScope.launch {
      markingService.saveMarkingDraft(paper.quiz.id, paper.student.id, paper.attempt.draftVersion, st.markingDraft, st.questionMarksMap, st.overallCommentInput, token).fold(
        onSuccess = { newVersion ->
          _uiState.update { cur ->
            val updatedPaper = cur.activePaper?.let { p ->
              p.copy(attempt = p.attempt.copy(draftVersion = newVersion, status = if (p.attempt.status == MarkingStatus.PENDING) MarkingStatus.MARKING else p.attempt.status))
            }
            cur.copy(isSavingDraft = false, activePaper = updatedPaper, successMessage = "Draft saved (v$newVersion)")
          }
          st.markingQuiz?.let { mq -> openExamSubmissionsSilent(mq.quizId, token) }
        },
        onFailure = { err -> _uiState.update { it.copy(isSavingDraft = false, errorMessage = err.message) } }
      )
    }
  }

  fun releaseMarkedPaper(context: Context, session: UserSession?) {
    val st = _uiState.value
    val paper = st.activePaper ?: return
    val token = session?.accessToken.orEmpty()
    if (token.isBlank() || st.isReleasingPaper) return

    val missingQ = paper.questions.firstOrNull { q -> q.questionType == QuestionType.WRITTEN && st.questionMarksMap[q.ord]?.marks == null }
    if (missingQ != null) {
      _uiState.update { it.copy(errorMessage = "Please enter marks for Question ${missingQ.ord} before returning.") }
      return
    }
    val overMax = paper.questions.firstOrNull { q -> q.questionType == QuestionType.WRITTEN && (st.questionMarksMap[q.ord]?.marks ?: 0.0) > q.marks }
    if (overMax != null) {
      _uiState.update { it.copy(errorMessage = "Question ${overMax.ord} marks exceed maximum (${overMax.marks}).") }
      return
    }

    _uiState.update { it.copy(isReleasingPaper = true, errorMessage = null) }
    viewModelScope.launch {
      if (paper.attempt.status != MarkingStatus.RELEASED) {
        markingService.saveMarkingDraft(paper.quiz.id, paper.student.id, paper.attempt.draftVersion, st.markingDraft, st.questionMarksMap, st.overallCommentInput, token)
      }
      val pdfBytes = runCatching {
        withContext(Dispatchers.Default) {
          MarkedPaperPdfGenerator.generatePdfBytes(paper, st.markingDraft, st.questionMarksMap, st.overallCommentInput)
        }
      }.getOrElse { err ->
        _uiState.update { it.copy(isReleasingPaper = false, errorMessage = err.message ?: "Failed to generate PDF") }
        return@launch
      }

      val storagePath = ExamMarkingService.buildPdfStoragePath(paper.quiz.classId, paper.quiz.id, paper.student.id)
      val uploadRes = markingService.uploadMarkedPdf(storagePath, pdfBytes, token)
      if (uploadRes.isFailure) {
        _uiState.update { it.copy(isReleasingPaper = false, errorMessage = uploadRes.exceptionOrNull()?.message ?: "Failed to upload marked PDF") }
        return@launch
      }

      markingService.releaseMarkedPaper(paper.quiz.id, paper.student.id, storagePath, st.questionMarksMap, st.overallCommentInput, token).fold(
        onSuccess = {
          val localFile = File(File(context.cacheDir, "marked_papers"), "${paper.quiz.id}_${paper.student.id}.pdf")
          withContext(Dispatchers.IO) { localFile.parentFile?.mkdirs(); localFile.writeBytes(pdfBytes) }
          _uiState.update { cur ->
            cur.copy(isReleasingPaper = false, activePaper = null, markingDraft = MarkingDraftData(), questionMarksMap = emptyMap(), overallCommentInput = "", successMessage = "Marked paper returned to ${paper.student.fullName}!")
          }
          st.markingQuiz?.let { mq -> openExamSubmissionsSilent(mq.quizId, token) }
          loadPapersToMark(session)
        },
        onFailure = { err -> _uiState.update { it.copy(isReleasingPaper = false, errorMessage = err.message) } }
      )
    }
  }

  fun releaseAllReadyPapers(context: Context, session: UserSession?) {
    val st = _uiState.value
    val quiz = st.markingQuiz ?: return
    val token = session?.accessToken.orEmpty()
    if (token.isBlank() || st.isBatchReleasing) return

    val readyRows = st.submissions.filter { it.isReadyForBatchReturn }
    if (readyRows.isEmpty()) {
      _uiState.update { it.copy(errorMessage = "No fully marked papers are ready to return yet.") }
      return
    }

    _uiState.update {
      it.copy(isBatchReleasing = true, batchReleaseDone = 0, batchReleaseTotal = readyRows.size, errorMessage = null, successMessage = null)
    }
    viewModelScope.launch {
      val outcome = ChallengesBatchAndPdfHelper.releaseAllReadyPapers(
        context = context,
        quizId = quiz.quizId,
        readyRows = readyRows,
        accessToken = token,
        markingService = markingService
      ) { done, total ->
        _uiState.update { it.copy(batchReleaseDone = done, batchReleaseTotal = total) }
      }
      _uiState.update {
        it.copy(
          isBatchReleasing = false,
          batchReleaseDone = 0,
          batchReleaseTotal = 0,
          errorMessage = if (outcome.releasedCount == 0) outcome.firstError ?: "Failed to return papers." else null,
          successMessage = if (outcome.releasedCount > 0) {
            val extra = if (outcome.skippedCount > 0) " (${outcome.skippedCount} skipped)" else ""
            "Returned ${outcome.releasedCount} marked paper(s) at once!$extra"
          } else null
        )
      }
      openExamSubmissionsSilent(quiz.quizId, token)
      loadPapersToMark(session)
    }
  }

  fun reopenMarkedPaper(quizId: String, studentId: String, session: UserSession?) {
    val token = session?.accessToken.orEmpty()
    if (token.isBlank()) return
    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      markingService.reopenMarkedPaper(quizId, studentId, token).fold(
        onSuccess = {
          _uiState.update { st ->
            val updatedPaper = st.activePaper?.let { p -> p.copy(attempt = p.attempt.copy(status = MarkingStatus.MARKING)) }
            st.copy(isActionLoading = false, activePaper = updatedPaper, successMessage = "Paper reopened for corrections.")
          }
          _uiState.value.markingQuiz?.let { mq -> openExamSubmissionsSilent(mq.quizId, token) }
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  private fun openExamSubmissionsSilent(quizId: String, token: String) {
    viewModelScope.launch {
      markingService.listExamSubmissions(quizId, token).onSuccess { rows ->
        _uiState.update { it.copy(submissions = rows) }
      }
    }
  }

  fun openMarkedPaperForStudent(context: Context, quiz: ChallengeItem, session: UserSession?) {
    val token = session?.accessToken.orEmpty()
    if (token.isBlank()) return
    _uiState.update { it.copy(isLoadingPdf = true, pdfViewerTitle = quiz.title, errorMessage = null) }
    viewModelScope.launch {
      ChallengesBatchAndPdfHelper.downloadVerifyAndCleanupRemotePdf(context, quiz.quizId, token, markingService).fold(
        onSuccess = { file ->
          _uiState.update {
            it.copy(
              isLoadingPdf = false,
              pdfViewerFile = file,
              pdfViewerTitle = quiz.title,
              successMessage = "Saved on device & cleared from cloud storage."
            )
          }
        },
        onFailure = { err ->
          _uiState.update { it.copy(isLoadingPdf = false, errorMessage = err.message) }
        }
      )
    }
  }

  fun openReleasedPdfForMarker(
    context: Context,
    quizId: String,
    studentId: String,
    storagePath: String,
    title: String,
    session: UserSession?
  ) {
    val token = session?.accessToken.orEmpty()
    if (token.isBlank()) return
    _uiState.update { it.copy(isLoadingPdf = true, pdfViewerTitle = title, errorMessage = null) }
    viewModelScope.launch {
      val localFile = File(File(context.cacheDir, "marked_papers"), "${quizId}_$studentId.pdf")
      if (storagePath.isNotBlank()) {
        val dl = markingService.downloadMarkedPdf(storagePath, localFile, token)
        if (dl.isSuccess) {
          _uiState.update { it.copy(isLoadingPdf = false, pdfViewerFile = dl.getOrThrow(), pdfViewerTitle = title) }
          return@launch
        }
      }
      // If remote PDF already deleted by student download, regenerate locally for marker preview
      markingService.getExamPaper(quizId, studentId, token).fold(
        onSuccess = { detail ->
          val marksMap = buildMap {
            detail.questions.filter { it.questionType == QuestionType.WRITTEN }.forEach { q ->
              put(q.ord, QuestionMarkInput(marks = q.awardedMarks, comment = q.comment))
            }
          }
          val bytes = withContext(Dispatchers.Default) {
            MarkedPaperPdfGenerator.generatePdfBytes(detail, detail.draft, marksMap, detail.attempt.overallComment)
          }
          withContext(Dispatchers.IO) { localFile.parentFile?.mkdirs(); localFile.writeBytes(bytes) }
          _uiState.update { it.copy(isLoadingPdf = false, pdfViewerFile = localFile, pdfViewerTitle = title) }
        },
        onFailure = { err -> _uiState.update { it.copy(isLoadingPdf = false, errorMessage = err.message) } }
      )
    }
  }

  fun closePdfViewer() = _uiState.update { it.copy(pdfViewerFile = null, pdfViewerTitle = "", isLoadingPdf = false) }
  fun clearError() = _uiState.update { it.copy(errorMessage = null) }
  fun clearSuccess() = _uiState.update { it.copy(successMessage = null) }

  override fun onCleared() {
    super.onCleared()
    stopCountdown()
  }
}
