package com.example

import com.example.data.ChallengesService
import com.example.data.ClassItem
import com.example.data.DraftQuizQuestion
import com.example.data.ExamAnswerFormat
import com.example.data.ExamMarkingService
import com.example.data.ExamPaperAttemptInfo
import com.example.data.ExamPaperDetail
import com.example.data.ExamPaperQuestion
import com.example.data.ExamPaperQuizInfo
import com.example.data.ExamPaperStudentInfo
import com.example.data.ExamSubmissionRow
import com.example.data.InkStroke
import com.example.data.MarkingDraftData
import com.example.data.MarkingStamp
import com.example.data.MarkingStatus
import com.example.data.NormalizedPoint
import com.example.data.PlayQuestion
import com.example.data.QuestionMarkInput
import com.example.data.QuestionMarkingDraft
import com.example.data.QuestionType
import com.example.data.StampKind
import com.example.data.UserSession
import com.example.data.formatMarks
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import com.example.notifications.NotificationDeepLink
import com.example.ui.MainActivityNotificationRouter
import com.example.ui.components.ClassTab
import com.example.ui.screens.MoreSubView
import com.example.util.MarkedPaperPdfGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.util.ReflectionHelpers
import java.io.OutputStream

@Implements(PdfDocument::class)
class ShadowTestPdfDocument {
  private var pagesStarted = 0

  @Implementation
  fun __constructor__() {}

  @Implementation
  fun startPage(pageInfo: PdfDocument.PageInfo): PdfDocument.Page {
    pagesStarted++
    val ctor = PdfDocument.Page::class.java.declaredConstructors.first()
    ctor.isAccessible = true
    val params = ctor.parameterTypes.map { t ->
      when (t) {
        PdfDocument.PageInfo::class.java -> pageInfo
        Canvas::class.java -> Canvas()
        else -> null
      }
    }.toTypedArray()
    return ctor.newInstance(*params) as PdfDocument.Page
  }

  @Implementation
  fun finishPage(page: PdfDocument.Page) {}

  @Implementation
  fun writeTo(out: OutputStream) {
    val payload = "%PDF-1.4\n% ClassRep Test PDF ($pagesStarted page(s))\n" + "0".repeat(256)
    out.write(payload.toByteArray(Charsets.US_ASCII))
  }

  @Implementation
  fun close() {}
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [ShadowTestPdfDocument::class])
class WrittenExamsAndMarkingTest {

  @Test
  fun examCsvCodec_parsesTemplateV2AndInfersAnswerFormat() {
    val parsed = com.example.data.ExamCsvCodec.parse(com.example.data.ExamCsvCodec.SAMPLE_CSV_TEMPLATE)
    assertNull(parsed.error)
    assertEquals(3, parsed.questions.size)

    val q1 = parsed.questions[0]
    assertEquals(QuestionType.MCQ, q1.questionType)
    assertEquals("Which data structure removes the oldest item first (FIFO)?", q1.question)
    assertEquals(listOf("Stack", "Queue", "Tree", "Graph"), q1.options)
    assertEquals(1, q1.correctIndex)
    assertEquals(1.0, q1.marks, 0.001)
    assertEquals("A queue serves items in arrival order.", q1.explanation)

    val q3 = parsed.questions[2]
    assertEquals(QuestionType.WRITTEN, q3.questionType)
    assertEquals(10.0, q3.marks, 0.001)
    assertTrue(q3.modelAnswer.contains("Stack = LIFO"))

    assertEquals(ExamAnswerFormat.BOTH, com.example.data.ExamCsvCodec.inferAnswerFormat(parsed.questions))
  }

  @Test
  fun examCsvCodec_handlesQuotedCommasAndCompactedEmptyOptions() {
    val csv = """
      type,question,option_a,option_b,option_c,option_d,option_e,correct,marks,explanation,model_answer
      mcq,"Pick ""one"", please",,Alpha,,Beta,Gamma,E,2.5,"Gamma is option E.",
      written,"Define acid, base, and salt.",,,,,,,7.5,,Proton donor/acceptor
    """.trimIndent()

    val res = com.example.data.ExamCsvCodec.parse(csv)
    assertNull(res.error)
    assertEquals(2, res.questions.size)

    val mcq = res.questions[0]
    assertEquals("Pick \"one\", please", mcq.question)
    assertEquals(listOf("Alpha", "Beta", "Gamma"), mcq.options)
    // E was slot 4 -> compacted non-blank index 2
    assertEquals(2, mcq.correctIndex)
    assertEquals(2.5, mcq.marks, 0.001)

    val written = res.questions[1]
    assertEquals(QuestionType.WRITTEN, written.questionType)
    assertEquals("Define acid, base, and salt.", written.question)
    assertEquals(7.5, written.marks, 0.001)
    assertEquals("Proton donor/acceptor", written.modelAnswer)
  }

