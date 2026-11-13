package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
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
import com.example.data.AttendanceRegisterDetail
import com.example.data.AttendanceRegisterRowItem
import com.example.data.ClassUnitItem
import com.example.ui.theme.*
import com.example.util.NairobiTimeHelper

@Composable
fun EditRegisterDetailsDialog(
  detail: AttendanceRegisterDetail,
  units: List<ClassUnitItem>,
  isLoading: Boolean,
  onDismiss: () -> Unit,
  onSave: (courseTitle: String, courseCode: String, lecturerName: String, venueText: String, sessionTitle: String) -> Unit
) {
  var unitMenuExpanded by remember { mutableStateOf(false) }
  var courseTitle by remember { mutableStateOf(detail.courseName.orEmpty()) }
  var courseCode by remember { mutableStateOf(detail.courseCode.orEmpty()) }
  var lecturerName by remember { mutableStateOf(detail.lecturerName.orEmpty()) }
  var venueText by remember { mutableStateOf(detail.venue.orEmpty()) }
  var sessionTitle by remember { mutableStateOf(detail.title.ifBlank { "Class Attendance" }) }

  Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
    Box(
      modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DarkSurface)
        .border(1.dp, GoldPrimary, RoundedCornerShape(16.dp)).padding(20.dp).testTag("edit_register_details_dialog")
    ) {
      Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Edit, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Edit Register Details", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GoldLight)
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (units.isNotEmpty()) {
          Box(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant)
                .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                .clickable(enabled = !isLoading) { unitMenuExpanded = true }
                .padding(horizontal = 12.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Pick a unit to prefill", fontSize = 13.5.sp, color = TextSecondary, maxLines = 1)
              Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = GoldPrimary)
            }
            DropdownMenu(expanded = unitMenuExpanded, onDismissRequest = { unitMenuExpanded = false }, modifier = Modifier.background(DarkSurfaceElevated)) {
              units.forEach { u ->
                DropdownMenuItem(
                  text = { Text("${u.courseCode} - ${u.courseName}".trim(' ', '-'), color = TextPrimary, fontSize = 14.sp) },
                  onClick = { courseTitle = u.courseName; courseCode = u.courseCode; lecturerName = u.lecturerName; unitMenuExpanded = false }
                )
              }
              DropdownMenuItem(
                text = { Text("Other (type manually)", color = GoldLight, fontSize = 14.sp) },
                onClick = { courseTitle = ""; courseCode = ""; lecturerName = ""; unitMenuExpanded = false }
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
        }

        RegisterDialogField(courseTitle, { courseTitle = it }, "Course title", "edit_course_title_input")
        Spacer(modifier = Modifier.height(8.dp))
        RegisterDialogField(courseCode, { courseCode = it }, "Course code", "edit_course_code_input")
        Spacer(modifier = Modifier.height(8.dp))
        RegisterDialogField(lecturerName, { lecturerName = it }, "Lecturer in charge", "edit_lecturer_input")
        Spacer(modifier = Modifier.height(8.dp))
        RegisterDialogField(venueText, { venueText = it }, "Venue", "edit_venue_input")
        Spacer(modifier = Modifier.height(8.dp))
        RegisterDialogField(sessionTitle, { sessionTitle = it }, "Session title", "edit_session_title_input")
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(
            onClick = onDismiss, enabled = !isLoading, modifier = Modifier.weight(1f).height(44.dp),
            border = BorderStroke(1.5.dp, GoldPrimary),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldLight)
          ) { Text("Cancel", fontSize = 14.sp, color = GoldLight, maxLines = 1, softWrap = false) }
          Button(
            onClick = { onSave(courseTitle, courseCode, lecturerName, venueText, sessionTitle) },
            enabled = !isLoading, modifier = Modifier.weight(1f).height(44.dp).testTag("save_register_details_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
          ) { Text("Save", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold, maxLines = 1, softWrap = false) }
        }
      }
    }
  }
}

@Composable
fun DeleteRegisterDialog(
  registerTitle: String,
  isLoading: Boolean,
  onDismiss: () -> Unit,
  onConfirmDelete: (reason: String) -> Unit
) {
  var reason by remember { mutableStateOf("") }
  val canConfirm = reason.trim().length >= 5 && !isLoading
  Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
    Box(
      modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DarkSurface)
        .border(1.dp, ErrorRed, RoundedCornerShape(16.dp)).padding(20.dp).testTag("delete_register_dialog")
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Delete Attendance History", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GoldLight)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Delete \"$registerTitle\"? Enter a reason (minimum 5 characters).", fontSize = 13.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = reason, onValueChange = { reason = it },
          label = { Text("Reason for deletion") }, minLines = 2,
          modifier = Modifier.fillMaxWidth().testTag("delete_register_reason_input"),
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
            onClick = { onConfirmDelete(reason.trim()) }, enabled = canConfirm,
            modifier = Modifier.weight(1f).height(44.dp).testTag("confirm_delete_register_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White, disabledContainerColor = ErrorRed.copy(alpha = 0.4f), disabledContentColor = Color.White.copy(alpha = 0.6f))
          ) { Text("Delete", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false) }
        }
      }
    }
  }
}

