package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChallengeItem
import com.example.data.ExamAnswerFormat
import com.example.data.MarkingStatus
import com.example.data.QuizStatus
import com.example.data.QuizStatusFilter
import com.example.data.formatMarks
import com.example.notifications.NotificationTimeFormatter
import com.example.ui.ChallengesUiState
import com.example.ui.components.ActivityStatsSummaryCard
import com.example.ui.components.EmptyState
import com.example.ui.components.FriendlyErrorSnackbar
import com.example.ui.components.LoadingSkeleton
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
import com.example.ui.theme.NavLabelStyle
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.SilverWhite
import com.example.ui.theme.SuccessColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ChallengesSection(
  uiState: ChallengesUiState,
  isStaff: Boolean,
  canPlayChallenges: Boolean,
  isAdmin: Boolean,
  isLecturer: Boolean,
  currentUserId: String?,
  onRefresh: () -> Unit,
  onSelectFilter: (QuizStatusFilter) -> Unit,
  onCreateClick: () -> Unit,
  onAddQuestionsClick: (ChallengeItem) -> Unit,
  onPlayClick: (ChallengeItem) -> Unit,
  onReviewClick: (ChallengeItem) -> Unit,
  onResultsClick: (ChallengeItem) -> Unit,
  onMarkPapersClick: (ChallengeItem) -> Unit,
  onViewMarkedPaperClick: (ChallengeItem) -> Unit,
  onDeleteClick: (ChallengeItem) -> Unit,
  onOpenLeaderboard: () -> Unit,
  onDismissError: () -> Unit,
  onDismissSuccess: () -> Unit
) {
  val canMarkExams = isAdmin || isLecturer

  Column(
    modifier = Modifier.fillMaxWidth().testTag("challenges_section"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    ActivityStatsSummaryCard(
      stats = uiState.activityStats,
      isLoading = uiState.isLoadingStats,
      openChallengesCount = uiState.openCount,
      showQuickActions = false
    )

    OutlinedButton(
      onClick = onOpenLeaderboard,
      shape = RoundedCornerShape(ButtonRadius),
      border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.65f)),
      modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("challenges_to_leaderboard_btn")
    ) {
      Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("View Class Leaderboard", style = ButtonTextStyle.copy(color = ChampagneGold))
    }

    if (!uiState.errorMessage.isNullOrBlank()) {
      FriendlyErrorSnackbar(errorMessage = uiState.errorMessage, onDismiss = onDismissError)
    }

    if (!uiState.successMessage.isNullOrBlank()) {
      RoyalCard(
        borderColor = SuccessColor.copy(alpha = 0.6f),
        backgroundColor = RoyalSurfaceHigh
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(uiState.successMessage, style = BodyStyle.copy(color = SuccessColor), modifier = Modifier.weight(1f))
          OutlinedButton(
            onClick = onDismissSuccess,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, SuccessColor.copy(alpha = 0.5f)),
            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
          ) {
            Text("OK", style = FieldLabelStyle.copy(color = SuccessColor))
          }
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "Challenges & Exams (${uiState.filteredQuizzes.size})",
        style = SectionHeadingStyle
      )
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        IconButton(
          onClick = onRefresh,
          modifier = Modifier.size(48.dp).testTag("refresh_challenges_btn")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh challenges", tint = ChampagneGold)
        }
        if (isStaff) {
          Button(
            onClick = onCreateClick,
            shape = RoundedCornerShape(ButtonRadius),
            colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("create_challenge_btn")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Midnight, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Create", style = ButtonTextStyle.copy(fontSize = 14.sp, color = Midnight))
          }
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      QuizStatusFilter.entries.forEach { filter ->
        val selected = uiState.selectedFilter == filter
        FilterChip(
          selected = selected,
          onClick = { onSelectFilter(filter) },
          label = { Text(filter.label, style = NavLabelStyle) },
          modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("challenge_filter_${filter.name.lowercase()}"),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = DarkBorder,
            selectedBorderColor = ChampagneGold
          ),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ChampagneGold,
            selectedLabelColor = Midnight,
            containerColor = RoyalSurfaceHigh,
            labelColor = Lavender
          )
        )
      }
    }

    if (uiState.isLoadingQuizzes && uiState.quizzes.isEmpty()) {
      LoadingSkeleton(height = 120)
      LoadingSkeleton(height = 120)
    } else if (uiState.filteredQuizzes.isEmpty()) {
      EmptyState(
        icon = Icons.Default.Quiz,
        title = "No Challenges Yet",
        message = if (isStaff) {
          "Create a timed challenge or written exam for your class."
        } else {
          "Your lecturers or class prefect haven't posted a challenge for this filter yet."
        },
        actionLabel = if (isStaff) "Create Assessment" else null,
        onActionClick = if (isStaff) onCreateClick else null,
        modifier = Modifier.testTag("challenges_empty_state")
      )
    } else {
      uiState.filteredQuizzes.forEach { quiz ->
        val canManageQuiz = isAdmin || isLecturer || (!quiz.createdBy.isNullOrBlank() && quiz.createdBy == currentUserId)
        val canMarkForQuiz = isAdmin || (isLecturer && (uiState.canMarkByQuizId[quiz.quizId] ?: (quiz.createdBy == currentUserId)))
        ChallengeCard(
          quiz = quiz,
          isStaff = isStaff,
          canPlay = canPlayChallenges,
          canMark = canMarkForQuiz && quiz.isExam && quiz.answerFormat != ExamAnswerFormat.CHOICES,
          canManageQuiz = canManageQuiz,
          isLoadingPdf = uiState.isLoadingPdf,
          onPlayClick = { onPlayClick(quiz) },
          onReviewClick = { onReviewClick(quiz) },
          onResultsClick = { onResultsClick(quiz) },
          onMarkPapersClick = { onMarkPapersClick(quiz) },
          onViewMarkedPaperClick = { onViewMarkedPaperClick(quiz) },
          onAddQuestionsClick = { onAddQuestionsClick(quiz) },
          onDeleteClick = { onDeleteClick(quiz) }
        )
      }
    }
  }
}

