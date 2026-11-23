package com.example.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.ChallengeItem
import com.example.data.MarkingStatus
import com.example.data.QuizResultRow
import com.example.data.formatMarks
import com.example.ui.ExamSubmissionSorter
import java.io.ByteArrayOutputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RankedResultEntry(
  val rank: Int,
  val row: QuizResultRow,
  val percentage: Double,
  val gradeLabel: String
)

data class RankedResultsSummary(
  val totalSubmissions: Int,
  val gradedCount: Int,
  val pendingCount: Int,
  val averageScore: Double,
  val averagePercentage: Double,
  val highestScore: Double,
  val lowestScore: Double,
  val passCount: Int,
  val passRatePercent: Double,
  val flaggedCount: Int,
  val maxPossible: Double
)

object RankedExamResultsPdfGenerator {
  private const val PAGE_WIDTH = 595
  private const val PAGE_HEIGHT = 842
  private const val MARGIN = 32f
  private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2f

  fun computeRankedEntries(rows: List<QuizResultRow>, fallbackMaxTotal: Double = 0.0): List<RankedResultEntry> {
    val sorted = rows.sortedWith { a, b ->
      val sa = a.score
      val sb = b.score
      when {
        sa != null && sb == null -> -1
        sa == null && sb != null -> 1
        sa != null && sb != null && sa != sb -> sb.compareTo(sa)
        else -> {
          val admCmp = ExamSubmissionSorter.compareNatural(a.admissionNumber, b.admissionNumber)
          if (admCmp != 0) admCmp else ExamSubmissionSorter.compareNatural(a.fullName, b.fullName)
        }
      }
    }

    var lastScore: Double? = null
    var lastRank = 0
    return sorted.mapIndexed { idx, row ->
      val rank = if (row.score == null) {
        idx + 1
      } else if (lastScore != null && kotlin.math.abs(row.score - lastScore!!) < 0.001) {
        lastRank
      } else {
        idx + 1
      }
      if (row.score != null) {
        lastScore = row.score
        lastRank = rank
      }
      val denom = row.total.takeIf { it > 0.0 } ?: fallbackMaxTotal.takeIf { it > 0.0 } ?: 1.0
      val pct = if (row.score != null) ((row.score * 100.0) / denom).coerceIn(0.0, 100.0) else 0.0
      RankedResultEntry(
        rank = rank,
        row = row,
        percentage = pct,
        gradeLabel = if (row.score == null) "Pending" else computeGrade(pct)
      )
    }
  }

  fun computeSummary(entries: List<RankedResultEntry>, fallbackMaxTotal: Double = 0.0): RankedResultsSummary {
    val graded = entries.filter { it.row.score != null }
    val maxPossible = entries.firstOrNull { it.row.total > 0.0 }?.row?.total ?: fallbackMaxTotal.coerceAtLeast(1.0)
    val scores = graded.mapNotNull { it.row.score }
    val avgScore = if (scores.isNotEmpty()) scores.average() else 0.0
    val avgPct = if (graded.isNotEmpty()) graded.map { it.percentage }.average() else 0.0
    val passCount = graded.count { it.percentage >= 50.0 }
    val passRate = if (graded.isNotEmpty()) (passCount * 100.0) / graded.size else 0.0
    val flagged = entries.count { it.row.late || it.row.focusLossCount > 0 || it.row.autoSubmittedReason.isNotBlank() }

    return RankedResultsSummary(
      totalSubmissions = entries.size,
      gradedCount = graded.size,
      pendingCount = (entries.size - graded.size).coerceAtLeast(0),
      averageScore = avgScore,
      averagePercentage = avgPct,
      highestScore = scores.maxOrNull() ?: 0.0,
      lowestScore = scores.minOrNull() ?: 0.0,
      passCount = passCount,
      passRatePercent = passRate,
      flaggedCount = flagged,
      maxPossible = maxPossible
    )
  }

  private fun computeGrade(pct: Double): String = when {
    pct >= 75.0 -> "A"
    pct >= 65.0 -> "B"
    pct >= 55.0 -> "C"
    pct >= 45.0 -> "D"
    else -> "E"
  }

