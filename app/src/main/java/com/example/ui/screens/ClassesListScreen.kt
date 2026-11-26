package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassItem
import com.example.ui.components.ClassCard
import com.example.ui.components.ClassRepPullToRefresh
import com.example.ui.components.EmptyState
import com.example.ui.components.FriendlyErrorSnackbar
import com.example.ui.components.LoadingSkeleton
import com.example.ui.components.MetallicGoldText
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ItemSpacing
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.ScreenPadding
import com.example.ui.theme.ScreenTitleStyle
import com.example.ui.theme.SectionHeadingStyle

@Composable
fun ClassesListScreen(
  classes: List<ClassItem>,
  isStaff: Boolean,
  isAdmin: Boolean = false,
  isLecturer: Boolean = false,
  isVerifiedLecturer: Boolean = false,
  isLoading: Boolean,
  errorMessage: String?,
  unreadNotificationCount: Int = 0,
  onOpenNotifications: () -> Unit = {},
  isOnline: Boolean = true,
  onOpenAssistant: () -> Unit = {},
  onDismissError: () -> Unit,
  onRefresh: () -> Unit,
  onSelectClass: (ClassItem) -> Unit,
  onNavigateToJoinClass: () -> Unit,
  onNavigateToCreateClass: () -> Unit,
  onNavigateToFindClasses: () -> Unit = {}
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .testTag("classes_list_screen")
  ) {
    ClassRepPullToRefresh(
      isRefreshing = isLoading,
      onRefresh = onRefresh
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = ScreenPadding, vertical = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ItemSpacing)
      ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          MetallicGoldText(
            text = "My Classes",
            style = ScreenTitleStyle,
            modifier = Modifier.testTag("classes_list_title")
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "(${classes.size})",
            style = SectionHeadingStyle.copy(color = Lavender, fontSize = 18.sp)
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          if (isOnline) {
            IconButton(
              onClick = onOpenAssistant,
              modifier = Modifier.size(48.dp).testTag("classes_assistant_button")
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = "Gemini AI", tint = ChampagneGold, modifier = Modifier.size(22.dp))
            }
          }
          com.example.ui.components.NotificationBellButton(
            unreadCount = unreadNotificationCount,
            onClick = onOpenNotifications
          )
          IconButton(
            onClick = onRefresh,
            modifier = Modifier.size(48.dp).testTag("classes_refresh_button")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ChampagneGold, modifier = Modifier.size(22.dp))
          }
        }
      }

      if (!errorMessage.isNullOrBlank()) {
        FriendlyErrorSnackbar(errorMessage = errorMessage, onDismiss = onDismissError)
      }

      if (isLoading) {
        LoadingSkeleton(height = 90)
        LoadingSkeleton(height = 90)
        LoadingSkeleton(height = 90)
      } else if (classes.isEmpty()) {
        val emptyMessage = when {
          isAdmin -> "You haven't created any classes yet. Tap the button below to add your first class."
          isLecturer -> "You haven't joined any classes yet. Use a class code from your class prefect or department."
          else -> "You haven't joined any classes yet. Use a join code from your class prefect."
        }
        val emptyActionLabel = when {
          isAdmin -> "Create Class"
          isLecturer -> "Join with code"
          else -> "Join Class"
        }
        val emptyAction = when {
          isAdmin -> onNavigateToCreateClass
          else -> onNavigateToJoinClass
        }
        EmptyState(
          icon = Icons.Default.Class,
          title = "No Classes Yet",
          message = emptyMessage,
          actionLabel = emptyActionLabel,
          onActionClick = emptyAction
        )
      } else {
        classes.forEach { item ->
          ClassCard(
            item = item,
            onClick = { onSelectClass(item) }
          )
        }
      }

      Spacer(modifier = Modifier.height(80.dp))
    }
  }

    // Only admin sees Create Class button
    if (isAdmin) {
      FloatingActionButton(
        onClick = onNavigateToCreateClass,
        containerColor = MetallicGold,
        contentColor = Midnight,
        shape = CircleShape,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(24.dp)
          .testTag("classes_fab_add_class")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Class", modifier = Modifier.size(24.dp))
      }
    }
  }
}
