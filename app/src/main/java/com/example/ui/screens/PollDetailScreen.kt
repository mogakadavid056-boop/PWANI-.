package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PollItem
import com.example.data.PollOption
import com.example.data.PollResultsData
import androidx.compose.foundation.layout.defaultMinSize
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.ErrorBanner
import com.example.ui.components.MetallicGoldText
import com.example.ui.components.PollResultsView
import com.example.ui.components.PollVotingView
import com.example.ui.components.RoyalCard
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.ScreenTitleStyle

@Composable
fun PollDetailScreen(
  poll: PollItem,
  isStaff: Boolean,
  isLoading: Boolean,
  isActionLoading: Boolean,
  errorMessage: String?,
  pollOptions: List<PollOption>,
  pollResults: PollResultsData?,
  selectedOptionId: String?,
  showCloseConfirm: Boolean,
  showDeleteConfirm: Boolean,
  onSelectOption: (String) -> Unit,
  onCastVote: () -> Unit,
  onPromptClose: () -> Unit,
  onDismissClose: () -> Unit,
  onConfirmClose: () -> Unit,
  onPromptDelete: () -> Unit,
  onDismissDelete: () -> Unit,
  onConfirmDelete: () -> Unit,
  onDismissError: () -> Unit,
  onBack: () -> Unit
) {
  val (isOpen, statusText) = poll.statusBadge()

  Box(modifier = Modifier.fillMaxSize().testTag("poll_detail_screen")) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("poll_detail_back_btn")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
        }
        Spacer(modifier = Modifier.width(6.dp))
        MetallicGoldText(
          text = "Poll",
          style = ScreenTitleStyle,
          modifier = Modifier.weight(1f)
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isOpen) MetallicGold.copy(alpha = 0.16f) else RoyalSurfaceHigh)
            .border(1.dp, if (isOpen) MetallicGold.copy(alpha = 0.55f) else DarkBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = statusText,
            style = FieldLabelStyle.copy(fontSize = 12.sp, color = if (isOpen) ChampagneGold else Lavender)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (!errorMessage.isNullOrBlank()) {
        ErrorBanner(message = errorMessage, onDismiss = onDismissError)
        Spacer(modifier = Modifier.height(12.dp))
      }

      RoyalCard {
        Text(text = "QUESTION", style = FieldLabelStyle.copy(fontSize = 12.sp))
        Text(text = poll.question, style = BodyStyle.copy(fontWeight = FontWeight.SemiBold))
      }

      Spacer(modifier = Modifier.height(18.dp))

      if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = MetallicGold, modifier = Modifier.size(32.dp))
        }
      } else if (pollResults != null) {
        PollResultsView(pollResults = pollResults)
      } else if (pollOptions.isNotEmpty()) {
        PollVotingView(
          pollOptions = pollOptions,
          selectedOptionId = selectedOptionId,
          isActionLoading = isActionLoading,
          onSelectOption = onSelectOption,
          onCastVote = onCastVote
        )
      }

      if (isStaff) {
        Spacer(modifier = Modifier.height(28.dp))
        RoyalCard {
          Text("Staff Actions", style = FieldLabelStyle.copy(color = Lavender))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isOpen) {
              OutlinedButton(
                onClick = onPromptClose,
                shape = RoundedCornerShape(ButtonRadius),
                border = androidx.compose.foundation.BorderStroke(1.dp, MetallicGold.copy(alpha = 0.6f)),
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("close_poll_now_btn")
              ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Close Poll", style = ButtonTextStyle.copy(fontSize = 14.sp, color = ChampagneGold))
              }
            }
            OutlinedButton(
              onClick = onPromptDelete,
              shape = RoundedCornerShape(ButtonRadius),
              border = androidx.compose.foundation.BorderStroke(1.dp, ErrorColor.copy(alpha = 0.7f)),
              modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("delete_poll_btn")
            ) {
              Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Delete Poll", style = ButtonTextStyle.copy(fontSize = 14.sp, color = ErrorColor))
            }
          }
        }
      }
    }

    if (showCloseConfirm) {
      ConfirmActionDialog(
        title = "Close Poll Now?",
        message = "Are you sure you want to close this poll? Further votes will not be accepted.",
        confirmButtonText = "Close Poll",
        isLoading = isActionLoading,
        onDismiss = onDismissClose,
        onConfirm = onConfirmClose
      )
    }

    if (showDeleteConfirm) {
      ConfirmActionDialog(
        title = "Delete Poll?",
        message = "Are you sure you want to permanently delete this poll? This cannot be undone.",
        confirmButtonText = "Delete",
        confirmButtonColor = ErrorColor,
        isLoading = isActionLoading,
        onDismiss = onDismissDelete,
        onConfirm = onConfirmDelete
      )
    }
  }
}
