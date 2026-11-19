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

enum class MoreSubView { None, Materials, Polls, AlarmSettings }

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
  trackerUiState: AttendanceTrackerUiState = AttendanceTrackerUiState(),
  trackerCallbacks: ClassTrackerCallbacks = ClassTrackerCallbacks(),
  groupsUiState: GroupsUiState, onRefreshGroups: () -> Unit,
  onCreateGroup: (name: String, desc: String, size: Int) -> Unit, onOpenChat: (DiscussionGroupItem) -> Unit,
  onJoinGroup: (DiscussionGroupItem) -> Unit, onDismissGroupsError: () -> Unit,
  timetableUiState: TimetableUiState, onRefreshTimetable: () -> Unit, onSelectTimetableDay: (Int) -> Unit,
  onAddScheduleClick: () -> Unit, onEditScheduleClick: (ScheduleItem) -> Unit, onDeleteScheduleClick: (ScheduleItem) -> Unit,
  onStatusScheduleClick: (ScheduleItem) -> Unit = {}, onDismissAddEditSchedule: () -> Unit,
  onSaveAddSchedule: (title: String, day: Int, start: String, end: String, room: String, lecturer: String) -> Unit,
  onSaveEditSchedule: (id: String, title: String, day: Int, start: String, end: String, room: String, lecturer: String) -> Unit,
  onDismissPromptDeleteSchedule: () -> Unit, onConfirmDeleteSchedule: () -> Unit,
  onDismissScheduleStatus: () -> Unit = {}, onSaveScheduleStatus: (String, String, String) -> Unit = { _, _, _ -> },
  onDismissTimetableError: () -> Unit,
  materialsUiState: MaterialsUiState, onRefreshMaterials: () -> Unit,
  onMaterialsSearchChange: (String) -> Unit, onMaterialsSelectUnitFilter: (String?) -> Unit = {},
  onRegisterUnitClick: () -> Unit = {}, onDismissRegisterUnit: () -> Unit = {},
  onSubmitRegisterUnit: (String, String, String) -> Unit = { _, _, _ -> },
  onAddMaterialClick: () -> Unit, onOpenFileMaterial: (MaterialItem) -> Unit = {},
  onDownloadMaterial: (MaterialItem) -> Unit = {}, onRemoveDownloadMaterial: (MaterialItem) -> Unit = {},
  onEditMaterialClick: (MaterialItem) -> Unit, onDeleteMaterialClick: (MaterialItem) -> Unit,
  onDismissAddEditMaterial: () -> Unit,
  onSaveAddMaterial: (courseId: String, title: String, desc: String, type: String, url: String) -> Unit,
  onSaveUploadMaterial: (courseId: String, title: String, desc: String, fileName: String, fileSize: Long, mimeType: String, inputStreamProvider: () -> java.io.InputStream) -> Unit = { _, _, _, _, _, _, _ -> },
  onSaveEditMaterial: (id: String, title: String, desc: String) -> Unit,
  onDismissPromptDeleteMaterial: () -> Unit, onConfirmDeleteMaterial: () -> Unit, onDismissMaterialsError: () -> Unit,
  pollsUiState: PollsUiState, onRefreshPolls: () -> Unit, onCreatePollClick: () -> Unit, onPollClick: (PollItem) -> Unit,
  onSelectPollOption: (String) -> Unit, onCastVote: () -> Unit,
  onPromptClosePoll: () -> Unit, onDismissClosePoll: () -> Unit, onConfirmClosePoll: () -> Unit,
  onPromptDeletePoll: () -> Unit, onDismissDeletePoll: () -> Unit, onConfirmDeletePoll: () -> Unit,
  onDismissPollsError: () -> Unit, onClosePollDetail: () -> Unit, onDismissCreatePoll: () -> Unit,
  onCreatePollSubmit: (String, List<String>, String) -> Unit,
  onRegenerateJoinCode: () -> Unit = {},
  unreadNotificationCount: Int = 0, onOpenNotifications: () -> Unit = {},
  allClasses: List<ClassItem> = emptyList(), isAdmin: Boolean = false, isLecturer: Boolean = false,
  openTimetableSignal: Int = 0, openTabSignal: Triple<Int, ClassTab, MoreSubView>? = null,
  onSwitchClass: (ClassItem) -> Unit = {}, onJoinWithCode: () -> Unit = {}, onNavigateToCreateClass: () -> Unit = {},
  onFindMoreClasses: () -> Unit = {}, onNavigateToProfile: () -> Unit = {}, onBack: () -> Unit
) {
  var selectedTab by remember { mutableStateOf(ClassTab.Notices) }
  var moreSubView by remember { mutableStateOf(MoreSubView.None) }
  LaunchedEffect(openTimetableSignal) {
    if (openTimetableSignal > 0) { selectedTab = ClassTab.Timetable; moreSubView = MoreSubView.None }
  }
  LaunchedEffect(openTabSignal?.first) {
    val sig = openTabSignal ?: return@LaunchedEffect
    if (sig.first > 0) { selectedTab = sig.second; moreSubView = sig.third }
  }
  var showStartAttendanceDialog by remember { mutableStateOf(false) }
  var showEditClassNameDialog by remember { mutableStateOf(false) }
  var showCreateGroupDialog by remember { mutableStateOf(false) }
  var showClassSwitcherSheet by remember { mutableStateOf(false) }

  val isOverlayActive = HandleClassSubScreenOverlay(
    attendanceUiState = attendanceUiState, pollsUiState = pollsUiState, trackerUiState = trackerUiState,
    isStaff = isStaff, session = session, className = classItem.name,
    onDismissAttendanceError = attendanceCallbacks.onDismissAttendanceError,
    onOpenViewAttendance = attendanceCallbacks.onOpenViewAttendance,
    onCloseAttendanceSubScreen = attendanceCallbacks.onCloseAttendanceSubScreen,
    onDismissAttendanceSuccess = attendanceCallbacks.onDismissAttendanceSuccess,
    onRefreshTracker = trackerCallbacks.onRefreshTracker, onSelectTrackerLevel = trackerCallbacks.onSelectTrackerLevel,
    onTrackerSearchChange = trackerCallbacks.onTrackerSearchChange, onOpenTrackerStudent = trackerCallbacks.onOpenTrackerStudent,
    onCloseTrackerStudent = trackerCallbacks.onCloseTrackerStudent, onExcuseTrackerSession = trackerCallbacks.onExcuseTrackerSession,
    onRemoveTrackerExcuse = trackerCallbacks.onRemoveTrackerExcuse, onDismissTrackerError = trackerCallbacks.onDismissTrackerError,
    onDismissTrackerSuccess = trackerCallbacks.onDismissTrackerSuccess,
    onUpdateMyDetails = attendanceCallbacks.onUpdateMyDetails, onSignAttendanceSubmit = attendanceCallbacks.onSignAttendanceSubmit,
    onCloseRegisterSession = { attendanceUiState.selectedSessionId?.let(attendanceCallbacks.onCloseSession) },
    onVoidRegisterSession = attendanceCallbacks.onVoidRegisterSession, onDeleteRegisterSession = attendanceCallbacks.onDeleteRegisterSession,
    onSignOutRegisterSession = { attendanceUiState.selectedSessionId?.let(attendanceCallbacks.onSignOutSession) },
    onSignRegister = attendanceCallbacks.onSignRegister, onCorrectRegisterTime = attendanceCallbacks.onCorrectRegisterTime,
    onMarkPresent = attendanceCallbacks.onMarkPresent, onUpdateRegisterDetails = attendanceCallbacks.onUpdateRegisterDetails,
    onSelectPollOption = onSelectPollOption, onCastVote = onCastVote, onPromptClosePoll = onPromptClosePoll,
    onDismissClosePoll = onDismissClosePoll, onConfirmClosePoll = onConfirmClosePoll, onPromptDeletePoll = onPromptDeletePoll,
    onDismissDeletePoll = onDismissDeletePoll, onConfirmDeletePoll = onConfirmDeletePoll,
    onDismissPollsError = onDismissPollsError, onClosePollDetail = onClosePollDetail
  )
  if (isOverlayActive) return

  if (selectedTab == ClassTab.More && moreSubView != MoreSubView.None) {
    BackHandler { moreSubView = MoreSubView.None }
  } else {
    BackHandler(onBack = onBack)
  }

  val attendanceSnackbarHostState = remember { SnackbarHostState() }
  var attendanceSnackbarIsError by remember { mutableStateOf(true) }
  AttendanceSnackbarEffect(
    selectedTab = selectedTab, attendanceUiState = attendanceUiState, hostState = attendanceSnackbarHostState,
    onSetIsError = { attendanceSnackbarIsError = it },
    onDismissError = attendanceCallbacks.onDismissAttendanceError,
    onDismissSuccess = attendanceCallbacks.onDismissAttendanceSuccess
  )

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
      ClassTopAppBar(
        classItem = classItem, isStaff = isStaff, unreadNotificationCount = unreadNotificationCount,
        onOpenNotifications = onOpenNotifications,
        onBack = { if (selectedTab == ClassTab.More && moreSubView != MoreSubView.None) moreSubView = MoreSubView.None else onBack() },
        onEditClassNameClick = { showEditClassNameDialog = true },
        onOpenClassSwitcher = if (isStaff && !isPrefect) { { showClassSwitcherSheet = true } } else null,
        onNavigateToProfile = onNavigateToProfile
      )
      ClassRepPullToRefresh(isRefreshing = isTabRefreshing, onRefresh = onTabRefresh, modifier = Modifier.weight(1f)) {
        Column(
          modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = ScreenPadding, end = ScreenPadding, top = 20.dp, bottom = ScreenPadding),
          verticalArrangement = Arrangement.spacedBy(ItemSpacing)
        ) {
          ClassCodeHeaderChip(classItem.name, classItem.joinCode, isLecturer, onRegenerateCode = onRegenerateJoinCode)
          ClassPrefectBanner(prefects = prefects)
          val (activeTabErr, onDismissTabErr) = activeClassTabError(
            selectedTab, moreSubView, errorMessage, groupsUiState.errorMessage,
            timetableUiState.errorMessage, materialsUiState.errorMessage, pollsUiState.errorMessage,
            onDismissError, attendanceCallbacks.onDismissAttendanceError, onDismissGroupsError,
            onDismissTimetableError, onDismissMaterialsError, onDismissPollsError
          )
          if (!activeTabErr.isNullOrBlank()) FriendlyErrorSnackbar(errorMessage = activeTabErr, onDismiss = onDismissTabErr)

          AnimatedContent(
            targetState = selectedTab to moreSubView,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
            label = "class_tab_content_transition"
          ) { (curTab, curMore) ->
            ClassTabContent(
              selectedTab = curTab, moreSubView = curMore, isStaff = isStaff, isLecturer = isLecturer, session = session, notices = notices,
              readNoticeIds = readNoticeIds, canPostNotice = canPostNotice, isLoading = isLoading,
              attendanceUiState = attendanceUiState, trackerUiState = trackerUiState, groupsUiState = groupsUiState,
              timetableUiState = timetableUiState, materialsUiState = materialsUiState, pollsUiState = pollsUiState,
              onRefreshNotices = onRefreshNotices, onNoticeClick = onNoticeClick, onNavigateToPostNotice = onNavigateToPostNotice,
              onEditNoticeClick = onEditNoticeClick, onDeleteNoticeClick = onDeleteNoticeClick,
              onStartAttendanceClick = { showStartAttendanceDialog = true }, onRefreshAttendance = attendanceCallbacks.onRefreshAttendance,
              onOpenViewAttendance = attendanceCallbacks.onOpenViewAttendance, onOpenSignAttendance = attendanceCallbacks.onOpenSignAttendance,
              onOpenAttendanceSummary = attendanceCallbacks.onOpenAttendanceSummary, onOpenMyAttendance = attendanceCallbacks.onOpenMyAttendance,
              onOpenAttendanceTracker = attendanceCallbacks.onOpenAttendanceTracker,
              onOpenRegister = attendanceCallbacks.onOpenRegister, onCloseSession = attendanceCallbacks.onCloseSession,
              onSignOutSession = attendanceCallbacks.onSignOutSession, onShowActiveCode = attendanceCallbacks.onShowActiveCode,
              onDeleteSession = attendanceCallbacks.onPromptVoidSession, onDeleteRegisterPrompt = attendanceCallbacks.onPromptDeleteRegister,
              onRefreshGroups = onRefreshGroups, onCreateGroupClick = { showCreateGroupDialog = true },
              onOpenChat = onOpenChat, onJoinGroup = onJoinGroup, onRefreshTimetable = onRefreshTimetable,
              onSelectTimetableDay = onSelectTimetableDay, onAddScheduleClick = onAddScheduleClick,
              onEditScheduleClick = onEditScheduleClick, onDeleteScheduleClick = onDeleteScheduleClick,
              onStatusScheduleClick = onStatusScheduleClick, onRefreshMaterials = onRefreshMaterials,
              onMaterialsSearchChange = onMaterialsSearchChange, onMaterialsSelectUnitFilter = onMaterialsSelectUnitFilter,
              onRegisterUnitClick = onRegisterUnitClick, onAddMaterialClick = onAddMaterialClick,
              onOpenFileMaterial = onOpenFileMaterial, onDownloadMaterial = onDownloadMaterial,
              onRemoveDownloadMaterial = onRemoveDownloadMaterial, onEditMaterialClick = onEditMaterialClick,
              onDeleteMaterialClick = onDeleteMaterialClick, onRefreshPolls = onRefreshPolls,
              onCreatePollClick = onCreatePollClick, onPollClick = onPollClick,
              onOpenMaterials = { moreSubView = MoreSubView.Materials }, onOpenPolls = { moreSubView = MoreSubView.Polls },
              onOpenAlarmSettings = { moreSubView = MoreSubView.AlarmSettings },
              onOpenManageMembers = onNavigateToManageMembers, onBackFromMoreSubView = { moreSubView = MoreSubView.None }
            )
          }
          Spacer(modifier = Modifier.height(64.dp))
        }
      }
      ClassBottomNavigationBar(selectedTab = selectedTab, onSelectTab = { tab -> selectedTab = tab; if (tab != ClassTab.More) moreSubView = MoreSubView.None })
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
      showStartAttendanceDialog = showStartAttendanceDialog, classUnits = attendanceUiState.classUnits,
      isUnitsLoading = attendanceUiState.isUnitsLoading, onDismissStartAttendance = { showStartAttendanceDialog = false },
      onStartAttendance = attendanceCallbacks.onStartAttendance, isAttendanceActionLoading = attendanceUiState.isActionLoading,
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
      onDismissScheduleStatus = onDismissScheduleStatus, onSaveScheduleStatus = onSaveScheduleStatus,
      materialsUiState = materialsUiState, onDismissAddEditMaterial = onDismissAddEditMaterial,
      onSaveAddLinkMaterial = onSaveAddMaterial, onSaveUploadMaterial = onSaveUploadMaterial, onSaveEditMaterial = onSaveEditMaterial,
      onDismissRegisterUnit = onDismissRegisterUnit, onSubmitRegisterUnit = onSubmitRegisterUnit,
      onDismissPromptDeleteMaterial = onDismissPromptDeleteMaterial, onConfirmDeleteMaterial = onConfirmDeleteMaterial,
      showCreatePollDialog = pollsUiState.showCreateDialog, isPollActionLoading = pollsUiState.isActionLoading,
      onDismissCreatePollDialog = onDismissCreatePoll, onCreatePoll = onCreatePollSubmit
    )
    if (showClassSwitcherSheet) ClassSwitcherBottomSheet(
      classes = allClasses, currentClassId = classItem.id, isAdmin = isAdmin, isLecturer = isLecturer,
      onSelectClass = onSwitchClass, onJoinWithCode = onJoinWithCode, onCreateClass = onNavigateToCreateClass,
      onFindMoreClasses = onFindMoreClasses, onDismiss = { showClassSwitcherSheet = false }
    )
    ClassRepSnackbarHost(attendanceSnackbarHostState, attendanceSnackbarIsError, Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp))
  }
}
