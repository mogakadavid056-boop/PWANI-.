package com.example.data

data class AttendanceSessionItem(
  val id: String,
  val startsAt: String,
  val expiresAt: String,
  val active: Boolean = true
)

data class AttendanceRecordItem(
  val fullName: String,
  val admissionNumber: String,
  val timeSigned: String,
  val signatureData: String? = null
)

enum class AttendanceSubScreen {
  None,
  ViewAttendance,
  SignAttendance,
  AttendanceSummary,
  MyAttendance
}

data class StudentAttendanceSummaryItem(
  val fullName: String,
  val admissionNumber: String,
  val attended: Int,
  val total: Int,
  val percentage: Double
)

data class MyAttendanceRecordItem(
  val sessionTitle: String,
  val date: String,
  val isPresent: Boolean
)

data class MyAttendanceSummaryData(
  val studentName: String,
  val admissionNumber: String,
  val className: String,
  val attended: Int,
  val total: Int,
  val percentage: Double,
  val records: List<MyAttendanceRecordItem>
)

data class ClassPdfMetadata(
  val className: String,
  val yearOfStudy: Int? = null,
  val universityName: String? = null,
  val schoolName: String? = null,
  val programmeName: String? = null
)
