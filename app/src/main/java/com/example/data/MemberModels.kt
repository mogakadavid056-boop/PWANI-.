package com.example.data

data class ClassMemberItem(
  val userId: String,
  val fullName: String,
  val admissionNumber: String,
  val isRep: Boolean = false,
  val role: String? = null
)
