package com.example.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.admin.ui.AdminAuthViewModel
import com.example.admin.ui.AdminMainViewModel
import com.example.admin.ui.AdminManageViewModel
import com.example.admin.ui.AdminTab
import com.example.admin.ui.components.AdminBackdrop
import com.example.admin.ui.components.AdminFeedbackBanner
import com.example.admin.ui.screens.AdminDashboardScreen
import com.example.admin.ui.screens.AdminLoginScreen
import com.example.admin.ui.screens.AdminOversightScreen
import com.example.admin.ui.screens.AdminSettingsDialog
import com.example.admin.ui.screens.AdminUniversityScreen
import com.example.admin.ui.screens.AdminUsersScreen
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.ClassRepAdminTheme
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
  private val authViewModel: AdminAuthViewModel by viewModels()
  private val mainViewModel: AdminMainViewModel by viewModels()
  private val manageViewModel: AdminManageViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      ClassRepAdminTheme {
        val authState by authViewModel.state.collectAsStateWithLifecycle()
        val mainState by mainViewModel.state.collectAsStateWithLifecycle()
        val manageState by manageViewModel.state.collectAsStateWithLifecycle()
        var showSettings by remember { mutableStateOf(false) }

        LaunchedEffect(authState.session) {
          mainViewModel.setSession(authState.session)
          manageViewModel.setSession(authState.session)
        }

        if (authState.isInitializing) {
          AdminBackdrop {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(color = GoldPrimary)
            }
          }
        } else if (authState.session == null) {
          AdminLoginScreen(
            isLoading = authState.isLoading,
            errorMessage = authState.errorMessage,
            onLogin = authViewModel::login,
            onDismissError = authViewModel::clearError
          )
        } else {
          val session = authState.session!!
          val combinedError = mainState.errorMessage ?: manageState.errorMessage
          val combinedSuccess = mainState.successMessage ?: manageState.successMessage

          AdminShell(activeTab = mainState.activeTab, onSelectTab = mainViewModel::selectTab) { innerModifier ->
            Column(modifier = innerModifier.fillMaxSize()) {
              combinedError?.let { msg ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                  AdminFeedbackBanner(
                    message = msg,
                    isError = true,
                    onDismiss = { mainViewModel.clearError(); manageViewModel.clearError() }
                  )
                }
              }
              combinedSuccess?.let { msg ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                  AdminFeedbackBanner(
                    message = msg,
                    isError = false,
                    onDismiss = { mainViewModel.clearSuccess(); manageViewModel.clearSuccess() }
                  )
                }
              }

              when (mainState.activeTab) {
                AdminTab.OVERVIEW -> AdminDashboardScreen(
                  session = session,
                  stats = mainState.stats,
                  isLoading = mainState.isStatsLoading,
                  onRefresh = { mainViewModel.refreshAll(); manageViewModel.refreshUniversityData(); manageViewModel.refreshClasses() },
                  onJumpToUnverified = mainViewModel::jumpToUnverifiedLecturers,
                  onNavigateTab = mainViewModel::selectTab,
                  onOpenSettings = { showSettings = true }
                )
                AdminTab.USERS -> AdminUsersScreen(
                  users = mainState.users,
                  searchQuery = mainState.userSearch,
                  activeFilter = mainState.userFilter,
                  isLoading = mainState.isUsersLoading,
                  isActionLoading = mainState.isActionLoading,
                  onSearchChanged = mainViewModel::onUserSearchChanged,
                  onFilterChanged = mainViewModel::onUserFilterChanged,
                  onRefresh = mainViewModel::refreshUsers,
                  onAssignRole = mainViewModel::assignRole,
                  onToggleVerify = mainViewModel::setLecturerVerified
                )
                AdminTab.UNIVERSITY -> AdminUniversityScreen(
                  section = manageState.universitySection,
                  schools = manageState.schools,
                  programmes = manageState.programmes,
                  academicYears = manageState.academicYears,
                  isLoading = manageState.isUniLoading,
                  isActionLoading = manageState.isActionLoading,
                  onSelectSection = manageViewModel::selectUniversitySection,
                  onRefresh = manageViewModel::refreshUniversityData,
                  onSaveSchool = manageViewModel::saveSchool,
                  onDeleteSchool = manageViewModel::deleteSchool,
                  onSaveProgramme = manageViewModel::saveProgramme,
                  onDeleteProgramme = manageViewModel::deleteProgramme,
                  onCreateYear = manageViewModel::createAcademicYear,
                  onSetCurrentYear = manageViewModel::setCurrentAcademicYear
                )
                AdminTab.OVERSIGHT -> AdminOversightScreen(
                  classes = manageState.classes,
                  classSearch = manageState.classSearch,
                  isClassesLoading = manageState.isClassesLoading,
                  selectedClass = manageState.selectedClass,
                  registers = manageState.registers,
                  isRegistersLoading = manageState.isRegistersLoading,
                  selectedRegister = manageState.selectedRegister,
                  isRegisterDetailLoading = manageState.isRegisterDetailLoading,
                  isActionLoading = manageState.isActionLoading,
                  onSearchChanged = manageViewModel::onClassSearchChanged,
                  onRefreshClasses = manageViewModel::refreshClasses,
                  onOpenClass = manageViewModel::openClassRegisters,
                  onCloseClass = manageViewModel::closeClassRegisters,
                  onOpenRegister = manageViewModel::openRegisterDetail,
                  onCloseRegisterDetail = manageViewModel::closeRegisterDetail,
                  onCloseSession = manageViewModel::closeAttendanceSession,
                  onVoidSession = manageViewModel::voidAttendanceSession
                )
              }
            }
          }

          if (showSettings) {
            AdminSettingsDialog(
              session = session,
              rpcSqlInstalled = mainState.stats.rpcSqlInstalled,
              onRefreshRole = authViewModel::refreshAdminAccess,
              onLogout = authViewModel::logout,
              onDismiss = { showSettings = false }
            )
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    authViewModel.refreshAdminAccess()
  }
}

