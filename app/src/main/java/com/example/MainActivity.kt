package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.AttendanceSubScreen
import com.example.data.ClassItem
import com.example.data.UserSession
import com.example.ui.*
import com.example.ui.screens.*
import com.example.ui.theme.ClassRepTheme
import com.example.util.RoleHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val authViewModel: AuthViewModel by viewModels()
  private val classesViewModel: ClassesViewModel by viewModels()
  private val noticesViewModel: NoticesViewModel by viewModels()
  private val attendanceViewModel: AttendanceViewModel by viewModels()
  private val membersViewModel: MembersViewModel by viewModels()
  private val groupsViewModel: GroupsViewModel by viewModels()
  private val timetableViewModel: TimetableViewModel by viewModels()
  private val materialsViewModel: MaterialsViewModel by viewModels()
  private val universityViewModel: UniversityViewModel by viewModels()
  private val pollsViewModel: PollsViewModel by viewModels()
  private val adminViewModel: AdminViewModel by viewModels()

  private var pendingAlarmTarget by mutableStateOf<com.example.alarms.AlarmNavigationTarget?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    com.example.data.OfflineCache.appContext = applicationContext
    com.example.data.NetworkMonitor.initialize(applicationContext)
    com.example.alarms.AlarmNotificationHelper.ensureChannel(applicationContext)
    com.example.alarms.AlarmPreferences.refreshState(applicationContext)
    com.example.alarms.AlarmNavigationHelper.parseIntent(intent)?.let { pendingAlarmTarget = it }
    setContent {
      ClassRepTheme {
        val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
        val isFirstScreen = authUiState.currentScreen is Screen.Welcome || authUiState.currentScreen is Screen.Landing
        com.example.ui.components.NatureBackground(isFirstScreen = isFirstScreen) {
          androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            ClassRepApp(
              authViewModel, classesViewModel, noticesViewModel, attendanceViewModel,
              membersViewModel, groupsViewModel, timetableViewModel, materialsViewModel,
              universityViewModel, pollsViewModel, adminViewModel,
              pendingAlarmTarget = pendingAlarmTarget,
              onAlarmTargetHandled = { pendingAlarmTarget = null },
              onSaveLastClass = { id -> lifecycleScope.launch { authViewModel.sessionManager.saveLastClassId(id) } },
              onGetLastClass = { authViewModel.sessionManager.getLastClassId() }
            )
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    com.example.alarms.AlarmNavigationHelper.parseIntent(intent)?.let { pendingAlarmTarget = it }
  }

  override fun onResume() {
    super.onResume()
    authViewModel.refreshProfileStatus()
  }
}

@Composable
fun ClassRepApp(
  viewModel: AuthViewModel,
  classesViewModel: ClassesViewModel,
  noticesViewModel: NoticesViewModel,
  attendanceViewModel: AttendanceViewModel,
  membersViewModel: MembersViewModel,
  groupsViewModel: GroupsViewModel,
  timetableViewModel: TimetableViewModel,
  materialsViewModel: MaterialsViewModel,
  universityViewModel: UniversityViewModel,
  pollsViewModel: PollsViewModel,
  adminViewModel: AdminViewModel,
  pendingAlarmTarget: com.example.alarms.AlarmNavigationTarget? = null,
  onAlarmTargetHandled: () -> Unit = {},
  onSaveLastClass: (String) -> Unit,
  onGetLastClass: suspend () -> String?
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val classesUiState by classesViewModel.uiState.collectAsStateWithLifecycle()
  val noticesUiState by noticesViewModel.uiState.collectAsStateWithLifecycle()
  val attendanceUiState by attendanceViewModel.uiState.collectAsStateWithLifecycle()
  val membersUiState by membersViewModel.uiState.collectAsStateWithLifecycle()
  val groupsUiState by groupsViewModel.uiState.collectAsStateWithLifecycle()
  val timetableUiState by timetableViewModel.uiState.collectAsStateWithLifecycle()
  val materialsUiState by materialsViewModel.uiState.collectAsStateWithLifecycle()
  val universityUiState by universityViewModel.uiState.collectAsStateWithLifecycle()
  val pollsUiState by pollsViewModel.uiState.collectAsStateWithLifecycle()
  val adminUiState by adminViewModel.uiState.collectAsStateWithLifecycle()
  val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
  val lastUpdatedText by com.example.data.OfflineCache.lastUpdatedFlow.collectAsStateWithLifecycle()
  val alarmState by com.example.alarms.AlarmPreferences.state.collectAsStateWithLifecycle()
  val context = androidx.compose.ui.platform.LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var openTimetableSignal by remember { mutableIntStateOf(0) }

  LaunchedEffect(isOnline) {
    if (isOnline) {
      com.example.data.OfflineCache.clearLastUpdated()
      val session = uiState.session
      if (session != null) {
        when (uiState.currentScreen) {
          Screen.Home -> {
            classesViewModel.loadClasses(session)
            noticesViewModel.loadLatestAnnouncements(session)
          }
          Screen.ClassDetail -> {
            val sel = noticesUiState.selectedClass
            if (sel != null) {
              val canManage = RoleHelper.canManageClassContent(session.role, noticesUiState.isRep)
              val canSign = RoleHelper.canSignAttendance(session.role)
              noticesViewModel.selectClassAndLoad(sel, session)
              attendanceViewModel.loadSessions(sel.id, canManage, canSign, session)
              timetableViewModel.loadSchedules(sel.id, session)
              materialsViewModel.loadMaterials(sel.id, session)
              pollsViewModel.loadPolls(sel.id, session)
              groupsViewModel.loadGroups(sel.id, session)
            }
          }
          Screen.Profile -> {
            universityViewModel.loadProfile(session)
            if (!RoleHelper.usesStaffNumber(session.role)) universityViewModel.loadSchools(session.accessToken)
          }
          Screen.ManageMembers -> {
            val sel = noticesUiState.selectedClass
            if (sel != null) membersViewModel.loadMembers(sel.id, session)
          }
          Screen.Waiting -> classesViewModel.loadClasses(session)
          Screen.PendingApproval -> viewModel.refreshProfileStatus()
          else -> {}
        }
      }
    }
  }

  val openClassDirectly: (ClassItem) -> Unit = { classItem ->
    onSaveLastClass(classItem.id)
    val session = uiState.session
    val canManage = RoleHelper.canManageClassContent(session?.role)
    val canSign = RoleHelper.canSignAttendance(session?.role)
    noticesViewModel.selectClassAndLoad(classItem, session)
    attendanceViewModel.initForClass(classItem.id, canManage, canSign, session)
    groupsViewModel.loadGroups(classItem.id, session)
    timetableViewModel.loadSchedules(classItem.id, session)
    materialsViewModel.loadMaterials(classItem.id, session)
    pollsViewModel.loadPolls(classItem.id, session)
    viewModel.navigateTo(Screen.ClassDetail)
  }

  val resolveDestination: (UserSession) -> Unit = { session ->
    val isStaff = RoleHelper.isStaffRole(session.role)
    classesViewModel.loadClasses(session) { loadedClasses ->
      if (!isStaff) {
        if (loadedClasses.isNotEmpty()) {
          openClassDirectly(loadedClasses.first())
        } else {
          classesViewModel.joinMyClass(session, onResult = { classId ->
            if (!classId.isNullOrBlank()) {
              classesViewModel.loadClasses(session) { refreshed ->
                val target = refreshed.find { it.id == classId } ?: refreshed.firstOrNull()
                if (target != null) openClassDirectly(target) else viewModel.navigateTo(Screen.Waiting)
              }
            } else {
              viewModel.navigateTo(Screen.Waiting)
            }
          }, onError = { viewModel.navigateTo(Screen.Waiting) })
        }
      } else {
        coroutineScope.launch {
          val lastId = onGetLastClass()
          val target = if (!lastId.isNullOrBlank()) loadedClasses.find { it.id == lastId } else null
          if (target != null) openClassDirectly(target) else viewModel.navigateTo(Screen.Home)
        }
      }
    }
  }

  LaunchedEffect(uiState.session?.userId, uiState.currentScreen == Screen.Home) {
    val s = uiState.session
    if (s != null && !RoleHelper.isPendingApproval(s.roleStatus) && !RoleHelper.isRejectedStatus(s.roleStatus) &&
      noticesUiState.selectedClass == null && uiState.currentScreen == Screen.Home) {
      resolveDestination(s)
      noticesViewModel.loadLatestAnnouncements(s)
    }
  }

  LaunchedEffect(noticesUiState.selectedClass?.id, noticesUiState.isStaff, uiState.session?.role) {
    val sel = noticesUiState.selectedClass
    val s = uiState.session
    if (sel != null && s != null) {
      val canManage = RoleHelper.canManageClassContent(s.role, noticesUiState.isRep)
      val canSign = RoleHelper.canSignAttendance(s.role)
      attendanceViewModel.setRolePermissions(canManage, canSign, s)
    }
  }

  LaunchedEffect(pendingAlarmTarget, uiState.session?.userId, classesUiState.classes, noticesUiState.selectedClass?.id) {
    val target = pendingAlarmTarget ?: return@LaunchedEffect
    val s = uiState.session ?: return@LaunchedEffect
    if (RoleHelper.isPendingApproval(s.roleStatus) || RoleHelper.isRejectedStatus(s.roleStatus)) return@LaunchedEffect
    com.example.alarms.AlarmNavigationHelper.dismissTriggeredAlarm(context, target.notificationId)
    val cls = classesUiState.classes.find { it.id == target.classId }
      ?: noticesUiState.selectedClass?.takeIf { target.classId.isBlank() || it.id == target.classId }
      ?: classesUiState.classes.firstOrNull()
    if (cls != null) {
      if (noticesUiState.selectedClass?.id != cls.id || uiState.currentScreen != Screen.ClassDetail) openClassDirectly(cls)
      timetableViewModel.setSelectedDay(target.dayOfWeek)
      openTimetableSignal++
      onAlarmTargetHandled()
    } else if (!classesUiState.isLoading) {
      classesViewModel.loadClasses(s)
    }
  }

  val navigateToProfileScreen: () -> Unit = {
    universityViewModel.loadProfile(uiState.session)
    if (!RoleHelper.usesStaffNumber(uiState.session?.role)) universityViewModel.loadSchools(uiState.session?.accessToken)
    viewModel.navigateTo(Screen.Profile)
  }

  when (uiState.currentScreen) {
    Screen.Register, Screen.Login -> BackHandler { viewModel.navigateTo(Screen.Welcome) }
    Screen.Welcome, Screen.Landing, Screen.Waiting, Screen.PendingApproval -> BackHandler { }
    Screen.Profile -> BackHandler {
      if (noticesUiState.selectedClass != null) viewModel.navigateTo(Screen.ClassDetail)
      else if (classesUiState.classes.isEmpty() && !RoleHelper.isStaffRole(uiState.session?.role)) viewModel.navigateTo(Screen.Waiting)
      else viewModel.navigateTo(Screen.Home)
    }
    Screen.ManageUsers -> BackHandler { viewModel.navigateTo(Screen.Home) }
    Screen.JoinClass, Screen.CreateClass, Screen.FindClasses -> BackHandler { classesViewModel.clearError(); viewModel.navigateTo(Screen.Home) }
    Screen.ClassDetail -> BackHandler {
      if (pollsUiState.selectedPoll != null) pollsViewModel.closePollDetail()
      else if (attendanceUiState.currentSubScreen != AttendanceSubScreen.None) attendanceViewModel.closeSubScreen()
      else if (RoleHelper.isStaffRole(uiState.session?.role)) {
        classesViewModel.clearError(); noticesViewModel.clearSelectedClass(); pollsViewModel.clearError(); viewModel.navigateTo(Screen.Home)
      }
    }
    Screen.PostNotice, Screen.ManageMembers -> BackHandler { noticesViewModel.clearError(); membersViewModel.clearError(); viewModel.navigateTo(Screen.ClassDetail) }
    Screen.GroupChat -> BackHandler { groupsViewModel.closeChat(); viewModel.navigateTo(Screen.ClassDetail) }
    Screen.GroupInfo -> BackHandler { viewModel.navigateTo(Screen.GroupChat) }
    else -> {}
  }

  androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
    com.example.ui.components.OfflineBanner(isOffline = !isOnline, lastUpdatedText = lastUpdatedText)
    androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
      ClassRepNavHost(
        currentScreen = uiState.currentScreen, authUiState = uiState, classesUiState = classesUiState,
        noticesUiState = noticesUiState, attendanceUiState = attendanceUiState, membersUiState = membersUiState,
        groupsUiState = groupsUiState, timetableUiState = timetableUiState, materialsUiState = materialsUiState,
        universityUiState = universityUiState, pollsUiState = pollsUiState, adminUiState = adminUiState,
        authViewModel = viewModel, classesViewModel = classesViewModel, noticesViewModel = noticesViewModel,
        attendanceViewModel = attendanceViewModel, membersViewModel = membersViewModel, groupsViewModel = groupsViewModel,
        timetableViewModel = timetableViewModel, materialsViewModel = materialsViewModel, universityViewModel = universityViewModel,
        pollsViewModel = pollsViewModel, adminViewModel = adminViewModel, openTimetableSignal = openTimetableSignal,
        openClassDirectly = openClassDirectly, resolveDestination = resolveDestination, navigateToProfileScreen = navigateToProfileScreen
      )
    }
    com.example.ui.components.SupportFooter(uiState.currentScreen in listOf(Screen.Welcome, Screen.Login, Screen.Register))
  }
  if (!alarmState.onboardingCompleted) {
    AlarmOnboardingDialog { com.example.alarms.AlarmPreferences.setOnboardingCompleted(context, true) }
  }
}
