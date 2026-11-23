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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceRecordItem
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardTitleStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MontserratFamily
import com.example.ui.theme.RoyalSurface
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SilverWhite
import com.example.util.JsonHelper
import com.example.util.NairobiTimeHelper

@Composable
fun AttendanceRecordRow(record: AttendanceRecordItem) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(RoyalSurface)
      .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
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
          .size(40.dp)
          .clip(CircleShape)
          .background(RoyalSurfaceHigh)
          .border(1.dp, MetallicGold.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(text = initial, fontFamily = MontserratFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ChampagneGold)
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = JsonHelper.orDash(record.fullName),
          style = CardTitleStyle.copy(fontSize = 16.sp, color = SilverWhite)
        )
        if (record.admissionNumber.isNotBlank() && !record.admissionNumber.equals("null", true)) {
          Text(
            text = "Adm: ${record.admissionNumber}",
            style = CaptionStyle.copy(color = ChampagneGold)
          )
        }
      }

      if (record.timeSigned.isNotBlank() && !record.timeSigned.equals("null", true)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Schedule, contentDescription = null, tint = Lavender, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = NairobiTimeHelper.formatIsoToTimeOnly(record.timeSigned),
            style = CaptionStyle.copy(color = Lavender)
          )
        }
      }
    }
  }
}