  @Test
  fun examCsvCodec_returnsClearRowErrorsOnInvalidInput() {
    val badRow = """
      type,question,option_a,option_b,option_c,option_d,option_e,correct,marks
      mcq,Bad MCQ,OnlyOne,,,,,B,1
    """.trimIndent()
    val res1 = com.example.data.ExamCsvCodec.parse(badRow)
    assertNotNull(res1.error)
    assertTrue(res1.error!!.contains("Row 2"))

    val badMarks = """
      type,question,marks
      written,Explain ACID,200
    """.trimIndent()
    val res2 = com.example.data.ExamCsvCodec.parse(badMarks)
    assertNotNull(res2.error)
    assertTrue(res2.error!!.contains("Marks"))
  }

  @Test
  fun examCsvCodec_supportsSemicolonTabPipeCombinedAndAliases() {
    val semicolonBom = "\uFEFFsep=;\nPrompt;A;B;C;D;Answer;Points\n“Capital of Kenya?”;Mombasa;Nairobi;Kisumu;Nakuru;Nairobi;2 pts"
    val resSemi = com.example.data.ExamCsvCodec.parse(semicolonBom)
    assertNull(resSemi.error)
    assertEquals(1, resSemi.questions.size)
    assertEquals(QuestionType.MCQ, resSemi.questions[0].questionType)
    assertEquals(1, resSemi.questions[0].correctIndex)
    assertEquals(2.0, resSemi.questions[0].marks, 0.001)

    val tabPasted = "prompt\toptions\tanswer\tpoints\trubric\n" +
      "Which protocol is stateless?\tTCP | *HTTP | FTP | SSH\t\t2\t\n" +
      "Define normalization.\t\tOrganizing tables to reduce redundancy\t8 marks\t1NF, 2NF, 3NF"
    val resTab = com.example.data.ExamCsvCodec.parse(tabPasted)
    assertNull(resTab.error)
    assertEquals(2, resTab.questions.size)
    assertEquals(QuestionType.MCQ, resTab.questions[0].questionType)
    assertEquals(listOf("TCP", "HTTP", "FTP", "SSH"), resTab.questions[0].options)
    assertEquals(1, resTab.questions[0].correctIndex)
    assertEquals(QuestionType.WRITTEN, resTab.questions[1].questionType)
    assertEquals(8.0, resTab.questions[1].marks, 0.001)
    assertEquals("1NF, 2NF, 3NF", resTab.questions[1].modelAnswer)
    assertEquals(ExamAnswerFormat.BOTH, com.example.data.ExamCsvCodec.inferAnswerFormat(resTab.questions))
  }

  @Test
  fun examCsvCodec_handlesTrueFalseHeaderlessAndDefaultCorrectWarning() {
    val headerless = """
      What is 2 + 2?,3,4,5,6,b
      Earth orbits the Sun,True
      1. Define deadlock.,10,Mutual exclusion and circular wait
      Pick one without key,Red,Blue,Green
    """.trimIndent()
    val res = com.example.data.ExamCsvCodec.parse(headerless)
    assertNull(res.error)
    assertEquals(4, res.questions.size)
    assertEquals(1, res.questions[0].correctIndex)
    assertEquals(listOf("True", "False"), res.questions[1].options)
    assertEquals(0, res.questions[1].correctIndex)
    assertEquals(QuestionType.WRITTEN, res.questions[2].questionType)
    assertEquals("Define deadlock.", res.questions[2].question)
    assertEquals(10.0, res.questions[2].marks, 0.001)
    assertEquals("Mutual exclusion and circular wait", res.questions[2].modelAnswer)
    assertEquals(0, res.questions[3].correctIndex)
    assertTrue(res.warnings.isNotEmpty())

    assertEquals("CSC201 Midterm Exam", com.example.data.ExamCsvCodec.suggestTitleFromFilename("csc201_midterm-exam.csv"))
  }

