package com.example.data

import java.io.Reader
import java.io.StringReader
import java.util.Locale

data class CsvParseResult(
  val questions: List<DraftQuizQuestion> = emptyList(),
  val error: String? = null,
  val warnings: List<String> = emptyList()
)

object ExamCsvCodec {
  const val SAMPLE_CSV_TEMPLATE: String =
    "type,question,option_a,option_b,option_c,option_d,option_e,correct,marks,time,explanation,model_answer\n" +
      "mcq,\"Which data structure removes the oldest item first (FIFO)?\",Stack,Queue,Tree,Graph,,B,1,45s,\"A queue serves items in arrival order.\",\n" +
      "mcq,\"What does SQL stand for?\",Structured Query Language,Simple Question Language,Sequential Query Logic,Stored Query List,,A,1,30s,,\n" +
      "written,\"Explain the difference between a stack and a queue. Give one real-life example of each.\",,,,,,,10,5m,,\"Stack = LIFO (undo, browser back button). Queue = FIFO (printer queue, bank line). 2 marks per correct definition, 2 per example, 2 for a clear comparison.\"\n"

  private val QUESTION_ALIASES = setOf(
    "question", "question_text", "questiontext", "prompt", "stem", "q",
    "item", "body", "text", "query", "title", "question_title"
  )
  private val TYPE_ALIASES = setOf(
    "type", "question_type", "questiontype", "qtype", "q_type", "kind", "format", "category"
  )
  private val COMBINED_OPTIONS_ALIASES = setOf(
    "options", "choices", "alternatives", "option_list", "answer_choices"
  )
  private val CORRECT_ALIASES = setOf(
    "correct", "correct_answer", "correctanswer", "correct_option", "correctoption",
    "correct_choice", "correct_letter", "correct_index", "correctindex",
    "answer", "right_answer", "rightanswer", "key", "ans", "solution", "answer_index"
  )
  private val ZERO_BASED_CORRECT_ALIASES = setOf(
    "correct_index", "correctindex", "answer_index"
  )
  private val MARKS_ALIASES = setOf(
    "marks", "mark", "points", "point", "pts", "score", "weight", "value", "max_marks"
  )
  private val EXPLANATION_ALIASES = setOf(
    "explanation", "rationale", "feedback", "reason", "hint", "note", "notes"
  )
  private val MODEL_ANSWER_ALIASES = setOf(
    "model_answer", "modelanswer", "marking_guide", "markingguide", "rubric",
    "sample_answer", "expected_answer", "answer_key", "marking_scheme", "guide"
  )
  private val TIME_ALIASES = setOf(
    "time", "time_limit", "timelimit", "time_limit_seconds", "time_seconds", "duration", "question_time", "seconds"
  )
  private val OPTION_SLOT_ALIASES: List<Set<String>> = listOf(
    setOf("option_a", "optiona", "opt_a", "opta", "choice_a", "choicea", "ans_a", "answer_a", "a", "option_1", "option1", "opt_1", "opt1", "choice_1", "choice1", "ans_1", "answer_1"),
    setOf("option_b", "optionb", "opt_b", "optb", "choice_b", "choiceb", "ans_b", "answer_b", "b", "option_2", "option2", "opt_2", "opt2", "choice_2", "choice2", "ans_2", "answer_2"),
    setOf("option_c", "optionc", "opt_c", "optc", "choice_c", "choicec", "ans_c", "answer_c", "c", "option_3", "option3", "opt_3", "opt3", "choice_3", "choice3", "ans_3", "answer_3"),
    setOf("option_d", "optiond", "opt_d", "optd", "choice_d", "choiced", "ans_d", "answer_d", "d", "option_4", "option4", "opt_4", "opt4", "choice_4", "choice4", "ans_4", "answer_4"),
    setOf("option_e", "optione", "opt_e", "opte", "choice_e", "choicee", "ans_e", "answer_e", "e", "option_5", "option5", "opt_5", "opt5", "choice_5", "choice5", "ans_5", "answer_5")
  )

