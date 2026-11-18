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
import com.example.data.ClassItem
import com.example.data.UserSession
import com.example.ui.screens.*
import com.example.util.RoleHelper

@Composable
fun ClassRepNavHost(
  currentScreen: Screen,
  authUiState: AuthUiState,
  classesUiState: ClassesUiState,
  noticesUiState: NoticesUiState,
  attendanceUiState: AttendanceUiState,
  membersUiState: MembersUiState,
  groupsUiState: GroupsUiState,
  timetableUiState: TimetableUiState,
  materialsUiState: MaterialsUiState,
  universityUiState: UniversityUiState,
  pollsUiState: PollsUiState,
  adminUiState: AdminUiState,
  authViewModel: AuthViewModel,
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
  openTimetableSignal: Int = 0,
  openClassDirectly: (ClassItem) -> Unit,
  resolveDestination: (UserSession) -> Unit,
  navigateToProfileScreen: () -> Unit
) {
  AnimatedContent(
    targetState = currentScreen,
    transitionSpec = {
      (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 8 }) togetherWith
        (fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it / 8 })
    },
    label = "main_screen_transition"
  ) { screen ->
    when (screen) {
      Screen.Welcome, Screen.Landing -> WelcomeScreen(
        onNavigateToLogin = { authViewModel.navigateTo(Screen.Login) }
      )
      Screen.Register -> RegisterScreen(
        isLoading = authUiState.isLoading, errorMessage = authUiState.errorMessage,
        schools = universityUiState.schools, programmes = universityUiState.programmes,
        onSchoolSelected = { s -> universityViewModel.loadProgrammesForSchool(s.id) },
        onDismissError = { authViewModel.clearError() },
        onRegisterWithKnown = { name, adm, email, pass, s, prog, y ->
          authViewModel.registerWithKnown(name, adm, email, pass, s, prog, y) { classId ->
            if (!classId.isNullOrBlank()) {
              classesViewModel.loadClasses(authViewModel.uiState.value.session) { list ->
                val target = list.find { it.id == classId } ?: list.firstOrNull()
                if (target != null) openClassDirectly(target) else authViewModel.navigateTo(Screen.Waiting)
              }
            }
          }
        },
        onRegisterWithCode = { name, adm, email, pass, code ->
          authViewModel.registerWithCode(name, adm, email, pass, code) { classId ->
            classesViewModel.loadClasses(authViewModel.uiState.value.session) { list ->
              val target = if (!classId.isNullOrBlank()) list.find { it.id == classId } else list.firstOrNull()
              if (target != null) openClassDirectly(target) else authViewModel.navigateTo(Screen.Waiting)
            }
          }
        },
        onNavigateToLogin = { authViewModel.navigateTo(Screen.Login) },
        onBack = { authViewModel.navigateTo(Screen.Welcome) }
      )
      Screen.Login -> LoginScreen(
        isLoading = authUiState.isLoading,
        errorMessage = authUiState.errorMessage,
        infoMessage = authUiState.infoMessage,
        onDismissError = { authViewModel.clearError() },
        onDismissInfo = { authViewModel.clearInfo() },
        onLoginSubmit = { e, p -> authViewModel.login(e, p) { session -> resolveDestination(session) } },
        onNavigateToRegister = { universityViewModel.loadSchools(); authViewModel.navigateTo(Screen.Register) },
        onBack = { authViewModel.navigateTo(Screen.Welcome) }
      )
      Screen.Waiting -> WaitingScreen(
        isLoading = classesUiState.isActionLoading, errorMessage = classesUiState.errorMessage ?: authUiState.errorMessage,
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
            } else {
              authViewModel.setError("No matching class found for your programme and year yet.")
            }
          })
        },
        onNavigateToProfile = navigateToProfileScreen
      )
      Screen.Home, Screen.ManageUsers -> MainContainerRoute(
        session = authUiState.session, classesUiState = classesUiState, noticesUiState = noticesUiState,
        universityUiState = universityUiState, adminUiState = adminUiState, classesViewModel = classesViewModel,
        noticesViewModel = noticesViewModel, universityViewModel = universityViewModel, adminViewModel = adminViewModel,
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
        onLogout = { classesViewModel.clearError(); noticesViewModel.clearSelectedClass(); authViewModel.logout() }
      )
      Screen.Profile -> {
        val role = authUiState.session?.role.orEmpty()
        val isStaff = RoleHelper.isStaffRole(role)
        ProfileScreen(
          profile = universityUiState.profile, schools = universityUiState.schools,
          programmes = universityUiState.programmes, role = role, isStaff = isStaff,
          isStudentInClass = classesUiState.classes.isNotEmpty(), isLoading = universityUiState.isProfileLoading,
          isActionLoading = universityUiState.isActionLoading, errorMessage = universityUiState.errorMessage,
          successMessage = universityUiState.successMessage, onDismissError = { universityViewModel.clearError() },
          onDismissSuccess = { universityViewModel.clearSuccess() },
          onSchoolSelected = { s -> universityViewModel.loadProgrammesForSchool(s.id, authUiState.session?.accessToken) },
          onSaveProfile = { name, s, prog, y ->
            authUiState.session?.let { universityViewModel.updateProfile(it.userId, it.accessToken, name, s, prog, y) }
          },
          onBecomeLecturer = { passcode, cb -> authViewModel.becomeLecturer(passcode, cb) },
          onShowError = { msg -> universityViewModel.showError(msg) },
          onShowSuccess = { msg -> universityViewModel.showSuccess(msg) },
          onLogout = { classesViewModel.clearError(); noticesViewModel.clearSelectedClass(); authViewModel.logout() },
          onBack = {
            if (noticesUiState.selectedClass != null) authViewModel.navigateTo(Screen.ClassDetail)
            else if (classesUiState.classes.isEmpty() && !isStaff) authViewModel.navigateTo(Screen.Waiting)
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
        session = authUiState.session,
        isAdmin = RoleHelper.isAdmin(authUiState.session?.role),
        onBack = { authViewModel.navigateTo(Screen.Home) },
        onOpenClass = { cls ->
          classesViewModel.loadClasses(authUiState.session) {
            openClassDirectly(cls)
          }
        }
      )
      Screen.ClassDetail -> ClassDetailRoute(
        session = authUiState.session, noticesUiState = noticesUiState, attendanceUiState = attendanceUiState,
        groupsUiState = groupsUiState, timetableUiState = timetableUiState, materialsUiState = materialsUiState,
        pollsUiState = pollsUiState, noticesViewModel = noticesViewModel, classesViewModel = classesViewModel,
        attendanceViewModel = attendanceViewModel, membersViewModel = membersViewModel,
        groupsViewModel = groupsViewModel, timetableViewModel = timetableViewModel,
        materialsViewModel = materialsViewModel, pollsViewModel = pollsViewModel,
        allClasses = classesUiState.classes,
        openTimetableSignal = openTimetableSignal,
        onMemberDetailsSaved = { name, adm -> authViewModel.updateMemberDetails(name, adm) },
        onSwitchClass = openClassDirectly,
        onNavigateToJoinClass = { classesViewModel.clearError(); authViewModel.navigateTo(Screen.JoinClass) },
        onNavigateToCreateClass = {
          classesViewModel.clearError()
          universityViewModel.loadSchools(authUiState.session?.accessToken)
          universityViewModel.loadAcademicYears(authUiState.session)
          authViewModel.navigateTo(Screen.CreateClass)
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
