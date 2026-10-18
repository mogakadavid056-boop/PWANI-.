package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminService
import com.example.data.AdminUserItem
import com.example.data.UserSession
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class AdminViewModel(
  private val adminService: AdminService = AdminService()
) : ViewModel() {

  private val _uiState = MutableStateFlow(AdminUiState())
  val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

  private val _searchQueryFlow = MutableStateFlow("")
  private var activeSession: UserSession? = null
  private var searchJob: Job? = null

  init {
    viewModelScope.launch {
      _searchQueryFlow
        .debounce(400L)
        .distinctUntilChanged()
        .collectLatest { query ->
          executeSearch(query)
        }
    }
  }

  fun setSession(session: UserSession?) {
    val previousSession = activeSession
    activeSession = session
    if (session != null && (previousSession?.userId != session.userId || _uiState.value.users.isEmpty())) {
      refreshUsers()
    }
  }

  fun onSearchQueryChanged(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
    _searchQueryFlow.value = query
  }

  fun refreshUsers() {
    executeSearch(_uiState.value.searchQuery)
  }

  private fun executeSearch(query: String) {
    val session = activeSession ?: return
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val result = adminService.listUsers(searchText = query.trim(), maxRows = 20, accessToken = session.accessToken)
      result.fold(
        onSuccess = { list ->
          _uiState.update { it.copy(users = list, isLoading = false) }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(isLoading = false, errorMessage = err.message ?: "Failed to list users")
          }
        }
      )
    }
  }

  fun selectUserForRoleChange(user: AdminUserItem) {
    val initialChoice = when (user.role.trim().lowercase()) {
      "admin" -> "admin"
      "lecturer" -> "lecturer"
      else -> "student"
    }
    _uiState.update {
      it.copy(
        selectedUser = user,
        selectedRoleChoice = initialChoice,
        showConfirmDialog = false,
        errorMessage = null,
        successMessage = null
      )
    }
  }

  fun updateRoleChoice(newRole: String) {
    _uiState.update { it.copy(selectedRoleChoice = newRole) }
  }

  fun promptConfirmRole() {
    _uiState.update { it.copy(showConfirmDialog = true) }
  }

  fun dismissConfirmDialog() {
    _uiState.update { it.copy(showConfirmDialog = false) }
  }

  fun dismissRoleDialog() {
    _uiState.update {
      it.copy(
        selectedUser = null,
        showConfirmDialog = false
      )
    }
  }

  fun confirmAssignRole() {
    val user = _uiState.value.selectedUser ?: return
    val newRole = _uiState.value.selectedRoleChoice
    val session = activeSession ?: return

    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      val result = adminService.assignRole(
        targetUserId = user.id,
        newRole = newRole,
        accessToken = session.accessToken
      )
      result.fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              isActionLoading = false,
              selectedUser = null,
              showConfirmDialog = false,
              successMessage = "Role updated successfully for ${user.fullName.ifBlank { "user" }}"
            )
          }
          refreshUsers()
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isActionLoading = false,
              showConfirmDialog = false,
              errorMessage = err.message ?: "Failed to assign role"
            )
          }
        }
      )
    }
  }

  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun clearSuccess() {
    _uiState.update { it.copy(successMessage = null) }
  }
}
