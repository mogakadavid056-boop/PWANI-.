package com.example.ui.screens

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
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StartAttendanceDialog(
  isLoading: Boolean,
  onDismiss: () -> Unit,
  onStart: (durationMinutes: Int) -> Unit
) {
  var selectedDuration by remember { mutableIntStateOf(10) }
  val durations = listOf(10, 20, 30)

  Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurface)
        .border(1.dp, DarkBorder, RoundedCornerShape(18.dp))
        .padding(24.dp)
        .testTag("start_attendance_dialog")
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.HourglassTop, contentDescription = null, tint = GoldLight, modifier = Modifier.size(26.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Start Attendance Session",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = GoldLight
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Select a duration. A 6-digit code will be generated for students to sign attendance.",
          fontSize = 13.sp,
          color = TextSecondary,
          lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = "Session Duration",
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextPrimary,
          modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          durations.forEach { duration ->
            val isSelected = selectedDuration == duration
            Box(
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) GoldPrimary else DarkSurface)
                .border(1.dp, if (isSelected) GoldPrimary else DarkBorder, RoundedCornerShape(10.dp))
                .clickable(enabled = !isLoading) { selectedDuration = duration }
                .testTag("duration_${duration}_btn"),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.Schedule,
                  contentDescription = null,
                  tint = if (isSelected) com.example.ui.theme.DarkTextOnGold else TextSecondary,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "$duration min",
                  fontSize = 13.5.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) com.example.ui.theme.DarkTextOnGold else TextPrimary
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            enabled = !isLoading,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkBorder),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = TextPrimary)
          ) {
            Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
          }

          val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
          val context = LocalContext.current
          Button(
            onClick = {
              if (!isOnline) {
                com.example.ui.components.OfflineToastHelper.show(context)
              } else {
                onStart(selectedDuration)
              }
            },
            enabled = if (!isOnline) true else !isLoading,
            modifier = Modifier.weight(1.3f).height(48.dp)
              .then(if (!isOnline) Modifier.alpha(0.5f) else Modifier)
              .testTag("confirm_start_attendance_btn"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = com.example.ui.theme.DarkTextOnGold)
          ) {
            if (isLoading) {
              CircularProgressIndicator(color = com.example.ui.theme.DarkTextOnGold, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
              Text("Generate & Start", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.DarkTextOnGold)
            }
          }
        }
      }
    }
  }
}
