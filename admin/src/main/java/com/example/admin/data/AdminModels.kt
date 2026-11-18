package com.example.admin.data

import com.example.admin.util.AdminRoleHelper

data class AdminSession(
  val userId: String,
  val email: String,
  val fullName: String,
  val role: String,
  val accessToken: String,
  val refreshToken: String? = null
) {
  val isAdmin: Boolean get() = AdminRoleHelper.isAdmin(role)
}

data class AdminDashboardStats(
  val totalUsers: Int = 0,
  val totalStudents: Int = 0,
  val totalClassReps: Int = 0,
  val totalLecturers: Int = 0,
  val unverifiedLecturers: Int = 0,
  val totalAdmins: Int = 0,
  val totalSchools: Int = 0,
  val totalProgrammes: Int = 0,
  val totalClasses: Int = 0,
  val totalRegisters: Int = 0,
  val openRegisters: Int = 0,
  val rpcSqlInstalled: Boolean = true
)

data class AdminUserItem(
  val id: String,
  val fullName: String,
  val email: String,
  val admissionNumber: String,
  val role: String,
  val isVerified: Boolean = false
) {
  val displayRole: String get() = AdminRoleHelper.formatDisplayName(role)
  val isAdmin: Boolean get() = AdminRoleHelper.isAdmin(role)
  val isLecturer: Boolean get() = AdminRoleHelper.isLecturer(role)
  val isClassRep: Boolean get() = AdminRoleHelper.isClassRep(role)
  val isStudent: Boolean get() = AdminRoleHelper.isStudentOnly(role) || role.isBlank()
}

data class SchoolItem(
  val id: String,
  val name: String,
  val universityId: String = ""
)

data class ProgrammeItem(
  val id: String,
  val name: String,
  val schoolId: String,
  val schoolName: String = "",
  val durationYears: Int? = 4
)

data class AcademicYearItem(
  val id: String,
  val name: String,
  val isCurrent: Boolean = false
)

data class AdminClassItem(
  val id: String,
  val name: String,
  val code: String = "",
  val joinCode: String = "",
  val yearOfStudy: Int? = null,
  val programmeName: String = "",
  val memberCount: Int? = null,
  val registerCount: Int? = null
)

data class AdminRegisterSummary(
  val sessionId: String,
  val registerNo: String,
  val title: String,
  val courseName: String,
  val dateText: String,
  val timeText: String,
  val status: String,
  val presentCount: Int
)

data class AdminRegisterRow(
  val n: Int,
  val fullName: String,
  val admissionNumber: String,
  val isPrefect: Boolean,
  val present: Boolean,
  val timeInText: String,
  val timeOutText: String
)

data class AdminRegisterDetail(
  val sessionId: String,
  val registerNo: String,
  val title: String,
  val status: String,
  val voidReason: String,
  val courseName: String,
  val courseCode: String,
  val className: String,
  val lecturerName: String,
  val venue: String,
  val dateText: String,
  val timeText: String,
  val presentCount: Int,
  val total: Int,
  val canClose: Boolean,
  val canVoid: Boolean,
  val rows: List<AdminRegisterRow>
)
