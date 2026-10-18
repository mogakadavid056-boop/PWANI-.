package com.example.data

data class DiscussionGroupItem(
  val id: String,
  val name: String,
  val description: String,
  val memberCount: Int,
  val maxMembers: Int,
  val isMember: Boolean,
  val creatorId: String? = null,
  val isOwner: Boolean = false
) {
  val isFull: Boolean get() = maxMembers > 0 && memberCount >= maxMembers
}

data class GroupMessageItem(
  val id: String,
  val groupId: String,
  val senderId: String,
  val senderName: String,
  val messageText: String,
  val createdAt: String
)

data class GroupMemberItem(
  val userId: String,
  val fullName: String,
  val admissionNumber: String,
  val isOwner: Boolean = false,
  val role: String? = null
)
