package com.example.admin.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.assistant.AdminAssistantSparkleButton
import com.example.admin.data.AdminDashboardStats
import com.example.admin.data.AdminSession
import com.example.admin.ui.AdminTab
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminMetallicGoldText
import com.example.admin.ui.components.AdminNotificationBellButton
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.components.KpiBentoCard
import com.example.admin.ui.theme.AdminButtonTextStyle
import com.example.admin.ui.theme.AdminCaptionStyle
import com.example.admin.ui.theme.AdminCardTitleStyle
import com.example.admin.ui.theme.AdminGreetingStyle
import com.example.admin.ui.theme.AdminScreenTitleStyle
import com.example.admin.ui.theme.AdminSectionHeadingStyle
import com.example.admin.ui.theme.AmberWarning
import com.example.admin.ui.theme.ChampagneGold
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.InfoBlue
import com.example.admin.ui.theme.Lavender
import com.example.admin.ui.theme.MetallicGold
import com.example.admin.ui.theme.MontserratFamily
import com.example.admin.ui.theme.SilverWhite

@Composable
fun AdminDashboardScreen(
  session: AdminSession,
  stats: AdminDashboardStats,
  unreadNotifications: Int,
  pendingUnitsCount: Int,
  pendingApprovalsCount: Int,
  unverifiedLecturersCount: Int = stats.unverifiedLecturers,
  isLoading: Boolean,
  isOnline: Boolean = true,
  onRefresh: () -> Unit,
  onJumpToUnverified: () -> Unit,
  onNavigateTab: (AdminTab) -> Unit,
  onOpenAssistant: () -> Unit = {},
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
          AdminMetallicGoldText("Control Center", style = AdminScreenTitleStyle, maxLines = 1)
          Text(
            text = "Welcome, ${session.fullName.ifBlank { session.email }}",
            style = AdminGreetingStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          AdminAssistantSparkleButton(isOnline = isOnline, onClick = onOpenAssistant)
          IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp).testTag("dashboard_refresh_btn")) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh dashboard", tint = ChampagneGold)
          }
          AdminNotificationBellButton(unreadCount = unreadNotifications, onClick = onOpenNotifications)
          IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp).testTag("dashboard_settings_btn")) {
            Icon(Icons.Default.Settings, contentDescription = "Account & Setup", tint = ChampagneGold)
          }
        }
      }
      if (isLoading) {
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ChampagneGold)
      }
    }

    if (unverifiedLecturersCount > 0) {
      item {
        AdminGlassCard(borderColor = ChampagneGold.copy(alpha = 0.7f), isHero = true, onClick = onJumpToUnverified) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
              AdminStatusPill(text = "Action Required", color = ChampagneGold)
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "$unverifiedLecturersCount lecturer account(s) waiting verification",
                style = AdminCardTitleStyle
              )
              Text("Tap to review and verify them now", style = AdminCaptionStyle.copy(color = Lavender))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = onJumpToUnverified,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("dashboard_review_lecturers_btn"),
              colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
            ) {
              Text("Review", style = AdminButtonTextStyle.copy(fontSize = 14.sp, color = DarkTextOnGold))
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
        Text("Quick Governance Actions", style = AdminSectionHeadingStyle)
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(
            onClick = { onNavigateTab(AdminTab.APPROVALS) },
            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("dashboard_approvals_btn"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, MetallicGold)
          ) {
            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp), tint = ChampagneGold)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Approvals ($pendingApprovalsCount)", fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = ChampagneGold, maxLines = 1)
          }
          OutlinedButton(
            onClick = { onNavigateTab(AdminTab.RISK) },
            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("dashboard_risk_btn"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, EmeraldSuccess.copy(alpha = 0.7f))
          ) {
            Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp), tint = EmeraldSuccess)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Attendance Risk", fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SilverWhite, maxLines = 1)
          }
        }
      }
    }

    if (isOnline) {
      item {
        AdminGlassCard(
          borderColor = ChampagneGold.copy(alpha = 0.55f),
          onClick = onOpenAssistant,
          modifier = Modifier.testTag("admin_dashboard_assistant_card")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Ask ClassRep Assistant", style = AdminCardTitleStyle.copy(color = ChampagneGold))
              Text("Check unverified lecturers, prefect gaps, system stats & draft notices", style = AdminCaptionStyle.copy(color = Lavender))
            }
          }
        }
      }
    }

    item {
      AdminGlassCard(
        borderColor = (if (stats.rpcSqlInstalled) EmeraldSuccess else AmberWarning).copy(alpha = 0.55f),
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
              style = AdminCaptionStyle.copy(color = Lavender)
            )
          }
        }
      }
    }
  }
}
