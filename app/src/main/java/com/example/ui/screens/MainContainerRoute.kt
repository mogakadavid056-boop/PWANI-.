package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.ClassItem
import com.example.data.UserSession
import com.example.ui.AdminUiState
import com.example.ui.AdminViewModel
import com.example.ui.ClassesUiState
import com.example.ui.ClassesViewModel
import com.example.ui.NoticesUiState
import com.example.ui.NoticesViewModel
import com.example.ui.UniversityUiState
import com.example.ui.UniversityViewModel
import com.example.ui.components.MainBottomNavigationBar
import com.example.ui.components.MainTab
import com.example.ui.theme.DarkBackground
import com.example.util.RoleHelper

@Composable
fun MainContainerRoute(
  session: UserSession?,
  classesUiState: ClassesUiState,
  noticesUiState: NoticesUiState,
  universityUiState: UniversityUiState,
  adminUiState: AdminUiState,
  classesViewModel: ClassesViewModel,
  noticesViewModel: NoticesViewModel,
  universityViewModel: UniversityViewModel,
  adminViewModel: AdminViewModel,
  onNavigateToJoinClass: () -> Unit,
  onNavigateToCreateClass: () -> Unit,
  onNavigateToFindClasses: () -> Unit = {},
  onSelectClass: (ClassItem) -> Unit,
  onOpenManageMembersForClass: (String, String) -> Unit = { _, _ -> },
  onBecomeLecturer: (String, (Result<String>) -> Unit) -> Unit = { _, cb -> cb(Result.failure(Exception("Unavailable"))) },
  unreadNotificationCount: Int = 0,
  onOpenNotifications: () -> Unit = {},
  onLogout: () -> Unit
) {
  var selectedTab by remember { mutableStateOf(MainTab.Home) }
  val role = session?.role.orEmpty()
  val isAdmin = RoleHelper.isAdmin(role)
  val isLecturer = RoleHelper.isLecturer(role)
  val isStaff = RoleHelper.isStaffRole(role)

  LaunchedEffect(session?.userId, role) {
    if (session != null) {
      noticesViewModel.loadLatestAnnouncements(session)
      if (isStaff) {
        classesViewModel.loadClassesWaitingForRep(session)
      }
    }
  }

  if (selectedTab != MainTab.Home) {
    BackHandler { selectedTab = MainTab.Home }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Box(modifier = Modifier.weight(1f)) {
        AnimatedContent(
          targetState = selectedTab,
          transitionSpec = {
            (fadeIn(tween(180)) + slideInHorizontally(tween(180)) { it / 10 }) togetherWith
              (fadeOut(tween(140)) + slideOutHorizontally(tween(140)) { -it / 10 })
          },
          label = "main_tab_transition"
        ) { currentTab ->
          when (currentTab) {
            MainTab.Home -> HomeScreen(
              session = session,
              latestAnnouncements = noticesUiState.latestAnnouncements,
              isLatestLoading = noticesUiState.isLatestLoading,
              classes = classesUiState.classes,
              isClassesLoading = classesUiState.isLoading,
              classesError = classesUiState.errorMessage,
              classesWaitingForRep = classesUiState.classesWaitingForRep,
              isWaitingLoading = classesUiState.isWaitingLoading,
              unreadNotificationCount = unreadNotificationCount,
              onOpenNotifications = onOpenNotifications,
              onAppointPrefectClick = { waitingItem ->
                onOpenManageMembersForClass(waitingItem.classId, waitingItem.className)
              },
              onDismissClassesError = { classesViewModel.clearError() },
              onRefreshClasses = {
                classesViewModel.loadClasses(session)
                noticesViewModel.loadLatestAnnouncements(session)
                if (isStaff) {
                  classesViewModel.loadClassesWaitingForRep(session)
                }
              },
              onNavigateToJoinClass = onNavigateToJoinClass,
              onNavigateToCreateClass = onNavigateToCreateClass,
              onNavigateToFindClasses = onNavigateToFindClasses,
              onNavigateToMyClasses = { selectedTab = MainTab.Classes },
              onNavigateToManageUsers = {
                adminViewModel.setSession(session)
                selectedTab = MainTab.Admin
              },
              onNavigateToProfile = {
                universityViewModel.loadProfile(session)
                universityViewModel.loadSchools(session?.accessToken)
                selectedTab = MainTab.Profile
              },
              onSelectClass = onSelectClass,
              onLogout = onLogout
            )

            MainTab.Classes -> {
              if (isAdmin) {
                FindClassesScreen(
                  session = session,
                  isAdmin = true,
                  unreadNotificationCount = unreadNotificationCount,
                  onOpenNotifications = onOpenNotifications,
                  onBack = null,
                  onOpenClass = onSelectClass
                )
              } else {
                ClassesListScreen(
                  classes = classesUiState.classes,
                  isStaff = isStaff,
                  isAdmin = isAdmin,
                  isLecturer = isLecturer,
                  isVerifiedLecturer = session?.isVerified == true,
                  isLoading = classesUiState.isLoading,
                  errorMessage = classesUiState.errorMessage,
                  unreadNotificationCount = unreadNotificationCount,
                  onOpenNotifications = onOpenNotifications,
                  onDismissError = { classesViewModel.clearError() },
                  onRefresh = { classesViewModel.loadClasses(session) },
                  onSelectClass = onSelectClass,
                  onNavigateToJoinClass = onNavigateToJoinClass,
                  onNavigateToCreateClass = onNavigateToCreateClass,
                  onNavigateToFindClasses = onNavigateToFindClasses
                )
              }
            }

            MainTab.Profile -> ProfileScreen(
              profile = universityUiState.profile,
              schools = universityUiState.schools,
              programmes = universityUiState.programmes,
              role = role,
              isStaff = isStaff,
              isStudentInClass = classesUiState.classes.isNotEmpty(),
              isLoading = universityUiState.isProfileLoading,
              isActionLoading = universityUiState.isActionLoading,
              errorMessage = universityUiState.errorMessage,
              successMessage = universityUiState.successMessage,
              onDismissError = { universityViewModel.clearError() },
              onDismissSuccess = { universityViewModel.clearSuccess() },
              onSchoolSelected = { s -> universityViewModel.loadProgrammesForSchool(s.id, session?.accessToken) },
              onSaveProfile = { name, s, prog, y ->
                session?.let { universityViewModel.updateProfile(it.userId, it.accessToken, name, s, prog, y) }
              },
              onBecomeLecturer = onBecomeLecturer,
              onShowError = { msg -> universityViewModel.showError(msg) },
              onShowSuccess = { msg -> universityViewModel.showSuccess(msg) },
              unreadNotificationCount = unreadNotificationCount,
              onOpenNotifications = onOpenNotifications,
              onLogout = onLogout,
              onBack = { selectedTab = MainTab.Home }
            )

            MainTab.Admin -> {
              if (!isAdmin) {
                selectedTab = MainTab.Home
              } else {
                ManageUsersScreen(
                  users = adminUiState.users,
                  searchQuery = adminUiState.searchQuery,
                  isLoading = adminUiState.isLoading,
                  isActionLoading = adminUiState.isActionLoading,
                  errorMessage = adminUiState.errorMessage,
                  successMessage = adminUiState.successMessage,
                  selectedUser = adminUiState.selectedUser,
                  selectedRoleChoice = adminUiState.selectedRoleChoice,
                  showConfirmDialog = adminUiState.showConfirmDialog,
                  verificationPendingUser = adminUiState.verificationPendingUser,
                  verificationTargetStatus = adminUiState.verificationTargetStatus,
                  showVerificationConfirmDialog = adminUiState.showVerificationConfirmDialog,
                  onSearchQueryChanged = { q -> adminViewModel.onSearchQueryChanged(q) },
                  onRefresh = { adminViewModel.refreshUsers() },
                  onUserClick = { u -> adminViewModel.selectUserForRoleChange(u) },
                  onSelectRole = { r -> adminViewModel.updateRoleChoice(r) },
                  onPromptConfirmRole = { adminViewModel.promptConfirmRole() },
                  onDismissConfirmRole = { adminViewModel.dismissConfirmDialog() },
                  onConfirmSaveRole = { adminViewModel.confirmAssignRole() },
                  onDismissRoleDialog = { adminViewModel.dismissRoleDialog() },
                  onToggleVerification = { u -> adminViewModel.promptToggleLecturerVerification(u) },
                  onDismissVerificationDialog = { adminViewModel.dismissVerificationDialog() },
                  onConfirmToggleVerification = { adminViewModel.confirmToggleLecturerVerification() },
                  onDismissError = { adminViewModel.clearError() },
                  onDismissSuccess = { adminViewModel.clearSuccess() },
                  onBack = {
                    adminViewModel.clearError()
                    adminViewModel.dismissRoleDialog()
                    selectedTab = MainTab.Home
                  }
                )
              }
            }
          }
        }
      }

      MainBottomNavigationBar(
        selectedTab = selectedTab,
        isAdmin = isAdmin,
        onSelectTab = { tab ->
          if (tab == MainTab.Admin) {
            adminViewModel.setSession(session)
          } else if (tab == MainTab.Profile) {
            universityViewModel.loadProfile(session)
            universityViewModel.loadSchools(session?.accessToken)
          }
          selectedTab = tab
        }
      )
    }
  }
}
