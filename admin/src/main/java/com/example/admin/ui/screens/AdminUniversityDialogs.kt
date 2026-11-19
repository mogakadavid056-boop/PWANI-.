package com.example.admin.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.admin.data.ProgrammeItem
import com.example.admin.data.SchoolItem
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
fun SchoolFormDialog(
  existing: SchoolItem?,
  isActionLoading: Boolean,
  onSave: (String?, String) -> Unit,
  onDismiss: () -> Unit
) {
  var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
  AlertDialog(
    onDismissRequest = { if (!isActionLoading) onDismiss() },
    containerColor = AdminCardElevated,
    shape = RoundedCornerShape(16.dp),
    title = { Text(if (existing == null) "Add School" else "Edit School", color = GoldLight, fontWeight = FontWeight.Bold) },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("School Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = AdminSurface, unfocusedContainerColor = AdminSurface,
          focusedBorderColor = GoldPrimary, unfocusedBorderColor = AdminBorder,
          focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
        )
      )
    },
    confirmButton = {
      Button(
        onClick = { onSave(existing?.id, name) },
        enabled = !isActionLoading && name.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) { Text("Save") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isActionLoading) { Text("Cancel", color = TextSecondary) }
    }
  )
}

@Composable
fun ProgrammeFormDialog(
  existing: ProgrammeItem?,
  schools: List<SchoolItem>,
  isActionLoading: Boolean,
  onSave: (String?, String, String, Int) -> Unit,
  onDismiss: () -> Unit
) {
  var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
  var duration by remember(existing?.id) { mutableStateOf((existing?.durationYears ?: 4).toString()) }
  var selectedSchool by remember(existing?.id, schools) {
    mutableStateOf(schools.firstOrNull { it.id == existing?.schoolId } ?: schools.firstOrNull())
  }
  var showSchoolPicker by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = { if (!isActionLoading) onDismiss() },
    containerColor = AdminCardElevated,
    shape = RoundedCornerShape(16.dp),
    title = { Text(if (existing == null) "Add Programme" else "Edit Programme", color = GoldLight, fontWeight = FontWeight.Bold) },
    text = {
      Column {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Programme Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AdminSurface, unfocusedContainerColor = AdminSurface,
            focusedBorderColor = GoldPrimary, unfocusedBorderColor = AdminBorder,
            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
          )
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
          onClick = { showSchoolPicker = true },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("School: ${selectedSchool?.name ?: "Select School"}", color = GoldLight)
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = duration,
          onValueChange = { duration = it.filter { ch -> ch.isDigit() } },
          label = { Text("Duration (Years)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AdminSurface, unfocusedContainerColor = AdminSurface,
            focusedBorderColor = GoldPrimary, unfocusedBorderColor = AdminBorder,
            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
          )
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val sid = selectedSchool?.id.orEmpty()
          if (sid.isNotBlank()) onSave(existing?.id, name, sid, duration.toIntOrNull()?.coerceIn(1, 8) ?: 4)
        },
        enabled = !isActionLoading && name.isNotBlank() && selectedSchool != null,
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) { Text("Save") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isActionLoading) { Text("Cancel", color = TextSecondary) }
    }
  )

  if (showSchoolPicker) {
    AdminOptionPickerDialog(
      title = "Select School",
      options = schools,
      selectedPredicate = { it.id == selectedSchool?.id },
      labelProvider = { it.name },
      onSelect = { selectedSchool = it; showSchoolPicker = false },
      onDismiss = { showSchoolPicker = false }
    )
  }
}

@Composable
fun AcademicYearFormDialog(
  isActionLoading: Boolean,
  onCreate: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var name by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = { if (!isActionLoading) onDismiss() },
    containerColor = AdminCardElevated,
    shape = RoundedCornerShape(16.dp),
    title = { Text("Add Academic Year", color = GoldLight, fontWeight = FontWeight.Bold) },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("e.g. 2026/2027") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = AdminSurface, unfocusedContainerColor = AdminSurface,
          focusedBorderColor = GoldPrimary, unfocusedBorderColor = AdminBorder,
          focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
        )
      )
    },
    confirmButton = {
      Button(
        onClick = { onCreate(name) },
        enabled = !isActionLoading && name.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) { Text("Add Year") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isActionLoading) { Text("Cancel", color = TextSecondary) }
    }
  )
}