  private val MCQ_TYPE_VALUES = setOf(
    "mcq", "mc", "choice", "choices", "multiple_choice", "multiple choice",
    "single_choice", "single choice", "select", "radio", "objective",
    "tf", "t/f", "true_false", "true/false", "true false", "boolean"
  )
  private val WRITTEN_TYPE_VALUES = setOf(
    "written", "essay", "open", "open_ended", "open-ended", "open ended",
    "text", "short", "short_answer", "short answer", "long", "long_answer", "long answer",
    "structured", "subjective", "paragraph", "theory", "descriptive"
  )

  fun inferAnswerFormat(questions: List<DraftQuizQuestion>): ExamAnswerFormat {
    val hasMcq = questions.any { it.questionType == QuestionType.MCQ }
    val hasWritten = questions.any { it.questionType == QuestionType.WRITTEN }
    return when {
      hasMcq && hasWritten -> ExamAnswerFormat.BOTH
      hasWritten -> ExamAnswerFormat.WRITTEN
      else -> ExamAnswerFormat.CHOICES
    }
  }

  fun suggestTitleFromFilename(displayName: String?): String {
    if (displayName.isNullOrBlank()) return ""
    val base = displayName
      .substringAfterLast('/')
      .substringAfterLast('\\')
      .replace(Regex("\\.(csv|tsv|txt|xlsx?)$", RegexOption.IGNORE_CASE), "")
      .replace(Regex("[_\\-]+"), " ")
      .replace(Regex("\\s+"), " ")
      .trim()
    if (base.isBlank()) return ""
    return base.split(' ').joinToString(" ") { word ->
      val lower = word.lowercase(Locale.ROOT)
      when {
        Regex("^[a-z]{2,5}\\d{2,4}$").matches(lower) -> lower.uppercase(Locale.ROOT)
        word.length <= 4 && word.all { it.isUpperCase() || it.isDigit() } -> word
        else -> lower.replaceFirstChar { it.titlecase(Locale.ROOT) }
      }
    }
  }

  fun parse(csvContent: String): CsvParseResult {
    var cleaned = csvContent
      .removePrefix("\uFEFF")
      .replace("\r\n", "\n")
      .replace('\r', '\n')
      .replace('“', '"')
      .replace('”', '"')
      .replace('‘', '\'')
      .replace('’', '\'')

    if (cleaned.isBlank()) {
      return CsvParseResult(error = "The CSV file or text is empty.")
    }

    var forcedDelimiter: Char? = null
    val firstNewline = cleaned.indexOf('\n')
    val firstLineRaw = (if (firstNewline >= 0) cleaned.substring(0, firstNewline) else cleaned).trim()
    if (firstLineRaw.startsWith("sep=", ignoreCase = true) && firstLineRaw.length >= 5) {
      val sepSpec = firstLineRaw.substring(4).trim()
      forcedDelimiter = when {
        sepSpec.equals("\\t", ignoreCase = true) || sepSpec.equals("tab", ignoreCase = true) -> '\t'
        sepSpec.isNotEmpty() -> sepSpec[0]
        else -> ','
      }
      cleaned = if (firstNewline >= 0) cleaned.substring(firstNewline + 1) else ""
    }

    if (cleaned.isBlank()) {
      return CsvParseResult(error = "The CSV file or text has no rows.")
    }

    val delimiter = forcedDelimiter ?: detectDelimiter(cleaned)
    val rows = parseDelimitedRows(StringReader(cleaned), delimiter)
      .map { row -> row.map { it.trim() } }
      .filter { row -> row.any { it.isNotBlank() } }

    if (rows.isEmpty()) {
      return CsvParseResult(error = "The CSV has no non-empty rows.")
    }

    val headerResolution = resolveHeader(rows.first())
    return if (headerResolution.isHeaderRow) {
      parseHeaderedRows(rows.drop(1), headerResolution)
    } else {
      parseHeaderlessRows(rows)
    }
  }

