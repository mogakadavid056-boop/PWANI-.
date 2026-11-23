package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.Lavender
import com.example.ui.theme.ScreenTitleStyle

@Composable
fun AdminTopBar(
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.size(48.dp).testTag("admin_back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = ChampagneGold
        )
      }
      Spacer(modifier = Modifier.width(4.dp))
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          MetallicGoldText(
            text = "Manage Users",
            style = ScreenTitleStyle.copy(fontSize = 22.sp, lineHeight = 28.sp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          RoyalCrownIcon(size = 20.dp)
        }
        Text(
          text = "Super Admin Portal",
          style = CaptionStyle.copy(color = Lavender)
        )
      }
    }

    IconButton(
      onClick = onRefresh,
      modifier = Modifier.size(48.dp).testTag("admin_refresh_button")
    ) {
      Icon(
        imageVector = Icons.Default.Refresh,
        contentDescription = "Refresh Users",
        tint = ChampagneGold
      )
    }
  }
}
