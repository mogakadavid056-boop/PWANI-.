package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.ErrorBanner
import com.example.ui.components.SignaturePad
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SignAttendanceScreen(
  isLoading: Boolean,
  errorMessage: String?,
  successMessage: String?,
  onDismissError: () -> Unit,
  onDismissSuccess: () -> Unit,
  onSubmit: (code: String, signatureBase64: String) -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  var code by remember { mutableStateOf("") }
  var hasSignature by remember { mutableStateOf(false) }
  var base64Generator by remember { mutableStateOf<(() -> String?)?>({ null }) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
      .testTag("sign_attendance_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top Navigation
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.size(40.dp).testTag("sign_attendance_back_btn")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text("Sign Attendance", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GoldLight)
          Text("Enter session code and sign below", fontSize = 12.sp, color = TextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      if (!errorMessage.isNullOrBlank()) {
        ErrorBanner(message = errorMessage, onDismiss = onDismissError)
        Spacer(modifier = Modifier.height(16.dp))
      }

      // 6-digit Code Field
      Column(modifier = Modifier.fillMaxWidth()) {
        Text("6-Digit Session Code", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = code,
          onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) code = it },
          modifier = Modifier.fillMaxWidth().testTag("attendance_code_input"),
          placeholder = { Text("e.g. 123456", color = TextSecondary.copy(alpha = 0.5f), fontSize = 14.sp) },
          leadingIcon = {
            Icon(Icons.Default.Pin, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = DarkBorder,
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface,
            focusedTextColor = GoldLight,
            unfocusedTextColor = TextPrimary
          )
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Signature Pad
      SignaturePad(
        onSignatureChanged = { hasSig, generator ->
          hasSignature = hasSig
          base64Generator = generator
        }
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Submit Button
      val canSubmit = code.length == 6 && hasSignature && !isLoading
      val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
      val context = androidx.compose.ui.platform.LocalContext.current
      Button(
        onClick = {
          if (!isOnline) {
            com.example.ui.components.OfflineToastHelper.show(context)
          } else {
            val signatureBase64 = base64Generator?.invoke().orEmpty()
            if (code.length == 6 && signatureBase64.isNotBlank()) {
              onSubmit(code, signatureBase64)
            }
          }
        },
        enabled = if (!isOnline) true else canSubmit,
        modifier = Modifier.fillMaxWidth().height(48.dp)
          .then(if (!isOnline) Modifier.alpha(0.5f) else Modifier)
          .testTag("submit_sign_attendance_btn"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = GoldPrimary,
          contentColor = com.example.ui.theme.DarkTextOnGold,
          disabledContainerColor = DarkSurface,
          disabledContentColor = TextSecondary.copy(alpha = 0.4f)
        )
      ) {
        if (isLoading) {
          CircularProgressIndicator(color = com.example.ui.theme.DarkTextOnGold, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
        } else {
          Text("Submit Attendance", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.DarkTextOnGold)
        }
      }
    }

    // Success Dialog
    if (!successMessage.isNullOrBlank()) {
      Dialog(onDismissRequest = onDismissSuccess) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, GoldPrimary, RoundedCornerShape(16.dp))
            .padding(24.dp)
            .testTag("attendance_success_dialog"),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text("Success!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GoldLight)
            Spacer(modifier = Modifier.height(6.dp))
            Text(successMessage, fontSize = 14.sp, color = TextPrimary, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Button(
              onClick = onDismissSuccess,
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dismiss_success_btn"),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = com.example.ui.theme.DarkTextOnGold)
            ) {
              Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.DarkTextOnGold)
            }
          }
        }
      }
    }
  }
}
