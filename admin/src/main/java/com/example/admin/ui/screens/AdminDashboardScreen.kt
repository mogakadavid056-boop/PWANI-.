package com.example.admin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.AdminDashboardStats
import com.example.admin.data.AdminSession
import com.example.admin.ui.AdminTab
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminNotificationBellButton
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.components.KpiBentoCard
import com.example.admin.ui.theme.AmberWarning
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.InfoBlue
import com.example.admin.ui.theme.TextMuted
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary

@Composable
fun AdminDashboardScreen(
  session: AdminSession,
  stats: AdminDashboardStats,
  unreadNotifications: Int,
  pendingUnitsCount: Int,
  pendingApprovalsCount: Int,
  unverifiedLecturersCount: Int = stats.unverifiedLecturers,
  isLoading: Boolean,
  onRefresh: () -> Unit,
  onJumpToUnverified: () -> Unit,
  onNavigateTab: (AdminTab) -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenSettings: () -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("admin_dashboard_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Control Center", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
          Text(
            text = "Welcome, ${session.fullName.ifBlank { session.email }}",
            fontSize = 13.sp,
            color = GoldLight
          )
        }
        Row {
          IconButton(onClick = onRefresh, modifier = Modifier.testTag("dashboard_refresh_btn")) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh dashboard", tint = GoldPrimary)
          }
          AdminNotificationBellButton(unreadCount = unreadNotifications, onClick = onOpenNotifications)
          IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("dashboard_settings_btn")) {
            Icon(Icons.Default.Settings, contentDescription = "Account & Setup", tint = GoldLight)
          }
        }
      }
      if (isLoading) {
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = GoldPrimary)
      }
    }

    if (unverifiedLecturersCount > 0) {
      item {
        AdminGlassCard(borderColor = AmberWarning.copy(alpha = 0.7f), onClick = onJumpToUnverified) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
              AdminStatusPill(text = "Action Required", color = AmberWarning)
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "$unverifiedLecturersCount lecturer account(s) waiting verification",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
              )
              Text("Tap to review and verify them now", fontSize = 12.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = onJumpToUnverified,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
              modifier = Modifier.testTag("dashboard_review_lecturers_btn")
            ) {
              Text("Review", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiBentoCard(
          title = "Total Users",
          value = stats.totalUsers.toString(),
          subtitle = "${stats.totalStudents} students • ${stats.totalClassReps} reps",
          icon = Icons.Default.Groups,
          accentColor = GoldPrimary,
          modifier = Modifier.weight(1f),
          onClick = { onNavigateTab(AdminTab.USERS) }
        )
        KpiBentoCard(
          title = "Lecturers",
          value = stats.totalLecturers.toString(),
          subtitle = "$unverifiedLecturersCount unverified • ${stats.totalAdmins} admins",
          icon = Icons.Default.VerifiedUser,
          accentColor = if (unverifiedLecturersCount > 0) AmberWarning else EmeraldSuccess,
          modifier = Modifier.weight(1f),
          onClick = onJumpToUnverified
        )
      }
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiBentoCard(
          title = "University",
          value = stats.totalSchools.toString(),
          subtitle = "${stats.totalProgrammes} programmes",
          icon = Icons.Default.AccountBalance,
          accentColor = InfoBlue,
          modifier = Modifier.weight(1f),
          onClick = { onNavigateTab(AdminTab.UNIVERSITY) }
        )
        KpiBentoCard(
          title = "Classes",
          value = stats.totalClasses.toString(),
          subtitle = if (pendingUnitsCount > 0) "$pendingUnitsCount pending unit(s)" else if (stats.rpcSqlInstalled) "${stats.openRegisters} open registers" else "Tap to monitor",
          icon = Icons.Default.AssignmentTurnedIn,
          accentColor = EmeraldSuccess,
          modifier = Modifier.weight(1f),
          onClick = { onNavigateTab(AdminTab.CLASSES) }
        )
      }
    }

    item {
      AdminGlassCard {
        Text("Quick Governance Actions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldLight)
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(
            onClick = { onNavigateTab(AdminTab.APPROVALS) },
            modifier = Modifier.weight(1f).height(48.dp).testTag("dashboard_approvals_btn"),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp), tint = GoldPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Approvals ($pendingApprovalsCount)", fontSize = 12.sp, color = TextPrimary)
          }
          OutlinedButton(
            onClick = { onNavigateTab(AdminTab.RISK) },
            modifier = Modifier.weight(1f).height(48.dp).testTag("dashboard_risk_btn"),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp), tint = EmeraldSuccess)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Attendance Risk", fontSize = 12.sp, color = TextPrimary)
          }
        }
      }
    }

    item {
      AdminGlassCard(
        borderColor = (if (stats.rpcSqlInstalled) EmeraldSuccess else AmberWarning).copy(alpha = 0.5f),
        onClick = onOpenSettings
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
          Column(modifier = Modifier.weight(1f)) {
            AdminStatusPill(
              text = if (stats.rpcSqlInstalled) "Supabase Admin RPCs Active" else "Optional SQL Setup Available",
              color = if (stats.rpcSqlInstalled) EmeraldSuccess else AmberWarning
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (stats.rpcSqlInstalled) {
                "Connected to the same ClassRep Supabase database with full admin RPCs."
              } else {
                "Basic user management works now. Tap here to copy the SQL script for full Schools & Class Oversight."
              },
              fontSize = 12.sp,
              color = TextMuted
            )
          }
        }
      }
    }
  }
}
