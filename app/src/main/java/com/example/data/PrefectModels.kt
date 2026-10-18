package com.example.data

data class ClassPrefectInfo(
  val userId: String,
  val fullName: String,
  val admissionNumber: String = ""
)

data class ClassWaitingForPrefect(
  val classId: String,
  val className: String,
  val studentCount: Int
)
