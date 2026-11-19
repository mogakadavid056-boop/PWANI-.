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
