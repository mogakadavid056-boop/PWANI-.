package com.example

import com.example.data.ChallengeItem
import com.example.data.ChallengesService
import com.example.data.CountdownMode
import com.example.data.CountdownStyle
import com.example.data.DraftQuizQuestion
import com.example.data.ExamAnswerFormat
import com.example.data.ExamCsvCodec
import com.example.data.ExamExitAction
import com.example.data.ExamSecurityConfig
import com.example.data.ExamSubmissionRow
import com.example.data.MarkingStatus
import com.example.data.PlayQuestion
import com.example.data.QuestionType
import com.example.data.QuizResultRow
import com.example.util.DurationHelper
import com.example.util.DurationUnit
import com.example.util.RankedExamResultsPdfGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [ShadowTestPdfDocument::class])
class ExamSecurityAndTimingTest {

  @Test
  fun durationHelper_supportsAllUnitsSecondsThroughMonths() {
    assertEquals(45L, DurationHelper.parseDurationExpressionToSeconds("45s"))
    assertEquals(1800L, DurationHelper.parseDurationExpressionToSeconds("30 min"))
    assertEquals(7200L, DurationHelper.parseDurationExpressionToSeconds("2hr"))
    assertEquals(259_200L, DurationHelper.parseDurationExpressionToSeconds("3 days"))
    assertEquals(1_209_600L, DurationHelper.parseDurationExpressionToSeconds("2wks"))
    assertEquals(5_184_000L, DurationHelper.parseDurationExpressionToSeconds("2 mos"))
    assertNull(DurationHelper.parseDurationExpressionToSeconds("0s"))
    assertNull(DurationHelper.parseDurationExpressionToSeconds("abc"))

    assertEquals(2L to DurationUnit.MONTHS, DurationHelper.decomposeBestUnit(5_184_000L))
    assertEquals("3 wks", DurationHelper.formatSummary(DurationHelper.toSeconds(3L, DurationUnit.WEEKS)))
    assertEquals("01:05:09", DurationHelper.formatCountdown(3909L))
    assertEquals("2d 01:00:00", DurationHelper.formatCountdown(2 * 86_400L + 3600L))
    assertEquals("1mo 1d 00:00:05", DurationHelper.formatCountdown(2_592_000L + 86_400L + 5L))
  }

  @Test
  fun examCsvCodec_parsesPerQuestionTimeExpressions() {
    val csv = """
      type,question,option_a,option_b,correct,marks,time
      mcq,Quick check?,Yes,No,A,1,45s
      written,Explain TCP handshake.,,,,5,3 min
    """.trimIndent()

    val res = ExamCsvCodec.parse(csv)
    assertNull(res.error)
    assertEquals(2, res.questions.size)
    assertEquals(45L, res.questions[0].timeLimitSeconds)
    assertEquals(180L, res.questions[1].timeLimitSeconds)
  }

