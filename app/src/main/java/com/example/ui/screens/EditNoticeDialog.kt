package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.NoticeItem
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun EditNoticeDialog(
  notice: NoticeItem,
  isLoading: Boolean = false,
  onDismiss: () -> Unit,
  onSave: (title: String, content: String, category: String, urgent: Boolean, pinned: Boolean) -> Unit
) {
  var title by remember { mutableStateOf(notice.title) }
  var content by remember { mutableStateOf(notice.content) }
  var category by remember { mutableStateOf(notice.category.ifBlank { "general" }) }
  var urgent by remember { mutableStateOf(notice.isUrgent) }
  var pinned by remember { mutableStateOf(notice.isPinned) }

  val categories = listOf("general", "exam", "assignment", "event")
  val canSave = title.trim().isNotBlank() && content.trim().isNotBlank()

  Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(DarkSurface)
        .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
        .padding(20.dp)
        .verticalScroll(rememberScrollState())
        .testTag("edit_notice_dialog")
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Edit, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Edit Notice", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
      }

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Title") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("edit_notice_title_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = GoldPrimary, unfocusedBorderColor = DarkBorder,
          focusedLabelColor = GoldPrimary, unfocusedLabelColor = TextSecondary,
          focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
          focusedContainerColor = DarkSurfaceVariant, unfocusedContainerColor = DarkSurfaceVariant
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = content,
        onValueChange = { content = it },
        label = { Text("Content") },
        minLines = 3,
        maxLines = 6,
        modifier = Modifier.fillMaxWidth().testTag("edit_notice_content_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = GoldPrimary, unfocusedBorderColor = DarkBorder,
          focusedLabelColor = GoldPrimary, unfocusedLabelColor = TextSecondary,
          focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
          focusedContainerColor = DarkSurfaceVariant, unfocusedContainerColor = DarkSurfaceVariant
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text("Category", fontSize = 12.sp, color = TextSecondary)
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        categories.forEach { cat ->
          val isSel = category.equals(cat, ignoreCase = true)
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSel) GoldPrimary else DarkSurfaceVariant)
              .border(1.dp, if (isSel) GoldLight else DarkBorder, RoundedCornerShape(8.dp))
              .clickable { category = cat }
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              cat.replaceFirstChar { it.uppercase() },
              fontSize = 13.5.sp,
              fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
              color = if (isSel) com.example.ui.theme.DarkTextOnGold else TextPrimary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("Mark as Urgent", fontSize = 14.sp, color = TextPrimary)
        Switch(
          checked = urgent,
          onCheckedChange = { urgent = it },
          colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldLight.copy(alpha = 0.5f))
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("Pin to Top", fontSize = 14.sp, color = TextPrimary)
        Switch(
          checked = pinned,
          onCheckedChange = { pinned = it },
          colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldLight.copy(alpha = 0.5f))
        )
      }

      Spacer(modifier = Modifier.height(18.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = onDismiss,
          enabled = !isLoading,
          modifier = Modifier.weight(1f).height(48.dp).testTag("edit_notice_cancel_button"),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkBorder),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = TextPrimary)
        ) {
          Text("Cancel", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Button(
          onClick = { onSave(title.trim(), content.trim(), category, urgent, pinned) },
          enabled = !isLoading && canSave,
          modifier = Modifier.weight(1f).height(48.dp).testTag("edit_notice_save_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = com.example.ui.theme.DarkTextOnGold)
        ) {
          if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = com.example.ui.theme.DarkTextOnGold, strokeWidth = 2.dp)
          } else {
            Text("Save", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = com.example.ui.theme.DarkTextOnGold)
          }
        }
      }
    }
  }
}
