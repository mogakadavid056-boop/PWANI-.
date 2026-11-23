package com.example.data

import com.example.util.DurationHelper
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

enum class QuizStatus(val apiValue: String, val label: String) {
  OPEN("open", "Open"),
  UPCOMING("upcoming", "Upcoming"),
  CLOSED("closed", "Closed");

  companion object {
    fun fromApi(raw: String?): QuizStatus = when (raw?.trim()?.lowercase()) {
      "open" -> OPEN
      "upcoming" -> UPCOMING
      else -> CLOSED
    }
  }
}

enum class QuizStatusFilter(val label: String) {
  ALL("All"),
  OPEN("Open"),
  UPCOMING("Upcoming"),
  CLOSED("Closed")
}

enum class LeaderboardPeriod(val apiValue: String, val label: String) {
  WEEK("week", "This Week"),
  MONTH("month", "This Month"),
  ALL("all", "All Time")
}

enum class QuestionType(val apiValue: String, val label: String) {
  MCQ("mcq", "Multiple Choice"),
  WRITTEN("written", "Written");

  companion object {
    fun fromApi(raw: String?): QuestionType = when (raw?.trim()?.lowercase()) {
      "written" -> WRITTEN
      else -> MCQ
    }
  }
}

enum class ExamAnswerFormat(val apiValue: String, val label: String) {
  CHOICES("choices", "Choices Only"),
  WRITTEN("written", "Written Only"),
  BOTH("both", "Choices & Written");

  companion object {
    fun fromApi(raw: String?): ExamAnswerFormat = when (raw?.trim()?.lowercase()) {
      "written" -> WRITTEN
      "both" -> BOTH
      else -> CHOICES
    }
  }
}

enum class CountdownMode(val apiValue: String, val label: String, val shortHint: String) {
  OVERALL("overall", "Overall Exam Timer", "Single countdown across whole paper"),
  PER_QUESTION("per_question", "Per-Question Timer", "Each question has its own countdown"),
  BOTH("both", "Overall + Per-Question", "Enforces both exam and question timers"),
  CLOSING_WINDOW("closing_window", "Schedule Close Window", "Counts down to assessment closing time");

  companion object {
    fun fromApi(raw: String?): CountdownMode = when (raw?.trim()?.lowercase()) {
      "per_question" -> PER_QUESTION
      "both" -> BOTH
      "closing_window" -> CLOSING_WINDOW
      else -> OVERALL
    }
  }
}

enum class CountdownStyle(val apiValue: String, val label: String) {
  DIGITAL("digital", "Digital Clock"),
  BAR("bar", "Progress + Clock"),
  MINIMAL("minimal", "Minimal Pill");

  companion object {
    fun fromApi(raw: String?): CountdownStyle = when (raw?.trim()?.lowercase()) {
      "bar" -> BAR
      "minimal" -> MINIMAL
      else -> DIGITAL
    }
  }
}

enum class ExamExitAction(val apiValue: String, val label: String) {
  OFF("off", "Off"),
  WARN("warn", "Warn & Auto-Submit at Max"),
  AUTO_SUBMIT("auto_submit", "Submit Immediately on Exit");

  companion object {
    fun fromApi(raw: String?): ExamExitAction = when (raw?.trim()?.lowercase()) {
      "off" -> OFF
      "auto_submit" -> AUTO_SUBMIT
      else -> WARN
    }
  }
}

data class ExamSecurityConfig(
  val timeLimitSeconds: Long? = null,
  val perQuestionSeconds: Long? = null,
  val countdownMode: CountdownMode = CountdownMode.OVERALL,
  val countdownStyle: CountdownStyle = CountdownStyle.DIGITAL,
  val oneWayNavigation: Boolean = false,
  val lockOnAnswer: Boolean = false,
  val noCopyPaste: Boolean = false,
  val onExitAction: ExamExitAction = ExamExitAction.WARN,
  val maxExitViolations: Int = 2,
  val strictDeadline: Boolean = true
)

enum class MarkingStatus(val apiValue: String, val label: String) {
  AUTO("auto", "Auto-graded"),
  PENDING("pending", "Waiting"),
  MARKING("marking", "In Progress"),
  RELEASED("released", "Returned");

  companion object {
    fun fromApi(raw: String?): MarkingStatus = when (raw?.trim()?.lowercase()) {
      "pending" -> PENDING
      "marking" -> MARKING
      "released" -> RELEASED
      else -> AUTO
    }

    fun fromApiOrNull(raw: String?): MarkingStatus? {
      val clean = raw?.trim()?.lowercase().orEmpty()
      if (clean.isBlank() || clean == "null") return null
      return fromApi(clean)
    }
  }
}

