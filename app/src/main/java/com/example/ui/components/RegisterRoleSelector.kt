package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun RegisterRoleSelector(
  isLecturerMode: Boolean,
  onSelectStudent: () -> Unit,
  onSelectLecturer: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text("I am a:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(modifier = Modifier.width(12.dp))
    Row(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(10.dp))
        .background(DarkSurfaceVariant)
        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
        .padding(3.dp)
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(8.dp))
          .background(if (!isLecturerMode) GoldPrimary else DarkSurfaceVariant)
          .clickable { onSelectStudent() }
          .padding(vertical = 8.dp)
          .testTag("role_choice_student"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Student",
          fontSize = 13.sp,
          fontWeight = if (!isLecturerMode) FontWeight.Bold else FontWeight.Normal,
          color = if (!isLecturerMode) DarkTextOnGold else TextSecondary
        )
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(8.dp))
          .background(if (isLecturerMode) GoldPrimary else DarkSurfaceVariant)
          .clickable { onSelectLecturer() }
          .padding(vertical = 8.dp)
          .testTag("role_choice_lecturer"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Lecturer",
          fontSize = 13.sp,
          fontWeight = if (isLecturerMode) FontWeight.Bold else FontWeight.Normal,
          color = if (isLecturerMode) DarkTextOnGold else TextSecondary
        )
      }
    }
  }
}

@Composable
fun LecturerRegisterFields(
  staffNumber: String,
  onStaffNumberChange: (String) -> Unit,
  email: String,
  onEmailChange: (String) -> Unit,
  password: String,
  onPasswordChange: (String) -> Unit,
  lecturerPasscode: String,
  onLecturerPasscodeChange: (String) -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    DarkTextField(
      value = staffNumber, onValueChange = onStaffNumberChange, label = "Staff Number (Optional)",
      leadingIcon = Icons.Default.Badge, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
      testTag = "register_staff_number_input"
    )
    Spacer(modifier = Modifier.height(12.dp))

    DarkTextField(
      value = email, onValueChange = onEmailChange, label = "University Email *",
      leadingIcon = Icons.Default.Email, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
      testTag = "register_email_input"
    )
    Spacer(modifier = Modifier.height(12.dp))

    PasswordTextField(
      value = password, onValueChange = onPasswordChange, label = "Password *",
      leadingIcon = Icons.Default.Lock, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
      testTag = "register_password_input"
    )
    Spacer(modifier = Modifier.height(12.dp))

    PasswordTextField(
      value = lecturerPasscode, onValueChange = onLecturerPasscodeChange, label = "Lecturer passcode *",
      leadingIcon = Icons.Default.VpnKey, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
      testTag = "register_lecturer_passcode_input"
    )
  }
}
