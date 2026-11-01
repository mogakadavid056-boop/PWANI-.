package com.example.data

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
}

fun formatRoleDisplayName(role: String?): String {
  if (role.isNullOrBlank()) return "Student"
  return when (role.trim().lowercase()) {
    "admin" -> "Super Admin"
    "lecturer" -> "Lecturer"
    "student" -> "Student"
    else -> role.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
  }
}