  private data class HeaderResolution(
    val isHeaderRow: Boolean,
    val questionCol: Int = -1,
    val typeCol: Int = -1,
    val optionCols: List<Int> = List(5) { -1 },
    val combinedOptionsCol: Int = -1,
    val correctCol: Int = -1,
    val zeroBasedCorrect: Boolean = false,
    val marksCol: Int = -1,
    val timeCol: Int = -1,
    val explanationCol: Int = -1,
    val modelAnswerCol: Int = -1
  )

  private fun normalizeHeader(raw: String): String {
    return raw.trim()
      .trim('"', '\'', '*', ':', '#')
      .lowercase(Locale.ROOT)
      .replace(Regex("[\\s\\-.]+"), "_")
      .replace(Regex("_+"), "_")
      .trim('_')
  }

  private fun resolveHeader(firstRow: List<String>): HeaderResolution {
    var questionCol = -1
    var typeCol = -1
    val optionCols = MutableList(5) { -1 }
    var combinedOptionsCol = -1
    var correctCol = -1
    var zeroBasedCorrect = false
    var marksCol = -1
    var timeCol = -1
    var explanationCol = -1
    var modelAnswerCol = -1
    var recognizedCount = 0

    firstRow.forEachIndexed { idx, rawCell ->
      val key = normalizeHeader(rawCell)
      if (key.isBlank()) return@forEachIndexed

      when {
        questionCol == -1 && key in QUESTION_ALIASES -> {
          questionCol = idx
          recognizedCount++
        }
        typeCol == -1 && key in TYPE_ALIASES -> {
          typeCol = idx
          recognizedCount++
        }
        combinedOptionsCol == -1 && key in COMBINED_OPTIONS_ALIASES -> {
          combinedOptionsCol = idx
          recognizedCount++
        }
        correctCol == -1 && key in CORRECT_ALIASES -> {
          correctCol = idx
          zeroBasedCorrect = key in ZERO_BASED_CORRECT_ALIASES
          recognizedCount++
        }
        marksCol == -1 && key in MARKS_ALIASES -> {
          marksCol = idx
          recognizedCount++
        }
        timeCol == -1 && key in TIME_ALIASES -> {
          timeCol = idx
          recognizedCount++
        }
        explanationCol == -1 && key in EXPLANATION_ALIASES -> {
          explanationCol = idx
          recognizedCount++
        }
        modelAnswerCol == -1 && key in MODEL_ANSWER_ALIASES -> {
          modelAnswerCol = idx
          recognizedCount++
        }
        else -> {
          val slotIdx = OPTION_SLOT_ALIASES.indexOfFirst { aliases -> key in aliases }
          if (slotIdx >= 0 && optionCols[slotIdx] == -1) {
            optionCols[slotIdx] = idx
            recognizedCount++
          }
        }
      }
    }

    val isHeader = questionCol != -1 || recognizedCount >= 2
    return HeaderResolution(
      isHeaderRow = isHeader,
      questionCol = questionCol,
      typeCol = typeCol,
      optionCols = optionCols,
      combinedOptionsCol = combinedOptionsCol,
      correctCol = correctCol,
      zeroBasedCorrect = zeroBasedCorrect,
      marksCol = marksCol,
      timeCol = timeCol,
      explanationCol = explanationCol,
      modelAnswerCol = modelAnswerCol
    )
  }

