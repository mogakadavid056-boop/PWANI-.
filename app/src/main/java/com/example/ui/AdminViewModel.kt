package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminService
import com.example.data.AdminUserItem
import com.example.data.UserSession
import com.example.util.RoleHelper
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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

  fun setFilterTab(tab: String) {
    _uiState.update { it.copy(filterTab = tab) }
  }

  fun refreshUsers() {
    executeSearch(_uiState.value.searchQuery)
  }

  private fun executeSearch(query: String) {
    val session = activeSession ?: return
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val usersDeferred = async {
        adminService.listUsers(searchText = query.trim(), maxRows = 50, accessToken = session.accessToken)
      }
      val pendingDeferred = async {
        adminService.listPendingLecturers(accessToken = session.accessToken)
      }
      val usersResult = usersDeferred.await()
      val pendingResult = pendingDeferred.await()
      val pendingList = pendingResult.getOrDefault(_uiState.value.pendingLecturers)

      usersResult.fold(
        onSuccess = { list ->
          _uiState.update {
            it.copy(users = list, pendingLecturers = pendingList, isLoading = false)
          }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              pendingLecturers = pendingList,
              isLoading = false,
              errorMessage = err.message ?: "Failed to list users"
            )
          }
        }
      )
    }
  }

  fun selectUserForRoleChange(user: AdminUserItem) {
    val initialChoice = when (RoleHelper.normalizeRole(user.role)) {
      RoleHelper.ROLE_ADMIN -> RoleHelper.ROLE_ADMIN
      RoleHelper.ROLE_LECTURER -> RoleHelper.ROLE_LECTURER
      RoleHelper.ROLE_CLASS_REP -> RoleHelper.ROLE_CLASS_REP
      else -> RoleHelper.ROLE_STUDENT
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
    _uiState.update { it.copy(selectedUser = null, showConfirmDialog = false) }
  }

  fun confirmAssignRole() {
    val user = _uiState.value.selectedUser ?: return
    val newRole = _uiState.value.selectedRoleChoice
    val session = activeSession ?: return

    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      val result: Result<String> = when {
        newRole == RoleHelper.ROLE_CLASS_REP -> adminService.setClassRep(user.id, true, accessToken = session.accessToken)
        user.isClassRep && newRole == RoleHelper.ROLE_STUDENT -> adminService.setClassRep(user.id, false, accessToken = session.accessToken)
        else -> adminService.assignRole(targetUserId = user.id, newRole = newRole, accessToken = session.accessToken)
          .map { "Role updated successfully for ${user.fullName.ifBlank { "user" }}" }
      }
      result.fold(
        onSuccess = { msg ->
          _uiState.update {
            it.copy(
              isActionLoading = false,
              selectedUser = null,
              showConfirmDialog = false,
              successMessage = msg
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

  fun promptApproveLecturer(user: AdminUserItem, approve: Boolean) {
    _uiState.update {
      it.copy(approvalPendingUser = user, approvalDecisionApprove = approve, errorMessage = null)
    }
  }

  fun dismissApprovalDialog() {
    _uiState.update { it.copy(approvalPendingUser = null) }
  }

  fun confirmApproveLecturer() {
    val user = _uiState.value.approvalPendingUser ?: return
    val approve = _uiState.value.approvalDecisionApprove
    val session = activeSession ?: return

    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      val result = adminService.approveLecturer(user.id, approve, session.accessToken)
      result.fold(
        onSuccess = { msg ->
          _uiState.update {
            it.copy(isActionLoading = false, approvalPendingUser = null, successMessage = msg)
          }
          refreshUsers()
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isActionLoading = false,
              approvalPendingUser = null,
              errorMessage = err.message ?: "Failed to update lecturer approval"
            )
          }
        }
      )
    }
  }

  fun promptSetClassRep(user: AdminUserItem, isRep: Boolean) {
    _uiState.update {
      it.copy(repTogglePendingUser = user, repTargetIsRep = isRep, errorMessage = null)
    }
  }

  fun dismissRepToggleDialog() {
    _uiState.update { it.copy(repTogglePendingUser = null) }
  }

  fun confirmSetClassRep() {
    val user = _uiState.value.repTogglePendingUser ?: return
    val isRep = _uiState.value.repTargetIsRep
    val session = activeSession ?: return

    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      val result = adminService.setClassRep(user.id, isRep, accessToken = session.accessToken)
      result.fold(
        onSuccess = { msg ->
          _uiState.update {
            it.copy(isActionLoading = false, repTogglePendingUser = null, successMessage = msg)
          }
          refreshUsers()
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isActionLoading = false,
              repTogglePendingUser = null,
              errorMessage = err.message ?: "Failed to update Class Rep role"
            )
          }
        }
      )
    }
  }

  fun promptToggleLecturerVerification(user: AdminUserItem) {
    _uiState.update {
      it.copy(
        verificationPendingUser = user,
        verificationTargetStatus = !user.isVerified,
        showVerificationConfirmDialog = true,
        errorMessage = null
      )
    }
  }

  fun dismissVerificationDialog() {
    _uiState.update { it.copy(verificationPendingUser = null, showVerificationConfirmDialog = false) }
  }

  fun confirmToggleLecturerVerification() {
    val user = _uiState.value.verificationPendingUser ?: return
    val targetStatus = _uiState.value.verificationTargetStatus
    val session = activeSession ?: return
    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      adminService.setLecturerVerified(user.id, targetStatus, session.accessToken).fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              isActionLoading = false,
              verificationPendingUser = null,
              showVerificationConfirmDialog = false,
              successMessage = if (targetStatus) "Lecturer verified successfully" else "Lecturer verification removed"
            )
          }
          refreshUsers()
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isActionLoading = false,
              showVerificationConfirmDialog = false,
              errorMessage = err.message ?: "Failed to update lecturer verification"
            )
          }
        }
      )
    }
  }

  fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
  fun clearSuccess() { _uiState.update { it.copy(successMessage = null) } }
}
