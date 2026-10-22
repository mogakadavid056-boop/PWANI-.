package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceSessionItem
import com.example.data.AttendanceSubScreen
import com.example.data.ClassItem
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
import com.example.ui.components.ClassBottomNavigationBar
import com.example.ui.components.ClassDialogs
import com.example.ui.components.ClassRepPullToRefresh
import com.example.ui.components.ClassScreenFab
import com.example.ui.components.ClassSwitcherBottomSheet
import com.example.ui.components.ClassTab
import com.example.ui.components.ClassTabContent
import com.example.ui.components.ClassTopAppBar
import com.example.ui.components.FriendlyErrorSnackbar
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ItemSpacing
import com.example.ui.theme.ScreenPadding

enum class MoreSubView { None, Materials, Polls }

@Composable
fun ClassScreen(
  classItem: ClassItem,
  isStaff: Boolean,
  prefects: List<com.example.data.ClassPrefectInfo> = emptyList(),
  isPrefect: Boolean = false,
  session: UserSession? = null,
  notices: List<NoticeItem>,
  readNoticeIds: Set<String>,
  canPostNotice: Boolean,
  isLoading: Boolean,
  errorMessage: String?,
  activeNoticeDetail: NoticeItem?, noticePendingEdit: NoticeItem?, noticePendingDelete: NoticeItem?,
  onDismissError: () -> Unit, onRefreshNotices: () -> Unit, onNoticeClick: (NoticeItem) -> Unit,
  onCloseNoticeDetail: () -> Unit, onNavigateToPostNotice: () -> Unit,
  onEditNoticeClick: (NoticeItem) -> Unit, onDeleteNoticeClick: (NoticeItem) -> Unit,
  onDismissEditNotice: () -> Unit,
  onSaveEditNotice: (id: String, title: String, content: String, cat: String, urgent: Boolean, pinned: Boolean) -> Unit,
  onDismissPromptDeleteNotice: () -> Unit, onConfirmDeleteNotice: () -> Unit,
  onUpdateClassName: (newName: String) -> Unit, onNavigateToManageMembers: () -> Unit,
  attendanceUiState: AttendanceUiState, onRefreshAttendance: () -> Unit,
  onStartAttendance: (durationMinutes: Int) -> Unit, onOpenViewAttendance: (sessionId: String) -> Unit,
  onOpenSignAttendance: (sessionId: String) -> Unit, onCloseAttendanceSubScreen: () -> Unit,
  onOpenAttendanceSummary: () -> Unit = {}, onOpenMyAttendance: () -> Unit = {},
  onSignAttendanceSubmit: (code: String, signatureBase64: String) -> Unit,
  onPromptDeleteSession: (AttendanceSessionItem) -> Unit, onDismissPromptDeleteSession: () -> Unit,
  onConfirmDeleteSession: () -> Unit, onDismissAttendanceError: () -> Unit, onDismissAttendanceSuccess: () -> Unit,
  groupsUiState: GroupsUiState, onRefreshGroups: () -> Unit,
  onCreateGroup: (name: String, desc: String, size: Int) -> Unit, onOpenChat: (DiscussionGroupItem) -> Unit,
  onJoinGroup: (DiscussionGroupItem) -> Unit, onDismissGroupsError: () -> Unit,
  timetableUiState: TimetableUiState, onRefreshTimetable: () -> Unit, onSelectTimetableDay: (Int) -> Unit,
  onAddScheduleClick: () -> Unit, onEditScheduleClick: (ScheduleItem) -> Unit, onDeleteScheduleClick: (ScheduleItem) -> Unit,
  onDismissAddEditSchedule: () -> Unit,
  onSaveAddSchedule: (title: String, day: Int, start: String, end: String, room: String, lecturer: String) -> Unit,
  onSaveEditSchedule: (id: String, title: String, day: Int, start: String, end: String, room: String, lecturer: String) -> Unit,
  onDismissPromptDeleteSchedule: () -> Unit, onConfirmDeleteSchedule: () -> Unit, onDismissTimetableError: () -> Unit,
  materialsUiState: MaterialsUiState, onRefreshMaterials: () -> Unit,
  onMaterialsSearchChange: (String) -> Unit, onMaterialsSelectTypeFilter: (String) -> Unit,
  onAddMaterialClick: () -> Unit, onOpenFileMaterial: (MaterialItem) -> Unit = {},
  onDownloadMaterial: (MaterialItem) -> Unit = {}, onRemoveDownloadMaterial: (MaterialItem) -> Unit = {},
  onEditMaterialClick: (MaterialItem) -> Unit, onDeleteMaterialClick: (MaterialItem) -> Unit,
  onDismissAddEditMaterial: () -> Unit,
  onSaveAddMaterial: (title: String, desc: String, type: String, url: String) -> Unit,
  onSaveUploadMaterial: (title: String, desc: String, fileName: String, fileSize: Long, mimeType: String, inputStreamProvider: () -> java.io.InputStream) -> Unit = { _, _, _, _, _, _ -> },
  onSaveEditMaterial: (id: String, title: String, desc: String) -> Unit,
  onDismissPromptDeleteMaterial: () -> Unit, onConfirmDeleteMaterial: () -> Unit, onDismissMaterialsError: () -> Unit,
  pollsUiState: PollsUiState, onRefreshPolls: () -> Unit, onCreatePollClick: () -> Unit, onPollClick: (PollItem) -> Unit,
  onSelectPollOption: (String) -> Unit, onCastVote: () -> Unit,
  onPromptClosePoll: () -> Unit, onDismissClosePoll: () -> Unit, onConfirmClosePoll: () -> Unit,
  onPromptDeletePoll: () -> Unit, onDismissDeletePoll: () -> Unit, onConfirmDeletePoll: () -> Unit,
  onDismissPollsError: () -> Unit, onClosePollDetail: () -> Unit, onDismissCreatePoll: () -> Unit,
  onCreatePollSubmit: (String, List<String>, String) -> Unit,
  allClasses: List<ClassItem> = emptyList(),
  onSwitchClass: (ClassItem) -> Unit = {},
  onJoinWithCode: () -> Unit = {},
  onNavigateToCreateClass: () -> Unit = {},
  onNavigateToProfile: () -> Unit = {},
  onBack: () -> Unit
) {
  var selectedTab by remember { mutableStateOf(ClassTab.Notices) }
  var moreSubView by remember { mutableStateOf(MoreSubView.None) }
  var showStartAttendanceDialog by remember { mutableStateOf(false) }
  var showEditClassNameDialog by remember { mutableStateOf(false) }
  var showCreateGroupDialog by remember { mutableStateOf(false) }
  var showClassSwitcherSheet by remember { mutableStateOf(false) }

  val isOverlayActive = HandleClassSubScreenOverlay(
    attendanceUiState = attendanceUiState,
    pollsUiState = pollsUiState,
    isStaff = isStaff,
    onDismissAttendanceError = onDismissAttendanceError,
    onOpenViewAttendance = onOpenViewAttendance,
    onCloseAttendanceSubScreen = onCloseAttendanceSubScreen,
    onDismissAttendanceSuccess = onDismissAttendanceSuccess,
    onSignAttendanceSubmit = onSignAttendanceSubmit,
    onSelectPollOption = onSelectPollOption,
    onCastVote = onCastVote,
    onPromptClosePoll = onPromptClosePoll,
    onDismissClosePoll = onDismissClosePoll,
    onConfirmClosePoll = onConfirmClosePoll,
    onPromptDeletePoll = onPromptDeletePoll,
    onDismissDeletePoll = onDismissDeletePoll,
    onConfirmDeletePoll = onConfirmDeletePoll,
    onDismissPollsError = onDismissPollsError,
    onClosePollDetail = onClosePollDetail
  )
  if (isOverlayActive) return

  if (selectedTab == ClassTab.More && moreSubView != MoreSubView.None) {
    BackHandler { moreSubView = MoreSubView.None }
  } else {
    BackHandler(onBack = onBack)
  }

  val isTabRefreshing = when (selectedTab) {
    ClassTab.Notices -> isLoading
    ClassTab.Attendance -> attendanceUiState.isLoading
    ClassTab.Groups -> groupsUiState.isLoadingGroups
    ClassTab.Timetable -> timetableUiState.isLoading
    ClassTab.More -> if (moreSubView == MoreSubView.Materials) materialsUiState.isLoading else if (moreSubView == MoreSubView.Polls) pollsUiState.isLoading else false
  }

  val onTabRefresh: () -> Unit = {
    when (selectedTab) {
      ClassTab.Notices -> onRefreshNotices()
      ClassTab.Attendance -> onRefreshAttendance()
      ClassTab.Groups -> onRefreshGroups()
      ClassTab.Timetable -> onRefreshTimetable()
      ClassTab.More -> if (moreSubView == MoreSubView.Materials) onRefreshMaterials() else if (moreSubView == MoreSubView.Polls) onRefreshPolls() else onRefreshNotices()
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(DarkBackground).testTag("class_screen")) {
    Column(modifier = Modifier.fillMaxSize()) {
      val showSwitcher = isStaff && !isPrefect
      ClassTopAppBar(
        classItem = classItem, isStaff = isStaff,
        onBack = { if (selectedTab == ClassTab.More && moreSubView != MoreSubView.None) moreSubView = MoreSubView.None else onBack() },
        onEditClassNameClick = { showEditClassNameDialog = true },
        onOpenClassSwitcher = if (showSwitcher) { { showClassSwitcherSheet = true } } else null,
        onNavigateToProfile = onNavigateToProfile
      )

      ClassRepPullToRefresh(
        isRefreshing = isTabRefreshing,
        onRefresh = onTabRefresh,
        modifier = Modifier.weight(1f)
      ) {
        Column(
          modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = ScreenPadding, vertical = ScreenPadding),
          verticalArrangement = Arrangement.spacedBy(ItemSpacing)
        ) {
          com.example.ui.components.ClassPrefectBanner(prefects = prefects)
          val activeTabErr = when (selectedTab) {
            ClassTab.Notices -> errorMessage
            ClassTab.Attendance -> attendanceUiState.errorMessage
            ClassTab.Groups -> groupsUiState.errorMessage
            ClassTab.Timetable -> timetableUiState.errorMessage
            ClassTab.More -> if (moreSubView == MoreSubView.Materials) materialsUiState.errorMessage else if (moreSubView == MoreSubView.Polls) pollsUiState.errorMessage else null
          }
          val onDismissTabErr = when (selectedTab) {
            ClassTab.Notices -> onDismissError
            ClassTab.Attendance -> onDismissAttendanceError
            ClassTab.Groups -> onDismissGroupsError
            ClassTab.Timetable -> onDismissTimetableError
            ClassTab.More -> if (moreSubView == MoreSubView.Materials) onDismissMaterialsError else if (moreSubView == MoreSubView.Polls) onDismissPollsError else onDismissError
          }
          if (!activeTabErr.isNullOrBlank()) FriendlyErrorSnackbar(errorMessage = activeTabErr, onDismiss = onDismissTabErr)

          AnimatedContent(
            targetState = selectedTab to moreSubView,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
            label = "class_tab_content_transition"
          ) { (curTab, curMore) ->
            ClassTabContent(
              selectedTab = curTab, moreSubView = curMore, isStaff = isStaff, notices = notices,
              readNoticeIds = readNoticeIds, canPostNotice = canPostNotice, isLoading = isLoading,
              attendanceUiState = attendanceUiState, groupsUiState = groupsUiState, timetableUiState = timetableUiState,
              materialsUiState = materialsUiState, pollsUiState = pollsUiState, onRefreshNotices = onRefreshNotices,
              onNoticeClick = onNoticeClick, onNavigateToPostNotice = onNavigateToPostNotice, onEditNoticeClick = onEditNoticeClick,
              onDeleteNoticeClick = onDeleteNoticeClick, onStartAttendanceClick = { showStartAttendanceDialog = true },
              onRefreshAttendance = onRefreshAttendance, onOpenViewAttendance = onOpenViewAttendance, onOpenSignAttendance = onOpenSignAttendance,
              onOpenAttendanceSummary = onOpenAttendanceSummary, onOpenMyAttendance = onOpenMyAttendance,
              onDeleteSession = onPromptDeleteSession, onRefreshGroups = onRefreshGroups, onCreateGroupClick = { showCreateGroupDialog = true },
              onOpenChat = onOpenChat, onJoinGroup = onJoinGroup, onRefreshTimetable = onRefreshTimetable,
              onSelectTimetableDay = onSelectTimetableDay, onAddScheduleClick = onAddScheduleClick, onEditScheduleClick = onEditScheduleClick,
              onDeleteScheduleClick = onDeleteScheduleClick, onRefreshMaterials = onRefreshMaterials, onMaterialsSearchChange = onMaterialsSearchChange,
              onMaterialsSelectTypeFilter = onMaterialsSelectTypeFilter, onAddMaterialClick = onAddMaterialClick,
              onOpenFileMaterial = onOpenFileMaterial, onDownloadMaterial = onDownloadMaterial, onRemoveDownloadMaterial = onRemoveDownloadMaterial,
              onEditMaterialClick = onEditMaterialClick,
              onDeleteMaterialClick = onDeleteMaterialClick, onRefreshPolls = onRefreshPolls, onCreatePollClick = onCreatePollClick,
              onPollClick = onPollClick, onOpenMaterials = { moreSubView = MoreSubView.Materials }, onOpenPolls = { moreSubView = MoreSubView.Polls },
              onOpenManageMembers = onNavigateToManageMembers, onBackFromMoreSubView = { moreSubView = MoreSubView.None }
            )
          }
          Spacer(modifier = Modifier.height(64.dp))
        }
      }

      ClassBottomNavigationBar(
        selectedTab = selectedTab,
        onSelectTab = { tab -> selectedTab = tab; if (tab != ClassTab.More) moreSubView = MoreSubView.None }
      )
    }

    ClassScreenFab(
      selectedTab = selectedTab,
      moreSubView = moreSubView,
      isStaff = isStaff,
      canPostNotice = canPostNotice,
      onNavigateToPostNotice = onNavigateToPostNotice,
      onStartAttendanceClick = { showStartAttendanceDialog = true },
      onAddScheduleClick = onAddScheduleClick,
      onCreateGroupClick = { showCreateGroupDialog = true },
      onAddMaterialClick = onAddMaterialClick,
      onCreatePollClick = onCreatePollClick
    )

    ClassDialogs(
      activeNoticeDetail = activeNoticeDetail, onCloseNoticeDetail = onCloseNoticeDetail,
      showStartAttendanceDialog = showStartAttendanceDialog, onDismissStartAttendance = { showStartAttendanceDialog = false },
      onStartAttendance = onStartAttendance, isAttendanceActionLoading = attendanceUiState.isActionLoading,
      showEditClassNameDialog = showEditClassNameDialog, currentClassName = classItem.name,
      onDismissEditClassName = { showEditClassNameDialog = false }, onSaveClassName = onUpdateClassName,
      showCreateGroupDialog = showCreateGroupDialog, isGroupActionLoading = groupsUiState.isActionLoading,
      onDismissCreateGroup = { showCreateGroupDialog = false }, onCreateGroup = onCreateGroup,
      noticePendingEdit = noticePendingEdit, onDismissEditNotice = onDismissEditNotice, onSaveEditNotice = onSaveEditNotice,
      noticePendingDelete = noticePendingDelete, onDismissPromptDeleteNotice = onDismissPromptDeleteNotice,
      onConfirmDeleteNotice = onConfirmDeleteNotice, sessionPendingDelete = attendanceUiState.sessionPendingDelete,
      onDismissPromptDeleteSession = onDismissPromptDeleteSession, onConfirmDeleteSession = onConfirmDeleteSession,
      timetableUiState = timetableUiState, onDismissAddEditSchedule = onDismissAddEditSchedule,
      onSaveAddSchedule = onSaveAddSchedule, onSaveEditSchedule = onSaveEditSchedule,
      onDismissPromptDeleteSchedule = onDismissPromptDeleteSchedule, onConfirmDeleteSchedule = onConfirmDeleteSchedule,
      materialsUiState = materialsUiState, onDismissAddEditMaterial = onDismissAddEditMaterial,
      onSaveAddLinkMaterial = onSaveAddMaterial, onSaveUploadMaterial = onSaveUploadMaterial, onSaveEditMaterial = onSaveEditMaterial,
      onDismissPromptDeleteMaterial = onDismissPromptDeleteMaterial, onConfirmDeleteMaterial = onConfirmDeleteMaterial,
      showCreatePollDialog = pollsUiState.showCreateDialog, isPollActionLoading = pollsUiState.isActionLoading,
      onDismissCreatePollDialog = onDismissCreatePoll, onCreatePoll = onCreatePollSubmit
    )

    if (showClassSwitcherSheet) {
      ClassSwitcherBottomSheet(
        classes = allClasses,
        currentClassId = classItem.id,
        onSelectClass = onSwitchClass,
        onJoinWithCode = onJoinWithCode,
        onCreateClass = onNavigateToCreateClass,
        onDismiss = { showClassSwitcherSheet = false }
      )
    }
  }
}
