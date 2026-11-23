package com.example.admin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stars
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.assistant.AdminAssistantSparkleButton
import com.example.admin.data.AdminUserItem
import com.example.admin.ui.UserFilterChip
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
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.InfoBlue
import com.example.admin.ui.theme.Lavender
import com.example.admin.ui.theme.MontserratFamily
import com.example.admin.ui.theme.TextSecondary
import com.example.admin.util.AdminJsonHelper

@Composable
fun AdminUsersScreen(
  users: List<AdminUserItem>,
  searchQuery: String,
  activeFilter: UserFilterChip,
  unreadNotifications: Int,
  isOnline: Boolean = true,
  isLoading: Boolean,
  isActionLoading: Boolean,
  onSearchChanged: (String) -> Unit,
  onFilterChanged: (UserFilterChip) -> Unit,
  onRefresh: () -> Unit,
  onAssignRole: (AdminUserItem, String) -> Unit,
  onToggleVerify: (AdminUserItem, Boolean) -> Unit,
  onOpenAssistant: () -> Unit = {},
  onOpenNotifications: () -> Unit
) {
  var roleTarget by remember { mutableStateOf<AdminUserItem?>(null) }
  var verifyTarget by remember { mutableStateOf<AdminUserItem?>(null) }

  val filtered = remember(users, activeFilter) {
    when (activeFilter) {
      UserFilterChip.ALL -> users
      UserFilterChip.UNVERIFIED -> users.filter { it.isLecturer && !it.isVerified }
      UserFilterChip.LECTURERS -> users.filter { it.isLecturer }
      UserFilterChip.CLASS_REPS -> users.filter { it.isClassRep }
      UserFilterChip.STUDENTS -> users.filter { it.isStudent }
      UserFilterChip.ADMINS -> users.filter { it.isAdmin }
    }
  }

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp).testTag("admin_users_screen")) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        AdminMetallicGoldText(text = "Users & Roles", style = AdminScreenTitleStyle)
        Text("${filtered.size} shown • Tap user to change role", style = AdminCaptionStyle)
      }
      Row {
        AdminAssistantSparkleButton(isOnline = isOnline, onClick = onOpenAssistant)
        IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp)) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh users", tint = ChampagneGold)
        }
        AdminNotificationBellButton(unreadCount = unreadNotifications, onClick = onOpenNotifications)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    AdminSearchField(
      value = searchQuery,
      onValueChange = onSearchChanged,
      placeholder = "Search name, email, or admission no...",
      tag = "admin_users_search"
    )

    Spacer(modifier = Modifier.height(10.dp))
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      UserFilterChip.entries.forEach { chip ->
        FilterChip(
          selected = activeFilter == chip,
          onClick = { onFilterChanged(chip) },
          modifier = Modifier.defaultMinSize(minHeight = 48.dp),
          label = { Text(chip.label, style = AdminNavLabelStyle) },
          border = FilterChipDefaults.filterChipBorder(enabled = true, selected = activeFilter == chip, borderColor = AdminBorder, selectedBorderColor = ChampagneGold),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ChampagneGold,
            selectedLabelColor = DarkTextOnGold,
            containerColor = AdminSurface,
            labelColor = Lavender
          )
        )
      }
    }

    if (isLoading) {
      Spacer(modifier = Modifier.height(6.dp))
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = GoldPrimary)
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (filtered.isEmpty() && !isLoading) {
      AdminEmptyState(
        icon = Icons.Default.People,
        title = "No matching users",
        subtitle = "Try another filter or pull/tap refresh."
      )
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        items(filtered, key = { it.id }) { user ->
          AdminUserCard(
            user = user,
            onClick = { roleTarget = user },
            onVerifyClick = { verifyTarget = user }
          )
        }
      }
    }
  }

  roleTarget?.let { user ->
    AdminAssignRoleDialog(
      user = user,
      isActionLoading = isActionLoading,
      onConfirm = { newRole ->
        onAssignRole(user, newRole)
        roleTarget = null
      },
      onDismiss = { roleTarget = null }
    )
  }

  verifyTarget?.let { user ->
    AdminVerifyDialog(
      user = user,
      isActionLoading = isActionLoading,
      onConfirm = { target ->
        onToggleVerify(user, target)
        verifyTarget = null
      },
      onDismiss = { verifyTarget = null }
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminUserCard(
  user: AdminUserItem,
  onClick: () -> Unit,
  onVerifyClick: () -> Unit
) {
  val accent = when {
    user.isAdmin -> GoldPrimary
    user.isLecturer -> GoldLight
    user.isClassRep -> InfoBlue
    else -> TextSecondary
  }
  val icon = when {
    user.isAdmin -> Icons.Default.Security
    user.isLecturer -> Icons.Default.School
    user.isClassRep -> Icons.Default.Stars
    else -> Icons.Default.Person
  }

  AdminGlassCard(onClick = onClick) {
    Row(verticalAlignment = Alignment.Top) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(AdminSurface)
          .border(1.dp, accent.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(21.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(user.fullName.ifBlank { "Unnamed User" }, style = AdminCardTitleStyle)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          AdminStatusPill(text = user.displayRole, color = accent)
          if (user.isLecturer) {
            AdminStatusPill(
              text = if (user.isVerified) "Verified" else "Not verified",
              color = if (user.isVerified) EmeraldSuccess else AmberWarning
            )
          }
        }
        if (user.email.isNotBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Email, contentDescription = null, tint = Lavender, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(user.email, style = AdminCaptionStyle)
          }
        }
        if (user.admissionNumber.isNotBlank()) {
          Spacer(modifier = Modifier.height(2.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Badge, contentDescription = null, tint = Lavender, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Adm: ${AdminJsonHelper.orDash(user.admissionNumber)}", style = AdminCaptionStyle)
          }
        }
        if (user.isLecturer) {
          Spacer(modifier = Modifier.height(10.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(
              onClick = onVerifyClick,
              modifier = Modifier.defaultMinSize(minHeight = 48.dp),
              shape = RoundedCornerShape(14.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
              border = BorderStroke(1.dp, if (user.isVerified) AmberWarning else GoldPrimary)
            ) {
              Text(
                text = if (user.isVerified) "Remove Verification" else "Verify Lecturer",
                fontFamily = MontserratFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (user.isVerified) AmberWarning else ChampagneGold
              )
            }
          }
        }
      }
    }
  }
}
