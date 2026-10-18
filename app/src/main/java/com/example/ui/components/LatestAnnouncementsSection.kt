package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoticeItem
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardRadius
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LatestAnnouncementsSection(
  announcements: List<NoticeItem>,
  isLoading: Boolean,
  onNoticeClick: (NoticeItem) -> Unit = {}
) {
  Column(
    modifier = Modifier.fillMaxWidth().testTag("latest_announcements_section"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        Icons.Default.Campaign,
        contentDescription = null,
        tint = GoldPrimary,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Latest Announcements",
        style = SectionHeadingStyle,
        color = GoldLight
      )
    }

    if (isLoading) {
      LoadingSkeleton(height = 72)
      LoadingSkeleton(height = 72)
    } else if (announcements.isEmpty()) {
      EmptyState(
        icon = Icons.Default.Campaign,
        title = "No Announcements",
        message = "No recent announcements posted yet."
      )
    } else {
      announcements.forEach { notice ->
        AnnouncementCard(notice = notice, onClick = { onNoticeClick(notice) })
      }
    }
  }
}

@Composable
private fun AnnouncementCard(
  notice: NoticeItem,
  onClick: () -> Unit
) {
  val shape = RoundedCornerShape(CardRadius)
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .background(DarkCard)
      .border(1.dp, if (notice.isUrgent) ErrorRed.copy(alpha = 0.6f) else DarkBorder, shape)
      .clickable { onClick() }
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        if (notice.isUrgent) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(ErrorContainer)
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(12.dp))
              Text("URGENT", style = CaptionStyle.copy(color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 12.sp))
            }
          }
          Spacer(modifier = Modifier.width(6.dp))
        }

        Text(
          text = notice.title,
          style = BodyStyle.copy(fontWeight = FontWeight.SemiBold),
          color = TextPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      if (!notice.className.isNullOrBlank()) {
        Spacer(modifier = Modifier.width(8.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = notice.className,
            style = CaptionStyle.copy(color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }

    if (notice.content.isNotBlank()) {
      Text(
        text = notice.content,
        style = CaptionStyle.copy(color = TextSecondary, fontSize = 13.sp),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}
