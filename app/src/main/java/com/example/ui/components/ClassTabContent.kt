package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarms.AlarmPreferences
import com.example.data.DiscussionGroupItem
import com.example.data.MaterialItem
import com.example.data.NoticeItem
import com.example.data.PollItem
import com.example.data.ScheduleItem
import com.example.data.UserSession
import com.example.ui.AttendanceUiState
import com.example.ui.GroupsUiState
import com.example.ui.MaterialsUiState
import com.example.ui.PollsUiState
import com.example.ui.TimetableUiState
import com.example.ui.screens.AlarmSettingsScreen
import com.example.ui.screens.MoreSubView
import com.example.ui.theme.GoldLight
import com.example.ui.theme.SectionHeadingStyle

@Composable
fun ClassTabContent(
  selectedTab: ClassTab,
  moreSubView: MoreSubView,
  isStaff: Boolean,
  isLecturer: Boolean = false,
  session: UserSession? = null,
  notices: List<NoticeItem>,
  readNoticeIds: Set<String>,
  canPostNotice: Boolean,
  isLoading: Boolean,
  attendanceUiState: AttendanceUiState,
  trackerUiState: com.example.ui.AttendanceTrackerUiState = com.example.ui.AttendanceTrackerUiState(),
  groupsUiState: GroupsUiState,
  timetableUiState: TimetableUiState,
  materialsUiState: MaterialsUiState,
  pollsUiState: PollsUiState,
  challengesUiState: com.example.ui.ChallengesUiState = com.example.ui.ChallengesUiState(),
  isAdmin: Boolean = false,
  showOnLeaderboard: Boolean = true,
  challengesCallbacks: com.example.ui.screens.ClassChallengesCallbacks = com.example.ui.screens.ClassChallengesCallbacks(),
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
  onOpenAttendanceTracker: () -> Unit = {},
  onOpenRegister: (String) -> Unit = {},
  onCloseSession: (String) -> Unit = {},
  onSignOutSession: (String) -> Unit = {},
  onShowActiveCode: ((String, String) -> Unit)? = null,
  onDeleteSession: (com.example.data.AttendanceSessionItem) -> Unit,
  onDeleteRegisterPrompt: (com.example.data.AttendanceRegisterListItem) -> Unit = {},
  onRefreshGroups: () -> Unit,
  onCreateGroupClick: () -> Unit,
  onOpenChat: (DiscussionGroupItem) -> Unit,
  onJoinGroup: (DiscussionGroupItem) -> Unit,
  onRefreshTimetable: () -> Unit,
  onSelectTimetableDay: (Int) -> Unit,
  onAddScheduleClick: () -> Unit,
  onEditScheduleClick: (ScheduleItem) -> Unit,
  onDeleteScheduleClick: (ScheduleItem) -> Unit,
  onStatusScheduleClick: (ScheduleItem) -> Unit = {},
  onRefreshMaterials: () -> Unit,
  onMaterialsSearchChange: (String) -> Unit,
  onMaterialsSelectUnitFilter: (String?) -> Unit = {},
  onRegisterUnitClick: () -> Unit = {},
  onAddMaterialClick: () -> Unit,
  onOpenFileMaterial: (MaterialItem) -> Unit = {},
  onDownloadMaterial: (MaterialItem) -> Unit = {},
  onRemoveDownloadMaterial: (MaterialItem) -> Unit = {},
  onEditMaterialClick: (MaterialItem) -> Unit,
  onDeleteMaterialClick: (MaterialItem) -> Unit,
  onRefreshPolls: () -> Unit,
  onCreatePollClick: () -> Unit,
  onPollClick: (PollItem) -> Unit,
  onOpenMaterials: () -> Unit,
  onOpenPolls: () -> Unit,
  onOpenChallenges: () -> Unit = {},
  onOpenLeaderboard: () -> Unit = {},
  onOpenAlarmSettings: () -> Unit = {},
  onOpenManageMembers: () -> Unit,
  onBackFromMoreSubView: () -> Unit
) {
  val alarmSettings by AlarmPreferences.state.collectAsStateWithLifecycle()

  when (selectedTab) {
    ClassTab.Notices -> NoticesSection(
      notices = notices, readNoticeIds = readNoticeIds, canPostNotice = canPostNotice, isStaff = isStaff, isLoading = isLoading,
      showMyAttendanceCard = !isStaff && !isLecturer,
      myAttendanceStatus = trackerUiState.myStatus,
      isMyAttendanceLoading = trackerUiState.isMyStatusLoading,
      showActivityStatsCard = !isLecturer && !isAdmin,
      activityStats = challengesUiState.activityStats,
      isActivityStatsLoading = challengesUiState.isLoadingStats,
      openChallengesCount = challengesUiState.openCount,
      onOpenChallenges = onOpenChallenges,
      onOpenLeaderboard = onOpenLeaderboard,
      onOpenMyAttendance = onOpenMyAttendance,
      onRefreshNotices = onRefreshNotices, onNoticeClick = onNoticeClick, onNavigateToPostNotice = onNavigateToPostNotice,
      onEditNotice = onEditNoticeClick, onDeleteNotice = onDeleteNoticeClick
    )
    ClassTab.Attendance -> AttendanceSection(
      isStaff = isStaff,
      isLecturer = isLecturer,
      canSignAttendance = attendanceUiState.canSignAttendance,
      isLoading = attendanceUiState.isLoading, staffSessions = attendanceUiState.staffSessions, openSessions = attendanceUiState.openSessions,
      registers = attendanceUiState.registers,
      activeSessionCode = attendanceUiState.activeSessionCode, activeSessionId = attendanceUiState.activeSessionId, activeRemainingSeconds = attendanceUiState.activeSessionRemainingSeconds,
      canManageActiveSession = attendanceUiState.canManageActiveSession,
      onStartAttendanceClick = onStartAttendanceClick, onRefreshSessions = onRefreshAttendance, onOpenViewAttendance = onOpenViewAttendance,
      onOpenSignAttendance = onOpenSignAttendance, onOpenAttendanceSummary = onOpenAttendanceSummary,
      onOpenMyAttendance = onOpenMyAttendance, onOpenAttendanceTracker = onOpenAttendanceTracker,
      onOpenRegister = onOpenRegister, onCloseSession = onCloseSession,
      onSignOutSession = onSignOutSession, onVoidSessionPrompt = onDeleteSession, onDeleteRegisterPrompt = onDeleteRegisterPrompt, onShowActiveCode = onShowActiveCode
    )
    ClassTab.Timetable -> TimetableSection(
      schedules = timetableUiState.schedules, selectedDay = timetableUiState.selectedDay, todayDayOfWeek = timetableUiState.todayDayOfWeek,
      isStaff = isStaff, isLoading = timetableUiState.isLoading, onRefresh = onRefreshTimetable, onSelectDay = onSelectTimetableDay,
      onAddClassClick = onAddScheduleClick, onEditSchedule = onEditScheduleClick, onDeleteSchedule = onDeleteScheduleClick,
      onChangeScheduleStatus = onStatusScheduleClick
    )
    ClassTab.Groups -> GroupsSection(
      groups = groupsUiState.groups, isLoading = groupsUiState.isLoadingGroups, isStaff = isStaff, onRefreshGroups = onRefreshGroups,
      onCreateGroupClick = onCreateGroupClick, onOpenChat = onOpenChat, onJoinGroup = onJoinGroup
    )
    ClassTab.More -> when (moreSubView) {
      MoreSubView.None -> ClassMoreMenu(
        isStaff = isStaff,
        materialsCount = materialsUiState.materials.size,
        pollsCount = pollsUiState.polls.size,
        openChallengesCount = challengesUiState.openCount,
        totalChallengesCount = challengesUiState.quizzes.size,
        weekPoints = challengesUiState.activityStats?.weekPoints ?: 0,
        alarmsEnabled = alarmSettings.masterEnabled,
        alarmReminderMinutes = alarmSettings.reminderMinutes,
        onOpenMaterials = onOpenMaterials,
        onOpenPolls = onOpenPolls,
        onOpenChallenges = onOpenChallenges,
        onOpenLeaderboard = onOpenLeaderboard,
        onOpenAlarmSettings = onOpenAlarmSettings,
        onOpenManageMembers = onOpenManageMembers
      )
      MoreSubView.Challenges -> {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBackFromMoreSubView) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
          }
          Text("Challenges", style = SectionHeadingStyle, color = GoldLight)
        }
        com.example.ui.screens.ChallengesSection(
          uiState = challengesUiState,
          isStaff = isStaff,
          canPlayChallenges = com.example.util.RoleHelper.canSignAttendance(session?.role),
          isAdmin = isAdmin,
          isLecturer = isLecturer,
          currentUserId = session?.userId,
          onRefresh = challengesCallbacks.onRefreshAll,
          onSelectFilter = challengesCallbacks.onSelectFilter,
          onCreateClick = challengesCallbacks.onOpenCreateDialog,
          onAddQuestionsClick = challengesCallbacks.onOpenAddQuestionsDialog,
          onPlayClick = challengesCallbacks.onPlayChallenge,
          onReviewClick = challengesCallbacks.onOpenReview,
          onResultsClick = challengesCallbacks.onOpenResults,
          onMarkPapersClick = challengesCallbacks.onOpenExamSubmissions,
          onViewMarkedPaperClick = challengesCallbacks.onViewMarkedPaperForStudent,
          onDeleteClick = challengesCallbacks.onPromptDelete,
          onOpenLeaderboard = onOpenLeaderboard,
          onDismissError = challengesCallbacks.onDismissError,
          onDismissSuccess = challengesCallbacks.onDismissSuccess
        )
      }
      MoreSubView.Leaderboard -> {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBackFromMoreSubView) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
          }
          Text("Leaderboard", style = SectionHeadingStyle, color = GoldLight)
        }
        com.example.ui.screens.LeaderboardSection(
          uiState = challengesUiState,
          showPrivacyToggle = com.example.util.RoleHelper.canSignAttendance(session?.role),
          showOnLeaderboard = showOnLeaderboard,
          onToggleVisibility = challengesCallbacks.onToggleLeaderboardVisibility,
          onSelectPeriod = challengesCallbacks.onSelectLeaderboardPeriod,
          onRefresh = challengesCallbacks.onRefreshAll,
          onDismissError = challengesCallbacks.onDismissError
        )
      }
      MoreSubView.AlarmSettings -> {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBackFromMoreSubView) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
          }
          Text("Class Alarm Settings", style = SectionHeadingStyle, color = GoldLight)
        }
        AlarmSettingsScreen(
          currentClassSchedules = timetableUiState.schedules,
          session = session
        )
      }
      MoreSubView.Materials -> {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBackFromMoreSubView) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
          }
          Text("Course Materials", style = SectionHeadingStyle, color = GoldLight)
        }
        MaterialsSection(
          units = materialsUiState.units,
          groupedMaterials = materialsUiState.groupedMaterials,
          materialsCount = materialsUiState.filteredMaterials.size,
          searchQuery = materialsUiState.searchQuery,
          selectedUnitFilterId = materialsUiState.selectedUnitFilterId,
          isStaff = isStaff,
          isLecturer = isLecturer,
          isLoading = materialsUiState.isLoading,
          isUnitsLoading = materialsUiState.isUnitsLoading,
          successMessage = materialsUiState.successMessage,
          downloadingMaterialId = materialsUiState.downloadingMaterialId,
          downloadProgress = materialsUiState.downloadProgress,
          onSearchChange = onMaterialsSearchChange,
          onSelectUnitFilter = onMaterialsSelectUnitFilter,
          onRegisterUnitClick = onRegisterUnitClick,
          onRefresh = onRefreshMaterials,
          onAddMaterialClick = onAddMaterialClick,
          onOpenFileMaterial = onOpenFileMaterial,
          onDownloadMaterial = onDownloadMaterial,
          onRemoveDownloadMaterial = onRemoveDownloadMaterial,
          onEditMaterial = onEditMaterialClick,
          onDeleteMaterial = onDeleteMaterialClick
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
