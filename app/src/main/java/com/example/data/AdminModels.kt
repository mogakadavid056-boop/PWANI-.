package com.example.data

import com.example.util.RoleHelper

data class AdminUserItem(
  val id: String,
  val fullName: String,
  val email: String,
  val admissionNumber: String,
  val role: String,
  val isVerified: Boolean = false,
  val roleStatus: String = RoleHelper.STATUS_APPROVED,
  val staffNumber: String = "",
  val department: String = "",
  val requestedRole: String = ""
) {
  val displayRole: String
    get() = formatRoleDisplayName(role.ifBlank { requestedRole })

  val isPendingApproval: Boolean
    get() = RoleHelper.isPendingApproval(roleStatus)

  val isRejected: Boolean
    get() = RoleHelper.isRejectedStatus(roleStatus)

  val isClassRep: Boolean
    get() = RoleHelper.isClassRep(role)

  val isLecturer: Boolean
    get() = RoleHelper.isLecturer(role) ||
      (isPendingApproval && RoleHelper.isLecturer(requestedRole))

  val isAdmin: Boolean
    get() = RoleHelper.isAdmin(role)

  val isStudent: Boolean
    get() = RoleHelper.isStudentOnly(role)
}

fun formatRoleDisplayName(role: String?): String = RoleHelper.formatDisplayName(role)
