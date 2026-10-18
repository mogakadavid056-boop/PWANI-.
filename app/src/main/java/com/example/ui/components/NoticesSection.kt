package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoticeItem
import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun NoticesSection(
  notices: List<NoticeItem>,
  readNoticeIds: Set<String>,
  canPostNotice: Boolean,
  isStaff: Boolean,
  isLoading: Boolean,
  onRefreshNotices: () -> Unit,
  onNoticeClick: (NoticeItem) -> Unit,
  onNavigateToPostNotice: () -> Unit,
  onEditNotice: (NoticeItem) -> Unit = {},
  onDeleteNotice: (NoticeItem) -> Unit = {}
) {
  val pinnedNotices = notices.filter { it.isPinned }
  val regularNotices = notices.filter { !it.isPinned }

  Column(modifier = Modifier.fillMaxWidth().testTag("notices_section")) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "Notices",
          style = SectionHeadingStyle,
          color = TextPrimary,
          modifier = Modifier.testTag("notices_section_title")
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "(${notices.size})", fontSize = 14.sp, color = TextSecondary)
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onRefreshNotices,
          modifier = Modifier.size(36.dp).testTag("refresh_notices_button")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh notices", tint = GoldLight, modifier = Modifier.size(20.dp))
        }

        if (isStaff && canPostNotice) {
          Spacer(modifier = Modifier.width(6.dp))
          Button(
            onClick = onNavigateToPostNotice,
            modifier = Modifier.height(36.dp).testTag("post_notice_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = DarkTextOnGold)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Post Notice", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (isLoading) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LoadingSkeleton(height = 95)
        LoadingSkeleton(height = 95)
        LoadingSkeleton(height = 95)
      }
    } else if (notices.isEmpty()) {
      EmptyState(
        icon = Icons.Default.Campaign,
        title = "No Notices Yet",
        message = if (isStaff) "Tap the Post Notice button above to share updates with your class." else "Nothing here yet. Waiting for your class prefect to update this.",
        actionLabel = if (isStaff && canPostNotice) "Post Notice" else null,
        onActionClick = if (isStaff && canPostNotice) onNavigateToPostNotice else null,
        modifier = Modifier.testTag("notices_empty_state")
      )
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Pinned Section
        if (pinnedNotices.isNotEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 2.dp)
            ) {
              Icon(
                Icons.Default.PushPin,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Pinned",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = GoldLight
              )
            }

            pinnedNotices.forEach { notice ->
              NoticeCard(
                notice = notice,
                isRead = notice.id in readNoticeIds,
                isStaff = isStaff,
                onClick = { onNoticeClick(notice) },
                onEdit = if (isStaff) { { onEditNotice(notice) } } else null,
                onDelete = if (isStaff) { { onDeleteNotice(notice) } } else null
              )
            }
          }
        }

        // Regular Notices
        if (regularNotices.isNotEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (pinnedNotices.isNotEmpty()) {
              Text(
                text = "Recent Notices",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 2.dp)
              )
            }

            regularNotices.forEach { notice ->
              NoticeCard(
                notice = notice,
                isRead = notice.id in readNoticeIds,
                isStaff = isStaff,
                onClick = { onNoticeClick(notice) },
                onEdit = if (isStaff) { { onEditNotice(notice) } } else null,
                onDelete = if (isStaff) { { onDeleteNotice(notice) } } else null
              )
            }
          }
        }
      }
    }
  }
}
