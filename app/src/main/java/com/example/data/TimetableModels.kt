package com.example.data

import com.example.util.JsonHelper

data class ScheduleItem(
  val id: String,
  val title: String,
  val lecturerName: String,
  val dayOfWeek: Int,
  val startTime: String,
  val endTime: String,
  val room: String,
  val status: String = "scheduled",
  val statusNote: String = "",
  val classId: String = "",
  val courseId: String = "",
  val courseCode: String = ""
) {
  val normalizedStatus: String
    get() = when (status.trim().lowercase()) {
      "cancelled", "canceled" -> "cancelled"
      "moved", "rescheduled" -> "moved"
      else -> "scheduled"
    }

  val isCancelled: Boolean
    get() = normalizedStatus == "cancelled"

  val isMoved: Boolean
    get() = normalizedStatus == "moved"

  val courseKey: String
    get() = courseId.ifBlank { courseCode.ifBlank { title.trim().lowercase() } }

  val displayCourseLabel: String
    get() = JsonHelper.orDash(courseCode.ifBlank { title })

  val dayName: String
    get() = when (dayOfWeek) {
      1 -> "Monday"
      2 -> "Tuesday"
      3 -> "Wednesday"
      4 -> "Thursday"
      5 -> "Friday"
      6 -> "Saturday"
      7 -> "Sunday"
      else -> "Day $dayOfWeek"
    }

  val formattedTimeRange: String
    get() {
      val start = startTime.take(5)
      val end = endTime.take(5)
      return if (start.isNotBlank() && end.isNotBlank()) "$start - $end" else "$startTime - $endTime"
    }
}

data class RegisteredCourseItem(
  val courseId: String,
  val courseCode: String = "",
  val courseName: String = "",
  val classId: String = ""
)
