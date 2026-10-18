package com.example.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.screens.MoreSubView
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GoldPrimary

@Composable
fun BoxScope.ClassScreenFab(
  selectedTab: ClassTab,
  moreSubView: MoreSubView,
  isStaff: Boolean,
  canPostNotice: Boolean,
  onNavigateToPostNotice: () -> Unit,
  onStartAttendanceClick: () -> Unit,
  onAddScheduleClick: () -> Unit,
  onCreateGroupClick: () -> Unit,
  onAddMaterialClick: () -> Unit,
  onCreatePollClick: () -> Unit
) {
  val showFab = when (selectedTab) {
    ClassTab.Notices -> canPostNotice
    ClassTab.Attendance -> isStaff
    ClassTab.Timetable -> isStaff
    ClassTab.Groups -> isStaff
    ClassTab.More -> (moreSubView == MoreSubView.Materials || moreSubView == MoreSubView.Polls) && isStaff
  }

  if (showFab) {
    FloatingActionButton(
      onClick = {
        when (selectedTab) {
          ClassTab.Notices -> onNavigateToPostNotice()
          ClassTab.Attendance -> onStartAttendanceClick()
          ClassTab.Timetable -> onAddScheduleClick()
          ClassTab.Groups -> onCreateGroupClick()
          ClassTab.More -> if (moreSubView == MoreSubView.Materials) onAddMaterialClick() else if (moreSubView == MoreSubView.Polls) onCreatePollClick()
        }
      },
      containerColor = GoldPrimary,
      contentColor = com.example.ui.theme.DarkTextOnGold,
      shape = CircleShape,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 20.dp, bottom = 80.dp)
        .testTag("class_screen_fab")
    ) {
      val fabIcon = when (selectedTab) {
        ClassTab.Notices -> Icons.Default.PostAdd
        ClassTab.Attendance -> Icons.Default.PlayArrow
        else -> Icons.Default.Add
      }
      Icon(fabIcon, contentDescription = "Action", modifier = Modifier.size(24.dp))
    }
  }
}
