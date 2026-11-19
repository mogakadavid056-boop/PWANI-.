package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
  onDelete: () -> Unit,
  onChangeStatus: () -> Unit = {}
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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Default.Schedule, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = schedule.formattedTimeRange,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight
          )
          if (schedule.isCancelled || schedule.isMoved) {
            Spacer(modifier = Modifier.width(8.dp))
            val badgeBg = if (schedule.isCancelled) ErrorRed.copy(alpha = 0.16f) else Color(0x22FFB300)
            val badgeBorder = if (schedule.isCancelled) ErrorRed else Color(0xFFFFB300)
            val badgeText = if (schedule.isCancelled) "CANCELLED" else "MOVED"
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeBg)
                .border(1.dp, badgeBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("schedule_status_badge_${schedule.id}")
            ) {
              Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = badgeBorder)
            }
          }
        }

        if (isStaff) {
          Box {
            IconButton(
              onClick = { showMenu = true },
              modifier = Modifier.size(28.dp).testTag("schedule_more_btn_${schedule.id}")
            ) {
              Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
            if (showMenu) {
              val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
              val context = LocalContext.current
              androidx.compose.ui.window.Dialog(onDismissRequest = { showMenu = false }) {
                Column(
                  modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFF0B2A1E))
                    .border(1.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(16.dp)).padding(16.dp)
                ) {
                  Text("Class Options", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                  Spacer(modifier = Modifier.height(10.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                      showMenu = false; if (!isOnline) OfflineToastHelper.show(context) else onChangeStatus()
                    }.padding(vertical = 12.dp, horizontal = 8.dp).testTag("status_schedule_btn_${schedule.id}"),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Mark Cancelled / Moved", color = GoldLight, fontSize = 14.sp)
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                      showMenu = false; if (!isOnline) OfflineToastHelper.show(context) else onEdit()
                    }.padding(vertical = 12.dp, horizontal = 8.dp).testTag("edit_schedule_btn_${schedule.id}"),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Edit Class", color = TextPrimary, fontSize = 14.sp)
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                      showMenu = false; if (!isOnline) OfflineToastHelper.show(context) else onDelete()
                    }.padding(vertical = 12.dp, horizontal = 8.dp).testTag("delete_schedule_btn_${schedule.id}"),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Delete Class", color = ErrorRed, fontSize = 14.sp)
                  }
                }
              }
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
        color = if (schedule.isCancelled) TextSecondary else TextPrimary,
        textDecoration = if (schedule.isCancelled) TextDecoration.LineThrough else TextDecoration.None
      )

      if ((schedule.isCancelled || schedule.isMoved) && schedule.statusNote.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = schedule.statusNote,
          fontSize = 12.sp,
          color = if (schedule.isCancelled) ErrorRed else Color(0xFFFFD54F),
          fontWeight = FontWeight.Medium
        )
      }

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
