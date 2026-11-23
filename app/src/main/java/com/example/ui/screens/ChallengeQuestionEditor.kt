package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DraftQuizQuestion
import com.example.data.ExamAnswerFormat
import com.example.data.QuestionType
import com.example.data.formatMarks
import com.example.ui.components.royalOutlinedTextFieldColors
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.RoyalSurface
import com.example.ui.theme.SuccessColor

@Composable
fun QuestionEditorCard(
  index: Int,
  draft: DraftQuizQuestion,
  allowWrittenType: Boolean,
  canRemoveQuestion: Boolean,
  onUpdate: (DraftQuizQuestion) -> Unit,
  onRemove: () -> Unit,
  onCollapse: (() -> Unit)? = null
) {
  var marksText by remember(index, draft.questionType, draft.marks) {
    mutableStateOf(formatMarks(draft.marks))
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(RoyalSurface)
      .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Question ${index + 1}", style = FieldLabelStyle.copy(color = ChampagneGold))
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (allowWrittenType) {
          QuestionTypeChip(
            label = "MCQ",
            selected = draft.questionType == QuestionType.MCQ,
            onClick = {
              if (draft.questionType != QuestionType.MCQ) {
                val defaultOpts = if (draft.options.size >= 2) draft.options else listOf("", "", "", "")
                onUpdate(draft.copy(questionType = QuestionType.MCQ, options = defaultOpts, marks = 1.0))
              }
            }
          )
          QuestionTypeChip(
            label = "Written",
            selected = draft.questionType == QuestionType.WRITTEN,
            onClick = {
              if (draft.questionType != QuestionType.WRITTEN) {
                onUpdate(draft.copy(questionType = QuestionType.WRITTEN, marks = 5.0))
              }
            }
          )
        }
        if (onCollapse != null) {
          IconButton(onClick = onCollapse, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.ExpandLess, contentDescription = "Collapse question ${index + 1}", tint = Lavender)
          }
        }
        if (canRemoveQuestion) {
          IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove question", tint = ErrorColor)
          }
        }
      }
    }

    OutlinedTextField(
      value = draft.question,
      onValueChange = { onUpdate(draft.copy(question = it)) },
      label = { Text("Question Prompt *") },
      colors = royalOutlinedTextFieldColors(),
      modifier = Modifier.fillMaxWidth().testTag("challenge_q_prompt_$index"),
      maxLines = 4
    )

    OutlinedTextField(
      value = marksText,
      onValueChange = { raw ->
        val filtered = raw.filter { it.isDigit() || it == '.' }
        marksText = filtered
        val parsed = filtered.toDoubleOrNull()
        if (parsed != null && parsed >= 0.5 && parsed <= 100.0) {
          onUpdate(draft.copy(marks = parsed))
        }
      },
      label = { Text("Marks (0.5 - 100)") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      colors = royalOutlinedTextFieldColors(),
      modifier = Modifier.fillMaxWidth().testTag("challenge_q_marks_$index"),
      singleLine = true
    )

    if (draft.questionType == QuestionType.MCQ) {
      Text("Tap circle to mark the correct answer:", style = CaptionStyle)

      draft.options.forEachIndexed { optIdx, optText ->
        val isCorrect = draft.correctIndex == optIdx
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = { onUpdate(draft.copy(correctIndex = optIdx)) },
            modifier = Modifier.size(48.dp).testTag("challenge_q_${index}_correct_$optIdx")
          ) {
            Icon(
              imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
              contentDescription = "Mark option ${'A' + optIdx} correct",
              tint = if (isCorrect) SuccessColor else Lavender
            )
          }
          OutlinedTextField(
            value = optText,
            onValueChange = { updatedText ->
              val next = draft.options.toMutableList()
              next[optIdx] = updatedText
              onUpdate(draft.copy(options = next))
            },
            label = { Text("Option ${'A' + optIdx}") },
            colors = royalOutlinedTextFieldColors(),
            modifier = Modifier.weight(1f).testTag("challenge_q_${index}_opt_$optIdx"),
            singleLine = true
          )
          if (draft.options.size > 2) {
            IconButton(
              onClick = {
                val next = draft.options.toMutableList()
                next.removeAt(optIdx)
                val nextCorrect = when {
                  draft.correctIndex == optIdx -> 0
                  draft.correctIndex > optIdx -> draft.correctIndex - 1
                  else -> draft.correctIndex
                }.coerceIn(0, next.lastIndex)
                onUpdate(draft.copy(options = next, correctIndex = nextCorrect))
              },
              modifier = Modifier.size(48.dp)
            ) {
              Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove option", tint = ErrorColor, modifier = Modifier.size(18.dp))
            }
          }
        }
      }

      if (draft.options.size < 5) {
        OutlinedButton(
          onClick = { onUpdate(draft.copy(options = draft.options + "")) },
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, DarkBorder),
          modifier = Modifier.defaultMinSize(minHeight = 48.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Option (${draft.options.size}/5)", style = ButtonTextStyle.copy(fontSize = 14.sp, color = ChampagneGold))
        }
      }
    } else {
      OutlinedTextField(
        value = draft.modelAnswer,
        onValueChange = { onUpdate(draft.copy(modelAnswer = it)) },
        label = { Text("Marking Guide / Model Answer (for marker only)") },
        placeholder = { Text("Key points expected from the student...") },
        colors = royalOutlinedTextFieldColors(),
        modifier = Modifier.fillMaxWidth().testTag("challenge_q_model_$index"),
        maxLines = 4
      )
    }

    OutlinedTextField(
      value = draft.explanation,
      onValueChange = { onUpdate(draft.copy(explanation = it)) },
      label = { Text("Explanation (optional)") },
      placeholder = { Text("Shown after student submits / exam closes...") },
      colors = royalOutlinedTextFieldColors(),
      modifier = Modifier.fillMaxWidth(),
      maxLines = 2
    )

    DurationAmountUnitPicker(
      title = "Question Timer Override (optional)",
      subtitle = "Overrides default per-question countdown for Q${index + 1}",
      totalSeconds = draft.timeLimitSeconds,
      allowOff = true,
      tagPrefix = "q_${index}_timer",
      onSecondsChange = { secs -> onUpdate(draft.copy(timeLimitSeconds = secs)) }
    )
  }
}

