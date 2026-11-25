package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ClassItem
import com.example.data.UserSession
import com.example.ui.components.ClassTab
import com.example.ui.screens.*
import com.example.util.RoleHelper

@Composable
fun ClassRepNavHost(
  currentScreen: Screen,
  authUiState: AuthUiState,
  classesUiState: ClassesUiState,
  noticesUiState: NoticesUiState,
  attendanceUiState: AttendanceUiState,
  trackerUiState: AttendanceTrackerUiState,
  notificationUiState: NotificationUiState,
  membersUiState: MembersUiState,
  groupsUiState: GroupsUiState,
  timetableUiState: TimetableUiState,
  materialsUiState: MaterialsUiState,
  universityUiState: UniversityUiState,
  pollsUiState: PollsUiState,
  challengesUiState: ChallengesUiState = ChallengesUiState(),
  adminUiState: AdminUiState,
  authViewModel: AuthViewModel,
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
  challengesViewModel: ChallengesViewModel? = null,
  adminViewModel: AdminViewModel,
  openTimetableSignal: Int = 0,
  openTabSignal: Triple<Int, ClassTab, MoreSubView>? = null,
  onOpenNotifications: () -> Unit = {},
  onBackFromNotifications: () -> Unit = {},
  isOnline: Boolean = true,
  assistantViewModel: com.example.assistant.AssistantViewModel? = null,
  assistantExecutor: com.example.assistant.ActionExecutor? = null,
  assistantReturnScreen: Screen = Screen.Home,
  onOpenAssistant: () -> Unit = {},
  onBackFromAssistant: () -> Unit = {},
  openClassDirectly: (ClassItem) -> Unit,
  onOpenPaperToMark: (com.example.data.PaperToMarkItem) -> Unit = {},
  resolveDestination: (UserSession) -> Unit,
  navigateToProfileScreen: () -> Unit
) {
  val unreadCount = notificationUiState.unreadCount

  AnimatedContent(
    targetState = currentScreen,
    transitionSpec = {
      (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 8 }) togetherWith
        (fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it / 8 })
    },
    label = "main_screen_transition"
  ) { screen ->
    when (screen) {
      Screen.Splash -> SplashScreen()
      Screen.Welcome, Screen.Landing -> WelcomeScreen(
        onNavigateToLogin = { authViewModel.navigateTo(Screen.Login) }
      )
      Screen.Register -> RegisterScreen(
        isLoading = authUiState.isLoading, errorMessage = authUiState.errorMessage,
        schools = universityUiState.schools, programmes = universityUiState.programmes,
        onSchoolSelected = { s -> universityViewModel.loadProgrammesForSchool(s.id) },
        onDismissError = { authViewModel.clearError() },
        onSubmitWizard = { wizard -> authViewModel.submitRegistrationWizard(wizard) },
        onNavigateToLogin = { authViewModel.navigateTo(Screen.Login) },
        onBack = { authViewModel.navigateTo(Screen.Welcome) }
      )
      Screen.CheckEmail -> CheckEmailScreen(
        email = authUiState.pendingConfirmationEmail.orEmpty(),
        isLoading = authUiState.isLoading, errorMessage = authUiState.errorMessage,
        infoMessage = authUiState.infoMessage, onDismissError = { authViewModel.clearError() },
        onResendEmail = { authViewModel.resendConfirmationEmail() },
        onGoToLogin = { authViewModel.navigateTo(Screen.Login) }
      )
      Screen.RegisterResult -> RegisterResultScreen(
        result = authUiState.registerResult ?: RegisterResultState.StudentNotPlaced(),
        isLoading = authUiState.isLoading, errorMessage = authUiState.errorMessage,
        onDismissError = { authViewModel.clearError() },
        onOpenMyClass = { classId ->
          classesViewModel.loadClasses(authUiState.session) { list ->
            val target = list.find { it.id == classId } ?: list.firstOrNull()
            if (target != null) openClassDirectly(target) else authViewModel.navigateTo(Screen.Home)
          }
        },
        onRetryAutoJoin = { authViewModel.retryStudentAutoJoin() },
        onJoinWithCode = { code -> authViewModel.retryStudentJoinByCode(code) },
        onOpenFindClasses = { authViewModel.navigateTo(Screen.FindClasses) },
        onOpenJoinClass = { classesViewModel.clearError(); authViewModel.navigateTo(Screen.JoinClass) },
        onOpenProfile = navigateToProfileScreen,
        onContinueHome = { authViewModel.navigateTo(Screen.Home) }
      )
      Screen.Login -> LoginScreen(
        isLoading = authUiState.isLoading, errorMessage = authUiState.errorMessage,
        infoMessage = authUiState.infoMessage, onDismissError = { authViewModel.clearError() },
        onDismissInfo = { authViewModel.clearInfo() },
        onLoginSubmit = { e, p -> authViewModel.login(e, p) { session -> resolveDestination(session) } },
        onNavigateToRegister = { universityViewModel.loadSchools(); authViewModel.navigateTo(Screen.Register) },
        onBack = { authViewModel.navigateTo(Screen.Welcome) }
      )
      Screen.Waiting -> WaitingScreen(
        isLoading = classesUiState.isActionLoading, errorMessage = classesUiState.errorMessage ?: authUiState.errorMessage,
        unreadNotificationCount = unreadCount, onOpenNotifications = onOpenNotifications,
        onDismissError = { classesViewModel.clearError(); authViewModel.clearError() },
        onJoinWithCode = { code ->
          classesViewModel.joinClass(code, authUiState.session, onSuccess = { classId, isPending ->
            if (isPending) return@joinClass
            classesViewModel.loadClasses(authUiState.session) { list ->
              val target = if (!classId.isNullOrBlank()) list.find { it.id == classId } else list.firstOrNull()
              if (target != null) openClassDirectly(target)
            }
          })
        },
        onTryAutoJoin = {
          classesViewModel.joinMyClass(authUiState.session, onResult = { classId ->
            if (!classId.isNullOrBlank()) {
              classesViewModel.loadClasses(authUiState.session) { list ->
                val target = list.find { it.id == classId } ?: list.firstOrNull()
                if (target != null) openClassDirectly(target)
              }
            } else authViewModel.setError("No matching class found for your programme and year yet.")
          })
        },
        onNavigateToProfile = navigateToProfileScreen,
        onContinueHome = { authViewModel.navigateTo(Screen.Home) }
      )
      Screen.Home, Screen.ManageUsers -> MainContainerRoute(
        session = authUiState.session, classesUiState = classesUiState, noticesUiState = noticesUiState,
        universityUiState = universityUiState, adminUiState = adminUiState, classesViewModel = classesViewModel,
        noticesViewModel = noticesViewModel, universityViewModel = universityViewModel, adminViewModel = adminViewModel,
        challengesUiState = challengesUiState, challengesViewModel = challengesViewModel, onOpenPaperToMark = onOpenPaperToMark,
        authInfoMessage = authUiState.infoMessage, onDismissAuthInfo = { authViewModel.clearInfo() },
        unreadNotificationCount = unreadCount, onOpenNotifications = onOpenNotifications,
        isOnline = isOnline, onOpenAssistant = onOpenAssistant,
        onNavigateToJoinClass = { classesViewModel.clearError(); authViewModel.navigateTo(Screen.JoinClass) },
        onNavigateToCreateClass = {
          classesViewModel.clearError()
          universityViewModel.loadSchools(authUiState.session?.accessToken)
          universityViewModel.loadAcademicYears(authUiState.session)
          authViewModel.navigateTo(Screen.CreateClass)
        },
        onNavigateToFindClasses = { authViewModel.navigateTo(Screen.FindClasses) },
        onSelectClass = openClassDirectly,
        onOpenManageMembersForClass = { classId, className ->
          noticesViewModel.selectClassAndLoad(ClassItem(id = classId, name = className, code = ""), authUiState.session)
          membersViewModel.loadMembers(classId, authUiState.session)
          authViewModel.navigateTo(Screen.ManageMembers)
        },
        onBecomeLecturer = { passcode, cb -> authViewModel.becomeLecturer(passcode, cb) },
        onLogout = { classesViewModel.clearError(); noticesViewModel.clearSelectedClass(); notificationViewModel.stopAndClear(); assistantViewModel?.clearOnLogout(); authViewModel.logout() }
      )
      Screen.Profile -> {
        val role = authUiState.session?.role.orEmpty()
        val isStaff = RoleHelper.isStaffRole(role)
        ProfileScreen(
          profile = universityUiState.profile, schools = universityUiState.schools, programmes = universityUiState.programmes,
          role = role, isStaff = isStaff, isStudentInClass = classesUiState.classes.isNotEmpty(),
          isLoading = universityUiState.isProfileLoading, isActionLoading = universityUiState.isActionLoading,
          errorMessage = universityUiState.errorMessage, successMessage = universityUiState.successMessage,
          onDismissError = { universityViewModel.clearError() }, onDismissSuccess = { universityViewModel.clearSuccess() },
          onSchoolSelected = { s -> universityViewModel.loadProgrammesForSchool(s.id, authUiState.session?.accessToken) },
          onSaveProfile = { name, s, prog, y -> authUiState.session?.let { universityViewModel.updateProfile(it.userId, it.accessToken, name, s, prog, y) } },
          onToggleLeaderboardVisibility = { visible ->
            val s = authUiState.session
            universityViewModel.setLeaderboardVisibility(visible, s) {
              val cid = noticesUiState.selectedClass?.id.orEmpty()
              if (cid.isNotBlank()) challengesViewModel?.loadLeaderboard(classId = cid, session = s)
            }
          },
          onBecomeLecturer = { passcode, cb -> authViewModel.becomeLecturer(passcode, cb) },
          onShowError = { msg -> universityViewModel.showError(msg) }, onShowSuccess = { msg -> universityViewModel.showSuccess(msg) },
          unreadNotificationCount = unreadCount, onOpenNotifications = onOpenNotifications,
          onLogout = { classesViewModel.clearError(); noticesViewModel.clearSelectedClass(); notificationViewModel.stopAndClear(); assistantViewModel?.clearOnLogout(); authViewModel.logout() },
          onBack = {
            if (noticesUiState.selectedClass != null) authViewModel.navigateTo(Screen.ClassDetail)
            else authViewModel.navigateTo(Screen.Home)
          }
        )
      }
      Screen.JoinClass -> JoinClassScreen(
        isLoading = classesUiState.isActionLoading, errorMessage = classesUiState.errorMessage,
        onDismissError = { classesViewModel.clearError() },
        onJoinSubmit = { code ->
          classesViewModel.joinClass(code, authUiState.session, onSuccess = { classId, isPending ->
            if (isPending) return@joinClass
            classesViewModel.loadClasses(authUiState.session) { list ->
              val target = if (!classId.isNullOrBlank()) list.find { it.id == classId } else list.firstOrNull()
              if (target != null) openClassDirectly(target) else authViewModel.navigateTo(Screen.Home)
            }
          })
        },
        onBack = { classesViewModel.clearError(); authViewModel.navigateTo(Screen.Home) }
      )
      Screen.CreateClass -> {
        val isAdmin = RoleHelper.isAdmin(authUiState.session?.role)
        if (!isAdmin) LaunchedEffect(Unit) { authViewModel.navigateTo(Screen.Home) }
        else CreateClassScreen(
          isLoading = classesUiState.isActionLoading, errorMessage = classesUiState.errorMessage,
          createdJoinCode = classesUiState.createdJoinCode, schools = universityUiState.schools,
          programmes = universityUiState.programmes, academicYears = universityUiState.academicYears,
          onSchoolSelected = { s -> universityViewModel.loadProgrammesForSchool(s.id, authUiState.session?.accessToken) },
          onDismissError = { classesViewModel.clearError() },
          onDismissSuccessDialog = { classesViewModel.dismissCreatedDialog(); authViewModel.navigateTo(Screen.Home) },
          onCreateSubmit = { name, code, uId, sId, pId, aId, y -> classesViewModel.createClass(name, code, uId, sId, pId, aId, y, authUiState.session) {} },
          onBack = { classesViewModel.clearError(); authViewModel.navigateTo(Screen.Home) }
        )
      }
      Screen.FindClasses -> FindClassesScreen(
        session = authUiState.session, isAdmin = RoleHelper.isAdmin(authUiState.session?.role),
        unreadNotificationCount = unreadCount, onOpenNotifications = onOpenNotifications,
        isOnline = isOnline, onOpenAssistant = onOpenAssistant,
        onBack = { authViewModel.navigateTo(Screen.Home) },
        onOpenClass = { cls -> classesViewModel.loadClasses(authUiState.session) { openClassDirectly(cls) } }
      )
      Screen.Notifications -> NotificationCenterScreen(
        uiState = notificationUiState,
        onRefresh = { notificationViewModel.refresh(authUiState.session) },
        onLoadMore = { notificationViewModel.loadMore(authUiState.session) },
        onMarkAllRead = { notificationViewModel.markAllAsRead(authUiState.session) },
        onNotificationClick = { item -> notificationViewModel.onNotificationClick(item, authUiState.session) },
        onDismissError = { notificationViewModel.clearError() },
        onBack = onBackFromNotifications
      )
      Screen.Assistant -> {
        val avm = assistantViewModel
        val exec = assistantExecutor
        if (avm != null && exec != null) {
          val aState by avm.uiState.collectAsStateWithLifecycle()
          val screenName = if (assistantReturnScreen == Screen.ClassDetail) "class_detail" else "home"
          com.example.assistant.ChatScreen(
            uiState = aState,
            session = authUiState.session,
            isOnline = isOnline,
            isRepInClass = noticesUiState.isRep,
            activeClassId = noticesUiState.selectedClass?.id ?: classesUiState.classes.firstOrNull()?.id,
            screenContextName = screenName,
            executor = exec,
            onEnterAssistant = { avm.onEnterAssistant(authUiState.session) },
            onAcceptTerms = { avm.acceptTerms(authUiState.session) },
            onInputChanged = { avm.onInputChanged(it) },
            onToggleSpeechLanguage = { avm.toggleSpeechLanguage() },
            onDictationResult = { avm.appendDictation(it) },
            onNewChat = { avm.newChat(authUiState.session) },
            onSendMessage = { msg, ctx -> avm.sendMessage(msg, authUiState.session, ctx, isOnline, noticesUiState.isRep) },
            onConfirmAction = { msgId, actId ->
              avm.confirmAction(
                messageId = msgId,
                actionId = actId,
                fallbackClassId = noticesUiState.selectedClass?.id ?: classesUiState.classes.firstOrNull()?.id,
                session = authUiState.session,
                executor = exec
              )
            },
            onUpdateActionArgs = { msgId, actId, args ->
              avm.updateActionArgs(msgId, actId, args, authUiState.session?.userId.orEmpty())
            },
            onCancelAction = { msgId, actId ->
              avm.cancelAction(msgId, actId, authUiState.session?.userId.orEmpty())
            },
            onDismissError = { avm.clearError() },
            onBack = onBackFromAssistant
          )
        }
      }
      Screen.ClassDetail -> ClassDetailRoute(
        session = authUiState.session, noticesUiState = noticesUiState, attendanceUiState = attendanceUiState,
        trackerUiState = trackerUiState, groupsUiState = groupsUiState, timetableUiState = timetableUiState,
        materialsUiState = materialsUiState, pollsUiState = pollsUiState, challengesUiState = challengesUiState,
        noticesViewModel = noticesViewModel, classesViewModel = classesViewModel, attendanceViewModel = attendanceViewModel,
        trackerViewModel = trackerViewModel, membersViewModel = membersViewModel, groupsViewModel = groupsViewModel,
        timetableViewModel = timetableViewModel, materialsViewModel = materialsViewModel, pollsViewModel = pollsViewModel,
        challengesViewModel = challengesViewModel,
        showOnLeaderboard = universityUiState.profile?.showOnLeaderboard ?: true,
        onToggleLeaderboardVisibility = { visible ->
          val s = authUiState.session
          universityViewModel.setLeaderboardVisibility(visible, s) {
            noticesUiState.selectedClass?.id?.let { cid ->
              challengesViewModel?.loadLeaderboard(classId = cid, session = s)
            }
          }
        },
        allClasses = classesUiState.classes,
        unreadNotificationCount = unreadCount, openTimetableSignal = openTimetableSignal, openTabSignal = openTabSignal,
        onOpenNotifications = onOpenNotifications,
        isOnline = isOnline,
        onOpenAssistant = onOpenAssistant,
        onMemberDetailsSaved = { name, adm -> authViewModel.updateMemberDetails(name, adm) },
        onSwitchClass = openClassDirectly,
        onNavigateToJoinClass = { classesViewModel.clearError(); authViewModel.navigateTo(Screen.JoinClass) },
        onNavigateToCreateClass = {
          classesViewModel.clearError(); universityViewModel.loadSchools(authUiState.session?.accessToken)
          universityViewModel.loadAcademicYears(authUiState.session); authViewModel.navigateTo(Screen.CreateClass)
        },
        onNavigateToFindClasses = { authViewModel.navigateTo(Screen.FindClasses) },
        onNavigateToProfile = navigateToProfileScreen,
        onNavigateToPostNotice = { noticesViewModel.clearError(); authViewModel.navigateTo(Screen.PostNotice) },
        onNavigateToManageMembers = { membersViewModel.loadMembers(noticesUiState.selectedClass?.id.orEmpty(), authUiState.session); authViewModel.navigateTo(Screen.ManageMembers) },
        onOpenChat = { authViewModel.navigateTo(Screen.GroupChat) },
        onBackToHome = { noticesViewModel.clearSelectedClass(); pollsViewModel.clearError(); authViewModel.navigateTo(Screen.Home) }
      )
      Screen.PostNotice, Screen.ManageMembers, Screen.GroupChat, Screen.GroupInfo -> ClassSubScreensRoute(
        currentScreen = screen, session = authUiState.session, noticesUiState = noticesUiState,
        membersUiState = membersUiState, groupsUiState = groupsUiState, authViewModel = authViewModel,
        noticesViewModel = noticesViewModel, membersViewModel = membersViewModel, groupsViewModel = groupsViewModel
      )
    }
  }
}
