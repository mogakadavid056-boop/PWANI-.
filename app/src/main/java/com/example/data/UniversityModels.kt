package com.example.data

data class SchoolItem(
  val id: String,
  val name: String,
  val universityId: String
)

data class ProgrammeItem(
  val id: String,
  val name: String,
  val schoolId: String? = null,
  val durationYears: Int? = null
)

data class AcademicYearItem(
  val id: String,
  val name: String,
  val isCurrent: Boolean
)

data class ProfileItem(
  val id: String = "",
  val fullName: String = "",
  val email: String = "",
  val admissionNumber: String = "",
  val yearOfStudy: Int? = null,
  val schoolId: String? = null,
  val schoolName: String? = null,
  val programmeId: String? = null,
  val programmeName: String? = null,
  val universityId: String? = null
)
