package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MyAttendanceStatus
import com.example.data.RiskLevel
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun MyAttendanceStatusCard(
  status: MyAttendanceStatus?,
  isLoading: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (status == null && !isLoading) return

  val riskColor = when (status?.riskLevel) {
    RiskLevel.CRITICAL -> ErrorRed
    RiskLevel.HIGH -> ElectricViolet
    RiskLevel.WATCH -> ChampagneGold
    else -> SuccessGreen
  }
  val pct = (status?.percentage ?: 100.0).coerceIn(0.0, 100.0)
  val progress = (pct / 100.0).toFloat()
  val pctLabel = String.format(Locale.US, "%.0f%%", pct)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(DarkCard)
      .border(1.dp, riskColor.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(14.dp)
      .testTag("my_attendance_status_card")
  ) {
    if (isLoading && status == null) {
      LoadingSkeleton(height = 72)
    } else if (status != null) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(modifier = Modifier.size(62.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = riskColor.copy(alpha = 0.2f),
            strokeWidth = 6.dp
          )
          CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = riskColor,
            strokeWidth = 6.dp,
            strokeCap = StrokeCap.Round
          )
          Text(
            text = pctLabel,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = riskColor
          )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("My attendance", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldLight)
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(riskColor.copy(alpha = 0.18f))
                .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
              Text(
                text = status.riskLevel.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = riskColor
              )
            }
          }
          Spacer(modifier = Modifier.height(3.dp))
          val accounted = status.attended + status.excused
          Text(
            text = "$accounted of ${status.totalSessions} sessions attended",
            fontSize = 13.sp,
            color = TextPrimary
          )
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = status.encouragementMessage,
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 17.sp
          )
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open my attendance", tint = GoldLight, modifier = Modifier.size(18.dp))
      }
    }
  }
}
