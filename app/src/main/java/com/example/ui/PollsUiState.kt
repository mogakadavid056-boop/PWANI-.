package com.example.ui

import com.example.data.PollItem
import com.example.data.PollOption
import com.example.data.PollResultsData

data class PollsUiState(
  val classId: String? = null,
  val polls: List<PollItem> = emptyList(),
  val isLoading: Boolean = false,
  val isDetailLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val selectedPoll: PollItem? = null,
  val pollOptions: List<PollOption> = emptyList(),
  val pollResults: PollResultsData? = null,
  val selectedOptionIdForVote: String? = null,
  val showCreateDialog: Boolean = false,
  val showCloseConfirm: Boolean = false,
  val showDeleteConfirm: Boolean = false
)
