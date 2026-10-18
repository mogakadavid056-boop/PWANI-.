package com.example.ui

import com.example.data.DiscussionGroupItem
import com.example.data.GroupMemberItem
import com.example.data.GroupMessageItem

data class GroupsUiState(
  val classId: String = "",
  val groups: List<DiscussionGroupItem> = emptyList(),
  val isLoadingGroups: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val activeGroup: DiscussionGroupItem? = null,
  val messages: List<GroupMessageItem> = emptyList(),
  val isLoadingMessages: Boolean = false,
  val isSendingMessage: Boolean = false,
  val groupMembers: List<GroupMemberItem> = emptyList(),
  val isLoadingMembers: Boolean = false,
  val messagePendingDelete: GroupMessageItem? = null,
  val memberPendingRemoval: GroupMemberItem? = null,
  val showLeaveGroupConfirm: Boolean = false,
  val showDeleteGroupConfirm: Boolean = false,
  val showEditGroupDialog: Boolean = false
)
