package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.*
import com.example.ui.components.*
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.ItemSpacing
import com.example.ui.theme.ScreenPadding

enum class MoreSubView { None, Materials, Polls }

@Composable
fun ClassScreen(
  classItem: ClassItem,
  isStaff: Boolean,
  prefects: List<ClassPrefectInfo> = emptyList(),
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
  attendanceUiState: AttendanceUiState,
  attendanceCallbacks: ClassAttendanceCallbacks,
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
  allClasses: List<ClassItem> = emptyList(), isAdmin: Boolean = false, isLecturer: Boolean = false,
  onSwitchClass: (ClassItem) -> Unit = {}, onJoinWithCode: () -> Unit = {}, onNavigateToCreateClass: () -> Unit = {},
  onFindMoreClasses: () -> Unit = {}, onNavigateToProfile: () -> Unit = {}, onBack: () -> Unit
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
    session = session,
    className = classItem.name,
    onDismissAttendanceError = attendanceCallbacks.onDismissAttendanceError,
    onOpenViewAttendance = attendanceCallbacks.onOpenViewAttendance,
    onCloseAttendanceSubScreen = attendanceCallbacks.onCloseAttendanceSubScreen,
    onDismissAttendanceSuccess = attendanceCallbacks.onDismissAttendanceSuccess,
    onUpdateMyDetails = attendanceCallbacks.onUpdateMyDetails,
    onSignAttendanceSubmit = attendanceCallbacks.onSignAttendanceSubmit,
    onCloseRegisterSession = { attendanceUiState.selectedSessionId?.let(attendanceCallbacks.onCloseSession) },
    onVoidRegisterSession = attendanceCallbacks.onVoidRegisterSession,
    onDeleteRegisterSession = attendanceCallbacks.onDeleteRegisterSession,
    onSignOutRegisterSession = { attendanceUiState.selectedSessionId?.let(attendanceCallbacks.onSignOutSession) },
    onSignRegister = attendanceCallbacks.onSignRegister,
    onCorrectRegisterTime = attendanceCallbacks.onCorrectRegisterTime,
    onMarkPresent = attendanceCallbacks.onMarkPresent,
    onUpdateRegisterDetails = attendanceCallbacks.onUpdateRegisterDetails,
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

  val attendanceSnackbarHostState = remember { SnackbarHostState() }
  var attendanceSnackbarIsError by remember { mutableStateOf(true) }
  LaunchedEffect(attendanceUiState.errorMessage, selectedTab) {
    val err = attendanceUiState.errorMessage
    if (selectedTab == ClassTab.Attendance && !err.isNullOrBlank()) {
      attendanceSnackbarIsError = true
      attendanceSnackbarHostState.showSnackbar(err, actionLabel = "Dismiss", duration = SnackbarDuration.Long)
      attendanceCallbacks.onDismissAttendanceError()
    }
  }
  LaunchedEffect(attendanceUiState.successMessage, selectedTab) {
    val msg = attendanceUiState.successMessage
    if (selectedTab == ClassTab.Attendance && !msg.isNullOrBlank()) {
      attendanceSnackbarIsError = false
      attendanceSnackbarHostState.showSnackbar(msg, actionLabel = "OK", duration = SnackbarDuration.Short)
      attendanceCallbacks.onDismissAttendanceSuccess()
    }
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
      ClassTab.Attendance -> attendanceCallbacks.onRefreshAttendance()
      ClassTab.Groups -> onRefreshGroups()
      ClassTab.Timetable -> onRefreshTimetable()
      ClassTab.More -> if (moreSubView == MoreSubView.Materials) onRefreshMaterials() else if (moreSubView == MoreSubView.Polls) onRefreshPolls() else onRefreshNotices()
    }
  }
  val canManageAttendance = if (attendanceUiState.registers.isNotEmpty()) attendanceUiState.registers.any { it.canManage } else isStaff

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

      ClassRepPullToRefresh(isRefreshing = isTabRefreshing, onRefresh = onTabRefresh, modifier = Modifier.weight(1f)) {
        Column(
          modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = ScreenPadding, end = ScreenPadding, top = 20.dp, bottom = ScreenPadding),
          verticalArrangement = Arrangement.spacedBy(ItemSpacing)
        ) {
          ClassPrefectBanner(prefects = prefects)
          val activeTabErr = when (selectedTab) {
            ClassTab.Notices -> errorMessage
            ClassTab.Attendance -> null
            ClassTab.Groups -> groupsUiState.errorMessage
            ClassTab.Timetable -> timetableUiState.errorMessage
            ClassTab.More -> if (moreSubView == MoreSubView.Materials) materialsUiState.errorMessage else if (moreSubView == MoreSubView.Polls) pollsUiState.errorMessage else null
          }
          val onDismissTabErr = when (selectedTab) {
            ClassTab.Notices -> onDismissError
            ClassTab.Attendance -> attendanceCallbacks.onDismissAttendanceError
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
              onRefreshAttendance = attendanceCallbacks.onRefreshAttendance,
              onOpenViewAttendance = attendanceCallbacks.onOpenViewAttendance,
              onOpenSignAttendance = attendanceCallbacks.onOpenSignAttendance,
              onOpenAttendanceSummary = attendanceCallbacks.onOpenAttendanceSummary,
              onOpenMyAttendance = attendanceCallbacks.onOpenMyAttendance,
              onOpenRegister = attendanceCallbacks.onOpenRegister,
              onCloseSession = attendanceCallbacks.onCloseSession,
              onSignOutSession = attendanceCallbacks.onSignOutSession,
              onShowActiveCode = attendanceCallbacks.onShowActiveCode,
              onDeleteSession = attendanceCallbacks.onPromptVoidSession,
              onDeleteRegisterPrompt = attendanceCallbacks.onPromptDeleteRegister,
              onRefreshGroups = onRefreshGroups, onCreateGroupClick = { showCreateGroupDialog = true },
              onOpenChat = onOpenChat, onJoinGroup = onJoinGroup, onRefreshTimetable = onRefreshTimetable,
              onSelectTimetableDay = onSelectTimetableDay, onAddScheduleClick = onAddScheduleClick, onEditScheduleClick = onEditScheduleClick,
              onDeleteScheduleClick = onDeleteScheduleClick, onRefreshMaterials = onRefreshMaterials, onMaterialsSearchChange = onMaterialsSearchChange,
              onMaterialsSelectTypeFilter = onMaterialsSelectTypeFilter, onAddMaterialClick = onAddMaterialClick,
              onOpenFileMaterial = onOpenFileMaterial, onDownloadMaterial = onDownloadMaterial, onRemoveDownloadMaterial = onRemoveDownloadMaterial,
              onEditMaterialClick = onEditMaterialClick, onDeleteMaterialClick = onDeleteMaterialClick, onRefreshPolls = onRefreshPolls,
              onCreatePollClick = onCreatePollClick, onPollClick = onPollClick,
              onOpenMaterials = { moreSubView = MoreSubView.Materials }, onOpenPolls = { moreSubView = MoreSubView.Polls },
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
      selectedTab = selectedTab, moreSubView = moreSubView, isStaff = isStaff,
      canPostNotice = canPostNotice, canManageAttendance = canManageAttendance,
      onNavigateToPostNotice = onNavigateToPostNotice, onStartAttendanceClick = { showStartAttendanceDialog = true },
      onAddScheduleClick = onAddScheduleClick, onCreateGroupClick = { showCreateGroupDialog = true },
      onAddMaterialClick = onAddMaterialClick, onCreatePollClick = onCreatePollClick
    )

    ClassDialogs(
      activeNoticeDetail = activeNoticeDetail, onCloseNoticeDetail = onCloseNoticeDetail,
      showStartAttendanceDialog = showStartAttendanceDialog,
      classUnits = attendanceUiState.classUnits, isUnitsLoading = attendanceUiState.isUnitsLoading,
      onDismissStartAttendance = { showStartAttendanceDialog = false },
      onStartAttendance = attendanceCallbacks.onStartAttendance,
      isAttendanceActionLoading = attendanceUiState.isActionLoading,
      showEditClassNameDialog = showEditClassNameDialog, currentClassName = classItem.name,
      onDismissEditClassName = { showEditClassNameDialog = false }, onSaveClassName = onUpdateClassName,
      showCreateGroupDialog = showCreateGroupDialog, isGroupActionLoading = groupsUiState.isActionLoading,
      onDismissCreateGroup = { showCreateGroupDialog = false }, onCreateGroup = onCreateGroup,
      noticePendingEdit = noticePendingEdit, onDismissEditNotice = onDismissEditNotice, onSaveEditNotice = onSaveEditNotice,
      noticePendingDelete = noticePendingDelete, onDismissPromptDeleteNotice = onDismissPromptDeleteNotice,
      onConfirmDeleteNotice = onConfirmDeleteNotice, sessionPendingDelete = attendanceUiState.sessionPendingVoid,
      onDismissPromptDeleteSession = attendanceCallbacks.onDismissPromptVoidSession,
      onConfirmDeleteSessionWithReason = attendanceCallbacks.onVoidRegisterSession,
      registerPendingDelete = attendanceUiState.registerPendingDelete,
      onDismissPromptDeleteRegister = attendanceCallbacks.onDismissPromptDeleteRegister,
      onConfirmDeleteRegister = attendanceCallbacks.onConfirmDeleteRegister,
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
        classes = allClasses, currentClassId = classItem.id, isAdmin = isAdmin, isLecturer = isLecturer,
        onSelectClass = onSwitchClass, onJoinWithCode = onJoinWithCode, onCreateClass = onNavigateToCreateClass,
        onFindMoreClasses = onFindMoreClasses, onDismiss = { showClassSwitcherSheet = false }
      )
    }

    ClassRepSnackbarHost(
      hostState = attendanceSnackbarHostState,
      isError = attendanceSnackbarIsError,
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp)
    )
  }
}
