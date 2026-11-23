package com.example.admin.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

data class AdminRankedEntry(
  val rank: Int,
  val row: AdminQuizResultRow,
  val percentage: Double,
  val gradeLabel: String
)

object AdminExamResultsPdfExporter {
  private const val PAGE_W = 595
  private const val PAGE_H = 842
  private const val MARGIN = 32f
  private const val CONTENT_W = PAGE_W - MARGIN * 2f

  private val naturalTokens = Regex("(\\d+)|(\\D+)")

  fun compareAlphaNumeric(a: String, b: String): Int {
    val tA = naturalTokens.findAll(a.trim().lowercase(Locale.ROOT)).map { it.value }.toList()
    val tB = naturalTokens.findAll(b.trim().lowercase(Locale.ROOT)).map { it.value }.toList()
    val count = minOf(tA.size, tB.size)
    for (i in 0 until count) {
      val p1 = tA[i]; val p2 = tB[i]
      if (p1 == p2) continue
      val n1 = p1.toLongOrNull(); val n2 = p2.toLongOrNull()
      if (n1 != null && n2 != null) {
        val c = n1.compareTo(n2)
        if (c != 0) return c
      } else {
        val c = p1.compareTo(p2)
        if (c != 0) return c
      }
    }
    return tA.size.compareTo(tB.size)
  }

