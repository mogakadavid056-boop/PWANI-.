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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.RoyalSurface

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
      .clip(RoundedCornerShape(14.dp))
      .background(RoyalSurface)
      .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    tabs.forEachIndexed { idx, tab ->
      val isSel = selectedTab == idx
      val shape = RoundedCornerShape(10.dp)
      val glowMod = if (isSel) {
        Modifier.shadow(8.dp, shape, ambientColor = ElectricViolet, spotColor = ElectricViolet)
      } else {
        Modifier
      }

      Box(
        modifier = Modifier
          .height(44.dp)
          .then(glowMod)
          .clip(shape)
          .background(
            if (isSel) Brush.horizontalGradient(listOf(ChampagneGold, MetallicGold))
            else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
          )
          .clickable { onSelectTab(idx) }
          .padding(horizontal = 12.dp)
          .testTag("tab_$idx"),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            tab.icon,
            contentDescription = null,
            tint = if (isSel) Midnight else Lavender,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = tab.label,
            style = ButtonTextStyle.copy(
              fontSize = 13.5.sp,
              color = if (isSel) Midnight else Lavender
            )
          )
        }
      }
    }
  }
}
