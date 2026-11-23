package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActivityStats
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SilverWhite

@Composable
fun ActivityStatsSummaryCard(
  stats: ActivityStats?,
  isLoading: Boolean,
  openChallengesCount: Int,
  showQuickActions: Boolean = true,
  onOpenChallenges: () -> Unit = {},
  onOpenLeaderboard: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  if (isLoading && stats == null) {
    LoadingSkeleton(height = 108, modifier = modifier)
    return
  }
  val effective = stats ?: ActivityStats()

  RoyalCard(
    isHero = true,
    modifier = modifier.testTag("activity_stats_card")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Stars, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Points & Streaks", style = FieldLabelStyle.copy(color = ChampagneGold))
      }
      if (effective.weekRank != null && effective.rankedStudents > 0) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ElectricViolet.copy(alpha = 0.22f))
            .border(1.dp, ElectricViolet.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = "Rank #${effective.weekRank} of ${effective.rankedStudents}",
            style = FieldLabelStyle.copy(color = SilverWhite, fontSize = 12.sp)
          )
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      StatTile(
        label = "Total Pts",
        value = "${effective.totalPoints}",
        sub = "+${effective.weekPoints} this wk",
        icon = Icons.Default.Stars,
        tint = ChampagneGold,
        modifier = Modifier.weight(1f)
      )
      StatTile(
        label = "Streak",
        value = "${effective.streakDays}d",
        sub = "Best ${effective.bestStreak}d",
        icon = Icons.Default.LocalFireDepartment,
        tint = if (effective.streakDays > 0) MetallicGold else Lavender,
        modifier = Modifier.weight(1f)
      )
    }

    if (showQuickActions) {
      Spacer(modifier = Modifier.height(2.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onOpenChallenges,
          shape = RoundedCornerShape(ButtonRadius),
          colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("open_challenges_quick_btn")
        ) {
          Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Midnight, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          val label = if (openChallengesCount > 0) "Challenges ($openChallengesCount)" else "Challenges"
          Text(label, style = ButtonTextStyle.copy(fontSize = 14.sp, color = Midnight), maxLines = 1)
        }
        OutlinedButton(
          onClick = onOpenLeaderboard,
          shape = RoundedCornerShape(ButtonRadius),
          border = androidx.compose.foundation.BorderStroke(1.dp, MetallicGold.copy(alpha = 0.65f)),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("open_leaderboard_quick_btn")
        ) {
          Icon(Icons.Default.Leaderboard, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Leaderboard", style = ButtonTextStyle.copy(fontSize = 14.sp, color = ChampagneGold), maxLines = 1)
        }
      }
    }
  }
}

@Composable
private fun StatTile(
  label: String,
  value: String,
  sub: String,
  icon: ImageVector,
  tint: Color,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(RoyalSurfaceHigh)
      .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
      Spacer(modifier = Modifier.width(5.dp))
      Text(text = label, style = CaptionStyle.copy(color = Lavender))
    }
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = value,
      fontFamily = PlayfairDisplayFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 22.sp,
      color = SilverWhite
    )
    Text(text = sub, style = CaptionStyle.copy(color = tint))
  }
}