@Composable
private fun ChallengeCard(
  quiz: ChallengeItem,
  isStaff: Boolean,
  canPlay: Boolean,
  canMark: Boolean,
  canManageQuiz: Boolean,
  isLoadingPdf: Boolean,
  onPlayClick: () -> Unit,
  onReviewClick: () -> Unit,
  onResultsClick: () -> Unit,
  onMarkPapersClick: () -> Unit,
  onViewMarkedPaperClick: () -> Unit,
  onAddQuestionsClick: () -> Unit,
  onDeleteClick: () -> Unit
) {
  val statusColor = when (quiz.status) {
    QuizStatus.OPEN -> SuccessColor
    QuizStatus.UPCOMING -> ChampagneGold
    QuizStatus.CLOSED -> Lavender
  }

  RoyalCard(
    borderColor = when {
      quiz.isExam && quiz.status == QuizStatus.OPEN -> ChampagneGold.copy(alpha = 0.75f)
      quiz.status == QuizStatus.OPEN && !quiz.attempted -> ElectricViolet.copy(alpha = 0.7f)
      else -> null
    },
    modifier = Modifier.testTag("challenge_card_${quiz.quizId}")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())
      ) {
        StatusPill(quiz.status.label.uppercase(), statusColor)
        if (quiz.isExam) {
          StatusPill("EXAM", ChampagneGold)
        }
        if (quiz.answerFormat != ExamAnswerFormat.CHOICES) {
          StatusPill(quiz.answerFormat.label.uppercase(), ElectricViolet)
        }
        if (quiz.unitCode.isNotBlank()) {
          StatusPill(quiz.unitCode, ChampagneGold, darkBg = true)
        }
        if (quiz.oneWayNavigation) {
          StatusPill("NO BACK", ElectricViolet, darkBg = true)
        }
        if (quiz.noCopyPaste) {
          StatusPill("SECURE", ChampagneGold, darkBg = true)
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      if (quiz.attempted && quiz.myScore != null && quiz.myTotal != null) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SuccessColor.copy(alpha = 0.15f))
            .border(1.dp, SuccessColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${formatMarks(quiz.myScore)}/${formatMarks(quiz.myTotal)}",
            style = FieldLabelStyle.copy(fontSize = 12.sp, color = SuccessColor)
          )
        }
      } else if (quiz.attempted && quiz.myMarkingStatus != null) {
        val mColor = when (quiz.myMarkingStatus) {
          MarkingStatus.RELEASED -> SuccessColor
          MarkingStatus.MARKING -> ElectricViolet
          else -> ChampagneGold
        }
        StatusPill(quiz.myMarkingStatus.label.uppercase(), mColor)
      }
    }

    Text(text = quiz.title, style = CardTitleStyle)

    if (quiz.description.isNotBlank()) {
      Text(text = quiz.description, style = BodyStyle.copy(color = Lavender))
    }

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Quiz, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(4.dp))
        val marksInfo = if (quiz.totalMarks > 0.0) " • ${formatMarks(quiz.totalMarks)} marks" else ""
        Text("${quiz.questionCount} Qs$marksInfo", style = CaptionStyle)
      }
      if (quiz.timerSummaryText.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Timer, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(quiz.timerSummaryText, style = CaptionStyle)
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Schedule, contentDescription = null, tint = Lavender, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(4.dp))
        val windowLabel = when (quiz.status) {
          QuizStatus.UPCOMING -> "Opens ${formatIsoShort(quiz.opensAt)}"
          QuizStatus.OPEN -> "Closes ${formatIsoShort(quiz.closesAt)}"
          QuizStatus.CLOSED -> "Closed ${NotificationTimeFormatter.formatRelative(quiz.closesAt).ifBlank { formatIsoShort(quiz.closesAt) }}"
        }
        Text(windowLabel, style = CaptionStyle)
      }
    }

    if (isStaff) {
      Text(
        text = "${quiz.attemptsCount} ${if (quiz.attemptsCount == 1) "submission" else "submissions"}",
        style = CaptionStyle.copy(color = ChampagneGold)
      )
    }

    Spacer(modifier = Modifier.height(2.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (canPlay && quiz.status == QuizStatus.OPEN && !quiz.attempted) {
        Button(
          onClick = onPlayClick,
          shape = RoundedCornerShape(ButtonRadius),
          colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("play_challenge_btn_${quiz.quizId}")
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Midnight, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (quiz.isExam) "Take Exam" else "Play Challenge", style = ButtonTextStyle.copy(fontSize = 14.sp, color = Midnight))
        }
      } else if (quiz.attempted && quiz.myMarkingStatus == MarkingStatus.RELEASED) {
        Button(
          onClick = onViewMarkedPaperClick,
          enabled = !isLoadingPdf,
          shape = RoundedCornerShape(ButtonRadius),
          colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("view_marked_paper_btn_${quiz.quizId}")
        ) {
          Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Midnight, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Marked Paper", style = ButtonTextStyle.copy(fontSize = 14.sp, color = Midnight))
        }
        OutlinedButton(
          onClick = onReviewClick,
          shape = RoundedCornerShape(ButtonRadius),
          border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.7f)),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("review_challenge_btn_${quiz.quizId}")
        ) {
          Text("Breakdown", style = ButtonTextStyle.copy(fontSize = 14.sp, color = ChampagneGold))
        }
      } else if (quiz.attempted) {
        OutlinedButton(
          onClick = onReviewClick,
          shape = RoundedCornerShape(ButtonRadius),
          border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.7f)),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("review_challenge_btn_${quiz.quizId}")
        ) {
          Icon(Icons.Default.Visibility, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Review Answers", style = ButtonTextStyle.copy(fontSize = 14.sp, color = ChampagneGold))
        }
      } else if (isStaff) {
        OutlinedButton(
          onClick = onReviewClick,
          shape = RoundedCornerShape(ButtonRadius),
          border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.65f)),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("preview_challenge_btn_${quiz.quizId}")
        ) {
          Icon(Icons.Default.Visibility, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Questions", style = ButtonTextStyle.copy(fontSize = 14.sp, color = ChampagneGold))
        }
      }

      if (isStaff) {
        if (canMark && quiz.attemptsCount > 0) {
          Button(
            onClick = onMarkPapersClick,
            shape = RoundedCornerShape(ButtonRadius),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, contentColor = SilverWhite),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("mark_papers_btn_${quiz.quizId}")
          ) {
            Icon(Icons.Default.Draw, contentDescription = null, tint = SilverWhite, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Mark Papers", style = ButtonTextStyle.copy(fontSize = 13.sp, color = SilverWhite))
          }
        } else {
          OutlinedButton(
            onClick = onResultsClick,
            shape = RoundedCornerShape(ButtonRadius),
            border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.7f)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("results_challenge_btn_${quiz.quizId}")
          ) {
            Icon(Icons.Default.Assessment, contentDescription = null, tint = Lavender, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Scores", style = ButtonTextStyle.copy(fontSize = 14.sp, color = SilverWhite))
          }
        }
        if (canManageQuiz && quiz.attemptsCount == 0) {
          IconButton(
            onClick = onAddQuestionsClick,
            modifier = Modifier.size(48.dp).testTag("add_questions_btn_${quiz.quizId}")
          ) {
            Icon(Icons.Default.PlaylistAdd, contentDescription = "Add more questions", tint = ChampagneGold)
          }
        }
        if (canManageQuiz) {
          IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.size(48.dp).testTag("delete_challenge_btn_${quiz.quizId}")
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorColor)
          }
        }
      }
    }
  }
}

@Composable
private fun StatusPill(
  text: String,
  color: androidx.compose.ui.graphics.Color,
  darkBg: Boolean = false
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(if (darkBg) RoyalSurfaceHigh else color.copy(alpha = 0.16f))
      .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
      .padding(horizontal = 8.dp, vertical = 3.dp)
  ) {
    Text(text, style = FieldLabelStyle.copy(fontSize = 11.sp, color = color))
  }
}

private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.US)

private fun formatIsoShort(iso: String): String {
  if (iso.isBlank()) return "-"
  return runCatching {
    Instant.parse(iso.trim()).atZone(ZoneId.systemDefault()).format(shortDateFormatter)
  }.getOrElse { iso.replace("T", " ").take(16) }
}
