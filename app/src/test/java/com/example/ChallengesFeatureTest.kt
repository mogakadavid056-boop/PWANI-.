package com.example

import com.example.data.ChallengesService
import com.example.data.ClassItem
import com.example.data.DraftQuizQuestion
import com.example.data.LeaderboardPeriod
import com.example.data.PlayQuestion
import com.example.data.QuizStatus
import com.example.data.UserSession
import com.example.notifications.NotificationDeepLink
import com.example.ui.MainActivityNotificationRouter
import com.example.ui.components.ClassTab
import com.example.ui.screens.MoreSubView
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChallengesFeatureTest {

  @Test
  fun buildCreateQuizPayload_handlesOptionalFieldsAndQuestionClamping() {
    val draftQuestions = listOf(
      DraftQuizQuestion(
        question = "  What is Big-O of binary search? ",
        options = listOf(" O(n) ", " O(log n) ", " O(1) ", "  "),
        correctIndex = 1,
        explanation = " Divides sorted array in half each step "
      ),
      DraftQuizQuestion(
        question = "Edge clamped index",
        options = listOf("Yes", "No"),
        correctIndex = 99,
        explanation = ""
      )
    )

    val payload = ChallengesService.buildCreateQuizPayload(
      classId = "cls-1",
      title = " Week 4 Quiz ",
      description = " Algorithms ",
      opensAtIso = "2026-11-20T08:00:00Z",
      closesAtIso = "2026-11-21T08:00:00Z",
      timeLimitMinutes = 15,
      questions = draftQuestions,
      courseId = "crs-9"
    )

    assertEquals("cls-1", payload.getString("target_class_id"))
    assertEquals("Week 4 Quiz", payload.getString("quiz_title"))
    assertEquals("Algorithms", payload.getString("quiz_description"))
    assertEquals(15, payload.getInt("time_limit"))
    assertEquals("crs-9", payload.getString("target_course_id"))

    val arr = payload.getJSONArray("questions")
    assertEquals(2, arr.length())
    val q1 = arr.getJSONObject(0)
    assertEquals("What is Big-O of binary search?", q1.getString("question"))
    assertEquals(3, q1.getJSONArray("options").length())
    assertEquals(1, q1.getInt("correct_index"))
    assertEquals("Divides sorted array in half each step", q1.getString("explanation"))

    val q2 = arr.getJSONObject(1)
    assertEquals(1, q2.getInt("correct_index"))
    assertFalse(q2.has("explanation"))

    val payloadNoLimit = ChallengesService.buildCreateQuizPayload(
      classId = "cls-1",
      title = "Untimed Quiz",
      description = "",
      opensAtIso = "2026-11-20T08:00:00Z",
      closesAtIso = "2026-11-21T08:00:00Z",
      timeLimitMinutes = null,
      questions = draftQuestions.take(1),
      courseId = null
    )
    assertTrue(payloadNoLimit.isNull("time_limit"))
    assertTrue(payloadNoLimit.isNull("target_course_id"))
  }

  @Test
  fun buildSubmitQuizPayload_alignsOneBasedOrderWithSkippedMinusOne() {
    val questions = listOf(
      PlayQuestion(questionId = "q3", ord = 3, question = "Q3", options = listOf("A", "B")),
      PlayQuestion(questionId = "q1", ord = 1, question = "Q1", options = listOf("A", "B")),
      PlayQuestion(questionId = "q2", ord = 2, question = "Q2", options = listOf("A", "B"))
    )
    val payload = ChallengesService.buildSubmitQuizPayload(
      quizId = "quiz-10",
      questionsInOrder = questions,
      selectedAnswersByOrd = mapOf(1 to 0, 3 to 1)
    )
    assertEquals("quiz-10", payload.getString("target_quiz_id"))
    val answers = payload.getJSONArray("chosen_answers")
    assertEquals(3, answers.length())
    assertEquals(0, answers.getInt(0))
    assertEquals(-1, answers.getInt(1))
    assertEquals(1, answers.getInt(2))
  }

  @Test
  fun computeRemainingSeconds_clampsBetweenTimeLimitAndClosesAt() {
    assertNull(
      ChallengesService.computeRemainingSeconds(
        startedAtIso = "2026-11-20T10:00:00Z",
        closesAtIso = "2026-11-20T12:00:00Z",
        timeLimitSeconds = null,
        nowMillis = java.time.Instant.parse("2026-11-20T10:01:00Z").toEpochMilli()
      )
    )

    // 10 min limit -> 10:10:00, closes at 12:00:00, now 10:02:00 -> 480s
    assertEquals(
      480L,
      ChallengesService.computeRemainingSeconds(
        startedAtIso = "2026-11-20T10:00:00Z",
        closesAtIso = "2026-11-20T12:00:00Z",
        timeLimitSeconds = 10L * 60L,
        nowMillis = java.time.Instant.parse("2026-11-20T10:02:00Z").toEpochMilli()
      )
    )

    // 30 min limit -> 10:30:00, but quiz closes at 10:05:00, now 10:02:00 -> 180s
    assertEquals(
      180L,
      ChallengesService.computeRemainingSeconds(
        startedAtIso = "2026-11-20T10:00:00Z",
        closesAtIso = "2026-11-20T10:05:00Z",
        timeLimitSeconds = 30L * 60L,
        nowMillis = java.time.Instant.parse("2026-11-20T10:02:00Z").toEpochMilli()
      )
    )

    // Expired -> 0s
    assertEquals(
      0L,
      ChallengesService.computeRemainingSeconds(
        startedAtIso = "2026-11-20T10:00:00Z",
        closesAtIso = "2026-11-20T10:05:00Z",
        timeLimitSeconds = 30L * 60L,
        nowMillis = java.time.Instant.parse("2026-11-20T10:06:00Z").toEpochMilli()
      )
    )
  }

  @Test
  fun parsers_handleQuizzesPlaySubmitReviewResultsStatsAndLeaderboard() {
    val quizzesJson = """
      [
        {
          "quiz_id": "qz-1",
          "title": "DSA Sprint",
          "description": "Trees & Graphs",
          "unit_code": "csc 201",
          "opens_at": "2026-11-20T08:00:00Z",
          "closes_at": "2026-11-21T08:00:00Z",
          "time_limit_minutes": 15,
          "question_count": 5,
          "status": "open",
          "attempted": false,
          "my_score": null,
          "my_total": null,
          "attempts_count": 12
        },
        {
          "quiz_id": "qz-2",
          "title": "Operating Systems",
          "description": "null",
          "unit_code": "null",
          "opens_at": "2026-11-18T08:00:00Z",
          "closes_at": "2026-11-19T08:00:00Z",
          "time_limit_minutes": null,
          "question_count": 4,
          "status": "closed",
          "attempted": true,
          "my_score": 3,
          "my_total": 4,
          "attempts_count": 20
        }
      ]
    """.trimIndent()
    val quizzes = ChallengesService.parseQuizzes(quizzesJson)
    assertEquals(2, quizzes.size)
    assertEquals(QuizStatus.OPEN, quizzes[0].status)
    assertEquals("CSC 201", quizzes[0].unitCode)
    assertEquals(15, quizzes[0].timeLimitMinutes)
    assertNull(quizzes[0].myScore)
    assertEquals(QuizStatus.CLOSED, quizzes[1].status)
    assertTrue(quizzes[1].attempted)
    assertEquals(3.0, quizzes[1].myScore!!, 0.001)
    assertEquals(4.0, quizzes[1].myTotal!!, 0.001)
    assertEquals("", quizzes[1].description)

    val creators = ChallengesService.parseCreatedByMap("""[{"id":"qz-1","created_by":"user-99"}]""")
    assertEquals("user-99", creators["qz-1"])

    val playJson = """
      [
        {"question_id":"qq-2","ord":2,"question":"Second?","options":["X","Y"],"time_limit_minutes":10,"started_at":"2026-11-20T09:00:00Z"},
        {"question_id":"qq-1","ord":1,"question":"First?","options":["A","B"],"time_limit_minutes":10,"started_at":"2026-11-20T09:00:00Z"}
      ]
    """.trimIndent()
    val playQs = ChallengesService.parseStartQuizQuestions(playJson)
    assertEquals(2, playQs.size)
    // Server ordering is preserved so shuffled exams stay in server order
    assertEquals(2, playQs[0].ord)
    assertEquals("Second?", playQs[0].question)
    assertEquals(1, playQs[1].ord)

    val submitRes = ChallengesService.parseSubmitQuizResult("""[{"score":4,"total":5,"points_awarded":9}]""")
    assertEquals(4.0, submitRes.score, 0.001)
    assertEquals(5.0, submitRes.total, 0.001)
    assertEquals(9, submitRes.pointsAwarded)

    val review = ChallengesService.parseQuizReview(
      """[{"ord":1,"question":"Q1","options":["A","B"],"correct_index":1,"chosen_index":-1,"explanation":"B is right"}]"""
    )
    assertEquals(1, review.size)
    assertEquals(1, review[0].correctIndex)
    assertEquals(-1, review[0].chosenIndex)
    assertEquals("B is right", review[0].explanation)

    val results = ChallengesService.parseQuizResults(
      """[{"student_id":"s1","full_name":"Grace Hopper","admission_number":"null","score":5,"total":5,"submitted_at":"2026-11-20T09:10:00Z"}]"""
    )
    assertEquals(1, results.size)
    assertEquals("-", results[0].admissionNumber)
    assertEquals(5.0, results[0].score!!, 0.001)

    val stats = ChallengesService.parseActivityStats(
      """[{"total_points":145,"week_points":32,"streak_days":4,"best_streak":9,"week_rank":2,"ranked_students":38}]"""
    )
    assertEquals(145, stats.totalPoints)
    assertEquals(32, stats.weekPoints)
    assertEquals(4, stats.streakDays)
    assertEquals(9, stats.bestStreak)
    assertEquals(2, stats.weekRank)
    assertEquals(38, stats.rankedStudents)

    val leaderboard = ChallengesService.parseLeaderboard(
      """[{"rank_no":1,"display_name":"Ada Lovelace","points":50,"is_me":false},{"rank_no":2,"display_name":"null","points":32,"is_me":true}]"""
    )
    assertEquals(2, leaderboard.size)
    assertEquals("Ada Lovelace", leaderboard[0].displayName)
    assertEquals("Anonymous", leaderboard[1].displayName)
    assertTrue(leaderboard[1].isMe)
    assertEquals("week", LeaderboardPeriod.WEEK.apiValue)
  }

  @Test
  fun notificationRouter_routesQuizKindToMoreChallengesTab() {
    val cls = ClassItem(id = "cls-42", name = "BSc CS Y2", code = "CS2")
    val session = UserSession(
      userId = "u1",
      email = "a@b.com",
      fullName = "Ada Lovelace",
      admissionNumber = "ADM-101",
      role = "student",
      accessToken = "tok"
    )
    var openedTab: ClassTab? = null
    var openedSubView: MoreSubView? = null
    var openedClassId: String? = null

    val handled = MainActivityNotificationRouter.routeDeepLink(
      link = NotificationDeepLink(kind = "quiz", classId = "cls-42"),
      session = session,
      classes = listOf(cls),
      selectedClass = null,
      openClassDirectly = { openedClassId = it.id },
      onOpenTab = { tab, sub ->
        openedTab = tab
        openedSubView = sub
      },
      onOpenMyAttendance = {},
      onOpenAttendanceTracker = {},
      onOpenSignAttendance = {},
      onOpenManageMembers = {},
      onOpenHome = {},
      onLoadClasses = {}
    )

    assertTrue(handled)
    assertEquals("cls-42", openedClassId)
    assertEquals(ClassTab.More, openedTab)
    assertEquals(MoreSubView.Challenges, openedSubView)
  }
}
