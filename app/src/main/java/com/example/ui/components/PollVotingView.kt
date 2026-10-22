package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PollOption
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PollVotingView(
  pollOptions: List<PollOption>,
  selectedOptionId: String?,
  isActionLoading: Boolean,
  onSelectOption: (String) -> Unit,
  onCastVote: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(text = "Cast Your Vote", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    Spacer(modifier = Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      pollOptions.forEach { opt ->
        val isSelected = selectedOptionId == opt.id
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCard)
            .border(1.dp, if (isSelected) GoldPrimary else DarkBorder, RoundedCornerShape(10.dp))
            .clickable { onSelectOption(opt.id) }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("poll_option_${opt.id}"),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RadioButton(
            selected = isSelected,
            onClick = { onSelectOption(opt.id) },
            colors = RadioButtonDefaults.colors(
              selectedColor = GoldPrimary,
              unselectedColor = TextSecondary
            )
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = opt.optionText,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = TextPrimary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(DarkSurface)
        .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(Icons.Default.Info, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Note: Your vote is final and cannot be changed.", fontSize = 12.sp, color = TextSecondary)
    }

    Spacer(modifier = Modifier.height(14.dp))

    val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Button(
      onClick = {
        if (!isOnline) {
          OfflineToastHelper.show(context)
        } else {
          onCastVote()
        }
      },
      enabled = if (!isOnline) true else (!selectedOptionId.isNullOrBlank() && !isActionLoading),
      shape = RoundedCornerShape(10.dp),
      colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
      modifier = Modifier.fillMaxWidth().height(48.dp)
        .then(if (!isOnline) Modifier.alpha(0.5f) else Modifier)
        .testTag("cast_vote_button")
    ) {
      if (isActionLoading) {
        CircularProgressIndicator(color = DarkTextOnGold, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
      } else {
        Text("Vote", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold)
      }
    }
  }
}
