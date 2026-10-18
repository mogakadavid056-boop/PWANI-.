package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.example.data.DiscussionGroupItem
import com.example.data.MaterialItem
import com.example.data.NoticeItem
import com.example.data.PollItem
import com.example.data.ScheduleItem
import com.example.ui.AttendanceUiState
import com.example.ui.GroupsUiState
import com.example.ui.MaterialsUiState
import com.example.ui.PollsUiState
import com.example.ui.TimetableUiState
import com.example.ui.screens.MoreSubView
import com.example.ui.theme.GoldLight
import com.example.ui.theme.SectionHeadingStyle

@Composable
fun ClassTabContent(
  selectedTab: ClassTab,
  moreSubView: MoreSubView,
  isStaff: Boolean,
  notices: List<NoticeItem>,
  readNoticeIds: Set<String>,
  canPostNotice: Boolean,
  isLoading: Boolean,
  attendanceUiState: AttendanceUiState,
  groupsUiState: GroupsUiState,
  timetableUiState: TimetableUiState,
  materialsUiState: MaterialsUiState,
  pollsUiState: PollsUiState,
  onRefreshNotices: () -> Unit,
  onNoticeClick: (NoticeItem) -> Unit,
  onNavigateToPostNotice: () -> Unit,
  onEditNoticeClick: (NoticeItem) -> Unit,
  onDeleteNoticeClick: (NoticeItem) -> Unit,
  onStartAttendanceClick: () -> Unit,
  onRefreshAttendance: () -> Unit,
  onOpenViewAttendance: (String) -> Unit,
  onOpenSignAttendance: (String) -> Unit,
  onOpenAttendanceSummary: () -> Unit = {},
  onOpenMyAttendance: () -> Unit = {},
  onDeleteSession: (com.example.data.AttendanceSessionItem) -> Unit,
  onRefreshGroups: () -> Unit,
  onCreateGroupClick: () -> Unit,
  onOpenChat: (DiscussionGroupItem) -> Unit,
  onJoinGroup: (DiscussionGroupItem) -> Unit,
  onRefreshTimetable: () -> Unit,
  onSelectTimetableDay: (Int) -> Unit,
  onAddScheduleClick: () -> Unit,
  onEditScheduleClick: (ScheduleItem) -> Unit,
  onDeleteScheduleClick: (ScheduleItem) -> Unit,
  onRefreshMaterials: () -> Unit,
  onMaterialsSearchChange: (String) -> Unit,
  onMaterialsSelectTypeFilter: (String) -> Unit,
  onAddMaterialClick: () -> Unit,
  onEditMaterialClick: (MaterialItem) -> Unit,
  onDeleteMaterialClick: (MaterialItem) -> Unit,
  onRefreshPolls: () -> Unit,
  onCreatePollClick: () -> Unit,
  onPollClick: (PollItem) -> Unit,
  onOpenMaterials: () -> Unit,
  onOpenPolls: () -> Unit,
  onOpenManageMembers: () -> Unit,
  onBackFromMoreSubView: () -> Unit
) {
  when (selectedTab) {
    ClassTab.Notices -> NoticesSection(
      notices = notices, readNoticeIds = readNoticeIds, canPostNotice = canPostNotice, isStaff = isStaff, isLoading = isLoading,
      onRefreshNotices = onRefreshNotices, onNoticeClick = onNoticeClick, onNavigateToPostNotice = onNavigateToPostNotice,
      onEditNotice = onEditNoticeClick, onDeleteNotice = onDeleteNoticeClick
    )
    ClassTab.Attendance -> AttendanceSection(
      isStaff = isStaff, isLoading = attendanceUiState.isLoading, staffSessions = attendanceUiState.staffSessions, openSessions = attendanceUiState.openSessions,
      activeSessionCode = attendanceUiState.activeSessionCode, activeSessionId = attendanceUiState.activeSessionId, activeRemainingSeconds = attendanceUiState.activeSessionRemainingSeconds,
      onStartAttendanceClick = onStartAttendanceClick, onRefreshSessions = onRefreshAttendance, onOpenViewAttendance = onOpenViewAttendance,
      onOpenSignAttendance = onOpenSignAttendance, onOpenAttendanceSummary = onOpenAttendanceSummary,
      onOpenMyAttendance = onOpenMyAttendance, onDeleteSession = onDeleteSession
    )
    ClassTab.Timetable -> TimetableSection(
      schedules = timetableUiState.schedules, selectedDay = timetableUiState.selectedDay, todayDayOfWeek = timetableUiState.todayDayOfWeek,
      isStaff = isStaff, isLoading = timetableUiState.isLoading, onRefresh = onRefreshTimetable, onSelectDay = onSelectTimetableDay,
      onAddClassClick = onAddScheduleClick, onEditSchedule = onEditScheduleClick, onDeleteSchedule = onDeleteScheduleClick
    )
    ClassTab.Groups -> GroupsSection(
      groups = groupsUiState.groups, isLoading = groupsUiState.isLoadingGroups, isStaff = isStaff, onRefreshGroups = onRefreshGroups,
      onCreateGroupClick = onCreateGroupClick, onOpenChat = onOpenChat, onJoinGroup = onJoinGroup
    )
    ClassTab.More -> when (moreSubView) {
      MoreSubView.None -> ClassMoreMenu(
        isStaff = isStaff, materialsCount = materialsUiState.materials.size, pollsCount = pollsUiState.polls.size,
        onOpenMaterials = onOpenMaterials, onOpenPolls = onOpenPolls, onOpenManageMembers = onOpenManageMembers
      )
      MoreSubView.Materials -> {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBackFromMoreSubView) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
          }
          Text("Course Materials", style = SectionHeadingStyle, color = GoldLight)
        }
        MaterialsSection(
          materials = materialsUiState.filteredMaterials, searchQuery = materialsUiState.searchQuery, selectedTypeFilter = materialsUiState.selectedTypeFilter,
          isStaff = isStaff, isLoading = materialsUiState.isLoading, onSearchChange = onMaterialsSearchChange, onSelectTypeFilter = onMaterialsSelectTypeFilter,
          onRefresh = onRefreshMaterials, onAddMaterialClick = onAddMaterialClick, onEditMaterial = onEditMaterialClick, onDeleteMaterial = onDeleteMaterialClick
        )
      }
      MoreSubView.Polls -> {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBackFromMoreSubView) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
          }
          Text("Class Polls", style = SectionHeadingStyle, color = GoldLight)
        }
        PollsSection(
          polls = pollsUiState.polls, isStaff = isStaff, isLoading = pollsUiState.isLoading,
          onRefresh = onRefreshPolls, onCreatePollClick = onCreatePollClick, onPollClick = onPollClick
        )
      }
    }
  }
}
