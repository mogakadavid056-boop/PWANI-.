package com.example.ui

import com.example.data.AdminUserItem
import com.example.util.RoleHelper

data class AdminUiState(
  val users: List<AdminUserItem> = emptyList(),
  val searchQuery: String = "",
  val isLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val selectedUser: AdminUserItem? = null,
  val selectedRoleChoice: String = RoleHelper.ROLE_STUDENT,
  val showConfirmDialog: Boolean = false,
  val verificationPendingUser: AdminUserItem? = null,
  val verificationTargetStatus: Boolean = false,
  val showVerificationConfirmDialog: Boolean = false
)
