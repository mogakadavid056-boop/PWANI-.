package com.example.data

import com.example.util.JsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class ExamMarkingService(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
  private val pdfMediaType = "application/pdf".toMediaType()

  private suspend fun postRpc(
    rpcName: String,
    payload: JSONObject,
    accessToken: String
  ): Result<String> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    if (accessToken.isBlank()) {
      return@withContext Result.failure(IllegalStateException("Session expired. Please log in again."))
    }
    val req = Request.Builder()
      .url("${SupabaseConfig.url}/rest/v1/rpc/$rpcName")
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(payload.toString().toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (resp.isSuccessful) Result.success(body)
        else Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Network request failed")))
    }
  }

  suspend fun listPapersToMark(accessToken: String): Result<List<PaperToMarkItem>> =
    postRpc("list_papers_to_mark", JSONObject(), accessToken).mapCatching { parsePapersToMark(it) }

  suspend fun listExamSubmissions(quizId: String, accessToken: String): Result<List<ExamSubmissionRow>> {
    val payload = JSONObject().apply { put("target_quiz_id", quizId) }
    return postRpc("list_exam_submissions", payload, accessToken).mapCatching { parseExamSubmissions(it) }
  }

  suspend fun getExamPaper(quizId: String, studentId: String, accessToken: String): Result<ExamPaperDetail> {
    val payload = JSONObject().apply {
      put("target_quiz_id", quizId)
      put("target_student_id", studentId)
    }
    return postRpc("get_exam_paper", payload, accessToken).mapCatching { parseExamPaper(it) }
  }

  suspend fun saveMarkingDraft(
    quizId: String,
    studentId: String,
    expectedVersion: Int,
    draftData: MarkingDraftData,
    questionMarks: Map<Int, QuestionMarkInput>,
    overallComment: String,
    accessToken: String
  ): Result<Int> {
    val payload = JSONObject().apply {
      put("target_quiz_id", quizId)
      put("target_student_id", studentId)
      put("expected_version", expectedVersion)
      put("draft_json", serializeMarkingDraft(draftData))
      put("marks_json", serializeQuestionMarks(questionMarks))
      put("overall_text", overallComment.trim().take(2000))
    }
    return postRpc("save_marking_draft", payload, accessToken).mapCatching { body ->
      body.trim().trim('"').toIntOrNull() ?: (expectedVersion + 1)
    }
  }

  suspend fun uploadMarkedPdf(
    storagePath: String,
    pdfBytes: ByteArray,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    if (accessToken.isBlank()) {
      return@withContext Result.failure(IllegalStateException("Session expired. Please log in again."))
    }
    val uploadUrl = "${SupabaseConfig.url}/storage/v1/object/marked-papers/$storagePath"
    val req = Request.Builder()
      .url(uploadUrl)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/pdf")
      .addHeader("x-upsert", "true")
      .post(pdfBytes.toRequestBody(pdfMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        if (resp.isSuccessful) Result.success(Unit)
        else {
          val err = resp.body?.string().orEmpty()
          Result.failure(Exception(SupabaseConfig.extractErrorMessage(err, resp.code)))
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to upload marked PDF")))
    }
  }

  suspend fun releaseMarkedPaper(
    quizId: String,
    studentId: String,
    pdfPath: String,
    questionMarks: Map<Int, QuestionMarkInput>,
    overallComment: String,
    accessToken: String
  ): Result<Unit> {
    val payload = JSONObject().apply {
      put("target_quiz_id", quizId)
      put("target_student_id", studentId)
      put("pdf_path", pdfPath)
      put("marks_json", serializeQuestionMarks(questionMarks))
      put("overall_text", overallComment.trim().take(2000))
    }
    return postRpc("release_marked_paper", payload, accessToken).map { }
  }

  suspend fun reopenMarkedPaper(
    quizId: String,
    studentId: String,
    accessToken: String
  ): Result<Unit> {
    val payload = JSONObject().apply {
      put("target_quiz_id", quizId)
      put("target_student_id", studentId)
    }
    return postRpc("reopen_marked_paper", payload, accessToken).map { }
  }

  suspend fun getMyMarkedPaper(quizId: String, accessToken: String): Result<MyMarkedPaperInfo?> {
    val payload = JSONObject().apply { put("target_quiz_id", quizId) }
    return postRpc("get_my_marked_paper", payload, accessToken).mapCatching { parseMyMarkedPaper(it) }
  }

  suspend fun downloadMarkedPdf(
    storagePath: String,
    destinationFile: File,
    accessToken: String
  ): Result<File> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    if (accessToken.isBlank()) {
      return@withContext Result.failure(IllegalStateException("Session expired. Please log in again."))
    }
    val signUrl = "${SupabaseConfig.url}/storage/v1/object/sign/marked-papers/$storagePath"
    val signBody = JSONObject().apply { put("expiresIn", 600) }
    val signReq = Request.Builder()
      .url(signUrl)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .post(signBody.toString().toRequestBody(jsonMediaType))
      .build()

    val downloadUrl = try {
      client.newCall(signReq).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (resp.isSuccessful) {
          val signed = JSONObject(body).optString("signedURL")
          if (signed.isNotBlank()) {
            if (signed.startsWith("http")) signed else "${SupabaseConfig.url}/storage/v1$signed"
          } else {
            "${SupabaseConfig.url}/storage/v1/object/authenticated/marked-papers/$storagePath"
          }
        } else {
          "${SupabaseConfig.url}/storage/v1/object/authenticated/marked-papers/$storagePath"
        }
      }
    } catch (_: Exception) {
      "${SupabaseConfig.url}/storage/v1/object/authenticated/marked-papers/$storagePath"
    }

    val req = Request.Builder()
      .url(downloadUrl)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .get()
      .build()

    try {
      client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) {
          val err = resp.body?.string().orEmpty()
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(err, resp.code)))
        }
        val bytes = resp.body?.bytes()
          ?: return@withContext Result.failure(Exception("Empty PDF response"))
        if (bytes.size < 16) {
          return@withContext Result.failure(Exception("Downloaded PDF was invalid"))
        }
        destinationFile.parentFile?.mkdirs()
        destinationFile.writeBytes(bytes)
        Result.success(destinationFile)
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to download marked PDF")))
    }
  }

  suspend fun deleteMarkedPdfFromStorage(
    storagePath: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    if (accessToken.isBlank() || storagePath.isBlank()) {
      return@withContext Result.success(Unit)
    }
    val deleteUrl = "${SupabaseConfig.url}/storage/v1/object/marked-papers/$storagePath"
    val req = Request.Builder()
      .url(deleteUrl)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .delete()
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        if (resp.isSuccessful || resp.code == 404) Result.success(Unit)
        else {
          val err = resp.body?.string().orEmpty()
          Result.failure(Exception(SupabaseConfig.extractErrorMessage(err, resp.code)))
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to remove remote PDF")))
    }
  }

  suspend fun confirmMarkedPaperDownloaded(
    quizId: String,
    accessToken: String
  ): Result<Boolean> {
    val payload = JSONObject().apply { put("target_quiz_id", quizId) }
    return postRpc("confirm_marked_paper_downloaded", payload, accessToken).mapCatching { body ->
      body.trim().equals("true", ignoreCase = true)
    }
  }

  suspend fun getMyMarkedPaperDetail(
    quizId: String,
    accessToken: String
  ): Result<ExamPaperDetail> {
    val payload = JSONObject().apply { put("target_quiz_id", quizId) }
    return postRpc("get_my_marked_paper_detail", payload, accessToken).mapCatching { parseExamPaper(it) }
  }

  companion object {
    fun buildPdfStoragePath(classId: String, quizId: String, studentId: String): String =
      "$classId/$quizId/$studentId.pdf"

    fun parsePapersToMark(body: String): List<PaperToMarkItem> {
      val trimmed = body.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyList()
      val arr = JSONArray(trimmed)
      return buildList {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          add(
            PaperToMarkItem(
              quizId = JsonHelper.str(o, "quiz_id"),
              classId = JsonHelper.str(o, "class_id"),
              className = JsonHelper.str(o, "class_name").ifBlank { "Class" },
              title = JsonHelper.str(o, "title").ifBlank { "Exam" },
              closesAt = JsonHelper.str(o, "closes_at"),
              waiting = JsonHelper.intVal(o, "waiting", 0),
              inProgress = JsonHelper.intVal(o, "in_progress", 0),
              returned = JsonHelper.intVal(o, "returned", 0)
            )
          )
        }
      }
    }

    fun parseExamSubmissions(body: String): List<ExamSubmissionRow> {
      val trimmed = body.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyList()
      val arr = JSONArray(trimmed)
      return buildList {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          add(
            ExamSubmissionRow(
              studentId = JsonHelper.str(o, "student_id"),
              fullName = JsonHelper.str(o, "full_name").ifBlank { "Student" },
              admissionNumber = JsonHelper.orDash(JsonHelper.str(o, "admission_number")),
              submittedAt = JsonHelper.str(o, "submitted_at"),
              late = JsonHelper.bool(o, "late", false),
              mcqMarks = JsonHelper.doubleOrNull(o, "mcq_marks"),
              writtenMarks = JsonHelper.doubleOrNull(o, "written_marks"),
              finalScore = JsonHelper.doubleOrNull(o, "final_score"),
              maxTotal = JsonHelper.doubleVal(o, "max_total", 0.0),
              markingStatus = MarkingStatus.fromApi(JsonHelper.strOrNull(o, "marking_status")),
              focusLossCount = JsonHelper.intVal(o, "focus_loss_count", 0),
              autoSubmittedReason = JsonHelper.str(o, "auto_submitted_reason"),
              writtenReadyCount = JsonHelper.intVal(o, "written_ready_count", 0),
              writtenTotalCount = JsonHelper.intVal(o, "written_total_count", 0)
            )
          )
        }
      }
    }

    fun parseExamPaper(body: String): ExamPaperDetail {
      val trimmed = body.trim()
      val root = when {
        trimmed.startsWith("[") -> JSONArray(trimmed).optJSONObject(0) ?: JSONObject()
        trimmed.startsWith("{") -> JSONObject(trimmed)
        else -> JSONObject()
      }
      val quizObj = root.optJSONObject("quiz") ?: JSONObject()
      val studentObj = root.optJSONObject("student") ?: JSONObject()
      val attemptObj = root.optJSONObject("attempt") ?: JSONObject()
      val qArr = root.optJSONArray("questions") ?: JSONArray()

      val questions = buildList {
        for (i in 0 until qArr.length()) {
          val qObj = qArr.optJSONObject(i) ?: continue
          val optsArr = qObj.optJSONArray("options")
          val opts = buildList {
            if (optsArr != null) {
              for (j in 0 until optsArr.length()) {
                val optStr = optsArr.optString(j, "").trim()
                if (optStr.isNotBlank() && !optStr.equals("null", ignoreCase = true)) add(optStr)
              }
            }
          }
          add(
            ExamPaperQuestion(
              ord = JsonHelper.intVal(qObj, "ord", i + 1),
              questionType = QuestionType.fromApi(JsonHelper.strOrNull(qObj, "type")),
              question = JsonHelper.str(qObj, "question"),
              options = opts,
              marks = JsonHelper.doubleVal(qObj, "marks", 1.0),
              correctIndex = if (qObj.has("correct_index") && !qObj.isNull("correct_index")) qObj.optInt("correct_index") else null,
              chosenIndex = JsonHelper.intVal(qObj, "chosen_index", -1),
              writtenAnswer = JsonHelper.str(qObj, "written_answer"),
              modelAnswer = JsonHelper.str(qObj, "model_answer"),
              awardedMarks = JsonHelper.doubleOrNull(qObj, "awarded_marks"),
              comment = JsonHelper.str(qObj, "comment")
            )
          )
        }
      }.sortedBy { it.ord }

      return ExamPaperDetail(
        quiz = ExamPaperQuizInfo(
          id = JsonHelper.str(quizObj, "id"),
          title = JsonHelper.str(quizObj, "title").ifBlank { "Exam Paper" },
          classId = JsonHelper.str(quizObj, "class_id"),
          className = JsonHelper.str(quizObj, "class_name"),
          unitCode = JsonHelper.str(quizObj, "unit_code").uppercase(),
          unitName = JsonHelper.str(quizObj, "unit_name"),
          closesAt = JsonHelper.str(quizObj, "closes_at"),
          answerFormat = ExamAnswerFormat.fromApi(JsonHelper.strOrNull(quizObj, "answer_format")),
          totalMarks = JsonHelper.doubleVal(quizObj, "total_marks", 0.0)
        ),
        student = ExamPaperStudentInfo(
          id = JsonHelper.str(studentObj, "id"),
          fullName = JsonHelper.str(studentObj, "full_name").ifBlank { "Student" },
          admissionNumber = JsonHelper.orDash(JsonHelper.str(studentObj, "admission_number"))
        ),
        attempt = ExamPaperAttemptInfo(
          status = MarkingStatus.fromApi(JsonHelper.strOrNull(attemptObj, "status")),
          submittedAt = JsonHelper.str(attemptObj, "submitted_at"),
          late = JsonHelper.bool(attemptObj, "late", false),
          mcqMarks = JsonHelper.doubleOrNull(attemptObj, "mcq_marks"),
          writtenMarks = JsonHelper.doubleOrNull(attemptObj, "written_marks"),
          draftVersion = JsonHelper.intVal(attemptObj, "draft_version", 0),
          overallComment = JsonHelper.str(attemptObj, "overall_comment"),
          markedPdfPath = JsonHelper.str(attemptObj, "marked_pdf_path")
        ),
        questions = questions,
        draft = parseMarkingDraft(root.optJSONObject("draft"))
      )
    }

    fun parseMarkingDraft(obj: JSONObject?): MarkingDraftData {
      if (obj == null) return MarkingDraftData()
      val questionsObj = obj.optJSONObject("questions") ?: return MarkingDraftData()
      val map = buildMap {
        val keys = questionsObj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          val ord = k.toIntOrNull() ?: continue
          val qObj = questionsObj.optJSONObject(k) ?: continue
          val strokes = buildList {
            val sArr = qObj.optJSONArray("strokes")
            if (sArr != null) {
              for (i in 0 until sArr.length()) {
                val sObj = sArr.optJSONObject(i) ?: continue
                val ptsArr = sObj.optJSONArray("points") ?: continue
                val pts = buildList {
                  for (p in 0 until ptsArr.length()) {
                    val pObj = ptsArr.optJSONObject(p) ?: continue
                    add(
                      NormalizedPoint(
                        x = pObj.optDouble("x", 0.0).toFloat().coerceIn(0f, 1f),
                        y = pObj.optDouble("y", 0.0).toFloat().coerceIn(0f, 1f)
                      )
                    )
                  }
                }
                if (pts.isNotEmpty()) {
                  add(
                    InkStroke(
                      points = pts,
                      colorHex = sObj.optLong("color", 0xFFD4AF37L),
                      strokeWidth = sObj.optDouble("width", 3.5).toFloat().coerceIn(1f, 14f)
                    )
                  )
                }
              }
            }
          }
          val stamps = buildList {
            val stArr = qObj.optJSONArray("stamps")
            if (stArr != null) {
              for (i in 0 until stArr.length()) {
                val stObj = stArr.optJSONObject(i) ?: continue
                add(
                  MarkingStamp(
                    kind = StampKind.fromApi(JsonHelper.strOrNull(stObj, "kind")),
                    x = stObj.optDouble("x", 0.5).toFloat().coerceIn(0f, 1f),
                    y = stObj.optDouble("y", 0.5).toFloat().coerceIn(0f, 1f),
                    label = JsonHelper.str(stObj, "label")
                  )
                )
              }
            }
          }
          put(ord, QuestionMarkingDraft(strokes = strokes, stamps = stamps))
        }
      }
      return MarkingDraftData(questions = map)
    }

    fun serializeMarkingDraft(draft: MarkingDraftData): JSONObject {
      val questionsObj = JSONObject()
      draft.questions.forEach { (ord, qDraft) ->
        val sArr = JSONArray().apply {
          qDraft.strokes.forEach { stroke ->
            put(
              JSONObject().apply {
                put("color", stroke.colorHex)
                put("width", stroke.strokeWidth.toDouble())
                put(
                  "points",
                  JSONArray().apply {
                    stroke.points.forEach { pt ->
                      put(
                        JSONObject().apply {
                          put("x", pt.x.toDouble())
                          put("y", pt.y.toDouble())
                        }
                      )
                    }
                  }
                )
              }
            )
          }
        }
        val stArr = JSONArray().apply {
          qDraft.stamps.forEach { stamp ->
            put(
              JSONObject().apply {
                put("kind", stamp.kind.apiValue)
                put("x", stamp.x.toDouble())
                put("y", stamp.y.toDouble())
                if (stamp.label.isNotBlank()) put("label", stamp.label)
              }
            )
          }
        }
        questionsObj.put(
          ord.toString(),
          JSONObject().apply {
            put("strokes", sArr)
            put("stamps", stArr)
          }
        )
      }
      return JSONObject().apply {
        put("version", 1)
        put("questions", questionsObj)
      }
    }

    fun serializeQuestionMarks(marksMap: Map<Int, QuestionMarkInput>): JSONObject {
      val obj = JSONObject()
      marksMap.forEach { (ord, entry) ->
        if (entry.marks != null || entry.comment.isNotBlank()) {
          obj.put(
            ord.toString(),
            JSONObject().apply {
              if (entry.marks != null) put("marks", entry.marks)
              if (entry.comment.isNotBlank()) put("comment", entry.comment.trim().take(2000))
            }
          )
        }
      }
      return obj
    }

    fun parseMyMarkedPaper(body: String): MyMarkedPaperInfo? {
      val trimmed = body.trim()
      val o = when {
        trimmed.startsWith("[") -> JSONArray(trimmed).optJSONObject(0) ?: return null
        trimmed.startsWith("{") -> JSONObject(trimmed)
        else -> return null
      }
      return MyMarkedPaperInfo(
        markingStatus = MarkingStatus.fromApi(JsonHelper.strOrNull(o, "marking_status")),
        score = JsonHelper.doubleOrNull(o, "score"),
        total = JsonHelper.doubleVal(o, "total", 0.0),
        markedPdfPath = JsonHelper.strOrNull(o, "marked_pdf_path"),
        releasedAt = JsonHelper.strOrNull(o, "released_at"),
        overallComment = JsonHelper.str(o, "overall_comment"),
        pdfDownloadedAt = JsonHelper.strOrNull(o, "pdf_downloaded_at")
      )
    }
  }
}
