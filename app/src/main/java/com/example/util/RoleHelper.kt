package com.example.util

object RoleHelper {
  const val ROLE_STUDENT = "student"
  const val ROLE_CLASS_REP = "class_rep"
  const val ROLE_LECTURER = "lecturer"
  const val ROLE_ADMIN = "admin"

  const val STATUS_PENDING = "pending"
  const val STATUS_APPROVED = "approved"
  const val STATUS_REJECTED = "rejected"

  fun normalizeRole(role: String?): String {
    val clean = role?.trim().orEmpty()
    if (clean.isBlank() || clean.equals("null", ignoreCase = true)) return ""
    return when (clean.lowercase()) {
      "student" -> ROLE_STUDENT
      "class_rep", "class representative", "class_prefect", "prefect", "rep" -> ROLE_CLASS_REP
      "lecturer" -> ROLE_LECTURER
      "admin", "super_admin", "superadmin", "super admin" -> ROLE_ADMIN
      else -> clean.lowercase()
    }
  }

  fun normalizeRoleStatus(status: String?): String {
    val clean = status?.trim().orEmpty()
    if (clean.isBlank() || clean.equals("null", ignoreCase = true)) return ""
    return when (clean.lowercase()) {
      "pending", "awaiting_approval", "awaiting approval" -> STATUS_PENDING
      "rejected", "declined" -> STATUS_REJECTED
      "approved", "active", "verified" -> STATUS_APPROVED
      else -> clean.lowercase()
    }
  }

  fun isAdmin(role: String?): Boolean = normalizeRole(role) == ROLE_ADMIN

  fun isLecturer(role: String?): Boolean = normalizeRole(role) == ROLE_LECTURER

  fun isClassRep(role: String?): Boolean = normalizeRole(role) == ROLE_CLASS_REP

  fun isStudentOnly(role: String?): Boolean = normalizeRole(role) == ROLE_STUDENT

  fun isStaffRole(role: String?): Boolean {
    val r = normalizeRole(role)
    return r == ROLE_ADMIN || r == ROLE_LECTURER
  }

  fun canManageClassContent(role: String?, isRepInClass: Boolean = false): Boolean {
    val r = normalizeRole(role)
    return r == ROLE_ADMIN || r == ROLE_LECTURER || r == ROLE_CLASS_REP || isRepInClass
  }

  fun canSignAttendance(role: String?): Boolean {
    val r = normalizeRole(role)
    return r == ROLE_STUDENT || r == ROLE_CLASS_REP
  }

  fun usesStaffNumber(role: String?): Boolean {
    val r = normalizeRole(role)
    return r == ROLE_LECTURER || r == ROLE_ADMIN
  }

  fun isPendingApproval(roleStatus: String?): Boolean =
    normalizeRoleStatus(roleStatus) == STATUS_PENDING

  fun isRejectedStatus(roleStatus: String?): Boolean =
    normalizeRoleStatus(roleStatus) == STATUS_REJECTED

  fun formatDisplayName(role: String?): String {
    return when (normalizeRole(role)) {
      ROLE_ADMIN -> "Super Admin"
      ROLE_LECTURER -> "Lecturer"
      ROLE_CLASS_REP -> "Class Rep"
      ROLE_STUDENT -> "Student"
      "" -> "-"
      else -> role.orEmpty().trim().replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
      }
    }
  }
}
