package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PollItem
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.PoppinsFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PollCard(
  poll: PollItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val (isOpen, statusText) = poll.statusBadge()

  ClassRepCard(
    modifier = modifier.testTag("poll_card_${poll.id}"),
    onClick = onClick
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Status Badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isOpen) GoldPrimary.copy(alpha = 0.15f) else DarkSurface)
            .border(1.dp, if (isOpen) GoldPrimary.copy(alpha = 0.5f) else DarkBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("poll_status_badge_${poll.id}")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.Schedule,
              contentDescription = null,
              tint = if (isOpen) GoldLight else TextSecondary,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = statusText,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (isOpen) GoldLight else TextSecondary
            )
          }
        }

        // Voted Badge
        if (poll.hasVoted) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(SuccessGreen.copy(alpha = 0.18f))
              .border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
              .padding(horizontal = 8.dp, vertical = 3.dp)
              .testTag("poll_voted_badge_${poll.id}")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Voted",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SuccessGreen
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Question
      Text(
        text = poll.question,
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = TextPrimary
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Total votes footer
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Default.HowToVote,
          contentDescription = null,
          tint = GoldPrimary,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = "${poll.totalVotes} ${if (poll.totalVotes == 1) "vote" else "votes"}",
          fontSize = 12.sp,
          color = TextSecondary
        )
      }
    }
  }
}
