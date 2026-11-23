package com.example.admin.assistant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.ui.components.royalAdminTextFieldColors
import com.example.admin.ui.theme.AdminBorder
import com.example.admin.ui.theme.AdminCaptionStyle
import com.example.admin.ui.theme.AdminFieldLabelStyle
import com.example.admin.ui.theme.ChampagneGold
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.Lavender
import com.example.admin.ui.theme.RoyalSurfaceHigh
import com.example.admin.ui.theme.SilverWhite

@Composable
fun AdminActionProposalCard(
  action: AdminAssistantAction,
  onConfirm: () -> Unit,
  onSaveEdit: (Map<String, Any?>) -> Unit,
  onCancel: () -> Unit
) {
  var isEditing by remember(action.id) { mutableStateOf(false) }

  val borderColor = when (action.status) {
    AdminActionCardStatus.DONE -> EmeraldSuccess.copy(alpha = 0.6f)
    AdminActionCardStatus.FAILED -> CrimsonError.copy(alpha = 0.65f)
    AdminActionCardStatus.CANCELLED -> AdminBorder.copy(alpha = 0.45f)
    else -> GoldPrimary.copy(alpha = 0.7f)
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(RoyalSurfaceHigh)
      .border(1.dp, borderColor, RoundedCornerShape(16.dp))
      .padding(12.dp)
      .testTag("admin_action_card_${action.tool}"),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = if (action.tool == AdminActionExecutor.TOOL_POST_NOTICE) "PROPOSED NOTICE" else "NAVIGATION",
        style = AdminFieldLabelStyle.copy(fontSize = 12.sp, color = ChampagneGold)
      )
      if (action.status == AdminActionCardStatus.DONE) {
        Text("Confirmed", style = AdminCaptionStyle.copy(color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
      } else if (action.status == AdminActionCardStatus.CANCELLED) {
        Text("Cancelled", style = AdminCaptionStyle.copy(color = Lavender, fontSize = 12.sp))
      }
    }

    if (action.summary.isNotBlank()) {
      Text(action.summary, style = AdminCaptionStyle.copy(color = SilverWhite, fontWeight = FontWeight.Medium))
    }

    if (!isEditing) {
      AdminActionArgsPreview(action)
    } else if (action.tool == AdminActionExecutor.TOOL_POST_NOTICE) {
      AdminNoticeInlineEditor(
        action = action,
        onCancelEdit = { isEditing = false },
        onSaveEdit = { updated ->
          onSaveEdit(updated)
          isEditing = false
        }
      )
    }

    if (!action.resultMessage.isNullOrBlank()) {
      val msgColor = if (action.status == AdminActionCardStatus.FAILED) CrimsonError else EmeraldSuccess
      Text(text = action.resultMessage, style = AdminCaptionStyle.copy(color = msgColor, fontSize = 13.sp))
    }

    val canInteract = (action.status == AdminActionCardStatus.PENDING || action.status == AdminActionCardStatus.FAILED) && !isEditing
    if (action.status == AdminActionCardStatus.EXECUTING) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ChampagneGold, strokeWidth = 2.dp)
        Text("Applying...", style = AdminCaptionStyle.copy(color = ChampagneGold))
      }
    } else if (canInteract) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        val isOpen = action.tool == AdminActionExecutor.TOOL_OPEN_SCREEN
        Button(
          onClick = onConfirm,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
          modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("admin_action_confirm_btn")
        ) {
          Icon(if (isOpen) Icons.Default.PlayArrow else Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (isOpen) "Open" else "Confirm", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        if (!isOpen) {
          OutlinedButton(
            onClick = { isEditing = true },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)),
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("admin_action_edit_btn")
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Edit", color = ChampagneGold, fontSize = 13.sp)
          }
        }

        TextButton(
          onClick = onCancel,
          modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("admin_action_cancel_btn")
        ) {
          Icon(Icons.Default.Close, contentDescription = null, tint = Lavender, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(2.dp))
          Text("Cancel", color = Lavender, fontSize = 13.sp)
        }
      }
    }
  }
}

@Composable
private fun AdminActionArgsPreview(action: AdminAssistantAction) {
  val args = action.args
  val lines = buildList {
    if (action.tool == AdminActionExecutor.TOOL_POST_NOTICE) {
      val t = AdminActionExecutor.strArg(args, "title", "notice_title")
      val c = AdminActionExecutor.strArg(args, "content", "notice_content", "body")
      val cat = AdminActionExecutor.strArg(args, "category", "notice_category").ifBlank { "general" }
      val urgent = AdminActionExecutor.boolArg(args, "is_urgent", "urgent", defaultValue = false)
      if (t.isNotBlank()) add("Title: $t")
      if (c.isNotBlank()) add("Message: $c")
      add("Category: $cat${if (urgent) " • Urgent" else ""}")
    } else if (action.tool == AdminActionExecutor.TOOL_OPEN_SCREEN) {
      val screen = AdminActionExecutor.strArg(args, "screen", "destination", "target")
      if (screen.isNotBlank()) add("Destination: ${screen.replace('_', ' ')}")
    }
  }
  if (lines.isNotEmpty()) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      lines.forEach { Text(it, style = AdminCaptionStyle.copy(color = Lavender, fontSize = 13.sp)) }
    }
  }
}

@Composable
private fun AdminNoticeInlineEditor(
  action: AdminAssistantAction,
  onCancelEdit: () -> Unit,
  onSaveEdit: (Map<String, Any?>) -> Unit
) {
  val args = action.args
  var title by remember(action.id) { mutableStateOf(AdminActionExecutor.strArg(args, "title", "notice_title")) }
  var content by remember(action.id) { mutableStateOf(AdminActionExecutor.strArg(args, "content", "notice_content", "body")) }
  var category by remember(action.id) { mutableStateOf(AdminActionExecutor.strArg(args, "category", "notice_category").ifBlank { "general" }) }
  var urgent by remember(action.id) { mutableStateOf(AdminActionExecutor.boolArg(args, "is_urgent", "urgent", defaultValue = false)) }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, colors = royalAdminTextFieldColors(), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Notice Content") }, colors = royalAdminTextFieldColors(), modifier = Modifier.fillMaxWidth(), minLines = 2)
    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (general/exam/assignment)") }, colors = royalAdminTextFieldColors(), modifier = Modifier.fillMaxWidth(), singleLine = true)
    Row(verticalAlignment = Alignment.CenterVertically) {
      Checkbox(checked = urgent, onCheckedChange = { urgent = it })
      Text("Mark as Urgent", style = AdminCaptionStyle.copy(color = SilverWhite))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(
        onClick = {
          val updated = args.toMutableMap()
          updated["title"] = title.trim()
          updated["content"] = content.trim()
          updated["category"] = category.trim().ifBlank { "general" }
          updated["is_urgent"] = urgent
          onSaveEdit(updated)
        },
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
      ) { Text("Save Draft") }
      TextButton(onClick = onCancelEdit, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) { Text("Back", color = Lavender) }
    }
  }
}
