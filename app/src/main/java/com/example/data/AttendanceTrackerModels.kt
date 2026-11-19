package com.example.data

enum class RiskLevel(val key: String, val label: String) {
  ALL("all", "All"),
  CRITICAL("critical", "Critical"),
  HIGH("high", "High"),
  WATCH("watch", "Watch"),
  OK("ok", "OK");

  companion object {
    fun fromKey(raw: String?): RiskLevel = when (raw?.trim()?.lowercase()) {
      "critical" -> CRITICAL
      "high" -> HIGH
      "watch" -> WATCH
      "ok" -> OK
      else -> OK
    }

    fun classify(percentage: Double, missStreak: Int, totalSessions: Int): RiskLevel {
      if (totalSessions <= 0) return OK
      return when {
        missStreak >= 4 || percentage < 60.0 -> CRITICAL
        missStreak == 3 || percentage < 75.0 -> HIGH
        missStreak == 2 || percentage < 85.0 -> WATCH
        else -> OK
      }
    }
  }
}

data class StudentRiskItem(
  val studentId: String,
  val fullName: String,
  val admissionNumber: String,
  val attended: Int,
  val excused: Int,
  val totalSessions: Int,
  val percentage: Double,
  val missStreak: Int,
  val lastAttendedAt: String,
  val riskLevel: RiskLevel
)

data class StudentSessionTimelineItem(
  val sessionId: String,
  val title: String,
  val sessionType: String,
  val startsAt: String,
  val status: String,
  val excuseReason: String
)

data class MyAttendanceStatus(
  val attended: Int,
  val excused: Int,
  val totalSessions: Int,
  val percentage: Double,
  val missStreak: Int,
  val lastAttendedAt: String,
  val riskLevel: RiskLevel
) {
  val encouragementMessage: String
    get() = when (riskLevel) {
      RiskLevel.OK, RiskLevel.ALL -> "Great job, keep it up"
      RiskLevel.WATCH -> "Try to attend the next session"
      RiskLevel.HIGH -> "You have missed several sessions. If something is wrong, talk to your lecturer."
      RiskLevel.CRITICAL -> "Please speak to your lecturer or class prefect soon."
    }
}
