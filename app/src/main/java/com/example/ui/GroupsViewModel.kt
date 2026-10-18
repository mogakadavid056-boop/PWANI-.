package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DiscussionGroupItem
import com.example.data.GroupMemberItem
import com.example.data.GroupMessageItem
import com.example.data.GroupsService
import com.example.data.UserSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GroupsViewModel : ViewModel() {

  private val groupsService = GroupsService()
  private val _uiState = MutableStateFlow(GroupsUiState())
  val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

  private var messagePollJob: Job? = null

  fun loadGroups(classId: String, session: UserSession?) {
    _uiState.update { it.copy(classId = classId, isLoadingGroups = true, errorMessage = null) }
    if (session == null || session.accessToken.isBlank()) {
      _uiState.update { it.copy(isLoadingGroups = false) }; return
    }
    viewModelScope.launch {
      groupsService.listGroups(classId, session.accessToken).fold(
        onSuccess = { list -> _uiState.update { it.copy(isLoadingGroups = false, groups = list) } },
        onFailure = { err -> _uiState.update { it.copy(isLoadingGroups = false, errorMessage = err.message) } }
      )
    }
  }

  fun refreshGroups(session: UserSession?) {
    val cid = _uiState.value.classId
    if (cid.isNotBlank()) loadGroups(cid, session)
  }

  fun joinGroup(group: DiscussionGroupItem, session: UserSession?) {
    val cid = _uiState.value.classId
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      groupsService.joinGroup(group.id, session.accessToken).fold(
        onSuccess = {
          _uiState.update { it.copy(isActionLoading = false, successMessage = "Joined ${group.name}!") }
          loadGroups(cid, session)
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun createGroup(name: String, desc: String, size: Int, session: UserSession?, onSuccess: () -> Unit) {
    val cid = _uiState.value.classId
    if (session == null || cid.isBlank()) return
    if (name.isBlank()) { _uiState.update { it.copy(errorMessage = "Group name cannot be empty") }; return }
    if (size !in 1..100) { _uiState.update { it.copy(errorMessage = "Max members must be between 1 and 100") }; return }

    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      groupsService.createGroup(cid, name, desc, size, session.accessToken).fold(
        onSuccess = {
          _uiState.update { it.copy(isActionLoading = false, successMessage = "Group created!") }
          loadGroups(cid, session); onSuccess()
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun openChat(group: DiscussionGroupItem, session: UserSession?) {
    _uiState.update { it.copy(activeGroup = group, messages = emptyList(), isLoadingMessages = true, errorMessage = null) }
    fetchInitialMessages(group.id, session)
    startMessagePolling(group.id, session)
  }

  fun closeChat() {
    stopMessagePolling()
    _uiState.update { it.copy(activeGroup = null, messages = emptyList()) }
  }

  private fun fetchInitialMessages(groupId: String, session: UserSession?) {
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      groupsService.getGroupMessages(groupId, null, 100, session.accessToken).fold(
        onSuccess = { msgs -> _uiState.update { it.copy(isLoadingMessages = false, messages = msgs) } },
        onFailure = { err -> _uiState.update { it.copy(isLoadingMessages = false, errorMessage = err.message) } }
      )
    }
  }

  private fun startMessagePolling(groupId: String, session: UserSession?) {
    stopMessagePolling()
    if (session == null || session.accessToken.isBlank()) return
    messagePollJob = viewModelScope.launch {
      while (isActive) {
        delay(3000L)
        val lastTimestamp = _uiState.value.messages.lastOrNull()?.createdAt
        val res = groupsService.getGroupMessages(groupId, lastTimestamp, null, session.accessToken)
        res.getOrNull()?.let { newMsgs ->
          if (newMsgs.isNotEmpty()) {
            _uiState.update { s ->
              val ids = s.messages.map { it.id }.toSet()
              s.copy(messages = s.messages + newMsgs.filter { it.id !in ids })
            }
          }
        }
      }
    }
  }

  fun stopMessagePolling() {
    messagePollJob?.cancel()
    messagePollJob = null
  }

  fun sendMessage(text: String, session: UserSession?) {
    val grp = _uiState.value.activeGroup ?: return
    if (session == null || session.accessToken.isBlank()) return
    val trimmed = text.trim()
    if (trimmed.isBlank()) return
    if (trimmed.length > 2000) { _uiState.update { it.copy(errorMessage = "Message exceeds 2000 characters") }; return }

    viewModelScope.launch {
      _uiState.update { it.copy(isSendingMessage = true, errorMessage = null) }
      groupsService.sendGroupMessage(grp.id, trimmed, session.accessToken).fold(
        onSuccess = {
          _uiState.update { it.copy(isSendingMessage = false) }
          val last = _uiState.value.messages.lastOrNull()?.createdAt
          groupsService.getGroupMessages(grp.id, last, null, session.accessToken).getOrNull()?.let { fresh ->
            _uiState.update { s ->
              val ids = s.messages.map { it.id }.toSet()
              s.copy(messages = s.messages + fresh.filter { it.id !in ids })
            }
          }
        },
        onFailure = { err -> _uiState.update { it.copy(isSendingMessage = false, errorMessage = err.message) } }
      )
    }
  }

  fun promptDeleteMessage(message: GroupMessageItem) { _uiState.update { it.copy(messagePendingDelete = message) } }
  fun dismissPromptDeleteMessage() { _uiState.update { it.copy(messagePendingDelete = null) } }

  fun confirmDeleteMessage(session: UserSession?) {
    val msg = _uiState.value.messagePendingDelete ?: return
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(messagePendingDelete = null, isActionLoading = true) }
      groupsService.deleteGroupMessage(msg.id, session.accessToken).fold(
        onSuccess = { _uiState.update { s -> s.copy(isActionLoading = false, messages = s.messages.filter { it.id != msg.id }) } },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun loadGroupMembers(session: UserSession?) {
    val grp = _uiState.value.activeGroup ?: return
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingMembers = true) }
      groupsService.listGroupMembers(grp.id, session.accessToken).fold(
        onSuccess = { list -> _uiState.update { it.copy(isLoadingMembers = false, groupMembers = list) } },
        onFailure = { err -> _uiState.update { it.copy(isLoadingMembers = false, errorMessage = err.message) } }
      )
    }
  }

  fun setShowLeaveGroupConfirm(show: Boolean) { _uiState.update { it.copy(showLeaveGroupConfirm = show) } }
  fun setShowDeleteGroupConfirm(show: Boolean) { _uiState.update { it.copy(showDeleteGroupConfirm = show) } }
  fun setShowEditGroupDialog(show: Boolean) { _uiState.update { it.copy(showEditGroupDialog = show) } }
  fun setMemberPendingRemoval(member: GroupMemberItem?) { _uiState.update { it.copy(memberPendingRemoval = member) } }

  fun leaveGroup(session: UserSession?, onLeft: () -> Unit) {
    val grp = _uiState.value.activeGroup ?: return
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, showLeaveGroupConfirm = false) }
      groupsService.leaveGroup(grp.id, session.accessToken).fold(
        onSuccess = { _uiState.update { it.copy(isActionLoading = false) }; closeChat(); refreshGroups(session); onLeft() },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun updateGroupDetails(name: String, desc: String, size: Int, session: UserSession?) {
    val grp = _uiState.value.activeGroup ?: return
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, showEditGroupDialog = false) }
      groupsService.updateGroup(grp.id, name, desc, size, session.accessToken).fold(
        onSuccess = {
          _uiState.update { s ->
            s.copy(isActionLoading = false, activeGroup = s.activeGroup?.copy(name = name, description = desc, maxMembers = size), successMessage = "Group updated!")
          }
          refreshGroups(session)
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun removeGroupMember(session: UserSession?) {
    val grp = _uiState.value.activeGroup ?: return
    val member = _uiState.value.memberPendingRemoval ?: return
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, memberPendingRemoval = null) }
      groupsService.removeGroupMember(grp.id, member.userId, session.accessToken).fold(
        onSuccess = {
          _uiState.update { it.copy(isActionLoading = false, successMessage = "Removed ${member.fullName}") }
          loadGroupMembers(session); refreshGroups(session)
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun deleteGroup(session: UserSession?, onDeleted: () -> Unit) {
    val grp = _uiState.value.activeGroup ?: return
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, showDeleteGroupConfirm = false) }
      groupsService.deleteGroup(grp.id, session.accessToken).fold(
        onSuccess = { _uiState.update { it.copy(isActionLoading = false) }; closeChat(); refreshGroups(session); onDeleted() },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
  fun clearSuccess() { _uiState.update { it.copy(successMessage = null) } }

  override fun onCleared() {
    super.onCleared()
    stopMessagePolling()
  }
}
