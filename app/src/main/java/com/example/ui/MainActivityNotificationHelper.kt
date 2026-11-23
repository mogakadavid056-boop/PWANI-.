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
    onLoadClasses: (UserSession) -> Unit,
    onOpenQuizDetail: (kind: String, quizId: String, cls: ClassItem) -> Unit = { _, _, _ -> }
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
      "quiz", "paper_marked" -> {
        onOpenTab(ClassTab.More, MoreSubView.Challenges)
        val qid = link.data["quiz_id"]
        if (!qid.isNullOrBlank()) {
          onOpenQuizDetail(link.kind, qid, cls)
        }
      }
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
  challengesUiState: ChallengesUiState = ChallengesUiState(),
  notifReturnScreen: Screen,
  assistantReturnScreen: Screen = Screen.Home,
  viewModel: AuthViewModel,
  classesViewModel: ClassesViewModel,
  noticesViewModel: NoticesViewModel,
  attendanceViewModel: AttendanceViewModel,
  membersViewModel: MembersViewModel,
  pollsViewModel: PollsViewModel,
  challengesViewModel: ChallengesViewModel? = null
) {
  when (uiState.currentScreen) {
    Screen.Home -> {}
    Screen.Register, Screen.Login -> BackHandler { viewModel.navigateTo(Screen.Welcome) }
    Screen.CheckEmail -> BackHandler { viewModel.navigateTo(Screen.Login) }
    Screen.RegisterResult -> BackHandler { viewModel.navigateTo(Screen.Home) }
    Screen.Splash, Screen.Welcome, Screen.Landing -> BackHandler { }
    Screen.Waiting -> BackHandler { viewModel.navigateTo(Screen.Home) }
    Screen.Notifications -> BackHandler { viewModel.navigateTo(notifReturnScreen) }
    Screen.Assistant -> BackHandler { viewModel.navigateTo(assistantReturnScreen) }
    Screen.Profile -> BackHandler {
      if (noticesUiState.selectedClass != null) viewModel.navigateTo(Screen.ClassDetail)
      else viewModel.navigateTo(Screen.Home)
    }
    Screen.ManageUsers -> BackHandler { viewModel.navigateTo(Screen.Home) }
    Screen.JoinClass, Screen.CreateClass, Screen.FindClasses -> BackHandler { classesViewModel.clearError(); viewModel.navigateTo(Screen.Home) }
    Screen.ClassDetail -> BackHandler {
      if (challengesUiState.pdfViewerFile != null) challengesViewModel?.closePdfViewer()
      else if (challengesUiState.activePaper != null) challengesViewModel?.closePaperMarking()
      else if (challengesUiState.markingQuiz != null) challengesViewModel?.closeExamSubmissions()
      else if (challengesUiState.playingQuiz != null) challengesViewModel?.closePlayQuiz()
      else if (challengesUiState.reviewQuiz != null) challengesViewModel?.closeQuizReview()
      else if (challengesUiState.resultsQuiz != null) challengesViewModel?.closeQuizResults()
      else if (pollsUiState.selectedPoll != null) pollsViewModel.closePollDetail()
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

fun resolveStudentOrStaffDestination(
  session: UserSession,
  clearMsg: Boolean,
  classesViewModel: ClassesViewModel,
  viewModel: AuthViewModel,
  openClassDirectly: (ClassItem) -> Unit,
  onLaunchLastClass: (List<ClassItem>) -> Unit
) {
  val isStaff = RoleHelper.isStaffRole(session.role)
  classesViewModel.loadClasses(session) { loaded ->
    if (!isStaff) {
      if (loaded.isNotEmpty()) openClassDirectly(loaded.first())
      else classesViewModel.joinMyClass(
        session = session,
        onResult = { classId ->
          if (!classId.isNullOrBlank()) classesViewModel.loadClasses(session) { refreshed ->
            val target = refreshed.find { it.id == classId } ?: refreshed.firstOrNull()
            if (target != null) openClassDirectly(target) else viewModel.navigateTo(Screen.Home, clearMsg)
          } else viewModel.navigateTo(Screen.Home, clearMsg)
        },
        onError = { classesViewModel.clearError(); viewModel.navigateTo(Screen.Home, clearMsg) }
      )
    } else {
      onLaunchLastClass(loaded)
    }
  }
}


