package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextSecondary

enum class ClassTab {
  Notices,
  Attendance,
  Timetable,
  Groups,
  More
}

@Composable
fun ClassBottomNavigationBar(
  selectedTab: ClassTab,
  onSelectTab: (ClassTab) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(64.dp)
      .background(DarkSurface)
      .border(1.dp, DarkBorder)
      .testTag("class_bottom_nav_bar"),
    horizontalArrangement = Arrangement.SpaceAround,
    verticalAlignment = Alignment.CenterVertically
  ) {
    ClassNavItem(
      label = "Notices",
      icon = Icons.Default.Campaign,
      isSelected = selectedTab == ClassTab.Notices,
      onClick = { onSelectTab(ClassTab.Notices) },
      tag = "class_nav_notices"
    )
    ClassNavItem(
      label = "Attendance",
      icon = Icons.Default.CheckCircle,
      isSelected = selectedTab == ClassTab.Attendance,
      onClick = { onSelectTab(ClassTab.Attendance) },
      tag = "class_nav_attendance"
    )
    ClassNavItem(
      label = "Timetable",
      icon = Icons.Default.CalendarMonth,
      isSelected = selectedTab == ClassTab.Timetable,
      onClick = { onSelectTab(ClassTab.Timetable) },
      tag = "class_nav_timetable"
    )
    ClassNavItem(
      label = "Groups",
      icon = Icons.Default.Groups,
      isSelected = selectedTab == ClassTab.Groups,
      onClick = { onSelectTab(ClassTab.Groups) },
      tag = "class_nav_groups"
    )
    ClassNavItem(
      label = "More",
      icon = Icons.Default.MoreHoriz,
      isSelected = selectedTab == ClassTab.More,
      onClick = { onSelectTab(ClassTab.More) },
      tag = "class_nav_more"
    )
  }
}

@Composable
private fun ClassNavItem(
  label: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  tag: String
) {
  val tint = if (isSelected) GoldPrimary else TextSecondary
  Column(
    modifier = Modifier
      .clickable { onClick() }
      .padding(horizontal = 8.dp, vertical = 6.dp)
      .testTag(tag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      icon,
      contentDescription = label,
      tint = tint,
      modifier = Modifier.size(22.dp)
    )
    Text(
      text = label,
      style = CaptionStyle.copy(
        color = tint,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        fontSize = 13.sp
      )
    )
  }
}
