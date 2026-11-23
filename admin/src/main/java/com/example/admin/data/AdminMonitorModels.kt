package com.example.admin.data

data class AdminSearchClassItem(
  val id: String,
  val className: String,
  val classCode: String = "",
  val schoolName: String = "",
  val programmeName: String = "",
  val studentCount: Int = 0,
  val hasPrefect: Boolean = false
)

data class AdminClassOverview(
  val classId: String,
  val className: String,
  val classCode: String = "",
  val joinCode: String = "",
  val yearOfStudy: Int? = null,
  val schoolName: String = "",
  val programmeName: String = "",
  val studentCount: Int = 0,
  val lecturerCount: Int = 0,
  val prefectCount: Int = 0,
  val unitCount: Int = 0,
  val materialCount: Int = 0,
  val noticeCount: Int = 0,
  val sessionCount: Int = 0,
  val pollCount: Int = 0
)

data class AdminNoticeItem(
  val id: String,
  val title: String,
  val content: String,
  val category: String = "general",
  val isUrgent: Boolean = false,
  val isPinned: Boolean = false,
  val createdAt: String = ""
)

data class AdminClassMemberItem(
  val userId: String,
  val fullName: String,
  val admissionNumber: String,
  val isRep: Boolean = false,
  val role: String = "student",
  val memberRole: String = "student"
)

data class AdminPendingLecturerItem(
  val userId: String,
  val fullName: String,
  val email: String = "",
  val staffNumber: String = ""
)

data class AdminGlobalJoinRequestItem(
  val classId: String,
  val className: String,
  val userId: String,
  val fullName: String,
  val email: String = "",
  val staffNumber: String = ""
)

data class AdminScheduleItem(
  val id: String,
  val title: String,
  val lecturerName: String = "",
  val dayOfWeek: Int = 1,
  val startTime: String = "",
  val endTime: String = "",
  val room: String = "",
  val status: String = "scheduled",
  val statusNote: String = ""
) {
  val dayName: String
    get() = when (dayOfWeek) {
      1 -> "Monday"; 2 -> "Tuesday"; 3 -> "Wednesday"; 4 -> "Thursday"
      5 -> "Friday"; 6 -> "Saturday"; 7 -> "Sunday"; else -> "Day $dayOfWeek"
    }
}

data class AdminCourseUnit(
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
  val isApproved: Boolean get() = status.equals("approved", ignoreCase = true)
  val isPending: Boolean get() = status.equals("pending", ignoreCase = true)
  val displayLabel: String get() = if (code.isNotBlank() && name.isNotBlank()) "$code - $name" else name.ifBlank { code }
}

data class AdminMaterialItem(
  val id: String,
  val classId: String,
  val courseId: String = "",
  val title: String,
  val description: String = "",
  val materialType: String = "notes",
  val externalUrl: String = "",
  val storagePath: String = "",
  val fileName: String = "",
  val fileSize: Long = 0L,
  val mimeType: String = "",
  val createdAt: String = ""
) {
  val isFile: Boolean get() = storagePath.isNotBlank()
}

data class AdminAttendanceSession(
  val id: String,
  val classId: String = "",
  val courseId: String = "",
  val sessionTitle: String = "",
  val courseTitle: String = "",
  val courseCode: String = "",
  val lecturerName: String = "",
  val venueText: String = "",
  val startsAt: String = "",
  val expiresAt: String = "",
  val active: Boolean = false,
  val status: String = "open"
)

data class AdminAttendanceRecord(
  val fullName: String,
  val admissionNumber: String,
  val timeSigned: String
)

data class AdminPollItem(
  val id: String,
  val question: String,
  val totalVotes: Int = 0,
  val active: Boolean = true,
  val expiresAt: String = "",
  val createdAt: String = ""
)

data class AdminPollResultOption(
  val id: String,
  val text: String,
  val votes: Int,
  val percentage: Double
)

data class AdminPollResults(
  val totalVotes: Int,
  val options: List<AdminPollResultOption>
)

data class AdminGroupItem(
  val id: String,
  val name: String,
  val description: String = "",
  val memberCount: Int = 0,
  val maxMembers: Int = 50
)

data class AdminGroupMessage(
  val id: String,
  val senderName: String,
  val messageText: String,
  val createdAt: String = ""
)

data class AdminGroupMember(
  val userId: String,
  val fullName: String,
  val admissionNumber: String = "-",
  val isOwner: Boolean = false
)

data class AdminQuizSummary(
  val id: String,
  val title: String,
  val unitCode: String = "",
  val status: String = "open",
  val isExam: Boolean = false,
  val answerFormat: String = "choices",
  val questionCount: Int = 0,
  val totalMarks: Double = 0.0,
  val attemptsCount: Int = 0,
  val timeLimitSeconds: Long? = null,
  val oneWayNavigation: Boolean = false,
  val noCopyPaste: Boolean = false,
  val opensAt: String = "",
  val closesAt: String = ""
)

data class AdminQuizResultRow(
  val studentId: String,
  val fullName: String,
  val admissionNumber: String,
  val score: Double?,
  val total: Double,
  val submittedAt: String = "",
  val markingStatus: String = "auto",
  val late: Boolean = false,
  val mcqMarks: Double? = null,
  val writtenMarks: Double? = null,
  val focusLossCount: Int = 0,
  val autoSubmittedReason: String = ""
)

