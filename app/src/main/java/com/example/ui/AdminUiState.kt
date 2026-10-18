package com.example.ui

import com.example.data.AdminUserItem

data class AdminUiState(
  val users: List<AdminUserItem> = emptyList(),
  val searchQuery: String = "",
  val isLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val selectedUser: AdminUserItem? = null,
  val selectedRoleChoice: String = "student",
  val showConfirmDialog: Boolean = false
)
