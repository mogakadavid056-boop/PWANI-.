package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun RegisterUnitDialog(
  isLoading: Boolean,
  errorMessage: String?,
  onDismiss: () -> Unit,
  onSubmit: (code: String, name: String, description: String) -> Unit
) {
  var unitCode by remember { mutableStateOf("") }
  var unitName by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var localError by remember { mutableStateOf<String?>(null) }

  val fieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GoldPrimary, unfocusedBorderColor = DarkBorder,
    focusedLabelColor = GoldPrimary, unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedContainerColor = DarkSurfaceVariant, unfocusedContainerColor = DarkSurfaceVariant
  )

  Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(DarkSurface)
        .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
        .padding(20.dp)
        .verticalScroll(rememberScrollState())
        .testTag("register_unit_dialog")
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.MenuBook, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Register a Unit", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text("An administrator will review and approve the unit before materials can be added.", fontSize = 12.sp, color = TextSecondary)
      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = unitCode,
        onValueChange = { unitCode = it.uppercase(); localError = null },
        label = { Text("Unit Code * (e.g. MGE B101)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        colors = fieldColors,
        modifier = Modifier.fillMaxWidth().testTag("register_unit_code_input")
      )
      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = unitName,
        onValueChange = { unitName = it; localError = null },
        label = { Text("Unit Name *") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        colors = fieldColors,
        modifier = Modifier.fillMaxWidth().testTag("register_unit_name_input")
      )
      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Description (Optional)") },
        minLines = 2,
        maxLines = 4,
        colors = fieldColors,
        modifier = Modifier.fillMaxWidth().testTag("register_unit_desc_input")
      )

      val shownError = localError ?: errorMessage
      if (!shownError.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(shownError, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.testTag("register_unit_error"))
      }

      Spacer(modifier = Modifier.height(18.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        OutlinedButton(
          onClick = onDismiss,
          enabled = !isLoading,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("cancel_register_unit_btn")
        ) {
          Text("Cancel", color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Button(
          onClick = {
            val c = unitCode.trim()
            val n = unitName.trim()
            if (c.length < 2) {
              localError = "Enter a valid unit code"
              return@Button
            }
            if (n.length < 2) {
              localError = "Enter a valid unit name"
              return@Button
            }
            onSubmit(c, n, description.trim())
          },
          enabled = !isLoading,
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("submit_register_unit_btn")
        ) {
          if (isLoading) {
            CircularProgressIndicator(color = DarkTextOnGold, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
          } else {
            Text("Submit", fontWeight = FontWeight.Bold, color = DarkTextOnGold)
          }
        }
      }
    }
  }
}
