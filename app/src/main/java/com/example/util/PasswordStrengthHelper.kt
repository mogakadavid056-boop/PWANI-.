package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class PasswordStrength(val label: String, val level: Int) {
  WEAK("Weak", 1),
  OK("OK", 2),
  STRONG("Strong", 3)
}

object PasswordStrengthHelper {
  private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

  fun isValidName(name: String): Boolean = name.trim().length >= 3

  fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

  fun isValidPassword(password: String): Boolean =
    password.length >= 8 && password.any { it.isLetter() } && password.any { it.isDigit() }

  fun passwordsMatch(password: String, confirm: String): Boolean =
    password.isNotEmpty() && password == confirm

  fun evaluateStrength(password: String): PasswordStrength {
    if (password.isEmpty() || !isValidPassword(password)) return PasswordStrength.WEAK
    var score = 0
    if (password.length >= 10) score++
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return when {
      score >= 2 -> PasswordStrength.STRONG
      else -> PasswordStrength.OK
    }
  }

  fun currentUtcIso8601(): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    fmt.timeZone = TimeZone.getTimeZone("UTC")
    return fmt.format(Date())
  }

  fun formatRegistrationError(rawMessage: String?): String {
    val msg = rawMessage.orEmpty().trim()
    if (msg.isBlank()) return "Could not create your account. Please try again."
    val lower = msg.lowercase()
    return when {
      lower.contains("already registered") ||
        lower.contains("already exists") ||
        lower.contains("user_already_exists") ||
        lower.contains("duplicate") ->
        "This email already has an account. Try logging in."
      lower.contains("invalid email") || lower.contains("unable to validate email") ->
        "Please enter a valid email address."
      lower.contains("rate limit") || lower.contains("too many requests") || lower.contains("429") ->
        "Too many attempts right now. Please wait a minute and try again."
      lower.contains("password") && (lower.contains("6") || lower.contains("weak") || lower.contains("short")) ->
        "Please choose a stronger password with at least 8 characters, a letter, and a number."
      lower.contains("cannot reach the server") || lower.contains("unable to resolve host") ->
        "Cannot reach the server. Check your internet and try again."
      else -> msg
    }
  }
}
