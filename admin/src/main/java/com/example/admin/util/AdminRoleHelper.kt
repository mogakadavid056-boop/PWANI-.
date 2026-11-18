package com.example.admin.util

object AdminRoleHelper {
  const val ROLE_STUDENT = "student"
  const val ROLE_CLASS_REP = "class_rep"
  const val ROLE_LECTURER = "lecturer"
  const val ROLE_ADMIN = "admin"

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

  fun isAdmin(role: String?): Boolean = normalizeRole(role) == ROLE_ADMIN
  fun isLecturer(role: String?): Boolean = normalizeRole(role) == ROLE_LECTURER
  fun isClassRep(role: String?): Boolean = normalizeRole(role) == ROLE_CLASS_REP
  fun isStudentOnly(role: String?): Boolean = normalizeRole(role) == ROLE_STUDENT

  fun formatDisplayName(role: String?): String {
    return when (normalizeRole(role)) {
      ROLE_ADMIN -> "Super Admin"
      ROLE_LECTURER -> "Lecturer"
      ROLE_CLASS_REP -> "Class Rep"
      ROLE_STUDENT, "" -> "Student"
      else -> role.orEmpty().trim().replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
      }
    }
  }
}
