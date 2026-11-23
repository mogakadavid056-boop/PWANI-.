package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.ScreenTitleStyle

@Composable
fun HomeIdentityHeader(
  displayName: String,
  displayRole: String,
  onNavigateToProfile: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .weight(1f)
        .clickable(onClick = onNavigateToProfile)
        .testTag("home_profile_header")
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(RoyalSurfaceHigh)
          .border(1.5.dp, MetallicGold, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        RoyalCrownIcon(size = 24.dp, tint = ChampagneGold)
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        MetallicGoldText(
          text = "ClassRep",
          style = ScreenTitleStyle.copy(fontSize = 20.sp, lineHeight = 24.sp),
          maxLines = 1
        )
        Text(
          text = displayName,
          style = CaptionStyle.copy(color = Lavender),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(
        onClick = onNavigateToProfile,
        modifier = Modifier.size(48.dp).testTag("home_profile_button")
      ) {
        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = ChampagneGold, modifier = Modifier.size(26.dp))
      }
      Spacer(modifier = Modifier.width(4.dp))
      RoleBadge(
        roleText = displayRole,
        modifier = Modifier.testTag("home_user_role")
      )
    }
  }
}
