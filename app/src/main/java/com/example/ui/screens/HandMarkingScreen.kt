package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExamPaperDetail
import com.example.data.InkStroke
import com.example.data.MarkingDraftData
import com.example.data.MarkingStamp
import com.example.data.MarkingStatus
import com.example.data.QuestionMarkInput
import com.example.data.QuestionMarkingDraft
import com.example.data.QuestionType
import com.example.data.formatMarks
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.FriendlyErrorSnackbar
import com.example.ui.components.MetallicGoldText
import com.example.ui.components.RoyalCard
import com.example.ui.components.royalOutlinedTextFieldColors
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardTitleStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.ScreenTitleStyle
import com.example.ui.theme.SilverWhite
import com.example.ui.theme.SuccessColor

@Composable
fun HandMarkingScreen(
  paper: ExamPaperDetail,
  draft: MarkingDraftData,
  questionMarksMap: Map<Int, QuestionMarkInput>,
  overallComment: String,
  isSavingDraft: Boolean,
  isReleasing: Boolean,
  isLoadingPdf: Boolean,
  errorMessage: String?,
  successMessage: String?,
  paperPosition: Int = 0,
  totalPapers: Int = 0,
  hasPrevPaper: Boolean = false,
  hasNextPaper: Boolean = false,
  onPrevPaper: () -> Unit = {},
  onNextPaper: () -> Unit = {},
  onAddStroke: (ord: Int, InkStroke) -> Unit,
  onAddStamp: (ord: Int, MarkingStamp) -> Unit,
  onUndoLast: (ord: Int) -> Unit,
  onClearQuestionInk: (ord: Int) -> Unit,
  onUpdateMark: (ord: Int, Double?) -> Unit,
  onUpdateComment: (ord: Int, String) -> Unit,
  onUpdateOverallComment: (String) -> Unit,
  onSaveDraft: () -> Unit,
  onReleasePaper: () -> Unit,
  onReopenPaper: () -> Unit,
  onViewReleasedPdf: () -> Unit,
  onDismissError: () -> Unit,
  onDismissSuccess: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  val isReleased = paper.attempt.status == MarkingStatus.RELEASED
  var activeTool by remember { mutableStateOf(MarkingTool.PEN) }
  var selectedColorHex by remember { mutableLongStateOf(0xFFFF6B6BL) }
  var showReleaseConfirm by remember { mutableStateOf(false) }

  val writtenQuestions = paper.questions.filter { it.questionType == QuestionType.WRITTEN }
  val mcqScore = paper.attempt.mcqMarks ?: paper.questions
    .filter { it.questionType == QuestionType.MCQ && it.chosenIndex == it.correctIndex }
    .sumOf { it.marks }
  val writtenTotalAwarded = writtenQuestions.sumOf { q -> questionMarksMap[q.ord]?.marks ?: 0.0 }
  val runningScore = mcqScore + writtenTotalAwarded
  val maxTotal = paper.quiz.totalMarks.takeIf { it > 0.0 } ?: paper.questions.sumOf { it.marks }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("hand_marking_screen"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Top Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("marking_back_btn")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
        }
        Spacer(modifier = Modifier.width(4.dp))
        MetallicGoldText("Marking", style = ScreenTitleStyle)
      }
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (totalPapers > 0 && paperPosition > 0) {
          IconButton(
            onClick = onPrevPaper,
            enabled = hasPrevPaper,
            modifier = Modifier.size(48.dp).testTag("marking_prev_paper_btn")
          ) {
            Icon(
              Icons.AutoMirrored.Filled.NavigateBefore,
              contentDescription = "Previous student paper",
              tint = if (hasPrevPaper) ChampagneGold else Lavender.copy(alpha = 0.35f)
            )
          }
          Text(
            text = "$paperPosition/$totalPapers",
            style = CaptionStyle.copy(color = ChampagneGold, fontWeight = FontWeight.SemiBold)
          )
          IconButton(
            onClick = onNextPaper,
            enabled = hasNextPaper,
            modifier = Modifier.size(48.dp).testTag("marking_next_paper_btn")
          ) {
            Icon(
              Icons.AutoMirrored.Filled.NavigateNext,
              contentDescription = "Next student paper",
              tint = if (hasNextPaper) ChampagneGold else Lavender.copy(alpha = 0.35f)
            )
          }
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(RoyalSurfaceHigh)
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = "v${paper.attempt.draftVersion} • ${paper.attempt.status.label}",
            style = CaptionStyle.copy(color = ChampagneGold)
          )
        }
      }
    }

    // Tool Palette Bar (Scroll/Draw/Tick/Cross + Ink Swatches)
    if (!isReleased) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(RoyalSurfaceHigh)
          .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
          .padding(horizontal = 8.dp, vertical = 6.dp)
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        ToolChip(Icons.Default.PanTool, "Scroll", activeTool == MarkingTool.PAN) { activeTool = MarkingTool.PAN }
        ToolChip(Icons.Default.Draw, "Pen", activeTool == MarkingTool.PEN) { activeTool = MarkingTool.PEN }
        ToolChip(Icons.Default.Check, "Tick", activeTool == MarkingTool.TICK) { activeTool = MarkingTool.TICK }
        ToolChip(Icons.Default.Close, "Cross", activeTool == MarkingTool.CROSS) { activeTool = MarkingTool.CROSS }

        Spacer(modifier = Modifier.width(6.dp))
        listOf(
          0xFFFF6B6BL to ErrorColor,
          0xFF3DDC84L to SuccessColor,
          0xFFD4AF37L to MetallicGold,
          0xFF8B24FFL to ElectricViolet
        ).forEach { (hex, col) ->
          val isChosen = selectedColorHex == hex
          Box(
            modifier = Modifier
              .size(30.dp)
              .clip(CircleShape)
              .background(col)
              .border(if (isChosen) 2.5.dp else 1.dp, if (isChosen) SilverWhite else DarkBorder, CircleShape)
              .clickable { selectedColorHex = hex; activeTool = MarkingTool.PEN }
          )
        }
      }
    }

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      if (!errorMessage.isNullOrBlank()) {
        FriendlyErrorSnackbar(errorMessage = errorMessage, onDismiss = onDismissError)
      }
      if (!successMessage.isNullOrBlank()) {
        RoyalCard(borderColor = SuccessColor.copy(alpha = 0.6f), backgroundColor = RoyalSurfaceHigh) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(successMessage, style = BodyStyle.copy(color = SuccessColor), modifier = Modifier.weight(1f))
            IconButton(onClick = onDismissSuccess, modifier = Modifier.size(48.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = SuccessColor)
            }
          }
        }
      }

      RoyalCard(isHero = true) {
        Text(paper.student.fullName, style = CardTitleStyle)
        Text("${paper.student.admissionNumber} • ${paper.quiz.title}", style = CaptionStyle)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text(
            "MCQ: ${formatMarks(mcqScore)}  |  Written: ${formatMarks(writtenTotalAwarded)}",
            style = CaptionStyle.copy(color = Lavender)
          )
          Text(
            text = "${formatMarks(runningScore)} / ${formatMarks(maxTotal)}",
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = ChampagneGold
          )
        }
      }

      paper.questions.forEach { q ->
        if (q.questionType == QuestionType.MCQ) {
          val ok = q.chosenIndex >= 0 && q.chosenIndex == q.correctIndex
          RoyalCard(borderColor = if (ok) SuccessColor.copy(alpha = 0.45f) else ErrorColor.copy(alpha = 0.45f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Q${q.ord}. Multiple Choice", style = FieldLabelStyle.copy(color = ChampagneGold))
              Text(
                "${formatMarks(if (ok) q.marks else 0.0)} / ${formatMarks(q.marks)}",
                style = FieldLabelStyle.copy(color = if (ok) SuccessColor else ErrorColor)
              )
            }
            Text(q.question, style = BodyStyle)
            val chosenL = if (q.chosenIndex in q.options.indices) ('A' + q.chosenIndex).toString() else "Skipped"
            val correctL = if (q.correctIndex != null && q.correctIndex in q.options.indices) ('A' + q.correctIndex).toString() else "-"
            Text("Student chose: $chosenL  •  Correct: $correctL", style = CaptionStyle)
          }
        } else {
          WrittenQuestionMarkingCard(
            ord = q.ord,
            questionText = q.question,
            maxMarks = q.marks,
            studentAnswer = q.writtenAnswer,
            modelAnswer = q.modelAnswer,
            draft = draft.questions[q.ord] ?: QuestionMarkingDraft(),
            markInput = questionMarksMap[q.ord] ?: QuestionMarkInput(),
            activeTool = activeTool,
            penColorHex = selectedColorHex,
            isEditable = !isReleased,
            onAddStroke = { s -> onAddStroke(q.ord, s) },
            onAddStamp = { st -> onAddStamp(q.ord, st) },
            onUndo = { onUndoLast(q.ord) },
            onClearInk = { onClearQuestionInk(q.ord) },
            onUpdateMark = { m -> onUpdateMark(q.ord, m) },
            onUpdateComment = { c -> onUpdateComment(q.ord, c) }
          )
        }
      }

      RoyalCard {
        Text("Overall Examiner Feedback", style = FieldLabelStyle)
        OutlinedTextField(
          value = overallComment,
          onValueChange = onUpdateOverallComment,
          enabled = !isReleased,
          placeholder = { Text("Summary remarks for the student...") },
          colors = royalOutlinedTextFieldColors(),
          minLines = 2,
          maxLines = 5,
          modifier = Modifier.fillMaxWidth().testTag("marking_overall_comment_input")
        )
      }

      Spacer(modifier = Modifier.height(8.dp))
    }

    // Action Bar
    if (isReleased) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = onReopenPaper,
          shape = RoundedCornerShape(ButtonRadius),
          border = BorderStroke(1.dp, ChampagneGold),
          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp).testTag("marking_reopen_btn")
        ) {
          Icon(Icons.Default.EditOff, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Reopen Paper", style = ButtonTextStyle.copy(color = ChampagneGold))
        }
        if (paper.attempt.markedPdfPath.isNotBlank()) {
          Button(
            onClick = onViewReleasedPdf,
            enabled = !isLoadingPdf,
            shape = RoundedCornerShape(ButtonRadius),
            colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp).testTag("marking_view_pdf_btn")
          ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Midnight, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isLoadingPdf) "Opening..." else "View / Print PDF", style = ButtonTextStyle.copy(color = Midnight))
          }
        }
      }
    } else {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = onSaveDraft,
          enabled = !isSavingDraft && !isReleasing,
          shape = RoundedCornerShape(ButtonRadius),
          border = BorderStroke(1.dp, ChampagneGold),
          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp).testTag("marking_save_draft_btn")
        ) {
          if (isSavingDraft) {
            CircularProgressIndicator(color = ChampagneGold, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
          } else {
            Icon(Icons.Default.Save, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save Draft", style = ButtonTextStyle.copy(fontSize = 14.sp, color = ChampagneGold))
          }
        }

        Button(
          onClick = { showReleaseConfirm = true },
          enabled = !isReleasing && !isSavingDraft,
          shape = RoundedCornerShape(ButtonRadius),
          colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          modifier = Modifier.weight(1.3f).defaultMinSize(minHeight = 52.dp).testTag("marking_release_btn")
        ) {
          if (isReleasing) {
            CircularProgressIndicator(color = Midnight, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
          } else {
            Icon(Icons.Default.Send, contentDescription = null, tint = Midnight, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Return Paper", style = ButtonTextStyle.copy(fontSize = 14.sp, color = Midnight))
          }
        }
      }
    }
  }

  if (showReleaseConfirm) {
    ConfirmActionDialog(
      title = "Return Marked Paper?",
      message = "A PDF with your ink annotations, marks (${formatMarks(runningScore)}/${formatMarks(maxTotal)}), and feedback will be uploaded and sent to ${paper.student.fullName}.",
      confirmButtonText = "Return & Notify",
      isLoading = isReleasing,
      onConfirm = {
        showReleaseConfirm = false
        onReleasePaper()
      },
      onDismiss = { showReleaseConfirm = false }
    )
  }
}