  @Test
  fun challengesService_serializesSecurityAndTimingConfigAndLongTimers() {
    val config = ExamSecurityConfig(
      timeLimitSeconds = 2 * 86_400L, // exceeds 24h -> clamped to 86_400L (1440 min)
      perQuestionSeconds = 45L,
      countdownMode = CountdownMode.BOTH,
      countdownStyle = CountdownStyle.BAR,
      oneWayNavigation = true,
      lockOnAnswer = true,
      noCopyPaste = true,
      onExitAction = ExamExitAction.AUTO_SUBMIT,
      maxExitViolations = 1,
      strictDeadline = true
    )
    val qs = listOf(
      DraftQuizQuestion(
        questionType = QuestionType.MCQ,
        question = "Q1",
        options = listOf("A", "B"),
        correctIndex = 0,
        marks = 2.0,
        timeLimitSeconds = 90L
      )
    )

    val payload = ChallengesService.buildCreateQuizPayload(
      classId = "cls-1",
      title = "CAT 1",
      description = "Strict exam",
      opensAtIso = "2026-11-22T08:00:00Z",
      closesAtIso = "2026-11-30T08:00:00Z",
      timeLimitMinutes = 2880,
      questions = qs,
      courseId = null,
      asExam = true,
      shuffleQuestions = true,
      answerFormat = ExamAnswerFormat.CHOICES,
      securityConfig = config
    )

    assertEquals(86_400L, payload.getLong("p_time_limit_seconds"))
    assertEquals(1440, payload.getInt("time_limit"))
    assertEquals(45L, payload.getLong("p_per_question_seconds"))
    assertEquals("both", payload.getString("p_countdown_mode"))
    assertEquals("bar", payload.getString("p_countdown_style"))
    assertTrue(payload.getBoolean("p_one_way_navigation"))
    assertTrue(payload.getBoolean("p_lock_on_answer"))
    assertTrue(payload.getBoolean("p_no_copy_paste"))
    assertEquals("auto_submit", payload.getString("p_on_exit_action"))
    assertEquals(1, payload.getInt("p_max_exit_violations"))
    assertTrue(payload.getBoolean("p_strict_deadline"))
    assertEquals(90L, payload.getJSONArray("questions").getJSONObject(0).getLong("time_limit_seconds"))

    val rem = ChallengesService.computeRemainingSeconds(
      startedAtIso = "2026-11-22T08:00:00Z",
      closesAtIso = "2026-11-30T08:00:00Z",
      timeLimitSeconds = 86_400L,
      nowMillis = Instant.parse("2026-11-22T20:00:00Z").toEpochMilli()
    )
    assertEquals(43_200L, rem)

    val submitPayload = ChallengesService.buildSubmitQuizPayload(
      quizId = "q-1",
      questionsInOrder = listOf(PlayQuestion("id1", 1, QuestionType.MCQ, "Q1", listOf("A", "B"))),
      selectedAnswersByOrd = mapOf(1 to 0)
    )
    assertEquals("q-1", submitPayload.getString("target_quiz_id"))
    assertFalse(submitPayload.has("p_auto_submitted_reason"))
  }

  @Test
  fun examV2SqlContracts_progressViolationFlagsWarnSubmitAndSparseStartQuiz() {
    assertEquals("warn_submit", ExamExitAction.WARN.apiValue)
    assertEquals(ExamExitAction.WARN, ExamExitAction.fromApi("warn_submit"))
    assertEquals(ExamExitAction.WARN, ExamExitAction.fromApi("warn"))

    val playQs = listOf(
      PlayQuestion("q1", 1, QuestionType.MCQ, "Q1", listOf("A", "B")),
      PlayQuestion("q2", 2, QuestionType.WRITTEN, "Q2", emptyList())
    )
    val savePayload = ChallengesService.buildSaveProgressPayload(
      quizId = "qz-9",
      questionsInOrder = playQs,
      selectedAnswersByOrd = mapOf(1 to 1),
      writtenAnswersByOrd = mapOf(2 to "w".repeat(8200))
    )
    assertEquals("qz-9", savePayload.getString("target_quiz_id"))
    assertEquals(1, savePayload.getJSONArray("chosen_answers").getInt(0))
    assertEquals(-1, savePayload.getJSONArray("chosen_answers").getInt(1))
    assertEquals(8000, savePayload.getJSONObject("written_answers").getString("2").length)

    val progress = ChallengesService.parseSavedExamProgress(
      """[{"chosen_answers":[1,-1],"written_answers":{"2":"Answer text"},"saved_at":"2026-11-24T09:00:00Z","exit_violations":2}]"""
    )
    assertEquals(mapOf(1 to 1), progress?.chosenAnswersByOrd)
    assertEquals(mapOf(2 to "Answer text"), progress?.writtenAnswersByOrd)
    assertEquals(2, progress?.exitViolations)

    val violation = ChallengesService.parseViolationResult(
      """[{"violations":3,"max_violations":3,"auto_submitted":true}]"""
    )
    assertEquals(3, violation.focusLossCount)
    assertEquals(3, violation.maxExitViolations)
    assertTrue(violation.shouldAutoSubmit)

    val flags = ChallengesService.parseExamFlags(
      """[{"student_id":"s1","full_name":"Ada","admission_number":"CS/01","exit_violations":3,"auto_submitted":true,"auto_submit_reason":"exit_limit","late":false,"submitted_at":"2026-11-24T09:10:00Z"}]"""
    )
    assertEquals(1, flags.size)
    assertEquals("exit_limit", flags[0].autoSubmitReason)
    assertTrue(flags[0].autoSubmitted)
    assertEquals(3, flags[0].exitViolations)

    val sparseStart = ChallengesService.parseStartQuizQuestions(
      """[{"question_id":"qq-1","ord":1,"question_type":"mcq","question":"Q1","options":["A","B"],"marks":2,"time_limit_minutes":30,"started_at":"2026-11-24T08:00:00Z"}]"""
    )
    assertEquals(1, sparseStart.size)
    assertNull(sparseStart[0].oneWayNavigation)
    assertNull(sparseStart[0].lockOnAnswer)
    assertNull(sparseStart[0].noCopyPaste)
    assertNull(sparseStart[0].onExitAction)
  }

