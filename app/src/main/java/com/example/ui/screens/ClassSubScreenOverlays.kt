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
  trackerUiState: com.example.ui.AttendanceTrackerUiState = com.example.ui.AttendanceTrackerUiState(),
  isStaff: Boolean,
  session: UserSession? = null,
  className: String = "",
  onDismissAttendanceError: () -> Unit,
  onOpenViewAttendance: (String) -> Unit,
  onCloseAttendanceSubScreen: () -> Unit,
  onDismissAttendanceSuccess: () -> Unit,
  onRefreshTracker: () -> Unit = {},
  onSelectTrackerLevel: (com.example.data.RiskLevel) -> Unit = {},
  onTrackerSearchChange: (String) -> Unit = {},
  onOpenTrackerStudent: (com.example.data.StudentRiskItem) -> Unit = {},
  onCloseTrackerStudent: () -> Unit = {},
  onExcuseTrackerSession: (String, String) -> Unit = { _, _ -> },
  onRemoveTrackerExcuse: (String) -> Unit = {},
  onDismissTrackerError: () -> Unit = {},
  onDismissTrackerSuccess: () -> Unit = {},
  onUpdateMyDetails: (fullName: String, admissionNo: String, phone: String, onSaved: () -> Unit) -> Unit = { _, _, _, cb -> cb() },
  onSignAttendanceSubmit: (code: String, signatureBase64: String, memberFullName: String, memberAdmissionNo: String) -> Unit,
  onCloseRegisterSession: () -> Unit = {},
  onVoidRegisterSession: (String) -> Unit = {},
  onDeleteRegisterSession: (String) -> Unit = {},
  onSignOutRegisterSession: () -> Unit = {},
  onSignRegister: (String, String) -> Unit = { _, _ -> },
  onCorrectRegisterTime: (String, String, String, String) -> Unit = { _, _, _, _ -> },
  onMarkPresent: (String, String, String, String) -> Unit = { _, _, _, _ -> },
  onUpdateRegisterDetails: (String, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
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
  if (attendanceUiState.currentSubScreen == AttendanceSubScreen.AttendanceRegister) {
    val canManageRole = com.example.util.RoleHelper.canManageClassContent(session?.role, isStaff)
    val canSignRole = com.example.util.RoleHelper.canSignAttendance(session?.role)
    AttendanceRegisterScreen(
      detail = attendanceUiState.selectedRegisterDetail,
      units = attendanceUiState.classUnits,
      canManageRole = canManageRole,
      canSignAttendanceRole = canSignRole,
      isLoading = attendanceUiState.isRegisterLoading,
      isActionLoading = attendanceUiState.isActionLoading,
      errorMessage = attendanceUiState.errorMessage,
      successMessage = attendanceUiState.successMessage,
      onDismissError = onDismissAttendanceError,
      onDismissSuccess = onDismissAttendanceSuccess,
      onCloseSession = onCloseRegisterSession,
      onVoidSession = onVoidRegisterSession,
      onDeleteSession = onDeleteRegisterSession,
      onSignOut = onSignOutRegisterSession,
      onSignRegister = onSignRegister,
      onCorrectTime = onCorrectRegisterTime,
      onMarkPresent = onMarkPresent,
      onUpdateDetails = onUpdateRegisterDetails,
      onBack = onCloseAttendanceSubScreen
    )
    return true
  }

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

  if (attendanceUiState.currentSubScreen == AttendanceSubScreen.AttendanceTracker &&
    com.example.util.RoleHelper.isStaffRole(session?.role)
  ) {
    AttendanceTrackerScreen(
      classId = attendanceUiState.classId,
      className = className,
      session = session,
      uiState = trackerUiState,
      onRefresh = onRefreshTracker,
      onSelectLevel = onSelectTrackerLevel,
      onSearchChange = onTrackerSearchChange,
      onStudentClick = onOpenTrackerStudent,
      onCloseStudentDetail = onCloseTrackerStudent,
      onExcuseSession = onExcuseTrackerSession,
      onRemoveExcuse = onRemoveTrackerExcuse,
      onDismissError = onDismissTrackerError,
      onDismissSuccess = onDismissTrackerSuccess,
      onBack = onCloseAttendanceSubScreen
    )
    return true
  }

  if (attendanceUiState.currentSubScreen == AttendanceSubScreen.SignAttendance) {
    SignAttendanceScreen(
      memberFullName = session?.fullName.orEmpty(),
      memberAdmissionNo = session?.admissionNumber.orEmpty(),
      needsDetails = attendanceUiState.signNeedsDetails,
      isLoading = attendanceUiState.isActionLoading,
      errorMessage = attendanceUiState.errorMessage,
      successMessage = attendanceUiState.successMessage,
      isLockedOut = attendanceUiState.isSignLockedOut,
      lockoutRemainingSeconds = attendanceUiState.signLockoutRemainingSeconds,
      onDismissError = onDismissAttendanceError,
      onDismissSuccess = {
        onDismissAttendanceSuccess()
        onCloseAttendanceSubScreen()
      },
      onUpdateMyDetails = onUpdateMyDetails,
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
