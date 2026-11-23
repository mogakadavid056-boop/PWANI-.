package com.example.ui

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.data.ChallengeItem
import com.example.data.ExamMarkingService
import com.example.data.ExamSubmissionRow
import com.example.data.MarkingStatus
import com.example.data.QuestionMarkInput
import com.example.data.QuestionType
import com.example.data.QuizResultRow
import com.example.util.MarkedPaperPdfGenerator
import com.example.util.RankedExamResultsPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal data class BatchReleaseOutcome(
  val releasedCount: Int,
  val skippedCount: Int,
  val firstError: String?
)

internal object ChallengesBatchAndPdfHelper {
  suspend fun verifyPdfReadable(file: File): Boolean = withContext(Dispatchers.IO) {
    if (!file.exists() || file.length() < 32L) return@withContext false
    runCatching {
      ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        PdfRenderer(pfd).use { renderer -> renderer.pageCount > 0 }
      }
    }.getOrDefault(false)
  }

  suspend fun downloadVerifyAndCleanupRemotePdf(
    context: Context,
    quizId: String,
    accessToken: String,
    markingService: ExamMarkingService
  ): Result<File> {
    val permanentDir = File(context.filesDir, "marked_papers").apply { mkdirs() }
    val localFile = File(permanentDir, "my_$quizId.pdf")

    val info = markingService.getMyMarkedPaper(quizId, accessToken).getOrElse {
      if (verifyPdfReadable(localFile)) return Result.success(localFile)
      return Result.failure(it)
    } ?: return Result.failure(IllegalStateException("Your marked paper has not been returned yet."))

    if (info.markingStatus != MarkingStatus.RELEASED) {
      return Result.failure(IllegalStateException("Your marked paper is not ready yet."))
    }

    val remotePath = info.markedPdfPath
    if (!remotePath.isNullOrBlank()) {
      val dl = markingService.downloadMarkedPdf(remotePath, localFile, accessToken)
      if (dl.isSuccess && verifyPdfReadable(localFile)) {
        // Delete from Supabase Storage while marked_pdf_path still matches RLS, then clear DB path
        markingService.deleteMarkedPdfFromStorage(remotePath, accessToken)
        markingService.confirmMarkedPaperDownloaded(quizId, accessToken)
        return Result.success(localFile)
      }
    }

    if (verifyPdfReadable(localFile)) {
      return Result.success(localFile)
    }

    // Cross-device or cleared-cache fallback: rebuild identical PDF locally from saved DB marks & ink
    val detail = markingService.getMyMarkedPaperDetail(quizId, accessToken).getOrElse {
      return Result.failure(it)
    }
    val qMarks = buildMap {
      detail.questions.filter { it.questionType == QuestionType.WRITTEN }.forEach { q ->
        put(q.ord, QuestionMarkInput(marks = q.awardedMarks, comment = q.comment))
      }
    }
    val bytes = withContext(Dispatchers.Default) {
      MarkedPaperPdfGenerator.generatePdfBytes(
        paper = detail,
        draft = detail.draft,
        questionMarks = qMarks,
        overallComment = detail.attempt.overallComment
      )
    }
    withContext(Dispatchers.IO) {
      localFile.parentFile?.mkdirs()
      localFile.writeBytes(bytes)
    }
    return Result.success(localFile)
  }

  suspend fun releaseAllReadyPapers(
    context: Context,
    quizId: String,
    readyRows: List<ExamSubmissionRow>,
    accessToken: String,
    markingService: ExamMarkingService,
    onProgress: (done: Int, total: Int) -> Unit
  ): BatchReleaseOutcome {
    var released = 0
    var skipped = 0
    var firstErr: String? = null
    val total = readyRows.size

    for ((idx, row) in readyRows.withIndex()) {
      onProgress(idx, total)
      val detail = markingService.getExamPaper(quizId, row.studentId, accessToken).getOrElse { err ->
        if (firstErr == null) firstErr = err.message
        skipped++
        continue
      }
      val marksMap = buildMap {
        detail.questions.filter { it.questionType == QuestionType.WRITTEN }.forEach { q ->
          put(q.ord, QuestionMarkInput(marks = q.awardedMarks, comment = q.comment))
        }
      }
      val missingWritten = detail.questions.any { q ->
        q.questionType == QuestionType.WRITTEN && marksMap[q.ord]?.marks == null
      }
      if (missingWritten) {
        skipped++
        continue
      }

      val pdfBytes = runCatching {
        withContext(Dispatchers.Default) {
          MarkedPaperPdfGenerator.generatePdfBytes(
            paper = detail,
            draft = detail.draft,
            questionMarks = marksMap,
            overallComment = detail.attempt.overallComment
          )
        }
      }.getOrElse { err ->
        if (firstErr == null) firstErr = err.message
        skipped++
        continue
      }

      val storagePath = ExamMarkingService.buildPdfStoragePath(
        classId = detail.quiz.classId,
        quizId = detail.quiz.id,
        studentId = detail.student.id
      )
      val up = markingService.uploadMarkedPdf(storagePath, pdfBytes, accessToken)
      if (up.isFailure) {
        if (firstErr == null) firstErr = up.exceptionOrNull()?.message
        skipped++
        continue
      }
      val rel = markingService.releaseMarkedPaper(
        quizId = detail.quiz.id,
        studentId = detail.student.id,
        pdfPath = storagePath,
        questionMarks = marksMap,
        overallComment = detail.attempt.overallComment,
        accessToken = accessToken
      )
      if (rel.isSuccess) {
        withContext(Dispatchers.IO) {
          val localFile = File(File(context.cacheDir, "marked_papers"), "${detail.quiz.id}_${detail.student.id}.pdf")
          localFile.parentFile?.mkdirs()
          localFile.writeBytes(pdfBytes)
        }
        released++
      } else {
        if (firstErr == null) firstErr = rel.exceptionOrNull()?.message
        skipped++
      }
      onProgress(idx + 1, total)
    }

    return BatchReleaseOutcome(releasedCount = released, skippedCount = skipped, firstError = firstErr)
  }

  suspend fun buildRankedResultsPdfFile(
    context: Context,
    quiz: ChallengeItem,
    className: String,
    rows: List<QuizResultRow>,
    generatedBy: String
  ): File {
    val bytes = withContext(Dispatchers.Default) {
      RankedExamResultsPdfGenerator.generatePdfBytes(
        quiz = quiz,
        className = className,
        rows = rows,
        generatedBy = generatedBy
      )
    }
    val dir = File(context.cacheDir, "ranked_reports").apply { mkdirs() }
    val safe = quiz.title.replace(Regex("[^A-Za-z0-9_-]"), "_").take(40).ifBlank { "exam" }
    val file = File(dir, "ranked_${safe}_${quiz.quizId.take(8)}.pdf")
    withContext(Dispatchers.IO) {
      file.writeBytes(bytes)
    }
    return file
  }
}
