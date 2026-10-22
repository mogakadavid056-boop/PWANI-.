package com.example

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
import com.example.ui.theme.DarkBackground
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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    com.example.data.OfflineCache.appContext = applicationContext
    com.example.data.NetworkMonitor.initialize(applicationContext)
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
              onSaveLastClass = { id -> lifecycleScope.launch { authViewModel.sessionManager.saveLastClassId(id) } },
              onGetLastClass = { authViewModel.sessionManager.getLastClassId() }
            )
          }
        }
      }
    }
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
  val coroutineScope = rememberCoroutineScope()

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
              val isStaff = session.role.equals("Lecturer", ignoreCase = true) || session.role.equals("Class Representative", ignoreCase = true)
              noticesViewModel.selectClassAndLoad(sel, session)
              attendanceViewModel.loadSessions(sel.id, isStaff, session)
              timetableViewModel.loadSchedules(sel.id, session)
              materialsViewModel.loadMaterials(sel.id, session)
              pollsViewModel.loadPolls(sel.id, session)
              groupsViewModel.loadGroups(sel.id, session)
            }
          }
          Screen.Profile -> {
            universityViewModel.loadProfile(session)
            universityViewModel.loadSchools(session.accessToken)
          }
          Screen.ManageMembers -> {
            val sel = noticesUiState.selectedClass
            if (sel != null) {
              membersViewModel.loadMembers(sel.id, session)
            }
          }
          Screen.Waiting -> {
            classesViewModel.loadClasses(session)
          }
          else -> {}
        }
      }
    }
  }

  val openClassDirectly: (ClassItem) -> Unit = { classItem ->
    onSaveLastClass(classItem.id)
    val isRoleStaff = uiState.session?.role?.equals("admin", ignoreCase = true) == true ||
      uiState.session?.role?.equals("lecturer", ignoreCase = true) == true
    noticesViewModel.selectClassAndLoad(classItem, uiState.session)
    attendanceViewModel.initForClass(classItem.id, isRoleStaff, uiState.session)
    groupsViewModel.loadGroups(classItem.id, uiState.session)
    timetableViewModel.loadSchedules(classItem.id, uiState.session)
    materialsViewModel.loadMaterials(classItem.id, uiState.session)
    pollsViewModel.loadPolls(classItem.id, uiState.session)
    viewModel.navigateTo(Screen.ClassDetail)
  }

  val resolveDestination: (UserSession) -> Unit = { session ->
    val isStaff = session.role.equals("admin", ignoreCase = true) || session.role.equals("lecturer", ignoreCase = true)
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
        // Staff: open last used class if saved in DataStore, otherwise show class list
        coroutineScope.launch {
          val lastId = onGetLastClass()
          val target = if (!lastId.isNullOrBlank()) loadedClasses.find { it.id == lastId } else null
          if (target != null) openClassDirectly(target) else viewModel.navigateTo(Screen.Home)
        }
      }
    }
  }

  LaunchedEffect(uiState.session?.userId) {
    val s = uiState.session
    if (s != null && noticesUiState.selectedClass == null && uiState.currentScreen == Screen.Home) {
      resolveDestination(s)
      noticesViewModel.loadLatestAnnouncements(s)
    }
  }

  LaunchedEffect(noticesUiState.selectedClass?.id, noticesUiState.isStaff) {
    val sel = noticesUiState.selectedClass
    if (sel != null && uiState.session != null) {
      attendanceViewModel.setStaff(noticesUiState.isStaff, uiState.session)
    }
  }

  val navigateToProfileScreen: () -> Unit = {
    universityViewModel.loadProfile(uiState.session)
    universityViewModel.loadSchools(uiState.session?.accessToken)
    viewModel.navigateTo(Screen.Profile)
  }

  when (uiState.currentScreen) {
    Screen.Register, Screen.Login -> BackHandler { viewModel.navigateTo(Screen.Welcome) }
    Screen.Welcome -> BackHandler { /* on welcome root screen */ }
    Screen.Waiting -> BackHandler { /* stay on waiting */ }
    Screen.Profile -> BackHandler {
      if (noticesUiState.selectedClass != null) viewModel.navigateTo(Screen.ClassDetail)
      else if (classesUiState.classes.isEmpty() && uiState.session?.role?.equals("student", ignoreCase = true) == true) viewModel.navigateTo(Screen.Waiting)
      else viewModel.navigateTo(Screen.Home)
    }
    Screen.ManageUsers -> BackHandler { viewModel.navigateTo(Screen.Home) }
    Screen.JoinClass, Screen.CreateClass -> BackHandler { classesViewModel.clearError(); viewModel.navigateTo(Screen.Home) }
    Screen.ClassDetail -> BackHandler {
      if (pollsUiState.selectedPoll != null) pollsViewModel.closePollDetail()
      else if (attendanceUiState.currentSubScreen != AttendanceSubScreen.None) attendanceViewModel.closeSubScreen()
      else {
        val isStaff = uiState.session?.role?.equals("admin", ignoreCase = true) == true ||
          uiState.session?.role?.equals("lecturer", ignoreCase = true) == true
        if (isStaff) {
          classesViewModel.clearError()
          noticesViewModel.clearSelectedClass()
          pollsViewModel.clearError()
          viewModel.navigateTo(Screen.Home)
        }
      }
    }
    Screen.PostNotice, Screen.ManageMembers -> BackHandler {
      noticesViewModel.clearError(); membersViewModel.clearError(); viewModel.navigateTo(Screen.ClassDetail)
    }
    Screen.GroupChat -> BackHandler { groupsViewModel.closeChat(); viewModel.navigateTo(Screen.ClassDetail) }
    Screen.GroupInfo -> BackHandler { viewModel.navigateTo(Screen.GroupChat) }
    else -> {}
  }

  androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
    com.example.ui.components.OfflineBanner(
      isOffline = !isOnline,
      lastUpdatedText = lastUpdatedText
    )
    androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
      ClassRepNavHost(
        currentScreen = uiState.currentScreen,
        authUiState = uiState,
        classesUiState = classesUiState,
        noticesUiState = noticesUiState,
        attendanceUiState = attendanceUiState,
        membersUiState = membersUiState,
        groupsUiState = groupsUiState,
        timetableUiState = timetableUiState,
        materialsUiState = materialsUiState,
        universityUiState = universityUiState,
        pollsUiState = pollsUiState,
        adminUiState = adminUiState,
        authViewModel = viewModel,
        classesViewModel = classesViewModel,
        noticesViewModel = noticesViewModel,
        attendanceViewModel = attendanceViewModel,
        membersViewModel = membersViewModel,
        groupsViewModel = groupsViewModel,
        timetableViewModel = timetableViewModel,
        materialsViewModel = materialsViewModel,
        universityViewModel = universityViewModel,
        pollsViewModel = pollsViewModel,
        adminViewModel = adminViewModel,
        openClassDirectly = openClassDirectly,
        resolveDestination = resolveDestination,
        navigateToProfileScreen = navigateToProfileScreen
      )
    }
    com.example.ui.components.SupportFooter(
      showHelpBanner = uiState.currentScreen in listOf(Screen.Welcome, Screen.Login, Screen.Register)
    )
  }
}
