package com.example.data

import java.time.Duration
import java.time.Instant

data class PollItem(
  val id: String,
  val question: String,
  val totalVotes: Int = 0,
  val hasVoted: Boolean = false,
  val active: Boolean = true,
  val expiresAt: String? = null,
  val createdAt: String? = null
) {
  fun isOpen(): Boolean {
    if (!active) return false
    if (expiresAt.isNullOrBlank()) return true
    return try {
      val expiry = Instant.parse(expiresAt)
      Instant.now().isBefore(expiry)
    } catch (_: Exception) {
      active
    }
  }

  fun statusBadge(): Pair<Boolean, String> {
    if (!active) return false to "Closed"
    if (expiresAt.isNullOrBlank()) return true to "Open"
    return try {
      val expiry = Instant.parse(expiresAt)
      val now = Instant.now()
      val diff = Duration.between(now, expiry).seconds
      if (diff <= 0) {
        false to "Closed"
      } else {
        val days = diff / 86400
        val hours = (diff % 86400) / 3600
        val mins = (diff % 3600) / 60
        val text = when {
          days > 1 -> "${days}d left"
          days == 1L -> "1d ${hours}h left"
          hours > 0 -> "${hours}h ${mins}m left"
          mins > 0 -> "${mins}m left"
          else -> "${diff}s left"
        }
        true to "Open • $text"
      }
    } catch (_: Exception) {
      if (active) true to "Open" else false to "Closed"
    }
  }
}

data class PollOption(
  val id: String,
  val optionText: String,
  val position: Int = 0
)

data class PollResultOption(
  val id: String,
  val optionText: String,
  val votes: Int,
  val percentage: Double,
  val isMyVote: Boolean = false
)

data class PollResultsData(
  val totalVotes: Int,
  val options: List<PollResultOption>
)
