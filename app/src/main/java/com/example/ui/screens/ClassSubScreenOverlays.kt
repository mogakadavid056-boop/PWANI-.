package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.data.AttendanceSubScreen
import com.example.data.UserSession
import com.example.ui.AttendanceUiState
import com.example.ui.PollsUiState

@Composable
fun HandleClassSubScreenOverlay(
  attendanceUiState: AttendanceUiState,
  pollsUiState: PollsUiState,
  isStaff: Boolean,
  session: UserSession? = null,
  className: String = "",
  onDismissAttendanceError: () -> Unit,
  onOpenViewAttendance: (String) -> Unit,
  onCloseAttendanceSubScreen: () -> Unit,
  onDismissAttendanceSuccess: () -> Unit,
  onSignAttendanceSubmit: (code: String, signatureBase64: String) -> Unit,
  onSelectPollOption: (String) -> Unit,
  onCastVote: () -> Unit,
  onPromptClosePoll: () -> Unit,
  onDismissClosePoll: () -> Unit,
  onConfirmClosePoll: () -> Unit,
  onPromptDeletePoll: () -> Unit,
  onDismissDeletePoll: () -> Unit,
  onConfirmDeletePoll: () -> Unit,
  onDismissPollsError: () -> Unit,
  onClosePollDetail: () -> Unit
): Boolean {
  if (attendanceUiState.currentSubScreen == AttendanceSubScreen.ViewAttendance) {
    val selSession = attendanceUiState.staffSessions.find { it.id == attendanceUiState.selectedSessionId }
    val sessionDate = selSession?.startsAt?.replace("T", " ")?.substringBefore(".").orEmpty()
    val sessionTitle = if (sessionDate.isNotBlank()) "Session ($sessionDate)" else "Session"
    ViewAttendanceScreen(
      attendanceList = attendanceUiState.attendanceList,
      isLoading = attendanceUiState.isAttendanceListLoading,
      errorMessage = attendanceUiState.errorMessage,
      sessionTitle = sessionTitle,
      sessionDate = sessionDate,
      classId = attendanceUiState.classId,
      session = session,
      onDismissError = onDismissAttendanceError,
      onRefresh = { attendanceUiState.selectedSessionId?.let(onOpenViewAttendance) },
      onBack = onCloseAttendanceSubScreen
    )
    return true
  }

  if (attendanceUiState.currentSubScreen == AttendanceSubScreen.AttendanceSummary) {
    AttendanceSummaryScreen(
      classId = attendanceUiState.classId,
      className = className,
      session = session,
      onBack = onCloseAttendanceSubScreen
    )
    return true
  }

  if (attendanceUiState.currentSubScreen == AttendanceSubScreen.MyAttendance) {
    MyAttendanceScreen(
      classId = attendanceUiState.classId,
      session = session,
      onBack = onCloseAttendanceSubScreen
    )
    return true
  }

  if (attendanceUiState.currentSubScreen == AttendanceSubScreen.SignAttendance) {
    SignAttendanceScreen(
      isLoading = attendanceUiState.isActionLoading,
      errorMessage = attendanceUiState.errorMessage,
      successMessage = attendanceUiState.successMessage,
      onDismissError = onDismissAttendanceError,
      onDismissSuccess = {
        onDismissAttendanceSuccess()
        onCloseAttendanceSubScreen()
      },
      onSubmit = onSignAttendanceSubmit,
      onBack = onCloseAttendanceSubScreen
    )
    return true
  }

  val selectedPoll = pollsUiState.selectedPoll
  if (selectedPoll != null) {
    PollDetailScreen(
      poll = selectedPoll,
      isStaff = isStaff,
      isLoading = pollsUiState.isDetailLoading,
      isActionLoading = pollsUiState.isActionLoading,
      errorMessage = pollsUiState.errorMessage,
      pollOptions = pollsUiState.pollOptions,
      pollResults = pollsUiState.pollResults,
      selectedOptionId = pollsUiState.selectedOptionIdForVote,
      showCloseConfirm = pollsUiState.showCloseConfirm,
      showDeleteConfirm = pollsUiState.showDeleteConfirm,
      onSelectOption = onSelectPollOption,
      onCastVote = onCastVote,
      onPromptClose = onPromptClosePoll,
      onDismissClose = onDismissClosePoll,
      onConfirmClose = onConfirmClosePoll,
      onPromptDelete = onPromptDeletePoll,
      onDismissDelete = onDismissDeletePoll,
      onConfirmDelete = onConfirmDeletePoll,
      onDismissError = onDismissPollsError,
      onBack = onClosePollDetail
    )
    return true
  }

  return false
}
