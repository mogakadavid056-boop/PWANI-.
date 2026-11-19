package com.example.ui.screens.register

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RegisterStep1Role(
  selectedRole: RegisterWizardRole?,
  onSelectRole: (RegisterWizardRole) -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = "Who are you?",
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      color = GoldLight
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = "Choose how you will use ClassRep.",
      fontSize = 14.sp,
      color = TextSecondary
    )
    Spacer(modifier = Modifier.height(22.dp))

    RoleOptionCard(
      title = "Student",
      subtitle = "Join your class",
      description = "View notices, sign attendance registers, access timetables and class materials.",
      icon = Icons.Default.Groups,
      selected = selectedRole == RegisterWizardRole.STUDENT,
      onClick = { onSelectRole(RegisterWizardRole.STUDENT) },
      testTag = "register_role_student"
    )

    Spacer(modifier = Modifier.height(16.dp))

    RoleOptionCard(
      title = "Lecturer",
      subtitle = "Manage your classes",
      description = "Start attendance sessions, sign registers, post announcements and manage units.",
      icon = Icons.Default.School,
      selected = selectedRole == RegisterWizardRole.LECTURER,
      onClick = { onSelectRole(RegisterWizardRole.LECTURER) },
      testTag = "register_role_lecturer"
    )
  }
}

@Composable
private fun RoleOptionCard(
  title: String,
  subtitle: String,
  description: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  val borderColor = if (selected) GoldPrimary else DarkBorder
  val borderWidth = if (selected) 2.dp else 1.dp
  val bgColor = if (selected) GoldPrimary.copy(alpha = 0.14f) else DarkCard

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(bgColor)
      .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .padding(20.dp)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (selected) GoldPrimary else DarkSurface)
            .border(1.dp, GoldPrimary.copy(alpha = 0.6f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (selected) com.example.ui.theme.DarkTextOnGold else GoldPrimary,
            modifier = Modifier.size(28.dp)
          )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
          Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) GoldLight else TextPrimary
          )
          Text(
            text = subtitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = GoldPrimary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = description,
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 17.sp
          )
        }
      }
      if (selected) {
        Spacer(modifier = Modifier.width(10.dp))
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = "Selected",
          tint = GoldPrimary,
          modifier = Modifier.size(26.dp)
        )
      }
    }
  }
}
