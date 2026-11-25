package com.example.admin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.admin.assistant.AdminActionExecutor
import com.example.admin.assistant.AdminAssistantViewModel
import com.example.admin.assistant.AdminChatScreen
import com.example.admin.assistant.AdminNavigationTarget
import com.example.admin.assistant.AdminNetworkMonitor
import com.example.admin.notifications.AdminNotificationDispatcher
import com.example.admin.notifications.AdminNotificationPrefs
import com.example.admin.ui.AdminApprovalsViewModel
import com.example.admin.ui.AdminAuthViewModel
import com.example.admin.ui.AdminClassMonitorViewModel
import com.example.admin.ui.AdminClassesSearchViewModel
import com.example.admin.ui.AdminMainViewModel
import com.example.admin.ui.AdminManageViewModel
import com.example.admin.ui.AdminMonitorExtraViewModel
import com.example.admin.ui.AdminNotificationViewModel
import com.example.admin.ui.AdminRiskViewModel
import com.example.admin.ui.AdminTab
import com.example.admin.ui.ApprovalsSubTab
import com.example.admin.ui.MonitorTab
import com.example.admin.ui.components.AdminBackdrop
import com.example.admin.ui.components.AdminFeedbackBanner
import com.example.admin.ui.components.AdminPermissionRationaleDialog
import com.example.admin.ui.components.AdminShell
import com.example.admin.ui.components.routeAdminDeepLink
import com.example.admin.ui.screens.AdminApprovalsScreen
import com.example.admin.ui.screens.AdminClassMonitorScreen
import com.example.admin.ui.screens.AdminClassesSearchScreen
import com.example.admin.ui.screens.AdminDashboardScreen
import com.example.admin.ui.screens.AdminLoginScreen
import com.example.admin.ui.screens.AdminNotificationCenterScreen
import com.example.admin.ui.screens.AdminRiskScreen
import com.example.admin.ui.screens.AdminSettingsDialog
import com.example.admin.ui.screens.AdminUniversityScreen
import com.example.admin.ui.screens.AdminUsersScreen
import com.example.admin.ui.theme.ClassRepAdminTheme
import com.example.admin.ui.theme.GoldPrimary

class MainActivity : ComponentActivity() {
  private val authViewModel: AdminAuthViewModel by viewModels()
  private val mainViewModel: AdminMainViewModel by viewModels()
  private val manageViewModel: AdminManageViewModel by viewModels()
  private val notifViewModel: AdminNotificationViewModel by viewModels()
  private val classesViewModel: AdminClassesSearchViewModel by viewModels()
  private val monitorViewModel: AdminClassMonitorViewModel by viewModels()
  private val extraViewModel: AdminMonitorExtraViewModel by viewModels()
  private val approvalsViewModel: AdminApprovalsViewModel by viewModels()
  private val riskViewModel: AdminRiskViewModel by viewModels()
  private val assistantViewModel: AdminAssistantViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    AdminNotificationDispatcher.ensureChannels(this)
    handleNotificationIntent(intent)

    setContent {
      ClassRepAdminTheme {
        val authState by authViewModel.state.collectAsStateWithLifecycle()
        val mainState by mainViewModel.state.collectAsStateWithLifecycle()
        val manageState by manageViewModel.state.collectAsStateWithLifecycle()
        val notifState by notifViewModel.state.collectAsStateWithLifecycle()
        val classesState by classesViewModel.state.collectAsStateWithLifecycle()
        val monitorState by monitorViewModel.state.collectAsStateWithLifecycle()
        val extraState by extraViewModel.state.collectAsStateWithLifecycle()
        val approvalsState by approvalsViewModel.state.collectAsStateWithLifecycle()
        val riskState by riskViewModel.state.collectAsStateWithLifecycle()
        val assistantState by assistantViewModel.uiState.collectAsStateWithLifecycle()

        val networkMonitor = remember { AdminNetworkMonitor(applicationContext) }
        val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle()
        DisposableEffect(networkMonitor) {
          onDispose { networkMonitor.stop() }
        }

        var showSettings by remember { mutableStateOf(false) }
        var showNotifications by remember { mutableStateOf(false) }
        var showAssistant by remember { mutableStateOf(false) }
        var showPermDialog by remember { mutableStateOf(false) }

        val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
          AdminNotificationPrefs.setPermissionPromptDismissed(this@MainActivity, true)
          if (granted) notifViewModel.refresh(authState.session)
        }

        LaunchedEffect(authState.session) {
          val s = authState.session
          mainViewModel.setSession(s)
          manageViewModel.setSession(s)
          classesViewModel.setSession(s)
          monitorViewModel.setSession(s)
          extraViewModel.setSession(s)
          approvalsViewModel.setSession(s)
          riskViewModel.setSession(s)
          notifViewModel.startSessionSync(s)
          if (s == null) showAssistant = false
          if (s != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted && !AdminNotificationPrefs.wasPermissionPromptDismissed(this@MainActivity)) {
              showPermDialog = true
            }
          }
        }