@Composable
private fun WrittenQuestionMarkingCard(
  ord: Int,
  questionText: String,
  maxMarks: Double,
  studentAnswer: String,
  modelAnswer: String,
  draft: QuestionMarkingDraft,
  markInput: QuestionMarkInput,
  activeTool: MarkingTool,
  penColorHex: Long,
  isEditable: Boolean,
  onAddStroke: (InkStroke) -> Unit,
  onAddStamp: (MarkingStamp) -> Unit,
  onUndo: () -> Unit,
  onClearInk: () -> Unit,
  onUpdateMark: (Double?) -> Unit,
  onUpdateComment: (String) -> Unit
) {
  var showModelAnswer by remember { mutableStateOf(false) }
  var markStr by remember(ord, markInput.marks) {
    mutableStateOf(markInput.marks?.let { formatMarks(it) }.orEmpty())
  }

  RoyalCard(borderColor = ElectricViolet.copy(alpha = 0.55f)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("Question $ord (Max ${formatMarks(maxMarks)} marks)", style = FieldLabelStyle.copy(color = ChampagneGold))
      if (isEditable) {
        Row {
          IconButton(onClick = onUndo, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo stroke", tint = ChampagneGold)
          }
          IconButton(onClick = onClearInk, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear ink", tint = ErrorColor)
          }
        }
      }
    }

    Text(questionText, style = CardTitleStyle.copy(color = SilverWhite))

    if (modelAnswer.isNotBlank()) {
      Text(
        text = if (showModelAnswer) "Hide Marking Guide" else "Show Marking Guide",
        style = CaptionStyle.copy(color = ChampagneGold, fontWeight = FontWeight.SemiBold),
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable { showModelAnswer = !showModelAnswer }
          .padding(vertical = 4.dp)
      )
      AnimatedVisibility(visible = showModelAnswer) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RoyalSurfaceHigh)
            .border(1.dp, ChampagneGold.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(10.dp)
        ) {
          Text("Guide: $modelAnswer", style = CaptionStyle.copy(color = ChampagneGold))
        }
      }
    }

    Text(
      text = if (isEditable && activeTool != MarkingTool.PAN) "Draw or tap ${activeTool.label} on the paper below:" else "Student Answer Sheet:",
      style = CaptionStyle
    )

    InkMarkingAnswerSheet(
      studentAnswer = studentAnswer,
      draft = draft,
      activeTool = activeTool,
      penColorHex = penColorHex,
      isEditable = isEditable,
      onAddStroke = onAddStroke,
      onAddStamp = onAddStamp
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        value = markStr,
        onValueChange = { raw ->
          val cleaned = raw.filter { it.isDigit() || it == '.' }
          markStr = cleaned
          val parsed = cleaned.toDoubleOrNull()
          onUpdateMark(parsed?.coerceIn(0.0, maxMarks))
        },
        enabled = isEditable,
        label = { Text("Marks / ${formatMarks(maxMarks)}") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = royalOutlinedTextFieldColors(),
        singleLine = true,
        modifier = Modifier.weight(1f).testTag("marking_q_mark_$ord")
      )

      if (isEditable) {
        QuickMarkPill("0") { markStr = "0"; onUpdateMark(0.0) }
        QuickMarkPill("Half") {
          val h = (maxMarks / 2.0)
          markStr = formatMarks(h)
          onUpdateMark(h)
        }
        QuickMarkPill("Full") { markStr = formatMarks(maxMarks); onUpdateMark(maxMarks) }
      }
    }

    OutlinedTextField(
      value = markInput.comment,
      onValueChange = onUpdateComment,
      enabled = isEditable,
      label = { Text("Question Feedback (optional)") },
      colors = royalOutlinedTextFieldColors(),
      maxLines = 3,
      modifier = Modifier.fillMaxWidth().testTag("marking_q_comment_$ord")
    )
  }
}

@Composable
private fun ToolChip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .defaultMinSize(minHeight = 40.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(if (selected) MetallicGold else Color.Transparent)
      .border(1.dp, if (selected) ChampagneGold else DarkBorder, RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Icon(icon, contentDescription = label, tint = if (selected) Midnight else ChampagneGold, modifier = Modifier.size(16.dp))
    Text(label, style = FieldLabelStyle.copy(fontSize = 12.sp, color = if (selected) Midnight else SilverWhite))
  }
}

@Composable
private fun QuickMarkPill(label: String, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .defaultMinSize(minHeight = 48.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(RoyalSurfaceHigh)
      .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(label, style = FieldLabelStyle.copy(fontSize = 12.sp, color = ChampagneGold))
  }
}
