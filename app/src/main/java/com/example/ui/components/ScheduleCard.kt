package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ScheduleItem
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.PoppinsFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ScheduleCard(
  schedule: ScheduleItem,
  isStaff: Boolean,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  var showMenu by remember { mutableStateOf(false) }

  ClassRepCard(
    modifier = Modifier.testTag("schedule_card_${schedule.id}")
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Schedule, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = schedule.formattedTimeRange,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight
          )
        }

        if (isStaff) {
          Box {
            IconButton(
              onClick = { showMenu = true },
              modifier = Modifier.size(28.dp).testTag("schedule_more_btn_${schedule.id}")
            ) {
              Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(
              expanded = showMenu,
              onDismissRequest = { showMenu = false },
              modifier = Modifier.background(DarkSurface).border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            ) {
              DropdownMenuItem(
                text = { Text("Edit Class", color = TextPrimary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp)) },
                onClick = { showMenu = false; onEdit() },
                modifier = Modifier.testTag("edit_schedule_btn_${schedule.id}")
              )
              DropdownMenuItem(
                text = { Text("Delete Class", color = ErrorRed, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp)) },
                onClick = { showMenu = false; onDelete() },
                modifier = Modifier.testTag("delete_schedule_btn_${schedule.id}")
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = schedule.title,
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = TextPrimary
      )

      if (schedule.room.isNotBlank() || schedule.lecturerName.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (schedule.room.isNotBlank()) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurfaceVariant)
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = GoldLight, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(schedule.room, fontSize = 11.sp, color = TextSecondary)
              }
            }
          }

          if (schedule.lecturerName.isNotBlank()) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurfaceVariant)
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = GoldLight, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(schedule.lecturerName, fontSize = 11.sp, color = TextSecondary)
              }
            }
          }
        }
      }
    }
  }
}
