package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceRecordItem
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AttendanceRecordRow(record: AttendanceRecordItem) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(DarkSurface)
      .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
      .padding(14.dp)
      .testTag("attendance_record_item")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      val initial = record.fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(DarkSurfaceVariant)
          .border(1.dp, GoldDark.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(text = initial, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldLight)
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = record.fullName,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextPrimary
        )
        if (record.admissionNumber.isNotBlank()) {
          Text(
            text = "Adm: ${record.admissionNumber}",
            fontSize = 13.sp,
            color = GoldLight
          )
        }
      }

      if (record.timeSigned.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Schedule, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          val displayTime = record.timeSigned.replace("T", " ").substringBefore(".")
          Text(
            text = displayTime.takeLast(8),
            fontSize = 13.sp,
            color = TextSecondary
          )
        }
      }
    }
  }
}
