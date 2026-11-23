package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LeaderboardEntry
import com.example.data.LeaderboardPeriod
import com.example.ui.ChallengesUiState
import com.example.ui.components.ActivityStatsSummaryCard
import com.example.ui.components.EmptyState
import com.example.ui.components.FriendlyErrorSnackbar
import com.example.ui.components.LoadingSkeleton
import com.example.ui.components.RoyalCard
import com.example.ui.components.RoyalCrownIcon
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardRadius
import com.example.ui.theme.CardTitleStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.NavLabelStyle
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.SilverWhite

@Composable
fun LeaderboardSection(
  uiState: ChallengesUiState,
  showPrivacyToggle: Boolean,
  showOnLeaderboard: Boolean,
  onToggleVisibility: (Boolean) -> Unit,
  onSelectPeriod: (LeaderboardPeriod) -> Unit,
  onRefresh: () -> Unit,
  onDismissError: () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxWidth().testTag("leaderboard_section"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    ActivityStatsSummaryCard(
      stats = uiState.activityStats,
      isLoading = uiState.isLoadingStats,
      openChallengesCount = uiState.openCount,
      showQuickActions = false
    )

    if (!uiState.errorMessage.isNullOrBlank()) {
      FriendlyErrorSnackbar(errorMessage = uiState.errorMessage, onDismiss = onDismissError)
    }

    if (showPrivacyToggle) {
      RoyalCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = if (showOnLeaderboard) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription = null,
              tint = ChampagneGold,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Show my name on leaderboard", style = FieldLabelStyle.copy(color = SilverWhite))
              Text(
                text = if (showOnLeaderboard) "Classmates see your name and rank" else "Hidden as Anonymous to classmates",
                style = CaptionStyle
              )
            }
          }
          Switch(
            checked = showOnLeaderboard,
            onCheckedChange = onToggleVisibility,
            modifier = Modifier.testTag("leaderboard_visibility_switch"),
            colors = SwitchDefaults.colors(
              checkedThumbColor = Midnight,
              checkedTrackColor = ChampagneGold,
              uncheckedThumbColor = Lavender,
              uncheckedTrackColor = RoyalSurfaceHigh
            )
          )
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text("Class Standings", style = SectionHeadingStyle)
      IconButton(
        onClick = onRefresh,
        modifier = Modifier.size(48.dp).testTag("refresh_leaderboard_btn")
      ) {
        Icon(Icons.Default.Refresh, contentDescription = "Refresh leaderboard", tint = ChampagneGold)
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      LeaderboardPeriod.entries.forEach { period ->
        val selected = uiState.leaderboardPeriod == period
        FilterChip(
          selected = selected,
          onClick = { onSelectPeriod(period) },
          label = { Text(period.label, style = NavLabelStyle) },
          modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("leaderboard_period_${period.apiValue}"),
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

    if (uiState.isLoadingLeaderboard && uiState.leaderboardRows.isEmpty()) {
      LoadingSkeleton(height = 70)
      LoadingSkeleton(height = 70)
      LoadingSkeleton(height = 70)
    } else if (uiState.leaderboardRows.isEmpty()) {
      EmptyState(
        icon = Icons.Default.EmojiEvents,
        title = "No Points Yet",
        message = "Earn +10 pts for attendance, +2 pts per poll vote, and finish challenges for bonus points!",
        modifier = Modifier.testTag("leaderboard_empty_state")
      )
    } else {
      uiState.leaderboardRows.forEach { entry ->
        LeaderboardRowCard(entry = entry)
      }
    }
  }
}

@Composable
private fun LeaderboardRowCard(entry: LeaderboardEntry) {
  val isTopThree = entry.rankNo in 1..3
  val shape = RoundedCornerShape(CardRadius)
  val borderColor = when {
    entry.isMe -> ElectricViolet
    entry.rankNo == 1 -> ChampagneGold
    isTopThree -> MetallicGold.copy(alpha = 0.65f)
    else -> DarkBorder
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .then(if (entry.isMe) Modifier.shadow(8.dp, shape, ambientColor = ElectricViolet, spotColor = ElectricViolet) else Modifier)
      .clip(shape)
      .background(if (entry.isMe || entry.rankNo == 1) RoyalSurfaceHigh else com.example.ui.theme.RoyalCardSurface)
      .border(if (entry.isMe || entry.rankNo == 1) 1.5.dp else 1.dp, borderColor, shape)
      .padding(horizontal = 14.dp, vertical = 12.dp)
      .testTag("leaderboard_row_${entry.rankNo}"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(
            when (entry.rankNo) {
              1 -> ChampagneGold.copy(alpha = 0.22f)
              2, 3 -> MetallicGold.copy(alpha = 0.16f)
              else -> RoyalSurfaceHigh
            }
          )
          .border(1.dp, borderColor.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        if (entry.rankNo == 1) {
          RoyalCrownIcon(size = 22.dp, tint = ChampagneGold)
        } else {
          Text(
            text = "#${entry.rankNo}",
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = if (isTopThree) ChampagneGold else Lavender
          )
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = entry.displayName,
            style = CardTitleStyle.copy(
              fontSize = 17.sp,
              color = if (entry.isMe) ChampagneGold else SilverWhite
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (entry.isMe) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(ElectricViolet.copy(alpha = 0.28f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text("YOU", style = FieldLabelStyle.copy(fontSize = 12.sp, color = SilverWhite))
            }
          }
        }
        if (entry.rankNo == 1 && entry.points > 0) {
          Text("Leading this period", style = CaptionStyle.copy(color = ChampagneGold))
        }
      }
    }

    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = "${entry.points} pts",
      style = BodyStyle.copy(fontWeight = FontWeight.Bold, color = ChampagneGold)
    )
  }
}
