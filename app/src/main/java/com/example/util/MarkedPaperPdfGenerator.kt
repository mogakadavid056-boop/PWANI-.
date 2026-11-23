package com.example.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.ExamPaperDetail
import com.example.data.MarkingDraftData
import com.example.data.MarkingStamp
import com.example.data.QuestionMarkInput
import com.example.data.QuestionType
import com.example.data.StampKind
import com.example.data.formatMarks
import java.io.ByteArrayOutputStream
import kotlin.math.max

object MarkedPaperPdfGenerator {
  private const val PAGE_WIDTH = 595 // A4 points at 72 dpi
  private const val PAGE_HEIGHT = 842
  private const val MARGIN = 36f
  private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2

  fun generatePdfBytes(
    paper: ExamPaperDetail,
    draft: MarkingDraftData,
    questionMarks: Map<Int, QuestionMarkInput>,
    overallComment: String
  ): ByteArray {
    val doc = PdfDocument()
    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
    var page = doc.startPage(pageInfo)
    var canvas = page.canvas
    var cursorY = MARGIN

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#31005F")
      textSize = 18f
      typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#4A3D66")
      textSize = 11f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#1B0E33")
      textSize = 12.5f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#222222")
      textSize = 10.5f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#666666")
      textSize = 9.5f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
    }
    val goldLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#D4AF37")
      strokeWidth = 2f
      style = Paint.Style.STROKE
    }
    val boxBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#C7B8E8")
      strokeWidth = 1f
      style = Paint.Style.STROKE
    }
    val answerBoxBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#FAF8FF")
      style = Paint.Style.FILL
    }
    val scoreBoxBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#FFF8E1")
      style = Paint.Style.FILL
    }

    fun newPage() {
      drawFooter(canvas, pageNumber, paper.quiz.title, mutedPaint)
      doc.finishPage(page)
      pageNumber++
      pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
      page = doc.startPage(pageInfo)
      canvas = page.canvas
      cursorY = MARGIN
    }

    fun ensureSpace(neededHeight: Float) {
      if (cursorY + neededHeight > PAGE_HEIGHT - MARGIN - 20f) {
        newPage()
      }
    }

    // Header Banner
    canvas.drawText("CLASSREP MARKED EXAM PAPER", MARGIN, cursorY + 14f, titlePaint)
    cursorY += 22f
    val unitPrefix = if (paper.quiz.unitCode.isNotBlank()) "${paper.quiz.unitCode} • " else ""
    canvas.drawText("$unitPrefix${paper.quiz.title}", MARGIN, cursorY + 12f, headingPaint)
    cursorY += 18f
    canvas.drawLine(MARGIN, cursorY, PAGE_WIDTH - MARGIN, cursorY, goldLinePaint)
    cursorY += 12f

    // Student & Score Summary Card
    val mcqScore = paper.attempt.mcqMarks ?: paper.questions
      .filter { it.questionType == QuestionType.MCQ && it.chosenIndex == it.correctIndex }
      .sumOf { it.marks }
    val writtenScore = paper.questions
      .filter { it.questionType == QuestionType.WRITTEN }
      .sumOf { q -> questionMarks[q.ord]?.marks ?: q.awardedMarks ?: 0.0 }
    val finalTotal = paper.quiz.totalMarks.takeIf { it > 0.0 }
      ?: paper.questions.sumOf { it.marks }
    val totalGot = mcqScore + writtenScore

    val summaryRect = RectF(MARGIN, cursorY, PAGE_WIDTH - MARGIN, cursorY + 56f)
    canvas.drawRoundRect(summaryRect, 8f, 8f, scoreBoxBgPaint)
    canvas.drawRoundRect(summaryRect, 8f, 8f, goldLinePaint)
    canvas.drawText(
      "Student: ${paper.student.fullName} (${paper.student.admissionNumber})",
      MARGIN + 10f,
      cursorY + 18f,
      headingPaint
    )
    canvas.drawText(
      "Class: ${paper.quiz.className.ifBlank { "Class" }}   |   Late: ${if (paper.attempt.late) "Yes" else "No"}",
      MARGIN + 10f,
      cursorY + 34f,
      subtitlePaint
    )
    canvas.drawText(
      "Score: ${formatMarks(totalGot)} / ${formatMarks(finalTotal)}  (MCQ: ${formatMarks(mcqScore)}, Written: ${formatMarks(writtenScore)})",
      MARGIN + 10f,
      cursorY + 49f,
      headingPaint
    )
    cursorY += 68f

    if (overallComment.isNotBlank()) {
      val commentLines = wrapText("Lecturer Overall Comment: ${overallComment.trim()}", bodyPaint, CONTENT_WIDTH - 20f)
      val boxH = commentLines.size * 14f + 16f
      ensureSpace(boxH + 8f)
      val cRect = RectF(MARGIN, cursorY, PAGE_WIDTH - MARGIN, cursorY + boxH)
      canvas.drawRoundRect(cRect, 6f, 6f, answerBoxBgPaint)
      canvas.drawRoundRect(cRect, 6f, 6f, boxBorderPaint)
      var ly = cursorY + 14f
      commentLines.forEach { line ->
        canvas.drawText(line, MARGIN + 10f, ly, bodyPaint)
        ly += 14f
      }
      cursorY += boxH + 10f
    }

    // Questions Loop
    paper.questions.forEach { q ->
      val qHeading = wrapText(
        "Q${q.ord}. ${q.question} [${formatMarks(q.marks)} marks]",
        headingPaint,
        CONTENT_WIDTH
      )
      ensureSpace(qHeading.size * 15f + 46f)
      qHeading.forEach { line ->
        canvas.drawText(line, MARGIN, cursorY + 12f, headingPaint)
        cursorY += 15f
      }
      cursorY += 4f

      if (q.questionType == QuestionType.MCQ) {
        val isCorrect = q.chosenIndex >= 0 && q.chosenIndex == q.correctIndex
        val chosenLetter = if (q.chosenIndex in q.options.indices) ('A' + q.chosenIndex).toString() else "Skipped"
        val correctLetter = if (q.correctIndex != null && q.correctIndex in q.options.indices) ('A' + q.correctIndex).toString() else "-"
        val gotM = if (isCorrect) q.marks else 0.0
        val lineText = "Choice: $chosenLetter  |  Correct: $correctLetter  |  Awarded: ${formatMarks(gotM)} / ${formatMarks(q.marks)}"
        val statusPaint = Paint(bodyPaint).apply {
          color = if (isCorrect) Color.parseColor("#167A46") else Color.parseColor("#B71C1C")
        }
        canvas.drawText(lineText, MARGIN + 8f, cursorY + 11f, statusPaint)
        cursorY += 18f
      } else {
        val answerText = q.writtenAnswer.ifBlank { "(No answer submitted)" }
        val wrappedAns = wrapText(answerText, bodyPaint, CONTENT_WIDTH - 24f)
        val ansHeight = max(120f, wrappedAns.size * 14f + 22f)
        ensureSpace(ansHeight + 48f)

        val ansRect = RectF(MARGIN, cursorY, PAGE_WIDTH - MARGIN, cursorY + ansHeight)
        canvas.drawRoundRect(ansRect, 8f, 8f, answerBoxBgPaint)
        canvas.drawRoundRect(ansRect, 8f, 8f, boxBorderPaint)

        var ay = cursorY + 18f
        wrappedAns.forEach { line ->
          canvas.drawText(line, MARGIN + 12f, ay, bodyPaint)
          ay += 14f
        }

        // Render Hand-Marking Ink Strokes & Stamps over the student's answer box
        val qDraft = draft.questions[q.ord]
        if (qDraft != null) {
          drawStrokesInRect(canvas, ansRect, qDraft.strokes)
          drawStampsInRect(canvas, ansRect, qDraft.stamps)
        }
        cursorY += ansHeight + 6f

        val markEntry = questionMarks[q.ord]
        val awarded = markEntry?.marks ?: q.awardedMarks ?: 0.0
        val markText = "Marks Awarded: ${formatMarks(awarded)} / ${formatMarks(q.marks)}"
        val awardPaint = Paint(headingPaint).apply { color = Color.parseColor("#31005F") }
        canvas.drawText(markText, MARGIN + 4f, cursorY + 11f, awardPaint)
        cursorY += 16f

        val qComment = markEntry?.comment?.takeIf { it.isNotBlank() } ?: q.comment
        if (qComment.isNotBlank()) {
          val commentLines = wrapText("Comment: $qComment", mutedPaint, CONTENT_WIDTH - 12f)
          ensureSpace(commentLines.size * 13f + 6f)
          commentLines.forEach { line ->
            canvas.drawText(line, MARGIN + 4f, cursorY + 10f, mutedPaint)
            cursorY += 13f
          }
        }
      }

      cursorY += 10f
      canvas.drawLine(MARGIN, cursorY, PAGE_WIDTH - MARGIN, cursorY, boxBorderPaint)
      cursorY += 10f
    }

    drawFooter(canvas, pageNumber, paper.quiz.title, mutedPaint)
    doc.finishPage(page)

    val out = ByteArrayOutputStream()
    doc.writeTo(out)
    doc.close()
    return out.toByteArray()
  }

  private fun drawFooter(canvas: Canvas, pageNumber: Int, title: String, paint: Paint) {
    canvas.drawText("ClassRep • $title • Page $pageNumber", MARGIN, PAGE_HEIGHT - 16f, paint)
  }

  private fun drawStrokesInRect(canvas: Canvas, rect: RectF, strokes: List<com.example.data.InkStroke>) {
    strokes.forEach { stroke ->
      if (stroke.points.isEmpty()) return@forEach
      val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pdfSafeStrokeColor(stroke.colorHex)
        strokeWidth = stroke.strokeWidth.coerceIn(1.5f, 8f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
      }
      if (stroke.points.size == 1) {
        val pt = stroke.points.first()
        canvas.drawPoint(
          rect.left + pt.x * rect.width(),
          rect.top + pt.y * rect.height(),
          p
        )
      } else {
        val path = Path()
        stroke.points.forEachIndexed { idx, pt ->
          val px = rect.left + pt.x * rect.width()
          val py = rect.top + pt.y * rect.height()
          if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        canvas.drawPath(path, p)
      }
    }
  }

  private fun drawStampsInRect(canvas: Canvas, rect: RectF, stamps: List<MarkingStamp>) {
    stamps.forEach { stamp ->
      val cx = rect.left + stamp.x * rect.width()
      val cy = rect.top + stamp.y * rect.height()
      val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 18f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        color = if (stamp.kind == StampKind.TICK) Color.parseColor("#167A46") else Color.parseColor("#C62828")
      }
      val text = if (stamp.label.isNotBlank()) "${stamp.kind.symbol} ${stamp.label}" else stamp.kind.symbol
      canvas.drawText(text, cx - 6f, cy + 6f, paint)
    }
  }

  private fun pdfSafeStrokeColor(argbLong: Long): Int {
    val raw = argbLong.toInt()
    val r = Color.red(raw)
    val g = Color.green(raw)
    val b = Color.blue(raw)
    // Bright ChampagneGold / SilverWhite inks would be too faint on white PDF paper; darken for legibility
    return if (r + g + b > 550) Color.parseColor("#7A5C0E") else raw
  }

  private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
    val result = mutableListOf<String>()
    text.split('\n').forEach { paragraph ->
      if (paragraph.isBlank()) {
        result.add("")
      } else {
        val words = paragraph.split(Regex("\\s+"))
        var line = StringBuilder()
        for (word in words) {
          val candidate = if (line.isEmpty()) word else "$line $word"
          if (paint.measureText(candidate) <= maxWidth) {
            line = StringBuilder(candidate)
          } else {
            if (line.isNotEmpty()) result.add(line.toString())
            line = StringBuilder(word)
          }
        }
        if (line.isNotEmpty()) result.add(line.toString())
      }
    }
    return if (result.isEmpty()) listOf("") else result
  }
}
