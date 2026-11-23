package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.ChallengeItem
import com.example.data.CountdownStyle
import com.example.data.ExamExitAction
import com.example.data.PlayQuestion
import com.example.data.QuestionType
import com.example.data.QuizSubmissionResult
import com.example.data.formatMarks
import com.example.ui.ChallengesUiState
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.FriendlyErrorSnackbar
import com.example.ui.components.MetallicGoldText
import com.example.ui.components.RoyalCard
import com.example.ui.components.RoyalCrownIcon
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
import com.example.ui.theme.RoyalSurface
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.ScreenTitleStyle
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.SilverWhite
import com.example.ui.theme.SuccessColor
import com.example.util.DurationHelper

private val DisabledTextToolbar = object : TextToolbar {
  override val status: TextToolbarStatus = TextToolbarStatus.Hidden
  override fun hide() {}
  override fun showMenu(
    rect: Rect,
    onCopyRequested: (() -> Unit)?,
    onPasteRequested: (() -> Unit)?,
    onCutRequested: (() -> Unit)?,
    onSelectAllRequested: (() -> Unit)?
  ) {}
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}

@Composable
fun PlayChallengeScreen(
  quiz: ChallengeItem,
  uiState: ChallengesUiState,
  onSelectAnswer: (ord: Int, optionIndex: Int) -> Unit,
  onUpdateWrittenAnswer: (ord: Int, text: String) -> Unit,
  onGoToQuestion: (index: Int) -> Unit,
  onSecurityViolation: (eventType: String, detail: String) -> Unit = { _, _ -> },
  onDismissSecurityWarning: () -> Unit = {},
  onSubmit: () -> Unit,
  onOpenReview: () -> Unit,
  onDismissError: () -> Unit,
  onBack: () -> Unit
) {
  var showExitConfirm by remember { mutableStateOf(false) }
  var showSubmitConfirm by remember { mutableStateOf(false) }
  var pasteBlockedNotice by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val clipboardManager = LocalClipboardManager.current
  val defaultToolbar = LocalTextToolbar.current

  val isAttemptActive = uiState.submissionResult == null && uiState.playQuestions.isNotEmpty()
  val latestViolationHandler by rememberUpdatedState(onSecurityViolation)
  val latestActiveFlag by rememberUpdatedState(isAttemptActive)

  // Block screenshots / screen recording during protected exam or no-copy assessment
  DisposableEffect(quiz.quizId, quiz.noCopyPaste, quiz.isExam, isAttemptActive) {
    val activity = context.findActivity()
    val window = activity?.window
    val shouldSecure = (quiz.noCopyPaste || quiz.isExam) && isAttemptActive
    val hadSecureBefore = window != null && (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
    if (window != null && shouldSecure && !hadSecureBefore) {
      window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    onDispose {
      if (window != null && shouldSecure && !hadSecureBefore) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
      }
    }
  }

  // Detect leaving / backgrounding the app during an active assessment
  DisposableEffect(lifecycleOwner, quiz.quizId, quiz.onExitAction) {
    val activity = context.findActivity()
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_PAUSE && latestActiveFlag && quiz.onExitAction != ExamExitAction.OFF) {
        if (activity?.isChangingConfigurations != true) {
          latestViolationHandler("app_exit", "Moved app to background or switched window")
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val handleBack: () -> Unit = {
    if (uiState.submissionResult != null || uiState.playQuestions.isEmpty()) onBack()
    else showExitConfirm = true
  }
  BackHandler(onBack = handleBack)

  val rem = uiState.playRemainingSeconds
  val qRem = uiState.playQuestionRemainingSeconds
  val isUrgentTime = (rem != null && rem <= 60L) || (qRem != null && qRem <= 10L)

  val answeredCount = uiState.playQuestions.count { q ->
    if (q.questionType == QuestionType.WRITTEN) {
      !uiState.playWrittenAnswers[q.ord].isNullOrBlank()
    } else {
      uiState.playSelectedAnswers.containsKey(q.ord)
    }
  }

  val activeToolbar = if (quiz.noCopyPaste) DisabledTextToolbar else defaultToolbar

  CompositionLocalProvider(LocalTextToolbar provides activeToolbar) {
    Box(modifier = Modifier.fillMaxSize().testTag("play_challenge_screen")) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            IconButton(onClick = handleBack, modifier = Modifier.size(48.dp).testTag("play_back_btn")) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
            }
            Spacer(modifier = Modifier.width(4.dp))
            MetallicGoldText(text = if (quiz.isExam) "Exam" else "Challenge", style = ScreenTitleStyle)
          }

          if (rem != null && uiState.submissionResult == null) {
            val timeText = DurationHelper.formatCountdown(rem)
            val pillColor = if (isUrgentTime) ErrorColor else ChampagneGold
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(pillColor.copy(alpha = 0.18f))
                .border(1.dp, pillColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("play_timer_pill")
            ) {
              Icon(Icons.Default.Timer, contentDescription = "Remaining time", tint = pillColor, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(timeText, style = FieldLabelStyle.copy(color = pillColor))
            }
          }
        }

        if (rem != null && uiState.submissionResult == null && quiz.countdownStyle == CountdownStyle.BAR) {
          val totalSec = uiState.playTimeLimitSeconds?.coerceAtLeast(1L) ?: rem.coerceAtLeast(1L)
          val ratio = (rem.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
          LinearProgressIndicator(
            progress = { ratio },
            color = if (isUrgentTime) ErrorColor else ChampagneGold,
            trackColor = RoyalSurfaceHigh,
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp))
          )
        }

        if (quiz.oneWayNavigation || quiz.noCopyPaste || quiz.onExitAction != ExamExitAction.OFF) {
          val tags = buildList {
            if (quiz.oneWayNavigation) add("One-Way (No Turning Back)")
            if (quiz.noCopyPaste) add("Copy/Screenshot Blocked")
            if (quiz.onExitAction != ExamExitAction.OFF) add("Exit Monitored (${uiState.playFocusLossCount}/${quiz.maxExitViolations})")
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(RoyalSurfaceHigh)
              .border(1.dp, ChampagneGold.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
              .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(tags.joinToString(" • "), style = CaptionStyle.copy(color = ChampagneGold, fontSize = 11.5.sp))
          }
        }

        if (!uiState.playSecurityWarning.isNullOrBlank() && uiState.submissionResult == null) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(ErrorColor.copy(alpha = 0.16f))
              .border(1.dp, ErrorColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
              .padding(horizontal = 12.dp, vertical = 8.dp)
              .testTag("play_security_warning_banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(uiState.playSecurityWarning, style = CaptionStyle.copy(color = ErrorColor))
            }
            TextButton(onClick = onDismissSecurityWarning) {
              Text("OK", style = FieldLabelStyle.copy(color = ErrorColor, fontSize = 12.sp))
            }
          }
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
          FriendlyErrorSnackbar(errorMessage = uiState.errorMessage, onDismiss = onDismissError)
        }

        val result = uiState.submissionResult
        if (result != null) {
          ChallengeResultCelebration(
            isExam = quiz.isExam,
            quizTitle = quiz.title,
            result = result,
            autoSubmittedReason = uiState.playAutoSubmittedReason,
            onReviewAnswers = onOpenReview,
            onDone = onBack
          )
        } else if (uiState.isLoadingPlay) {
          Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
              CircularProgressIndicator(color = MetallicGold)
              Text(if (quiz.isExam) "Preparing exam paper..." else "Preparing challenge...", style = BodyStyle.copy(color = Lavender))
            }
          }
        } else if (uiState.playQuestions.isEmpty()) {
          RoyalCard {
            Text(quiz.title, style = CardTitleStyle)
            Text("No questions available right now.", style = BodyStyle.copy(color = Lavender))
            OutlinedButton(
              onClick = onBack,
              shape = RoundedCornerShape(ButtonRadius),
              border = BorderStroke(1.dp, ChampagneGold),
              modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
            ) {
              Text("Return", style = ButtonTextStyle.copy(color = ChampagneGold))
            }
          }
        } else {
          val questions = uiState.playQuestions
          val idx = uiState.playCurrentIndex.coerceIn(0, questions.lastIndex)
          val currentQ: PlayQuestion = questions[idx]
          val isCurrentLocked = currentQ.ord in uiState.playLockedQuestionOrds

          RoyalCard {
            Text(quiz.title, style = CardTitleStyle)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Question ${idx + 1} of ${questions.size}", style = FieldLabelStyle.copy(color = ChampagneGold))
              Text("$answeredCount/${questions.size} answered", style = CaptionStyle)
            }
            LinearProgressIndicator(
              progress = { (idx + 1).toFloat() / questions.size.toFloat() },
              color = MetallicGold,
              trackColor = RoyalSurfaceHigh,
              modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
            )

            if (qRem != null) {
              Spacer(modifier = Modifier.height(8.dp))
              val qTotal = uiState.playQuestionTotalSeconds?.coerceAtLeast(1L) ?: qRem.coerceAtLeast(1L)
              val qRatio = (qRem.toFloat() / qTotal.toFloat()).coerceIn(0f, 1f)
              val qColor = if (qRem <= 10L) ErrorColor else ChampagneGold
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("Question Timer (auto-locks at 0)", style = CaptionStyle.copy(color = qColor))
                Text(DurationHelper.formatCountdown(qRem), style = FieldLabelStyle.copy(color = qColor))
              }
              LinearProgressIndicator(
                progress = { qRatio },
                color = qColor,
                trackColor = RoyalSurfaceHigh,
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp))
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            questions.forEachIndexed { qIndex, qItem ->
              val isCurrent = qIndex == idx
              val isLockedDot = qItem.ord in uiState.playLockedQuestionOrds || (quiz.oneWayNavigation && qIndex < idx)
              val isAnswered = if (qItem.questionType == QuestionType.WRITTEN) {
                !uiState.playWrittenAnswers[qItem.ord].isNullOrBlank()
              } else {
                uiState.playSelectedAnswers.containsKey(qItem.ord)
              }
              val bg = when {
                isCurrent -> MetallicGold
                isLockedDot -> RoyalSurface
                isAnswered -> ElectricViolet.copy(alpha = 0.28f)
                else -> RoyalSurfaceHigh
              }
              val border = when {
                isCurrent -> ChampagneGold
                isLockedDot -> DarkBorder.copy(alpha = 0.6f)
                isAnswered -> ElectricViolet
                else -> DarkBorder
              }
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(bg)
                  .border(1.dp, border, RoundedCornerShape(12.dp))
                  .clickable(enabled = !isLockedDot) { onGoToQuestion(qIndex) }
                  .testTag("play_nav_dot_$qIndex"),
                contentAlignment = Alignment.Center
              ) {
                if (isLockedDot && !isCurrent) {
                  Icon(Icons.Default.Lock, contentDescription = "Locked question ${qIndex + 1}", tint = Lavender.copy(alpha = 0.65f), modifier = Modifier.size(16.dp))
                } else {
                  Text(
                    text = "${qIndex + 1}",
                    style = FieldLabelStyle.copy(color = if (isCurrent) Midnight else SilverWhite)
                  )
                }
              }
            }
          }

          RoyalCard(isHero = true) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = if (currentQ.questionType == QuestionType.WRITTEN) "Written Response" else "Multiple Choice",
                  style = CaptionStyle.copy(color = ChampagneGold)
                )
                if (isCurrentLocked) {
                  Text("• LOCKED", style = FieldLabelStyle.copy(fontSize = 11.sp, color = ErrorColor))
                }
              }
              Text(
                text = "${formatMarks(currentQ.marks)} ${if (currentQ.marks == 1.0) "mark" else "marks"}",
                style = FieldLabelStyle.copy(color = ChampagneGold)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = currentQ.question,
              style = SectionHeadingStyle.copy(color = SilverWhite, lineHeight = 28.sp)
            )
          }

          if (currentQ.questionType == QuestionType.WRITTEN) {
            val writtenText = uiState.playWrittenAnswers[currentQ.ord].orEmpty()
            OutlinedTextField(
              value = writtenText,
              onValueChange = { incoming ->
                if (isCurrentLocked) return@OutlinedTextField
                if (quiz.noCopyPaste && incoming.length > writtenText.length + 1) {
                  val clip = clipboardManager.getText()?.text.orEmpty()
                  val looksPasted = (incoming.length - writtenText.length >= 6) ||
                    (clip.length >= 3 && incoming.contains(clip) && !writtenText.contains(clip))
                  if (looksPasted) {
                    pasteBlockedNotice = true
                    return@OutlinedTextField
                  }
                }
                pasteBlockedNotice = false
                onUpdateWrittenAnswer(currentQ.ord, incoming)
              },
              enabled = !isCurrentLocked,
              readOnly = isCurrentLocked,
              label = { Text(if (isCurrentLocked) "Answer Locked" else "Your Answer") },
              placeholder = { Text("Write a clear, structured response...") },
              colors = royalOutlinedTextFieldColors(),
              minLines = 6,
              maxLines = 14,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("play_written_input_${currentQ.ord}")
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(
                text = if (pasteBlockedNotice) "Copy/paste is disabled for this exam." else "",
                style = CaptionStyle.copy(color = ErrorColor)
              )
              Text(
                text = "${writtenText.length} / 8000 characters",
                style = CaptionStyle
              )
            }
          } else {
            val selectedOpt = uiState.playSelectedAnswers[currentQ.ord]
            currentQ.options.forEachIndexed { optIdx, optText ->
              val isSelected = selectedOpt == optIdx
              val shape = RoundedCornerShape(14.dp)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .defaultMinSize(minHeight = 52.dp)
                  .then(if (isSelected) Modifier.shadow(10.dp, shape, ambientColor = ElectricViolet, spotColor = ElectricViolet) else Modifier)
                  .clip(shape)
                  .background(if (isSelected) RoyalSurfaceHigh else RoyalSurface)
                  .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) ElectricViolet else DarkBorder, shape)
                  .clickable(enabled = !isCurrentLocked) { onSelectAnswer(currentQ.ord, optIdx) }
                  .padding(horizontal = 14.dp, vertical = 14.dp)
                  .testTag("play_option_${currentQ.ord}_$optIdx"),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                  contentDescription = null,
                  tint = if (isSelected) ChampagneGold else Lavender,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                val letter = ('A' + optIdx).toString()
                Text(
                  text = "$letter. $optText",
                  style = BodyStyle.copy(
                    color = if (isSelected) ChampagneGold else SilverWhite,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                  ),
                  modifier = Modifier.weight(1f)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          val canGoBack = idx > 0 && !quiz.oneWayNavigation &&
            (questions.getOrNull(idx - 1)?.ord !in uiState.playLockedQuestionOrds)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            if (canGoBack) {
              OutlinedButton(
                onClick = { onGoToQuestion(idx - 1) },
                shape = RoundedCornerShape(ButtonRadius),
                border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.65f)),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp).testTag("play_prev_btn")
              ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Previous", style = ButtonTextStyle.copy(color = ChampagneGold))
              }
            }

            if (idx < questions.lastIndex) {
              Button(
                onClick = { onGoToQuestion(idx + 1) },
                shape = RoundedCornerShape(ButtonRadius),
                colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp).testTag("play_next_btn")
              ) {
                Text(if (quiz.oneWayNavigation) "Save & Next" else "Next", style = ButtonTextStyle.copy(color = Midnight))
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Midnight, modifier = Modifier.size(18.dp))
              }
            } else {
              Button(
                onClick = { showSubmitConfirm = true },
                enabled = !uiState.isActionLoading,
                shape = RoundedCornerShape(ButtonRadius),
                colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp).testTag("play_submit_btn")
              ) {
                if (uiState.isActionLoading) {
                  CircularProgressIndicator(color = Midnight, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                  Text("Submit", style = ButtonTextStyle.copy(color = Midnight))
                }
              }
            }
          }

          if (idx < questions.lastIndex) {
            OutlinedButton(
              onClick = { showSubmitConfirm = true },
              enabled = !uiState.isActionLoading,
              shape = RoundedCornerShape(ButtonRadius),
              border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.55f)),
              modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("play_finish_early_btn")
            ) {
              Text("Finish & Submit ($answeredCount/${questions.size})", style = ButtonTextStyle.copy(color = ChampagneGold))
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }

      if (showSubmitConfirm) {
        val unanswered = (uiState.playQuestions.size - answeredCount).coerceAtLeast(0)
        val msg = if (unanswered > 0) {
          "You have $unanswered unanswered question(s). Submit anyway? This cannot be undone."
        } else {
          "Submit your answers now?"
        }
        ConfirmActionDialog(
          title = if (quiz.isExam) "Submit Exam?" else "Submit Challenge?",
          message = msg,
          confirmButtonText = "Submit Answers",
          isLoading = uiState.isActionLoading,
          onConfirm = {
            showSubmitConfirm = false
            onSubmit()
          },
          onDismiss = { showSubmitConfirm = false }
        )
      }

      if (showExitConfirm) {
        val warnExtra = if (quiz.oneWayNavigation) " Answered or skipped questions stay locked." else ""
        ConfirmActionDialog(
          title = if (quiz.isExam) "Exit Exam Screen?" else "Pause Challenge?",
          message = "Your timer keeps running on the server and leaving is logged.$warnExtra",
          confirmButtonText = "Leave Screen",
          onConfirm = {
            showExitConfirm = false
            onBack()
          },
          onDismiss = { showExitConfirm = false }
        )
      }
    }
  }
}

