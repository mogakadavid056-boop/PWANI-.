package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PollItem
import com.example.data.PollsService
import com.example.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

class PollsViewModel(
  private val pollsService: PollsService = PollsService()
) : ViewModel() {

  private val _uiState = MutableStateFlow(PollsUiState())
  val uiState: StateFlow<PollsUiState> = _uiState.asStateFlow()

  fun loadPolls(classId: String, session: UserSession?) {
    val token = session?.accessToken.orEmpty()
    if (token.isBlank() || classId.isBlank()) return
    _uiState.update { it.copy(classId = classId, isLoading = true, errorMessage = null) }
    viewModelScope.launch {
      val res = pollsService.listPolls(classId, token)
      res.fold(
        onSuccess = { list ->
          _uiState.update { it.copy(polls = list, isLoading = false) }
        },
        onFailure = { err ->
          _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
        }
      )
    }
  }

  fun refreshPolls(session: UserSession?) {
    val cid = _uiState.value.classId ?: return
    loadPolls(cid, session)
  }

  fun openPoll(poll: PollItem, isStaff: Boolean, session: UserSession?) {
    val token = session?.accessToken.orEmpty()
    _uiState.update {
      it.copy(
        selectedPoll = poll,
        isDetailLoading = true,
        errorMessage = null,
        pollResults = null,
        pollOptions = emptyList(),
        selectedOptionIdForVote = null
      )
    }

    viewModelScope.launch {
      val needVote = poll.isOpen() && !poll.hasVoted && !isStaff
      if (needVote) {
        val resultAttempt = pollsService.getPollResults(poll.id, token)
        if (resultAttempt.isSuccess) {
          _uiState.update { it.copy(pollResults = resultAttempt.getOrNull(), isDetailLoading = false) }
        } else {
          val optRes = pollsService.fetchPollOptions(poll.id, token)
          optRes.fold(
            onSuccess = { opts ->
              _uiState.update { it.copy(pollOptions = opts, isDetailLoading = false) }
            },
            onFailure = { err ->
              _uiState.update { it.copy(isDetailLoading = false, errorMessage = err.message) }
            }
          )
        }
      } else {
        loadResultsOnly(poll.id, token)
      }
    }
  }

  private suspend fun loadResultsOnly(pollId: String, token: String) {
    val res = pollsService.getPollResults(pollId, token)
    res.fold(
      onSuccess = { data ->
        _uiState.update { it.copy(pollResults = data, isDetailLoading = false) }
      },
      onFailure = { err ->
        _uiState.update { it.copy(isDetailLoading = false, errorMessage = err.message) }
      }
    )
  }

  fun selectOptionForVote(optionId: String) {
    _uiState.update { it.copy(selectedOptionIdForVote = optionId) }
  }

  fun castVote(session: UserSession?) {
    val poll = _uiState.value.selectedPoll ?: return
    val optId = _uiState.value.selectedOptionIdForVote
    if (optId.isNullOrBlank()) {
      _uiState.update { it.copy(errorMessage = "Please select an option to vote") }
      return
    }
    val token = session?.accessToken.orEmpty()
    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      val res = pollsService.castVote(poll.id, optId, token)
      res.fold(
        onSuccess = {
          val updatedPoll = poll.copy(hasVoted = true, totalVotes = poll.totalVotes + 1)
          _uiState.update { s ->
            s.copy(
              selectedPoll = updatedPoll,
              polls = s.polls.map { if (it.id == poll.id) updatedPoll else it },
              isActionLoading = false,
              successMessage = "Vote recorded successfully"
            )
          }
          loadResultsOnly(poll.id, token)
          refreshPolls(session)
        },
        onFailure = { err ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) }
        }
      )
    }
  }

  fun createPoll(
    question: String,
    options: List<String>,
    durationLabel: String,
    session: UserSession?,
    onSuccess: () -> Unit
  ) {
    val cid = _uiState.value.classId ?: return
    val cleanQ = question.trim()
    if (cleanQ.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Question cannot be empty") }
      return
    }
    val cleanOpts = options.map { it.trim() }.filter { it.isNotBlank() }
    if (cleanOpts.size < 2) {
      _uiState.update { it.copy(errorMessage = "Please provide at least 2 options") }
      return
    }
    if (cleanOpts.size > 10) {
      _uiState.update { it.copy(errorMessage = "Maximum 10 options allowed") }
      return
    }
    val durationSeconds = when (durationLabel) {
      "1 hour" -> 3600L
      "6 hours" -> 6L * 3600L
      "24 hours" -> 24L * 3600L
      "3 days" -> 3L * 86400L
      "7 days" -> 7L * 86400L
      else -> 24L * 3600L
    }
    val expiryIso = Instant.now().plusSeconds(durationSeconds).toString()
    val token = session?.accessToken.orEmpty()

    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      val res = pollsService.createPoll(cid, cleanQ, cleanOpts, expiryIso, token)
      res.fold(
        onSuccess = {
          _uiState.update { it.copy(isActionLoading = false, showCreateDialog = false, successMessage = "Poll created") }
          refreshPolls(session)
          onSuccess()
        },
        onFailure = { err ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) }
        }
      )
    }
  }

  fun closePoll(session: UserSession?) {
    val poll = _uiState.value.selectedPoll ?: return
    val token = session?.accessToken.orEmpty()
    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      val res = pollsService.closePoll(poll.id, token)
      res.fold(
        onSuccess = {
          val updated = poll.copy(active = false)
          _uiState.update { s ->
            s.copy(
              selectedPoll = updated,
              polls = s.polls.map { if (it.id == poll.id) updated else it },
              isActionLoading = false,
              showCloseConfirm = false,
              successMessage = "Poll closed"
            )
          }
          loadResultsOnly(poll.id, token)
          refreshPolls(session)
        },
        onFailure = { err ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) }
        }
      )
    }
  }

  fun deletePoll(session: UserSession?, onSuccess: () -> Unit) {
    val poll = _uiState.value.selectedPoll ?: return
    val token = session?.accessToken.orEmpty()
    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      val res = pollsService.deletePoll(poll.id, token)
      res.fold(
        onSuccess = {
          _uiState.update { s ->
            s.copy(
              selectedPoll = null,
              polls = s.polls.filter { it.id != poll.id },
              isActionLoading = false,
              showDeleteConfirm = false,
              successMessage = "Poll deleted"
            )
          }
          refreshPolls(session)
          onSuccess()
        },
        onFailure = { err ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) }
        }
      )
    }
  }

  fun closePollDetail() {
    _uiState.update { it.copy(selectedPoll = null, pollResults = null, pollOptions = emptyList(), selectedOptionIdForVote = null) }
  }

  fun setShowCreateDialog(show: Boolean) = _uiState.update { it.copy(showCreateDialog = show) }
  fun setShowCloseConfirm(show: Boolean) = _uiState.update { it.copy(showCloseConfirm = show) }
  fun setShowDeleteConfirm(show: Boolean) = _uiState.update { it.copy(showDeleteConfirm = show) }
  fun clearError() = _uiState.update { it.copy(errorMessage = null) }
  fun clearSuccess() = _uiState.update { it.copy(successMessage = null) }
}
