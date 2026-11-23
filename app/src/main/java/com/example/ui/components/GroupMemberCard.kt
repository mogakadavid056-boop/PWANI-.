package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GroupMemberItem
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardTitleStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.Midnight
import com.example.ui.theme.RoyalSurface
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SilverWhite

@Composable
fun GroupMemberCard(
  member: GroupMemberItem,
  canManageGroup: Boolean,
  isSelf: Boolean,
  onRemove: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(RoyalSurface)
      .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
      .padding(12.dp)
      .testTag("group_member_${member.userId}")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(RoyalSurfaceHigh),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.Person, contentDescription = null, tint = Lavender, modifier = Modifier.size(18.dp))
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(member.fullName, style = CardTitleStyle.copy(fontSize = 16.sp, color = SilverWhite))
          if (member.isOwner) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(ChampagneGold)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text("Owner", style = FieldLabelStyle.copy(fontSize = 11.sp, color = Midnight))
            }
          }
        }
        if (member.admissionNumber.isNotBlank()) {
          Text(member.admissionNumber, style = CaptionStyle.copy(color = Lavender))
        }
      }

      if (canManageGroup && !isSelf) {
        IconButton(
          onClick = onRemove,
          modifier = Modifier.size(40.dp).testTag("remove_group_member_btn_${member.userId}")
        ) {
          Icon(Icons.Default.PersonRemove, contentDescription = "Remove", tint = ErrorRed, modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}
