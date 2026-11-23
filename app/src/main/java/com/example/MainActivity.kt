package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.alarms.AlarmNavigationHelper
import com.example.alarms.AlarmNavigationTarget
import com.example.alarms.AlarmNotificationHelper
import com.example.alarms.AlarmPreferences
import com.example.data.*
import com.example.notifications.AppNotificationDispatcher
import com.example.notifications.NotificationDeepLink
import com.example.ui.*
import com.example.ui.components.*
import com.example.ui.screens.AlarmOnboardingDialog
import com.example.ui.screens.MoreSubView
import com.example.ui.theme.ClassRepTheme
import com.example.util.RoleHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val authViewModel: AuthViewModel by viewModels()
  private val classesViewModel: ClassesViewModel by viewModels()
  private val noticesViewModel: NoticesViewModel by viewModels()
  private val attendanceViewModel: AttendanceViewModel by viewModels()
  private val trackerViewModel: AttendanceTrackerViewModel by viewModels()
  private val notificationViewModel: NotificationViewModel by viewModels()
  private val membersViewModel: MembersViewModel by viewModels()
  private val groupsViewModel: GroupsViewModel by viewModels()
  private val timetableViewModel: TimetableViewModel by viewModels()
  private val materialsViewModel: MaterialsViewModel by viewModels()
  private val universityViewModel: UniversityViewModel by viewModels()
  private val pollsViewModel: PollsViewModel by viewModels()
  private val challengesViewModel: ChallengesViewModel by viewModels()
  private val adminViewModel: AdminViewModel by viewModels()
  private val assistantViewModel: com.example.assistant.AssistantViewModel by viewModels()

  private var pendingAlarmTarget by mutableStateOf<AlarmNavigationTarget?>(null)
  private var pendingNotifTarget by mutableStateOf<NotificationDeepLink?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    com.example.assistant.configureAppCheck(this, intent)
    enableEdgeToEdge()
    OfflineCache.appContext = applicationContext
    NetworkMonitor.initialize(applicationContext)
    AppBackgroundManager.initialize(applicationContext)
    AlarmNotificationHelper.ensureChannel(applicationContext)
    AppNotificationDispatcher.ensureChannels(applicationContext)
    AlarmPreferences.refreshState(applicationContext)
    AlarmNavigationHelper.parseIntent(intent)?.let { pendingAlarmTarget = it }
    AppNotificationDispatcher.parseIntentDeepLink(intent, applicationContext)?.let { pendingNotifTarget = it }
    setContent {
      ClassRepTheme {
        val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
        val isAuthBg = isAuthBackgroundScreen(authUiState.currentScreen)
        StarfieldBackground(useAuthBackground = isAuthBg) {
          Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            ClassRepApp(
              authViewModel, classesViewModel, noticesViewModel, attendanceViewModel,
              trackerViewModel, notificationViewModel, membersViewModel, groupsViewModel,
              timetableViewModel, materialsViewModel, universityViewModel, pollsViewModel, challengesViewModel, adminViewModel,
              assistantViewModel = assistantViewModel,
              pendingAlarmTarget = pendingAlarmTarget, onAlarmTargetHandled = { pendingAlarmTarget = null },
              pendingNotifTarget = pendingNotifTarget, onNotifTargetHandled = { pendingNotifTarget = null },
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
    AlarmNavigationHelper.parseIntent(intent)?.let { pendingAlarmTarget = it }
    AppNotificationDispatcher.parseIntentDeepLink(intent, applicationContext)?.let { pendingNotifTarget = it }
  }

  override fun onResume() {
    super.onResume()
    authViewModel.refreshUserRole()
    AppBackgroundManager.refreshIfNeeded(authViewModel.uiState.value.session?.accessToken)
  }
}

fun isAuthBackgroundScreen(screen: Screen): Boolean {
  return when (screen) {
    Screen.Splash,
    Screen.Welcome,
    Screen.Landing,
    Screen.Register,
    Screen.CheckEmail,
    Screen.RegisterResult,
    Screen.Login -> true
    else -> false
  }
}

@Composable
fun ClassRepApp(
  viewModel: AuthViewModel,
  classesViewModel: ClassesViewModel,
  noticesViewModel: NoticesViewModel,
  attendanceViewModel: AttendanceViewModel,
  trackerViewModel: AttendanceTrackerViewModel,
  notificationViewModel: NotificationViewModel,
  membersViewModel: MembersViewModel,
  groupsViewModel: GroupsViewModel,
  timetableViewModel: TimetableViewModel,
  materialsViewModel: MaterialsViewModel,
  universityViewModel: UniversityViewModel,
  pollsViewModel: PollsViewModel,
  challengesViewModel: ChallengesViewModel,
  adminViewModel: AdminViewModel,
  assistantViewModel: com.example.assistant.AssistantViewModel? = null,
  pendingAlarmTarget: AlarmNavigationTarget? = null,
  onAlarmTargetHandled: () -> Unit = {},
  pendingNotifTarget: NotificationDeepLink? = null,
  onNotifTargetHandled: () -> Unit = {},
  onSaveLastClass: (String) -> Unit,
  onGetLastClass: suspend () -> String?
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val classesUiState by classesViewModel.uiState.collectAsStateWithLifecycle()
  val noticesUiState by noticesViewModel.uiState.collectAsStateWithLifecycle()
  val attendanceUiState by attendanceViewModel.uiState.collectAsStateWithLifecycle()
  val trackerUiState by trackerViewModel.uiState.collectAsStateWithLifecycle()
  val notificationUiState by notificationViewModel.uiState.collectAsStateWithLifecycle()
  val membersUiState by membersViewModel.uiState.collectAsStateWithLifecycle()
  val groupsUiState by groupsViewModel.uiState.collectAsStateWithLifecycle()
  val timetableUiState by timetableViewModel.uiState.collectAsStateWithLifecycle()
  val materialsUiState by materialsViewModel.uiState.collectAsStateWithLifecycle()
  val universityUiState by universityViewModel.uiState.collectAsStateWithLifecycle()
  val pollsUiState by pollsViewModel.uiState.collectAsStateWithLifecycle()
  val challengesUiState by challengesViewModel.uiState.collectAsStateWithLifecycle()
  val adminUiState by adminViewModel.uiState.collectAsStateWithLifecycle()
  val isOnline by NetworkMonitor.isOnline.collectAsStateWithLifecycle()
  val lastUpdatedText by OfflineCache.lastUpdatedFlow.collectAsStateWithLifecycle()
  val alarmState by AlarmPreferences.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var openTimetableSignal by remember { mutableIntStateOf(0) }
  var openTabSignal by remember { mutableStateOf<Triple<Int, ClassTab, MoreSubView>?>(null) }
  var notifReturnScreen by remember { mutableStateOf<Screen>(Screen.Home) }
  var assistantReturnScreen by remember { mutableStateOf<Screen>(Screen.Home) }
  var resolvedDestinationKey by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(uiState.session?.userId) {
    notificationViewModel.startSessionSync(uiState.session)
  }
  LaunchedEffect(pendingNotifTarget, uiState.session?.userId) {
    val target = pendingNotifTarget ?: return@LaunchedEffect
    val s = uiState.session ?: return@LaunchedEffect
    notificationViewModel.triggerDeepLink(target, s)
    onNotifTargetHandled()
  }

  val openClassDirectly: (ClassItem) -> Unit = { classItem ->
    onSaveLastClass(classItem.id)
    val session = uiState.session
    val canManage = RoleHelper.canManageClassContent(session?.role)
    val canSign = RoleHelper.canSignAttendance(session?.role)
    noticesViewModel.selectClassAndLoad(classItem, session)
    attendanceViewModel.initForClass(classItem.id, canManage, canSign, session)
    if (!RoleHelper.isStaffRole(session?.role)) trackerViewModel.loadMyStatus(classItem.id, session)
    groupsViewModel.loadGroups(classItem.id, session)
    timetableViewModel.loadSchedules(classItem.id, session)
    materialsViewModel.loadMaterials(classItem.id, session)
    pollsViewModel.loadPolls(classItem.id, session)
    challengesViewModel.loadClassChallengesData(classItem.id, session)
    if (universityUiState.profile == null) universityViewModel.loadProfile(session)
    viewModel.navigateTo(Screen.ClassDetail)
  }

  val resolveDestination: (UserSession) -> Unit = { session ->
    resolvedDestinationKey = "${session.userId}:${session.role}"
    val clearMsg = uiState.currentScreen != Screen.Home
    resolveStudentOrStaffDestination(session, clearMsg, classesViewModel, viewModel, openClassDirectly) { loaded ->
      coroutineScope.launch {
        val lastId = onGetLastClass()
        val target = if (!lastId.isNullOrBlank()) loaded.find { it.id == lastId } else null
        if (target != null) openClassDirectly(target) else viewModel.navigateTo(Screen.Home, clearMsg)
      }
    }
  }

  LaunchedEffect(isOnline, uiState.session?.accessToken) {
    if (isOnline) {
      AppBackgroundManager.refreshIfNeeded(
        accessToken = uiState.session?.accessToken,
        force = true
      )
    }
  }

  LaunchedEffect(isOnline) {
    val s = uiState.session ?: return@LaunchedEffect
    if (!isOnline) return@LaunchedEffect
    OfflineCache.clearLastUpdated()
    when (uiState.currentScreen) {
      Screen.Home -> {
        classesViewModel.loadClasses(s)
        noticesViewModel.loadLatestAnnouncements(s)
        if (RoleHelper.isLecturer(s.role) || RoleHelper.isAdmin(s.role)) {
          challengesViewModel.loadPapersToMark(s)
        }
      }
      Screen.ClassDetail -> noticesUiState.selectedClass?.let { sel ->
        noticesViewModel.selectClassAndLoad(sel, s)
        attendanceViewModel.loadSessions(sel.id, RoleHelper.canManageClassContent(s.role, noticesUiState.isRep), RoleHelper.canSignAttendance(s.role), s)
        timetableViewModel.loadSchedules(sel.id, s); materialsViewModel.loadMaterials(sel.id, s)
        pollsViewModel.loadPolls(sel.id, s); groupsViewModel.loadGroups(sel.id, s)
        challengesViewModel.loadClassChallengesData(sel.id, s)
        if (universityUiState.profile == null) universityViewModel.loadProfile(s)
      }
      Screen.Profile -> { universityViewModel.loadProfile(s); if (!RoleHelper.isStaffRole(s.role)) universityViewModel.loadSchools(s.accessToken) }
      Screen.ManageMembers -> noticesUiState.selectedClass?.let { membersViewModel.loadMembers(it.id, s) }
      Screen.Waiting -> classesViewModel.loadClasses(s)
      else -> {}
    }
  }

  LaunchedEffect(uiState.session?.userId, uiState.currentScreen == Screen.Home) {
    val s = uiState.session
    if (s == null) {
      resolvedDestinationKey = null
    } else if (noticesUiState.selectedClass == null && uiState.currentScreen == Screen.Home) {
      val key = "${s.userId}:${s.role}"
      if (resolvedDestinationKey != key) {
        if (uiState.pendingOpenClassId == null && uiState.infoMessage == null) resolveDestination(s)
        else { resolvedDestinationKey = key; classesViewModel.loadClasses(s) }
      }
      noticesViewModel.loadLatestAnnouncements(s)
    }
  }
  LaunchedEffect(uiState.pendingOpenClassId, uiState.session?.userId) {
    val targetId = uiState.pendingOpenClassId ?: return@LaunchedEffect
    val s = uiState.session ?: return@LaunchedEffect
    resolvedDestinationKey = "${s.userId}:${s.role}"
    viewModel.clearPendingOpenClass()
    classesViewModel.loadClasses(s) { list ->
      val target = list.find { it.id == targetId } ?: list.firstOrNull()
      if (target != null) openClassDirectly(target) else viewModel.navigateTo(Screen.Home, clearMessages = false)
    }
  }
  LaunchedEffect(noticesUiState.selectedClass?.id, noticesUiState.isStaff, uiState.session?.role) {
    val sel = noticesUiState.selectedClass; val s = uiState.session
    if (sel != null && s != null) {
      attendanceViewModel.setRolePermissions(RoleHelper.canManageClassContent(s.role, noticesUiState.isRep), RoleHelper.canSignAttendance(s.role), s)
    }
  }
  LaunchedEffect(pendingAlarmTarget, uiState.session?.userId, classesUiState.classes, noticesUiState.selectedClass?.id) {
    val target = pendingAlarmTarget ?: return@LaunchedEffect
    val s = uiState.session ?: return@LaunchedEffect
    AlarmNavigationHelper.dismissTriggeredAlarm(context, target.notificationId)
    val cls = classesUiState.classes.find { it.id == target.classId }
      ?: noticesUiState.selectedClass?.takeIf { target.classId.isBlank() || it.id == target.classId }
      ?: classesUiState.classes.firstOrNull()
    if (cls != null) {
      if (noticesUiState.selectedClass?.id != cls.id || uiState.currentScreen != Screen.ClassDetail) openClassDirectly(cls)
      timetableViewModel.setSelectedDay(target.dayOfWeek); openTimetableSignal++; onAlarmTargetHandled()
    } else if (!classesUiState.isLoading) classesViewModel.loadClasses(s)
  }
  LaunchedEffect(notificationUiState.pendingDeepLink, uiState.session?.userId, classesUiState.classes) {
    val link = notificationUiState.pendingDeepLink ?: return@LaunchedEffect
    val s = uiState.session ?: return@LaunchedEffect
    val handled = MainActivityNotificationRouter.routeDeepLink(
      link = link, session = s, classes = classesUiState.classes, selectedClass = noticesUiState.selectedClass,
      openClassDirectly = openClassDirectly,
      onOpenTab = { tab, sub ->
        if (sub == MoreSubView.Challenges || sub == MoreSubView.Leaderboard) {
          val cid = noticesUiState.selectedClass?.id?.ifBlank { link.classId } ?: link.classId
          if (cid.isNotBlank()) challengesViewModel.loadClassChallengesData(cid, s)
        }
        openTabSignal = Triple((openTabSignal?.first ?: 0) + 1, tab, sub)
        viewModel.navigateTo(Screen.ClassDetail)
      },
      onOpenMyAttendance = { attendanceViewModel.openMyAttendance() },
      onOpenAttendanceTracker = { cid -> trackerViewModel.loadClassRisk(cid, s); attendanceViewModel.openAttendanceTracker() },
      onOpenSignAttendance = { sid -> attendanceViewModel.openSignAttendance(sid) },
      onOpenManageMembers = { cls -> noticesViewModel.selectClassAndLoad(cls, s); membersViewModel.loadMembers(cls.id, s); viewModel.navigateTo(Screen.ManageMembers) },
      onOpenHome = { viewModel.refreshUserRole(); viewModel.navigateTo(Screen.Home) },
      onLoadClasses = { if (!classesUiState.isLoading) classesViewModel.loadClasses(it) },
      onOpenQuizDetail = { kind, quizId, _ ->
        if (kind == "paper_marked") {
          challengesViewModel.openMarkedPaperForStudent(
            context = context,
            quiz = ChallengeItem(quizId = quizId, title = "Marked Paper"),
            session = s
          )
        }
      }
    )
    if (handled) notificationViewModel.consumeDeepLink()
  }

  val navigateToProfileScreen: () -> Unit = {
    universityViewModel.loadProfile(uiState.session)
    if (!RoleHelper.isStaffRole(uiState.session?.role)) universityViewModel.loadSchools(uiState.session?.accessToken)
    viewModel.navigateTo(Screen.Profile)
  }
  val onOpenNotifications: () -> Unit = {
    if (uiState.currentScreen != Screen.Notifications) notifReturnScreen = uiState.currentScreen
    notificationViewModel.refresh(uiState.session)
    viewModel.navigateTo(Screen.Notifications)
  }
  val onOpenAssistant: () -> Unit = {
    if (uiState.currentScreen != Screen.Assistant) assistantReturnScreen = uiState.currentScreen
    viewModel.navigateTo(Screen.Assistant)
  }

  val assistantExecutor = remember(uiState.session?.userId) {
    com.example.assistant.ActionExecutor(
      onNoticesChanged = { cid ->
        val s = uiState.session
        if (noticesUiState.selectedClass?.id == cid) noticesViewModel.refreshNotices(s)
        noticesViewModel.loadLatestAnnouncements(s)
      },
      onPollsChanged = { cid ->
        val s = uiState.session
        if (pollsUiState.classId == cid) pollsViewModel.refreshPolls(s)
      },
      onAttendanceStarted = { cid, sessionId, code, _, remaining ->
        val s = uiState.session
        val target = classesUiState.classes.find { it.id == cid }
          ?: noticesUiState.selectedClass?.takeIf { it.id == cid }
          ?: ClassItem(id = cid, name = "Class", code = "")
        if (noticesUiState.selectedClass?.id != cid) openClassDirectly(target)
        attendanceViewModel.applyExternalStartedSession(sessionId, code, remaining, s)
      },
      onTimetableChanged = { cid ->
        val s = uiState.session
        if (timetableUiState.classId == cid) timetableViewModel.refreshSchedules(s)
      },
      onOpenChallengeDraft = { prefill ->
        val target = classesUiState.classes.find { it.id == prefill.classId }
          ?: noticesUiState.selectedClass?.takeIf { it.id == prefill.classId }
          ?: ClassItem(id = prefill.classId, name = "Class", code = "")
        if (noticesUiState.selectedClass?.id != target.id) openClassDirectly(target)
        challengesViewModel.openCreateDialogWithDraft(prefill)
        openTabSignal = Triple((openTabSignal?.first ?: 0) + 1, ClassTab.More, MoreSubView.Challenges)
        viewModel.navigateTo(Screen.ClassDetail)
      },
      onNavigate = { target ->
        when (target) {
          com.example.assistant.MainNavigationTarget.Home -> viewModel.navigateTo(Screen.Home)
          com.example.assistant.MainNavigationTarget.Profile -> navigateToProfileScreen()
          com.example.assistant.MainNavigationTarget.Notifications -> onOpenNotifications()
          is com.example.assistant.MainNavigationTarget.ClassDestination -> {
            val resolvedClass = target.classId?.let { id ->
              classesUiState.classes.find { it.id == id } ?: noticesUiState.selectedClass?.takeIf { it.id == id }
            } ?: noticesUiState.selectedClass ?: classesUiState.classes.firstOrNull()
            if (resolvedClass != null) {
              if (noticesUiState.selectedClass?.id != resolvedClass.id) openClassDirectly(resolvedClass)
              openTabSignal = Triple((openTabSignal?.first ?: 0) + 1, target.tab, target.moreSubView)
              viewModel.navigateTo(Screen.ClassDetail)
            } else {
              viewModel.navigateTo(Screen.Home)
            }
          }
        }
      }
    )
  }

  ClassRepAppBackHandlers(
    uiState = uiState, classesUiState = classesUiState, noticesUiState = noticesUiState,
    attendanceUiState = attendanceUiState, pollsUiState = pollsUiState, challengesUiState = challengesUiState,
    notifReturnScreen = notifReturnScreen, assistantReturnScreen = assistantReturnScreen,
    viewModel = viewModel, classesViewModel = classesViewModel,
    noticesViewModel = noticesViewModel, attendanceViewModel = attendanceViewModel,
    membersViewModel = membersViewModel, pollsViewModel = pollsViewModel, challengesViewModel = challengesViewModel
  )

  Column(modifier = Modifier.fillMaxSize()) {
    OfflineBanner(isOffline = !isOnline, lastUpdatedText = lastUpdatedText)
    Box(modifier = Modifier.weight(1f)) {
      ClassRepNavHost(
        currentScreen = uiState.currentScreen, authUiState = uiState, classesUiState = classesUiState,
        noticesUiState = noticesUiState, attendanceUiState = attendanceUiState, trackerUiState = trackerUiState,
        notificationUiState = notificationUiState, membersUiState = membersUiState, groupsUiState = groupsUiState,
        timetableUiState = timetableUiState, materialsUiState = materialsUiState, universityUiState = universityUiState,
        pollsUiState = pollsUiState, challengesUiState = challengesUiState, adminUiState = adminUiState, authViewModel = viewModel,
        classesViewModel = classesViewModel, noticesViewModel = noticesViewModel, attendanceViewModel = attendanceViewModel,
        trackerViewModel = trackerViewModel, notificationViewModel = notificationViewModel, membersViewModel = membersViewModel,
        groupsViewModel = groupsViewModel, timetableViewModel = timetableViewModel, materialsViewModel = materialsViewModel,
        universityViewModel = universityViewModel, pollsViewModel = pollsViewModel, challengesViewModel = challengesViewModel,
        adminViewModel = adminViewModel,
        openTimetableSignal = openTimetableSignal, openTabSignal = openTabSignal,
        onOpenNotifications = onOpenNotifications, onBackFromNotifications = { viewModel.navigateTo(notifReturnScreen) },
        isOnline = isOnline,
        assistantViewModel = assistantViewModel,
        assistantExecutor = assistantExecutor,
        assistantReturnScreen = assistantReturnScreen,
        onOpenAssistant = onOpenAssistant,
        onBackFromAssistant = { viewModel.navigateTo(assistantReturnScreen) },
        openClassDirectly = openClassDirectly,
        onOpenPaperToMark = { paperItem ->
          val targetClass = classesUiState.classes.find { it.id == paperItem.classId }
            ?: ClassItem(id = paperItem.classId, name = paperItem.className, code = "")
          openClassDirectly(targetClass)
          openTabSignal = Triple((openTabSignal?.first ?: 0) + 1, ClassTab.More, MoreSubView.Challenges)
          challengesViewModel.openExamSubmissions(
            ChallengeItem(
              quizId = paperItem.quizId,
              title = paperItem.title,
              isExam = true,
              answerFormat = ExamAnswerFormat.BOTH
            ),
            uiState.session
          )
        },
        resolveDestination = resolveDestination, navigateToProfileScreen = navigateToProfileScreen
      )
    }
    SupportFooter(uiState.currentScreen in listOf(Screen.Welcome, Screen.Login, Screen.Register, Screen.CheckEmail, Screen.RegisterResult))
  }
  if (!alarmState.onboardingCompleted) AlarmOnboardingDialog { AlarmPreferences.setOnboardingCompleted(context, true) }
  else NotificationPermissionRationaleDialog(isAuthenticated = uiState.session != null)
}
