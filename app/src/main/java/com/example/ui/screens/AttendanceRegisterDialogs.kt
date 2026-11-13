package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AttendanceRegisterRowItem
import com.example.ui.components.SignaturePad
import com.example.ui.theme.*
import com.example.util.NairobiTimeHelper

@Composable
fun VoidSessionDialog(
  isLoading: Boolean,
  onDismiss: () -> Unit,
  onConfirmVoid: (reason: String) -> Unit
) {
  var reason by remember { mutableStateOf("") }
  val canConfirm = reason.trim().length >= 5 && !isLoading

  Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
    Box(
      modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DarkSurface)
        .border(1.dp, ErrorRed, RoundedCornerShape(16.dp)).padding(20.dp).testTag("void_session_dialog")
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Block, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Void Attendance Session", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GoldLight)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          "Enter a reason for voiding this attendance session (minimum 5 characters). Voided registers cannot be unsigned.",
          fontSize = 12.5.sp, color = TextSecondary, lineHeight = 17.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = reason, onValueChange = { reason = it },
          label = { Text("Reason for voiding") },
          placeholder = { Text("e.g. Session created for wrong class") },
          modifier = Modifier.fillMaxWidth().testTag("void_reason_input"), minLines = 2,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPrimary, unfocusedBorderColor = DarkBorder,
            focusedContainerColor = DarkSurfaceVariant, unfocusedContainerColor = DarkSurfaceVariant,
            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
          )
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(
            onClick = onDismiss, enabled = !isLoading, modifier = Modifier.weight(1f).height(44.dp),
            border = BorderStroke(1.5.dp, GoldPrimary),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldLight)
          ) { Text("Cancel", fontSize = 14.sp, color = GoldLight, maxLines = 1, softWrap = false) }
          Button(
            onClick = { onConfirmVoid(reason.trim()) }, enabled = canConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White, disabledContainerColor = ErrorRed.copy(alpha = 0.4f), disabledContentColor = Color.White.copy(alpha = 0.6f)),
            modifier = Modifier.weight(1.2f).height(44.dp).testTag("confirm_void_session_btn")
          ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            else Text("Void Session", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
          }
        }
      }
    }
  }
}

@Composable
fun SignRegisterDialog(
  roleTitle: String,
  isLoading: Boolean,
  onDismiss: () -> Unit,
  onConfirmSign: (signatureBase64: String) -> Unit
) {
  var hasSignature by remember { mutableStateOf(false) }
  var base64Generator by remember { mutableStateOf<(() -> String?)?>({ null }) }

  Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
    Box(
      modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DarkSurface)
        .border(1.dp, GoldPrimary, RoundedCornerShape(16.dp)).padding(20.dp).testTag("sign_register_dialog")
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Draw, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Sign Register as $roleTitle", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GoldLight)
        }
        Spacer(modifier = Modifier.height(14.dp))
        SignaturePad { hasSig, generator -> hasSignature = hasSig; base64Generator = generator }
        Spacer(modifier = Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(
            onClick = onDismiss, enabled = !isLoading, modifier = Modifier.weight(1f).height(44.dp),
            border = BorderStroke(1.5.dp, GoldPrimary),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldLight)
          ) { Text("Cancel", fontSize = 14.sp, color = GoldLight, maxLines = 1, softWrap = false) }
          Button(
            onClick = { val sig = base64Generator?.invoke().orEmpty(); if (sig.isNotBlank()) onConfirmSign(sig) },
            enabled = hasSignature && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
            modifier = Modifier.weight(1.2f).height(44.dp).testTag("confirm_sign_register_btn")
          ) {
            if (isLoading) CircularProgressIndicator(color = DarkTextOnGold, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            else Text("Sign Register", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold, maxLines = 1, softWrap = false)
          }
        }
      }
    }
  }
}

@Composable
fun CorrectTimeDialog(
  row: AttendanceRegisterRowItem,
  isLoading: Boolean,
  onDismiss: () -> Unit,
  onConfirmCorrect: (recordId: String, newTimeIn: String, newTimeOut: String, reason: String) -> Unit
) {
  val initIn = row.timeInText.takeIf { it.isNotBlank() && it != "-" }
    ?: row.timeIn?.let { NairobiTimeHelper.formatIsoToTimeOnly(it).takeIf { t -> t != "-" } }
    ?: NairobiTimeHelper.nowNairobi("HH:mm")
  val initOut = row.timeOutText.takeIf { it.isNotBlank() && it != "-" }
    ?: row.timeOut?.let { NairobiTimeHelper.formatIsoToTimeOnly(it).takeIf { t -> t != "-" } }
    ?: ""

  var timeIn by remember { mutableStateOf(initIn) }
  var timeOut by remember { mutableStateOf(initOut) }
  var reason by remember { mutableStateOf("") }
  val canConfirm = row.recordId.isNotBlank() && timeIn.trim().isNotBlank() && reason.trim().isNotBlank() && !isLoading

  Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
    Box(
      modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DarkSurface)
        .border(1.dp, GoldPrimary, RoundedCornerShape(16.dp)).padding(20.dp).testTag("correct_time_dialog")
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Edit, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Correct Attendance Time", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GoldLight)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(row.fullName, fontSize = 13.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(12.dp))
        RegisterDialogField(timeIn, { timeIn = it }, "Time In (HH:mm)", "time_in_input")
        Spacer(modifier = Modifier.height(8.dp))
        RegisterDialogField(timeOut, { timeOut = it }, "Time Out (HH:mm)", "time_out_input")
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = reason, onValueChange = { reason = it },
          label = { Text("Reason for correction (required)") },
          modifier = Modifier.fillMaxWidth().testTag("correction_reason_input"), minLines = 2,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPrimary, unfocusedBorderColor = DarkBorder,
            focusedContainerColor = DarkSurfaceVariant, unfocusedContainerColor = DarkSurfaceVariant,
            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
          )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(
            onClick = onDismiss, enabled = !isLoading, modifier = Modifier.weight(1f).height(44.dp),
            border = BorderStroke(1.5.dp, GoldPrimary),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldLight)
          ) { Text("Cancel", fontSize = 14.sp, color = GoldLight, maxLines = 1, softWrap = false) }
          Button(
            onClick = { onConfirmCorrect(row.recordId, timeIn.trim(), timeOut.trim(), reason.trim()) },
            enabled = canConfirm, colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
            modifier = Modifier.weight(1.2f).height(44.dp).testTag("confirm_correct_time_btn")
          ) {
            if (isLoading) CircularProgressIndicator(color = DarkTextOnGold, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            else Text("Save Correction", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold, maxLines = 1, softWrap = false)
          }
        }
      }
    }
  }
}
