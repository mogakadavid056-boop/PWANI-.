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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DiscussionGroupItem
import com.example.data.GroupMemberItem
import com.example.ui.GroupsUiState
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.ErrorBanner
import com.example.ui.components.GroupMemberCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GroupInfoScreen(
  group: DiscussionGroupItem,
  currentUserId: String,
  isStaff: Boolean,
  uiState: GroupsUiState,
  onOpenEditGroup: () -> Unit,
  onDismissEditGroup: () -> Unit,
  onSaveEditGroup: (name: String, desc: String, size: Int) -> Unit,
  onPromptLeaveGroup: () -> Unit,
  onDismissPromptLeaveGroup: () -> Unit,
  onConfirmLeaveGroup: () -> Unit,
  onPromptDeleteGroup: () -> Unit,
  onDismissPromptDeleteGroup: () -> Unit,
  onConfirmDeleteGroup: () -> Unit,
  onPromptRemoveMember: (GroupMemberItem) -> Unit,
  onDismissPromptRemoveMember: () -> Unit,
  onConfirmRemoveMember: () -> Unit,
  onDismissError: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  val canManageGroup = isStaff || group.isOwner || (group.creatorId != null && group.creatorId == currentUserId)

  Box(modifier = Modifier.fillMaxSize().background(DarkBackground).testTag("group_info_screen")) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(38.dp).testTag("group_info_back_button")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text("Group Info", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = GoldLight, modifier = Modifier.weight(1f))
        if (canManageGroup) {
          IconButton(onClick = onOpenEditGroup, modifier = Modifier.size(38.dp).testTag("edit_group_button")) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Group", tint = GoldPrimary)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (!uiState.errorMessage.isNullOrBlank()) {
        ErrorBanner(message = uiState.errorMessage, onDismiss = onDismissError)
        Spacer(modifier = Modifier.height(10.dp))
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(DarkSurface)
          .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
          .padding(16.dp)
      ) {
        Column {
          Text(group.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = if (group.description.isNotBlank()) group.description else "No description provided.",
            fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Capacity: ${group.memberCount} of ${group.maxMembers} members",
            fontSize = 13.sp, color = GoldLight, fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = onPromptLeaveGroup,
          modifier = Modifier.weight(1f).height(42.dp).testTag("leave_group_button"),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Leave", fontSize = 13.sp, color = ErrorRed, fontWeight = FontWeight.SemiBold)
        }

        if (canManageGroup) {
          Button(
            onClick = onPromptDeleteGroup,
            modifier = Modifier.weight(1f).height(42.dp).testTag("delete_group_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White)
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Delete Group", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text("Members (${uiState.groupMembers.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
      Spacer(modifier = Modifier.height(8.dp))

      if (uiState.isLoadingMembers) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = GoldPrimary)
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(uiState.groupMembers, key = { it.userId }) { member ->
            GroupMemberCard(
              member = member,
              canManageGroup = canManageGroup,
              isSelf = member.userId == currentUserId,
              onRemove = { onPromptRemoveMember(member) }
            )
          }
        }
      }
    }

    if (uiState.showEditGroupDialog) {
      EditGroupDialog(group = group, isLoading = uiState.isActionLoading, onDismiss = onDismissEditGroup, onSave = onSaveEditGroup)
    }

    if (uiState.showLeaveGroupConfirm) {
      ConfirmActionDialog(
        title = "Leave Group", message = "Are you sure you want to leave \"${group.name}\"?",
        confirmButtonText = "Leave", confirmButtonColor = ErrorRed, isLoading = uiState.isActionLoading,
        onConfirm = onConfirmLeaveGroup, onDismiss = onDismissPromptLeaveGroup
      )
    }

    if (uiState.showDeleteGroupConfirm) {
      ConfirmActionDialog(
        title = "Delete Group", message = "Are you sure you want to delete \"${group.name}\"? All discussion messages will be permanently lost.",
        confirmButtonText = "Delete", confirmButtonColor = ErrorRed, isLoading = uiState.isActionLoading,
        onConfirm = onConfirmDeleteGroup, onDismiss = onDismissPromptDeleteGroup
      )
    }

    uiState.memberPendingRemoval?.let { target ->
      ConfirmActionDialog(
        title = "Remove Member", message = "Are you sure you want to remove ${target.fullName} from this group?",
        confirmButtonText = "Remove", confirmButtonColor = ErrorRed, isLoading = uiState.isActionLoading,
        onConfirm = onConfirmRemoveMember, onDismiss = onDismissPromptRemoveMember
      )
    }
  }
}
