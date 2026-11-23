package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ChallengeItem
import com.example.data.CsvParseResult
import com.example.data.DraftQuizQuestion
import com.example.data.ExamAnswerFormat
import com.example.data.ExamCsvCodec
import com.example.data.QuestionType
import com.example.ui.components.RoyalCardBorderBrush
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

@Composable
fun AddQuestionsDialog(
  quiz: ChallengeItem,
  isLoading: Boolean,
  errorMessage: String?,
  onDismiss: () -> Unit,
  onSubmit: (List<DraftQuizQuestion>) -> Unit
) {
  val context = LocalContext.current
  val defaultType = if (quiz.isExam && quiz.answerFormat == ExamAnswerFormat.WRITTEN) {
    QuestionType.WRITTEN
  } else {
    QuestionType.MCQ
  }
  val questions = remember(quiz.quizId) {
    mutableStateListOf(
      DraftQuizQuestion(
        questionType = defaultType,
        options = if (defaultType == QuestionType.MCQ) listOf("", "", "", "") else emptyList(),
        marks = if (defaultType == QuestionType.WRITTEN) 5.0 else 1.0
      )
    )
  }
  var expandedIndices by remember(quiz.quizId) { mutableStateOf(setOf(0)) }
  var showPastePanel by remember { mutableStateOf(false) }
  var pastedCsvText by remember { mutableStateOf("") }
  var localError by remember { mutableStateOf<String?>(null) }
  var csvBanner by remember { mutableStateOf<String?>(null) }
  var csvWarnings by remember { mutableStateOf<List<String>>(emptyList()) }

  fun applyCsvToExistingQuiz(parsed: CsvParseResult) {
    if (parsed.error != null) {
      localError = parsed.error
      csvBanner = null
      csvWarnings = emptyList()
      return
    }
    val hasBlankStarterOnly = questions.size == 1 && questions[0].question.isBlank()
    if (hasBlankStarterOnly) {
      questions.clear()
    }
    val remainingCapacity = (200 - questions.size).coerceAtLeast(0)
    val toAdd = parsed.questions.take(remainingCapacity)
    questions.addAll(toAdd)
    expandedIndices = emptySet()
    localError = if (parsed.questions.size > remainingCapacity) {
      "Loaded first $remainingCapacity questions (200 question batch limit)."
    } else null
    csvWarnings = parsed.warnings
    showPastePanel = false
    csvBanner = "Loaded ${toAdd.size} question(s) — tap Add below!"
  }

  val csvImportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    runCatching {
      context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
    }.onSuccess { text ->
      applyCsvToExistingQuiz(ExamCsvCodec.parse(text))
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
      csvBanner = "CSV template saved!"
    }.onFailure { err ->
      localError = err.localizedMessage ?: "Failed to save CSV template."
    }
  }

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
        .testTag("add_questions_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 740.dp)
      ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
          Text("Add Questions", style = SectionHeadingStyle)
          Text(quiz.title, style = CaptionStyle.copy(color = ChampagneGold))
        }
        HorizontalDivider(color = DarkBorder)

        Column(
          modifier = Modifier
            .weight(1f, fill = false)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
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
            onLoadPastedCsv = { applyCsvToExistingQuiz(parseCsvTextSafely(pastedCsvText)) }
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

          questions.forEachIndexed { idx, draftQ ->
            val onRemoveAtIdx = {
              questions.removeAt(idx)
              expandedIndices = expandedIndices.mapNotNull { old ->
                when {
                  old == idx -> null
                  old > idx -> old - 1
                  else -> old
                }
              }.toSet()
              localError = null
            }
            if (idx in expandedIndices) {
              QuestionEditorCard(
                index = idx,
                draft = draftQ,
                allowWrittenType = quiz.isExam && quiz.answerFormat != ExamAnswerFormat.CHOICES,
                canRemoveQuestion = questions.size > 1,
                onUpdate = { updated ->
                  questions[idx] = updated
                  localError = null
                },
                onRemove = onRemoveAtIdx,
                onCollapse = { expandedIndices = expandedIndices - idx }
              )
            } else {
              CompactQuestionPreviewRow(
                index = idx,
                draft = draftQ,
                canRemove = questions.size > 1,
                onExpand = { expandedIndices = expandedIndices + idx },
                onRemove = onRemoveAtIdx
              )
            }
          }

          OutlinedButton(
            onClick = {
              val newIdx = questions.size
              questions.add(
                DraftQuizQuestion(
                  questionType = defaultType,
                  options = if (defaultType == QuestionType.MCQ) listOf("", "", "", "") else emptyList(),
                  marks = if (defaultType == QuestionType.WRITTEN) 5.0 else 1.0
                )
              )
              expandedIndices = expandedIndices + newIdx
            },
            shape = RoundedCornerShape(ButtonRadius),
            border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Another Question", style = ButtonTextStyle.copy(color = ChampagneGold))
          }
        }

        HorizontalDivider(color = DarkBorder)
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(
            onClick = onDismiss,
            enabled = !isLoading,
            shape = RoundedCornerShape(ButtonRadius),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
          ) {
            Text("Cancel", style = ButtonTextStyle.copy(color = Lavender))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = {
              val err = validateDraftQuestionsList(questions, quiz.isExam, quiz.answerFormat)
              if (err != null) {
                localError = err
                return@Button
              }
              onSubmit(questions.toList())
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(ButtonRadius),
            colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("submit_add_questions_btn")
          ) {
            if (isLoading) {
              CircularProgressIndicator(color = Midnight, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
              Text("Add (${questions.size})", style = ButtonTextStyle.copy(color = Midnight))
            }
          }
        }
      }
    }
  }
}