  fun computeRankedEntries(rows: List<AdminQuizResultRow>, fallbackMaxTotal: Double): List<AdminRankedEntry> {
    val sorted = rows.sortedWith { a, b ->
      val sa = a.score; val sb = b.score
      when {
        sa != null && sb == null -> -1
        sa == null && sb != null -> 1
        sa != null && sb != null && abs(sa - sb) >= 0.001 -> sb.compareTo(sa)
        else -> {
          val admCmp = compareAlphaNumeric(a.admissionNumber, b.admissionNumber)
          if (admCmp != 0) admCmp else compareAlphaNumeric(a.fullName, b.fullName)
        }
      }
    }
    var lastScore: Double? = null
    var lastRank = 0
    return sorted.mapIndexed { idx, row ->
      val rank = if (row.score == null) {
        idx + 1
      } else if (lastScore != null && abs(row.score - lastScore!!) < 0.001) {
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
      val grade = when {
        row.score == null -> "Pend"
        pct >= 75.0 -> "A"
        pct >= 65.0 -> "B"
        pct >= 55.0 -> "C"
        pct >= 45.0 -> "D"
        else -> "E"
      }
      AdminRankedEntry(rank, row, pct, grade)
    }
  }

  private fun fmtMark(value: Double): String {
    val r = value.roundToLong()
    return if (abs(value - r.toDouble()) < 0.005) r.toString() else String.format(Locale.US, "%.1f", value)
  }

  fun exportAndShareRankedPdf(
    context: Context,
    quiz: AdminQuizSummary,
    className: String,
    rows: List<AdminQuizResultRow>
  ): File {
    val entries = computeRankedEntries(rows, quiz.totalMarks)
    val graded = entries.filter { it.row.score != null }
    val maxPossible = entries.firstOrNull { it.row.total > 0.0 }?.row?.total ?: quiz.totalMarks.coerceAtLeast(1.0)
    val scores = graded.mapNotNull { it.row.score }
    val avgScore = if (scores.isNotEmpty()) scores.average() else 0.0
    val avgPct = if (graded.isNotEmpty()) graded.map { it.percentage }.average() else 0.0
    val passCount = graded.count { it.percentage >= 50.0 }
    val passRate = if (graded.isNotEmpty()) (passCount * 100.0) / graded.size else 0.0
    val flagged = entries.count { it.row.late || it.row.focusLossCount > 0 || it.row.autoSubmittedReason.isNotBlank() }

    val doc = PdfDocument()
    var pageNum = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
    var canvas = page.canvas

    val bannerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#25004D"); style = Paint.Style.FILL }
    val goldLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D4AF37"); strokeWidth = 2f }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#F5E1A4"); textSize = 17f; typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E3D8FF"); textSize = 10f }
    val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#31005F"); textSize = 11.5f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val boxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F7F4FF"); style = Paint.Style.FILL }
    val boxBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D4AF37"); strokeWidth = 1f; style = Paint.Style.STROKE }
    val tblHeadBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#31005F"); style = Paint.Style.FILL }
    val tblHeadText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#F5E1A4"); textSize = 9f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val evenBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    val oddBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FAF8FF"); style = Paint.Style.FILL }
    val podiumBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFF8E1"); style = Paint.Style.FILL }
    val rowLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E0D8F0"); strokeWidth = 0.8f }
    val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1D162B"); textSize = 9.2f }
    val boldPaint = Paint(cellPaint).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
    val warnPaint = Paint(cellPaint).apply { color = Color.parseColor("#B71C1C"); typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
    val okPaint = Paint(cellPaint).apply { color = Color.parseColor("#167A46"); typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
    val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#666666"); textSize = 8.5f }

    val dateStr = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.US).format(ZonedDateTime.now())
    canvas.drawRoundRect(RectF(MARGIN, MARGIN, PAGE_W - MARGIN, MARGIN + 68f), 10f, 10f, bannerBg)
    canvas.drawText("ADMIN OFFICIAL RANKED RESULTS", MARGIN + 14f, MARGIN + 24f, titlePaint)
    val unitTag = if (quiz.unitCode.isNotBlank()) "${quiz.unitCode} • " else ""
    canvas.drawText("$unitTag${quiz.title}", MARGIN + 14f, MARGIN + 41f, subPaint)
    canvas.drawText("Class: ${className.ifBlank { "Class" }}   |   Generated: $dateStr", MARGIN + 14f, MARGIN + 56f, subPaint)
    var y = MARGIN + 78f
    canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, goldLine)
    y += 10f

    val boxGap = 8f
    val boxW = (CONTENT_W - boxGap * 3f) / 4f
    listOf(
      "Submissions" to "${entries.size} (${graded.size} graded)",
      "Class Mean" to String.format(Locale.US, "%.1f / %s (%.0f%%)", avgScore, fmtMark(maxPossible), avgPct),
      "High / Low" to "${fmtMark(scores.maxOrNull() ?: 0.0)} / ${fmtMark(scores.minOrNull() ?: 0.0)}",
      "Pass Rate" to String.format(Locale.US, "%.0f%% (%d/%d)", passRate, passCount, graded.size)
    ).forEachIndexed { idx, (lbl, v) ->
      val left = MARGIN + idx * (boxW + boxGap)
      val r = RectF(left, y, left + boxW, y + 40f)
      canvas.drawRoundRect(r, 6f, 6f, boxBg)
      canvas.drawRoundRect(r, 6f, 6f, boxBorder)
      canvas.drawText(lbl, left + 8f, y + 14f, mutedPaint)
      canvas.drawText(v.take(22), left + 8f, y + 30f, boldPaint)
    }
    y += 50f

    if (flagged > 0) {
      canvas.drawText("Integrity Alert: $flagged submission(s) have late/app-exit/auto-submit flags.", MARGIN, y + 8f, warnPaint)
      y += 18f
    }

    val top3 = graded.take(3)
    if (top3.isNotEmpty()) {
      canvas.drawText("TOP PERFORMERS", MARGIN, y + 10f, secPaint)
      y += 15f
      val pGap = 8f
      val pW = (CONTENT_W - pGap * 2f) / 3f
      top3.forEachIndexed { idx, e ->
        val left = MARGIN + idx * (pW + pGap)
        val r = RectF(left, y, left + pW, y + 42f)
        canvas.drawRoundRect(r, 6f, 6f, podiumBg)
        canvas.drawRoundRect(r, 6f, 6f, boxBorder)
        canvas.drawText("#${e.rank} ${e.row.fullName.take(18)}", left + 8f, y + 15f, boldPaint)
        canvas.drawText("${fmtMark(e.row.score ?: 0.0)}/${fmtMark(maxPossible)} (${e.percentage.toInt()}%)", left + 8f, y + 31f, okPaint)
      }
      y += 52f
    }

    fun drawHead(topY: Float): Float {
      canvas.drawRect(RectF(MARGIN, topY, PAGE_W - MARGIN, topY + 22f), tblHeadBg)
      val ty = topY + 14.5f
      canvas.drawText("Rank", MARGIN + 6f, ty, tblHeadText)
      canvas.drawText("Student Name", MARGIN + 40f, ty, tblHeadText)
      canvas.drawText("Adm No.", MARGIN + 196f, ty, tblHeadText)
      canvas.drawText("MCQ", MARGIN + 276f, ty, tblHeadText)
      canvas.drawText("Written", MARGIN + 314f, ty, tblHeadText)
      canvas.drawText("Score", MARGIN + 362f, ty, tblHeadText)
      canvas.drawText("%", MARGIN + 414f, ty, tblHeadText)
      canvas.drawText("Grd", MARGIN + 448f, ty, tblHeadText)
      canvas.drawText("Integrity / Status", MARGIN + 474f, ty, tblHeadText)
      return topY + 22f
    }

    y = drawHead(y)
    entries.forEachIndexed { idx, e ->
      val rowH = 20f
      if (y + rowH > PAGE_H - MARGIN - 16f) {
        canvas.drawText("ClassRep Admin Official Ranked Report • Page $pageNum", MARGIN, PAGE_H - 14f, mutedPaint)
        doc.finishPage(page); pageNum++
        page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
        canvas = page.canvas
        y = drawHead(MARGIN)
      }
      canvas.drawRect(RectF(MARGIN, y, PAGE_W - MARGIN, y + rowH), if (idx % 2 == 0) evenBg else oddBg)
      canvas.drawLine(MARGIN, y + rowH, PAGE_W - MARGIN, y + rowH, rowLine)
      val r = e.row; val ty = y + 13.5f
      canvas.drawText("#${e.rank}", MARGIN + 6f, ty, boldPaint)
      canvas.drawText(r.fullName.take(24), MARGIN + 40f, ty, cellPaint)
      canvas.drawText(r.admissionNumber.take(12), MARGIN + 196f, ty, cellPaint)
      canvas.drawText(r.mcqMarks?.let { fmtMark(it) } ?: "-", MARGIN + 276f, ty, cellPaint)
      canvas.drawText(r.writtenMarks?.let { fmtMark(it) } ?: "-", MARGIN + 314f, ty, cellPaint)
      val sc = r.score?.let { "${fmtMark(it)}/${fmtMark(maxPossible)}" } ?: "Pending"
      canvas.drawText(sc.take(9), MARGIN + 362f, ty, if (r.score != null) boldPaint else mutedPaint)
      canvas.drawText(if (r.score != null) "${e.percentage.toInt()}%" else "-", MARGIN + 414f, ty, cellPaint)
      canvas.drawText(e.gradeLabel, MARGIN + 448f, ty, boldPaint)
      val flags = buildList {
        if (r.late) add("LATE")
        if (r.focusLossCount > 0) add("EXIT(${r.focusLossCount})")
        if (r.autoSubmittedReason.isNotBlank() && !r.late) add("AUTO")
        if (isEmpty()) add(if (r.markingStatus == "released") "Returned" else r.markingStatus)
      }.joinToString(" ")
      canvas.drawText(flags.take(13), MARGIN + 474f, ty, if (r.late || r.focusLossCount > 0) warnPaint else okPaint)
      y += rowH
    }
    canvas.drawText("ClassRep Admin Official Ranked Report • Page $pageNum", MARGIN, PAGE_H - 14f, mutedPaint)
    doc.finishPage(page)

    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val safeTitle = quiz.title.replace(Regex("[^A-Za-z0-9_-]"), "_").take(32).ifBlank { "exam" }
    val file = File(dir, "ranked_${safeTitle}.pdf")
    file.outputStream().use { doc.writeTo(it) }
    doc.close()

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "application/pdf"
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Ranked Exam Results PDF"))
    return file
  }
}
