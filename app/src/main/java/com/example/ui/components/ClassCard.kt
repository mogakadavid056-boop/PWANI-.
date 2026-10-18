package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassItem
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.PoppinsFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ClassCard(
  item: ClassItem,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null
) {
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current

  ClassRepCard(
    modifier = modifier.testTag("class_card_${item.id}"),
    onClick = onClick
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(DarkSurfaceVariant)
          .border(1.dp, GoldDark.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.Class, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.name.ifBlank { "Untitled Class" },
          fontFamily = PoppinsFamily,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        if (!item.programmeName.isNullOrBlank()) {
          Text(
            text = item.programmeName,
            fontSize = 12.sp,
            color = GoldLight,
            fontWeight = FontWeight.Normal
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Code: ${item.code.ifBlank { "N/A" }}",
          fontSize = 12.sp,
          color = TextSecondary,
          fontWeight = FontWeight.Medium
        )
      }

      if (!item.joinCode.isNullOrBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(DarkSurfaceVariant)
              .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = item.joinCode,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = GoldLight
            )
          }
          IconButton(
            onClick = {
              clipboardManager.setText(AnnotatedString(item.joinCode))
              Toast.makeText(context, "Join code copied!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(32.dp)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy join code", tint = GoldLight, modifier = Modifier.size(16.dp))
          }
        }
      }
    }
  }
}
