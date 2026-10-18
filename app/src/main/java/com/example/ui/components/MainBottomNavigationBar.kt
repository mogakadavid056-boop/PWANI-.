package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextSecondary

enum class MainTab {
  Home,
  Classes,
  Profile,
  Admin
}

@Composable
fun MainBottomNavigationBar(
  selectedTab: MainTab,
  isAdmin: Boolean,
  onSelectTab: (MainTab) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(64.dp)
      .background(DarkSurface)
      .border(1.dp, DarkBorder)
      .testTag("main_bottom_nav_bar"),
    horizontalArrangement = Arrangement.SpaceAround,
    verticalAlignment = Alignment.CenterVertically
  ) {
    BottomNavItem(
      label = "Home",
      icon = Icons.Default.Home,
      isSelected = selectedTab == MainTab.Home,
      onClick = { onSelectTab(MainTab.Home) },
      tag = "main_nav_home"
    )

    BottomNavItem(
      label = "Classes",
      icon = Icons.Default.Class,
      isSelected = selectedTab == MainTab.Classes,
      onClick = { onSelectTab(MainTab.Classes) },
      tag = "main_nav_classes"
    )

    BottomNavItem(
      label = "Profile",
      icon = Icons.Default.Person,
      isSelected = selectedTab == MainTab.Profile,
      onClick = { onSelectTab(MainTab.Profile) },
      tag = "main_nav_profile"
    )

    if (isAdmin) {
      BottomNavItem(
        label = "Admin",
        icon = Icons.Default.SupervisorAccount,
        isSelected = selectedTab == MainTab.Admin,
        onClick = { onSelectTab(MainTab.Admin) },
        tag = "main_nav_admin"
      )
    }
  }
}

@Composable
private fun BottomNavItem(
  label: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  tag: String
) {
  val tint = if (isSelected) GoldPrimary else TextSecondary
  Column(
    modifier = Modifier
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .testTag(tag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      icon,
      contentDescription = label,
      tint = tint,
      modifier = Modifier.size(22.dp)
    )
    Text(
      text = label,
      style = CaptionStyle.copy(
        color = tint,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        fontSize = 13.sp
      )
    )
  }
}
