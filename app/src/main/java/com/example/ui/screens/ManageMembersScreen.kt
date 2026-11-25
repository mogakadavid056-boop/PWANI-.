package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassMemberItem
import com.example.ui.MembersUiState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.draw.shadow
import com.example.ui.components.ClassRepPullToRefresh
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LoadingSkeleton
import com.example.ui.components.MemberCard
import com.example.ui.components.MetallicGoldText
import com.example.ui.components.RoleBadge
import com.example.ui.components.royalOutlinedTextFieldColors
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RoyalSuccessContainer
import com.example.ui.theme.ScreenTitleStyle
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.SilverWhite

@Composable
fun ManageMembersScreen(
  className: String,
  canManageReps: Boolean,
  isAdmin: Boolean = false,
  uiState: MembersUiState,
  onRefresh: () -> Unit,
  onAppointRep: (ClassMemberItem) -> Unit,
  onRemoveRep: (ClassMemberItem) -> Unit,
  onPromptRemoveMember: (ClassMemberItem) -> Unit,
  onDismissPromptRemoveMember: () -> Unit,
  onConfirmRemoveMember: (reason: String?) -> Unit,
  onApproveLecturer: (com.example.data.PendingLecturerItem) -> Unit = {},
  onPromptRejectLecturer: (com.example.data.PendingLecturerItem) -> Unit = {},
  onDismissPromptRejectLecturer: () -> Unit = {},
  onConfirmRejectLecturer: () -> Unit = {},
  onDismissError: () -> Unit,
  onDismissSuccess: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)
  var memberSearch by remember { mutableStateOf("") }
  val filteredMembers = remember(uiState.members, memberSearch) {
    val q = memberSearch.trim()
    if (q.isBlank()) uiState.members
    else uiState.members.filter {
      it.fullName.contains(q, ignoreCase = true) || it.admissionNumber.contains(q, ignoreCase = true)
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .testTag("manage_members_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
      // Top Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.size(48.dp).testTag("manage_members_back_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
          MetallicGoldText("Manage Members", style = ScreenTitleStyle, maxLines = 1)
          Text(className, style = CaptionStyle.copy(color = Lavender))
        }
        IconButton(
          onClick = onRefresh,
          modifier = Modifier.size(48.dp).testTag("refresh_members_button")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ChampagneGold)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (!uiState.errorMessage.isNullOrBlank()) {
        ErrorBanner(message = uiState.errorMessage, onDismiss = onDismissError)
        Spacer(modifier = Modifier.height(12.dp))
      }

      if (!uiState.successMessage.isNullOrBlank()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ButtonRadius))
            .background(RoyalSuccessContainer)
            .border(1.dp, SuccessColor.copy(alpha = 0.65f), RoundedCornerShape(ButtonRadius))
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = SuccessColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(uiState.successMessage, style = CaptionStyle.copy(color = SilverWhite), modifier = Modifier.weight(1f))
            IconButton(onClick = onDismissSuccess, modifier = Modifier.size(48.dp)) {
              Text("✕", color = ChampagneGold, fontSize = 14.sp)
            }
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      if (uiState.pendingLecturers.isNotEmpty()) {
        com.example.ui.components.PendingLecturerRequestsSection(
          pendingLecturers = uiState.pendingLecturers,
          onApprove = onApproveLecturer,
          onReject = onPromptRejectLecturer
        )
        Spacer(modifier = Modifier.height(14.dp))
      }

      // Search box to filter members by name or admission number
      val searchInteraction = remember { MutableInteractionSource() }
      val isSearchFocused by searchInteraction.collectIsFocusedAsState()
      OutlinedTextField(
        value = memberSearch,
        onValueChange = { memberSearch = it },
        interactionSource = searchInteraction,
        placeholder = { Text("Search by name or admission number...", style = CaptionStyle.copy(color = Lavender.copy(alpha = 0.7f))) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = if (isSearchFocused) ChampagneGold else MetallicGold, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .then(if (isSearchFocused) Modifier.shadow(10.dp, RoundedCornerShape(ButtonRadius), ambientColor = ElectricViolet, spotColor = ElectricViolet) else Modifier)
          .testTag("member_search_input"),
        shape = RoundedCornerShape(ButtonRadius),
        colors = royalOutlinedTextFieldColors()
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("Roster (${filteredMembers.size})", style = SectionHeadingStyle.copy(fontSize = 18.sp, color = SilverWhite))
        if (canManageReps) {
          RoleBadge(roleText = "Admin / Lecturer", isGoldFilled = false)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      ClassRepPullToRefresh(
        isRefreshing = uiState.isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
      ) {
        if (uiState.isLoading && uiState.members.isEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LoadingSkeleton(height = 70)
            LoadingSkeleton(height = 70)
            LoadingSkeleton(height = 70)
          }
        } else if (filteredMembers.isEmpty()) {
          EmptyState(
            icon = Icons.Default.People,
            title = if (memberSearch.isNotBlank()) "No Matching Members" else "No Members Found",
            message = if (memberSearch.isNotBlank()) "No members found matching \"$memberSearch\"." else "No members are currently enrolled in this class."
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(filteredMembers, key = { it.userId }) { member ->
              MemberCard(
                member = member,
                canManageReps = canManageReps,
                onAppointRep = { onAppointRep(member) },
                onRemoveRep = { onRemoveRep(member) },
                onRemoveFromClass = { onPromptRemoveMember(member) }
              )
            }
          }
        }
      }
    }

    uiState.memberPendingRemoval?.let { target ->
      if (isAdmin) {
        var removalReason by remember(target.userId) { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
          onDismissRequest = { if (!uiState.isActionLoading) onDismissPromptRemoveMember() },
          shape = RoundedCornerShape(16.dp),
          containerColor = com.example.ui.theme.DarkSurface,
          title = {
            Text("Remove from Class", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ChampagneGold)
          },
          text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text(
                text = "Remove ${target.fullName} from this class? They will lose access to class notices and attendance.",
                fontSize = 13.sp,
                color = Lavender
              )
              OutlinedTextField(
                value = removalReason,
                onValueChange = { removalReason = it },
                label = { Text("Reason (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().testTag("admin_remove_member_reason_input"),
                colors = royalOutlinedTextFieldColors()
              )
            }
          },
          confirmButton = {
            androidx.compose.material3.Button(
              onClick = { onConfirmRemoveMember(removalReason.trim().ifBlank { null }) },
              enabled = !uiState.isActionLoading,
              modifier = Modifier.testTag("confirm_dialog_action_button"),
              colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = androidx.compose.ui.graphics.Color.White)
            ) {
              Text("Remove", fontWeight = FontWeight.Bold)
            }
          },
          dismissButton = {
            androidx.compose.material3.TextButton(
              onClick = onDismissPromptRemoveMember,
              enabled = !uiState.isActionLoading,
              modifier = Modifier.testTag("confirm_dialog_cancel_button")
            ) {
              Text("Cancel", color = Lavender)
            }
          },
          modifier = Modifier.testTag("confirm_action_dialog")
        )
      } else {
        ConfirmActionDialog(
          title = "Remove from Class",
          message = "Are you sure you want to remove ${target.fullName} from this class? They will lose access to all class notices and attendance.",
          confirmButtonText = "Remove",
          confirmButtonColor = ErrorRed,
          isLoading = uiState.isActionLoading,
          onConfirm = { onConfirmRemoveMember(null) },
          onDismiss = onDismissPromptRemoveMember
        )
      }
    }

    uiState.lecturerPendingRejection?.let { lecturer ->
      ConfirmActionDialog(
        title = "Reject Lecturer Request",
        message = "Are you sure you want to reject the lecturer request from ${lecturer.fullName} (${lecturer.email})?",
        confirmButtonText = "Reject",
        confirmButtonColor = ErrorRed,
        isLoading = uiState.isActionLoading,
        onConfirm = onConfirmRejectLecturer,
        onDismiss = onDismissPromptRejectLecturer
      )
    }
  }
}