@Composable
private fun AdminShell(
  activeTab: AdminTab,
  onSelectTab: (AdminTab) -> Unit,
  content: @Composable (Modifier) -> Unit
) {
  val items = listOf(
    Triple(AdminTab.OVERVIEW, "Overview", Icons.Default.Dashboard),
    Triple(AdminTab.USERS, "Users", Icons.Default.Groups),
    Triple(AdminTab.UNIVERSITY, "University", Icons.Default.AccountBalance),
    Triple(AdminTab.OVERSIGHT, "Oversight", Icons.Default.FactCheck)
  )

  AdminBackdrop {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      if (maxWidth >= 600.dp) {
        Row(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
          NavigationRail(containerColor = AdminSurface) {
            items.forEach { (tab, label, icon) ->
              NavigationRailItem(
                selected = activeTab == tab,
                onClick = { onSelectTab(tab) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationRailItemDefaults.colors(
                  selectedIconColor = DarkTextOnGold,
                  selectedTextColor = GoldPrimary,
                  indicatorColor = GoldPrimary,
                  unselectedIconColor = TextSecondary,
                  unselectedTextColor = TextSecondary
                )
              )
            }
          }
          content(Modifier.weight(1f))
        }
      } else {
        Scaffold(
          containerColor = Color.Transparent,
          contentWindowInsets = WindowInsets.safeDrawing,
          bottomBar = {
            NavigationBar(containerColor = AdminSurface, windowInsets = WindowInsets.navigationBars) {
              items.forEach { (tab, label, icon) ->
                NavigationBarItem(
                  selected = activeTab == tab,
                  onClick = { onSelectTab(tab) },
                  icon = { Icon(icon, contentDescription = label) },
                  label = { Text(label) },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DarkTextOnGold,
                    selectedTextColor = GoldPrimary,
                    indicatorColor = GoldPrimary,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                  )
                )
              }
            }
          }
        ) { padding -> content(Modifier.padding(padding)) }
      }
    }
  }
}