fun formatMarks(value: Double): String {
  val roundedInt = value.roundToLong()
  if (abs(value - roundedInt.toDouble()) < 0.005) {
    return roundedInt.toString()
  }
  val oneDec = ((value * 10.0).roundToLong()) / 10.0
  return if (abs(value - oneDec) < 0.005) {
    String.format(Locale.US, "%.1f", oneDec)
  } else {
    String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
  }
}

data class ChallengeItem(
  val quizId: String,
  val title: String,
  val description: String = "",
  val unitCode: String = "",
  val opensAt: String = "",
  val closesAt: String = "",
  val timeLimitMinutes: Int? = null,
  val timeLimitSeconds: Long? = timeLimitMinutes?.toLong()?.times(60L),
  val perQuestionSeconds: Long? = null,
  val countdownMode: CountdownMode = CountdownMode.OVERALL,
  val countdownStyle: CountdownStyle = CountdownStyle.DIGITAL,
  val oneWayNavigation: Boolean = false,
  val lockOnAnswer: Boolean = false,
  val noCopyPaste: Boolean = false,
  val onExitAction: ExamExitAction = ExamExitAction.WARN,
  val maxExitViolations: Int = 2,
  val strictDeadline: Boolean = true,
  val questionCount: Int = 0,
  val status: QuizStatus = QuizStatus.OPEN,
  val attempted: Boolean = false,
  val myScore: Double? = null,
  val myTotal: Double? = null,
  val attemptsCount: Int = 0,
  val createdBy: String? = null,
  val isExam: Boolean = false,
  val answerFormat: ExamAnswerFormat = ExamAnswerFormat.CHOICES,
  val totalMarks: Double = 0.0,
  val myMarkingStatus: MarkingStatus? = null,
  val pdfDownloadedAt: String? = null
) {
  val effectiveTimeLimitSeconds: Long?
    get() = timeLimitSeconds ?: timeLimitMinutes?.toLong()?.times(60L)

  val timerSummaryText: String
    get() = when {
      effectiveTimeLimitSeconds != null -> "${DurationHelper.formatSummary(effectiveTimeLimitSeconds)} limit"
      perQuestionSeconds != null -> "${DurationHelper.formatSummary(perQuestionSeconds)}/Q"
      else -> ""
    }
}

data class PlayQuestion(
  val questionId: String,
  val ord: Int,
  val questionType: QuestionType = QuestionType.MCQ,
  val question: String,
  val options: List<String>,
  val marks: Double = 1.0,
  val timeLimitMinutes: Int? = null,
  val timeLimitSeconds: Long? = timeLimitMinutes?.toLong()?.times(60L),
  val perQuestionSeconds: Long? = null,
  val questionTimeLimitSeconds: Long? = null,
  val countdownMode: CountdownMode = CountdownMode.OVERALL,
  val countdownStyle: CountdownStyle = CountdownStyle.DIGITAL,
  val oneWayNavigation: Boolean = false,
  val lockOnAnswer: Boolean = false,
  val noCopyPaste: Boolean = false,
  val onExitAction: ExamExitAction = ExamExitAction.WARN,
  val maxExitViolations: Int = 2,
  val strictDeadline: Boolean = true,
  val startedAt: String? = null,
  val serverNow: String? = null,
  val lockedQuestionOrds: List<Int> = emptyList(),
  val initialFocusLossCount: Int = 0
)

data class SecurityEventResult(
  val focusLossCount: Int,
  val maxExitViolations: Int,
  val shouldAutoSubmit: Boolean
)

data class QuizSubmissionResult(
  val score: Double,
  val total: Double,
  val pointsAwarded: Int,
  val needsMarking: Boolean = false
)

data class QuizReviewItem(
  val ord: Int,
  val questionType: QuestionType = QuestionType.MCQ,
  val question: String,
  val options: List<String>,
  val correctIndex: Int,
  val chosenIndex: Int,
  val writtenAnswer: String = "",
  val marks: Double = 1.0,
  val awardedMarks: Double? = null,
  val markerComment: String = "",
  val explanation: String = ""
)

data class QuizResultRow(
  val studentId: String,
  val fullName: String,
  val admissionNumber: String,
  val score: Double?,
  val total: Double,
  val submittedAt: String = "",
  val markingStatus: MarkingStatus = MarkingStatus.AUTO,
  val late: Boolean = false,
  val mcqMarks: Double? = null,
  val writtenMarks: Double? = null,
  val focusLossCount: Int = 0,
  val autoSubmittedReason: String = ""
)

