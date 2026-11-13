package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ClassRepSnackbarHost
import com.example.ui.components.OfflineToastHelper
import com.example.ui.components.SignaturePad
import com.example.ui.theme.*
import com.example.util.JsonHelper

private val ADM_REGEX = Regex("^[A-Za-z0-9/.\\-]{4,30}$")

@Composable
fun SignAttendanceScreen(
  memberFullName: String = "",
  memberAdmissionNo: String = "",
  needsDetails: Boolean = false,
  isLoading: Boolean,
  errorMessage: String?,
  successMessage: String?,
  isLockedOut: Boolean = false,
  lockoutRemainingSeconds: Long = 0L,
  onDismissError: () -> Unit,
  onDismissSuccess: () -> Unit,
  onUpdateMyDetails: (fullName: String, admissionNo: String, phone: String, onSaved: () -> Unit) -> Unit = { _, _, _, cb -> cb() },
  onSubmit: (code: String, signatureBase64: String, memberFullName: String, memberAdmissionNo: String) -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  var code by remember { mutableStateOf("") }
  var hasSignature by remember { mutableStateOf(false) }
  var base64Generator by remember { mutableStateOf<(() -> String?)?>({ null }) }
  var fullName by remember(memberFullName) { mutableStateOf(memberFullName) }
  var admissionNo by remember(memberAdmissionNo) { mutableStateOf(memberAdmissionNo) }
  var phone by remember { mutableStateOf("") }
  var showEditDetails by remember(memberFullName, memberAdmissionNo, needsDetails) {
    mutableStateOf(memberFullName.isBlank() || memberAdmissionNo.isBlank() || needsDetails)
  }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(errorMessage) {
    if (!errorMessage.isNullOrBlank()) {
      snackbarHostState.showSnackbar(
        message = errorMessage,
        actionLabel = "Dismiss",
        duration = SnackbarDuration.Long
      )
      onDismissError()
    }
  }

  val nameWordsValid = fullName.trim().split(Regex("\\s+")).count { it.isNotBlank() } >= 2
  val admValid = ADM_REGEX.matches(admissionNo.trim())
  val detailsValid = nameWordsValid && admValid

  Box(modifier = Modifier.fillMaxSize().background(DarkBackground).testTag("sign_attendance_screen")) {
    Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp).testTag("sign_attendance_back_btn")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text("Sign Attendance", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GoldLight)
          Text("Verify your details, enter code and sign", fontSize = 12.sp, color = TextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Member Details Card / Inline Editor
      Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DarkSurface)
          .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)).padding(14.dp)
      ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Your details", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldLight)
          }
          if (!showEditDetails) {
            TextButton(onClick = { showEditDetails = true }, modifier = Modifier.testTag("edit_my_details_btn")) {
              Icon(Icons.Default.Edit, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Edit my details", fontSize = 14.sp, color = GoldPrimary, maxLines = 1)
            }
          }
        }

        if (!showEditDetails) {
          Text("Name: ${JsonHelper.orDash(fullName)}", fontSize = 13.5.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
          Spacer(modifier = Modifier.height(2.dp))
          Text("Admission No: ${JsonHelper.orDash(admissionNo)}", fontSize = 13.5.sp, color = TextSecondary)
        } else {
          Spacer(modifier = Modifier.height(8.dp))
          RegisterDialogField(fullName, { fullName = it }, "Full name (2+ words)", "member_full_name_input")
          if (fullName.isNotBlank() && !nameWordsValid) {
            Text("Enter at least first and last name.", fontSize = 12.sp, color = ErrorRed)
          }
          Spacer(modifier = Modifier.height(8.dp))
          RegisterDialogField(admissionNo, { admissionNo = it }, "Admission number", "member_admission_input")
          if (admissionNo.isNotBlank() && !admValid) {
            Text("4-30 characters using letters, numbers, /, -, .", fontSize = 12.sp, color = ErrorRed)
          }
          Spacer(modifier = Modifier.height(8.dp))
          RegisterDialogField(phone, { phone = it }, "Phone (optional)", "member_phone_input")
          if (memberFullName.isNotBlank() && memberAdmissionNo.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = {
                  fullName = memberFullName
                  admissionNo = memberAdmissionNo
                  showEditDetails = false
                },
                enabled = !isLoading,
                modifier = Modifier.weight(1f).height(42.dp),
                border = BorderStroke(1.5.dp, GoldPrimary),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldLight)
              ) { Text("Cancel", fontSize = 14.sp, color = GoldLight, maxLines = 1, softWrap = false) }
              Button(
                onClick = {
                  onUpdateMyDetails(fullName.trim(), admissionNo.trim(), phone.trim()) {
                    showEditDetails = false
                  }
                },
                enabled = detailsValid && !isLoading,
                modifier = Modifier.weight(1f).height(42.dp).testTag("save_my_details_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
              ) { Text("Save Details", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold, maxLines = 1, softWrap = false) }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 6-digit Code Field
      Column(modifier = Modifier.fillMaxWidth()) {
        Text("6-Digit Session Code", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = code,
          onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) code = it },
          modifier = Modifier.fillMaxWidth().testTag("attendance_code_input"),
          placeholder = { Text("e.g. 123456", color = TextSecondary.copy(alpha = 0.5f), fontSize = 14.sp) },
          leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPrimary, unfocusedBorderColor = DarkBorder,
            focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface,
            focusedTextColor = GoldLight, unfocusedTextColor = TextPrimary
          )
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
      SignaturePad { hasSig, generator -> hasSignature = hasSig; base64Generator = generator }
      Spacer(modifier = Modifier.height(20.dp))

      if (isLockedOut) {
        val mins = lockoutRemainingSeconds / 60
        val secs = lockoutRemainingSeconds % 60
        Box(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DarkSurfaceVariant)
            .border(1.dp, ErrorRed, RoundedCornerShape(12.dp)).padding(12.dp).testTag("attendance_lockout_banner")
        ) {
          Text(
            text = "Too many attempts. The Sign button is disabled for ${String.format("%02d:%02d", mins, secs)}.",
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ErrorRed
          )
        }
        Spacer(modifier = Modifier.height(14.dp))
      }

      val canSubmit = code.length == 6 && hasSignature && detailsValid && !isLoading && !isLockedOut
      val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
      val context = LocalContext.current
      Button(
        onClick = {
          if (isLockedOut) return@Button
          if (!isOnline) {
            OfflineToastHelper.show(context)
          } else {
            val signatureBase64 = base64Generator?.invoke().orEmpty()
            if (code.length == 6 && signatureBase64.isNotBlank() && detailsValid) {
              onSubmit(code, signatureBase64, fullName.trim(), admissionNo.trim())
            }
          }
        },
        enabled = if (!isOnline) !isLockedOut else canSubmit,
        modifier = Modifier.fillMaxWidth().height(48.dp)
          .then(if (!isOnline || isLockedOut || !canSubmit) Modifier.alpha(0.5f) else Modifier)
          .testTag("submit_sign_attendance_btn"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = GoldPrimary, contentColor = DarkTextOnGold,
          disabledContainerColor = GoldPrimary.copy(alpha = 0.4f), disabledContentColor = DarkTextOnGold.copy(alpha = 0.7f)
        )
      ) {
        if (isLockedOut) {
          val mins = lockoutRemainingSeconds / 60
          val secs = lockoutRemainingSeconds % 60
          Text("Locked: ${mins}m ${secs}s", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold, maxLines = 1)
        } else if (isLoading) {
          CircularProgressIndicator(color = DarkTextOnGold, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
        } else {
          Text("Submit Attendance", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold, maxLines = 1, softWrap = false)
        }
      }
      Spacer(modifier = Modifier.height(56.dp))
    }

    ClassRepSnackbarHost(
      hostState = snackbarHostState,
      isError = true,
      modifier = Modifier.align(Alignment.BottomCenter)
    )

    if (!successMessage.isNullOrBlank()) {
      Dialog(onDismissRequest = onDismissSuccess) {
        Box(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DarkSurface)
            .border(1.dp, GoldPrimary, RoundedCornerShape(16.dp)).padding(24.dp).testTag("attendance_success_dialog"),
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
              colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
            ) {
              Text("Done", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold, maxLines = 1)
            }
          }
        }
      }
    }
  }
}
