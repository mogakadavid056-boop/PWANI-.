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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.AdminScheduleItem
import com.example.admin.ui.components.AdminConfirmDialog
import com.example.admin.ui.components.AdminEmptyState
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminOptionPickerDialog
import com.example.admin.ui.components.AdminStatusPill
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
import com.example.admin.util.AdminJsonHelper

@Composable
fun AdminMonitorTimetableTab(
  schedules: List<AdminScheduleItem>,
  isActionLoading: Boolean,
  onSaveSchedule: (String?, String, Int, String, String, String, String) -> Unit,
  onDeleteSchedule: (String) -> Unit
) {
  var editing by remember { mutableStateOf<AdminScheduleItem?>(null) }
  var showForm by remember { mutableStateOf(false) }
  var deleteTarget by remember { mutableStateOf<AdminScheduleItem?>(null) }

  val grouped = remember(schedules) {
    schedules.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime })).groupBy { it.dayOfWeek }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("Weekly Timetable (${schedules.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldLight)
      Button(
        onClick = { editing = null; showForm = true },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add Slot", fontWeight = FontWeight.Bold)
      }
    }
    Spacer(modifier = Modifier.height(8.dp))

    if (schedules.isEmpty()) {
      AdminEmptyState(Icons.Default.Schedule, "No timetable slots", "Tap Add Slot to schedule classes for the week.")
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        grouped.forEach { (_, itemsForDay) ->
          val dayTitle = itemsForDay.firstOrNull()?.dayName ?: "Day"
          item(key = "day_$dayTitle") {
            Text(dayTitle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
          }
          items(itemsForDay, key = { it.id }) { slot ->
            AdminGlassCard {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                  AdminStatusPill("${slot.startTime.take(5)} - ${slot.endTime.take(5)}", GoldLight)
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(slot.title.ifBlank { "Class" }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                  val details = listOf(
                    "Room: ${AdminJsonHelper.orDash(slot.room)}",
                    "Lecturer: ${AdminJsonHelper.orDash(slot.lecturerName)}"
                  ).joinToString(" • ")
                  Text(details, fontSize = 12.sp, color = TextMuted)
                }
                Row {
                  IconButton(onClick = { editing = slot; showForm = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit slot", tint = GoldLight, modifier = Modifier.size(18.dp))
                  }
                  IconButton(onClick = { deleteTarget = slot }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete slot", tint = CrimsonError, modifier = Modifier.size(18.dp))
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showForm) {
    ScheduleFormDialog(
      existing = editing,
      isActionLoading = isActionLoading,
      onSave = { title, day, start, end, room, lec ->
        onSaveSchedule(editing?.id, title, day, start, end, room, lec)
        showForm = false
      },
      onDismiss = { showForm = false }
    )
  }

  deleteTarget?.let { target ->
    AdminConfirmDialog(
      title = "Delete Timetable Slot?",
      message = "Remove \"${target.title}\" on ${target.dayName}?",
      confirmLabel = "Delete",
      isDestructive = true,
      isActionLoading = isActionLoading,
      onConfirm = { onDeleteSchedule(target.id); deleteTarget = null },
      onDismiss = { deleteTarget = null }
    )
  }
}

@Composable
private fun ScheduleFormDialog(
  existing: AdminScheduleItem?,
  isActionLoading: Boolean,
  onSave: (String, Int, String, String, String, String) -> Unit,
  onDismiss: () -> Unit
) {
  val days = listOf(1 to "Monday", 2 to "Tuesday", 3 to "Wednesday", 4 to "Thursday", 5 to "Friday", 6 to "Saturday", 7 to "Sunday")
  var title by remember { mutableStateOf(existing?.title.orEmpty()) }
  var day by remember { mutableIntStateOf(existing?.dayOfWeek ?: 1) }
  var start by remember { mutableStateOf(existing?.startTime?.take(5).orEmpty().ifBlank { "08:00" }) }
  var end by remember { mutableStateOf(existing?.endTime?.take(5).orEmpty().ifBlank { "10:00" }) }
  var room by remember { mutableStateOf(existing?.room.orEmpty()) }
  var lecturer by remember { mutableStateOf(existing?.lecturerName.orEmpty()) }
  var showDayPicker by remember { mutableStateOf(false) }

  val fieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = AdminSurface, unfocusedContainerColor = AdminSurface,
    focusedBorderColor = GoldPrimary, unfocusedBorderColor = AdminBorder,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
  )

  AlertDialog(
    onDismissRequest = { if (!isActionLoading) onDismiss() },
    containerColor = AdminCardElevated,
    shape = RoundedCornerShape(16.dp),
    title = { Text(if (existing == null) "Add Timetable Slot" else "Edit Timetable Slot", fontWeight = FontWeight.Bold, color = GoldLight) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Unit / Course Title") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { showDayPicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
          Text("Day: ${days.firstOrNull { it.first == day }?.second ?: "Monday"}", color = GoldLight)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Start (HH:mm)") }, singleLine = true, colors = fieldColors, modifier = Modifier.weight(1f))
          OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("End (HH:mm)") }, singleLine = true, colors = fieldColors, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Venue / Room") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = lecturer, onValueChange = { lecturer = it }, label = { Text("Lecturer") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(title.trim(), day, start.trim(), end.trim(), room.trim(), lecturer.trim()) },
        enabled = !isActionLoading && title.isNotBlank() && start.isNotBlank() && end.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) { Text("Save", fontWeight = FontWeight.Bold) }
    },
    dismissButton = { TextButton(onClick = onDismiss, enabled = !isActionLoading) { Text("Cancel", color = TextSecondary) } }
  )

  if (showDayPicker) {
    AdminOptionPickerDialog(
      title = "Select Day",
      options = days,
      selectedPredicate = { it.first == day },
      labelProvider = { it.second },
      onSelect = { day = it.first; showDayPicker = false },
      onDismiss = { showDayPicker = false }
    )
  }
}
