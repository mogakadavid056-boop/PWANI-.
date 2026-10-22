package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoticeItem
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardRadius
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.PoppinsFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun getCategoryColors(category: String): Pair<Color, Color> {
  return when (category.lowercase().trim()) {
    "exam" -> Pair(Color(0xFF381414), Color(0xFFFF5C5C))
    "assignment" -> Pair(Color(0xFF14283D), Color(0xFF58A6FF))
    "event" -> Pair(Color(0xFF12341F), Color(0xFF3DDC84))
    else -> Pair(Color(0xFF25252B), Color(0xFFA0A0A8)) // general = grey
  }
}

fun formatRelativeTime(rawDate: String): String {
  if (rawDate.isBlank()) return "Recently"
  return try {
    val clean = rawDate.take(19)
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    val date = inputFormat.parse(clean) ?: return rawDate.take(10)
    val now = System.currentTimeMillis()
    val diff = now - date.time
    val minutes = diff / (1000 * 60)
    val hours = minutes / 60
    val days = hours / 24

    when {
      minutes < 1 -> "Just now"
      minutes < 60 -> "${minutes}m ago"
      hours < 24 -> "${hours}h ago"
      days == 1L -> "Yesterday"
      days < 7 -> "${days}d ago"
      else -> SimpleDateFormat("MMM d", Locale.US).format(date)
    }
  } catch (_: Exception) {
    rawDate.take(10)
  }
}

@Composable
fun NoticeCard(
  notice: NoticeItem,
  isRead: Boolean,
  isStaff: Boolean = false,
  onClick: () -> Unit,
  onEdit: (() -> Unit)? = null,
  onDelete: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var showMenu by remember { mutableStateOf(false) }
  val (catBg, catText) = getCategoryColors(notice.category)
  val relativeTime = formatRelativeTime(notice.createdAt)
  val shape = RoundedCornerShape(CardRadius)
  val cardBg = if (!isRead) Color(0xFF24242A) else DarkCard

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(cardBg)
      .border(1.dp, if (notice.isUrgent) ErrorRed.copy(alpha = 0.5f) else DarkBorder, shape)
      .clickable(onClick = onClick)
      .testTag("notice_card_${notice.id}")
  ) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
      // Urgent red left accent bar
      if (notice.isUrgent) {
        Box(
          modifier = Modifier
            .width(5.dp)
            .fillMaxHeight()
            .background(ErrorRed)
        )
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .padding(14.dp)
      ) {
        // Badges row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // Unread gold dot
            if (!isRead) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(GoldPrimary)
                  .testTag("unread_dot_${notice.id}")
              )
            }

            // Coloured category chip
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(catBg)
                .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text(
                text = notice.category.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = catText
              )
            }

            // Urgent badge
            if (notice.isUrgent) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(ErrorContainer)
                  .padding(horizontal = 7.dp, vertical = 2.dp)
                  .testTag("urgent_badge_${notice.id}")
              ) {
                Text(
                  text = "URGENT",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = ErrorRed
                )
              }
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            if (notice.isPinned) {
              Icon(
                Icons.Default.PushPin,
                contentDescription = "Pinned",
                tint = GoldPrimary,
                modifier = Modifier.size(16.dp).testTag("pinned_icon_${notice.id}")
              )
            }
            Text(
              text = relativeTime,
              style = CaptionStyle.copy(fontSize = 13.sp, color = TextSecondary)
            )

            // Staff three-dot menu
            if (isStaff && (onEdit != null || onDelete != null)) {
              Box {
                IconButton(
                  onClick = { showMenu = true },
                  modifier = Modifier.size(36.dp).testTag("notice_more_btn_${notice.id}")
                ) {
                  Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Notice options",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                  )
                }

                DropdownMenu(
                  expanded = showMenu,
                  onDismissRequest = { showMenu = false },
                  modifier = Modifier.background(DarkSurface).border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                ) {
                  val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
                  val context = LocalContext.current
                  if (onEdit != null) {
                    DropdownMenuItem(
                      text = { Text("Edit Notice", color = TextPrimary, fontSize = 14.sp) },
                      leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp)) },
                      onClick = {
                        showMenu = false
                        if (!isOnline) OfflineToastHelper.show(context) else onEdit()
                      },
                      modifier = Modifier.testTag("notice_edit_menu_item_${notice.id}")
                    )
                  }
                  if (onDelete != null) {
                    DropdownMenuItem(
                      text = { Text("Delete Notice", color = ErrorRed, fontSize = 14.sp) },
                      leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp)) },
                      onClick = {
                        showMenu = false
                        if (!isOnline) OfflineToastHelper.show(context) else onDelete()
                      },
                      modifier = Modifier.testTag("notice_delete_menu_item_${notice.id}")
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title in bold (Poppins) - wrapping onto 2 lines
        Text(
          text = notice.title.ifBlank { "Untitled Notice" },
          fontFamily = PoppinsFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          lineHeight = 24.sp,
          color = TextPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 2-line preview
        Text(
          text = notice.content.ifBlank { "No content provided." },
          fontSize = 14.sp,
          color = TextSecondary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 20.sp
        )
      }
    }
  }
}