  private fun parseHeaderedRows(
    dataRows: List<List<String>>,
    header: HeaderResolution
  ): CsvParseResult {
    if (header.questionCol == -1) {
      return CsvParseResult(
        error = "Missing 'question' (or 'prompt') column in header. Put questions in a 'question' column or remove the header row."
      )
    }
    if (dataRows.isEmpty()) {
      return CsvParseResult(error = "Add at least 1 question row below the CSV header.")
    }
    if (dataRows.size > 200) {
      return CsvParseResult(error = "CSV exceeds the maximum limit of 200 questions (${dataRows.size} found).")
    }

    val parsed = mutableListOf<DraftQuizQuestion>()
    val warnings = mutableListOf<String>()

    dataRows.forEachIndexed { idx, row ->
      val rowNum = idx + 2
      fun cell(col: Int): String = if (col >= 0) row.getOrNull(col)?.trim().orEmpty() else ""

      val questionText = cell(header.questionCol)
      if (questionText.isBlank()) {
        return CsvParseResult(error = "Row $rowNum: Question text is required.")
      }

      val rawType = cell(header.typeCol).lowercase(Locale.ROOT).trim()
      val rawCorrect = cell(header.correctCol)
      val explanation = cell(header.explanationCol)
      val explicitModelAnswer = cell(header.modelAnswerCol)

      // Gather options either from option_a..option_e columns or single combined column
      val extractedSlots = header.optionCols.map { cell(it) }.toMutableList()
      var starCorrectSlot = -1
      extractedSlots.forEachIndexed { slot, value ->
        val (cleanVal, hasStar) = stripCorrectStarMarker(value)
        extractedSlots[slot] = cleanVal
        if (hasStar && starCorrectSlot == -1) starCorrectSlot = slot
      }

      var hasAnyExplicitOption = extractedSlots.any { it.isNotBlank() }
      if (!hasAnyExplicitOption && header.combinedOptionsCol >= 0) {
        val combinedRaw = cell(header.combinedOptionsCol)
        if (combinedRaw.isNotBlank()) {
          val splitOpts = splitCombinedOptions(combinedRaw).take(5)
          splitOpts.forEachIndexed { slot, opt ->
            val (cleanVal, hasStar) = stripCorrectStarMarker(opt)
            extractedSlots[slot] = cleanVal
            if (hasStar && starCorrectSlot == -1) starCorrectSlot = slot
          }
          hasAnyExplicitOption = extractedSlots.any { it.isNotBlank() }
        }
      }

      val isBooleanAnswer = rawCorrect.lowercase(Locale.ROOT) in setOf("true", "false", "t", "f")
      val isTfType = rawType in setOf("tf", "t/f", "true_false", "true/false", "true false", "boolean")

      val qType = when {
        rawType in MCQ_TYPE_VALUES -> QuestionType.MCQ
        rawType in WRITTEN_TYPE_VALUES -> QuestionType.WRITTEN
        rawType.isBlank() -> {
          val nonBlankCount = extractedSlots.count { it.isNotBlank() }
          if (nonBlankCount >= 2 || isBooleanAnswer) QuestionType.MCQ else QuestionType.WRITTEN
        }
        else -> return CsvParseResult(
          error = "Row $rowNum: Unknown type '$rawType'. Use 'mcq' or 'written'."
        )
      }

      val marksResult = parseMarksValue(cell(header.marksCol), qType, rowNum)
      if (marksResult.error != null) {
        return CsvParseResult(error = marksResult.error)
      }
      val marks = marksResult.value

      val rawTime = cell(header.timeCol)
      val qTimeSecs = if (rawTime.isBlank()) {
        null
      } else {
        com.example.util.DurationHelper.parseDurationExpressionToSeconds(rawTime)
          ?.takeIf { it >= 5L }
          ?: return CsvParseResult(error = "Row $rowNum: Invalid time '$rawTime'. Use e.g. 45s, 2m, 1h (min 5s).")
      }

      if (qType == QuestionType.MCQ) {
        if (!hasAnyExplicitOption && (isTfType || isBooleanAnswer)) {
          extractedSlots[0] = "True"
          extractedSlots[1] = "False"
        }

        val nonBlankCount = extractedSlots.count { it.isNotBlank() }
        if (nonBlankCount < 2) {
          return CsvParseResult(error = "Row $rowNum: MCQ requires at least 2 non-empty options (A-E).")
        }

        val slotIdx = resolveCorrectSlot(
          rawCorrect = rawCorrect,
          slots = extractedSlots,
          starCorrectSlot = starCorrectSlot,
          zeroBasedIndex = header.zeroBasedCorrect
        )

        val finalSlot = if (slotIdx == -1 && rawCorrect.isBlank()) {
          val firstNonBlank = extractedSlots.indexOfFirst { it.isNotBlank() }
          warnings.add("Row $rowNum: No correct answer marked; defaulted to Option A.")
          firstNonBlank
        } else {
          slotIdx
        }

        if (finalSlot !in extractedSlots.indices || extractedSlots[finalSlot].isBlank()) {
          return CsvParseResult(
            error = "Row $rowNum: Correct answer '$rawCorrect' must match option A-E, 1-5, or option text."
          )
        }

        val compactedOptions = mutableListOf<String>()
        var compactedCorrect = 0
        for (slot in extractedSlots.indices) {
          val opt = extractedSlots[slot]
          if (opt.isNotBlank()) {
            if (slot == finalSlot) {
              compactedCorrect = compactedOptions.size
            }
            compactedOptions.add(opt)
          }
        }

        if (explicitModelAnswer.isNotBlank()) {
          warnings.add("Row $rowNum: 'model_answer' is ignored for MCQ questions.")
        }

        parsed.add(
          DraftQuizQuestion(
            questionType = QuestionType.MCQ,
            question = questionText,
            options = compactedOptions,
            correctIndex = compactedCorrect,
            marks = marks,
            explanation = explanation,
            modelAnswer = "",
            timeLimitSeconds = qTimeSecs
          )
        )
      } else {
        val finalModelAnswer = explicitModelAnswer.ifBlank {
          // If teacher had no model_answer column and put the guide in 'answer'/'correct' column for a written question
          if (rawCorrect.isNotBlank() && rawType in WRITTEN_TYPE_VALUES || rawType.isBlank()) rawCorrect else ""
        }
        parsed.add(
          DraftQuizQuestion(
            questionType = QuestionType.WRITTEN,
            question = questionText,
            options = emptyList(),
            correctIndex = 0,
            marks = marks,
            explanation = explanation,
            modelAnswer = finalModelAnswer,
            timeLimitSeconds = qTimeSecs
          )
        )
      }
    }

    return CsvParseResult(questions = parsed, warnings = warnings)
  }