  @Test
  fun batchReturnReadiness_onlyMatchesFullyMarkedOrMcqUnreturnedPapers() {
    val readyWritten = ExamSubmissionRow("s1", "Alice", "ADM/1", markingStatus = MarkingStatus.MARKING, writtenReadyCount = 2, writtenTotalCount = 2)
    val incompleteWritten = ExamSubmissionRow("s2", "Bob", "ADM/2", markingStatus = MarkingStatus.MARKING, writtenReadyCount = 1, writtenTotalCount = 2)
    val alreadyReleased = ExamSubmissionRow("s3", "Cara", "ADM/3", markingStatus = MarkingStatus.RELEASED, writtenReadyCount = 2, writtenTotalCount = 2)
    val mcqOnlyPending = ExamSubmissionRow("s4", "Dan", "ADM/4", markingStatus = MarkingStatus.PENDING, writtenReadyCount = 0, writtenTotalCount = 0)

    assertTrue(readyWritten.isReadyForBatchReturn)
    assertFalse(incompleteWritten.isReadyForBatchReturn)
    assertFalse(alreadyReleased.isReadyForBatchReturn)
    assertTrue(mcqOnlyPending.isReadyForBatchReturn)
  }

  @Test
  fun rankedResultsPdfGenerator_ranksTiesCorrectlyAndGeneratesValidPdfBytes() {
    val quiz = ChallengeItem(quizId = "q-1", title = "Midterm CAT", unitCode = "CSC 301", isExam = true, totalMarks = 20.0)
    val rows = listOf(
      QuizResultRow("s1", "Zara", "CS/010", score = 18.0, total = 20.0, markingStatus = MarkingStatus.RELEASED, mcqMarks = 8.0, writtenMarks = 10.0),
      QuizResultRow("s2", "Amos", "CS/002", score = 18.0, total = 20.0, markingStatus = MarkingStatus.RELEASED, mcqMarks = 10.0, writtenMarks = 8.0, focusLossCount = 1),
      QuizResultRow("s3", "Bella", "CS/003", score = 15.0, total = 20.0, markingStatus = MarkingStatus.RELEASED, late = true),
      QuizResultRow("s4", "Pending Student", "CS/004", score = null, total = 20.0, markingStatus = MarkingStatus.PENDING)
    )

    val ranked = RankedExamResultsPdfGenerator.computeRankedEntries(rows, 20.0)
    assertEquals(1, ranked[0].rank)
    assertEquals("CS/002", ranked[0].row.admissionNumber) // tie broken naturally by admission no
    assertEquals(1, ranked[1].rank)
    assertEquals("CS/010", ranked[1].row.admissionNumber)
    assertEquals(3, ranked[2].rank) // competition ranking skips rank 2
    assertEquals(4, ranked[3].rank)

    val summary = RankedExamResultsPdfGenerator.computeSummary(ranked, 20.0)
    assertEquals(4, summary.totalSubmissions)
    assertEquals(3, summary.gradedCount)
    assertEquals(1, summary.pendingCount)
    assertEquals(2, summary.flaggedCount) // Amos (exit) + Bella (late)

    val pdfBytes = RankedExamResultsPdfGenerator.generatePdfBytes(quiz, "BSc CS Y3", rows, "Dr. Kim")
    assertTrue(pdfBytes.size > 100)
    assertTrue(String(pdfBytes.copyOfRange(0, 5), Charsets.US_ASCII) == "%PDF-")
  }
}
