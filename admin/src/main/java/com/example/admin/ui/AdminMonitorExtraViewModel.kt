package com.example.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.admin.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant

data class AdminMonitorExtraState(
  val sessions: List<AdminAttendanceSession> = emptyList(),
  val selectedSession: AdminAttendanceSession? = null,
  val attendanceRecords: List<AdminAttendanceRecord> = emptyList(),
  val liveCode: String = "",
  val polls: List<AdminPollItem> = emptyList(),
  val selectedPoll: AdminPollItem? = null,
  val pollResults: AdminPollResults? = null,
  val groups: List<AdminGroupItem> = emptyList(),
  val selectedGroup: AdminGroupItem? = null,
  val groupMessages: List<AdminGroupMessage> = emptyList(),
  val groupMembers: List<AdminGroupMember> = emptyList(),
  val isLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null
)

class AdminMonitorExtraViewModel(
  private val repo: AdminAttendancePollsGroupsRepository = AdminAttendancePollsGroupsRepository(),
  private val oversightRepo: AdminOversightRepository = AdminOversightRepository()
) : ViewModel() {
  private val _state = MutableStateFlow(AdminMonitorExtraState())
  val state: StateFlow<AdminMonitorExtraState> = _state.asStateFlow()

  private var session: AdminSession? = null
  private var currentClassId: String = ""
  private var codeJob: Job? = null

  fun setSession(newSession: AdminSession?) {
    session = newSession
    if (newSession == null) stopAndClear()
  }

  fun loadForClass(classId: String) {
    currentClassId = classId
    stopCodeLoop()
    _state.update { it.copy(selectedSession = null, selectedPoll = null, selectedGroup = null, errorMessage = null) }
    refreshAll()
  }

  fun stopAndClear() {
    stopCodeLoop()
    _state.value = AdminMonitorExtraState()
  }

  private fun stopCodeLoop() { codeJob?.cancel(); codeJob = null }
  fun clearError() = _state.update { it.copy(errorMessage = null) }
  fun clearSuccess() = _state.update { it.copy(successMessage = null) }

  fun refreshAll() {
    val token = session?.accessToken ?: return
    if (currentClassId.isBlank()) return
    viewModelScope.launch {
      _state.update { it.copy(isLoading = true) }
      val sessRes = repo.fetchSessions(currentClassId, token)
      val pollRes = repo.listPolls(currentClassId, token)
      val grpRes = repo.listGroups(currentClassId, token)
      _state.update { s ->
        s.copy(
          isLoading = false,
          sessions = sessRes.getOrElse { s.sessions },
          polls = pollRes.getOrElse { s.polls },
          groups = grpRes.getOrElse { s.groups },
          errorMessage = sessRes.exceptionOrNull()?.message ?: pollRes.exceptionOrNull()?.message
        )
      }
      val open = _state.value.sessions.firstOrNull { it.active || it.status.equals("open", true) }
      if (open != null) startLiveCodeLoop(open.id) else _state.update { it.copy(liveCode = "") }
    }
  }

  private fun startLiveCodeLoop(sessionId: String) {
    val token = session?.accessToken ?: return
    stopCodeLoop()
    codeJob = viewModelScope.launch {
      while (isActive) {
        repo.fetchOrRotateLiveCode(sessionId, token).onSuccess { c -> _state.update { it.copy(liveCode = c) } }
        repo.getAttendanceList(sessionId, token).onSuccess { list -> _state.update { it.copy(attendanceRecords = list) } }
        delay(60_000L)
      }
    }
  }

  fun startSession(courseId: String?, title: String, minutes: Int) {
    val token = session?.accessToken ?: return
    val expiry = Instant.now().plusSeconds(minutes * 60L).toString()
    viewModelScope.launch {
      _state.update { it.copy(isActionLoading = true, errorMessage = null) }
      repo.startSession(currentClassId, courseId, title, expiry, token).fold(
        onSuccess = { (sid, code) ->
          _state.update { it.copy(isActionLoading = false, liveCode = code, successMessage = "Session started (Code: $code)") }
          refreshAll()
          if (sid.isNotBlank()) startLiveCodeLoop(sid)
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun openSession(item: AdminAttendanceSession) {
    val token = session?.accessToken ?: return
    _state.update { it.copy(selectedSession = item, isLoading = true) }
    viewModelScope.launch {
      repo.getAttendanceList(item.id, token).fold(
        onSuccess = { list -> _state.update { it.copy(attendanceRecords = list, isLoading = false) } },
        onFailure = { e -> _state.update { it.copy(isLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun closeSessionDetail() = _state.update { it.copy(selectedSession = null) }

  fun closeAttendanceSession(sessionId: String) {
    val token = session?.accessToken ?: return
    viewModelScope.launch {
      _state.update { it.copy(isActionLoading = true) }
      oversightRepo.closeSession(sessionId, token).fold(
        onSuccess = { stopCodeLoop(); _state.update { it.copy(isActionLoading = false, liveCode = "", selectedSession = null, successMessage = "Session closed") }; refreshAll() },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun deleteAttendanceSession(sessionId: String, reason: String) {
    val token = session?.accessToken ?: return
    viewModelScope.launch {
      _state.update { it.copy(isActionLoading = true) }
      repo.deleteSession(sessionId, reason, token).fold(
        onSuccess = { stopCodeLoop(); _state.update { it.copy(isActionLoading = false, selectedSession = null, successMessage = "Session deleted") }; refreshAll() },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun openPoll(poll: AdminPollItem) {
    val token = session?.accessToken ?: return
    _state.update { it.copy(selectedPoll = poll, isLoading = true) }
    viewModelScope.launch {
      repo.getPollResults(poll.id, token).fold(
        onSuccess = { res -> _state.update { it.copy(pollResults = res, isLoading = false) } },
        onFailure = { e -> _state.update { it.copy(isLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun closePollDetail() = _state.update { it.copy(selectedPoll = null, pollResults = null) }

  fun createPoll(question: String, options: List<String>, hours: Int) {
    val token = session?.accessToken ?: return
    val expiry = Instant.now().plusSeconds(hours * 3600L).toString()
    viewModelScope.launch {
      _state.update { it.copy(isActionLoading = true) }
      repo.createPoll(currentClassId, question, options, expiry, token).fold(
        onSuccess = { _state.update { it.copy(isActionLoading = false, successMessage = "Poll created") }; refreshAll() },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun closePoll(pollId: String) = runSimple("Poll closed") { repo.closePoll(pollId, it) }
  fun deletePoll(pollId: String) {
    _state.update { it.copy(selectedPoll = null) }
    runSimple("Poll deleted") { repo.deletePoll(pollId, it) }
  }

  fun openGroup(group: AdminGroupItem) {
    val token = session?.accessToken ?: return
    _state.update { it.copy(selectedGroup = group, isLoading = true) }
    viewModelScope.launch {
      val msgs = repo.getGroupMessages(group.id, token)
      val mems = repo.listGroupMembers(group.id, token)
      _state.update {
        it.copy(
          isLoading = false,
          groupMessages = msgs.getOrDefault(emptyList()),
          groupMembers = mems.getOrDefault(emptyList()),
          errorMessage = msgs.exceptionOrNull()?.message ?: mems.exceptionOrNull()?.message
        )
      }
    }
  }

  fun closeGroupDetail() = _state.update { it.copy(selectedGroup = null, groupMessages = emptyList(), groupMembers = emptyList()) }
  fun deleteGroupMessage(msgId: String) = runSimple("Message deleted") { tok ->
    repo.deleteGroupMessage(msgId, tok).also { if (it.isSuccess) _state.value.selectedGroup?.let(::openGroup) }
  }
  fun removeGroupMember(groupId: String, userId: String) = runSimple("Removed from group") { tok ->
    repo.removeGroupMember(groupId, userId, tok).also { if (it.isSuccess) _state.value.selectedGroup?.let(::openGroup) }
  }
  fun deleteGroup(groupId: String) {
    _state.update { it.copy(selectedGroup = null) }
    runSimple("Group deleted") { repo.deleteGroup(groupId, it) }
  }

  private fun runSimple(okMsg: String, block: suspend (String) -> Result<Unit>) {
    val token = session?.accessToken ?: return
    viewModelScope.launch {
      _state.update { it.copy(isActionLoading = true, errorMessage = null) }
      block(token).fold(
        onSuccess = { _state.update { it.copy(isActionLoading = false, successMessage = okMsg) }; refreshAll() },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }
}
