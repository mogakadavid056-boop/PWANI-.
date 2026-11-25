package com.example.admin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.example.admin.data.AdminClassMemberItem
import com.example.admin.data.AdminPendingLecturerItem
import com.example.admin.ui.components.AdminConfirmDialog
import com.example.admin.ui.components.AdminEmptyState
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminSearchField
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.AmberWarning
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.InfoBlue
import com.example.admin.ui.theme.TextMuted
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary
import com.example.admin.util.AdminJsonHelper

@Composable
fun AdminMonitorMembersTab(
  members: List<AdminClassMemberItem>,
  pendingLecturers: List<AdminPendingLecturerItem>,
  search: String,
  isActionLoading: Boolean,
  onSearchChanged: (String) -> Unit,
  onReviewJoinRequest: (String, Boolean) -> Unit,
  onTogglePrefect: (String, Boolean) -> Unit,
  onRemoveMember: (String, String?) -> Unit
) {
  var removeTarget by remember { mutableStateOf<AdminClassMemberItem?>(null) }

  val filtered = remember(members, search) {
    val q = search.trim().lowercase()
    if (q.isBlank()) members
    else members.filter {
      it.fullName.lowercase().contains(q) || it.admissionNumber.lowercase().contains(q) || it.role.lowercase().contains(q)
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    AdminSearchField(value = search, onValueChange = onSearchChanged, placeholder = "Search members...", tag = "monitor_member_search")
    Spacer(modifier = Modifier.height(10.dp))

    if (filtered.isEmpty() && pendingLecturers.isEmpty()) {
      AdminEmptyState(Icons.Default.People, "No members found", "Try another search query or refresh.")
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        if (pendingLecturers.isNotEmpty()) {
          item {
            Text("Pending Lecturer Requests (${pendingLecturers.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
          }
          items(pendingLecturers, key = { "pending_${it.userId}" }) { req ->
            AdminGlassCard(borderColor = AmberWarning.copy(alpha = 0.7f)) {
              Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(req.fullName.ifBlank { "Lecturer" }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                  if (req.email.isNotBlank()) Text(req.email, fontSize = 12.sp, color = TextSecondary)
                  if (req.staffNumber.isNotBlank()) Text("Staff: ${req.staffNumber}", fontSize = 11.sp, color = TextMuted)
                }
                Button(
                  onClick = { onReviewJoinRequest(req.userId, true) },
                  enabled = !isActionLoading,
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
                ) {
                  Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("Approve", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { onReviewJoinRequest(req.userId, false) }, enabled = !isActionLoading) {
                  Icon(Icons.Default.Close, contentDescription = "Reject request", tint = CrimsonError)
                }
              }
            }
          }
        }

        items(filtered, key = { it.userId }) { m ->
          val isLec = m.memberRole.equals("lecturer", true) || m.role.equals("lecturer", true)
          val roleColor = when {
            isLec -> InfoBlue
            m.isRep -> EmeraldSuccess
            else -> TextSecondary
          }
          val roleLabel = when {
            isLec -> "Lecturer"
            m.isRep -> "Prefect"
            else -> "Student"
          }
          AdminGlassCard {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(m.fullName.ifBlank { "Member" }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                  AdminStatusPill(roleLabel, roleColor)
                }
                Text("Adm/Staff: ${AdminJsonHelper.orDash(m.admissionNumber)}", fontSize = 12.sp, color = TextMuted)
              }
              if (!isLec) {
                OutlinedButton(
                  onClick = { onTogglePrefect(m.userId, !m.isRep) },
                  enabled = !isActionLoading,
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, if (m.isRep) AmberWarning else GoldPrimary),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = if (m.isRep) "Remove Prefect" else "Make Prefect",
                    fontSize = 11.sp,
                    color = if (m.isRep) AmberWarning else GoldLight
                  )
                }
              }
              IconButton(onClick = { removeTarget = m }, enabled = !isActionLoading) {
                Icon(Icons.Default.Delete, contentDescription = "Remove member", tint = CrimsonError, modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    }
  }

  removeTarget?.let { m ->
    AdminConfirmDialog(
      title = "Remove from Class?",
      message = "Remove ${m.fullName} from this class? They will receive a notification.",
      confirmLabel = "Remove",
      isDestructive = true,
      isActionLoading = isActionLoading,
      optionalReason = true,
      reasonLabel = "Reason (optional)",
      onConfirm = { reason -> onRemoveMember(m.userId, reason.ifBlank { null }); removeTarget = null },
      onDismiss = { removeTarget = null }
    )
  }
}
