package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DiscussionGroupItem
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GroupsSection(
  groups: List<DiscussionGroupItem>,
  isLoading: Boolean,
  isStaff: Boolean = false,
  onRefreshGroups: () -> Unit,
  onCreateGroupClick: () -> Unit,
  onOpenChat: (DiscussionGroupItem) -> Unit,
  onJoinGroup: (DiscussionGroupItem) -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "Groups",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          modifier = Modifier.testTag("groups_section_title")
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "(${groups.size})", fontSize = 14.sp, color = TextSecondary)
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onRefreshGroups,
          modifier = Modifier.size(36.dp).testTag("refresh_groups_button")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GoldLight, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Open to everyone in the class
        Button(
          onClick = onCreateGroupClick,
          modifier = Modifier.height(36.dp).testTag("create_group_button"),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = DarkTextOnGold)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Create Group", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold)
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (isLoading) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LoadingSkeleton(height = 90)
        LoadingSkeleton(height = 90)
      }
    } else if (groups.isEmpty()) {
      EmptyState(
        icon = Icons.Default.Forum,
        title = "No Discussion Groups Yet",
        message = if (isStaff) "Create a group to start discussions and chat with your classmates." else "Nothing here yet. Waiting for your class prefect to update this.",
        actionLabel = if (isStaff) "Create Group" else null,
        onActionClick = if (isStaff) onCreateGroupClick else null,
        modifier = Modifier.testTag("groups_empty_state")
      )
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        groups.forEach { group ->
          GroupCard(
            group = group,
            onOpenChat = { onOpenChat(group) },
            onJoinGroup = { onJoinGroup(group) }
          )
        }
      }
    }
  }
}