  @Test
  fun challengesService_buildsWrittenExamCreateAndSubmitPayloads() {
    val qs = listOf(
      DraftQuizQuestion(
        questionType = QuestionType.MCQ,
        question = "Q1",
        options = listOf("A", "B"),
        correctIndex = 0,
        marks = 2.0
      ),
      DraftQuizQuestion(
        questionType = QuestionType.WRITTEN,
        question = "Discuss CAP theorem.",
        marks = 12.5,
        modelAnswer = "Consistency, Availability, Partition tolerance"
      )
    )

    val createPayload = ChallengesService.buildCreateQuizPayload(
      classId = "cls-9",
      title = "Final Exam",
      description = "Closed book",
      opensAtIso = "2026-11-21T08:00:00Z",
      closesAtIso = "2026-11-21T11:00:00Z",
      timeLimitMinutes = 120,
      questions = qs,
      courseId = "crs-2",
      asExam = true,
      shuffleQuestions = true,
      answerFormat = ExamAnswerFormat.BOTH
    )
    assertTrue(createPayload.getBoolean("as_exam"))
    assertTrue(createPayload.getBoolean("shuffle"))
    assertEquals("both", createPayload.getString("answer_format"))
    val qArr = createPayload.getJSONArray("questions")
    assertEquals("written", qArr.getJSONObject(1).getString("type"))
    assertEquals(12.5, qArr.getJSONObject(1).getDouble("marks"), 0.001)
    assertEquals("Consistency, Availability, Partition tolerance", qArr.getJSONObject(1).getString("model_answer"))

    val playQuestions = listOf(
      PlayQuestion("q1", ord = 1, questionType = QuestionType.MCQ, question = "Q1", options = listOf("A", "B"), marks = 2.0),
      PlayQuestion("q2", ord = 2, questionType = QuestionType.WRITTEN, question = "Discuss CAP", options = emptyList(), marks = 12.5)
    )
    val oversizedEssay = "x".repeat(8200)
    val submitPayload = ChallengesService.buildSubmitQuizPayload(
      quizId = "quiz-88",
      questionsInOrder = playQuestions,
      selectedAnswersByOrd = mapOf(1 to 1),
      writtenAnswersByOrd = mapOf(2 to oversizedEssay)
    )
    assertEquals("quiz-88", submitPayload.getString("target_quiz_id"))
    assertEquals(1, submitPayload.getJSONArray("chosen_answers").getInt(0))
    assertEquals(-1, submitPayload.getJSONArray("chosen_answers").getInt(1))
    val writtenObj = submitPayload.getJSONObject("written_answers")
    assertEquals(8000, writtenObj.getString("2").length)
  }

  @Test
  fun examMarkingService_roundTripsDraftInkStampsAndPaperDetail() {
    val draft = MarkingDraftData(
      questions = mapOf(
        2 to QuestionMarkingDraft(
          strokes = listOf(
            InkStroke(
              points = listOf(NormalizedPoint(0.1f, 0.2f), NormalizedPoint(0.8f, 0.4f)),
              colorHex = 0xFFFF6B6BL,
              strokeWidth = 4.0f
            )
          ),
          stamps = listOf(MarkingStamp(kind = StampKind.TICK, x = 0.75f, y = 0.25f, label = "+5"))
        )
      )
    )

    val serialized = ExamMarkingService.serializeMarkingDraft(draft)
    val parsedDraft = ExamMarkingService.parseMarkingDraft(serialized)
    val q2 = parsedDraft.questions[2]
    assertNotNull(q2)
    assertEquals(1, q2!!.strokes.size)
    assertEquals(2, q2.strokes[0].points.size)
    assertEquals(0.8f, q2.strokes[0].points[1].x, 0.001f)
    assertEquals(1, q2.stamps.size)
    assertEquals(StampKind.TICK, q2.stamps[0].kind)
    assertEquals("+5", q2.stamps[0].label)

    val marksJson = ExamMarkingService.serializeQuestionMarks(
      mapOf(2 to QuestionMarkInput(marks = 8.5, comment = "Well argued"))
    )
    assertEquals(8.5, marksJson.getJSONObject("2").getDouble("marks"), 0.001)
    assertEquals("Well argued", marksJson.getJSONObject("2").getString("comment"))

    val detailJson = """
      {
        "quiz": {"id":"qz-5","title":"Midterm","class_id":"cls-1","class_name":"BSc CS","unit_code":"cs201","total_marks":12.0,"answer_format":"both"},
        "student": {"id":"stu-1","full_name":"Ada Lovelace","admission_number":"CS/01"},
        "attempt": {"status":"marking","submitted_at":"2026-11-21T10:00:00Z","late":false,"mcq_marks":2.0,"draft_version":3,"overall_comment":"Good start","marked_pdf_path":""},
        "questions": [
          {"ord":1,"type":"mcq","question":"1+1?","options":["1","2"],"marks":2.0,"correct_index":1,"chosen_index":1},
          {"ord":2,"type":"written","question":"Explain FIFO.","marks":10.0,"written_answer":"First in first out","model_answer":"Queue rule","awarded_marks":8.5,"comment":"Clear"}
        ],
        "draft": $serialized
      }
    """.trimIndent()
    val paper = ExamMarkingService.parseExamPaper(detailJson)
    assertEquals("CS201", paper.quiz.unitCode)
    assertEquals(MarkingStatus.MARKING, paper.attempt.status)
    assertEquals(3, paper.attempt.draftVersion)
    assertEquals(2, paper.questions.size)
    assertEquals(8.5, paper.questions[1].awardedMarks!!, 0.001)
    assertFalse(paper.draft.questions[2]!!.strokes.isEmpty())

    assertEquals("10", formatMarks(10.0))
    assertEquals("8.5", formatMarks(8.5))
  }

