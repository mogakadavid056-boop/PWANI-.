package com.example.data

data class UserSession(
  val userId: String,
  val email: String,
  val fullName: String,
  val admissionNumber: String,
  val role: String,
  val accessToken: String,
  val refreshToken: String? = null
)
