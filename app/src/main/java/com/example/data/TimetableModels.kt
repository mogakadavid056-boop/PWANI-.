package com.example.data

data class ScheduleItem(
  val id: String,
  val title: String,
  val lecturerName: String,
  val dayOfWeek: Int,
  val startTime: String,
  val endTime: String,
  val room: String
) {
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
