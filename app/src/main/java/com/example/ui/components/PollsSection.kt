package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PollItem
import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PollsSection(
  polls: List<PollItem>,
  isStaff: Boolean,
  isLoading: Boolean,
  onRefresh: () -> Unit,
  onCreatePollClick: () -> Unit,
  onPollClick: (PollItem) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth().testTag("polls_section")) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "Polls",
          style = SectionHeadingStyle,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "(${polls.size})", fontSize = 14.sp, color = TextSecondary)
        IconButton(
          onClick = onRefresh,
          modifier = Modifier.size(32.dp).testTag("refresh_polls_btn")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh Polls", tint = GoldLight, modifier = Modifier.size(18.dp))
        }
      }

      if (isStaff) {
        Button(
          onClick = onCreatePollClick,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = GoldPrimary,
            contentColor = DarkTextOnGold
          ),
          modifier = Modifier.height(34.dp).testTag("create_poll_btn")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = DarkTextOnGold)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Create Poll", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold)
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (isLoading && polls.isEmpty()) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LoadingSkeleton(height = 90)
        LoadingSkeleton(height = 90)
      }
    } else if (polls.isEmpty()) {
      EmptyState(
        icon = Icons.Default.HowToVote,
        title = "No Polls Yet",
        message = if (isStaff) "Tap \"Create Poll\" above to ask a question to your class." else "Nothing here yet. Waiting for your class prefect to update this.",
        actionLabel = if (isStaff) "Create Poll" else null,
        onActionClick = if (isStaff) onCreatePollClick else null,
        modifier = Modifier.testTag("polls_empty_state")
      )
    } else {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        polls.forEach { poll ->
          PollCard(
            poll = poll,
            onClick = { onPollClick(poll) }
          )
        }
      }
    }
  }
}
