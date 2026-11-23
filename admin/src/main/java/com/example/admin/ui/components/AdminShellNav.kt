package com.example.admin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
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
import com.example.admin.ui.theme.AdminNavLabelStyle
import com.example.admin.ui.theme.ChampagneGold
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.ElectricViolet
import com.example.admin.ui.theme.Lavender
import com.example.admin.ui.theme.MetallicGold
import com.example.admin.ui.theme.Midnight
import com.example.admin.ui.theme.MontserratFamily
import com.example.admin.ui.theme.RoyalNavSurface
import com.example.admin.ui.theme.RoyalSurfaceHigh
import com.example.admin.ui.theme.SilverWhite

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
          NavigationRail(containerColor = RoyalNavSurface) {
            railItems.forEach { item ->
              NavigationRailItem(
                selected = activeTab == item.tab,
                onClick = { onSelectTab(item.tab) },
                icon = { NavIconWithBadge(item, if (item.tab == AdminTab.APPROVALS) pendingApprovalsCount else 0) },
                label = { Text(item.label, style = AdminNavLabelStyle, maxLines = 1, softWrap = false) },
                colors = NavigationRailItemDefaults.colors(
                  selectedIconColor = ChampagneGold,
                  selectedTextColor = ChampagneGold,
                  indicatorColor = ElectricViolet.copy(alpha = 0.28f),
                  unselectedIconColor = Lavender.copy(alpha = 0.78f),
                  unselectedTextColor = Lavender.copy(alpha = 0.78f)
                )
              )
            }
          }
          Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MetallicGold.copy(alpha = 0.25f)))
          content(Modifier.weight(1f))
        }
      } else {
        Scaffold(
          containerColor = Color.Transparent,
          contentWindowInsets = WindowInsets.safeDrawing,
          bottomBar = {
            Column {
              Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MetallicGold.copy(alpha = 0.25f)))
              NavigationBar(containerColor = RoyalNavSurface, windowInsets = WindowInsets.navigationBars) {
                phoneItems.forEach { item ->
                  val isSelected = activeTab == item.tab || (item.tab == AdminTab.USERS && activeTab == AdminTab.UNIVERSITY)
                  NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelectTab(item.tab) },
                    icon = { NavIconWithBadge(item, if (item.tab == AdminTab.APPROVALS) pendingApprovalsCount else 0) },
                    label = { Text(item.label, style = AdminNavLabelStyle, maxLines = 1, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(
                      selectedIconColor = ChampagneGold,
                      selectedTextColor = ChampagneGold,
                      indicatorColor = ElectricViolet.copy(alpha = 0.28f),
                      unselectedIconColor = Lavender.copy(alpha = 0.78f),
                      unselectedTextColor = Lavender.copy(alpha = 0.78f)
                    )
                  )
                }
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
                  modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                  border = BorderStroke(1.dp, if (activeTab == AdminTab.USERS) ChampagneGold else MetallicGold.copy(alpha = 0.4f)),
                  label = { Text("Users & Roles", fontFamily = MontserratFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MetallicGold,
                    selectedLabelColor = Midnight,
                    containerColor = RoyalSurfaceHigh,
                    labelColor = Lavender
                  )
                )
                FilterChip(
                  selected = activeTab == AdminTab.UNIVERSITY,
                  onClick = { onSelectTab(AdminTab.UNIVERSITY) },
                  modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                  border = BorderStroke(1.dp, if (activeTab == AdminTab.UNIVERSITY) ChampagneGold else MetallicGold.copy(alpha = 0.4f)),
                  label = { Text("University", fontFamily = MontserratFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MetallicGold,
                    selectedLabelColor = Midnight,
                    containerColor = RoyalSurfaceHigh,
                    labelColor = Lavender
                  )
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
    BadgedBox(badge = { Badge(containerColor = CrimsonError, contentColor = SilverWhite) { Text(if (badgeCount > 99) "99+" else "$badgeCount", fontFamily = MontserratFamily, fontWeight = FontWeight.Bold) } }) {
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
        mainVm.selectTab(AdminTab.CLASSES)
      } else {
        approvalsVm.selectSubTab(ApprovalsSubTab.JOIN_REQUESTS)
        approvalsVm.refresh()
        mainVm.selectTab(AdminTab.APPROVALS)
      }
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
