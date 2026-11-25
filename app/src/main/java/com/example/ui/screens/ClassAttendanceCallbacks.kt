package com.example.ui.screens

import com.example.data.AttendanceRegisterListItem
import com.example.data.AttendanceSessionItem

data class ClassAttendanceCallbacks(
  val onRefreshAttendance: () -> Unit,
  val onStartAttendance: (
    courseId: String?, courseTitle: String, courseCode: String, lecturerName: String,
    venueText: String, sessionTitle: String, durationMinutes: Int
  ) -> Unit,
  val onOpenViewAttendance: (sessionId: String) -> Unit,
  val onOpenSignAttendance: (sessionId: String) -> Unit,
  val onCloseAttendanceSubScreen: () -> Unit,
  val onOpenAttendanceSummary: () -> Unit = {},
  val onOpenMyAttendance: () -> Unit = {},
  val onOpenAttendanceTracker: () -> Unit = {},
  val onShowActiveCode: ((String, String) -> Unit)? = null,
  val onOpenRegister: (sessionId: String) -> Unit = {},
  val onCloseSession: (sessionId: String) -> Unit = {},
  val onSignOutSession: (sessionId: String) -> Unit = {},
  val onVoidRegisterSession: (reason: String) -> Unit = {},
  val onDeleteRegisterSession: () -> Unit = {},
  val onSignRegister: (signerRole: String, signatureBase64: String) -> Unit = { _, _ -> },
  val onCorrectRegisterTime: (recordId: String, newTimeIn: String, newTimeOut: String, reason: String) -> Unit = { _, _, _, _ -> },
  val onMarkPresent: (studentId: String, timeIn: String, timeOut: String, reason: String) -> Unit = { _, _, _, _ -> },
  val onUpdateRegisterDetails: (courseTitle: String, courseCode: String, lecturerName: String, venueText: String, sessionTitle: String) -> Unit = { _, _, _, _, _ -> },
  val onUpdateMyDetails: (fullName: String, admissionNo: String, phone: String, onSaved: () -> Unit) -> Unit = { _, _, _, cb -> cb() },
  val onSignAttendanceSubmit: (code: String, signatureBase64: String, memberFullName: String, memberAdmissionNo: String) -> Unit,
  val onPromptVoidSession: (AttendanceSessionItem) -> Unit,
  val onDismissPromptVoidSession: () -> Unit,
  val onPromptDeleteRegister: (AttendanceRegisterListItem) -> Unit = {},
  val onDismissPromptDeleteRegister: () -> Unit = {},
  val onConfirmDeleteRegister: () -> Unit = {},
  val onDismissAttendanceError: () -> Unit,
  val onDismissAttendanceSuccess: () -> Unit
)