  private fun parseHeaderlessRows(rows: List<List<String>>): CsvParseResult {
    if (rows.size > 200) {
      return CsvParseResult(error = "CSV exceeds the maximum limit of 200 questions (${rows.size} found).")
    }

    val parsed = mutableListOf<DraftQuizQuestion>()
    val warnings = mutableListOf<String>()

    rows.forEachIndexed { idx, rawRow ->
      val rowNum = idx + 1
      var cells = rawRow.toMutableList()

      // Drop leading numeric question index like "1", "1.", "Q1" when at least 2 cells remain
      if (cells.size >= 3 && Regex("^(?:q|no\\.?)?\\s*\\d{1,3}[.)]?$", RegexOption.IGNORE_CASE).matches(cells.first())) {
        cells.removeAt(0)
      }

      var explicitType: QuestionType? = null
      val firstLower = cells.firstOrNull()?.lowercase(Locale.ROOT)?.trim().orEmpty()
      if (cells.size >= 2 && (firstLower in MCQ_TYPE_VALUES || firstLower in WRITTEN_TYPE_VALUES)) {
        explicitType = if (firstLower in WRITTEN_TYPE_VALUES) QuestionType.WRITTEN else QuestionType.MCQ
        cells.removeAt(0)
      }

      var questionText = cells.firstOrNull()?.trim().orEmpty()
      questionText = questionText.replace(Regex("^(?:q\\s*)?\\d{1,3}[.)]\\s+", RegexOption.IGNORE_CASE), "").trim()
      if (questionText.isBlank()) {
        return CsvParseResult(error = "Row $rowNum: Question text is required.")
      }

      val tail = cells.drop(1).dropLastWhile { it.isBlank() }.toMutableList()
      if (explicitType == QuestionType.WRITTEN || (explicitType == null && tail.size <= 2 && !looksLikeBoolean(tail.lastOrNull()))) {
        // Check if headerless written row: e.g. [question, marks] or [question, marks, model_answer] or [question, model_answer]
        var marks = 5.0
        var modelAnswer = ""
        if (tail.size == 1) {
          val m = parseOptionalMarks(tail[0])
          if (m != null) marks = m else modelAnswer = tail[0]
        } else if (tail.size >= 2) {
          val m0 = parseOptionalMarks(tail[0])
          val m1 = parseOptionalMarks(tail[1])
          when {
            m0 != null && m1 == null -> {
              marks = m0
              modelAnswer = tail[1]
            }
            m1 != null -> {
              marks = m1
              modelAnswer = tail[0]
            }
            else -> modelAnswer = tail.joinToString(" — ") { it }.trim()
          }
        }
        parsed.add(
          DraftQuizQuestion(
            questionType = QuestionType.WRITTEN,
            question = questionText,
            marks = marks,
            modelAnswer = modelAnswer
          )
        )
        return@forEachIndexed
      }

      // Headerless MCQ / True-False row
      var marks = 1.0
      if (tail.size >= 4) {
        val maybeMarks = parseOptionalMarks(tail.last())
        val prev = tail[tail.size - 2]
        if (maybeMarks != null && looksLikeAnswerSpecifier(prev, tail.dropLast(2))) {
          marks = maybeMarks
          tail.removeAt(tail.lastIndex)
        }
      }

      // Handle 1-cell True/False e.g. ["Earth is flat?", "False"]
      if (tail.size == 1 && looksLikeBoolean(tail[0])) {
        val correctIdx = if (tail[0].lowercase(Locale.ROOT) in setOf("true", "t")) 0 else 1
        parsed.add(
          DraftQuizQuestion(
            questionType = QuestionType.MCQ,
            question = questionText,
            options = listOf("True", "False"),
            correctIndex = correctIdx,
            marks = 1.0
          )
        )
        return@forEachIndexed
      }

      var rawCorrect = ""
      var optionCells = tail.toMutableList()
      if (optionCells.size >= 3 && looksLikeAnswerSpecifier(optionCells.last(), optionCells.dropLast(1))) {
        rawCorrect = optionCells.removeAt(optionCells.lastIndex)
      }

      var starCorrectSlot = -1
      val cleanedOptions = mutableListOf<String>()
      optionCells.take(5).forEach { opt ->
        val (cleanVal, hasStar) = stripCorrectStarMarker(opt)
        if (cleanVal.isNotBlank()) {
          if (hasStar && starCorrectSlot == -1) starCorrectSlot = cleanedOptions.size
          cleanedOptions.add(cleanVal)
        }
      }

      if (cleanedOptions.size < 2) {
        return CsvParseResult(error = "Row $rowNum: Need at least 2 options for a multiple-choice question.")
      }

      var correctIdx = resolveCorrectSlot(
        rawCorrect = rawCorrect,
        slots = cleanedOptions,
        starCorrectSlot = starCorrectSlot,
        zeroBasedIndex = false
      )
      if (correctIdx !in cleanedOptions.indices) {
        correctIdx = 0
        warnings.add("Row $rowNum: No correct answer column detected; defaulted to Option A.")
      }

      parsed.add(
        DraftQuizQuestion(
          questionType = QuestionType.MCQ,
          question = questionText,
          options = cleanedOptions,
          correctIndex = correctIdx,
          marks = marks
        )
      )
    }

    return CsvParseResult(questions = parsed, warnings = warnings)
  }

