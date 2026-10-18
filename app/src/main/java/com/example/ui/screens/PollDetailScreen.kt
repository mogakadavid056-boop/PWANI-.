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
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.ErrorBanner
import com.example.ui.components.PollResultsView
import com.example.ui.components.PollVotingView
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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

  Box(modifier = Modifier.fillMaxSize().background(DarkBackground).testTag("poll_detail_screen")) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp).testTag("poll_detail_back_btn")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Poll",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = GoldLight,
          modifier = Modifier.weight(1f)
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isOpen) GoldPrimary.copy(alpha = 0.15f) else DarkSurface)
            .border(1.dp, if (isOpen) GoldPrimary.copy(alpha = 0.5f) else DarkBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(text = statusText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isOpen) GoldLight else TextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (!errorMessage.isNullOrBlank()) {
        ErrorBanner(message = errorMessage, onDismiss = onDismissError)
        Spacer(modifier = Modifier.height(12.dp))
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(DarkCard)
          .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
          .padding(16.dp)
      ) {
        Column {
          Text(text = "QUESTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldLight)
          Spacer(modifier = Modifier.height(6.dp))
          Text(text = poll.question, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(32.dp))
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
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
        ) {
          Column {
            Text("Staff Actions", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              if (isOpen) {
                OutlinedButton(
                  onClick = onPromptClose,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f).height(40.dp).testTag("close_poll_now_btn")
                ) {
                  Icon(Icons.Default.Close, contentDescription = null, tint = GoldLight, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Close Poll", fontSize = 12.sp, color = GoldLight)
                }
              }
              OutlinedButton(
                onClick = onPromptDelete,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(40.dp).testTag("delete_poll_btn")
              ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete Poll", fontSize = 12.sp, color = Color(0xFFEF5350))
              }
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
        isLoading = isActionLoading,
        onDismiss = onDismissDelete,
        onConfirm = onConfirmDelete
      )
    }
  }
}