  @Test
  fun markedPaperPdfGenerator_producesValidPdfWithInkAndMarks() {
    val paper = ExamPaperDetail(
      quiz = ExamPaperQuizInfo(
        id = "qz-5",
        title = "Midterm Exam",
        classId = "cls-1",
        className = "BSc CS Y2",
        unitCode = "CSC201",
        totalMarks = 12.0
      ),
      student = ExamPaperStudentInfo(id = "stu-1", fullName = "Ada Lovelace", admissionNumber = "CS/01"),
      attempt = ExamPaperAttemptInfo(status = MarkingStatus.MARKING, mcqMarks = 2.0),
      questions = listOf(
        ExamPaperQuestion(
          ord = 1,
          questionType = QuestionType.MCQ,
          question = "What is FIFO?",
          options = listOf("Stack", "Queue"),
          marks = 2.0,
          correctIndex = 1,
          chosenIndex = 1
        ),
        ExamPaperQuestion(
          ord = 2,
          questionType = QuestionType.WRITTEN,
          question = "Explain stack vs queue.",
          marks = 10.0,
          writtenAnswer = "A stack is LIFO while a queue is FIFO.\nStacks serve undo operations; queues serve print spooling."
        )
      )
    )
    val draft = MarkingDraftData(
      questions = mapOf(
        2 to QuestionMarkingDraft(
          strokes = listOf(InkStroke(listOf(NormalizedPoint(0.1f, 0.2f), NormalizedPoint(0.6f, 0.5f)))),
          stamps = listOf(MarkingStamp(StampKind.TICK, 0.8f, 0.3f, "+9"))
        )
      )
    )
    val bytes = MarkedPaperPdfGenerator.generatePdfBytes(
      paper = paper,
      draft = draft,
      questionMarks = mapOf(2 to QuestionMarkInput(9.0, "Spot on!")),
      overallComment = "Strong script overall."
    )
    assertTrue(bytes.size > 200)
    val header = String(bytes.take(5).toByteArray(), Charsets.US_ASCII)
    assertEquals("%PDF-", header)
  }

  @Test
  fun notificationRouter_routesPaperMarkedAndInvokesQuizDetailCallback() {
    val cls = ClassItem(id = "cls-9", name = "BSc CS", code = "CS")
    val session = UserSession(
      userId = "u1",
      email = "s@u.edu",
      fullName = "Student",
      admissionNumber = "ADM-1",
      role = "student",
      accessToken = "tok"
    )
    var openedTab: ClassTab? = null
    var openedSubView: MoreSubView? = null
    var openedQuizKind: String? = null
    var openedQuizId: String? = null

    val handled = MainActivityNotificationRouter.routeDeepLink(
      link = NotificationDeepLink(kind = "paper_marked", classId = "cls-9", data = mapOf("quiz_id" to "qz-777")),
      session = session,
      classes = listOf(cls),
      selectedClass = null,
      openClassDirectly = {},
      onOpenTab = { tab, sub -> openedTab = tab; openedSubView = sub },
      onOpenMyAttendance = {},
      onOpenAttendanceTracker = {},
      onOpenSignAttendance = {},
      onOpenManageMembers = {},
      onOpenHome = {},
      onLoadClasses = {},
      onOpenQuizDetail = { kind, qid, _ -> openedQuizKind = kind; openedQuizId = qid }
    )

    assertTrue(handled)
    assertEquals(ClassTab.More, openedTab)
    assertEquals(MoreSubView.Challenges, openedSubView)
    assertEquals("paper_marked", openedQuizKind)
    assertEquals("qz-777", openedQuizId)
  }

