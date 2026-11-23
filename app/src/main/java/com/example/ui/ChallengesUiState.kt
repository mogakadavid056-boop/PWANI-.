package com.example.ui

import com.example.data.ActivityStats
import com.example.data.ChallengeItem
import com.example.data.ExamPaperDetail
import com.example.data.ExamSubmissionRow
import com.example.data.LeaderboardEntry
import com.example.data.LeaderboardPeriod
import com.example.data.MarkingDraftData
import com.example.data.PaperToMarkItem
import com.example.data.PlayQuestion
import com.example.data.QuestionMarkInput
import com.example.data.QuizResultRow
import com.example.data.QuizReviewItem
import com.example.data.QuizStatus
import com.example.data.QuizStatusFilter
import com.example.data.QuizSubmissionResult
import java.io.File

enum class SubmissionFilter(val label: String) {
  ALL("All"),
  WAITING("Waiting"),
  MARKING("In Progress"),
  RETURNED("Returned")
}

data class ChallengesUiState(
  val classId: String = "",
  val quizzes: List<ChallengeItem> = emptyList(),
  val selectedFilter: QuizStatusFilter = QuizStatusFilter.ALL,
  val isLoadingQuizzes: Boolean = false,
  val activityStats: ActivityStats? = null,
  val isLoadingStats: Boolean = false,
  val leaderboardPeriod: LeaderboardPeriod = LeaderboardPeriod.WEEK,
  val leaderboardRows: List<LeaderboardEntry> = emptyList(),
  val isLoadingLeaderboard: Boolean = false,
  val papersToMark: List<PaperToMarkItem> = emptyList(),
  val isLoadingPapersToMark: Boolean = false,
  val showCreateDialog: Boolean = false,
  val challengeDraftPrefill: com.example.assistant.ChallengeDraftPrefill? = null,
  val addQuestionsForQuiz: ChallengeItem? = null,
  val playingQuiz: ChallengeItem? = null,
  val playQuestions: List<PlayQuestion> = emptyList(),
  val playCurrentIndex: Int = 0,
  val playSelectedAnswers: Map<Int, Int> = emptyMap(),
  val playWrittenAnswers: Map<Int, String> = emptyMap(),
  val playLockedQuestionOrds: Set<Int> = emptySet(),
  val playStartedAt: String? = null,
  val playTimeLimitMinutes: Int? = null,
  val playTimeLimitSeconds: Long? = null,
  val playRemainingSeconds: Long? = null,
  val playQuestionTotalSeconds: Long? = null,
  val playQuestionRemainingSeconds: Long? = null,
  val playFocusLossCount: Int = 0,
  val playSecurityWarning: String? = null,
  val playAutoSubmittedReason: String? = null,
  val isLoadingPlay: Boolean = false,
  val submissionResult: QuizSubmissionResult? = null,
  val reviewQuiz: ChallengeItem? = null,
  val reviewItems: List<QuizReviewItem> = emptyList(),
  val isLoadingReview: Boolean = false,
  val resultsQuiz: ChallengeItem? = null,
  val resultsRows: List<QuizResultRow> = emptyList(),
  val isLoadingResults: Boolean = false,
  val quizPendingDelete: ChallengeItem? = null,
  val markingQuiz: ChallengeItem? = null,
  val submissions: List<ExamSubmissionRow> = emptyList(),
  val submissionsFilter: SubmissionFilter = SubmissionFilter.ALL,
  val submissionsSortOrder: SubmissionSortOrder = SubmissionSortOrder.NAME_ASC,
  val submissionsWaitingFirst: Boolean = false,
  val submissionsSearchQuery: String = "",
  val isLoadingSubmissions: Boolean = false,
  val isBatchReleasing: Boolean = false,
  val batchReleaseDone: Int = 0,
  val batchReleaseTotal: Int = 0,
  val activePaper: ExamPaperDetail? = null,
  val markingDraft: MarkingDraftData = MarkingDraftData(),
  val questionMarksMap: Map<Int, QuestionMarkInput> = emptyMap(),
  val overallCommentInput: String = "",
  val isLoadingPaper: Boolean = false,
  val isSavingDraft: Boolean = false,
  val isReleasingPaper: Boolean = false,
  val pdfViewerFile: File? = null,
  val pdfViewerTitle: String = "",
  val isLoadingPdf: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null
) {
  val filteredQuizzes: List<ChallengeItem>
    get() = when (selectedFilter) {
      QuizStatusFilter.ALL -> quizzes
      QuizStatusFilter.OPEN -> quizzes.filter { it.status == QuizStatus.OPEN }
      QuizStatusFilter.UPCOMING -> quizzes.filter { it.status == QuizStatus.UPCOMING }
      QuizStatusFilter.CLOSED -> quizzes.filter { it.status == QuizStatus.CLOSED }
    }

  val filteredSubmissions: List<ExamSubmissionRow>
    get() = ExamSubmissionSorter.sortAndFilter(
      submissions = submissions,
      filter = submissionsFilter,
      sortOrder = submissionsSortOrder,
      waitingFirst = submissionsWaitingFirst,
      searchQuery = submissionsSearchQuery
    )

  val readyToBatchReturnCount: Int
    get() = submissions.count { it.isReadyForBatchReturn }

  val activePaperIndex: Int
    get() {
      val sid = activePaper?.student?.id ?: return -1
      return filteredSubmissions.indexOfFirst { it.studentId == sid }
    }

  val prevPaperSubmission: ExamSubmissionRow?
    get() = activePaperIndex.takeIf { it > 0 }?.let { filteredSubmissions.getOrNull(it - 1) }

  val nextPaperSubmission: ExamSubmissionRow?
    get() = activePaperIndex.takeIf { it in 0 until filteredSubmissions.lastIndex }
      ?.let { filteredSubmissions.getOrNull(it + 1) }

  val openCount: Int
    get() = quizzes.count { it.status == QuizStatus.OPEN }

  val hasActiveOverlay: Boolean
    get() = pdfViewerFile != null || activePaper != null || markingQuiz != null ||
      playingQuiz != null || reviewQuiz != null || resultsQuiz != null
}