  private data class MarksParseResult(val value: Double, val error: String? = null)

  private fun parseMarksValue(raw: String, qType: QuestionType, rowNum: Int): MarksParseResult {
    if (raw.isBlank()) {
      return MarksParseResult(if (qType == QuestionType.WRITTEN) 5.0 else 1.0)
    }
    val cleaned = raw.trim()
      .replace(Regex("\\s*(marks?|pts?|points?)\\s*$", RegexOption.IGNORE_CASE), "")
      .replace(",", ".")
    val num = cleaned.toDoubleOrNull()
    if (num == null || num < 0.5 || num > 100.0) {
      return MarksParseResult(1.0, error = "Row $rowNum: Marks must be between 0.5 and 100 (got '$raw').")
    }
    return MarksParseResult(num)
  }

  private fun parseOptionalMarks(raw: String): Double? {
    val cleaned = raw.trim()
      .replace(Regex("\\s*(marks?|pts?|points?)\\s*$", RegexOption.IGNORE_CASE), "")
      .replace(",", ".")
    val num = cleaned.toDoubleOrNull() ?: return null
    return if (num in 0.5..100.0) num else null
  }

  private fun stripCorrectStarMarker(raw: String): Pair<String, Boolean> {
    val trimmed = raw.trim()
    return when {
      trimmed.startsWith("*") -> trimmed.removePrefix("*").trim() to true
      trimmed.startsWith("[*]") -> trimmed.removePrefix("[*]").trim() to true
      trimmed.endsWith("*") && !trimmed.contains("**") -> trimmed.removeSuffix("*").trim() to true
      trimmed.startsWith("✓") -> trimmed.removePrefix("✓").trim() to true
      else -> trimmed to false
    }
  }

