package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GroupMessageItem
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(
  message: GroupMessageItem,
  isOwn: Boolean,
  canDelete: Boolean,
  onLongClick: () -> Unit
) {
  val formattedTime = formatMessageTime(message.createdAt)

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
  ) {
    Column(
      horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start,
      modifier = Modifier.widthIn(max = 280.dp)
    ) {
      if (!isOwn) {
        Text(
          text = message.senderName,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = GoldLight,
          modifier = Modifier.padding(start = 6.dp, bottom = 3.dp)
        )
      }

      // Rounded message bubble
      val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isOwn) 16.dp else 4.dp,
        bottomEnd = if (isOwn) 4.dp else 16.dp
      )

      Box(
        modifier = Modifier
          .clip(bubbleShape)
          .background(if (isOwn) GoldPrimary else Color(0xFF25252B))
          .border(
            width = 1.dp,
            color = if (isOwn) GoldLight.copy(alpha = 0.5f) else DarkBorder,
            shape = bubbleShape
          )
          .combinedClickable(
            onLongClick = if (canDelete) onLongClick else null,
            onClick = {}
          )
          .padding(horizontal = 14.dp, vertical = 10.dp)
          .testTag("chat_bubble_${message.id}")
      ) {
        Text(
          text = message.messageText,
          fontSize = 14.sp,
          color = if (isOwn) DarkTextOnGold else TextPrimary,
          lineHeight = 20.sp
        )
      }

      // Small time under each message
      if (formattedTime.isNotBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = formattedTime,
          style = CaptionStyle.copy(fontSize = 10.sp, color = TextSecondary),
          modifier = Modifier.padding(horizontal = 4.dp)
        )
      }
    }
  }
}

private fun formatMessageTime(rawDate: String): String {
  if (rawDate.isBlank()) return ""
  return try {
    val clean = rawDate.take(19)
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    val date = inputFormat.parse(clean)
    if (date != null) {
      val outputFormat = SimpleDateFormat("h:mm a", Locale.US)
      outputFormat.format(date)
    } else {
      rawDate.take(16).replace("T", " ")
    }
  } catch (_: Exception) {
    rawDate.take(16).replace("T", " ")
  }
}
