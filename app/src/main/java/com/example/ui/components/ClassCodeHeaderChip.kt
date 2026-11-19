package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.net.URLEncoder

@Composable
fun ClassCodeHeaderChip(
  className: String,
  joinCode: String?,
  isLecturer: Boolean,
  isRegenerating: Boolean = false,
  onRegenerateCode: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  if (joinCode.isNullOrBlank()) return

  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  var showConfirmChange by remember { mutableStateOf(false) }

  val shareText = "Join $className on ClassRep. Open the app, choose Join with a class code and enter: $joinCode"

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(DarkSurface)
      .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .testTag("class_code_header_chip")
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Default.Key, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Class code: ",
            fontSize = 13.sp,
            color = TextSecondary
          )
          Text(
            text = joinCode,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = GoldLight,
            modifier = Modifier.testTag("class_code_value_text")
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              clipboardManager.setText(AnnotatedString(joinCode))
              Toast.makeText(context, "Class code copied!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(40.dp).testTag("class_code_copy_btn")
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy class code", tint = GoldLight, modifier = Modifier.size(18.dp))
          }
          IconButton(
            onClick = {
              val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
              }
              context.startActivity(Intent.createChooser(sendIntent, "Share Class Code"))
            },
            modifier = Modifier.size(40.dp).testTag("class_code_share_btn")
          ) {
            Icon(Icons.Default.Share, contentDescription = "Share class code", tint = GoldLight, modifier = Modifier.size(18.dp))
          }
          IconButton(
            onClick = {
              val encoded = runCatching { URLEncoder.encode(shareText, "UTF-8") }.getOrDefault(shareText)
              val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=$encoded"))
              try {
                context.startActivity(waIntent)
              } catch (_: Exception) {
                val fallback = Intent(Intent.ACTION_SEND).apply {
                  type = "text/plain"
                  putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(fallback, "Share via"))
              }
            },
            modifier = Modifier.size(40.dp).testTag("class_code_whatsapp_btn")
          ) {
            Icon(
              Icons.Default.Chat,
              contentDescription = "Share on WhatsApp",
              tint = Color(0xFF25D366),
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      if (isLecturer) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(
            onClick = { showConfirmChange = true },
            enabled = !isRegenerating,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            modifier = Modifier.testTag("class_code_change_btn")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isRegenerating) "Changing..." else "Change code",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = GoldPrimary
            )
          }
        }
      }
    }
  }

  if (showConfirmChange) {
    ConfirmActionDialog(
      title = "Change Class Code?",
      message = "The old code will stop working",
      confirmButtonText = "Change code",
      confirmButtonColor = GoldPrimary,
      onConfirm = {
        showConfirmChange = false
        onRegenerateCode()
      },
      onDismiss = { showConfirmChange = false }
    )
  }
}
