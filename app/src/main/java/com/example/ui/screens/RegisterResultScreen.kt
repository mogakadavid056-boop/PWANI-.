package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RegisterResultState
import com.example.ui.components.DarkTextField
import com.example.ui.components.ErrorBanner
import com.example.ui.components.GoldButton
import com.example.ui.components.GoldOutlinedButton
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RegisterResultScreen(
  result: RegisterResultState,
  isLoading: Boolean,
  errorMessage: String?,
  onDismissError: () -> Unit,
  onOpenMyClass: (String) -> Unit,
  onRetryAutoJoin: () -> Unit,
  onJoinWithCode: (String) -> Unit,
  onOpenFindClasses: () -> Unit,
  onOpenProfile: () -> Unit,
  onContinueHome: () -> Unit
) {
  var showCodeInput by remember { mutableStateOf(false) }
  var code by remember { mutableStateOf("") }

  val icon = when (result) {
    is RegisterResultState.StudentPlaced -> Icons.Default.CheckCircle
    is RegisterResultState.StudentNotPlaced -> Icons.Default.HourglassTop
    is RegisterResultState.LecturerPasscodeRight -> Icons.Default.School
    is RegisterResultState.LecturerPasscodeWrong -> Icons.Default.WarningAmber
  }
  val iconTint = when (result) {
    is RegisterResultState.StudentPlaced, is RegisterResultState.LecturerPasscodeRight -> SuccessGreen
    else -> GoldPrimary
  }

  Box(modifier = Modifier.fillMaxSize().background(DarkBackground).testTag("register_result_screen")) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(88.dp)
          .clip(CircleShape)
          .background(iconTint.copy(alpha = 0.14f))
          .border(2.dp, iconTint.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(44.dp))
      }

      Spacer(modifier = Modifier.height(22.dp))

      when (result) {
        is RegisterResultState.StudentPlaced -> {
          Text(
            text = "Welcome to ${result.className}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = if (!result.hasPrefect) {
              "Your class prefect has not been appointed yet. Updates will appear here once they start."
            } else {
              "You have been placed in your class and are ready to receive updates and sign attendance."
            },
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
          )
          Spacer(modifier = Modifier.height(28.dp))
          GoldButton(
            text = "Open my class",
            onClick = { onOpenMyClass(result.classId) },
            testTag = "result_open_class_button"
          )
        }

        is RegisterResultState.StudentNotPlaced -> {
          Text(
            text = "We could not find your class yet",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = result.message ?: "Your class may not have been created yet, or you can join directly with a class code.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
          )
          if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            ErrorBanner(message = errorMessage, onDismiss = onDismissError)
          }
          Spacer(modifier = Modifier.height(22.dp))

          if (showCodeInput) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkCard)
                .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
            ) {
              Column {
                DarkTextField(
                  value = code,
                  onValueChange = { code = it.uppercase() },
                  label = "Class Join Code",
                  leadingIcon = Icons.Default.Key,
                  keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                  testTag = "result_join_code_input"
                )
                Spacer(modifier = Modifier.height(12.dp))
                GoldButton(
                  text = "Join with code",
                  onClick = { onJoinWithCode(code) },
                  enabled = code.isNotBlank() && !isLoading,
                  isLoading = isLoading,
                  testTag = "result_join_code_submit"
                )
              }
            }
            Spacer(modifier = Modifier.height(14.dp))
          }

          GoldButton(
            text = "Try again",
            onClick = onRetryAutoJoin,
            isLoading = isLoading,
            testTag = "result_try_again_button"
          )
          Spacer(modifier = Modifier.height(12.dp))
          GoldOutlinedButton(
            text = "Use a class code",
            onClick = { showCodeInput = !showCodeInput },
            testTag = "result_use_code_button"
          )
        }

        is RegisterResultState.LecturerPasscodeRight -> {
          Text(
            text = if (result.isVerified) "Welcome, Lecturer!" else "Account created",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Account created. An administrator will verify you shortly. Once verified, add your classes from Find my classes.",
            fontSize = 14.sp,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
          )
          Spacer(modifier = Modifier.height(28.dp))
          if (result.isVerified) {
            GoldButton(
              text = "Find my classes",
              onClick = onOpenFindClasses,
              testTag = "result_find_classes_button"
            )
          } else {
            GoldButton(
              text = "Continue",
              onClick = onContinueHome,
              testTag = "result_lecturer_continue_button"
            )
          }
        }

        is RegisterResultState.LecturerPasscodeWrong -> {
          Text(
            text = "Wrong passcode",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Wrong passcode. Your account was created as a student. You can try again from Profile.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
          )
          Spacer(modifier = Modifier.height(28.dp))
          GoldButton(
            text = "Go to Profile",
            onClick = onOpenProfile,
            testTag = "result_wrong_passcode_profile_button"
          )
          Spacer(modifier = Modifier.height(12.dp))
          GoldOutlinedButton(
            text = "Continue",
            onClick = onContinueHome,
            testTag = "result_wrong_passcode_continue_button"
          )
        }
      }
    }
  }
}