  private fun splitCombinedOptions(raw: String): List<String> {
    val sep = when {
      raw.contains('|') -> "|"
      raw.contains(";;") -> ";;"
      raw.contains('\n') -> "\n"
      raw.contains(';') -> ";"
      raw.contains("//") -> "//"
      else -> "|"
    }
    return raw.split(sep).map { it.trim() }.filter { it.isNotBlank() }
  }

  private fun looksLikeBoolean(raw: String?): Boolean {
    val s = raw?.trim()?.lowercase(Locale.ROOT).orEmpty()
    return s in setOf("true", "false", "t", "f")
  }

  private fun looksLikeAnswerSpecifier(candidate: String, precedingOptions: List<String>): Boolean {
    val cleaned = cleanCorrectSpecifier(candidate)
    if (cleaned.length == 1 && cleaned[0] in 'A'..'E') return true
    val num = cleaned.toIntOrNull()
    if (num != null && num in 1..precedingOptions.size.coerceAtLeast(5) && precedingOptions.any { it.toDoubleOrNull() == null }) {
      return true
    }
    if (looksLikeBoolean(cleaned)) return true
    val nonBlankOpts = precedingOptions.filter { it.isNotBlank() }
    return nonBlankOpts.any { it.trim().equals(candidate.trim(), ignoreCase = true) }
  }

  private fun cleanCorrectSpecifier(raw: String): String {
    return raw.trim()
      .replace(Regex("^(option|choice|ans|answer)\\s+", RegexOption.IGNORE_CASE), "")
      .trim('(', ')', '[', ']', '.', ':', ' ', '"', '\'')
      .uppercase(Locale.ROOT)
  }

  private fun resolveCorrectSlot(
    rawCorrect: String,
    slots: List<String>,
    starCorrectSlot: Int,
    zeroBasedIndex: Boolean
  ): Int {
    if (rawCorrect.isBlank()) {
      return starCorrectSlot
    }

    val cleanedUpper = cleanCorrectSpecifier(rawCorrect)
    when (cleanedUpper) {
      "A" -> return 0
      "B" -> return 1
      "C" -> return 2
      "D" -> return 3
      "E" -> return 4
    }

    val num = cleanedUpper.toIntOrNull()
    if (num != null) {
      val idx = if (zeroBasedIndex || num == 0) num else num - 1
      if (idx in slots.indices && slots[idx].isNotBlank()) return idx
    }

    if (cleanedUpper in setOf("TRUE", "T", "FALSE", "F")) {
      val target = if (cleanedUpper.startsWith("T")) "true" else "false"
      val boolMatch = slots.indexOfFirst { it.trim().equals(target, ignoreCase = true) }
      if (boolMatch >= 0) return boolMatch
    }

    val rawTrimmed = rawCorrect.trim()
    val exactTextMatch = slots.indexOfFirst {
      it.isNotBlank() && it.trim().equals(rawTrimmed, ignoreCase = true)
    }
    if (exactTextMatch >= 0) return exactTextMatch

    return starCorrectSlot
  }

