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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.defaultMinSize
import com.example.data.DiscussionGroupItem
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardRadius
import com.example.ui.theme.CardTitleStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RoyalSurfaceHigh

@Composable
fun GroupCard(
  group: DiscussionGroupItem,
  onOpenChat: () -> Unit,
  onJoinGroup: () -> Unit
) {
  ClassRepCard(
    modifier = Modifier
      .then(if (group.isMember) Modifier.border(1.dp, ElectricViolet.copy(alpha = 0.55f), RoundedCornerShape(CardRadius)) else Modifier)
      .testTag("group_card_${group.id}"),
    onClick = if (group.isMember) onOpenChat else null
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(RoyalSurfaceHigh)
            .border(1.dp, if (group.isMember) ElectricViolet else DarkBorder, RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Groups, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = group.name,
              style = CardTitleStyle,
              modifier = Modifier.weight(1f, fill = false)
            )
            if (group.isFull) {
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(ErrorContainer)
                  .padding(horizontal = 8.dp, vertical = 2.dp)
                  .testTag("group_full_badge_${group.id}")
              ) {
                Text("Full", style = FieldLabelStyle.copy(fontSize = 12.sp, color = ErrorColor))
              }
            }
          }

          Text(
            text = "${group.memberCount}/${group.maxMembers} members",
            style = CaptionStyle
          )
        }
      }

      if (group.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = group.description,
          style = CaptionStyle,
          maxLines = 2
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        if (group.isMember) {
          PrimaryButton(
            text = "Open Chat",
            leadingIcon = Icons.AutoMirrored.Filled.Chat,
            onClick = onOpenChat,
            modifier = Modifier.testTag("open_chat_btn_${group.id}")
          )
        } else {
          Button(
            onClick = onJoinGroup,
            enabled = !group.isFull,
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("join_group_btn_${group.id}"),
            shape = RoundedCornerShape(ButtonRadius),
            colors = ButtonDefaults.buttonColors(containerColor = RoyalSurfaceHigh, contentColor = ChampagneGold),
            border = androidx.compose.foundation.BorderStroke(1.dp, MetallicGold.copy(alpha = 0.55f))
          ) {
            Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (group.isFull) "Group Full" else "Join", style = ButtonTextStyle.copy(fontSize = 14.sp, color = ChampagneGold))
          }
        }
      }
    }
  }
}
