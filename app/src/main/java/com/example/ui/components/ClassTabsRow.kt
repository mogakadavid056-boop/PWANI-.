package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class ClassTabInfo(val label: String, val icon: ImageVector)

@Composable
fun ClassTabsRow(
  selectedTab: Int,
  noticesCount: Int,
  attendanceCount: Int,
  groupsCount: Int,
  timetableCount: Int,
  materialsCount: Int,
  pollsCount: Int,
  onSelectTab: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val tabs = listOf(
    ClassTabInfo("Notices ($noticesCount)", Icons.Default.Campaign),
    ClassTabInfo("Attendance ($attendanceCount)", Icons.Default.HowToReg),
    ClassTabInfo("Groups ($groupsCount)", Icons.Default.Forum),
    ClassTabInfo("Timetable ($timetableCount)", Icons.Default.CalendarMonth),
    ClassTabInfo("Materials ($materialsCount)", Icons.Default.Description),
    ClassTabInfo("Polls ($pollsCount)", Icons.Default.HowToVote)
  )

  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .clip(RoundedCornerShape(12.dp))
      .background(DarkSurface)
      .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
      .padding(3.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    tabs.forEachIndexed { idx, tab ->
      val isSel = selectedTab == idx
      Box(
        modifier = Modifier
          .height(42.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(if (isSel) GoldPrimary else DarkSurface)
          .clickable { onSelectTab(idx) }
          .padding(horizontal = 12.dp)
          .testTag("tab_$idx"),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            tab.icon,
            contentDescription = null,
            tint = if (isSel) com.example.ui.theme.DarkTextOnGold else TextSecondary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = tab.label,
            fontSize = 13.5.sp,
            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
            color = if (isSel) com.example.ui.theme.DarkTextOnGold else TextPrimary
          )
        }
      }
    }
  }
}
