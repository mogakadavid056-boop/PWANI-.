package com.example.admin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.AdminCourseUnit
import com.example.admin.data.AdminUserItem
import com.example.admin.ui.components.AdminEmptyState
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminOptionPickerDialog
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.AdminBorder
import com.example.admin.ui.theme.AdminCardElevated
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.AmberWarning
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.TextMuted
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary

@Composable
fun AdminMonitorUnitsTab(
  units: List<AdminCourseUnit>,
  lecturers: List<AdminUserItem>,
  isActionLoading: Boolean,
  onRegisterUnit: (String, String, String, String?) -> Unit,
  onSetUnitLecturer: (String, String, Boolean) -> Unit
) {
  var showRegister by remember { mutableStateOf(false) }
  var assignTarget by remember { mutableStateOf<AdminCourseUnit?>(null) }
  var removeTarget by remember { mutableStateOf<AdminCourseUnit?>(null) }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("Course Units (${units.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldLight)
      Button(
        onClick = { showRegister = true },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Register Unit", fontWeight = FontWeight.Bold)
      }
    }
    Spacer(modifier = Modifier.height(8.dp))

    if (units.isEmpty()) {
      AdminEmptyState(Icons.Default.MenuBook, "No course units yet", "Register a unit to organize materials and attendance.")
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        items(units, key = { it.courseId }) { unit ->
          val stColor = when {
            unit.isApproved -> EmeraldSuccess
            unit.isPending -> AmberWarning
            else -> CrimsonError
          }
          AdminGlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text(unit.displayLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
              AdminStatusPill(unit.status.uppercase(), stColor)
            }
            if (unit.description.isNotBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(unit.description, fontSize = 12.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Lecturers: ${unit.lecturers.ifBlank { "None assigned" }} • Materials: ${unit.materialCount}",
              fontSize = 12.sp,
              color = TextMuted
            )
            if (unit.rejectionReason.isNotBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Text("Rejected: ${unit.rejectionReason}", fontSize = 12.sp, color = CrimsonError)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
              OutlinedButton(
                onClick = { assignTarget = unit },
                enabled = !isActionLoading && lecturers.isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = GoldLight, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Assign Lecturer", fontSize = 12.sp, color = GoldLight)
              }
              if (unit.lecturerIds.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                  onClick = { removeTarget = unit },
                  enabled = !isActionLoading,
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Icon(Icons.Default.PersonRemove, contentDescription = null, tint = CrimsonError, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Remove", fontSize = 12.sp, color = CrimsonError)
                }
              }
            }
          }
        }
      }
    }
  }

  if (showRegister) {
    RegisterUnitDialog(
      lecturers = lecturers,
      isActionLoading = isActionLoading,
      onConfirm = { code, name, desc, lecId ->
        onRegisterUnit(code, name, desc, lecId)
        showRegister = false
      },
      onDismiss = { showRegister = false }
    )
  }

  assignTarget?.let { unit ->
    val available = lecturers.filterNot { unit.lecturerIds.contains(it.id) }.ifEmpty { lecturers }
    AdminOptionPickerDialog(
      title = "Assign Lecturer to ${unit.code}",
      options = available,
      selectedPredicate = { unit.lecturerIds.contains(it.id) },
      labelProvider = { it.fullName.ifBlank { it.email } },
      subtitleProvider = { if (it.isVerified) "Verified Lecturer" else "Unverified Lecturer" },
      onSelect = { lec ->
        onSetUnitLecturer(unit.courseId, lec.id, true)
        assignTarget = null
      },
      onDismiss = { assignTarget = null }
    )
  }

  removeTarget?.let { unit ->
    val assigned = unit.lecturerIds.map { id ->
      val match = lecturers.firstOrNull { it.id == id }
      id to (match?.fullName?.ifBlank { match.email } ?: "Lecturer ($id)")
    }
    AdminOptionPickerDialog(
      title = "Remove Lecturer from ${unit.code}",
      options = assigned,
      selectedPredicate = { false },
      labelProvider = { it.second },
      onSelect = { (lecId, _) ->
        onSetUnitLecturer(unit.courseId, lecId, false)
        removeTarget = null
      },
      onDismiss = { removeTarget = null }
    )
  }
}

@Composable
private fun RegisterUnitDialog(
  lecturers: List<AdminUserItem>,
  isActionLoading: Boolean,
  onConfirm: (String, String, String, String?) -> Unit,
  onDismiss: () -> Unit
) {
  var code by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }
  var desc by remember { mutableStateOf("") }
  var selectedLecturer by remember { mutableStateOf<AdminUserItem?>(null) }
  var showLecPicker by remember { mutableStateOf(false) }

  val fieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = AdminSurface, unfocusedContainerColor = AdminSurface,
    focusedBorderColor = GoldPrimary, unfocusedBorderColor = AdminBorder,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
  )

  AlertDialog(
    onDismissRequest = { if (!isActionLoading) onDismiss() },
    containerColor = AdminCardElevated,
    shape = RoundedCornerShape(16.dp),
    title = { Text("Register Course Unit", fontWeight = FontWeight.Bold, color = GoldLight) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text("Unit Code (e.g. CSC 201)") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Unit Name") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description (optional)") }, maxLines = 3, colors = fieldColors, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { showLecPicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
          Text("Lecturer: ${selectedLecturer?.fullName ?: "None (assign later)"}", color = GoldLight)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(code.trim(), name.trim(), desc.trim(), selectedLecturer?.id) },
        enabled = !isActionLoading && code.isNotBlank() && name.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) { Text("Register", fontWeight = FontWeight.Bold) }
    },
    dismissButton = { TextButton(onClick = onDismiss, enabled = !isActionLoading) { Text("Cancel", color = TextSecondary) } }
  )

  if (showLecPicker) {
    val opts: List<AdminUserItem?> = listOf(null) + lecturers
    AdminOptionPickerDialog(
      title = "Optional: Assign Lecturer",
      options = opts,
      selectedPredicate = { it?.id == selectedLecturer?.id },
      labelProvider = { it?.fullName?.ifBlank { it.email } ?: "None (assign later)" },
      subtitleProvider = { lec -> lec?.let { if (it.isVerified) "Verified" else "Unverified" } ?: "" },
      onSelect = { selectedLecturer = it; showLecPicker = false },
      onDismiss = { showLecPicker = false }
    )
  }
}
