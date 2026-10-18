package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.data.UserSession
import com.example.ui.AuthViewModel
import com.example.ui.GroupsUiState
import com.example.ui.GroupsViewModel
import com.example.ui.MembersUiState
import com.example.ui.MembersViewModel
import com.example.ui.NoticesUiState
import com.example.ui.NoticesViewModel
import com.example.ui.Screen

@Composable
fun ClassSubScreensRoute(
  currentScreen: Screen,
  session: UserSession?,
  noticesUiState: NoticesUiState,
  membersUiState: MembersUiState,
  groupsUiState: GroupsUiState,
  authViewModel: AuthViewModel,
  noticesViewModel: NoticesViewModel,
  membersViewModel: MembersViewModel,
  groupsViewModel: GroupsViewModel
) {
  when (currentScreen) {
    Screen.PostNotice -> {
      val selectedClass = noticesUiState.selectedClass
      if (selectedClass == null || !noticesUiState.canPostNotice) {
        LaunchedEffect(Unit) { authViewModel.navigateTo(Screen.Home) }
      } else {
        PostNoticeScreen(
          className = selectedClass.name,
          isLoading = noticesUiState.isActionLoading,
          errorMessage = noticesUiState.errorMessage,
          onDismissError = { noticesViewModel.clearError() },
          onSubmit = { t, c, cat, u, p ->
            noticesViewModel.postNotice(t, c, cat, u, p, session) { authViewModel.navigateTo(Screen.ClassDetail) }
          },
          onBack = { noticesViewModel.clearError(); authViewModel.navigateTo(Screen.ClassDetail) }
        )
      }
    }
    Screen.ManageMembers -> {
      val selectedClass = noticesUiState.selectedClass
      val canManageReps = session?.role?.equals("admin", ignoreCase = true) == true ||
        session?.role?.equals("lecturer", ignoreCase = true) == true
      if (selectedClass == null || !noticesUiState.isStaff) {
        LaunchedEffect(Unit) { authViewModel.navigateTo(Screen.Home) }
      } else {
        ManageMembersScreen(
          className = selectedClass.name,
          canManageReps = canManageReps,
          uiState = membersUiState,
          onRefresh = { membersViewModel.refreshMembers(session) },
          onAppointRep = { m -> membersViewModel.appointRep(m, session) },
          onRemoveRep = { m -> membersViewModel.removeRep(m, session) },
          onPromptRemoveMember = { m -> membersViewModel.promptRemoveMember(m) },
          onDismissPromptRemoveMember = { membersViewModel.dismissPromptRemoveMember() },
          onConfirmRemoveMember = { membersViewModel.confirmRemoveMember(session) },
          onApproveLecturer = { l -> membersViewModel.approveLecturer(l, session) },
          onPromptRejectLecturer = { l -> membersViewModel.promptRejectLecturer(l) },
          onDismissPromptRejectLecturer = { membersViewModel.dismissPromptRejectLecturer() },
          onConfirmRejectLecturer = { membersViewModel.confirmRejectLecturer(session) },
          onDismissError = { membersViewModel.clearError() },
          onDismissSuccess = { membersViewModel.clearSuccess() },
          onBack = { authViewModel.navigateTo(Screen.ClassDetail) }
        )
      }
    }
    Screen.GroupChat -> {
      val grp = groupsUiState.activeGroup
      if (grp == null) {
        LaunchedEffect(Unit) { authViewModel.navigateTo(Screen.ClassDetail) }
      } else {
        GroupChatScreen(
          group = grp,
          currentUserId = session?.userId.orEmpty(),
          isStaff = noticesUiState.isStaff,
          uiState = groupsUiState,
          onSendMessage = { t -> groupsViewModel.sendMessage(t, session) },
          onPromptDeleteMessage = { m -> groupsViewModel.promptDeleteMessage(m) },
          onDismissPromptDeleteMessage = { groupsViewModel.dismissPromptDeleteMessage() },
          onConfirmDeleteMessage = { groupsViewModel.confirmDeleteMessage(session) },
          onDismissError = { groupsViewModel.clearError() },
          onOpenInfo = { groupsViewModel.loadGroupMembers(session); authViewModel.navigateTo(Screen.GroupInfo) },
          onBack = { groupsViewModel.closeChat(); authViewModel.navigateTo(Screen.ClassDetail) }
        )
      }
    }
    Screen.GroupInfo -> {
      val grp = groupsUiState.activeGroup
      if (grp == null) {
        LaunchedEffect(Unit) { authViewModel.navigateTo(Screen.ClassDetail) }
      } else {
        GroupInfoScreen(
          group = grp,
          currentUserId = session?.userId.orEmpty(),
          isStaff = noticesUiState.isStaff,
          uiState = groupsUiState,
          onOpenEditGroup = { groupsViewModel.setShowEditGroupDialog(true) },
          onDismissEditGroup = { groupsViewModel.setShowEditGroupDialog(false) },
          onSaveEditGroup = { n, d, s -> groupsViewModel.updateGroupDetails(n, d, s, session) },
          onPromptLeaveGroup = { groupsViewModel.setShowLeaveGroupConfirm(true) },
          onDismissPromptLeaveGroup = { groupsViewModel.setShowLeaveGroupConfirm(false) },
          onConfirmLeaveGroup = { groupsViewModel.leaveGroup(session) { authViewModel.navigateTo(Screen.ClassDetail) } },
          onPromptDeleteGroup = { groupsViewModel.setShowDeleteGroupConfirm(true) },
          onDismissPromptDeleteGroup = { groupsViewModel.setShowDeleteGroupConfirm(false) },
          onConfirmDeleteGroup = { groupsViewModel.deleteGroup(session) { authViewModel.navigateTo(Screen.ClassDetail) } },
          onPromptRemoveMember = { m -> groupsViewModel.setMemberPendingRemoval(m) },
          onDismissPromptRemoveMember = { groupsViewModel.setMemberPendingRemoval(null) },
          onConfirmRemoveMember = { groupsViewModel.removeGroupMember(session) },
          onDismissError = { groupsViewModel.clearError() },
          onBack = { authViewModel.navigateTo(Screen.GroupChat) }
        )
      }
    }
    else -> {}
  }
}
