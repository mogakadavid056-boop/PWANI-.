package com.example.data

import com.example.util.RoleHelper

data class AdminUserItem(
  val id: String,
  val fullName: String,
  val email: String,
  val admissionNumber: String,
  val role: String,
  val isVerified: Boolean = false
) {
  val displayRole: String
    get() = formatRoleDisplayName(role)

  val isClassRep: Boolean
    get() = RoleHelper.isClassRep(role)

  val isLecturer: Boolean
    get() = RoleHelper.isLecturer(role)

  val isAdmin: Boolean
    get() = RoleHelper.isAdmin(role)

  val isStudent: Boolean
    get() = RoleHelper.isStudentOnly(role) || role.isBlank()
}

fun formatRoleDisplayName(role: String?): String = RoleHelper.formatDisplayName(role)
