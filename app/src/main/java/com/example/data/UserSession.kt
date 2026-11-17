package com.example.data

data class UserSession(
  val userId: String,
  val email: String,
  val fullName: String,
  val admissionNumber: String,
  val role: String,
  val accessToken: String,
  val refreshToken: String? = null,
  val isVerified: Boolean = false,
  val roleStatus: String = "approved",
  val staffNumber: String = "",
  val department: String = ""
)
