package com.example.admin.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.ui.theme.AdminBorder
import com.example.admin.ui.theme.AdminCardElevated
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.TextMuted
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary

@Composable
fun <T> AdminOptionPickerDialog(
  title: String,
  options: List<T>,
  selectedPredicate: (T) -> Boolean,
  labelProvider: (T) -> String,
  subtitleProvider: ((T) -> String)? = null,
  onSelect: (T) -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = AdminCardElevated,
    shape = RoundedCornerShape(16.dp),
    title = { Text(title, fontWeight = FontWeight.Bold, color = GoldLight, fontSize = 18.sp) },
    text = {
      if (options.isEmpty()) {
        Text("No options available.", color = TextSecondary, fontSize = 14.sp)
      } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp)) {
          items(options) { item ->
            val selected = selectedPredicate(item)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(item) }
                .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = selected,
                onClick = { onSelect(item) },
                colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary, unselectedColor = TextSecondary)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(labelProvider(item), color = TextPrimary, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                val sub = subtitleProvider?.invoke(item).orEmpty()
                if (sub.isNotBlank()) Text(sub, color = TextMuted, fontSize = 12.sp)
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Close", color = GoldLight) }
    }
  )
}

@Composable
fun AdminConfirmDialog(
  title: String,
  message: String,
  confirmLabel: String,
  isDestructive: Boolean = false,
  isActionLoading: Boolean = false,
  requireReason: Boolean = false,
  optionalReason: Boolean = false,
  reasonLabel: String = "Reason",
  onConfirm: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var reason by remember { mutableStateOf("") }
  val canConfirm = !isActionLoading && (!requireReason || reason.isNotBlank())

  AlertDialog(
    onDismissRequest = { if (!isActionLoading) onDismiss() },
    containerColor = AdminCardElevated,
    shape = RoundedCornerShape(16.dp),
    title = { Text(title, fontWeight = FontWeight.Bold, color = if (isDestructive) CrimsonError else GoldLight, fontSize = 18.sp) },
    text = {
      Column {
        Text(message, color = TextSecondary, fontSize = 14.sp)
        if (requireReason || optionalReason) {
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text(reasonLabel, color = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = AdminSurface,
              unfocusedContainerColor = AdminSurface,
              focusedBorderColor = GoldPrimary,
              unfocusedBorderColor = AdminBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            )
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(reason.trim()) },
        enabled = canConfirm,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isDestructive) CrimsonError else GoldPrimary,
          contentColor = if (isDestructive) TextPrimary else DarkTextOnGold
        )
      ) { Text(confirmLabel, fontWeight = FontWeight.Bold) }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isActionLoading) { Text("Cancel", color = TextSecondary) }
    }
  )
}
