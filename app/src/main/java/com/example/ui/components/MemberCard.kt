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
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import com.example.data.ClassMemberItem
import com.example.ui.theme.*

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
  val badgeLabel = when {
    isLecturer -> "Lecturer"
    isPrefect -> "Prefect"
    else -> "Student"
  }
  val badgeBg = when {
    isLecturer -> GoldLight.copy(alpha = 0.2f)
    isPrefect -> GoldPrimary
    else -> DarkSurfaceVariant
  }
  val badgeBorder = when {
    isLecturer -> GoldLight
    isPrefect -> GoldPrimary
    else -> DarkBorder
  }
  val badgeTextColor = when {
    isLecturer -> GoldLight
    isPrefect -> DarkTextOnGold
    else -> TextSecondary
  }

  ClassRepCard(
    modifier = modifier
      .then(if (isPrefect) Modifier.border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)) else Modifier)
      .testTag("member_card_${member.userId}"),
    onClick = {}
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(DarkSurfaceVariant)
            .border(1.dp, if (isPrefect) GoldPrimary else DarkBorder, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            if (isPrefect) Icons.Default.Star else Icons.Default.Person,
            contentDescription = null,
            tint = if (isPrefect) GoldPrimary else TextSecondary,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = member.fullName.ifBlank { "Member" },
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeBg)
                .border(1.dp, badgeBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("member_role_badge_${member.userId}")
            ) {
              Text(badgeLabel, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = badgeTextColor)
            }
          }
          if (member.admissionNumber.isNotBlank()) {
            Text(
              text = "Adm: ${member.admissionNumber}",
              fontSize = 13.sp,
              color = TextSecondary
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
              modifier = Modifier.height(34.dp).testTag("remove_rep_btn_${member.userId}"),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Remove Prefect", fontSize = 11.sp, color = TextSecondary)
            }
          } else {
            Button(
              onClick = onAppointRep,
              modifier = Modifier.height(34.dp).testTag("make_rep_btn_${member.userId}"),
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
            ) {
              Text("Make Prefect", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold)
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
        }

        IconButton(
          onClick = onRemoveFromClass,
          modifier = Modifier.size(34.dp).testTag("remove_from_class_btn_${member.userId}")
        ) {
          Icon(Icons.Default.Delete, contentDescription = "Remove from class", tint = ErrorRed, modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}
