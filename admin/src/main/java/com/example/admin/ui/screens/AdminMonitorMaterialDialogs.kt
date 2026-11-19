package com.example.admin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.AdminCourseUnit
import com.example.admin.data.AdminMaterialItem
import com.example.admin.ui.components.AdminOptionPickerDialog
import com.example.admin.ui.theme.AdminBorder
import com.example.admin.ui.theme.AdminCardElevated
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary

@Composable
fun AddLinkMaterialDialog(
  units: List<AdminCourseUnit>,
  loading: Boolean,
  onSave: (String, String, String, String, String) -> Unit,
  onDismiss: () -> Unit
) {
  var unit by remember { mutableStateOf(units.first()) }
  var title by remember { mutableStateOf("") }
  var desc by remember { mutableStateOf("") }
  var type by remember { mutableStateOf("notes") }
  var url by remember { mutableStateOf("") }
  var pickUnit by remember { mutableStateOf(false) }
  var pickType by remember { mutableStateOf(false) }
  val fc = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = AdminSurface, unfocusedContainerColor = AdminSurface,
    focusedBorderColor = GoldPrimary, unfocusedBorderColor = AdminBorder,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
  )

  AlertDialog(
    onDismissRequest = { if (!loading) onDismiss() },
    containerColor = AdminCardElevated,
    shape = RoundedCornerShape(16.dp),
    title = { Text("Add Material Link", fontWeight = FontWeight.Bold, color = GoldLight) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { pickUnit = true }, modifier = Modifier.fillMaxWidth()) { Text("Unit: ${unit.displayLabel}", color = GoldLight) }
        OutlinedButton(onClick = { pickType = true }, modifier = Modifier.fillMaxWidth()) { Text("Type: ${type.uppercase()}", color = GoldLight) }
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL (https://...)") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description (optional)") }, maxLines = 2, colors = fc, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(unit.courseId, title, desc, type, url) },
        enabled = !loading && title.isNotBlank() && url.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
  )
  if (pickUnit) AdminOptionPickerDialog("Select Unit", units, { it.courseId == unit.courseId }, { it.displayLabel }, onSelect = { unit = it; pickUnit = false }) { pickUnit = false }
  if (pickType) AdminOptionPickerDialog("Material Type", listOf("notes", "slides", "past_paper", "video", "link"), { it == type }, { it.uppercase() }, onSelect = { type = it; pickType = false }) { pickType = false }
}

@Composable
fun UploadFileMaterialDialog(
  fileName: String,
  units: List<AdminCourseUnit>,
  loading: Boolean,
  onUpload: (String, String, String) -> Unit,
  onDismiss: () -> Unit
) {
  var unit by remember { mutableStateOf(units.first()) }
  var title by remember { mutableStateOf(fileName.substringBeforeLast('.')) }
  var desc by remember { mutableStateOf("") }
  var pickUnit by remember { mutableStateOf(false) }
  val fc = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = AdminSurface, unfocusedContainerColor = AdminSurface,
    focusedBorderColor = GoldPrimary, unfocusedBorderColor = AdminBorder,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
  )

  AlertDialog(
    onDismissRequest = { if (!loading) onDismiss() },
    containerColor = AdminCardElevated,
    shape = RoundedCornerShape(16.dp),
    title = { Text("Upload Material", fontWeight = FontWeight.Bold, color = GoldLight) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("File: $fileName", fontSize = 12.sp, color = TextSecondary)
        OutlinedButton(onClick = { pickUnit = true }, modifier = Modifier.fillMaxWidth()) { Text("Unit: ${unit.displayLabel}", color = GoldLight) }
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description (optional)") }, maxLines = 2, colors = fc, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      Button(
        onClick = { onUpload(unit.courseId, title.trim(), desc.trim()) },
        enabled = !loading && title.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) { Text("Upload") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
  )
  if (pickUnit) AdminOptionPickerDialog("Select Unit", units, { it.courseId == unit.courseId }, { it.displayLabel }, onSelect = { unit = it; pickUnit = false }) { pickUnit = false }
}

@Composable
fun EditMaterialDialog(
  item: AdminMaterialItem,
  loading: Boolean,
  onSave: (String, String) -> Unit,
  onDismiss: () -> Unit
) {
  var title by remember { mutableStateOf(item.title) }
  var desc by remember { mutableStateOf(item.description) }
  val fc = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = AdminSurface, unfocusedContainerColor = AdminSurface,
    focusedBorderColor = GoldPrimary, unfocusedBorderColor = AdminBorder,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
  )

  AlertDialog(
    onDismissRequest = { if (!loading) onDismiss() },
    containerColor = AdminCardElevated,
    shape = RoundedCornerShape(16.dp),
    title = { Text("Edit Material", fontWeight = FontWeight.Bold, color = GoldLight) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, maxLines = 3, colors = fc, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(title.trim(), desc.trim()) },
        enabled = !loading && title.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
  )
}
