package com.example.ui

import com.example.data.AttendanceRecordItem
import com.example.data.AttendanceSessionItem
import com.example.data.AttendanceSubScreen

data class AttendanceUiState(
  val classId: String = "",
  val isStaff: Boolean = false,
  val isLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val staffSessions: List<AttendanceSessionItem> = emptyList(),
  val openSessions: List<AttendanceSessionItem> = emptyList(),
  val activeSessionId: String? = null,
  val activeSessionCode: String? = null,
  val activeSessionRemainingSeconds: Long = 0L,
  val selectedSessionId: String? = null,
  val attendanceList: List<AttendanceRecordItem> = emptyList(),
  val isAttendanceListLoading: Boolean = false,
  val currentSubScreen: AttendanceSubScreen = AttendanceSubScreen.None,
  val sessionPendingDelete: AttendanceSessionItem? = null,
  val signLockoutUntilMillis: Long = 0L
) {
  val isSignLockedOut: Boolean
    get() = System.currentTimeMillis() < signLockoutUntilMillis

  val signLockoutRemainingSeconds: Long
    get() = ((signLockoutUntilMillis - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
}
