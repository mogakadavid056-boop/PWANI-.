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
import com.example.ui.components.royalOutlinedTextFieldColors
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
    containerColor = RoyalSurfaceHigh,
    title = {
      Text("Excuse Absence", style = SectionHeadingStyle)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Mark \"$sessionTitle\" as excused for this student.",
          style = BodyStyle.copy(fontSize = 14.sp)
        )
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Reason (e.g. Medical note)") },
          singleLine = false,
          maxLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("excuse_reason_input"),
          shape = RoundedCornerShape(ButtonRadius),
          colors = royalOutlinedTextFieldColors()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmit(reason.trim()) },
        enabled = canSubmit,
        shape = RoundedCornerShape(ButtonRadius),
        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("confirm_excuse_btn")
      ) {
        Text(if (isActionLoading) "Saving..." else "Excuse", style = ButtonTextStyle.copy(color = Midnight, fontSize = 14.sp))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isActionLoading, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
        Text("Cancel", style = ButtonTextStyle.copy(color = Lavender, fontSize = 14.sp))
      }
    }
  )
}
