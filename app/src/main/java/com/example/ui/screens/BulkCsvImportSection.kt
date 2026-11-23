package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CsvParseResult
import com.example.data.DraftQuizQuestion
import com.example.data.ExamCsvCodec
import com.example.data.QuestionType
import com.example.data.formatMarks
import com.example.ui.components.royalOutlinedTextFieldColors
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.RoyalSurface
import com.example.ui.theme.SilverWhite
import com.example.ui.theme.SuccessColor

@Composable
fun BulkCsvImportCard(
  showPastePanel: Boolean,
  pastedCsvText: String,
  onTogglePastePanel: () -> Unit,
  onPastedTextChange: (String) -> Unit,
  onPickCsvClick: () -> Unit,
  onSaveTemplateClick: () -> Unit,
  onLoadPastedCsv: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(RoyalSurface)
      .border(1.dp, ElectricViolet.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          "Quick Bulk Import (Any CSV / Text)",
          style = FieldLabelStyle.copy(color = ChampagneGold, fontWeight = FontWeight.Bold)
        )
        Text(
          "Comma, semicolon, or tab files — auto-fills questions ready to publish",
          style = CaptionStyle.copy(fontSize = 12.sp, color = Lavender)
        )
      }
      TextButton(
        onClick = onSaveTemplateClick,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("save_csv_template_btn")
      ) {
        Icon(Icons.Default.Download, contentDescription = "Download template", tint = ChampagneGold, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Template", style = ButtonTextStyle.copy(fontSize = 12.sp, color = ChampagneGold))
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = onPickCsvClick,
        shape = RoundedCornerShape(ButtonRadius),
        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, contentColor = SilverWhite),
        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("import_csv_btn")
      ) {
        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Upload CSV", style = ButtonTextStyle.copy(fontSize = 13.sp, color = SilverWhite))
      }

      OutlinedButton(
        onClick = onTogglePastePanel,
        shape = RoundedCornerShape(ButtonRadius),
        border = BorderStroke(1.dp, if (showPastePanel) ChampagneGold else DarkBorder),
        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("paste_csv_toggle_btn")
      ) {
        Icon(
          Icons.Default.ContentPaste,
          contentDescription = null,
          tint = if (showPastePanel) ChampagneGold else Lavender,
          modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          if (showPastePanel) "Hide Paste" else "Paste CSV",
          style = ButtonTextStyle.copy(fontSize = 13.sp, color = if (showPastePanel) ChampagneGold else SilverWhite)
        )
      }
    }

    AnimatedVisibility(visible = showPastePanel) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = pastedCsvText,
          onValueChange = onPastedTextChange,
          label = { Text("Paste CSV / Excel / Sheets rows") },
          placeholder = {
            Text(
              "Question,Option A,Option B,Option C,Option D,Correct\nWhat is 2+2?,3,4,5,6,B",
              fontSize = 12.sp
            )
          },
          colors = royalOutlinedTextFieldColors(),
          minLines = 3,
          maxLines = 6,
          modifier = Modifier.fillMaxWidth().testTag("paste_csv_input")
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = { onPastedTextChange(ExamCsvCodec.SAMPLE_CSV_TEMPLATE) },
            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
          ) {
            Text("Insert Sample", style = ButtonTextStyle.copy(fontSize = 12.sp, color = Lavender))
          }
          Spacer(modifier = Modifier.weight(1f))
          Button(
            onClick = onLoadPastedCsv,
            enabled = pastedCsvText.isNotBlank(),
            shape = RoundedCornerShape(ButtonRadius),
            colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("load_pasted_csv_btn")
          ) {
            Text("Load Questions", style = ButtonTextStyle.copy(fontSize = 13.sp, color = Midnight))
          }
        }
      }
    }
  }
}