data class ActivityStats(
  val totalPoints: Int = 0,
  val weekPoints: Int = 0,
  val streakDays: Int = 0,
  val bestStreak: Int = 0,
  val weekRank: Int? = null,
  val rankedStudents: Int = 0
)

data class LeaderboardEntry(
  val rankNo: Int,
  val displayName: String,
  val points: Int,
  val isMe: Boolean
)

data class DraftQuizQuestion(
  val questionType: QuestionType = QuestionType.MCQ,
  val question: String = "",
  val options: List<String> = listOf("", "", "", ""),
  val correctIndex: Int = 0,
  val marks: Double = 1.0,
  val explanation: String = "",
  val modelAnswer: String = "",
  val timeLimitSeconds: Long? = null
)

data class ExamSubmissionRow(
  val studentId: String,
  val fullName: String,
  val admissionNumber: String,
  val submittedAt: String = "",
  val late: Boolean = false,
  val mcqMarks: Double? = null,
  val writtenMarks: Double? = null,
  val finalScore: Double? = null,
  val maxTotal: Double = 0.0,
  val markingStatus: MarkingStatus = MarkingStatus.PENDING,
  val focusLossCount: Int = 0,
  val autoSubmittedReason: String = "",
  val writtenReadyCount: Int = 0,
  val writtenTotalCount: Int = 0
) {
  val isReadyForBatchReturn: Boolean
    get() = (markingStatus == MarkingStatus.PENDING || markingStatus == MarkingStatus.MARKING) &&
      (writtenTotalCount == 0 || writtenReadyCount >= writtenTotalCount)
}

data class ExamPaperQuizInfo(
  val id: String,
  val title: String,
  val classId: String,
  val className: String = "",
  val unitCode: String = "",
  val unitName: String = "",
  val closesAt: String = "",
  val answerFormat: ExamAnswerFormat = ExamAnswerFormat.BOTH,
  val totalMarks: Double = 0.0
)

data class ExamPaperStudentInfo(
  val id: String,
  val fullName: String,
  val admissionNumber: String = "-"
)

data class ExamPaperAttemptInfo(
  val status: MarkingStatus = MarkingStatus.PENDING,
  val submittedAt: String = "",
  val late: Boolean = false,
  val mcqMarks: Double? = null,
  val writtenMarks: Double? = null,
  val draftVersion: Int = 0,
  val overallComment: String = "",
  val markedPdfPath: String = ""
)

data class ExamPaperQuestion(
  val ord: Int,
  val questionType: QuestionType,
  val question: String,
  val options: List<String> = emptyList(),
  val marks: Double = 1.0,
  val correctIndex: Int? = null,
  val chosenIndex: Int = -1,
  val writtenAnswer: String = "",
  val modelAnswer: String = "",
  val awardedMarks: Double? = null,
  val comment: String = ""
)

data class ExamPaperDetail(
  val quiz: ExamPaperQuizInfo,
  val student: ExamPaperStudentInfo,
  val attempt: ExamPaperAttemptInfo,
  val questions: List<ExamPaperQuestion>,
  val draft: MarkingDraftData = MarkingDraftData()
)

data class NormalizedPoint(
  val x: Float,
  val y: Float
)

data class InkStroke(
  val points: List<NormalizedPoint>,
  val colorHex: Long = 0xFFD4AF37L,
  val strokeWidth: Float = 3.5f
)

enum class StampKind(val apiValue: String, val symbol: String) {
  TICK("tick", "\u2713"),
  CROSS("cross", "\u2717");

  companion object {
    fun fromApi(raw: String?): StampKind = when (raw?.trim()?.lowercase()) {
      "cross", "x" -> CROSS
      else -> TICK
    }
  }
}

data class MarkingStamp(
  val kind: StampKind,
  val x: Float,
  val y: Float,
  val label: String = ""
)

data class QuestionMarkingDraft(
  val strokes: List<InkStroke> = emptyList(),
  val stamps: List<MarkingStamp> = emptyList()
)

data class MarkingDraftData(
  val questions: Map<Int, QuestionMarkingDraft> = emptyMap()
)

data class QuestionMarkInput(
  val marks: Double? = null,
  val comment: String = ""
)

data class PaperToMarkItem(
  val quizId: String,
  val classId: String,
  val className: String,
  val title: String,
  val closesAt: String = "",
  val waiting: Int = 0,
  val inProgress: Int = 0,
  val returned: Int = 0
)

data class MyMarkedPaperInfo(
  val markingStatus: MarkingStatus = MarkingStatus.PENDING,
  val score: Double? = null,
  val total: Double = 0.0,
  val markedPdfPath: String? = null,
  val releasedAt: String? = null,
  val overallComment: String = "",
  val pdfDownloadedAt: String? = null
)
