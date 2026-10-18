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
import com.example.data.DiscussionGroupItem
import com.example.ui.theme.CardRadius
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.PoppinsFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GroupCard(
  group: DiscussionGroupItem,
  onOpenChat: () -> Unit,
  onJoinGroup: () -> Unit
) {
  ClassRepCard(
    modifier = Modifier
      .then(if (group.isMember) Modifier.border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(CardRadius)) else Modifier)
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
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, if (group.isMember) GoldPrimary else DarkBorder, RoundedCornerShape(10.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Groups, contentDescription = null, tint = GoldLight, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = group.name,
              fontFamily = PoppinsFamily,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            if (group.isFull) {
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(ErrorContainer)
                  .padding(horizontal = 6.dp, vertical = 2.dp)
                  .testTag("group_full_badge_${group.id}")
              ) {
                Text("Full", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
              }
            }
          }

          Text(
            text = "${group.memberCount}/${group.maxMembers} members",
            fontSize = 13.sp,
            color = TextSecondary
          )
        }
      }

      if (group.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = group.description,
          fontSize = 13.sp,
          color = TextSecondary,
          lineHeight = 18.sp,
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
            modifier = Modifier.height(38.dp).testTag("open_chat_btn_${group.id}")
          )
        } else {
          Button(
            onClick = onJoinGroup,
            enabled = !group.isFull,
            modifier = Modifier.height(38.dp).testTag("join_group_btn_${group.id}"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = GoldLight)
          ) {
            Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (group.isFull) "Group Full" else "Join", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}