@Composable
fun ReadyQuestionsSummaryBanner(
  questions: List<DraftQuizQuestion>,
  warnings: List<String>,
  importBanner: String?,
  allExpanded: Boolean,
  onToggleExpandAll: () -> Unit
) {
  val readyCount = questions.count { it.question.isNotBlank() }
  if (readyCount == 0 && importBanner.isNullOrBlank()) return

  val mcqCount = questions.count { it.question.isNotBlank() && it.questionType == QuestionType.MCQ }
  val writtenCount = questions.count { it.question.isNotBlank() && it.questionType == QuestionType.WRITTEN }
  val totalMarks = questions.filter { it.question.isNotBlank() }.sumOf { it.marks }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(SuccessColor.copy(alpha = 0.12f))
      .border(1.dp, SuccessColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
      .padding(12.dp)
      .testTag("ready_summary_banner"),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(18.dp))
        Column {
          Text(
            text = importBanner ?: "Ready to Publish: $readyCount Question(s)",
            style = FieldLabelStyle.copy(color = SuccessColor, fontWeight = FontWeight.Bold)
          )
          Text(
            text = buildString {
              if (mcqCount > 0) append("$mcqCount MCQ")
              if (mcqCount > 0 && writtenCount > 0) append(" • ")
              if (writtenCount > 0) append("$writtenCount Written")
              if (totalMarks > 0) append(" • ${formatMarks(totalMarks)} total marks")
            },
            style = CaptionStyle.copy(color = SilverWhite, fontSize = 12.sp)
          )
        }
      }
      if (questions.isNotEmpty()) {
        TextButton(
          onClick = onToggleExpandAll,
          modifier = Modifier.defaultMinSize(minHeight = 48.dp)
        ) {
          Text(
            if (allExpanded) "Collapse" else "Edit All",
            style = ButtonTextStyle.copy(fontSize = 12.sp, color = ChampagneGold)
          )
        }
      }
    }

    if (warnings.isNotEmpty()) {
      Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 4.dp)
      ) {
        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(15.dp))
        Text(
          text = warnings.take(3).joinToString(" | ") +
            if (warnings.size > 3) " (+${warnings.size - 3} more notes)" else "",
          style = CaptionStyle.copy(color = ChampagneGold, fontSize = 12.sp)
        )
      }
    }
  }
}

@Composable
fun CompactQuestionPreviewRow(
  index: Int,
  draft: DraftQuizQuestion,
  canRemove: Boolean,
  onExpand: () -> Unit,
  onRemove: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(RoyalSurface)
      .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
      .clickable(onClick = onExpand)
      .padding(horizontal = 12.dp, vertical = 10.dp)
      .testTag("question_preview_row_$index"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = "Q${index + 1}",
          style = FieldLabelStyle.copy(color = ChampagneGold, fontWeight = FontWeight.Bold)
        )
        Text(
          text = if (draft.questionType == QuestionType.WRITTEN) "WRITTEN" else "MCQ",
          style = CaptionStyle.copy(
            fontSize = 11.sp,
            color = if (draft.questionType == QuestionType.WRITTEN) Lavender else SuccessColor,
            fontWeight = FontWeight.SemiBold
          )
        )
        Text(
          text = "• ${formatMarks(draft.marks)} mk${if (draft.marks == 1.0) "" else "s"}",
          style = CaptionStyle.copy(fontSize = 12.sp, color = Lavender)
        )
      }
      Text(
        text = draft.question.ifBlank { "(Tap to enter question text)" },
        style = BodyStyle.copy(
          fontSize = 14.sp,
          color = if (draft.question.isBlank()) Lavender else SilverWhite
        ),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      if (draft.questionType == QuestionType.MCQ && draft.options.isNotEmpty()) {
        val correctLetter = ('A' + draft.correctIndex.coerceIn(0, draft.options.lastIndex))
        val correctText = draft.options.getOrNull(draft.correctIndex).orEmpty()
        Text(
          text = "✓ $correctLetter: $correctText (${draft.options.size} choices)",
          style = CaptionStyle.copy(fontSize = 12.sp, color = SuccessColor),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      } else if (draft.questionType == QuestionType.WRITTEN && draft.modelAnswer.isNotBlank()) {
        Text(
          text = "Guide: ${draft.modelAnswer}",
          style = CaptionStyle.copy(fontSize = 12.sp, color = ChampagneGold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    IconButton(onClick = onExpand, modifier = Modifier.size(48.dp)) {
      Icon(Icons.Default.ExpandMore, contentDescription = "Expand question ${index + 1}", tint = ChampagneGold)
    }
    if (canRemove) {
      IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove question ${index + 1}", tint = ErrorColor)
      }
    }
  }
}

fun resolveDisplayName(context: Context, uri: Uri): String? {
  return runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
      if (cursor.moveToFirst()) {
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0) cursor.getString(idx) else null
      } else null
    }
  }.getOrNull() ?: uri.lastPathSegment
}

fun parseCsvTextSafely(raw: String): CsvParseResult = ExamCsvCodec.parse(raw)
