package com.example.admin.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.notifications.AdminNotificationItem
import com.example.admin.notifications.AdminNotificationTimeFormatter
import com.example.admin.ui.AdminNotificationUiState
import com.example.admin.ui.components.AdminEmptyState
import com.example.admin.ui.components.AdminFeedbackBanner
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminMetallicGoldText
import com.example.admin.ui.theme.AdminBorder
import com.example.admin.ui.theme.AdminCaptionStyle
import com.example.admin.ui.theme.AdminCardTitleStyle
import com.example.admin.ui.theme.AdminScreenTitleStyle
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.AmberWarning
import com.example.admin.ui.theme.ChampagneGold
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.ElectricViolet
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.InfoBlue
import com.example.admin.ui.theme.MontserratFamily
import com.example.admin.ui.theme.SilverWhite

@Composable
fun AdminNotificationCenterScreen(
  state: AdminNotificationUiState,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  onMarkAllRead: () -> Unit,
  onLoadMore: () -> Unit,
  onNotificationClick: (AdminNotificationItem) -> Unit,
  onDismissError: () -> Unit
) {
  BackHandler(onBack = onBack)

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp).testTag("admin_notifications_screen")) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
      }
      Column(modifier = Modifier.weight(1f)) {
        AdminMetallicGoldText(text = "Notifications", style = AdminScreenTitleStyle)
        Text(
          text = if (state.unreadCount > 0) "${state.unreadCount} unread alert(s)" else "All caught up",
          style = AdminCaptionStyle.copy(color = ChampagneGold)
        )
      }
      if (state.unreadCount > 0) {
        TextButton(onClick = onMarkAllRead, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
          Icon(Icons.Default.DoneAll, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Mark all", fontFamily = MontserratFamily, color = ChampagneGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
      }
      IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Default.Refresh, contentDescription = "Refresh notifications", tint = ChampagneGold)
      }
    }

    state.errorMessage?.let { msg ->
      Spacer(modifier = Modifier.height(6.dp))
      AdminFeedbackBanner(message = msg, isError = true, onDismiss = onDismissError)
    }

    if (state.isLoading) {
      Spacer(modifier = Modifier.height(6.dp))
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = GoldPrimary)
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (state.items.isEmpty() && !state.isLoading) {
      AdminEmptyState(
        icon = Icons.Default.NotificationsNone,
        title = "No notifications yet",
        subtitle = "Lecturer registrations, unit approvals, and attendance alerts appear here."
      )
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        items(state.items, key = { it.id }) { item ->
          AdminNotificationCard(item = item, onClick = { onNotificationClick(item) })
        }
        if (state.hasMore) {
          item {
            OutlinedButton(
              onClick = onLoadMore,
              enabled = !state.isLoadingMore,
              border = BorderStroke(1.dp, GoldPrimary),
              modifier = Modifier.fillMaxWidth().height(48.dp),
              shape = RoundedCornerShape(14.dp)
            ) {
              Text(if (state.isLoadingMore) "Loading..." else "Load more", fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, color = ChampagneGold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminNotificationCard(
  item: AdminNotificationItem,
  onClick: () -> Unit
) {
  val accent = when {
    item.kind == "attendance_alert" -> CrimsonError
    item.kind == "attendance_warning" -> AmberWarning
    item.kind == "unit_pending" -> InfoBlue
    item.kind == "lecturer_registered" || item.kind == "lecturer_join_request" -> GoldPrimary
    item.kind == "unit_approved" -> EmeraldSuccess
    item.kind == "quiz" -> ChampagneGold
    item.kind == "paper_marked" -> EmeraldSuccess
    else -> GoldLight
  }
  val icon = when {
    item.isUrgentKind -> Icons.Default.Warning
    item.kind == "quiz" -> Icons.Default.EmojiEvents
    item.kind == "paper_marked" -> Icons.Default.PictureAsPdf
    item.kind == "unit_pending" || item.kind == "unit_approved" -> Icons.Default.MenuBook
    item.kind == "lecturer_registered" || item.kind == "lecturer_join_request" -> Icons.Default.VerifiedUser
    item.kind.startsWith("attendance") -> Icons.Default.FactCheck
    else -> Icons.Default.Notifications
  }

  AdminGlassCard(
    borderColor = if (item.isUnread) ElectricViolet else AdminBorder,
    onClick = onClick
  ) {
    Row(verticalAlignment = Alignment.Top) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(AdminSurface),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
          Text(
            text = item.title.ifBlank { "Admin Alert" },
            style = AdminCardTitleStyle,
            modifier = Modifier.weight(1f)
          )
          if (item.isUnread) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(ElectricViolet))
          }
        }
        if (item.body.isNotBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(item.body, style = AdminCaptionStyle.copy(color = SilverWhite.copy(alpha = 0.88f)))
        }
        val ts = AdminNotificationTimeFormatter.formatRelative(item.createdAt)
        if (ts.isNotBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(ts, style = AdminCaptionStyle)
        }
      }
    }
  }
}
