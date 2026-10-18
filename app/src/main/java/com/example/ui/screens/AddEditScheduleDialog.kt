package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ScheduleItem
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun AddEditScheduleDialog(
  initialSchedule: ScheduleItem? = null,
  isLoading: Boolean = false,
  onDismiss: () -> Unit,
  onSave: (title: String, dayOfWeek: Int, startTime: String, endTime: String, room: String, lecturerName: String) -> Unit
) {
  val context = LocalContext.current
  val isEditing = initialSchedule != null

  var title by remember { mutableStateOf(initialSchedule?.title.orEmpty()) }
  var dayOfWeek by remember { mutableIntStateOf(initialSchedule?.dayOfWeek ?: 1) }
  var startTime by remember { mutableStateOf(initialSchedule?.startTime?.take(5) ?: "08:00") }
  var endTime by remember { mutableStateOf(initialSchedule?.endTime?.take(5) ?: "10:00") }
  var room by remember { mutableStateOf(initialSchedule?.room.orEmpty()) }
  var lecturerName by remember { mutableStateOf(initialSchedule?.lecturerName.orEmpty()) }
  var validationError by remember { mutableStateOf<String?>(null) }
  var dayDropdownExpanded by remember { mutableStateOf(false) }

  val days = listOf(
    1 to "Monday", 2 to "Tuesday", 3 to "Wednesday", 4 to "Thursday",
    5 to "Friday", 6 to "Saturday", 7 to "Sunday"
  )
  val selectedDayName = days.firstOrNull { it.first == dayOfWeek }?.second ?: "Monday"

  fun showTimePicker(currentTimeStr: String, onTimeSelected: (String) -> Unit) {
    val parts = currentTimeStr.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
    TimePickerDialog(context, { _, h, m -> onTimeSelected(String.format(Locale.US, "%02d:%02d", h, m)) }, hour, min, true).show()
  }

  val fieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GoldPrimary, unfocusedBorderColor = DarkBorder,
    focusedLabelColor = GoldPrimary, unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedContainerColor = DarkSurfaceVariant, unfocusedContainerColor = DarkSurfaceVariant
  )

  Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
    Column(
      modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(DarkSurface)
        .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)).padding(20.dp).verticalScroll(rememberScrollState())
        .testTag("add_edit_schedule_dialog")
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isEditing) "Edit Class Schedule" else "Add Class to Timetable", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
      }

      Spacer(modifier = Modifier.height(14.dp))
      OutlinedTextField(
        value = title, onValueChange = { title = it; validationError = null },
        label = { Text("Class / Subject Title") }, singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("schedule_title_input"), colors = fieldColors
      )

      Spacer(modifier = Modifier.height(12.dp))
      Text("Day of the Week", fontSize = 12.sp, color = TextSecondary)
      Spacer(modifier = Modifier.height(4.dp))
      Box(modifier = Modifier.fillMaxWidth()) {
        Box(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp)).clickable { dayDropdownExpanded = true }
            .padding(horizontal = 14.dp, vertical = 12.dp).testTag("schedule_day_dropdown"),
          contentAlignment = Alignment.CenterStart
        ) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(selectedDayName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Day", tint = GoldLight)
          }
        }
        DropdownMenu(expanded = dayDropdownExpanded, onDismissRequest = { dayDropdownExpanded = false }, modifier = Modifier.background(DarkSurface).border(1.dp, DarkBorder)) {
          days.forEach { (d, label) ->
            DropdownMenuItem(
              text = { Text(label, color = if (d == dayOfWeek) GoldPrimary else TextPrimary, fontWeight = if (d == dayOfWeek) FontWeight.Bold else FontWeight.Normal) },
              onClick = { dayOfWeek = d; dayDropdownExpanded = false },
              modifier = Modifier.testTag("day_option_$d")
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = startTime, onValueChange = { startTime = it; validationError = null },
          label = { Text("Start Time") }, singleLine = true,
          trailingIcon = {
            IconButton(onClick = { showTimePicker(startTime) { startTime = it; validationError = null } }) {
              Icon(Icons.Default.AccessTime, contentDescription = "Pick start time", tint = GoldLight, modifier = Modifier.size(20.dp))
            }
          },
          modifier = Modifier.weight(1f).testTag("schedule_start_time_input"), colors = fieldColors
        )
        OutlinedTextField(
          value = endTime, onValueChange = { endTime = it; validationError = null },
          label = { Text("End Time") }, singleLine = true,
          trailingIcon = {
            IconButton(onClick = { showTimePicker(endTime) { endTime = it; validationError = null } }) {
              Icon(Icons.Default.AccessTime, contentDescription = "Pick end time", tint = GoldLight, modifier = Modifier.size(20.dp))
            }
          },
          modifier = Modifier.weight(1f).testTag("schedule_end_time_input"), colors = fieldColors
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      OutlinedTextField(
        value = room, onValueChange = { room = it }, label = { Text("Room / Hall (optional)") },
        singleLine = true, modifier = Modifier.fillMaxWidth().testTag("schedule_room_input"), colors = fieldColors
      )

      Spacer(modifier = Modifier.height(12.dp))
      OutlinedTextField(
        value = lecturerName, onValueChange = { lecturerName = it }, label = { Text("Lecturer Name (optional)") },
        singleLine = true, modifier = Modifier.fillMaxWidth().testTag("schedule_lecturer_input"), colors = fieldColors
      )

      if (validationError != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(validationError!!, color = ErrorRed, fontSize = 12.sp)
      }

      Spacer(modifier = Modifier.height(18.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = onDismiss, enabled = !isLoading,
          modifier = Modifier.weight(1f).height(48.dp).testTag("schedule_cancel_button"),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkBorder),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = TextPrimary)
        ) {
          Text("Cancel", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Button(
          onClick = {
            val cleanTitle = title.trim(); val cleanStart = startTime.trim(); val cleanEnd = endTime.trim()
            if (cleanTitle.isBlank()) { validationError = "Please enter a class title."; return@Button }
            if (cleanEnd <= cleanStart) { validationError = "End time must be after start time."; return@Button }
            val fStart = if (cleanStart.length == 5) "$cleanStart:00" else cleanStart
            val fEnd = if (cleanEnd.length == 5) "$cleanEnd:00" else cleanEnd
            onSave(cleanTitle, dayOfWeek, fStart, fEnd, room.trim(), lecturerName.trim())
          },
          enabled = !isLoading, modifier = Modifier.weight(1f).height(48.dp).testTag("schedule_submit_button"),
          shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = com.example.ui.theme.DarkTextOnGold)
        ) {
          if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = com.example.ui.theme.DarkTextOnGold, strokeWidth = 2.dp)
          else Text(if (isEditing) "Save" else "Add", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = com.example.ui.theme.DarkTextOnGold)
        }
      }
    }
  }
}