@Composable
fun MarkPresentDialog(
  row: AttendanceRegisterRowItem,
  isLoading: Boolean,
  onDismiss: () -> Unit,
  onConfirmMarkPresent: (studentId: String, timeIn: String, timeOut: String, reason: String) -> Unit
) {
  var timeIn by remember { mutableStateOf(NairobiTimeHelper.nowNairobi("HH:mm")) }
  var timeOut by remember { mutableStateOf("") }
  var reason by remember { mutableStateOf("") }
  val canConfirm = timeIn.trim().isNotBlank() && reason.trim().length >= 5 && !isLoading

  Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
    Box(
      modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DarkSurface)
        .border(1.dp, GoldPrimary, RoundedCornerShape(16.dp)).padding(20.dp).testTag("mark_present_dialog")
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Mark Present", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GoldLight)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("${row.fullName} (${row.admissionNumber})", fontSize = 13.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(12.dp))
        RegisterDialogField(timeIn, { timeIn = it }, "Time In (HH:mm)", "mark_present_time_in_input")
        Spacer(modifier = Modifier.height(8.dp))
        RegisterDialogField(timeOut, { timeOut = it }, "Time Out (optional, HH:mm)", "mark_present_time_out_input")
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = reason, onValueChange = { reason = it },
          label = { Text("Reason (min 5 characters)") }, minLines = 2,
          modifier = Modifier.fillMaxWidth().testTag("mark_present_reason_input"),
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
            onClick = { onConfirmMarkPresent(row.studentId, timeIn.trim(), timeOut.trim(), reason.trim()) },
            enabled = canConfirm, modifier = Modifier.weight(1f).height(44.dp).testTag("confirm_mark_present_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
          ) { Text("Save", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold, maxLines = 1, softWrap = false) }
        }
      }
    }
  }
}

@Composable
fun AttendanceRegisterModals(
  detail: AttendanceRegisterDetail?,
  units: List<ClassUnitItem>,
  isActionLoading: Boolean,
  showVoidDialog: Boolean,
  showDeleteDialog: Boolean,
  showEditDetailsDialog: Boolean,
  signRoleToOpen: String?,
  rowToCorrect: AttendanceRegisterRowItem?,
  rowToMarkPresent: AttendanceRegisterRowItem?,
  onDismissVoid: () -> Unit,
  onConfirmVoid: (String) -> Unit,
  onDismissDelete: () -> Unit,
  onConfirmDelete: (String) -> Unit,
  onDismissEditDetails: () -> Unit,
  onSaveEditDetails: (String, String, String, String, String) -> Unit,
  onDismissSignRole: () -> Unit,
  onConfirmSignRole: (String, String) -> Unit,
  onDismissCorrect: () -> Unit,
  onConfirmCorrect: (String, String, String, String) -> Unit,
  onDismissMarkPresent: () -> Unit,
  onConfirmMarkPresent: (String, String, String, String) -> Unit
) {
  if (showVoidDialog) {
    VoidSessionDialog(isLoading = isActionLoading, onDismiss = onDismissVoid, onConfirmVoid = onConfirmVoid)
  }
  if (showDeleteDialog && detail != null) {
    DeleteRegisterDialog(registerTitle = detail.title, isLoading = isActionLoading, onDismiss = onDismissDelete, onConfirmDelete = onConfirmDelete)
  }
  if (showEditDetailsDialog && detail != null) {
    EditRegisterDetailsDialog(detail = detail, units = units, isLoading = isActionLoading, onDismiss = onDismissEditDetails, onSave = onSaveEditDetails)
  }
  signRoleToOpen?.let { role ->
    SignRegisterDialog(roleTitle = role.replaceFirstChar { it.uppercase() }, isLoading = isActionLoading, onDismiss = onDismissSignRole, onConfirmSign = { onConfirmSignRole(role, it) })
  }
  rowToCorrect?.let { row ->
    CorrectTimeDialog(row = row, isLoading = isActionLoading, onDismiss = onDismissCorrect, onConfirmCorrect = onConfirmCorrect)
  }
  rowToMarkPresent?.let { row ->
    MarkPresentDialog(row = row, isLoading = isActionLoading, onDismiss = onDismissMarkPresent, onConfirmMarkPresent = onConfirmMarkPresent)
  }
}