        LaunchedEffect(notifState.pendingDeepLink, authState.session) {
          val link = notifState.pendingDeepLink ?: return@LaunchedEffect
          if (authState.session != null) {
            showNotifications = false
            showAssistant = false
            routeAdminDeepLink(link, mainViewModel, approvalsViewModel, monitorViewModel, extraViewModel, riskViewModel)
            notifViewModel.consumeDeepLink()
          }
        }

        if (authState.isInitializing) {
          AdminBackdrop { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = com.example.admin.ui.theme.ChampagneGold) } }
        } else if (authState.session == null) {
          AdminLoginScreen(
            isLoading = authState.isLoading, errorMessage = authState.errorMessage,
            onLogin = authViewModel::login, onDismissError = authViewModel::clearError
          )
        } else {
          val session = authState.session!!
          val unverifiedLecturersCount = if (approvalsState.lecturers.isNotEmpty()) {
            approvalsState.lecturers.count { !it.isVerified }
          } else {
            mainState.stats.unverifiedLecturers
          }
          val pendingCount = approvalsState.pendingUnits.size + unverifiedLecturersCount + approvalsState.joinRequests.size
          val selectAdminTab: (AdminTab) -> Unit = { tab ->
            showNotifications = false
            showAssistant = false
            mainViewModel.selectTab(tab)
            if (tab == AdminTab.APPROVALS) approvalsViewModel.refresh()
          }
          val openAssistant: () -> Unit = {
            networkMonitor.refresh()
            showNotifications = false
            showAssistant = true
          }
          val assistantExecutor = remember {
            AdminActionExecutor(
              onNoticePosted = { classId ->
                if (monitorViewModel.state.value.selectedClassId == classId) {
                  monitorViewModel.refreshCurrentClass()
                }
              },
              onNavigate = { target ->
                showAssistant = false
                when (target) {
                  is AdminNavigationTarget.Tab -> selectAdminTab(target.tab)
                  AdminNavigationTarget.Notifications -> showNotifications = true
                  is AdminNavigationTarget.ApprovalsSubTabTarget -> {
                    approvalsViewModel.selectSubTab(target.subTab)
                    selectAdminTab(AdminTab.APPROVALS)
                  }
                  is AdminNavigationTarget.ClassDetailTab -> {
                    val cid = target.classId?.takeIf { it.isNotBlank() } ?: monitorViewModel.state.value.selectedClassId
                    if (!cid.isNullOrBlank()) {
                      if (monitorViewModel.state.value.selectedClassId != cid) {
                        monitorViewModel.openClass(cid, target.monitorTab)
                        extraViewModel.loadForClass(cid)
                      } else {
                        monitorViewModel.selectTab(target.monitorTab)
                      }
                    }
                    mainViewModel.selectTab(AdminTab.CLASSES)
                  }
                }
              }
            )
          }
          val contextScreen = when (mainState.activeTab) {
            AdminTab.OVERVIEW -> "overview"
            AdminTab.CLASSES -> if (monitorState.selectedClassId != null) "class_detail" else "classes"
            AdminTab.APPROVALS -> "approvals"
            AdminTab.RISK -> "risk"
            AdminTab.USERS -> "users"
            AdminTab.UNIVERSITY -> "university"
          }
          BackHandler(enabled = showAssistant) { showAssistant = false }

          AdminShell(
            activeTab = mainState.activeTab,
            pendingApprovalsCount = pendingCount,
            onSelectTab = selectAdminTab
          ) { innerMod ->
            if (showAssistant) {
              Box(modifier = innerMod.fillMaxSize()) {
                AdminChatScreen(
                  uiState = assistantState,
                  session = session,
                  isOnline = isOnline,
                  activeClassId = monitorState.selectedClassId,
                  screenContextName = contextScreen,
                  onEnterAssistant = { assistantViewModel.onEnterAssistant(session) },
                  onAcceptTerms = { assistantViewModel.acceptTerms(session) },
                  onInputChanged = assistantViewModel::onInputChanged,
                  onNewChat = { assistantViewModel.newChat(session) },
                  onSendMessage = { msg, ctx -> assistantViewModel.sendMessage(msg, session, ctx, isOnline) },
                  onConfirmAction = { msgId, actId ->
                    assistantViewModel.confirmAction(msgId, actId, monitorState.selectedClassId, session, assistantExecutor)
                  },
                  onSaveActionEdit = { msgId, actId, args ->
                    assistantViewModel.updateActionArgs(msgId, actId, args, session.userId)
                  },
                  onCancelAction = { msgId, actId ->
                    assistantViewModel.cancelAction(msgId, actId, session.userId)
                  },
                  onDismissError = assistantViewModel::clearError,
                  onBack = { showAssistant = false }
                )
              }
            } else if (showNotifications) {
              AdminNotificationCenterScreen(
                state = notifState,
                onBack = { showNotifications = false },
                onRefresh = { notifViewModel.refresh(session) },
                onMarkAllRead = { notifViewModel.markAllAsRead(session) },
                onLoadMore = { notifViewModel.loadMore(session) },
                onNotificationClick = { item -> notifViewModel.onNotificationClick(item, session) },
                onDismissError = notifViewModel::clearError
              )
            } else {
              val err = mainState.errorMessage ?: manageState.errorMessage ?: approvalsState.errorMessage ?: riskState.errorMessage ?: classesState.errorMessage
              val ok = mainState.successMessage ?: manageState.successMessage ?: approvalsState.successMessage ?: riskState.successMessage
              Column(modifier = innerMod.fillMaxSize()) {
                err?.let { msg ->
                  Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    AdminFeedbackBanner(msg, true) {
                      mainViewModel.clearError(); manageViewModel.clearError(); approvalsViewModel.clearError(); riskViewModel.clearError(); classesViewModel.clearError()
                    }
                  }
                }
                ok?.let { msg ->
                  Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    AdminFeedbackBanner(msg, false) {
                      mainViewModel.clearSuccess(); manageViewModel.clearSuccess(); approvalsViewModel.clearSuccess(); riskViewModel.clearSuccess()
                    }
                  }
                }

                when (mainState.activeTab) {
                  AdminTab.OVERVIEW -> AdminDashboardScreen(
                    session = session, stats = mainState.stats,
                    unreadNotifications = notifState.unreadCount,
                    pendingUnitsCount = approvalsState.pendingUnits.size,
                    pendingApprovalsCount = pendingCount,
                    unverifiedLecturersCount = unverifiedLecturersCount,
                    isLoading = mainState.isStatsLoading,
                    isActionLoading = mainState.isActionLoading,
                    isOnline = isOnline,
                    onRefresh = { mainViewModel.refreshAll(); manageViewModel.refreshUniversityData(); approvalsViewModel.refresh(); notifViewModel.refresh(session) },
                    onJumpToUnverified = { approvalsViewModel.selectSubTab(ApprovalsSubTab.LECTURERS); selectAdminTab(AdminTab.APPROVALS) },
                    onNavigateTab = selectAdminTab,
                    onClearEmptySessions = mainViewModel::deleteEmptyExpiredSessionsGlobal,
                    onSweepExpiredAttempts = mainViewModel::runExpiredAttemptsGlobal,
                    onOpenAssistant = openAssistant,
                    onOpenNotifications = { showNotifications = true },
                    onOpenSettings = { showSettings = true }
                  )
                  AdminTab.CLASSES -> if (monitorState.selectedClassId != null) {
                    AdminClassMonitorScreen(
                      monitorState = monitorState, extraState = extraState, allUsers = mainState.users,
                      unreadNotifications = notifState.unreadCount, isOnline = isOnline,
                      monitorVm = monitorViewModel, extraVm = extraViewModel,
                      onOpenClassRisk = { cid, cname -> riskViewModel.openClassRiskDirectly(cid, cname); mainViewModel.selectTab(AdminTab.RISK) },
                      onOpenAssistant = openAssistant,
                      onOpenNotifications = { showNotifications = true },
                      onBack = { monitorViewModel.closeClass(); extraViewModel.stopAndClear() }
                    )
                  } else {
                    AdminClassesSearchScreen(
                      state = classesState, schools = manageState.schools, unreadNotifications = notifState.unreadCount,
                      isOnline = isOnline,
                      onSearchChanged = classesViewModel::onSearchChanged, onSchoolFilterChanged = classesViewModel::setSchoolFilter,
                      onYearFilterChanged = classesViewModel::setYearFilter, onToggleNoPrefect = classesViewModel::setOnlyWithoutPrefect,
                      onRefresh = { classesViewModel.search(reset = true) }, onLoadMore = { classesViewModel.search(reset = false) },
                      onOpenClass = { cls -> monitorViewModel.openClass(cls.id, MonitorTab.NOTICES); extraViewModel.loadForClass(cls.id) },
                      onOpenAssistant = openAssistant,
                      onOpenNotifications = { showNotifications = true }
                    )
                  }
                  AdminTab.APPROVALS -> AdminApprovalsScreen(
                    state = approvalsState, unreadNotifications = notifState.unreadCount, isOnline = isOnline,
                    onSelectSubTab = approvalsViewModel::selectSubTab, onSearchLecturers = approvalsViewModel::onLecturerSearchChanged,
                    onRefresh = approvalsViewModel::refresh,
                    onReviewUnit = { unit, okReview, reason -> approvalsViewModel.reviewUnit(unit, okReview, reason) },
                    onToggleVerifyLecturer = { u, v -> approvalsViewModel.toggleVerifyLecturer(u, v) { mainViewModel.refreshUsers(); mainViewModel.refreshStats() } },
                    onReviewJoinRequest = { req, okJoin -> approvalsViewModel.reviewJoinRequest(req, okJoin) { classesViewModel.search(reset = true); mainViewModel.refreshStats() } },
                    onOpenAssistant = openAssistant,
                    onOpenNotifications = { showNotifications = true }
                  )
                  AdminTab.RISK -> AdminRiskScreen(
                    state = riskState, unreadNotifications = notifState.unreadCount, isOnline = isOnline,
                    onRefresh = riskViewModel::refreshAll, onSetMinLevel = riskViewModel::setMinLevel,
                    onSearchChanged = riskViewModel::onSearchChanged, onSelectClassFilter = riskViewModel::selectClassFilter,
                    onLoadMoreClasses = { riskViewModel.loadOverview(reset = false) },
                    onLoadMoreStudents = { riskViewModel.loadStudents(reset = false) },
                    onOpenStudent = riskViewModel::openStudentDetail, onCloseStudent = riskViewModel::closeStudentDetail,
                    onSetExcuse = riskViewModel::setExcuse, onRunWarningsNow = riskViewModel::runWarningsNow,
                    onOpenAssistant = openAssistant,
                    onOpenNotifications = { showNotifications = true }
                  )
                  AdminTab.USERS -> AdminUsersScreen(
                    users = mainState.users, searchQuery = mainState.userSearch, activeFilter = mainState.userFilter,
                    unreadNotifications = notifState.unreadCount, isOnline = isOnline, isLoading = mainState.isUsersLoading, isActionLoading = mainState.isActionLoading,
                    onSearchChanged = mainViewModel::onUserSearchChanged, onFilterChanged = mainViewModel::onUserFilterChanged,
                    onRefresh = mainViewModel::refreshUsers,
                    onAssignRole = { u, r -> mainViewModel.assignRole(u, r) { approvalsViewModel.refresh() } },
                    onToggleVerify = { u, v -> mainViewModel.setLecturerVerified(u, v) { approvalsViewModel.refresh() } },
                    onOpenAssistant = openAssistant,
                    onOpenNotifications = { showNotifications = true }
                  )
                  AdminTab.UNIVERSITY -> AdminUniversityScreen(
                    section = manageState.universitySection, schools = manageState.schools, programmes = manageState.programmes,
                    academicYears = manageState.academicYears, unreadNotifications = notifState.unreadCount, isOnline = isOnline,
                    isLoading = manageState.isUniLoading, isActionLoading = manageState.isActionLoading,
                    onSelectSection = manageViewModel::selectUniversitySection, onRefresh = manageViewModel::refreshUniversityData,
                    onSaveSchool = manageViewModel::saveSchool, onDeleteSchool = manageViewModel::deleteSchool,
                    onSaveProgramme = manageViewModel::saveProgramme, onDeleteProgramme = manageViewModel::deleteProgramme,
                    onCreateYear = manageViewModel::createAcademicYear, onSetCurrentYear = manageViewModel::setCurrentAcademicYear,
                    onOpenAssistant = openAssistant,
                    onOpenNotifications = { showNotifications = true }
                  )
                }
              }
            }
          }
          if (showSettings) {
            AdminSettingsDialog(
              session = session, rpcSqlInstalled = mainState.stats.rpcSqlInstalled,
              onRefreshRole = authViewModel::refreshAdminAccess,
              onLogout = { showAssistant = false; assistantViewModel.clearOnLogout(); notifViewModel.stopAndClear(); authViewModel.logout() },
              onDismiss = { showSettings = false }
            )
          }
          if (showPermDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AdminPermissionRationaleDialog(
              onAllow = { showPermDialog = false; permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
              onDismiss = { showPermDialog = false; AdminNotificationPrefs.setPermissionPromptDismissed(this@MainActivity, true) }
            )
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleNotificationIntent(intent)
  }

  private fun handleNotificationIntent(intent: Intent?) {
    AdminNotificationDispatcher.parseIntentDeepLink(intent, applicationContext)?.let { link ->
      notifViewModel.triggerDeepLink(link, authViewModel.state.value.session)
    }
  }

  override fun onResume() {
    super.onResume()
    authViewModel.refreshAdminAccess()
  }
}
