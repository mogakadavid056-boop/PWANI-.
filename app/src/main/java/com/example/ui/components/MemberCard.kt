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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassMemberItem
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardTitleStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SilverWhite
import com.example.util.RoleHelper

@Composable
fun MemberCard(
  member: ClassMemberItem,
  canManageReps: Boolean,
  onAppointRep: () -> Unit,
  onRemoveRep: () -> Unit,
  onRemoveFromClass: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isLecturer = member.memberRole.equals("lecturer", ignoreCase = true)
  val isPrefect = member.isRep || member.memberRole.equals("prefect", ignoreCase = true) || member.memberRole.equals("rep", ignoreCase = true)
  val badgeRole = when {
    isLecturer -> RoleHelper.ROLE_LECTURER
    isPrefect -> RoleHelper.ROLE_CLASS_REP
    else -> RoleHelper.ROLE_STUDENT
  }

  RoyalCard(
    borderColor = if (isPrefect) ElectricViolet else null,
    modifier = modifier.testTag("member_card_${member.userId}"),
    onClick = {}
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(RoyalSurfaceHigh)
            .border(1.dp, if (isPrefect) ElectricViolet else DarkBorder, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          when {
            isPrefect -> VioletGemIcon(size = 18.dp, tint = ElectricViolet)
            isLecturer -> Icon(Icons.Default.School, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(20.dp))
            else -> Icon(Icons.Default.Person, contentDescription = null, tint = Lavender, modifier = Modifier.size(20.dp))
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = member.fullName.ifBlank { "Member" },
              style = CardTitleStyle.copy(color = SilverWhite),
              modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            RoleBadge(
              role = badgeRole,
              testTag = "member_role_badge_${member.userId}"
            )
          }
          if (member.admissionNumber.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Adm: ${member.admissionNumber}",
              style = CaptionStyle.copy(color = Lavender)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (canManageReps && !isLecturer) {
          if (isPrefect) {
            OutlinedButton(
              onClick = onRemoveRep,
              modifier = Modifier.height(40.dp).testTag("remove_rep_btn_${member.userId}"),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Remove Prefect", style = ButtonTextStyle.copy(fontSize = 13.sp, color = Lavender))
            }
          } else {
            Button(
              onClick = onAppointRep,
              modifier = Modifier.height(40.dp).testTag("make_rep_btn_${member.userId}"),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight)
            ) {
              Text("Make Prefect", style = ButtonTextStyle.copy(fontSize = 13.sp, color = Midnight))
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
        }

        IconButton(
          onClick = onRemoveFromClass,
          modifier = Modifier.size(40.dp).testTag("remove_from_class_btn_${member.userId}")
        ) {
          Icon(Icons.Default.Delete, contentDescription = "Remove from class", tint = ErrorRed, modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}
