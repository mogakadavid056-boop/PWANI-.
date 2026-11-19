package com.example.admin.data

data class AdminPendingUnitItem(
  val courseId: String,
  val classId: String,
  val className: String,
  val code: String,
  val name: String,
  val description: String = "",
  val requestedBy: String = "",
  val requesterName: String = "Lecturer",
  val createdAt: String = ""
)

enum class AdminRiskLevel(val key: String, val label: String) {
  ALL("all", "All"),
  CRITICAL("critical", "Critical"),
  HIGH("high", "High"),
  WATCH("watch", "Watch"),
  OK("ok", "OK");

  companion object {
    fun fromKey(key: String?): AdminRiskLevel = when (key?.trim()?.lowercase()) {
      "critical" -> CRITICAL
      "high" -> HIGH
      "watch" -> WATCH
      "ok" -> OK
      else -> OK
    }

    fun classify(percentage: Double, missStreak: Int, totalSessions: Int): AdminRiskLevel {
      if (totalSessions <= 0) return OK
      if (missStreak >= 4 || percentage < 60.0) return CRITICAL
      if (missStreak == 3 || percentage < 75.0) return HIGH
      if (missStreak == 2 || percentage < 85.0) return WATCH
      return OK
    }
  }
}

data class AdminClassRiskSummary(
  val classId: String,
  val className: String,
  val schoolName: String,
  val studentCount: Int,
  val totalSessions: Int,
  val criticalCount: Int,
  val highCount: Int,
  val watchCount: Int,
  val okCount: Int
)

data class AdminAtRiskStudentItem(
  val classId: String,
  val className: String,
  val studentId: String,
  val fullName: String,
  val admissionNumber: String,
  val attended: Int,
  val excused: Int,
  val totalSessions: Int,
  val percentage: Double,
  val missStreak: Int,
  val lastAttendedAt: String,
  val riskLevel: AdminRiskLevel
)

data class AdminStudentTimelineItem(
  val sessionId: String,
  val title: String,
  val sessionType: String,
  val startsAt: String,
  val status: String,
  val excuseReason: String = ""
)

data class AdminRunAlertsResult(
  val processedClasses: Int,
  val warningsSent: Int,
  val alertsSent: Int
)
