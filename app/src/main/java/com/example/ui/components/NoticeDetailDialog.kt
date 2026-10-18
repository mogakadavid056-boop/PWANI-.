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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.NoticeItem
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardRadius
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.PoppinsFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private fun formatFullDateTime(rawDate: String): String {
  if (rawDate.isBlank()) return "Recently"
  return try {
    val clean = rawDate.take(19)
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    val date = inputFormat.parse(clean) ?: return rawDate.take(10)
    val outputFormat = SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.US)
    outputFormat.format(date)
  } catch (_: Exception) {
    rawDate.take(16).replace("T", " ")
  }
}

@Composable
fun NoticeDetailDialog(
  notice: NoticeItem,
  onDismiss: () -> Unit
) {
  val (catBg, catText) = getCategoryColors(notice.category)
  val shape = RoundedCornerShape(CardRadius)
  val fullDate = formatFullDateTime(notice.createdAt)

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .clip(shape)
        .background(DarkCard)
        .border(1.dp, if (notice.isUrgent) ErrorRed.copy(alpha = 0.5f) else DarkBorder, shape)
        .padding(20.dp)
        .testTag("notice_detail_dialog")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Top action bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(catBg)
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(
                text = notice.category.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = catText
              )
            }

            if (notice.isUrgent) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(ErrorContainer)
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "URGENT",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = ErrorRed
                )
              }
            }

            if (notice.isPinned) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PushPin, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Pinned", fontSize = 11.sp, color = GoldPrimary, fontWeight = FontWeight.SemiBold)
              }
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp).testTag("notice_detail_close_button")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(20.dp))
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Large title in Poppins
        Text(
          text = notice.title.ifBlank { "Untitled Notice" },
          fontFamily = PoppinsFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 20.sp,
          color = TextPrimary,
          lineHeight = 26.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Date and time row
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = fullDate,
            style = CaptionStyle.copy(color = TextSecondary, fontSize = 12.sp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Full content with comfortable line spacing
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp, max = 340.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkBackground)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
        ) {
          Text(
            text = notice.content.ifBlank { "No content provided." },
            style = BodyStyle.copy(fontSize = 15.sp, lineHeight = 23.sp),
            color = TextPrimary
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        PrimaryButton(
          text = "Done",
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth().testTag("notice_detail_dismiss_button")
        )
      }
    }
  }
}
