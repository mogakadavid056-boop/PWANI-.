package com.example.data

data class CourseUnit(
  val courseId: String,
  val code: String,
  val name: String,
  val description: String = "",
  val status: String = "approved",
  val lecturers: String = "",
  val lecturerIds: List<String> = emptyList(),
  val materialCount: Int = 0,
  val rejectionReason: String = ""
) {
  val isApproved: Boolean
    get() = status.equals("approved", ignoreCase = true)

  val isPending: Boolean
    get() = status.equals("pending", ignoreCase = true)

  val isRejected: Boolean
    get() = status.equals("rejected", ignoreCase = true)

  val displayLabel: String
    get() = if (code.isNotBlank() && name.isNotBlank()) "$code - $name" else code.ifBlank { name }
}
