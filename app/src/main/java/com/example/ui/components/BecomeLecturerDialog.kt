package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun BecomeLecturerDialog(
  isLoading: Boolean,
  onDismiss: () -> Unit,
  onSubmit: (passcode: String) -> Unit
) {
  var passcode by remember { mutableStateOf("") }
  var localError by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(16.dp),
    containerColor = DarkSurface,
    titleContentColor = GoldLight,
    textContentColor = TextPrimary,
    title = {
      Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Icon(
          Icons.Default.School,
          contentDescription = null,
          tint = GoldPrimary,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "I am a Lecturer",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = GoldLight
        )
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Enter the lecturer verification passcode to upgrade your account to Lecturer mode.",
          fontSize = 13.sp,
          color = TextSecondary,
          lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (!localError.isNullOrBlank()) {
          Text(
            text = localError!!,
            color = ErrorRed,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
          )
        }

        PasswordTextField(
          value = passcode,
          onValueChange = {
            passcode = it
            localError = null
          },
          label = "Lecturer Passcode *",
          leadingIcon = Icons.Default.VpnKey,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
          ),
          testTag = "become_lecturer_passcode_input"
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (passcode.isBlank()) {
            localError = "Please enter the passcode."
            return@Button
          }
          onSubmit(passcode)
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = GoldPrimary,
          contentColor = DarkTextOnGold
        ),
        modifier = Modifier.testTag("become_lecturer_submit_btn")
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            color = DarkTextOnGold,
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
          )
        } else {
          Text("Upgrade", fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        enabled = !isLoading,
        modifier = Modifier.testTag("become_lecturer_cancel_btn")
      ) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}