  @Test
  fun examSubmissionSorter_sortsByNameAndNaturalAdmissionOrderAndPrevNext() {
    val rows = listOf(
      ExamSubmissionRow(
        studentId = "u3",
        fullName = "zack Mwangi",
        admissionNumber = "SCT221-10/2024",
        submittedAt = "2026-11-21T09:05:00Z",
        markingStatus = MarkingStatus.PENDING
      ),
      ExamSubmissionRow(
        studentId = "u1",
        fullName = "Alice Wanjiku",
        admissionNumber = "SCT221-2/2024",
        submittedAt = "2026-11-21T09:01:00Z",
        markingStatus = MarkingStatus.RELEASED
      ),
      ExamSubmissionRow(
        studentId = "u2",
        fullName = "alice Wanjiku",
        admissionNumber = "SCT221-1/2024",
        submittedAt = "2026-11-21T09:02:00Z",
        markingStatus = MarkingStatus.MARKING
      ),
      ExamSubmissionRow(
        studentId = "u4",
        fullName = "Brian Otieno",
        admissionNumber = "-",
        submittedAt = "2026-11-21T09:03:00Z",
        markingStatus = MarkingStatus.PENDING
      )
    )

    // Strict Name A-Z (case-insensitive) with Admission No. tie-breaker (SCT221-1 before SCT221-2)
    val byName = com.example.ui.ExamSubmissionSorter.sortAndFilter(
      submissions = rows,
      filter = com.example.ui.SubmissionFilter.ALL,
      sortOrder = com.example.ui.SubmissionSortOrder.NAME_ASC,
      waitingFirst = false,
      searchQuery = ""
    )
    assertEquals(listOf("u2", "u1", "u4", "u3"), byName.map { it.studentId })

    // Natural Admission No. order: SCT221-1 before SCT221-2 before SCT221-10, and "-" last
    val byAdm = com.example.ui.ExamSubmissionSorter.sortAndFilter(
      submissions = rows,
      filter = com.example.ui.SubmissionFilter.ALL,
      sortOrder = com.example.ui.SubmissionSortOrder.ADMISSION_ASC,
      waitingFirst = false,
      searchQuery = ""
    )
    assertEquals(listOf("u2", "u1", "u3", "u4"), byAdm.map { it.studentId })

    // Waiting-first toggle puts PENDING rows first, then MARKING, then RELEASED
    val waitingFirst = com.example.ui.ExamSubmissionSorter.sortAndFilter(
      submissions = rows,
      filter = com.example.ui.SubmissionFilter.ALL,
      sortOrder = com.example.ui.SubmissionSortOrder.NAME_ASC,
      waitingFirst = true,
      searchQuery = ""
    )
    assertEquals(listOf("u4", "u3", "u2", "u1"), waitingFirst.map { it.studentId })

    // Search filters by admission substring cleanly
    val searched = com.example.ui.ExamSubmissionSorter.sortAndFilter(
      submissions = rows,
      filter = com.example.ui.SubmissionFilter.ALL,
      sortOrder = com.example.ui.SubmissionSortOrder.ADMISSION_ASC,
      waitingFirst = false,
      searchQuery = "221-10"
    )
    assertEquals(listOf("u3"), searched.map { it.studentId })

    // ChallengesUiState prev/next paper stepping follows the active sorted order
    val state = com.example.ui.ChallengesUiState(
      submissions = rows,
      submissionsSortOrder = com.example.ui.SubmissionSortOrder.ADMISSION_ASC,
      submissionsWaitingFirst = false,
      activePaper = ExamPaperDetail(
        quiz = ExamPaperQuizInfo(id = "qz-1", title = "Exam", classId = "cls-1", className = "CS"),
        student = ExamPaperStudentInfo(id = "u1", fullName = "Alice Wanjiku", admissionNumber = "SCT221-2/2024"),
        attempt = ExamPaperAttemptInfo(status = MarkingStatus.RELEASED),
        questions = emptyList()
      )
    )
    assertEquals(1, state.activePaperIndex)
    assertEquals("u2", state.prevPaperSubmission?.studentId)
    assertEquals("u3", state.nextPaperSubmission?.studentId)
  }
}
