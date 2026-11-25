package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChallengeItem
import com.example.data.ExamAnswerFormat
import com.example.data.ExamFlagItem
import com.example.data.MarkingStatus
import com.example.data.QuestionType
import com.example.data.QuizResultRow
import com.example.data.QuizReviewItem
import com.example.data.formatMarks
import com.example.notifications.NotificationTimeFormatter
import com.example.ui.components.EmptyState
import com.example.ui.components.FriendlyErrorSnackbar
import com.example.ui.components.MetallicGoldText
import com.example.ui.components.RoyalCard
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
import com.example.ui.theme.RoyalSurface
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.ScreenTitleStyle
import com.example.ui.theme.SilverWhite
import com.example.ui.theme.SuccessColor
import java.util.Locale

@Composable
fun ChallengeReviewScreen(
  quiz: ChallengeItem,
  items: List<QuizReviewItem>,
  isLoading: Boolean,
  errorMessage: String?,
  isStaffPreview: Boolean,
  isLoadingPdf: Boolean = false,
  onOpenMarkedPaper: () -> Unit = {},
  onDismissError: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 18.dp, vertical = 16.dp)
      .testTag("challenge_review_screen"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("review_back_btn")) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
      }
      Spacer(modifier = Modifier.width(6.dp))
      MetallicGoldText(if (isStaffPreview) "Answer Key" else "Review", style = ScreenTitleStyle)
    }

    RoyalCard(isHero = true) {
      Text(quiz.title, style = CardTitleStyle)
      if (!isStaffPreview && quiz.myScore != null && quiz.myTotal != null) {
        Text(
          text = "Your Score: ${formatMarks(quiz.myScore)} / ${formatMarks(quiz.myTotal)}",
          style = FieldLabelStyle.copy(color = ChampagneGold)
        )
      }
      if (!isStaffPreview && quiz.myMarkingStatus == MarkingStatus.RELEASED) {
        Spacer(modifier = Modifier.height(6.dp))
        Button(
          onClick = onOpenMarkedPaper,
          enabled = !isLoadingPdf,
          shape = RoundedCornerShape(ButtonRadius),
          colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
          modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("review_open_marked_pdf_btn")
        ) {
          Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Midnight, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (isLoadingPdf) "Opening PDF..." else "View & Print Marked Paper", style = ButtonTextStyle.copy(color = Midnight))
        }
      }
    }

    if (!errorMessage.isNullOrBlank()) {
      FriendlyErrorSnackbar(errorMessage = errorMessage, onDismiss = onDismissError)
    }

    if (isLoading) {
      Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MetallicGold)
      }
    } else if (items.isEmpty()) {
      EmptyState(
        icon = Icons.Default.Visibility,
        title = "No Questions Found",
        message = "This assessment does not have reviewable questions yet."
      )
    } else {
      items.forEach { q ->
        val isWritten = q.questionType == QuestionType.WRITTEN
        val isCorrect = !isWritten && q.chosenIndex == q.correctIndex
        val wasSkipped = !isWritten && q.chosenIndex < 0
        RoyalCard(
          borderColor = when {
            isStaffPreview -> null
            isWritten && q.awardedMarks != null -> ChampagneGold.copy(alpha = 0.65f)
            isWritten -> ElectricViolet.copy(alpha = 0.5f)
            isCorrect -> SuccessColor.copy(alpha = 0.6f)
            else -> ErrorColor.copy(alpha = 0.6f)
          }
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "Question ${q.ord} (${formatMarks(q.marks)} marks)",
              style = FieldLabelStyle.copy(color = ChampagneGold)
            )
            if (!isStaffPreview) {
              if (isWritten) {
                val markLabel = if (q.awardedMarks != null) {
                  "${formatMarks(q.awardedMarks)} / ${formatMarks(q.marks)}"
                } else {
                  "Pending marking"
                }
                Text(
                  text = markLabel,
                  style = FieldLabelStyle.copy(color = if (q.awardedMarks != null) SuccessColor else ChampagneGold)
                )
              } else {
                Text(
                  text = when {
                    isCorrect -> "Correct (+${formatMarks(q.marks)})"
                    wasSkipped -> "Skipped"
                    else -> "Incorrect"
                  },
                  style = FieldLabelStyle.copy(color = if (isCorrect) SuccessColor else ErrorColor)
                )
              }
            }
          }

          Text(q.question, style = CardTitleStyle.copy(color = SilverWhite))

          if (isWritten) {
            if (!isStaffPreview) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(RoyalSurface)
                  .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                  .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text("Your Answer:", style = CaptionStyle.copy(color = ChampagneGold))
                Text(
                  text = q.writtenAnswer.ifBlank { "(No answer submitted)" },
                  style = BodyStyle.copy(color = SilverWhite)
                )
              }
            }
            if (q.markerComment.isNotBlank()) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(RoyalSurfaceHigh)
                  .border(1.dp, ElectricViolet.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                  .padding(10.dp),
                verticalAlignment = Alignment.Top
              ) {
                Icon(Icons.Default.Draw, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Marker: ${q.markerComment}", style = BodyStyle.copy(fontSize = 14.sp, color = Lavender))
              }
            }
          } else {
            q.options.forEachIndexed { idx, opt ->
              val optCorrect = idx == q.correctIndex
              val optChosen = idx == q.chosenIndex
              val optBorder = when {
                optCorrect -> SuccessColor
                optChosen && !optCorrect -> ErrorColor
                else -> DarkBorder
              }
              val optBg = when {
                optCorrect -> SuccessColor.copy(alpha = 0.14f)
                optChosen && !optCorrect -> ErrorColor.copy(alpha = 0.14f)
                else -> RoyalSurface
              }
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(optBg)
                  .border(1.dp, optBorder, RoundedCornerShape(10.dp))
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (optCorrect) {
                  Icon(Icons.Default.CheckCircle, contentDescription = "Correct option", tint = SuccessColor, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                } else if (optChosen) {
                  Icon(Icons.Default.Cancel, contentDescription = "Your wrong choice", tint = ErrorColor, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                }
                val letter = ('A' + idx).toString()
                Text(
                  text = "$letter. $opt",
                  style = BodyStyle.copy(color = if (optCorrect) SuccessColor else SilverWhite)
                )
              }
            }
          }

          if (q.explanation.isNotBlank()) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(RoyalSurfaceHigh)
                .border(1.dp, ChampagneGold.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .padding(10.dp),
              verticalAlignment = Alignment.Top
            ) {
              Icon(Icons.Default.Info, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(q.explanation, style = CaptionStyle.copy(color = ChampagneGold))
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

@Composable
fun ChallengeResultsScreen(
  quiz: ChallengeItem,
  rows: List<QuizResultRow>,
  examFlags: List<ExamFlagItem> = emptyList(),
  isLoading: Boolean,
  errorMessage: String?,
  canMarkPapers: Boolean = false,
  canExportRankedPdf: Boolean = canMarkPapers,
  isLoadingPdf: Boolean = false,
  onOpenAnswerKey: () -> Unit,
  onOpenMarking: () -> Unit = {},
  onExportRankedPdf: () -> Unit = {},
  onDismissError: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 18.dp, vertical = 16.dp)
      .testTag("challenge_results_screen"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("results_back_btn")) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
      }
      Spacer(modifier = Modifier.width(6.dp))
      MetallicGoldText("Scores", style = ScreenTitleStyle)
    }

    RoyalCard(isHero = true) {
      Text(quiz.title, style = CardTitleStyle)
      val releasedRows = rows.mapNotNull { r -> r.score?.let { it to r.total } }
      val avg = if (releasedRows.isNotEmpty()) {
        val avgVal = releasedRows.map { it.first }.average()
        val totalQ = releasedRows.first().second.coerceAtLeast(1.0)
        String.format(Locale.US, "Avg: %.1f / %s", avgVal, formatMarks(totalQ))
      } else {
        "${quiz.questionCount} questions"
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("${rows.size} submissions", style = FieldLabelStyle.copy(color = ChampagneGold))
        Text(avg, style = FieldLabelStyle.copy(color = Lavender))
      }
      Spacer(modifier = Modifier.height(6.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = onOpenAnswerKey,
          shape = RoundedCornerShape(ButtonRadius),
          border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.65f)),
          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("results_answer_key_btn")
        ) {
          Icon(Icons.Default.Visibility, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Answer Key", style = ButtonTextStyle.copy(fontSize = 14.sp, color = ChampagneGold))
        }
        if (canMarkPapers && quiz.isExam && quiz.answerFormat != ExamAnswerFormat.CHOICES) {
          Button(
            onClick = onOpenMarking,
            shape = RoundedCornerShape(ButtonRadius),
            colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("results_mark_papers_btn")
          ) {
            Icon(Icons.Default.Draw, contentDescription = null, tint = Midnight, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Mark Papers", style = ButtonTextStyle.copy(fontSize = 14.sp, color = Midnight))
          }
        }
      }

      if (canExportRankedPdf && rows.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = onExportRankedPdf,
          enabled = !isLoadingPdf,
          shape = RoundedCornerShape(ButtonRadius),
          colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, contentColor = SilverWhite),
          modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("results_ranked_pdf_btn")
        ) {
          Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = SilverWhite, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (isLoadingPdf) "Generating Ranked PDF..." else "Ranked Results PDF (Lecturer & Admin)",
            style = ButtonTextStyle.copy(fontSize = 13.5.sp, color = SilverWhite)
          )
        }
      }
    }

    if (!errorMessage.isNullOrBlank()) {
      FriendlyErrorSnackbar(errorMessage = errorMessage, onDismiss = onDismissError)
    }

    if (examFlags.isNotEmpty()) {
      RoyalCard(borderColor = ErrorColor.copy(alpha = 0.55f)) {
        Text(
          text = "Integrity Flags (${examFlags.size})",
          style = CardTitleStyle.copy(fontSize = 15.sp, color = ErrorColor),
          modifier = Modifier.testTag("results_exam_flags_card")
        )
        examFlags.take(8).forEach { flag ->
          val tags = buildList {
            if (flag.exitViolations > 0) add("Exits: ${flag.exitViolations}")
            if (flag.autoSubmitted) {
              val r = flag.autoSubmitReason.ifBlank { "auto" }
              add("Auto ($r)")
            }
            if (flag.late) add("Late")
          }.joinToString(" • ")
          Text(
            text = "${flag.fullName} (${flag.admissionNumber}) — $tags",
            style = CaptionStyle.copy(color = Lavender)
          )
        }
        if (examFlags.size > 8) {
          Text("+${examFlags.size - 8} more flagged submission(s)", style = CaptionStyle.copy(color = ChampagneGold))
        }
      }
    }

    if (isLoading) {
      Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MetallicGold)
      }
    } else if (rows.isEmpty()) {
      EmptyState(
        icon = Icons.Default.Assessment,
        title = "No Submissions Yet",
        message = "Student scores will appear here automatically once they submit."
      )
    } else {
      rows.forEachIndexed { idx, row ->
        RoyalCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${idx + 1}. ${row.fullName}", style = CardTitleStyle.copy(fontSize = 17.sp), modifier = Modifier.weight(1f, fill = false))
                if (row.late) {
                  Text("LATE", style = FieldLabelStyle.copy(fontSize = 11.sp, color = ErrorColor))
                }
                if (row.focusLossCount > 0) {
                  Text("EXIT(${row.focusLossCount})", style = FieldLabelStyle.copy(fontSize = 11.sp, color = ErrorColor))
                }
              }
              val rel = NotificationTimeFormatter.formatRelative(row.submittedAt)
              val sub = listOf(row.admissionNumber, row.markingStatus.label, rel)
                .filter { it.isNotBlank() && it != "-" }
                .joinToString(" • ")
              if (sub.isNotBlank()) Text(sub, style = CaptionStyle)
            }
            Spacer(modifier = Modifier.width(10.dp))
            val scoreText = if (row.score != null) {
              "${formatMarks(row.score)}/${formatMarks(row.total)}"
            } else {
              "Pending"
            }
            Text(
              text = scoreText,
              fontFamily = PlayfairDisplayFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = if (row.score != null) ChampagneGold else Lavender
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}
