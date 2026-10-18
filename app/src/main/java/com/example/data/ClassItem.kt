package com.example.data

data class ClassItem(
  val id: String,
  val name: String,
  val code: String,
  val joinCode: String? = null,
  val yearOfStudy: Int? = null,
  val programmeName: String? = null
)
