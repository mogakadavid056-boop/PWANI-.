package com.example.data

data class SearchClassItem(
  val id: String,
  val className: String,
  val classCode: String = "",
  val schoolName: String = "",
  val programmeName: String = "",
  val studentCount: Int = 0,
  val hasPrefect: Boolean = false,
  val isMine: Boolean = false
)
