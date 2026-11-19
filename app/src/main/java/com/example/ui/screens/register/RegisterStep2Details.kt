package com.example.ui.screens.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DarkTextField
import com.example.ui.components.PasswordTextField
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextSecondary
import com.example.util.PasswordStrength
import com.example.util.PasswordStrengthHelper

@Composable
fun RegisterStep2Details(
  state: RegisterWizardState,
  onUpdate: ((RegisterWizardState) -> RegisterWizardState) -> Unit
) {
  val nameValid = PasswordStrengthHelper.isValidName(state.fullName)
  val idValid = state.isLecturer || state.idNumber.trim().isNotBlank()
  val emailValid = PasswordStrengthHelper.isValidEmail(state.email)
  val passwordValid = PasswordStrengthHelper.isValidPassword(state.password)
  val confirmValid = PasswordStrengthHelper.passwordsMatch(state.password, state.confirmPassword)
  val strength = PasswordStrengthHelper.evaluateStrength(state.password)

  Column(modifier = Modifier.fillMaxWidth()) {
    Text("Your details", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GoldLight)
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = if (state.isLecturer) "Enter your staff and contact details." else "Enter your student and contact details.",
      fontSize = 14.sp,
      color = TextSecondary
    )
    Spacer(modifier = Modifier.height(18.dp))

    DarkTextField(
      value = state.fullName,
      onValueChange = { v -> onUpdate { it.copy(fullName = v, nameTouched = true) } },
      label = "Full Name *",
      leadingIcon = Icons.Default.Person,
      keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
      modifier = Modifier.onFocusChanged { f -> if (!f.isFocused && state.fullName.isNotEmpty()) onUpdate { it.copy(nameTouched = true) } },
      testTag = "register_fullname_input"
    )
    FieldRuleText(
      rule = "At least 3 characters",
      isValid = nameValid,
      showError = state.nameTouched && !nameValid
    )
    Spacer(modifier = Modifier.height(10.dp))

    DarkTextField(
      value = state.idNumber,
      onValueChange = { v -> onUpdate { it.copy(idNumber = v, idTouched = true) } },
      label = if (state.isLecturer) "Staff Number (optional)" else "Admission Number *",
      leadingIcon = Icons.Default.Badge,
      keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
      modifier = Modifier.onFocusChanged { f -> if (!f.isFocused && state.idNumber.isNotEmpty()) onUpdate { it.copy(idTouched = true) } },
      testTag = "register_admission_input"
    )
    if (!state.isLecturer) {
      FieldRuleText(
        rule = "Required for class placement and attendance",
        isValid = idValid,
        showError = state.idTouched && !idValid
      )
    }
    Spacer(modifier = Modifier.height(10.dp))

    DarkTextField(
      value = state.email,
      onValueChange = { v -> onUpdate { it.copy(email = v, emailTouched = true) } },
      label = "University Email *",
      leadingIcon = Icons.Default.Email,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
      modifier = Modifier.onFocusChanged { f -> if (!f.isFocused && state.email.isNotEmpty()) onUpdate { it.copy(emailTouched = true) } },
      testTag = "register_email_input"
    )
    FieldRuleText(
      rule = "Enter a valid email address",
      isValid = emailValid,
      showError = state.emailTouched && !emailValid
    )
    Spacer(modifier = Modifier.height(10.dp))

    DarkTextField(
      value = state.phone,
      onValueChange = { v -> onUpdate { it.copy(phone = v) } },
      label = "Phone Number (optional)",
      leadingIcon = Icons.Default.Phone,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
      testTag = "register_phone_input"
    )
    Spacer(modifier = Modifier.height(12.dp))

    PasswordTextField(
      value = state.password,
      onValueChange = { v -> onUpdate { it.copy(password = v, passwordTouched = true) } },
      label = "Password *",
      leadingIcon = Icons.Default.Lock,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
      modifier = Modifier.onFocusChanged { f -> if (!f.isFocused && state.password.isNotEmpty()) onUpdate { it.copy(passwordTouched = true) } },
      testTag = "register_password_input"
    )
    FieldRuleText(
      rule = "At least 8 characters with a letter and a number",
      isValid = passwordValid,
      showError = state.passwordTouched && !passwordValid
    )

    if (state.password.isNotEmpty()) {
      Spacer(modifier = Modifier.height(6.dp))
      PasswordStrengthBar(strength = strength)
    }

    Spacer(modifier = Modifier.height(10.dp))

    PasswordTextField(
      value = state.confirmPassword,
      onValueChange = { v -> onUpdate { it.copy(confirmPassword = v, confirmTouched = true) } },
      label = "Confirm Password *",
      leadingIcon = Icons.Default.Lock,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
      modifier = Modifier.onFocusChanged { f -> if (!f.isFocused && state.confirmPassword.isNotEmpty()) onUpdate { it.copy(confirmTouched = true) } },
      testTag = "register_confirm_password_input"
    )
    FieldRuleText(
      rule = "Passwords must match",
      isValid = confirmValid,
      showError = state.confirmTouched && !confirmValid
    )
  }
}

@Composable
private fun FieldRuleText(rule: String, isValid: Boolean, showError: Boolean) {
  val color = when {
    showError -> ErrorRed
    isValid -> SuccessGreen
    else -> TextSecondary
  }
  Text(
    text = rule,
    fontSize = 12.sp,
    color = color,
    fontWeight = if (showError) FontWeight.Medium else FontWeight.Normal,
    modifier = Modifier.padding(start = 6.dp, top = 4.dp)
  )
}

@Composable
private fun PasswordStrengthBar(strength: PasswordStrength) {
  val color = when (strength) {
    PasswordStrength.WEAK -> ErrorRed
    PasswordStrength.OK -> GoldPrimary
    PasswordStrength.STRONG -> SuccessGreen
  }
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    for (bar in 1..3) {
      Box(
        modifier = Modifier
          .weight(1f)
          .height(5.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(if (bar <= strength.level) color else DarkSurface)
      )
    }
    Text(
      text = strength.label,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      color = color,
      modifier = Modifier.padding(start = 4.dp)
    )
  }
}