  private fun detectDelimiter(text: String): Char {
    val sampleLines = text.lineSequence()
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .take(4)
      .toList()
    if (sampleLines.isEmpty()) return ','

    val candidates = listOf(',', ';', '\t', '|')
    fun countUnquoted(line: String, delim: Char): Int {
      var inQuotes = false
      var count = 0
      var i = 0
      while (i < line.length) {
        val c = line[i]
        if (c == '"') {
          if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
            i++
          } else {
            inQuotes = !inQuotes
          }
        } else if (!inQuotes && c == delim) {
          count++
        }
        i++
      }
      return count
    }

    val firstCounts = candidates.associateWith { d -> countUnquoted(sampleLines[0], d) }
    val secondCounts = if (sampleLines.size > 1) {
      candidates.associateWith { d -> countUnquoted(sampleLines[1], d) }
    } else {
      firstCounts
    }

    // Prefer tab or semicolon if comma is absent OR if tab/semicolon consistently splits more columns cleanly
    val commaScore = firstCounts[','] ?: 0
    val semiScore = firstCounts[';'] ?: 0
    val tabScore = firstCounts['\t'] ?: 0
    val pipeScore = firstCounts['|'] ?: 0

    return when {
      tabScore >= 1 && (commaScore == 0 || (tabScore >= commaScore && (secondCounts['\t'] ?: 0) == tabScore)) -> '\t'
      semiScore >= 1 && (commaScore == 0 || (semiScore >= commaScore && (secondCounts[';'] ?: 0) == semiScore)) -> ';'
      pipeScore >= 2 && commaScore == 0 -> '|'
      commaScore >= 1 -> ','
      semiScore >= 1 -> ';'
      tabScore >= 1 -> '\t'
      pipeScore >= 1 -> '|'
      else -> ','
    }
  }

  /**
   * Standard RFC-4180 style parser parameterized by delimiter, handling quoted delimiters,
   * newlines inside quotes, and escaped double quotes ("").
   */
  private fun parseDelimitedRows(reader: Reader, delimiter: Char): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var currentRow = mutableListOf<String>()
    val currentField = StringBuilder()
    var inQuotes = false
    var chInt = reader.read()

    while (chInt != -1) {
      val ch = chInt.toChar()
      if (inQuotes) {
        if (ch == '"') {
          reader.mark(1)
          val next = reader.read()
          if (next == '"'.code) {
            currentField.append('"')
          } else {
            inQuotes = false
            if (next != -1) reader.reset()
          }
        } else {
          currentField.append(ch)
        }
      } else {
        when (ch) {
          '"' -> {
            if (currentField.isEmpty()) {
              inQuotes = true
            } else {
              currentField.append(ch)
            }
          }
          delimiter -> {
            currentRow.add(currentField.toString())
            currentField.setLength(0)
          }
          '\r' -> {
            reader.mark(1)
            val next = reader.read()
            if (next != '\n'.code && next != -1) reader.reset()
            currentRow.add(currentField.toString())
            currentField.setLength(0)
            rows.add(currentRow)
            currentRow = mutableListOf()
          }
          '\n' -> {
            currentRow.add(currentField.toString())
            currentField.setLength(0)
            rows.add(currentRow)
            currentRow = mutableListOf()
          }
          else -> currentField.append(ch)
        }
      }
      chInt = reader.read()
    }

    if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
      currentRow.add(currentField.toString())
      rows.add(currentRow)
    }
    return rows
  }
}
