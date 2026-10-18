package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DiscussionGroupItem
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun EditGroupDialog(
  group: DiscussionGroupItem,
  isLoading: Boolean = false,
  onDismiss: () -> Unit,
  onSave: (newName: String, newDescription: String, newMaxSize: Int) -> Unit
) {
  var name by remember { mutableStateOf(group.name) }
  var description by remember { mutableStateOf(group.description) }
  var maxSizeText by remember { mutableStateOf(group.maxMembers.toString()) }

  val maxSizeInt = maxSizeText.toIntOrNull() ?: 0
  val isValid = name.trim().isNotBlank() && maxSizeInt in 1..100

  Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(DarkSurface)
        .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
        .padding(22.dp)
        .testTag("edit_group_dialog")
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Edit, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Edit Group", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
      }

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Group Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("edit_group_name_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = GoldPrimary, unfocusedBorderColor = DarkBorder,
          focusedLabelColor = GoldPrimary, unfocusedLabelColor = TextSecondary,
          focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
          focusedContainerColor = DarkSurfaceVariant, unfocusedContainerColor = DarkSurfaceVariant
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Description") },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth().testTag("edit_group_desc_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = GoldPrimary, unfocusedBorderColor = DarkBorder,
          focusedLabelColor = GoldPrimary, unfocusedLabelColor = TextSecondary,
          focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
          focusedContainerColor = DarkSurfaceVariant, unfocusedContainerColor = DarkSurfaceVariant
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = maxSizeText,
        onValueChange = { if (it.length <= 3 && it.all { ch -> ch.isDigit() }) maxSizeText = it },
        label = { Text("Max Members (1 - 100)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("edit_group_max_size_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = GoldPrimary, unfocusedBorderColor = DarkBorder,
          focusedLabelColor = GoldPrimary, unfocusedLabelColor = TextSecondary,
          focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
          focusedContainerColor = DarkSurfaceVariant, unfocusedContainerColor = DarkSurfaceVariant
        )
      )

      Spacer(modifier = Modifier.height(22.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = onDismiss,
          enabled = !isLoading,
          modifier = Modifier.weight(1f).height(48.dp).testTag("edit_group_cancel_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = com.example.ui.theme.DarkSurface, contentColor = com.example.ui.theme.TextPrimary),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, com.example.ui.theme.DarkBorder)
        ) {
          Text("Cancel", color = com.example.ui.theme.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Button(
          onClick = { onSave(name.trim(), description.trim(), maxSizeInt) },
          enabled = !isLoading && isValid,
          modifier = Modifier.weight(1f).height(48.dp).testTag("edit_group_save_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = com.example.ui.theme.DarkTextOnGold)
        ) {
          if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = com.example.ui.theme.DarkTextOnGold, strokeWidth = 2.dp)
          } else {
            Text("Save", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = com.example.ui.theme.DarkTextOnGold)
          }
        }
      }
    }
  }
}
