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
import com.example.ui.components.ClassRepPullToRefresh
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LoadingSkeleton
import com.example.ui.components.MemberCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ManageMembersScreen(
  className: String,
  canManageReps: Boolean,
  uiState: MembersUiState,
  onRefresh: () -> Unit,
  onAppointRep: (ClassMemberItem) -> Unit,
  onRemoveRep: (ClassMemberItem) -> Unit,
  onPromptRemoveMember: (ClassMemberItem) -> Unit,
  onDismissPromptRemoveMember: () -> Unit,
  onConfirmRemoveMember: () -> Unit,
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
      .background(DarkBackground)
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
          modifier = Modifier.size(40.dp).testTag("manage_members_back_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text("Manage Members", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GoldLight)
          Text(className, fontSize = 12.sp, color = TextSecondary)
        }
        IconButton(
          onClick = onRefresh,
          modifier = Modifier.size(36.dp).testTag("refresh_members_button")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GoldLight)
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
            .clip(RoundedCornerShape(10.dp))
            .background(GoldPrimary.copy(alpha = 0.15f))
            .border(1.dp, GoldPrimary, RoundedCornerShape(10.dp))
            .padding(12.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(uiState.successMessage, fontSize = 13.sp, color = GoldLight, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismissSuccess, modifier = Modifier.size(24.dp)) {
              Text("✕", color = GoldLight, fontSize = 12.sp)
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
      OutlinedTextField(
        value = memberSearch,
        onValueChange = { memberSearch = it },
        placeholder = { Text("Search by name or admission number...", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("member_search_input"),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = GoldPrimary,
          unfocusedBorderColor = DarkBorder,
          focusedContainerColor = DarkSurface,
          unfocusedContainerColor = DarkSurface,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("Roster (${filteredMembers.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        if (canManageReps) {
          Text("Admin / Lecturer Mode", fontSize = 11.sp, color = GoldLight, fontWeight = FontWeight.SemiBold)
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
      ConfirmActionDialog(
        title = "Remove from Class",
        message = "Are you sure you want to remove ${target.fullName} from this class? They will lose access to all class notices and attendance.",
        confirmButtonText = "Remove",
        confirmButtonColor = ErrorRed,
        isLoading = uiState.isActionLoading,
        onConfirm = onConfirmRemoveMember,
        onDismiss = onDismissPromptRemoveMember
      )
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
