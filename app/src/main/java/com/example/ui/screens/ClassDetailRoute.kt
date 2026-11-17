package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.data.UserSession
import com.example.ui.*

@Composable
fun ClassDetailRoute(
  session: UserSession?,
  noticesUiState: NoticesUiState,
  attendanceUiState: AttendanceUiState,
  groupsUiState: GroupsUiState,
  timetableUiState: TimetableUiState,
  materialsUiState: MaterialsUiState,
  pollsUiState: PollsUiState,
  noticesViewModel: NoticesViewModel,
  classesViewModel: ClassesViewModel,
  attendanceViewModel: AttendanceViewModel,
  membersViewModel: MembersViewModel,
  groupsViewModel: GroupsViewModel,
  timetableViewModel: TimetableViewModel,
  materialsViewModel: MaterialsViewModel,
  pollsViewModel: PollsViewModel,
  allClasses: List<com.example.data.ClassItem> = emptyList(),
  openTimetableSignal: Int = 0,
  onMemberDetailsSaved: (fullName: String, admissionNumber: String) -> Unit = { _, _ -> },
  onSwitchClass: (com.example.data.ClassItem) -> Unit = {},
  onNavigateToJoinClass: () -> Unit = {},
  onNavigateToCreateClass: () -> Unit = {},
  onNavigateToFindClasses: () -> Unit = {},
  onNavigateToProfile: () -> Unit = {},
  onNavigateToPostNotice: () -> Unit,
  onNavigateToManageMembers: () -> Unit,
  onOpenChat: () -> Unit,
  onBackToHome: () -> Unit
) {
  val selectedClass = noticesUiState.selectedClass
  if (selectedClass == null) {
    LaunchedEffect(Unit) { onBackToHome() }
    return
  }
  val context = androidx.compose.ui.platform.LocalContext.current

  val attendanceCallbacks = ClassAttendanceCallbacks(
    onRefreshAttendance = {
      attendanceViewModel.loadSessions(session = session)
      attendanceViewModel.loadClassUnits(selectedClass.id, session)
    },
    onStartAttendance = { cId, ct, cc, lec, ven, st, dur ->
      attendanceViewModel.startAttendanceSession(cId, ct, cc, lec, ven, st, dur, session)
    },
    onOpenViewAttendance = { sid -> attendanceViewModel.openViewAttendance(sid, session) },
    onOpenSignAttendance = { sid -> attendanceViewModel.openSignAttendance(sid) },
    onCloseAttendanceSubScreen = { attendanceViewModel.closeSubScreen() },
    onOpenAttendanceSummary = { attendanceViewModel.openAttendanceSummary() },
    onOpenMyAttendance = { attendanceViewModel.openMyAttendance() },
    onShowActiveCode = { sid, exp -> attendanceViewModel.fetchActiveSessionCode(sid, exp, session) },
    onOpenRegister = { sid -> attendanceViewModel.openRegister(sid, session) },
    onCloseSession = { sid -> attendanceViewModel.closeSession(sid, session) },
    onSignOutSession = { sid -> attendanceViewModel.signOutSession(sid, session) },
    onVoidRegisterSession = { reason ->
      val targetId = attendanceUiState.sessionPendingVoid?.id ?: attendanceUiState.selectedSessionId.orEmpty()
      if (targetId.isNotBlank()) attendanceViewModel.voidSession(targetId, reason, session)
    },
    onDeleteRegisterSession = { reason ->
      val targetId = attendanceUiState.selectedSessionId.orEmpty()
      if (targetId.isNotBlank()) attendanceViewModel.deleteRegisterSession(targetId, reason, session)
    },
    onSignRegister = { role, sig ->
      val targetId = attendanceUiState.selectedSessionId.orEmpty()
      if (targetId.isNotBlank()) attendanceViewModel.signRegister(targetId, role, sig, session)
    },
    onCorrectRegisterTime = { recId, tIn, tOut, r -> attendanceViewModel.correctRecord(recId, tIn, tOut, r, session) },
    onMarkPresent = { studentId, tIn, tOut, reason ->
      val targetId = attendanceUiState.selectedSessionId.orEmpty()
      if (targetId.isNotBlank()) attendanceViewModel.markStudentPresent(targetId, studentId, tIn, tOut, reason, session)
    },
    onUpdateRegisterDetails = { ct, cc, lec, ven, st ->
      val targetId = attendanceUiState.selectedSessionId.orEmpty()
      if (targetId.isNotBlank()) attendanceViewModel.updateRegisterDetails(targetId, ct, cc, lec, ven, st, session)
    },
    onUpdateMyDetails = { fullName, adm, phone, onSaved ->
      attendanceViewModel.updateMyDetails(fullName, adm, phone, session) { savedName, savedAdm ->
        onMemberDetailsSaved(savedName, savedAdm)
        onSaved()
      }
    },
    onSignAttendanceSubmit = { code, sig, name, adm ->
      attendanceViewModel.signAttendance(code, sig, name, adm, session) {
        onMemberDetailsSaved(name, adm)
      }
    },
    onPromptVoidSession = { s -> attendanceViewModel.promptVoidSession(s) },
    onDismissPromptVoidSession = { attendanceViewModel.dismissPromptVoidSession() },
    onPromptDeleteRegister = { reg -> attendanceViewModel.promptDeleteRegister(reg) },
    onDismissPromptDeleteRegister = { attendanceViewModel.dismissPromptDeleteRegister() },
    onConfirmDeleteRegister = { reason ->
      val targetId = attendanceUiState.registerPendingDelete?.sessionId.orEmpty()
      if (targetId.isNotBlank()) attendanceViewModel.deleteRegisterSession(targetId, reason, session)
    },
    onDismissAttendanceError = { attendanceViewModel.clearError() },
    onDismissAttendanceSuccess = { attendanceViewModel.clearSuccess() }
  )

  ClassScreen(
    classItem = selectedClass,
    isStaff = noticesUiState.isStaff,
    prefects = noticesUiState.prefects,
    isPrefect = noticesUiState.isRep,
    session = session,
    notices = noticesUiState.notices,
    readNoticeIds = noticesUiState.readNoticeIds,
    canPostNotice = noticesUiState.canPostNotice,
    isLoading = noticesUiState.isLoading,
    errorMessage = noticesUiState.errorMessage,
    activeNoticeDetail = noticesUiState.activeNoticeDetail,
    noticePendingEdit = noticesUiState.noticePendingEdit,
    noticePendingDelete = noticesUiState.noticePendingDelete,
    onDismissError = { noticesViewModel.clearError() },
    onRefreshNotices = { noticesViewModel.refreshNotices(session) },
    onNoticeClick = { n -> noticesViewModel.openNotice(n, session) },
    onCloseNoticeDetail = { noticesViewModel.closeNoticeDetail() },
    onNavigateToPostNotice = onNavigateToPostNotice,
    onEditNoticeClick = { n -> noticesViewModel.setNoticePendingEdit(n) },
    onDeleteNoticeClick = { n -> noticesViewModel.setNoticePendingDelete(n) },
    onDismissEditNotice = { noticesViewModel.setNoticePendingEdit(null) },
    onSaveEditNotice = { id, t, c, cat, u, p -> noticesViewModel.editNotice(id, t, c, cat, u, p, session) {} },
    onDismissPromptDeleteNotice = { noticesViewModel.setNoticePendingDelete(null) },
    onConfirmDeleteNotice = { noticesViewModel.confirmDeleteNotice(session) },
    onUpdateClassName = { newName -> classesViewModel.updateClassName(selectedClass.id, newName, session) { noticesViewModel.updateSelectedClassName(newName) } },
    onNavigateToManageMembers = { membersViewModel.loadMembers(selectedClass.id, session); onNavigateToManageMembers() },
    attendanceUiState = attendanceUiState,
    attendanceCallbacks = attendanceCallbacks,
    groupsUiState = groupsUiState,
    onRefreshGroups = { groupsViewModel.refreshGroups(session) },
    onCreateGroup = { n, d, s -> groupsViewModel.createGroup(n, d, s, session) {} },
    onOpenChat = { grp -> groupsViewModel.openChat(grp, session); onOpenChat() },
    onJoinGroup = { grp -> groupsViewModel.joinGroup(grp, session) },
    onDismissGroupsError = { groupsViewModel.clearError() },
    timetableUiState = timetableUiState,
    onRefreshTimetable = { timetableViewModel.refreshSchedules(session) },
    onSelectTimetableDay = { d -> timetableViewModel.setSelectedDay(d) },
    onAddScheduleClick = { timetableViewModel.setShowAddDialog(true) },
    onEditScheduleClick = { s -> timetableViewModel.setSchedulePendingEdit(s) },
    onDeleteScheduleClick = { s -> timetableViewModel.setSchedulePendingDelete(s) },
    onStatusScheduleClick = { s -> timetableViewModel.setSchedulePendingStatus(s) },
    onDismissAddEditSchedule = { timetableViewModel.setShowAddDialog(false); timetableViewModel.setSchedulePendingEdit(null) },
    onSaveAddSchedule = { t, d, st, et, r, l -> timetableViewModel.addSchedule(t, d, st, et, r, l, session) {} },
    onSaveEditSchedule = { id, t, d, st, et, r, l -> timetableViewModel.updateSchedule(id, t, d, st, et, r, l, session) {} },
    onDismissPromptDeleteSchedule = { timetableViewModel.setSchedulePendingDelete(null) },
    onConfirmDeleteSchedule = { timetableViewModel.confirmDeleteSchedule(session) },
    onDismissScheduleStatus = { timetableViewModel.setSchedulePendingStatus(null) },
    onSaveScheduleStatus = { id, status, note -> timetableViewModel.updateScheduleStatus(id, status, note, session) },
    onDismissTimetableError = { timetableViewModel.clearError() },
    materialsUiState = materialsUiState,
    onRefreshMaterials = { materialsViewModel.refreshMaterials(session) },
    onMaterialsSearchChange = { q -> materialsViewModel.setSearchQuery(q) },
    onMaterialsSelectTypeFilter = { f -> materialsViewModel.setSelectedTypeFilter(f) },
    onAddMaterialClick = { materialsViewModel.setShowAddDialog(true) },
    onEditMaterialClick = { m -> materialsViewModel.setMaterialPendingEdit(m) },
    onDeleteMaterialClick = { m -> materialsViewModel.setMaterialPendingDelete(m) },
    onDismissAddEditMaterial = { materialsViewModel.setShowAddDialog(false); materialsViewModel.setMaterialPendingEdit(null) },
    onSaveAddMaterial = { t, d, type, u -> materialsViewModel.addLinkMaterial(t, d, type, u, session) {} },
    onSaveUploadMaterial = { t, d, fn, fs, m, streamProvider -> materialsViewModel.uploadFileMaterial(t, d, fn, fs, m, streamProvider, session) {} },
    onSaveEditMaterial = { id, t, d -> materialsViewModel.updateMaterial(id, t, d, session) {} },
    onOpenFileMaterial = { material ->
      materialsViewModel.downloadAndOpenFile(material, context, session) { file, mime ->
        com.example.ui.components.MaterialFileOpener.openOrShareFile(context, file, mime)
      }
    },
    onDownloadMaterial = { material -> materialsViewModel.downloadMaterialForOffline(context, material, session) },
    onRemoveDownloadMaterial = { material -> materialsViewModel.removeDownloadedMaterial(context, material, session) },
    onDismissPromptDeleteMaterial = { materialsViewModel.setMaterialPendingDelete(null) },
    onConfirmDeleteMaterial = { materialsViewModel.confirmDeleteMaterial(session) },
    onDismissMaterialsError = { materialsViewModel.clearError() },
    pollsUiState = pollsUiState,
    onRefreshPolls = { pollsViewModel.refreshPolls(session) },
    onCreatePollClick = { pollsViewModel.setShowCreateDialog(true) },
    onPollClick = { p -> pollsViewModel.openPoll(p, com.example.util.RoleHelper.canSignAttendance(session?.role), session) },
    onSelectPollOption = { optId -> pollsViewModel.selectOptionForVote(optId) },
    onCastVote = { pollsViewModel.castVote(session) },
    onPromptClosePoll = { pollsViewModel.setShowCloseConfirm(true) },
    onDismissClosePoll = { pollsViewModel.setShowCloseConfirm(false) },
    onConfirmClosePoll = { pollsViewModel.closePoll(session) },
    onPromptDeletePoll = { pollsViewModel.setShowDeleteConfirm(true) },
    onDismissDeletePoll = { pollsViewModel.setShowDeleteConfirm(false) },
    onConfirmDeletePoll = { pollsViewModel.deletePoll(session) {} },
    onDismissPollsError = { pollsViewModel.clearError() },
    onClosePollDetail = { pollsViewModel.closePollDetail() },
    onDismissCreatePoll = { pollsViewModel.setShowCreateDialog(false) },
    onCreatePollSubmit = { q, opts, dur -> pollsViewModel.createPoll(q, opts, dur, session) {} },
    allClasses = allClasses,
    isAdmin = com.example.util.RoleHelper.isAdmin(session?.role),
    isLecturer = com.example.util.RoleHelper.isLecturer(session?.role),
    openTimetableSignal = openTimetableSignal,
    onFindMoreClasses = onNavigateToFindClasses,
    onSwitchClass = onSwitchClass,
    onJoinWithCode = onNavigateToJoinClass,
    onNavigateToCreateClass = onNavigateToCreateClass,
    onNavigateToProfile = onNavigateToProfile,
    onBack = onBackToHome
  )
}
