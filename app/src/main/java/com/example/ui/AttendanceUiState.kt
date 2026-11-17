package com.example.ui

import com.example.data.AttendanceRecordItem
import com.example.data.AttendanceRegisterDetail
import com.example.data.AttendanceRegisterListItem
import com.example.data.AttendanceSessionItem
import com.example.data.AttendanceSubScreen
import com.example.data.ClassUnitItem

data class AttendanceUiState(
  val classId: String = "",
  val isStaff: Boolean = false,
  val canSignAttendance: Boolean = true,
  val isLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val isUnitsLoading: Boolean = false,
  val classUnits: List<ClassUnitItem> = emptyList(),
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val staffSessions: List<AttendanceSessionItem> = emptyList(),
  val openSessions: List<AttendanceSessionItem> = emptyList(),
  val activeSessionId: String? = null,
  val activeSessionCode: String? = null,
  val activeSessionRemainingSeconds: Long = 0L,
  val canManageActiveSession: Boolean = false,
  val selectedSessionId: String? = null,
  val attendanceList: List<AttendanceRecordItem> = emptyList(),
  val isAttendanceListLoading: Boolean = false,
  val registers: List<AttendanceRegisterListItem> = emptyList(),
  val selectedRegisterDetail: AttendanceRegisterDetail? = null,
  val isRegisterLoading: Boolean = false,
  val currentSubScreen: AttendanceSubScreen = AttendanceSubScreen.None,
  val sessionPendingVoid: AttendanceSessionItem? = null,
  val registerPendingDelete: AttendanceRegisterListItem? = null,
  val signNeedsDetails: Boolean = false,
  val signLockoutUntilMillis: Long = 0L
) {
  val isSignLockedOut: Boolean
    get() = System.currentTimeMillis() < signLockoutUntilMillis

  val signLockoutRemainingSeconds: Long
    get() = ((signLockoutUntilMillis - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
}
