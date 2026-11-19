package com.example.admin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.notifications.AdminNotificationDeepLink
import com.example.admin.ui.AdminApprovalsViewModel
import com.example.admin.ui.AdminClassMonitorViewModel
import com.example.admin.ui.AdminMainViewModel
import com.example.admin.ui.AdminMonitorExtraViewModel
import com.example.admin.ui.AdminRiskViewModel
import com.example.admin.ui.AdminTab
import com.example.admin.ui.ApprovalsSubTab
import com.example.admin.ui.MonitorTab
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary

private data class NavSpec(val tab: AdminTab, val label: String, val icon: ImageVector)

@Composable
fun AdminShell(
  activeTab: AdminTab,
  pendingApprovalsCount: Int,
  onSelectTab: (AdminTab) -> Unit,
  content: @Composable (Modifier) -> Unit
) {
  val railItems = listOf(
    NavSpec(AdminTab.OVERVIEW, "Overview", Icons.Default.Dashboard),
    NavSpec(AdminTab.CLASSES, "Classes", Icons.Default.Class),
    NavSpec(AdminTab.APPROVALS, "Approvals", Icons.Default.VerifiedUser),
    NavSpec(AdminTab.RISK, "Risk", Icons.Default.Analytics),
    NavSpec(AdminTab.USERS, "Users", Icons.Default.Groups),
    NavSpec(AdminTab.UNIVERSITY, "University", Icons.Default.AccountBalance)
  )
  val phoneItems = listOf(
    NavSpec(AdminTab.OVERVIEW, "Overview", Icons.Default.Dashboard),
    NavSpec(AdminTab.CLASSES, "Classes", Icons.Default.Class),
    NavSpec(AdminTab.APPROVALS, "Approvals", Icons.Default.VerifiedUser),
    NavSpec(AdminTab.RISK, "Risk", Icons.Default.Analytics),
    NavSpec(AdminTab.USERS, "Manage", Icons.Default.Groups)
  )

  AdminBackdrop {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      if (maxWidth >= 600.dp) {
        Row(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
          NavigationRail(containerColor = AdminSurface) {
            railItems.forEach { item ->
              NavigationRailItem(
                selected = activeTab == item.tab,
                onClick = { onSelectTab(item.tab) },
                icon = { NavIconWithBadge(item, if (item.tab == AdminTab.APPROVALS) pendingApprovalsCount else 0) },
                label = { Text(item.label, fontSize = 11.sp) },
                colors = NavigationRailItemDefaults.colors(
                  selectedIconColor = DarkTextOnGold, selectedTextColor = GoldPrimary,
                  indicatorColor = GoldPrimary, unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary
                )
              )
            }
          }
          content(Modifier.weight(1f))
        }
      } else {
        Scaffold(
          containerColor = Color.Transparent,
          contentWindowInsets = WindowInsets.safeDrawing,
          bottomBar = {
            NavigationBar(containerColor = AdminSurface, windowInsets = WindowInsets.navigationBars) {
              phoneItems.forEach { item ->
                val isSelected = activeTab == item.tab || (item.tab == AdminTab.USERS && activeTab == AdminTab.UNIVERSITY)
                NavigationBarItem(
                  selected = isSelected,
                  onClick = { onSelectTab(item.tab) },
                  icon = { NavIconWithBadge(item, if (item.tab == AdminTab.APPROVALS) pendingApprovalsCount else 0) },
                  label = { Text(item.label, fontSize = 11.sp, maxLines = 1) },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DarkTextOnGold, selectedTextColor = GoldPrimary,
                    indicatorColor = GoldPrimary, unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary
                  )
                )
              }
            }
          }
        ) { padding ->
          Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (activeTab == AdminTab.USERS || activeTab == AdminTab.UNIVERSITY) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                FilterChip(
                  selected = activeTab == AdminTab.USERS,
                  onClick = { onSelectTab(AdminTab.USERS) },
                  label = { Text("Users & Roles", fontSize = 12.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPrimary, selectedLabelColor = DarkTextOnGold, containerColor = AdminSurface, labelColor = TextSecondary)
                )
                FilterChip(
                  selected = activeTab == AdminTab.UNIVERSITY,
                  onClick = { onSelectTab(AdminTab.UNIVERSITY) },
                  label = { Text("University", fontSize = 12.sp) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPrimary, selectedLabelColor = DarkTextOnGold, containerColor = AdminSurface, labelColor = TextSecondary)
                )
              }
            }
            content(Modifier.weight(1f))
          }
        }
      }
    }
  }
}

@Composable
private fun NavIconWithBadge(item: NavSpec, badgeCount: Int) {
  if (badgeCount > 0) {
    BadgedBox(badge = { Badge(containerColor = CrimsonError, contentColor = TextPrimary) { Text(if (badgeCount > 99) "99+" else "$badgeCount") } }) {
      Icon(item.icon, contentDescription = item.label)
    }
  } else {
    Icon(item.icon, contentDescription = item.label)
  }
}

fun routeAdminDeepLink(
  link: AdminNotificationDeepLink,
  mainVm: AdminMainViewModel,
  approvalsVm: AdminApprovalsViewModel,
  monitorVm: AdminClassMonitorViewModel,
  extraVm: AdminMonitorExtraViewModel,
  riskVm: AdminRiskViewModel
) {
  when (link.kind) {
    "lecturer_registered" -> {
      approvalsVm.selectSubTab(ApprovalsSubTab.LECTURERS)
      approvalsVm.refresh()
      mainVm.selectTab(AdminTab.APPROVALS)
    }
    "unit_pending" -> {
      approvalsVm.selectSubTab(ApprovalsSubTab.UNITS)
      approvalsVm.refresh()
      mainVm.selectTab(AdminTab.APPROVALS)
    }
    "lecturer_join_request" -> {
      if (link.classId.isNotBlank()) {
        monitorVm.openClass(link.classId, MonitorTab.MEMBERS)
        extraVm.loadForClass(link.classId)
      }
      mainVm.selectTab(AdminTab.CLASSES)
    }
    "attendance_alert", "attendance_warning" -> {
      if (link.classId.isNotBlank()) riskVm.openClassRiskDirectly(link.classId, "")
      else riskVm.refreshAll()
      mainVm.selectTab(AdminTab.RISK)
    }
    else -> {
      if (link.classId.isNotBlank()) {
        monitorVm.openClass(link.classId, MonitorTab.NOTICES)
        extraVm.loadForClass(link.classId)
        mainVm.selectTab(AdminTab.CLASSES)
      }
    }
  }
}
