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
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ItemSpacing
import com.example.ui.theme.ScreenPadding
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TitleStyle

@Composable
fun ClassesListScreen(
  classes: List<ClassItem>,
  isStaff: Boolean,
  isLoading: Boolean,
  errorMessage: String?,
  onDismissError: () -> Unit,
  onRefresh: () -> Unit,
  onSelectClass: (ClassItem) -> Unit,
  onNavigateToJoinClass: () -> Unit,
  onNavigateToCreateClass: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
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
          Text(
            text = "My Classes",
            style = TitleStyle,
            modifier = Modifier.testTag("classes_list_title")
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "(${classes.size})",
            style = SectionHeadingStyle.copy(color = TextSecondary, fontSize = 16.sp)
          )
        }

        IconButton(
          onClick = onRefresh,
          modifier = Modifier.size(40.dp).testTag("classes_refresh_button")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GoldLight, modifier = Modifier.size(22.dp))
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
        EmptyState(
          icon = Icons.Default.Class,
          title = "No Classes Yet",
          message = if (isStaff) "You haven't created or joined any classes yet. Tap the button below to add your first class." else "You haven't joined any classes yet. Use a join code from your class prefect.",
          actionLabel = if (isStaff) "Create Class" else "Join Class",
          onActionClick = if (isStaff) onNavigateToCreateClass else onNavigateToJoinClass
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

    if (isStaff) {
      FloatingActionButton(
        onClick = onNavigateToCreateClass,
        containerColor = GoldPrimary,
        contentColor = com.example.ui.theme.DarkTextOnGold,
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
