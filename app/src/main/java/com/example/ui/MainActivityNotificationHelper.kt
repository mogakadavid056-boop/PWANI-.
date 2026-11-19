package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.example.data.AttendanceSubScreen
import com.example.data.ClassItem
import com.example.data.UserSession
import com.example.notifications.NotificationDeepLink
import com.example.ui.components.ClassTab
import com.example.ui.screens.MoreSubView
import com.example.util.RoleHelper

object MainActivityNotificationRouter {
  fun routeDeepLink(
    link: NotificationDeepLink,
    session: UserSession,
    classes: List<ClassItem>,
    selectedClass: ClassItem?,
    openClassDirectly: (ClassItem) -> Unit,
    onOpenTab: (ClassTab, MoreSubView) -> Unit,
    onOpenMyAttendance: () -> Unit,
    onOpenAttendanceTracker: (String) -> Unit,
    onOpenSignAttendance: (String) -> Unit,
    onOpenManageMembers: (ClassItem) -> Unit,
    onOpenHome: () -> Unit,
    onLoadClasses: (UserSession) -> Unit
  ): Boolean {
    when (link.kind) {
      "lecturer_verified", "join_approved" -> {
        onOpenHome()
        return true
      }
    }
    val targetClassId = link.classId
    val cls = classes.find { it.id == targetClassId }
      ?: selectedClass?.takeIf { targetClassId.isBlank() || it.id == targetClassId }
      ?: if (targetClassId.isBlank()) classes.firstOrNull() else null

    if (cls == null) {
      onLoadClasses(session)
      return false
    }

    if (link.kind == "lecturer_join_request") {
      onOpenManageMembers(cls)
      return true
    }

    if (selectedClass?.id != cls.id) {
      openClassDirectly(cls)
    }

    when (link.kind) {
      "notice", "notice_urgent", "prefect_appointed" -> onOpenTab(ClassTab.Notices, MoreSubView.None)
      "attendance_open" -> {
        onOpenTab(ClassTab.Attendance, MoreSubView.None)
        val sid = link.data["session_id"]
        if (!sid.isNullOrBlank() && RoleHelper.canSignAttendance(session.role)) {
          onOpenSignAttendance(sid)
        }
      }
      "timetable" -> onOpenTab(ClassTab.Timetable, MoreSubView.None)
      "material", "new_unit", "unit_approved", "unit_rejected" -> onOpenTab(ClassTab.More, MoreSubView.Materials)
      "poll" -> onOpenTab(ClassTab.More, MoreSubView.Polls)
      "attendance_warning" -> {
        onOpenTab(ClassTab.Attendance, MoreSubView.None)
        onOpenMyAttendance()
      }
      "attendance_alert" -> {
        onOpenTab(ClassTab.Attendance, MoreSubView.None)
        onOpenAttendanceTracker(cls.id)
      }
      else -> onOpenTab(ClassTab.Notices, MoreSubView.None)
    }
    return true
  }
}

@Composable
fun ClassRepAppBackHandlers(
  uiState: AuthUiState,
  classesUiState: ClassesUiState,
  noticesUiState: NoticesUiState,
  attendanceUiState: AttendanceUiState,
  pollsUiState: PollsUiState,
  notifReturnScreen: Screen,
  viewModel: AuthViewModel,
  classesViewModel: ClassesViewModel,
  noticesViewModel: NoticesViewModel,
  attendanceViewModel: AttendanceViewModel,
  membersViewModel: MembersViewModel,
  pollsViewModel: PollsViewModel
) {
  when (uiState.currentScreen) {
    Screen.Home -> {}
    Screen.Register, Screen.Login -> BackHandler { viewModel.navigateTo(Screen.Welcome) }
    Screen.CheckEmail -> BackHandler { viewModel.navigateTo(Screen.Login) }
    Screen.RegisterResult -> BackHandler { viewModel.navigateTo(Screen.Home) }
    Screen.Welcome, Screen.Landing, Screen.Waiting -> BackHandler { }
    Screen.Notifications -> BackHandler { viewModel.navigateTo(notifReturnScreen) }
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
    Screen.PostNotice, Screen.ManageMembers, Screen.GroupChat, Screen.GroupInfo -> BackHandler {
      noticesViewModel.clearError(); membersViewModel.clearError(); viewModel.navigateTo(Screen.ClassDetail)
    }
  }
}

