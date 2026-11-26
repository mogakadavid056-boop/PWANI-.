package com.example.admin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.AdminGlobalJoinRequestItem
import com.example.admin.data.AdminPendingUnitItem
import com.example.admin.data.AdminUserItem
import com.example.admin.notifications.AdminNotificationTimeFormatter
import com.example.admin.ui.AdminApprovalsState
import com.example.admin.ui.ApprovalsSubTab
import com.example.admin.ui.components.AdminConfirmDialog
import com.example.admin.ui.components.AdminEmptyState
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminMetallicGoldText
import com.example.admin.ui.components.AdminNotificationBellButton
import com.example.admin.ui.components.AdminSearchField
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.AdminBorder
import com.example.admin.ui.theme.AdminCaptionStyle
import com.example.admin.ui.theme.AdminCardTitleStyle
import com.example.admin.ui.theme.AdminNavLabelStyle
import com.example.admin.ui.theme.AdminScreenTitleStyle
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.AmberWarning
import com.example.admin.ui.theme.ChampagneGold
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.Lavender
import com.example.admin.ui.theme.MontserratFamily
import com.example.admin.ui.theme.SilverWhite

@Composable
fun AdminApprovalsScreen(
  state: AdminApprovalsState,
  unreadNotifications: Int,
  onSelectSubTab: (ApprovalsSubTab) -> Unit,
  onSearchLecturers: (String) -> Unit,
  onRefresh: () -> Unit,
  onReviewUnit: (AdminPendingUnitItem, Boolean, String?) -> Unit,
  onToggleVerifyLecturer: (AdminUserItem, Boolean) -> Unit,
  onReviewJoinRequest: (AdminGlobalJoinRequestItem, Boolean) -> Unit,
  onOpenNotifications: () -> Unit
) {
  var approveTarget by remember { mutableStateOf<AdminPendingUnitItem?>(null) }
  var rejectTarget by remember { mutableStateOf<AdminPendingUnitItem?>(null) }
  var verifyTarget by remember { mutableStateOf<AdminUserItem?>(null) }
  var joinReviewTarget by remember { mutableStateOf<Pair<AdminGlobalJoinRequestItem, Boolean>?>(null) }

  val pendingLecturersCount = remember(state.lecturers) { state.lecturers.count { !it.isVerified } }
  val filteredLecturers = remember(state.lecturers, state.lecturerSearch) {
    val q = state.lecturerSearch.trim().lowercase()
    if (q.isBlank()) state.lecturers
    else state.lecturers.filter {
      it.fullName.lowercase().contains(q) || it.email.lowercase().contains(q) || it.admissionNumber.lowercase().contains(q)
    }
  }

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp).testTag("admin_approvals_screen")) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        AdminMetallicGoldText(text = "Approvals", style = AdminScreenTitleStyle)
        Text("Manage lecturer access, review units & class join requests", style = AdminCaptionStyle)
      }
      Row {
        IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp).testTag("approvals_refresh_btn")) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh approvals", tint = ChampagneGold)
        }
        AdminNotificationBellButton(unreadCount = unreadNotifications, onClick = onOpenNotifications)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterChip(
        selected = state.subTab == ApprovalsSubTab.UNITS,
        onClick = { onSelectSubTab(ApprovalsSubTab.UNITS) },
        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("approvals_tab_units"),
        label = { Text("Pending Units (${state.pendingUnits.size})", style = AdminNavLabelStyle) },
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = state.subTab == ApprovalsSubTab.UNITS, borderColor = AdminBorder, selectedBorderColor = ChampagneGold),
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = DarkTextOnGold, containerColor = AdminSurface, labelColor = Lavender)
      )
      FilterChip(
        selected = state.subTab == ApprovalsSubTab.LECTURERS,
        onClick = { onSelectSubTab(ApprovalsSubTab.LECTURERS) },
        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("approvals_tab_lecturers"),
        label = { Text("Lecturers ($pendingLecturersCount suspended)", style = AdminNavLabelStyle) },
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = state.subTab == ApprovalsSubTab.LECTURERS, borderColor = AdminBorder, selectedBorderColor = ChampagneGold),
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = DarkTextOnGold, containerColor = AdminSurface, labelColor = Lavender)
      )
      FilterChip(
        selected = state.subTab == ApprovalsSubTab.JOIN_REQUESTS,
        onClick = { onSelectSubTab(ApprovalsSubTab.JOIN_REQUESTS) },
        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("approvals_tab_join_requests"),
        label = { Text("Class Requests (${state.joinRequests.size})", style = AdminNavLabelStyle) },
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = state.subTab == ApprovalsSubTab.JOIN_REQUESTS, borderColor = AdminBorder, selectedBorderColor = ChampagneGold),
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ChampagneGold, selectedLabelColor = DarkTextOnGold, containerColor = AdminSurface, labelColor = Lavender)
      )
    }

    if (state.isLoading) {
      Spacer(modifier = Modifier.height(6.dp))
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = GoldPrimary)
    }

    Spacer(modifier = Modifier.height(10.dp))
    when (state.subTab) {
      ApprovalsSubTab.UNITS -> {
        if (state.pendingUnits.isEmpty() && !state.isLoading) {
          AdminEmptyState(Icons.Default.MenuBook, "No pending units", "New unit requests from lecturers and class reps appear here.")
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(state.pendingUnits, key = { it.courseId }) { unit ->
              AdminGlassCard(borderColor = AmberWarning.copy(alpha = 0.65f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Text("${unit.code} - ${unit.name}", style = AdminCardTitleStyle, modifier = Modifier.weight(1f))
                  AdminStatusPill("PENDING", AmberWarning)
                }
                Spacer(modifier = Modifier.height(4.dp))
                val ts = AdminNotificationTimeFormatter.formatRelative(unit.createdAt)
                Text("${unit.className} • By ${unit.requesterName}${if (ts.isNotBlank()) " • $ts" else ""}", style = AdminCaptionStyle)
                if (unit.description.isNotBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(unit.description, style = AdminCaptionStyle.copy(color = SilverWhite.copy(alpha = 0.85f)))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                  OutlinedButton(
                    onClick = { rejectTarget = unit },
                    enabled = !state.isActionLoading,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CrimsonError),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("approvals_reject_unit_${unit.courseId}")
                  ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = CrimsonError, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject", fontFamily = MontserratFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CrimsonError)
                  }
                  Button(
                    onClick = { approveTarget = unit },
                    enabled = !state.isActionLoading,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("approvals_approve_unit_${unit.courseId}")
                  ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve", fontFamily = MontserratFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                  }
                }
              }
            }
          }
        }
      }

      ApprovalsSubTab.LECTURERS -> {
        AdminSearchField(value = state.lecturerSearch, onValueChange = onSearchLecturers, placeholder = "Search lecturers...", tag = "approvals_lecturer_search")
        Spacer(modifier = Modifier.height(10.dp))
        if (filteredLecturers.isEmpty() && !state.isLoading) {
          AdminEmptyState(Icons.Default.VerifiedUser, "No lecturers found", "Lecturer accounts appear here as soon as they sign up.")
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(filteredLecturers, key = { it.id }) { lec ->
              AdminGlassCard(borderColor = (if (lec.isVerified) EmeraldSuccess else AmberWarning).copy(alpha = 0.55f)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Text(lec.fullName.ifBlank { lec.email }, style = AdminCardTitleStyle, modifier = Modifier.weight(1f, fill = false))
                      AdminStatusPill(if (lec.isVerified) "Active" else "Suspended", if (lec.isVerified) EmeraldSuccess else AmberWarning)
                    }
                    if (lec.email.isNotBlank()) Text(lec.email, style = AdminCaptionStyle)
                    if (lec.admissionNumber.isNotBlank()) Text("Staff No: ${lec.admissionNumber}", style = AdminCaptionStyle)
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  OutlinedButton(
                    onClick = { verifyTarget = lec },
                    enabled = !state.isActionLoading,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (lec.isVerified) AmberWarning else GoldPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("approvals_verify_lecturer_${lec.id}")
                  ) {
                    Text(if (lec.isVerified) "Suspend" else "Restore", fontFamily = MontserratFamily, fontSize = 13.sp, color = if (lec.isVerified) AmberWarning else GoldLight, fontWeight = FontWeight.SemiBold)
                  }
                }
              }
            }
          }
        }
      }

      ApprovalsSubTab.JOIN_REQUESTS -> {
        if (state.joinRequests.isEmpty() && !state.isLoading) {
          AdminEmptyState(Icons.Default.GroupAdd, "No pending class requests", "Requests from lecturers joining classes appear here.")
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(state.joinRequests, key = { "${it.classId}_${it.userId}" }) { req ->
              AdminGlassCard(borderColor = AmberWarning.copy(alpha = 0.65f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(req.fullName.ifBlank { req.email.ifBlank { "Lecturer" } }, style = AdminCardTitleStyle)
                    Text("Class: ${req.className}", style = AdminCaptionStyle.copy(color = ChampagneGold, fontWeight = FontWeight.Medium))
                    val meta = listOf(req.email, req.staffNumber.takeIf { it.isNotBlank() }?.let { "Staff: $it" }.orEmpty())
                      .filter { it.isNotBlank() }
                      .joinToString(" • ")
                    if (meta.isNotBlank()) Text(meta, style = AdminCaptionStyle)
                  }
                  AdminStatusPill("PENDING", AmberWarning)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                  OutlinedButton(
                    onClick = { joinReviewTarget = req to false },
                    enabled = !state.isActionLoading,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CrimsonError),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("approvals_reject_join_${req.userId}")
                  ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = CrimsonError, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject", fontFamily = MontserratFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CrimsonError)
                  }
                  Button(
                    onClick = { joinReviewTarget = req to true },
                    enabled = !state.isActionLoading,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("approvals_approve_join_${req.userId}")
                  ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve", fontFamily = MontserratFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  approveTarget?.let { u ->
    AdminConfirmDialog("Approve Unit?", "Approve ${u.code} - ${u.name} for ${u.className}?", "Approve",
      isActionLoading = state.isActionLoading, onConfirm = { onReviewUnit(u, true, null); approveTarget = null }, onDismiss = { approveTarget = null })
  }
  rejectTarget?.let { u ->
    AdminConfirmDialog("Reject Unit?", "Reject ${u.code} - ${u.name}? You can optionally include a reason.", "Reject",
      isDestructive = true, isActionLoading = state.isActionLoading, optionalReason = true, reasonLabel = "Rejection reason (optional)",
      onConfirm = { r -> onReviewUnit(u, false, r.ifBlank { null }); rejectTarget = null }, onDismiss = { rejectTarget = null })
  }
  verifyTarget?.let { user ->
    AdminVerifyDialog(user = user, isActionLoading = state.isActionLoading, onConfirm = { target -> onToggleVerifyLecturer(user, target); verifyTarget = null }, onDismiss = { verifyTarget = null })
  }
  joinReviewTarget?.let { (req, approve) ->
    AdminConfirmDialog(
      title = if (approve) "Approve Class Join?" else "Reject Class Join?",
      message = if (approve) "Allow ${req.fullName} to join ${req.className} as a lecturer?" else "Reject ${req.fullName}'s request to join ${req.className}?",
      confirmLabel = if (approve) "Approve" else "Reject",
      isDestructive = !approve,
      isActionLoading = state.isActionLoading,
      onConfirm = { onReviewJoinRequest(req, approve); joinReviewTarget = null },
      onDismiss = { joinReviewTarget = null }
    )
  }
}
