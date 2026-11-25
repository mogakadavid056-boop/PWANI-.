package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CountdownMode
import com.example.data.CourseUnit
import com.example.data.CsvParseResult
import com.example.data.DraftQuizQuestion
import com.example.data.ExamAnswerFormat
import com.example.data.ExamCsvCodec
import com.example.data.ExamExitAction
import com.example.data.ExamSecurityConfig
import com.example.data.QuestionType
import com.example.data.formatMarks
import com.example.ui.components.RoyalCardBorderBrush
import com.example.ui.components.royalOutlinedTextFieldColors
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardRadius
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SectionHeadingStyle
import kotlinx.coroutines.launch

@Composable
fun CreateChallengeDialog(
  approvedUnits: List<CourseUnit>,
  canCreateExam: Boolean = false,
  initialDraft: com.example.assistant.ChallengeDraftPrefill? = null,
  isLoading: Boolean,
  errorMessage: String?,
  onDismiss: () -> Unit,
  onCreate: (
    title: String,
    description: String,
    opensAtIso: String,
    closesAtIso: String,
    timeLimitMinutes: Int?,
    questions: List<DraftQuizQuestion>,
    courseId: String?,
    asExam: Boolean,
    shuffleQuestions: Boolean,
    answerFormat: ExamAnswerFormat,
    securityConfig: ExamSecurityConfig
  ) -> Unit
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()
  val scope = rememberCoroutineScope()

  var title by remember(initialDraft) { mutableStateOf(initialDraft?.title.orEmpty()) }
  var description by remember(initialDraft) { mutableStateOf(initialDraft?.description.orEmpty()) }
  var selectedUnit by remember { mutableStateOf<CourseUnit?>(null) }

  var asExam by remember { mutableStateOf(false) }
  var shuffleQuestions by remember { mutableStateOf(false) }
  var answerFormat by remember { mutableStateOf(ExamAnswerFormat.CHOICES) }
  var showOptionsPanel by remember { mutableStateOf(false) }
  var showSecurityPanel by remember { mutableStateOf(false) }

  val openPresets = listOf("Opens Now", "In 1 hour", "Tomorrow 8 AM", "Custom")
  var selectedOpenPreset by remember { mutableStateOf("Opens Now") }
  var customOpenDelaySeconds by remember { mutableStateOf<Long?>(1800L) }

  val closePresets = listOf("6 hours", "24 hours", "3 days", "7 days", "Custom")
  var selectedClosePreset by remember { mutableStateOf("24 hours") }
  var customCloseWindowSeconds by remember { mutableStateOf<Long?>(3600L) }

  var securityConfig by remember {
    mutableStateOf(
      ExamSecurityConfig(
        timeLimitSeconds = 600L,
        onExitAction = ExamExitAction.WARN,
        maxExitViolations = 2,
        strictDeadline = true
      )
    )
  }

  val questions = remember(initialDraft) {
    mutableStateListOf<DraftQuizQuestion>().apply {
      val prefilled = initialDraft?.questions.orEmpty()
      if (prefilled.isNotEmpty()) {
        addAll(prefilled)
      } else {
        add(DraftQuizQuestion(question = "", options = listOf("", "", "", ""), correctIndex = 0, explanation = ""))
      }
    }
  }
  var expandedIndices by remember(initialDraft) { mutableStateOf(setOf(0)) }

  var showPastePanel by remember { mutableStateOf(false) }
  var pastedCsvText by remember { mutableStateOf("") }
  var localError by remember { mutableStateOf<String?>(null) }
  var csvBanner by remember { mutableStateOf<String?>(null) }
  var csvWarnings by remember { mutableStateOf<List<String>>(emptyList()) }

  fun applyExamDefaults() {
    val curLimit = securityConfig.timeLimitSeconds ?: 3600L
    securityConfig = securityConfig.copy(
      timeLimitSeconds = curLimit,
      oneWayNavigation = true,
      noCopyPaste = true,
      onExitAction = ExamExitAction.WARN,
      maxExitViolations = 2,
      strictDeadline = true
    )
    showSecurityPanel = true
  }

  fun applyCsvResult(parsed: CsvParseResult, sourceFilename: String?) {
    if (parsed.error != null) {
      localError = parsed.error
      csvBanner = null
      csvWarnings = emptyList()
      return
    }
    questions.clear()
    questions.addAll(parsed.questions)
    expandedIndices = emptySet()
    localError = null
    csvWarnings = parsed.warnings
    showPastePanel = false

    val suggestedTitle = ExamCsvCodec.suggestTitleFromFilename(sourceFilename)
    if (title.isBlank() && suggestedTitle.isNotBlank()) {
      title = suggestedTitle
    }

    val inferred = ExamCsvCodec.inferAnswerFormat(parsed.questions)
    if (inferred != ExamAnswerFormat.CHOICES) {
      if (canCreateExam) {
        asExam = true
        answerFormat = inferred
        applyExamDefaults()
      } else {
        localError = "Only lecturers & admins can create exams with written questions."
      }
    } else {
      answerFormat = ExamAnswerFormat.CHOICES
      if (canCreateExam && !asExam && suggestedTitle.contains(Regex("\\b(exam|midterm|final|cat|test)\\b", RegexOption.IGNORE_CASE))) {
        asExam = true
        applyExamDefaults()
      }
    }

    if (parsed.questions.any { it.timeLimitSeconds != null } && securityConfig.countdownMode == CountdownMode.OVERALL) {
      securityConfig = securityConfig.copy(
        countdownMode = if (securityConfig.timeLimitSeconds != null) CountdownMode.BOTH else CountdownMode.PER_QUESTION
      )
    }

    csvBanner = "Imported ${parsed.questions.size} question(s) — ready to publish!"
  }

  val csvImportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    runCatching {
      val name = resolveDisplayName(context, uri)
      val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
      name to text
    }.onSuccess { (name, text) ->
      applyCsvResult(ExamCsvCodec.parse(text), name)
    }.onFailure { err ->
      localError = err.localizedMessage ?: "Failed to read CSV file."
    }
  }

  val saveTemplateLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("text/csv")
  ) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    runCatching {
      context.contentResolver.openOutputStream(uri)?.use { out ->
        out.write(ExamCsvCodec.SAMPLE_CSV_TEMPLATE.toByteArray(Charsets.UTF_8))
      }
    }.onSuccess {
      localError = null
      csvBanner = "CSV template saved!"
    }.onFailure { err ->
      localError = err.localizedMessage ?: "Failed to save CSV template."
    }
  }

  val readyCount = questions.count { it.question.isNotBlank() }
  val totalMarks = questions.filter { it.question.isNotBlank() }.sumOf { it.marks }

  Dialog(
    onDismissRequest = { if (!isLoading) onDismiss() },
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(CardRadius),
      color = RoyalSurfaceHigh,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 18.dp)
        .border(1.dp, RoyalCardBorderBrush, RoundedCornerShape(CardRadius))
        .testTag("create_challenge_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 760.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(if (asExam) "Create Exam / CAT" else "Create Challenge", style = SectionHeadingStyle)
          if (readyCount > 0) {
            Text(
              text = "$readyCount Qs • ${formatMarks(totalMarks)} mks",
              style = CaptionStyle.copy(color = ChampagneGold, fontWeight = FontWeight.SemiBold)
            )
          }
        }
        HorizontalDivider(color = DarkBorder)

        Column(
          modifier = Modifier
            .weight(1f, fill = false)
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 14.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          val shownErr = localError ?: errorMessage
          if (!shownErr.isNullOrBlank()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ErrorColor.copy(alpha = 0.14f))
                .border(1.dp, ErrorColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(10.dp)
            ) {
              Text(shownErr, style = CaptionStyle.copy(color = ErrorColor))
            }
          }

          BulkCsvImportCard(
            showPastePanel = showPastePanel,
            pastedCsvText = pastedCsvText,
            onTogglePastePanel = { showPastePanel = !showPastePanel },
            onPastedTextChange = { pastedCsvText = it; localError = null },
            onPickCsvClick = {
              csvImportLauncher.launch(
                arrayOf("text/csv", "text/comma-separated-values", "text/tab-separated-values", "text/plain", "*/*")
              )
            },
            onSaveTemplateClick = { saveTemplateLauncher.launch("exam_template_v2.csv") },
            onLoadPastedCsv = { applyCsvResult(parseCsvTextSafely(pastedCsvText), null) }
          )

          if (canCreateExam) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
              RoyalPresetPill(
                label = "Challenge",
                selected = !asExam,
                onClick = {
                  asExam = false
                  answerFormat = ExamAnswerFormat.CHOICES
                  securityConfig = securityConfig.copy(oneWayNavigation = false, noCopyPaste = false)
                },
                modifier = Modifier.weight(1f).testTag("mode_challenge_pill")
              )
              RoyalPresetPill(
                label = "Exam / CAT",
                selected = asExam,
                onClick = {
                  asExam = true
                  applyExamDefaults()
                  answerFormat = ExamCsvCodec.inferAnswerFormat(questions)
                },
                modifier = Modifier.weight(1f).testTag("mode_exam_pill")
              )
            }
          }

          if (asExam) {
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              ExamAnswerFormat.entries.forEach { fmt ->
                RoyalPresetPill(
                  label = fmt.label,
                  selected = answerFormat == fmt,
                  onClick = { answerFormat = fmt },
                  modifier = Modifier.testTag("format_${fmt.apiValue}_pill")
                )
              }
            }
          }

          OutlinedTextField(
            value = title,
            onValueChange = { title = it; localError = null },
            label = { Text(if (asExam) "Exam / CAT Title *" else "Challenge Title *") },
            placeholder = { Text(if (asExam) "e.g. CAT 1 • Midterm Exam" else "e.g. Week 4 Algorithms Sprint") },
            colors = royalOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("challenge_title_input"),
            singleLine = true
          )

          ScheduleOptionsSection(
            expanded = showOptionsPanel,
            onToggleExpanded = { showOptionsPanel = !showOptionsPanel },
            description = description,
            onDescriptionChange = { description = it; localError = null },
            shuffleQuestions = shuffleQuestions,
            onShuffleChange = { shuffleQuestions = it },
            approvedUnits = approvedUnits,
            selectedUnit = selectedUnit,
            onSelectUnit = { selectedUnit = it },
            asExam = asExam,
            openPresets = openPresets,
            selectedOpenPreset = selectedOpenPreset,
            onSelectOpenPreset = { selectedOpenPreset = it },
            customOpenDelaySeconds = customOpenDelaySeconds,
            onCustomOpenDelayChange = { customOpenDelaySeconds = it },
            closePresets = closePresets,
            selectedClosePreset = selectedClosePreset,
            onSelectClosePreset = { selectedClosePreset = it },
            customCloseWindowSeconds = customCloseWindowSeconds,
            onCustomCloseWindowChange = { customCloseWindowSeconds = it },
            timeLimitSeconds = securityConfig.timeLimitSeconds,
            onSelectTimeLimitSeconds = { secs -> securityConfig = securityConfig.copy(timeLimitSeconds = secs) }
          )

          ExamSecuritySettingsCard(
            expanded = showSecurityPanel,
            onToggleExpanded = { showSecurityPanel = !showSecurityPanel },
            asExam = asExam,
            securityConfig = securityConfig,
            onUpdateConfig = { updated -> securityConfig = updated }
          )

          ReadyQuestionsSummaryBanner(
            questions = questions,
            warnings = csvWarnings,
            importBanner = csvBanner,
            allExpanded = expandedIndices.size == questions.size && questions.isNotEmpty(),
            onToggleExpandAll = {
              expandedIndices = if (expandedIndices.size == questions.size) emptySet() else questions.indices.toSet()
            }
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Questions (${questions.size}/200)", style = SectionHeadingStyle.copy(fontSize = 18.sp))
            if (questions.size < 200) {
              OutlinedButton(
                onClick = {
                  val defaultType = if (asExam && answerFormat == ExamAnswerFormat.WRITTEN) {
                    QuestionType.WRITTEN
                  } else {
                    QuestionType.MCQ
                  }
                  val newIdx = questions.size
                  questions.add(
                    DraftQuizQuestion(
                      questionType = defaultType,
                      question = "",
                      options = if (defaultType == QuestionType.MCQ) listOf("", "", "", "") else emptyList(),
                      correctIndex = 0,
                      marks = if (defaultType == QuestionType.WRITTEN) 5.0 else 1.0
                    )
                  )
                  expandedIndices = expandedIndices + newIdx
                  localError = null
                },
                shape = RoundedCornerShape(ButtonRadius),
                border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.65f)),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("add_challenge_question_btn")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Question", style = ButtonTextStyle.copy(fontSize = 14.sp, color = ChampagneGold))
              }
            }
          }

          questions.forEachIndexed { qIdx, draftQ ->
            val onRemoveAtIdx = {
              questions.removeAt(qIdx)
              expandedIndices = expandedIndices.mapNotNull { old ->
                when {
                  old == qIdx -> null
                  old > qIdx -> old - 1
                  else -> old
                }
              }.toSet()
              localError = null
            }
            if (qIdx in expandedIndices) {
              QuestionEditorCard(
                index = qIdx,
                draft = draftQ,
                allowWrittenType = asExam && answerFormat != ExamAnswerFormat.CHOICES,
                canRemoveQuestion = questions.size > 1,
                onUpdate = { updated ->
                  questions[qIdx] = updated
                  localError = null
                },
                onRemove = onRemoveAtIdx,
                onCollapse = { expandedIndices = expandedIndices - qIdx }
              )
            } else {
              CompactQuestionPreviewRow(
                index = qIdx,
                draft = draftQ,
                canRemove = questions.size > 1,
                onExpand = { expandedIndices = expandedIndices + qIdx },
                onRemove = onRemoveAtIdx
              )
            }
          }
        }

        HorizontalDivider(color = DarkBorder)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onDismiss,
            enabled = !isLoading,
            shape = RoundedCornerShape(ButtonRadius),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("cancel_create_challenge_btn")
          ) {
            Text("Cancel", style = ButtonTextStyle.copy(color = Lavender))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = {
              val cleanTitle = title.trim()
              if (cleanTitle.isBlank()) {
                localError = "Please enter a title above before publishing."
                scope.launch { scrollState.animateScrollTo(0) }
                return@Button
              }
              val hasAnyPerQTimer = securityConfig.perQuestionSeconds != null || questions.any { it.timeLimitSeconds != null }
              if (asExam && securityConfig.timeLimitSeconds == null && !hasAnyPerQTimer && securityConfig.countdownMode != CountdownMode.CLOSING_WINDOW) {
                localError = "Exams require a time limit, per-question timer, or closing window countdown."
                return@Button
              }
              val overallSecs = securityConfig.timeLimitSeconds
              if (overallSecs != null && (overallSecs < 30L || overallSecs > 86_400L)) {
                localError = "Overall time limit must be between 30 seconds and 24 hours."
                return@Button
              }
              val perQSecs = securityConfig.perQuestionSeconds
              if (perQSecs != null && (perQSecs < 5L || perQSecs > 3600L)) {
                localError = "Per-question timer must be between 5 seconds and 60 minutes."
                return@Button
              }
              if (selectedClosePreset == "Custom" && (customCloseWindowSeconds ?: 0L) < 60L) {
                localError = "Availability window must be at least 1 minute."
                return@Button
              }
              val validationErr = validateDraftQuestionsList(questions, asExam, answerFormat)
              if (validationErr != null) {
                localError = validationErr
                return@Button
              }
              val (opensAt, closesAt) = computeChallengeOpenCloseIso(
                openPreset = selectedOpenPreset,
                closePreset = selectedClosePreset,
                customOpenDelaySeconds = customOpenDelaySeconds,
                customCloseWindowSeconds = customCloseWindowSeconds
              )
              val legacyMinutes = overallSecs?.let { kotlin.math.ceil(it / 60.0).toInt().coerceIn(1, 1440) }
              onCreate(
                cleanTitle,
                description.trim(),
                opensAt,
                closesAt,
                legacyMinutes,
                questions.toList(),
                selectedUnit?.courseId,
                asExam,
                shuffleQuestions,
                answerFormat,
                securityConfig
              )
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(ButtonRadius),
            colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("submit_create_challenge_btn")
          ) {
            if (isLoading) {
              CircularProgressIndicator(color = Midnight, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
              val label = if (readyCount > 0) "Publish ($readyCount)" else "Publish"
              Text(label, style = ButtonTextStyle.copy(color = Midnight))
            }
          }
        }
      }
    }
  }
}
