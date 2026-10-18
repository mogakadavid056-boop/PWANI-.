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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PollResultsData
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun PollResultsView(
  pollResults: PollResultsData,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Results",
        style = SectionHeadingStyle,
        color = GoldLight
      )
      Text(
        text = "Total votes: ${pollResults.totalVotes}",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      pollResults.options.forEach { opt ->
        ClassRepCard(
          modifier = if (opt.isMyVote) Modifier.border(1.5.dp, SuccessGreen.copy(alpha = 0.8f), RoundedCornerShape(16.dp)) else Modifier
        ) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                  text = opt.optionText,
                  fontSize = 15.sp,
                  fontWeight = if (opt.isMyVote) FontWeight.Bold else FontWeight.Medium,
                  color = TextPrimary
                )
                if (opt.isMyVote) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Icon(Icons.Default.Check, contentDescription = "Your vote", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                  Text(text = "(Your vote)", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                }
              }

              // Percentage badge
              Text(
                text = String.format(Locale.US, "%.1f%% (%d)", opt.percentage, opt.votes),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (opt.isMyVote) SuccessGreen else GoldLight
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rounded progress bar with percentage
            val progressFraction = (opt.percentage / 100.0).coerceIn(0.0, 1.0).toFloat()
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(DarkSurface)
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth(fraction = if (progressFraction > 0f) progressFraction else 0.001f)
                  .height(10.dp)
                  .clip(RoundedCornerShape(5.dp))
                  .background(if (opt.isMyVote) SuccessGreen else GoldPrimary)
              )
            }
          }
        }
      }
    }
  }
}
