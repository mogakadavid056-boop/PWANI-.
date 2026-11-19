package com.example.admin.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.AdminGroupItem
import com.example.admin.data.AdminGroupMember
import com.example.admin.data.AdminGroupMessage
import com.example.admin.notifications.AdminNotificationTimeFormatter
import com.example.admin.ui.AdminMonitorExtraState
import com.example.admin.ui.AdminMonitorExtraViewModel
import com.example.admin.ui.components.AdminConfirmDialog
import com.example.admin.ui.components.AdminEmptyState
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.InfoBlue
import com.example.admin.ui.theme.TextMuted
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary
import com.example.admin.util.AdminJsonHelper

@Composable
fun AdminMonitorGroupsTab(
  extraState: AdminMonitorExtraState,
  extraVm: AdminMonitorExtraViewModel
) {
  var deleteGroupTarget by remember { mutableStateOf<AdminGroupItem?>(null) }
  var deleteMsgTarget by remember { mutableStateOf<AdminGroupMessage?>(null) }
  var removeMemTarget by remember { mutableStateOf<AdminGroupMember?>(null) }
  val selectedGroup = extraState.selectedGroup
  var showMembersSub by remember(selectedGroup?.id) { mutableStateOf(false) }
  if (selectedGroup != null) {
    BackHandler(onBack = extraVm::closeGroupDetail)
    Column(modifier = Modifier.fillMaxSize()) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = extraVm::closeGroupDetail) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldPrimary)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(selectedGroup.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
          Text("${selectedGroup.memberCount}/${selectedGroup.maxMembers} members", fontSize = 12.sp, color = TextMuted)
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = !showMembersSub, onClick = { showMembersSub = false },
          label = { Text("Messages (${extraState.groupMessages.size})", fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPrimary, selectedLabelColor = DarkTextOnGold, containerColor = AdminSurface, labelColor = TextSecondary)
        )
        FilterChip(
          selected = showMembersSub, onClick = { showMembersSub = true },
          label = { Text("Members (${extraState.groupMembers.size})", fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPrimary, selectedLabelColor = DarkTextOnGold, containerColor = AdminSurface, labelColor = TextSecondary)
        )
      }
      Spacer(modifier = Modifier.height(8.dp))

      if (!showMembersSub) {
        if (extraState.groupMessages.isEmpty()) {
          AdminEmptyState(Icons.Default.Forum, "No messages yet", "Study group chat messages appear here for moderation.")
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(extraState.groupMessages, key = { it.id }) { m ->
              AdminGlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(m.senderName.ifBlank { "Student" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                    Text(m.messageText, fontSize = 13.sp, color = TextPrimary)
                    val ts = AdminNotificationTimeFormatter.formatRelative(m.createdAt)
                    if (ts.isNotBlank()) Text(ts, fontSize = 11.sp, color = TextMuted)
                  }
                  IconButton(onClick = { deleteMsgTarget = m }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete message", tint = CrimsonError, modifier = Modifier.size(18.dp))
                  }
                }
              }
            }
          }
        }
      } else {
        if (extraState.groupMembers.isEmpty()) {
          AdminEmptyState(Icons.Default.Groups, "No members", "Group member list is empty.")
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(extraState.groupMembers, key = { it.userId }) { mem ->
              AdminGlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                      Text(mem.fullName.ifBlank { "Member" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                      if (mem.isOwner) AdminStatusPill("OWNER", GoldPrimary)
                    }
                    Text("Adm: ${AdminJsonHelper.orDash(mem.admissionNumber)}", fontSize = 12.sp, color = TextMuted)
                  }
                  IconButton(onClick = { removeMemTarget = mem }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove from group", tint = CrimsonError, modifier = Modifier.size(18.dp))
                  }
                }
              }
            }
          }
        }
      }
    }

    deleteMsgTarget?.let { m ->
      AdminConfirmDialog("Delete Message?", "Remove this chat message permanently?", "Delete", isDestructive = true,
        isActionLoading = extraState.isActionLoading, onConfirm = { extraVm.deleteGroupMessage(m.id); deleteMsgTarget = null }, onDismiss = { deleteMsgTarget = null })
    }
    removeMemTarget?.let { mem ->
      AdminConfirmDialog("Remove Member?", "Remove ${mem.fullName} from ${selectedGroup.name}?", "Remove", isDestructive = true,
        isActionLoading = extraState.isActionLoading, onConfirm = { extraVm.removeGroupMember(selectedGroup.id, mem.userId); removeMemTarget = null }, onDismiss = { removeMemTarget = null })
    }
    return
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Text("Study Groups (${extraState.groups.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldLight)
    Spacer(modifier = Modifier.height(8.dp))
    if (extraState.groups.isEmpty()) {
      AdminEmptyState(Icons.Default.Groups, "No study groups", "Student-created study groups in this class appear here.")
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        items(extraState.groups, key = { it.id }) { g ->
          AdminGlassCard(onClick = { extraVm.openGroup(g) }) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text(g.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                if (g.description.isNotBlank()) Text(g.description, fontSize = 12.sp, color = TextSecondary, maxLines = 2)
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                AdminStatusPill("${g.memberCount}/${g.maxMembers}", InfoBlue)
                IconButton(onClick = { deleteGroupTarget = g }) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete group", tint = CrimsonError, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      }
    }
  }

  deleteGroupTarget?.let { g ->
    AdminConfirmDialog("Delete Study Group?", "Delete \"${g.name}\" and all its chat history?", "Delete", isDestructive = true,
      isActionLoading = extraState.isActionLoading, onConfirm = { extraVm.deleteGroup(g.id); deleteGroupTarget = null }, onDismiss = { deleteGroupTarget = null })
  }
}
