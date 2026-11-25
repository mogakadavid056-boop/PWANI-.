package com.example.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.admin.data.AdminAttendancePollsGroupsRepository
import com.example.admin.data.AdminDashboardStats
import com.example.admin.data.AdminExamsRepository
import com.example.admin.data.AdminSession
import com.example.admin.data.AdminUserItem
import com.example.admin.data.AdminUsersRepository
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

enum class AdminTab { OVERVIEW, CLASSES, APPROVALS, RISK, USERS, UNIVERSITY }
enum class UserFilterChip(val label: String) {
  ALL("All"), UNVERIFIED("Suspended"), LECTURERS("Lecturers"),
  CLASS_REPS("Class Reps"), STUDENTS("Students"), ADMINS("Admins")
}

data class AdminMainState(
  val activeTab: AdminTab = AdminTab.OVERVIEW,
  val stats: AdminDashboardStats = AdminDashboardStats(),
  val isStatsLoading: Boolean = false,
  val users: List<AdminUserItem> = emptyList(),
  val userSearch: String = "",
  val userFilter: UserFilterChip = UserFilterChip.ALL,
  val isUsersLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null
)

@OptIn(FlowPreview::class)
class AdminMainViewModel(
  private val usersRepo: AdminUsersRepository = AdminUsersRepository(),
  private val attendanceRepo: AdminAttendancePollsGroupsRepository = AdminAttendancePollsGroupsRepository(),
  private val examsRepo: AdminExamsRepository = AdminExamsRepository()
) : ViewModel() {

  private val _state = MutableStateFlow(AdminMainState())
  val state: StateFlow<AdminMainState> = _state.asStateFlow()

  private val userSearchFlow = MutableStateFlow("")
  private var session: AdminSession? = null
  private var searchJob: Job? = null

  init {
    viewModelScope.launch {
      userSearchFlow.debounce(380L).distinctUntilChanged().collectLatest { query ->
        fetchUsers(query)
      }
    }
  }

  fun setSession(newSession: AdminSession?) {
    val changed = session?.userId != newSession?.userId
    session = newSession
    if (newSession != null && changed) {
      refreshAll()
    } else if (newSession == null) {
      _state.value = AdminMainState()
    }
  }

  fun selectTab(tab: AdminTab) = _state.update { it.copy(activeTab = tab, errorMessage = null) }

  fun jumpToUnverifiedLecturers() {
    _state.update { it.copy(activeTab = AdminTab.USERS, userFilter = UserFilterChip.UNVERIFIED) }
  }

  fun refreshAll() {
    refreshStats()
    fetchUsers(_state.value.userSearch)
  }

  fun refreshStats() {
    val token = session?.accessToken ?: return
    viewModelScope.launch {
      _state.update { it.copy(isStatsLoading = true) }
      usersRepo.fetchDashboardStats(token).fold(
        onSuccess = { s -> _state.update { it.copy(stats = s, isStatsLoading = false) } },
        onFailure = { e -> _state.update { it.copy(isStatsLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun onUserSearchChanged(query: String) {
    _state.update { it.copy(userSearch = query) }
    userSearchFlow.value = query
  }

  fun onUserFilterChanged(chip: UserFilterChip) = _state.update { it.copy(userFilter = chip) }

  fun refreshUsers() = fetchUsers(_state.value.userSearch)

  private fun fetchUsers(query: String) {
    val token = session?.accessToken ?: return
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      _state.update { it.copy(isUsersLoading = true) }
      usersRepo.listUsers(query.trim(), 100, token).fold(
        onSuccess = { list -> _state.update { it.copy(users = list, isUsersLoading = false) } },
        onFailure = { e -> _state.update { it.copy(isUsersLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun assignRole(user: AdminUserItem, newRole: String, onUpdated: () -> Unit = {}) {
    val token = session?.accessToken ?: return
    _state.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      usersRepo.assignRole(user.id, newRole, token).fold(
        onSuccess = {
          _state.update { it.copy(isActionLoading = false, successMessage = "Updated role for ${user.fullName.ifBlank { user.email }}") }
          fetchUsers(_state.value.userSearch)
          refreshStats()
          onUpdated()
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun setLecturerVerified(user: AdminUserItem, verified: Boolean, onUpdated: () -> Unit = {}) {
    val token = session?.accessToken ?: return
    _state.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      usersRepo.setLecturerVerified(user.id, verified, token).fold(
        onSuccess = {
          _state.update {
            it.copy(
              isActionLoading = false,
              successMessage = if (verified) "${user.fullName} lecturer access restored" else "Suspended lecturer access for ${user.fullName}"
            )
          }
          fetchUsers(_state.value.userSearch)
          refreshStats()
          onUpdated()
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun deleteEmptyExpiredSessionsGlobal() {
    val token = session?.accessToken ?: return
    _state.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      attendanceRepo.deleteEmptyExpiredSessions(null, token).fold(
        onSuccess = { count ->
          val msg = if (count > 0) "Deleted $count empty expired session(s)" else "No empty expired sessions to clear"
          _state.update { it.copy(isActionLoading = false, successMessage = msg) }
          refreshStats()
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun runExpiredAttemptsGlobal() {
    val token = session?.accessToken ?: return
    _state.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      examsRepo.runExpiredAttempts(token).fold(
        onSuccess = { count ->
          val msg = if (count > 0) "Finalized $count expired exam attempt(s)" else "No expired exam attempts pending"
          _state.update { it.copy(isActionLoading = false, successMessage = msg) }
          refreshStats()
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun clearError() = _state.update { it.copy(errorMessage = null) }
  fun clearSuccess() = _state.update { it.copy(successMessage = null) }
}
