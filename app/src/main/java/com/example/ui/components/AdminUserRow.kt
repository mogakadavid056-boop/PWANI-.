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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminUserItem
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AdminUserRow(
  user: AdminUserItem,
  onClick: () -> Unit,
  onToggleVerification: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val isSuperAdmin = user.role.equals("admin", ignoreCase = true)
  val isLecturer = user.role.equals("lecturer", ignoreCase = true)
  val badgeColor = when {
    isSuperAdmin -> com.example.ui.theme.DarkTextOnGold
    isLecturer -> GoldLight
    else -> TextSecondary
  }
  val badgeBg = when {
    isSuperAdmin -> GoldPrimary
    isLecturer -> DarkSurfaceVariant
    else -> DarkSurfaceVariant
  }

  ClassRepCard(
    modifier = modifier
      .then(if (isSuperAdmin) Modifier.border(1.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(16.dp)) else Modifier)
      .testTag("admin_user_row_${user.id}"),
    onClick = onClick
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // User Avatar Circle
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

      // User details
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = user.fullName.ifBlank { "Unnamed User" },
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.weight(1f, fill = false)
          )

          Spacer(modifier = Modifier.width(8.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            // Role badge
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(badgeBg)
                .border(1.dp, if (isSuperAdmin) GoldPrimary else DarkBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .testTag("admin_user_role_badge_${user.id}")
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSuperAdmin) {
                  Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                  text = user.displayRole,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = badgeColor
                )
              }
            }

            if (isLecturer) {
              val verifiedBg = if (user.isVerified) androidx.compose.ui.graphics.Color(0x224CAF50) else androidx.compose.ui.graphics.Color(0x22FF9800)
              val verifiedBorder = if (user.isVerified) androidx.compose.ui.graphics.Color(0xFF4CAF50) else androidx.compose.ui.graphics.Color(0xFFFF9800)
              val verifiedTextColor = if (user.isVerified) androidx.compose.ui.graphics.Color(0xFF81C784) else androidx.compose.ui.graphics.Color(0xFFFFB74D)

              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .background(verifiedBg)
                  .border(1.dp, verifiedBorder, RoundedCornerShape(10.dp))
                  .padding(horizontal = 7.dp, vertical = 2.dp)
                  .testTag("admin_user_verified_badge_${user.id}")
              ) {
                Text(
                  text = if (user.isVerified) "Verified" else "Not verified",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = verifiedTextColor
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Email row
        if (user.email.isNotBlank()) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Email,
              contentDescription = null,
              tint = TextSecondary,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = user.email,
              fontSize = 12.sp,
              color = TextSecondary,
              maxLines = 1
            )
          }
        }

        // Admission number row
        if (user.admissionNumber.isNotBlank()) {
          Spacer(modifier = Modifier.height(2.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Badge,
              contentDescription = null,
              tint = TextSecondary,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Adm: ${user.admissionNumber}",
              fontSize = 12.sp,
              color = TextSecondary
            )
          }
        }

        if (isLecturer) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            androidx.compose.material3.OutlinedButton(
              onClick = onToggleVerification,
              modifier = Modifier
                .height(30.dp)
                .testTag("admin_verify_lecturer_btn_${user.id}"),
              shape = RoundedCornerShape(8.dp),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, if (user.isVerified) androidx.compose.ui.graphics.Color(0xFFFF9800) else GoldPrimary)
            ) {
              Text(
                text = if (user.isVerified) "Remove verification" else "Verify",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (user.isVerified) androidx.compose.ui.graphics.Color(0xFFFFB74D) else GoldLight
              )
            }
          }
        }
      }
    }
  }
}
