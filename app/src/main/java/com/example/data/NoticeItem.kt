package com.example.data

data class NoticeItem(
  val id: String,
  val title: String,
  val content: String,
  val category: String,
  val isUrgent: Boolean,
  val isPinned: Boolean,
  val createdAt: String,
  val className: String? = null
)
