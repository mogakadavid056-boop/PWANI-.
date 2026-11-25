package com.example.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.admin.data.AdminGlobalJoinRequestItem
import com.example.admin.data.AdminMonitorContentRepository
import com.example.admin.data.AdminPendingUnitItem
import com.example.admin.data.AdminSession
import com.example.admin.data.AdminUnitsMaterialsRepository
import com.example.admin.data.AdminUserItem
import com.example.admin.data.AdminUsersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ApprovalsSubTab(val label: String) {
  UNITS("Pending Units"),
  LECTURERS("Lecturers"),
  JOIN_REQUESTS("Class Requests")
}

data class AdminApprovalsState(
  val subTab: ApprovalsSubTab = ApprovalsSubTab.UNITS,
  val pendingUnits: List<AdminPendingUnitItem> = emptyList(),
  val lecturers: List<AdminUserItem> = emptyList(),
  val joinRequests: List<AdminGlobalJoinRequestItem> = emptyList(),
  val lecturerSearch: String = "",
  val isLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null
)

class AdminApprovalsViewModel(
  private val unitsRepo: AdminUnitsMaterialsRepository = AdminUnitsMaterialsRepository(),
  private val usersRepo: AdminUsersRepository = AdminUsersRepository(),
  private val contentRepo: AdminMonitorContentRepository = AdminMonitorContentRepository()
) : ViewModel() {
  private val _state = MutableStateFlow(AdminApprovalsState())
  val state: StateFlow<AdminApprovalsState> = _state.asStateFlow()

  private var session: AdminSession? = null

  fun setSession(newSession: AdminSession?) {
    val changed = session?.userId != newSession?.userId
    session = newSession
    if (newSession != null && changed) refresh() else if (newSession == null) _state.value = AdminApprovalsState()
  }

  fun selectSubTab(tab: ApprovalsSubTab) = _state.update { it.copy(subTab = tab) }
  fun onLecturerSearchChanged(q: String) = _state.update { it.copy(lecturerSearch = q) }
  fun clearError() = _state.update { it.copy(errorMessage = null) }
  fun clearSuccess() = _state.update { it.copy(successMessage = null) }

  fun refresh() {
    val token = session?.accessToken ?: return
    viewModelScope.launch {
      _state.update { it.copy(isLoading = true, errorMessage = null) }
      val unitsRes = unitsRepo.listPendingUnits(token)
      val usersRes = usersRepo.listUsers("", 200, token)
      val joinRes = contentRepo.listGlobalPendingJoinRequests(token)
      val fetchedLecturers = usersRes.getOrNull()
        ?.filter { it.isLecturer }
        ?.sortedWith(compareBy<AdminUserItem> { it.isVerified }.thenBy { it.fullName.lowercase() })
      val errs = listOfNotNull(
        unitsRes.exceptionOrNull()?.message,
        usersRes.exceptionOrNull()?.message,
        joinRes.exceptionOrNull()?.message
      ).distinct().joinToString(" • ").ifBlank { null }
      _state.update { s ->
        s.copy(
          isLoading = false,
          pendingUnits = unitsRes.getOrElse { s.pendingUnits },
          lecturers = fetchedLecturers ?: s.lecturers,
          joinRequests = joinRes.getOrElse { s.joinRequests },
          errorMessage = errs
        )
      }
    }
  }

  fun reviewUnit(item: AdminPendingUnitItem, approve: Boolean, reason: String?) {
    val token = session?.accessToken ?: return
    viewModelScope.launch {
      _state.update { it.copy(isActionLoading = true, errorMessage = null) }
      unitsRepo.reviewUnit(item.courseId, approve, reason, token).fold(
        onSuccess = {
          _state.update {
            it.copy(
              isActionLoading = false,
              successMessage = if (approve) "Approved ${item.code}" else "Rejected ${item.code}"
            )
          }
          refresh()
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun toggleVerifyLecturer(user: AdminUserItem, verify: Boolean, onUsersChanged: () -> Unit = {}) {
    val token = session?.accessToken ?: return
    viewModelScope.launch {
      _state.update { it.copy(isActionLoading = true, errorMessage = null) }
      usersRepo.setLecturerVerified(user.id, verify, token).fold(
        onSuccess = {
          _state.update {
            it.copy(
              isActionLoading = false,
              successMessage = if (verify) "${user.fullName} lecturer access restored" else "Suspended lecturer access for ${user.fullName}"
            )
          }
          refresh()
          onUsersChanged()
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun reviewJoinRequest(item: AdminGlobalJoinRequestItem, approve: Boolean, onUpdated: () -> Unit = {}) {
    val token = session?.accessToken ?: return
    viewModelScope.launch {
      _state.update { it.copy(isActionLoading = true, errorMessage = null) }
      contentRepo.reviewJoinRequest(item.classId, item.userId, approve, token).fold(
        onSuccess = {
          _state.update {
            it.copy(
              isActionLoading = false,
              successMessage = if (approve) "Approved ${item.fullName} for ${item.className}" else "Rejected request from ${item.fullName}"
            )
          }
          refresh()
          onUpdated()
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }
}
