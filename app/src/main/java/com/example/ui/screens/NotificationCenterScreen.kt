package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notifications.AppNotificationItem
import com.example.notifications.NotificationTimeFormatter
import com.example.ui.NotificationUiState
import com.example.ui.components.ClassRepPullToRefresh
import com.example.ui.components.EmptyState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LoadingSkeleton
import com.example.ui.components.MetallicGoldText
import com.example.ui.components.RoyalCrownIcon
import com.example.ui.theme.*

@Composable
fun NotificationCenterScreen(
  uiState: NotificationUiState,
  onRefresh: () -> Unit,
  onLoadMore: () -> Unit,
  onMarkAllRead: () -> Unit,
  onNotificationClick: (AppNotificationItem) -> Unit,
  onDismissError: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  Box(modifier = Modifier.fillMaxSize().testTag("notification_center_screen")) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("notifications_back_btn")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
          }
          Column {
            MetallicGoldText("Notifications", style = ScreenTitleStyle)
            if (uiState.unreadCount > 0) {
              Text("${uiState.unreadCount} unread", style = FieldLabelStyle.copy(color = ElectricViolet))
            }
          }
        }
        if (uiState.notifications.any { it.isUnread } || uiState.unreadCount > 0) {
          TextButton(
            onClick = onMarkAllRead,
            enabled = !uiState.isMarkingAll,
            modifier = Modifier.testTag("notifications_mark_all_read_btn")
          ) {
            Text(
              text = if (uiState.isMarkingAll) "Updating..." else "Mark all as read",
              style = FieldLabelStyle.copy(color = MetallicGold)
            )
          }
        }
      }

      if (!uiState.errorMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        ErrorBanner(message = uiState.errorMessage, onDismiss = onDismissError)
      }

      Spacer(modifier = Modifier.height(10.dp))

      ClassRepPullToRefresh(isRefreshing = uiState.isLoading, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
        if (uiState.isLoading && uiState.notifications.isEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LoadingSkeleton(height = 76)
            LoadingSkeleton(height = 76)
            LoadingSkeleton(height = 76)
          }
        } else if (uiState.notifications.isEmpty()) {
          EmptyState(
            icon = Icons.Default.NotificationsNone,
            title = "All caught up",
            message = "Class notices, open attendance sessions, unit approvals, and alerts appear here."
          )
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(uiState.notifications, key = { it.id }) { item ->
              NotificationRowCard(item = item, onClick = { onNotificationClick(item) })
            }
            if (uiState.hasMore) {
              item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                  OutlinedButton(
                    onClick = onLoadMore,
                    enabled = !uiState.isLoadingMore,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MetallicGold),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold)
                  ) {
                    Text(if (uiState.isLoadingMore) "Loading..." else "Load more", style = ButtonTextStyle)
                  }
                }
              }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
          }
        }
      }
    }
  }
}

@Composable
private fun NotificationRowCard(
  item: AppNotificationItem,
  onClick: () -> Unit
) {
  val (icon, tint) = iconForNotificationKind(item.kind)
  val borderColor = if (item.isUnread) ElectricViolet.copy(alpha = 0.75f) else DarkBorder
  val cardBg = if (item.isUnread) RoyalSurfaceHigh else DarkCard
  val shape = RoundedCornerShape(18.dp)

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .then(if (item.isUnread) Modifier.shadow(8.dp, shape, ambientColor = ElectricViolet, spotColor = ElectricViolet) else Modifier)
      .clip(shape)
      .background(cardBg)
      .border(1.dp, borderColor, shape)
      .clickable(onClick = onClick)
      .padding(14.dp)
      .testTag("notification_item_${item.id}"),
    verticalAlignment = Alignment.Top
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(tint.copy(alpha = 0.18f)),
      contentAlignment = Alignment.Center
    ) {
      if (item.kind == "prefect_appointed" || item.kind == "lecturer_verified") {
        RoyalCrownIcon(size = 22.dp, tint = tint)
      } else {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
      }
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = item.title,
          style = CardTitleStyle.copy(
            fontSize = 16.sp,
            color = if (item.isUnread) ChampagneGold else SilverWhite
          ),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        if (item.isUnread) {
          Spacer(modifier = Modifier.width(8.dp))
          Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ElectricViolet))
        }
      }
      if (item.body.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = item.body,
          style = BodyStyle.copy(fontSize = 14.sp, lineHeight = 20.sp, color = Lavender),
          maxLines = 3,
          overflow = TextOverflow.Ellipsis
        )
      }
      val rel = NotificationTimeFormatter.formatRelative(item.createdAt)
      if (rel.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = rel, style = CaptionStyle)
      }
    }
  }
}

private fun iconForNotificationKind(kind: String): Pair<ImageVector, Color> = when (kind) {
  "notice_urgent" -> Icons.Default.Warning to ErrorRed
  "notice" -> Icons.Default.Campaign to MetallicGold
  "attendance_open" -> Icons.Default.HowToReg to SuccessGreen
  "attendance_warning", "attendance_alert" -> Icons.Default.ReportProblem to ErrorRed
  "timetable" -> Icons.Default.CalendarToday to ChampagneGold
  "material" -> Icons.Default.Description to MetallicGold
  "poll" -> Icons.Default.Poll to ElectricViolet
  "quiz" -> Icons.Default.EmojiEvents to ChampagneGold
  "paper_marked" -> Icons.Default.PictureAsPdf to SuccessGreen
  "prefect_appointed" -> Icons.Default.Shield to ChampagneGold
  "new_unit", "unit_approved" -> Icons.Default.LibraryAddCheck to SuccessGreen
  "unit_rejected" -> Icons.Default.Cancel to ErrorRed
  "lecturer_verified", "join_approved" -> Icons.Default.Verified to SuccessGreen
  "lecturer_join_request" -> Icons.Default.PersonAdd to ElectricViolet
  else -> Icons.Default.Notifications to ChampagneGold
}
