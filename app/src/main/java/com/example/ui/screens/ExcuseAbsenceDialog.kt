package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun ExcuseAbsenceDialog(
  sessionTitle: String,
  isActionLoading: Boolean,
  onDismiss: () -> Unit,
  onSubmit: (reason: String) -> Unit
) {
  var reason by remember { mutableStateOf("") }
  val canSubmit = reason.trim().isNotBlank() && !isActionLoading

  AlertDialog(
    onDismissRequest = { if (!isActionLoading) onDismiss() },
    containerColor = Color(0xFF0E3324),
    title = {
      Text("Excuse Absence", fontWeight = FontWeight.Bold, color = GoldLight)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Mark \"$sessionTitle\" as excused for this student.",
          fontSize = 14.sp,
          color = TextPrimary
        )
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Reason (e.g. Medical note)") },
          singleLine = false,
          maxLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("excuse_reason_input"),
          shape = RoundedCornerShape(10.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = DarkBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedLabelColor = GoldLight,
            unfocusedLabelColor = TextSecondary
          )
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmit(reason.trim()) },
        enabled = canSubmit,
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
        modifier = Modifier.testTag("confirm_excuse_btn")
      ) {
        Text(if (isActionLoading) "Saving..." else "Excuse", fontWeight = FontWeight.Bold, color = DarkTextOnGold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isActionLoading) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}
