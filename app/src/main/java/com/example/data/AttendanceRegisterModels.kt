package com.example.data

data class RegisterPermissions(
  val canManage: Boolean = false,
  val canEditRecords: Boolean = false,
  val canEditDetails: Boolean = false,
  val canClose: Boolean = false,
  val canVoid: Boolean = false,
  val canDelete: Boolean = false,
  val canSignOut: Boolean = false,
  val canSignPrefect: Boolean = false,
  val canSignLecturer: Boolean = false
)

data class ClassUnitItem(
  val courseId: String,
  val courseName: String,
  val courseCode: String,
  val lecturerName: String
)

data class AttendanceRegisterListItem(
  val sessionId: String,
  val registerNo: String,
  val title: String,
  val courseName: String? = null,
  val startsAt: String = "",
  val dateText: String = "",
  val timeText: String = "",
  val status: String, // "open", "closed", "voided"
  val presentCount: Int = 0,
  val canManage: Boolean = false,
  val canDelete: Boolean = false
)

data class AttendanceRegisterRowItem(
  val n: Int,
  val studentId: String,
  val recordId: String = "",
  val fullName: String,
  val admissionNumber: String,
  val isPrefect: Boolean = false,
  val present: Boolean = false,
  val manual: Boolean = false,
  val timeIn: String? = null,
  val timeOut: String? = null,
  val timeInText: String = "",
  val timeOutText: String = "",
  val signature: String? = null
)

data class AttendanceRegisterDetail(
  val sessionId: String = "",
  val registerNo: String,
  val title: String,
  val status: String, // "open", "closed", "voided"
  val voidReason: String? = null,
  val courseName: String? = null,
  val courseCode: String? = null,
  val className: String? = null,
  val yearOfStudy: String? = null,
  val lecturerName: String? = null,
  val venue: String? = null,
  val dateText: String = "",
  val timeText: String = "",
  val generatedText: String = "",
  val prefectName: String? = null,
  val prefectSignature: String? = null,
  val prefectSignedText: String = "",
  val lecturerSignature: String? = null,
  val lecturerSignedText: String = "",
  val presentCount: Int = 0,
  val total: Int = 0,
  val missingDetails: Boolean = false,
  val permissions: RegisterPermissions = RegisterPermissions(),
  val rows: List<AttendanceRegisterRowItem> = emptyList()
)
