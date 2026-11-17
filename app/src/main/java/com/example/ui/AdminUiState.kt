package com.example.ui

import com.example.data.AdminUserItem
import com.example.util.RoleHelper

data class AdminUiState(
  val users: List<AdminUserItem> = emptyList(),
  val pendingLecturers: List<AdminUserItem> = emptyList(),
  val searchQuery: String = "",
  val filterTab: String = "all",
  val isLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val selectedUser: AdminUserItem? = null,
  val selectedRoleChoice: String = RoleHelper.ROLE_STUDENT,
  val showConfirmDialog: Boolean = false,
  val verificationPendingUser: AdminUserItem? = null,
  val verificationTargetStatus: Boolean = false,
  val showVerificationConfirmDialog: Boolean = false,
  val approvalPendingUser: AdminUserItem? = null,
  val approvalDecisionApprove: Boolean = true,
  val repTogglePendingUser: AdminUserItem? = null,
  val repTargetIsRep: Boolean = true
) {
  val pendingCount: Int
    get() {
      val byId = LinkedHashMap<String, AdminUserItem>()
      pendingLecturers.forEach { if (it.id.isNotBlank()) byId[it.id] = it }
      users.filter { it.isPendingApproval || (it.isLecturer && !it.isVerified && !it.isRejected) }
        .forEach { if (it.id.isNotBlank()) byId.putIfAbsent(it.id, it) }
      return byId.size
    }

  val displayedUsers: List<AdminUserItem>
    get() {
      val merged = if (searchQuery.isBlank()) {
        val byId = LinkedHashMap<String, AdminUserItem>()
        pendingLecturers.forEach { if (it.id.isNotBlank()) byId[it.id] = it }
        users.forEach { user ->
          if (user.id.isNotBlank()) {
            val existing = byId[user.id]
            byId[user.id] = if (existing == null) user else user.copy(
              roleStatus = if (user.isPendingApproval) user.roleStatus else existing.roleStatus,
              staffNumber = user.staffNumber.ifBlank { existing.staffNumber },
              department = user.department.ifBlank { existing.department },
              requestedRole = user.requestedRole.ifBlank { existing.requestedRole }
            )
          }
        }
        byId.values.toList()
      } else {
        users
      }

      return when (filterTab) {
        "pending" -> merged.filter { it.isPendingApproval || (it.isLecturer && !it.isVerified && !it.isRejected) }
        "class_rep" -> merged.filter { it.isClassRep }
        "lecturer" -> merged.filter { it.isLecturer }
        "student" -> merged.filter { it.isStudent }
        else -> merged
      }
    }
}
