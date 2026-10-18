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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GroupMemberItem
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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
      .clip(RoundedCornerShape(10.dp))
      .background(DarkSurface)
      .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
      .padding(12.dp)
      .testTag("group_member_${member.userId}")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(DarkSurfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(member.fullName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
          if (member.isOwner) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(GoldPrimary)
                .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
              Text("Owner", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = DarkTextOnGold)
            }
          }
        }
        if (member.admissionNumber.isNotBlank()) {
          Text(member.admissionNumber, fontSize = 13.sp, color = TextSecondary)
        }
      }

      if (canManageGroup && !isSelf) {
        IconButton(
          onClick = onRemove,
          modifier = Modifier.size(32.dp).testTag("remove_group_member_btn_${member.userId}")
        ) {
          Icon(Icons.Default.PersonRemove, contentDescription = "Remove", tint = ErrorRed, modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}
