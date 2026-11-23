package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.NavLabelStyle
import com.example.ui.theme.RoyalNavSurface

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
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(RoyalNavSurface)
      .testTag("main_bottom_nav_bar")
  ) {
    HorizontalDivider(thickness = 1.dp, color = MetallicGold.copy(alpha = 0.45f))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(66.dp),
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
}

@Composable
private fun BottomNavItem(
  label: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  tag: String
) {
  val tint = if (isSelected) ChampagneGold else Lavender
  Column(
    modifier = Modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .testTag(tag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(12.dp))
        .background(if (isSelected) ElectricViolet.copy(alpha = 0.25f) else Color.Transparent)
        .padding(horizontal = 12.dp, vertical = 3.dp),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        icon,
        contentDescription = label,
        tint = tint,
        modifier = Modifier.size(22.dp)
      )
    }
    Text(
      text = label,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      style = NavLabelStyle.copy(
        color = tint,
        shadow = focusTextShadow(isSelected)
      )
    )
  }
}
