package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceRegisterListItem
import com.example.ui.theme.*
import com.example.util.JsonHelper
import com.example.util.NairobiTimeHelper

@Composable
fun AttendanceRegisterCard(
  item: AttendanceRegisterListItem,
  onClick: () -> Unit,
  onDelete: (() -> Unit)? = null
) {
  val displayDate = item.dateText.takeIf { it.isNotBlank() && !it.equals("null", true) }
    ?: NairobiTimeHelper.formatIsoToDateOnly(item.startsAt)
  val displayTime = item.timeText.takeIf { it.isNotBlank() && !it.equals("null", true) }.orEmpty()
  val dateTimeStr = if (displayTime.isNotBlank()) "$displayDate • $displayTime" else displayDate

  ClassRepCard(
    modifier = Modifier.fillMaxWidth().testTag("register_card_${item.sessionId}"),
    onClick = onClick
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(DarkSurfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = GoldLight, modifier = Modifier.size(22.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(JsonHelper.orDash(item.registerNo), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldLight)
          Spacer(modifier = Modifier.width(6.dp))
          RegisterStatusChip(item.status)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(JsonHelper.orDash(item.title), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (!item.courseName.isNullOrBlank()) {
          Text(item.courseName, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(dateTimeStr, fontSize = 11.5.sp, color = TextSecondary)
          Spacer(modifier = Modifier.width(10.dp))
          Icon(Icons.Default.People, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("${item.presentCount} present", fontSize = 11.5.sp, color = GoldLight, fontWeight = FontWeight.Medium)
        }
      }

      if (item.canDelete && onDelete != null) {
        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(40.dp).testTag("delete_register_card_btn_${item.sessionId}")
        ) {
          Icon(Icons.Default.Delete, contentDescription = "Delete register", tint = ErrorRed, modifier = Modifier.size(20.dp))
        }
      }
    }
  }
}
