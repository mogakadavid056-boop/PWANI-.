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
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardRadius
import com.example.ui.theme.CardTitleStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.Lavender
import com.example.ui.theme.MontserratFamily
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.SilverWhite
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun getCategoryColors(category: String): Pair<Color, Color> {
  return when (category.lowercase().trim()) {
    "exam" -> Pair(ErrorContainer, ErrorColor)
    "assignment" -> Pair(ElectricViolet.copy(alpha = 0.24f), ChampagneGold)
    "event" -> Pair(SuccessContainer, SuccessColor)
    else -> Pair(RoyalSurfaceHigh, Lavender)
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
  val cardBg = if (!isRead) RoyalSurfaceHigh else DarkCard
  val borderStroke = when {
    notice.isUrgent -> ErrorRed.copy(alpha = 0.65f)
    !isRead -> ElectricViolet.copy(alpha = 0.65f)
    else -> DarkBorder
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(cardBg)
      .border(1.dp, borderStroke, shape)
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
            // Unread Electric Violet dot
            if (!isRead) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(ElectricViolet)
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
                  modifier = Modifier.size(48.dp).testTag("notice_more_btn_${notice.id}")
                ) {
                  Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Notice options",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                  )
                }

                if (showMenu) {
                  val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
                  val context = LocalContext.current
                  androidx.compose.ui.window.Dialog(onDismissRequest = { showMenu = false }) {
                    Column(
                      modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(RoyalSurfaceHigh)
                        .border(1.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(18.dp)).padding(16.dp)
                    ) {
                      Text("Notice Options", style = SectionHeadingStyle)
                      Spacer(modifier = Modifier.height(10.dp))
                      if (onEdit != null) {
                        Row(
                          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                            showMenu = false; if (!isOnline) OfflineToastHelper.show(context) else onEdit()
                          }.padding(vertical = 12.dp, horizontal = 8.dp).testTag("notice_edit_menu_item_${notice.id}"),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Icon(Icons.Default.Edit, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                          Spacer(modifier = Modifier.width(12.dp))
                          Text("Edit Notice", color = TextPrimary, fontSize = 15.sp)
                        }
                      }
                      if (onDelete != null) {
                        Row(
                          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                            showMenu = false; if (!isOnline) OfflineToastHelper.show(context) else onDelete()
                          }.padding(vertical = 12.dp, horizontal = 8.dp).testTag("notice_delete_menu_item_${notice.id}"),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                          Spacer(modifier = Modifier.width(12.dp))
                          Text("Delete Notice", color = ErrorRed, fontSize = 15.sp)
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = notice.title.ifBlank { "Untitled Notice" },
          style = CardTitleStyle.copy(color = SilverWhite),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = notice.content.ifBlank { "No content provided." },
          style = BodyStyle.copy(fontSize = 14.sp, lineHeight = 20.sp, color = Lavender),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
