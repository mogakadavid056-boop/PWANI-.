package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminUserItem
import com.example.ui.theme.CardTitleStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextSecondary
import com.example.util.JsonHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminUserRow(
  user: AdminUserItem,
  onClick: () -> Unit,
  onToggleVerification: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val isSuperAdmin = user.isAdmin
  val isLecturer = user.isLecturer
  val isClassRep = user.isClassRep
  val badgeColor = when {
    isSuperAdmin -> DarkTextOnGold
    isClassRep -> ElectricViolet
    isLecturer -> ChampagneGold
    else -> TextSecondary
  }
  val badgeBg = if (isSuperAdmin) GoldPrimary else DarkSurfaceVariant

  ClassRepCard(
    modifier = modifier
      .then(if (isSuperAdmin) Modifier.border(1.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(16.dp)) else Modifier)
      .testTag("admin_user_row_${user.id}"),
    onClick = onClick
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(DarkSurfaceVariant)
          .border(1.dp, if (isSuperAdmin) GoldPrimary else DarkBorder, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        val icon = when {
          isSuperAdmin -> Icons.Default.Security
          isLecturer -> Icons.Default.School
          isClassRep -> Icons.Default.Stars
          else -> Icons.Default.Person
        }
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isSuperAdmin) GoldPrimary else badgeColor,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = user.fullName.ifBlank { "Unnamed User" },
          style = CardTitleStyle
        )

        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(badgeBg)
              .border(1.dp, if (isSuperAdmin) GoldPrimary else DarkBorder, RoundedCornerShape(12.dp))
              .padding(horizontal = 8.dp, vertical = 3.dp)
              .testTag("admin_user_role_badge_${user.id}")
          ) {
            Text(
              text = user.displayRole,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = badgeColor
            )
          }

          if (isLecturer) {
            StatusChip(
              text = if (user.isVerified) "Active" else "Suspended",
              color = if (user.isVerified) SuccessGreen else ChampagneGold,
              tag = "admin_user_verified_badge_${user.id}"
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (user.email.isNotBlank()) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = user.email, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
          }
        }

        if (user.admissionNumber.isNotBlank()) {
          Spacer(modifier = Modifier.height(2.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Badge, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Adm: ${JsonHelper.orDash(user.admissionNumber)}", fontSize = 12.sp, color = TextSecondary)
          }
        }

        if (isLecturer) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(
              onClick = onToggleVerification,
              modifier = Modifier.height(48.dp).testTag("admin_verify_lecturer_btn_${user.id}"),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
              border = BorderStroke(1.dp, if (user.isVerified) ChampagneGold else GoldPrimary)
            ) {
              Text(
                text = if (user.isVerified) "Suspend Access" else "Restore Access",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (user.isVerified) ChampagneGold else GoldLight
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StatusChip(text: String, color: Color, tag: String) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(color.copy(alpha = 0.14f))
      .border(1.dp, color, RoundedCornerShape(10.dp))
      .padding(horizontal = 7.dp, vertical = 2.dp)
      .testTag(tag)
  ) {
    Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
  }
}