@Composable
private fun QuestionTypeChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .defaultMinSize(minHeight = 48.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(if (selected) MetallicGold else RoyalSurface)
      .border(1.dp, if (selected) ChampagneGold else DarkBorder, RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      style = FieldLabelStyle.copy(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (selected) Midnight else Lavender
      )
    )
  }
}

fun validateDraftQuestionsList(
  questions: List<DraftQuizQuestion>,
  asExam: Boolean,
  answerFormat: ExamAnswerFormat
): String? {
  if (questions.isEmpty() || questions.size > 200) {
    return "Must have between 1 and 200 questions."
  }
  if (!asExam && answerFormat != ExamAnswerFormat.CHOICES) {
    return "Written questions are only allowed in Exams."
  }
  questions.forEachIndexed { idx, q ->
    if (q.question.isBlank()) return "Question ${idx + 1} needs question text."
    if (q.marks < 0.5 || q.marks > 100.0) {
      return "Question ${idx + 1}: marks must be between 0.5 and 100."
    }
    if (q.timeLimitSeconds != null && q.timeLimitSeconds < 5L) {
      return "Question ${idx + 1}: timer must be at least 5 seconds."
    }
    if (q.questionType == QuestionType.WRITTEN && !asExam) {
      return "Question ${idx + 1}: switch mode to Exam for written questions."
    }
    if (q.questionType == QuestionType.WRITTEN && answerFormat == ExamAnswerFormat.CHOICES) {
      return "Question ${idx + 1}: change Answer Format to Written or Both."
    }
    if (q.questionType == QuestionType.MCQ && answerFormat == ExamAnswerFormat.WRITTEN) {
      return "Question ${idx + 1}: change Answer Format to Choices or Both."
    }
    if (q.questionType == QuestionType.MCQ) {
      val nonEmpty = q.options.map { it.trim() }.filter { it.isNotBlank() }
      if (nonEmpty.size < 2) return "Question ${idx + 1} must have at least 2 options."
      if (q.correctIndex !in q.options.indices || q.options[q.correctIndex].isBlank()) {
        return "Question ${idx + 1}: select a non-empty option as the correct answer."
      }
    }
  }
  return null
}