  fun generatePdfBytes(
    quiz: ChallengeItem,
    className: String,
    rows: List<QuizResultRow>,
    generatedBy: String = "Lecturer / Admin"
  ): ByteArray {
    val entries = computeRankedEntries(rows, quiz.totalMarks)
    val summary = computeSummary(entries, quiz.totalMarks)
    val doc = PdfDocument()
    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
    var page = doc.startPage(pageInfo)
    var canvas = page.canvas

    val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#25004D"); style = Paint.Style.FILL }
    val goldAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D4AF37"); strokeWidth = 2f }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#F5E1A4")
      textSize = 17f
      typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#E3D8FF")
      textSize = 10f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#31005F")
      textSize = 11.5f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val statCardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F7F4FF"); style = Paint.Style.FILL }
    val statCardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#D4AF37")
      strokeWidth = 1f
      style = Paint.Style.STROKE
    }
    val tableHeaderBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#31005F"); style = Paint.Style.FILL }
    val tableHeaderText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#F5E1A4")
      textSize = 9f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val rowEvenBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFFFFF"); style = Paint.Style.FILL }
    val rowOddBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FAF8FF"); style = Paint.Style.FILL }
    val podiumBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFF8E1"); style = Paint.Style.FILL }
    val rowBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E0D8F0"); strokeWidth = 0.8f }
    val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#1D162B")
      textSize = 9.2f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    val boldCellPaint = Paint(cellPaint).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
    val warnCellPaint = Paint(cellPaint).apply {
      color = Color.parseColor("#B71C1C")
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val okCellPaint = Paint(cellPaint).apply {
      color = Color.parseColor("#167A46")
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#666666")
      textSize = 8.5f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
    }

    val dateStr = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.US).format(ZonedDateTime.now())

    // Top Executive Banner
    val bannerRect = RectF(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, MARGIN + 68f)
    canvas.drawRoundRect(bannerRect, 10f, 10f, headerBgPaint)
    canvas.drawText("CLASSREP OFFICIAL RANKED RESULTS", MARGIN + 14f, MARGIN + 24f, titlePaint)
    val unitTag = if (quiz.unitCode.isNotBlank()) "${quiz.unitCode} • " else ""
    canvas.drawText("$unitTag${quiz.title}", MARGIN + 14f, MARGIN + 41f, subtitlePaint)
    canvas.drawText(
      "Class: ${className.ifBlank { "Class" }}   |   Generated: $dateStr   |   Staff: $generatedBy",
      MARGIN + 14f,
      MARGIN + 56f,
      subtitlePaint
    )
    var y = MARGIN + 78f
    canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, goldAccentPaint)
    y += 10f

    // Analytics Strip (4 metric boxes)
    val boxGap = 8f
    val boxW = (CONTENT_WIDTH - boxGap * 3f) / 4f
    val statsCards = listOf(
      "Submissions" to "${summary.totalSubmissions} (${summary.gradedCount} graded)",
      "Class Mean" to String.format(Locale.US, "%.1f / %s (%.0f%%)", summary.averageScore, formatMarks(summary.maxPossible), summary.averagePercentage),
      "High / Low" to "${formatMarks(summary.highestScore)} / ${formatMarks(summary.lowestScore)}",
      "Pass Rate" to String.format(Locale.US, "%.0f%% (%d/%d)", summary.passRatePercent, summary.passCount, summary.gradedCount)
    )
    statsCards.forEachIndexed { idx, (label, value) ->
      val left = MARGIN + idx * (boxW + boxGap)
      val rect = RectF(left, y, left + boxW, y + 40f)
      canvas.drawRoundRect(rect, 6f, 6f, statCardBg)
      canvas.drawRoundRect(rect, 6f, 6f, statCardBorder)
      canvas.drawText(label, left + 8f, y + 14f, mutedPaint)
      canvas.drawText(value.take(22), left + 8f, y + 30f, boldCellPaint)
    }
    y += 50f

    if (summary.flaggedCount > 0 || summary.pendingCount > 0) {
      canvas.drawText(
        "Notice: ${summary.pendingCount} paper(s) pending marking  •  ${summary.flaggedCount} submission(s) carry timing/app-exit flags.",
        MARGIN,
        y + 8f,
        if (summary.flaggedCount > 0) warnCellPaint else mutedPaint
      )
      y += 18f
    }

    // Top 3 Podium Highlight
    val top3 = entries.filter { it.row.score != null }.take(3)
    if (top3.isNotEmpty()) {
      canvas.drawText("TOP PERFORMERS", MARGIN, y + 10f, sectionPaint)
      y += 15f
      val pGap = 8f
      val pW = (CONTENT_WIDTH - pGap * 2f) / 3f
      top3.forEachIndexed { idx, entry ->
        val left = MARGIN + idx * (pW + pGap)
        val pRect = RectF(left, y, left + pW, y + 42f)
        canvas.drawRoundRect(pRect, 6f, 6f, podiumBg)
        canvas.drawRoundRect(pRect, 6f, 6f, statCardBorder)
        canvas.drawText("#${entry.rank}  ${entry.row.fullName.take(18)}", left + 8f, y + 15f, boldCellPaint)
        val scText = "${formatMarks(entry.row.score ?: 0.0)}/${formatMarks(summary.maxPossible)} (${String.format(Locale.US, "%.0f%%", entry.percentage)})"
        canvas.drawText(scText, left + 8f, y + 31f, okCellPaint)
      }
      y += 52f
    }

    fun drawTableHeader(topY: Float): Float {
      val headerRect = RectF(MARGIN, topY, PAGE_WIDTH - MARGIN, topY + 22f)
      canvas.drawRect(headerRect, tableHeaderBg)
      val ty = topY + 14.5f
      canvas.drawText("Rank", MARGIN + 6f, ty, tableHeaderText)
      canvas.drawText("Student Name", MARGIN + 40f, ty, tableHeaderText)
      canvas.drawText("Adm No.", MARGIN + 196f, ty, tableHeaderText)
      canvas.drawText("MCQ", MARGIN + 276f, ty, tableHeaderText)
      canvas.drawText("Written", MARGIN + 314f, ty, tableHeaderText)
      canvas.drawText("Score", MARGIN + 362f, ty, tableHeaderText)
      canvas.drawText("%", MARGIN + 414f, ty, tableHeaderText)
      canvas.drawText("Grd", MARGIN + 448f, ty, tableHeaderText)
      canvas.drawText("Integrity / Status", MARGIN + 474f, ty, tableHeaderText)
      return topY + 22f
    }

    fun drawFooter() {
      canvas.drawText(
        "ClassRep Official Ranked Report • Confidential (Lecturer & Admin Only) • Page $pageNumber",
        MARGIN,
        PAGE_HEIGHT - 14f,
        mutedPaint
      )
    }

    y = drawTableHeader(y)

    entries.forEachIndexed { idx, entry ->
      val rowH = 20f
      if (y + rowH > PAGE_HEIGHT - MARGIN - 16f) {
        drawFooter()
        doc.finishPage(page)
        pageNumber++
        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        page = doc.startPage(pageInfo)
        canvas = page.canvas
        y = drawTableHeader(MARGIN)
      }

      val bgRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowH)
      canvas.drawRect(bgRect, if (idx % 2 == 0) rowEvenBg else rowOddBg)
      canvas.drawLine(MARGIN, y + rowH, PAGE_WIDTH - MARGIN, y + rowH, rowBorder)

      val r = entry.row
      val ty = y + 13.5f
      canvas.drawText("#${entry.rank}", MARGIN + 6f, ty, boldCellPaint)
      canvas.drawText(r.fullName.take(24), MARGIN + 40f, ty, cellPaint)
      canvas.drawText(r.admissionNumber.take(12), MARGIN + 196f, ty, cellPaint)
      canvas.drawText(r.mcqMarks?.let { formatMarks(it) } ?: "-", MARGIN + 276f, ty, cellPaint)
      canvas.drawText(r.writtenMarks?.let { formatMarks(it) } ?: "-", MARGIN + 314f, ty, cellPaint)
      val scLabel = r.score?.let { "${formatMarks(it)}/${formatMarks(summary.maxPossible)}" } ?: "Pending"
      canvas.drawText(scLabel.take(9), MARGIN + 362f, ty, if (r.score != null) boldCellPaint else mutedPaint)
      canvas.drawText(
        if (r.score != null) String.format(Locale.US, "%.0f%%", entry.percentage) else "-",
        MARGIN + 414f,
        ty,
        cellPaint
      )
      canvas.drawText(entry.gradeLabel, MARGIN + 448f, ty, boldCellPaint)

      val flags = buildList {
        if (r.late) add("LATE")
        if (r.focusLossCount > 0) add("EXIT(${r.focusLossCount})")
        if (r.autoSubmittedReason.isNotBlank() && !r.late) add("AUTO")
        if (isEmpty()) {
          add(
            when (r.markingStatus) {
              MarkingStatus.RELEASED -> "Clean/Ret"
              MarkingStatus.AUTO -> "Clean"
              else -> r.markingStatus.label
            }
          )
        }
      }.joinToString(" ")
      val flagPaint = if (r.late || r.focusLossCount > 0) warnCellPaint else okCellPaint
      canvas.drawText(flags.take(13), MARGIN + 474f, ty, flagPaint)

      y += rowH
    }

    drawFooter()
    doc.finishPage(page)

    val out = ByteArrayOutputStream()
    doc.writeTo(out)
    doc.close()
    return out.toByteArray()
  }
}
