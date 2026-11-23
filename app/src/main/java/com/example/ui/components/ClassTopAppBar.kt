package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassItem
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RoyalNavSurface
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.ScreenTitleStyle

@Composable
fun ClassTopAppBar(
  classItem: ClassItem,
  isStaff: Boolean,
  unreadNotificationCount: Int = 0,
  onOpenNotifications: () -> Unit = {},
  isOnline: Boolean = true,
  onOpenAssistant: () -> Unit = {},
  onBack: () -> Unit,
  onEditClassNameClick: () -> Unit = {},
  onOpenClassSwitcher: (() -> Unit)? = null,
  onNavigateToProfile: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(RoyalNavSurface)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(60.dp)
        .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.size(48.dp).testTag("class_top_back_button")
        ) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = ChampagneGold,
            modifier = Modifier.size(22.dp)
          )
        }
        Spacer(modifier = Modifier.width(4.dp))

        val clickableModifier = if (isStaff && onOpenClassSwitcher != null) {
          Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onOpenClassSwitcher() }
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag("class_top_switcher_trigger")
        } else {
          Modifier
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = clickableModifier
        ) {
          MetallicGoldText(
            text = classItem.name,
            style = ScreenTitleStyle.copy(fontSize = 20.sp, lineHeight = 26.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).testTag("class_top_name_title")
          )
          if (isStaff && onOpenClassSwitcher != null) {
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
              Icons.Default.ArrowDropDown,
              contentDescription = "Switch class",
              tint = ChampagneGold,
              modifier = Modifier.size(24.dp)
            )
          }
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (isStaff) {
          IconButton(
            onClick = onEditClassNameClick,
            modifier = Modifier.size(44.dp).testTag("class_top_edit_button")
          ) {
            Icon(
              Icons.Default.Edit,
              contentDescription = "Edit Class Name",
              tint = MetallicGold,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        if (isOnline) {
          IconButton(
            onClick = onOpenAssistant,
            modifier = Modifier.size(44.dp).testTag("class_top_assistant_button")
          ) {
            Icon(
              Icons.Default.AutoAwesome,
              contentDescription = "Assistant",
              tint = ChampagneGold,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        NotificationBellButton(
          unreadCount = unreadNotificationCount,
          onClick = onOpenNotifications
        )

        IconButton(
          onClick = onNavigateToProfile,
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(RoyalSurfaceHigh)
            .border(1.dp, DarkBorder, CircleShape)
            .testTag("class_top_avatar_button")
        ) {
          Icon(
            Icons.Default.Person,
            contentDescription = "Profile",
            tint = ChampagneGold,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
    HorizontalDivider(thickness = 1.dp, color = MetallicGold.copy(alpha = 0.45f))
  }
}