@Composable
private fun ChallengeResultCelebration(
  isExam: Boolean,
  quizTitle: String,
  result: QuizSubmissionResult,
  autoSubmittedReason: String?,
  onReviewAnswers: () -> Unit,
  onDone: () -> Unit
) {
  RoyalCard(isHero = true, modifier = Modifier.testTag("challenge_result_card")) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      RoyalCrownIcon(size = 42.dp, tint = ChampagneGold)
      MetallicGoldText(
        text = if (result.needsMarking || isExam) "Submitted" else "Challenge Complete",
        style = ScreenTitleStyle,
        textAlign = TextAlign.Center
      )
      Text(quizTitle, style = CardTitleStyle, textAlign = TextAlign.Center)

      if (!autoSubmittedReason.isNullOrBlank()) {
        val reasonLabel = when (autoSubmittedReason) {
          "TIME_EXPIRED" -> "Auto-submitted: Exam time expired"
          "QUESTION_TIME_ENDED" -> "Auto-submitted: Final question timer ended"
          "SECURITY_APP_EXIT" -> "Auto-submitted: Security exit limit reached"
          else -> "Auto-submitted: $autoSubmittedReason"
        }
        Text(reasonLabel, style = CaptionStyle.copy(color = ChampagneGold), textAlign = TextAlign.Center)
      }

      if (result.needsMarking) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ElectricViolet.copy(alpha = 0.16f))
            .border(1.dp, ElectricViolet.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
          Icon(Icons.Default.HourglassTop, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Your paper is waiting for lecturer marking. You will be notified once it is returned.",
            style = BodyStyle.copy(color = ChampagneGold)
          )
        }
      } else if (isExam) {
        Text(
          text = "Your answers are recorded. Official scores and feedback are released once the exam closes.",
          style = BodyStyle.copy(color = Lavender),
          textAlign = TextAlign.Center
        )
      } else {
        Text(
          text = "${formatMarks(result.score)} / ${formatMarks(result.total)}",
          fontFamily = PlayfairDisplayFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 36.sp,
          color = ChampagneGold
        )
        val pct = if (result.total > 0.0) ((result.score * 100.0) / result.total).toInt() else 0
        Text("$pct% accuracy", style = BodyStyle.copy(color = Lavender))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SuccessColor.copy(alpha = 0.16f))
            .border(1.dp, SuccessColor.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
          Icon(Icons.Default.Stars, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (result.pointsAwarded > 0) "+${result.pointsAwarded} points awarded!" else "Submitted after deadline (0 bonus pts)",
            style = FieldLabelStyle.copy(color = if (result.pointsAwarded > 0) SuccessColor else Lavender)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      if (!isExam) {
        Button(
          onClick = onReviewAnswers,
          shape = RoundedCornerShape(ButtonRadius),
          colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
          modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp).testTag("result_review_answers_btn")
        ) {
          Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Midnight, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Review Answers & Explanations", style = ButtonTextStyle.copy(color = Midnight))
        }
      }

      OutlinedButton(
        onClick = onDone,
        shape = RoundedCornerShape(ButtonRadius),
        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp).testTag("result_done_btn")
      ) {
        Text("Back to Challenges", style = ButtonTextStyle.copy(color = ChampagneGold))
      }
    }
  }
}
