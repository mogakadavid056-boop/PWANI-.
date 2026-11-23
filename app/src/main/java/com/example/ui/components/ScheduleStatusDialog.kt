package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.ScheduleItem
import com.example.ui.theme.*

@Composable
fun ScheduleStatusDialog(
  schedule: ScheduleItem,
  isLoading: Boolean,
  onDismiss: () -> Unit,
  onSaveStatus: (status: String, note: String) -> Unit
) {
  var selectedStatus by remember(schedule.id) { mutableStateOf(schedule.normalizedStatus) }
  var note by remember(schedule.id) { mutableStateOf(schedule.statusNote) }

  val options = listOf(
    "scheduled" to "Scheduled (Normal)",
    "moved" to "Moved / Rescheduled",
    "cancelled" to "Cancelled"
  )

  AlertDialog(
    onDismissRequest = { if (!isLoading) onDismiss() },
    containerColor = RoyalSurfaceHigh,
    shape = RoundedCornerShape(18.dp),
    title = {
      Column {
        Text("Update Class Status", style = SectionHeadingStyle)
        Spacer(modifier = Modifier.height(2.dp))
        Text(schedule.title, style = CaptionStyle)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().testTag("schedule_status_dialog"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        options.forEach { (value, label) ->
          val isSelected = selectedStatus == value
          val activeColor = when (value) {
            "cancelled" -> ErrorRed
            "moved" -> ChampagneGold
            else -> MetallicGold
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) DarkSurfaceVariant else DarkSurface)
              .border(1.dp, if (isSelected) activeColor else DarkBorder, RoundedCornerShape(10.dp))
              .clickable { selectedStatus = value }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("schedule_status_option_$value"),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = isSelected,
              onClick = { selectedStatus = value },
              colors = RadioButtonDefaults.colors(selectedColor = activeColor, unselectedColor = TextSecondary)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = label,
              fontSize = 14.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) activeColor else TextPrimary
            )
          }
        }

        if (selectedStatus != "scheduled") {
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text(if (selectedStatus == "moved") "New Room / Time Note" else "Cancellation Reason (Optional)") },
            singleLine = false,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth().testTag("schedule_status_note_input"),
            shape = RoundedCornerShape(14.dp),
            colors = royalOutlinedTextFieldColors()
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSaveStatus(selectedStatus, if (selectedStatus == "scheduled") "" else note) },
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.defaultMinSize(minHeight = 44.dp).testTag("schedule_status_save_btn")
      ) {
        if (isLoading) {
          CircularProgressIndicator(color = DarkTextOnGold, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        } else {
          Text("Save", fontWeight = FontWeight.Bold, color = DarkTextOnGold)
        }
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        enabled = !isLoading,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, DarkBorder)
      ) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}
