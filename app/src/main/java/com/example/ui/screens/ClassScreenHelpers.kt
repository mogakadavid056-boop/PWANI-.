package com.example.ui.screens

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.ui.*
import com.example.ui.components.ClassTab

data class ClassTrackerCallbacks(
  val onRefreshTracker: () -> Unit = {},
  val onSelectTrackerLevel: (com.example.data.RiskLevel) -> Unit = {},
  val onTrackerSearchChange: (String) -> Unit = {},
  val onOpenTrackerStudent: (com.example.data.StudentRiskItem) -> Unit = {},
  val onCloseTrackerStudent: () -> Unit = {},
  val onExcuseTrackerSession: (String, String) -> Unit = { _, _ -> },
  val onRemoveTrackerExcuse: (String) -> Unit = {},
  val onDismissTrackerError: () -> Unit = {},
  val onDismissTrackerSuccess: () -> Unit = {}
)

data class ClassChallengesCallbacks(
  val onRefreshAll: () -> Unit = {},
  val onSelectFilter: (com.example.data.QuizStatusFilter) -> Unit = {},
  val onSelectLeaderboardPeriod: (com.example.data.LeaderboardPeriod) -> Unit = {},
  val onOpenCreateDialog: () -> Unit = {},
  val onDismissCreateDialog: () -> Unit = {},
  val onCreateChallenge: (
    title: String,
    description: String,
    opensAtIso: String,
    closesAtIso: String,
    timeLimitMinutes: Int?,
    questions: List<com.example.data.DraftQuizQuestion>,
    courseId: String?,
    asExam: Boolean,
    shuffleQuestions: Boolean,
    answerFormat: com.example.data.ExamAnswerFormat,
    securityConfig: com.example.data.ExamSecurityConfig
  ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
  val onOpenAddQuestionsDialog: (com.example.data.ChallengeItem) -> Unit = {},
  val onDismissAddQuestionsDialog: () -> Unit = {},
  val onAddQuestions: (quizId: String, questions: List<com.example.data.DraftQuizQuestion>) -> Unit = { _, _ -> },
  val onPlayChallenge: (com.example.data.ChallengeItem) -> Unit = {},
  val onSelectAnswer: (ord: Int, optionIndex: Int) -> Unit = { _, _ -> },
  val onUpdateWrittenAnswer: (ord: Int, text: String) -> Unit = { _, _ -> },
  val onGoToQuestion: (Int) -> Unit = {},
  val onSecurityViolation: (eventType: String, detail: String) -> Unit = { _, _ -> },
  val onDismissSecurityWarning: () -> Unit = {},
  val onSubmitChallenge: () -> Unit = {},
  val onClosePlay: () -> Unit = {},
  val onOpenReview: (com.example.data.ChallengeItem) -> Unit = {},
  val onCloseReview: () -> Unit = {},
  val onOpenResults: (com.example.data.ChallengeItem) -> Unit = {},
  val onCloseResults: () -> Unit = {},
  val onExportRankedPdfFromResults: () -> Unit = {},
  val onOpenExamSubmissions: (com.example.data.ChallengeItem) -> Unit = {},
  val onCloseExamSubmissions: () -> Unit = {},
  val onSelectSubmissionFilter: (SubmissionFilter) -> Unit = {},
  val onSelectSubmissionSortOrder: (SubmissionSortOrder) -> Unit = {},
  val onToggleSubmissionsWaitingFirst: () -> Unit = {},
  val onUpdateSubmissionsSearch: (String) -> Unit = {},
  val onRefreshExamSubmissions: () -> Unit = {},
  val onReleaseAllReady: () -> Unit = {},
  val onExportRankedPdfFromMarking: () -> Unit = {},
  val onOpenPaperForMarking: (quizId: String, studentId: String) -> Unit = { _, _ -> },
  val onOpenPrevPaper: () -> Unit = {},
  val onOpenNextPaper: () -> Unit = {},
  val onClosePaperMarking: () -> Unit = {},
  val onAddStroke: (ord: Int, com.example.data.InkStroke) -> Unit = { _, _ -> },
  val onAddStamp: (ord: Int, com.example.data.MarkingStamp) -> Unit = { _, _ -> },
  val onUndoAnnotation: (ord: Int) -> Unit = {},
  val onClearQuestionInk: (ord: Int) -> Unit = {},
  val onUpdateQuestionMark: (ord: Int, Double?) -> Unit = { _, _ -> },
  val onUpdateQuestionComment: (ord: Int, String) -> Unit = { _, _ -> },
  val onUpdateOverallComment: (String) -> Unit = {},
  val onSaveMarkingDraft: () -> Unit = {},
  val onReleaseMarkedPaper: () -> Unit = {},
  val onReopenMarkedPaper: () -> Unit = {},
  val onViewMarkedPaperForStudent: (com.example.data.ChallengeItem) -> Unit = {},
  val onViewReleasedPdfForMarker: () -> Unit = {},
  val onClosePdfViewer: () -> Unit = {},
  val onPromptDelete: (com.example.data.ChallengeItem) -> Unit = {},
  val onDismissDelete: () -> Unit = {},
  val onConfirmDelete: () -> Unit = {},
  val onToggleLeaderboardVisibility: (Boolean) -> Unit = {},
  val onDismissError: () -> Unit = {},
  val onDismissSuccess: () -> Unit = {}
)

@Composable
fun AttendanceSnackbarEffect(
  selectedTab: ClassTab,
  attendanceUiState: AttendanceUiState,
  hostState: SnackbarHostState,
  onSetIsError: (Boolean) -> Unit,
  onDismissError: () -> Unit,
  onDismissSuccess: () -> Unit
) {
  LaunchedEffect(attendanceUiState.errorMessage, selectedTab) {
    val err = attendanceUiState.errorMessage
    if (selectedTab == ClassTab.Attendance && !err.isNullOrBlank()) {
      onSetIsError(true)
      hostState.showSnackbar(err, actionLabel = "Dismiss", duration = SnackbarDuration.Long)
      onDismissError()
    }
  }
  LaunchedEffect(attendanceUiState.successMessage, selectedTab) {
    val msg = attendanceUiState.successMessage
    if (selectedTab == ClassTab.Attendance && !msg.isNullOrBlank()) {
      onSetIsError(false)
      hostState.showSnackbar(msg, actionLabel = "OK", duration = SnackbarDuration.Short)
      onDismissSuccess()
    }
  }
}

fun activeClassTabError(
  selectedTab: ClassTab,
  moreSubView: MoreSubView,
  noticesError: String?,
  groupsError: String?,
  timetableError: String?,
  materialsError: String?,
  pollsError: String?,
  onDismissNoticesError: () -> Unit,
  onDismissAttendanceError: () -> Unit,
  onDismissGroupsError: () -> Unit,
  onDismissTimetableError: () -> Unit,
  onDismissMaterialsError: () -> Unit,
  onDismissPollsError: () -> Unit
): Pair<String?, () -> Unit> = when (selectedTab) {
  ClassTab.Notices -> noticesError to onDismissNoticesError
  ClassTab.Attendance -> null to onDismissAttendanceError
  ClassTab.Groups -> groupsError to onDismissGroupsError
  ClassTab.Timetable -> timetableError to onDismissTimetableError
  ClassTab.More -> when (moreSubView) {
    MoreSubView.Materials -> materialsError to onDismissMaterialsError
    MoreSubView.Polls -> pollsError to onDismissPollsError
    else -> null to onDismissNoticesError
  }
}
