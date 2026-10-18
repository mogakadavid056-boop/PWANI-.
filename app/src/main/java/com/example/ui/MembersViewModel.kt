package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ClassMemberItem
import com.example.data.MembersService
import com.example.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.data.PendingLecturerItem

data class MembersUiState(
  val classId: String = "",
  val members: List<ClassMemberItem> = emptyList(),
  val pendingLecturers: List<PendingLecturerItem> = emptyList(),
  val isLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val memberPendingRemoval: ClassMemberItem? = null,
  val lecturerPendingRejection: PendingLecturerItem? = null
)

class MembersViewModel : ViewModel() {

  private val membersService = MembersService()

  private val _uiState = MutableStateFlow(MembersUiState())
  val uiState: StateFlow<MembersUiState> = _uiState.asStateFlow()

  private val lecturerService = com.example.data.LecturerService()

  fun loadMembers(classId: String, session: UserSession?) {
    _uiState.update { it.copy(classId = classId, isLoading = true, errorMessage = null) }
    if (session == null || session.accessToken.isBlank()) {
      _uiState.update { it.copy(isLoading = false, errorMessage = "User not logged in") }
      return
    }

    viewModelScope.launch {
      val result = membersService.listClassMembers(classId, session.accessToken)
      if (result.isSuccess) {
        val list = result.getOrDefault(emptyList())
        _uiState.update { it.copy(members = list, isLoading = false) }
      } else {
        val error = result.exceptionOrNull()?.message ?: "Failed to load class members"
        _uiState.update { it.copy(isLoading = false, errorMessage = error) }
      }
    }
    loadPendingLecturers(classId, session)
  }

  fun loadPendingLecturers(classId: String, session: UserSession?) {
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      val res = lecturerService.listPendingLecturers(classId, session.accessToken)
      if (res.isSuccess) {
        _uiState.update { it.copy(pendingLecturers = res.getOrDefault(emptyList())) }
      }
    }
  }

  fun approveLecturer(item: PendingLecturerItem, session: UserSession?) {
    val cid = _uiState.value.classId
    if (session == null || cid.isBlank()) return

    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      val res = lecturerService.approvePendingLecturer(cid, item.userId, session.accessToken)
      res.fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              isActionLoading = false,
              successMessage = "Approved ${item.fullName} as lecturer for this class"
            )
          }
          loadMembers(cid, session)
        },
        onFailure = { err ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) }
        }
      )
    }
  }

  fun promptRejectLecturer(item: PendingLecturerItem) {
    _uiState.update { it.copy(lecturerPendingRejection = item) }
  }

  fun dismissPromptRejectLecturer() {
    _uiState.update { it.copy(lecturerPendingRejection = null) }
  }

  fun confirmRejectLecturer(session: UserSession?) {
    val item = _uiState.value.lecturerPendingRejection ?: return
    val cid = _uiState.value.classId
    if (session == null || cid.isBlank()) return

    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, lecturerPendingRejection = null, errorMessage = null) }
      val res = lecturerService.rejectPendingLecturer(cid, item.userId, session.accessToken)
      res.fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              isActionLoading = false,
              successMessage = "Rejected lecturer request for ${item.fullName}"
            )
          }
          loadMembers(cid, session)
        },
        onFailure = { err ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) }
        }
      )
    }
  }

  fun refreshMembers(session: UserSession?) {
    val cid = _uiState.value.classId
    if (cid.isNotBlank()) {
      loadMembers(cid, session)
    }
  }

  fun appointRep(targetMember: ClassMemberItem, session: UserSession?) {
    val cid = _uiState.value.classId
    if (session == null || cid.isBlank()) return

    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      val result = membersService.appointClassRep(cid, targetMember.userId, session.accessToken)
      if (result.isSuccess) {
        _uiState.update {
          it.copy(
            isActionLoading = false,
            successMessage = "${targetMember.fullName} appointed as Class Prefect"
          )
        }
        loadMembers(cid, session)
      } else {
        val err = result.exceptionOrNull()?.message ?: "Failed to appoint class prefect"
        _uiState.update { it.copy(isActionLoading = false, errorMessage = err) }
      }
    }
  }

  fun removeRep(targetMember: ClassMemberItem, session: UserSession?) {
    val cid = _uiState.value.classId
    if (session == null || cid.isBlank()) return

    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      val result = membersService.removeClassRep(cid, targetMember.userId, session.accessToken)
      if (result.isSuccess) {
        _uiState.update {
          it.copy(
            isActionLoading = false,
            successMessage = "Removed Class Prefect role for ${targetMember.fullName}"
          )
        }
        loadMembers(cid, session)
      } else {
        val err = result.exceptionOrNull()?.message ?: "Failed to remove class prefect"
        _uiState.update { it.copy(isActionLoading = false, errorMessage = err) }
      }
    }
  }

  fun promptRemoveMember(member: ClassMemberItem) {
    _uiState.update { it.copy(memberPendingRemoval = member) }
  }

  fun dismissPromptRemoveMember() {
    _uiState.update { it.copy(memberPendingRemoval = null) }
  }

  fun confirmRemoveMember(session: UserSession?) {
    val member = _uiState.value.memberPendingRemoval ?: return
    val cid = _uiState.value.classId
    if (session == null || cid.isBlank()) return

    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, memberPendingRemoval = null, errorMessage = null) }
      val result = membersService.removeFromClass(cid, member.userId, session.accessToken)
      if (result.isSuccess) {
        _uiState.update {
          it.copy(
            isActionLoading = false,
            successMessage = "${member.fullName} removed from class"
          )
        }
        loadMembers(cid, session)
      } else {
        val err = result.exceptionOrNull()?.message ?: "Failed to remove member from class"
        _uiState.update { it.copy(isActionLoading = false, errorMessage = err) }
      }
    }
  }

  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun clearSuccess() {
    _uiState.update { it.copy(successMessage = null) }
  }
}
