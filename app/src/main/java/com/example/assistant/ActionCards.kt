package com.example.assistant

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
import com.example.ui.components.royalOutlinedTextFieldColors
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardRadius
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SilverWhite
import com.example.ui.theme.SuccessColor

@Composable
fun ActionProposalCard(
  action: AssistantAction,
  onConfirm: () -> Unit,
  onSaveEdit: (Map<String, Any?>) -> Unit,
  onCancel: () -> Unit
) {
  var isEditing by remember(action.id) { mutableStateOf(false) }

  val borderColor = when (action.status) {
    ActionCardStatus.DONE -> SuccessColor.copy(alpha = 0.6f)
    ActionCardStatus.FAILED -> ErrorColor.copy(alpha = 0.65f)
    ActionCardStatus.CANCELLED -> DarkBorder.copy(alpha = 0.45f)
    else -> MetallicGold.copy(alpha = 0.65f)
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(CardRadius))
      .background(RoyalSurfaceHigh)
      .border(1.dp, borderColor, RoundedCornerShape(CardRadius))
      .padding(12.dp)
      .testTag("assistant_action_card_${action.tool}"),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = toolBadgeLabel(action.tool),
        style = FieldLabelStyle.copy(fontSize = 12.sp, color = ChampagneGold)
      )
      if (action.status == ActionCardStatus.DONE) {
        Text("Confirmed", style = CaptionStyle.copy(color = SuccessColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
      } else if (action.status == ActionCardStatus.CANCELLED) {
        Text("Cancelled", style = CaptionStyle.copy(color = Lavender, fontSize = 12.sp))
      }
    }

    if (action.summary.isNotBlank()) {
      Text(
        text = action.summary,
        style = CaptionStyle.copy(color = SilverWhite, fontWeight = FontWeight.Medium)
      )
    }

    if (!isEditing) {
      ActionArgsPreview(action)
    } else {
      ActionInlineEditor(
        action = action,
        onCancelEdit = { isEditing = false },
        onSaveEdit = { updatedArgs ->
          onSaveEdit(updatedArgs)
          isEditing = false
        }
      )
    }

    if (!action.resultMessage.isNullOrBlank()) {
      val msgColor = if (action.status == ActionCardStatus.FAILED) ErrorColor else SuccessColor
      Text(text = action.resultMessage, style = CaptionStyle.copy(color = msgColor, fontSize = 13.sp))
    }

    val isEditable = action.tool != ActionExecutor.TOOL_OPEN_SCREEN && action.tool != ActionExecutor.TOOL_CHALLENGE_DRAFT
    val canInteract = (action.status == ActionCardStatus.PENDING || action.status == ActionCardStatus.FAILED) && !isEditing

    if (action.status == ActionCardStatus.EXECUTING) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ChampagneGold, strokeWidth = 2.dp)
        Text("Applying...", style = CaptionStyle.copy(color = ChampagneGold))
      }
    } else if (canInteract) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        val confirmLabel = when (action.tool) {
          ActionExecutor.TOOL_OPEN_SCREEN -> "Open"
          ActionExecutor.TOOL_CHALLENGE_DRAFT -> "Open Draft"
          else -> "Confirm"
        }
        Button(
          onClick = onConfirm,
          shape = RoundedCornerShape(ButtonRadius),
          colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
          modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("action_confirm_btn")
        ) {
          Icon(
            if (action.tool == ActionExecutor.TOOL_OPEN_SCREEN) Icons.Default.PlayArrow else Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(confirmLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        if (isEditable) {
          OutlinedButton(
            onClick = { isEditing = true },
            shape = RoundedCornerShape(ButtonRadius),
            border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.55f)),
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("action_edit_btn")
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Edit", color = ChampagneGold, fontSize = 13.sp)
          }
        }

        TextButton(
          onClick = onCancel,
          modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("action_cancel_btn")
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
private fun ActionArgsPreview(action: AssistantAction) {
  val args = action.args
  val lines = buildList {
    when (action.tool) {
      ActionExecutor.TOOL_POST_NOTICE -> {
        val t = ActionExecutor.strArg(args, "title", "notice_title")
        val c = ActionExecutor.strArg(args, "content", "notice_content", "body")
        val cat = ActionExecutor.strArg(args, "category", "notice_category").ifBlank { "general" }
        val urgent = ActionExecutor.boolArg(args, "is_urgent", "urgent", defaultValue = false)
        if (t.isNotBlank()) add("Title: $t")
        if (c.isNotBlank()) add("Message: $c")
        add("Category: $cat${if (urgent) " • Urgent" else ""}")
      }
      ActionExecutor.TOOL_CREATE_POLL -> {
        val q = ActionExecutor.strArg(args, "question", "poll_question", "title")
        val opts = ActionExecutor.stringListArg(args, "options", "poll_options")
        val hrs = ActionExecutor.intArg(args, "hours_open", "duration_hours", "hours", defaultValue = 24)
        if (q.isNotBlank()) add("Question: $q")
        if (opts.isNotEmpty()) add("Options: ${opts.joinToString(" | ")}")
        add("Duration: ${hrs}h")
      }
      ActionExecutor.TOOL_START_ATTENDANCE -> {
        val t = ActionExecutor.strArg(args, "session_title", "title").ifBlank { "Class Attendance" }
        val mins = ActionExecutor.intArg(args, "duration_minutes", "minutes", defaultValue = 15)
        val sType = ActionExecutor.strArg(args, "session_type", "type")
        val venue = ActionExecutor.strArg(args, "venue", "venue_text", "room")
        val typePart = if (sType.isNotBlank()) " • ${sType.uppercase()}" else ""
        add("Session: $t$typePart • $mins min")
        if (venue.isNotBlank()) add("Venue: $venue")
      }
      ActionExecutor.TOOL_TIMETABLE_ENTRY -> {
        val t = ActionExecutor.strArg(args, "title", "course_title", "subject")
        val d = ActionExecutor.parseDayOfWeek(args["day_of_week"] ?: args["day"])
        val dayName = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").getOrElse(d - 1) { "Day $d" }
        val st = ActionExecutor.strArg(args, "start_time", "start")
        val et = ActionExecutor.strArg(args, "end_time", "end")
        val r = ActionExecutor.strArg(args, "room", "venue")
        if (t.isNotBlank()) add("Class: $t")
        add("$dayName • $st - $et${if (r.isNotBlank()) " • $r" else ""}")
      }
      ActionExecutor.TOOL_CHALLENGE_DRAFT -> {
        val t = ActionExecutor.strArg(args, "title", "quiz_title")
        val qCount = (args["questions"] as? List<*>)?.size ?: 0
        if (t.isNotBlank()) add("Title: $t")
        add("$qCount draft question(s) — opens editor before publishing")
      }
      ActionExecutor.TOOL_OPEN_SCREEN -> {
        val screen = ActionExecutor.strArg(args, "screen", "destination", "target")
        if (screen.isNotBlank()) add("Destination: ${screen.replace('_', ' ')}")
      }
    }
  }
  if (lines.isNotEmpty()) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      lines.forEach { Text(it, style = CaptionStyle.copy(color = Lavender, fontSize = 13.sp)) }
    }
  }
}

@Composable
private fun ActionInlineEditor(
  action: AssistantAction,
  onCancelEdit: () -> Unit,
  onSaveEdit: (Map<String, Any?>) -> Unit
) {
  val args = action.args
  var f1 by remember(action.id) {
    mutableStateOf(
      when (action.tool) {
        ActionExecutor.TOOL_POST_NOTICE -> ActionExecutor.strArg(args, "title", "notice_title")
        ActionExecutor.TOOL_CREATE_POLL -> ActionExecutor.strArg(args, "question", "poll_question", "title")
        ActionExecutor.TOOL_START_ATTENDANCE -> ActionExecutor.strArg(args, "session_title", "title").ifBlank { "Class Attendance" }
        ActionExecutor.TOOL_TIMETABLE_ENTRY -> ActionExecutor.strArg(args, "title", "course_title", "subject")
        else -> ""
      }
    )
  }
  var f2 by remember(action.id) {
    mutableStateOf(
      when (action.tool) {
        ActionExecutor.TOOL_POST_NOTICE -> ActionExecutor.strArg(args, "content", "notice_content", "body")
        ActionExecutor.TOOL_CREATE_POLL -> ActionExecutor.stringListArg(args, "options", "poll_options").joinToString("\n")
        ActionExecutor.TOOL_START_ATTENDANCE -> ActionExecutor.intArg(args, "duration_minutes", "minutes", defaultValue = 15).toString()
        ActionExecutor.TOOL_TIMETABLE_ENTRY -> ActionExecutor.strArg(args, "start_time", "start")
        else -> ""
      }
    )
  }
  var f3 by remember(action.id) {
    mutableStateOf(
      when (action.tool) {
        ActionExecutor.TOOL_POST_NOTICE -> ActionExecutor.strArg(args, "category", "notice_category").ifBlank { "general" }
        ActionExecutor.TOOL_START_ATTENDANCE -> ActionExecutor.strArg(args, "venue", "venue_text", "room")
        ActionExecutor.TOOL_TIMETABLE_ENTRY -> ActionExecutor.strArg(args, "end_time", "end")
        else -> ""
      }
    )
  }
  var urgentFlag by remember(action.id) {
    mutableStateOf(ActionExecutor.boolArg(args, "is_urgent", "urgent", defaultValue = false))
  }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedTextField(
      value = f1,
      onValueChange = { f1 = it },
      label = {
        Text(
          when (action.tool) {
            ActionExecutor.TOOL_CREATE_POLL -> "Poll Question"
            ActionExecutor.TOOL_START_ATTENDANCE -> "Session Title"
            else -> "Title"
          }
        )
      },
      colors = royalOutlinedTextFieldColors(),
      modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
      value = f2,
      onValueChange = { f2 = it },
      label = {
        Text(
          when (action.tool) {
            ActionExecutor.TOOL_POST_NOTICE -> "Notice Content"
            ActionExecutor.TOOL_CREATE_POLL -> "Options (one per line)"
            ActionExecutor.TOOL_START_ATTENDANCE -> "Duration (minutes)"
            else -> "Start Time (HH:MM)"
          }
        )
      },
      colors = royalOutlinedTextFieldColors(),
      modifier = Modifier.fillMaxWidth(),
      minLines = if (action.tool == ActionExecutor.TOOL_POST_NOTICE || action.tool == ActionExecutor.TOOL_CREATE_POLL) 2 else 1
    )
    if (action.tool != ActionExecutor.TOOL_CREATE_POLL) {
      OutlinedTextField(
        value = f3,
        onValueChange = { f3 = it },
        label = {
          Text(
            when (action.tool) {
              ActionExecutor.TOOL_POST_NOTICE -> "Category (general/exam/assignment)"
              ActionExecutor.TOOL_START_ATTENDANCE -> "Venue"
              else -> "End Time (HH:MM)"
            }
          )
        },
        colors = royalOutlinedTextFieldColors(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )
    }
    if (action.tool == ActionExecutor.TOOL_POST_NOTICE) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = urgentFlag, onCheckedChange = { urgentFlag = it })
        Text("Mark as Urgent", style = CaptionStyle.copy(color = SilverWhite))
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(
        onClick = {
          val updated = args.toMutableMap()
          when (action.tool) {
            ActionExecutor.TOOL_POST_NOTICE -> {
              updated["title"] = f1.trim()
              updated["content"] = f2.trim()
              updated["category"] = f3.trim().ifBlank { "general" }
              updated["is_urgent"] = urgentFlag
              updated["urgent"] = urgentFlag
            }
            ActionExecutor.TOOL_CREATE_POLL -> {
              updated["question"] = f1.trim()
              updated["options"] = f2.lines().map { it.trim() }.filter { it.isNotBlank() }
            }
            ActionExecutor.TOOL_START_ATTENDANCE -> {
              updated["session_title"] = f1.trim().ifBlank { "Class Attendance" }
              updated["title"] = updated["session_title"]
              val parsedMins = f2.trim().toIntOrNull()?.coerceIn(1, 180) ?: 15
              updated["duration_minutes"] = parsedMins
              updated["minutes"] = parsedMins
              updated["venue"] = f3.trim()
            }
            ActionExecutor.TOOL_TIMETABLE_ENTRY -> {
              updated["title"] = f1.trim()
              updated["start_time"] = f2.trim()
              updated["end_time"] = f3.trim()
            }
          }
          onSaveEdit(updated)
        },
        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
      ) {
        Text("Save Draft")
      }
      TextButton(onClick = onCancelEdit, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
        Text("Back", color = Lavender)
      }
    }
  }
}

private fun toolBadgeLabel(tool: String): String = when (tool) {
  ActionExecutor.TOOL_POST_NOTICE -> "PROPOSED NOTICE"
  ActionExecutor.TOOL_CREATE_POLL -> "PROPOSED POLL"
  ActionExecutor.TOOL_START_ATTENDANCE -> "PROPOSED ATTENDANCE SESSION"
  ActionExecutor.TOOL_TIMETABLE_ENTRY -> "PROPOSED TIMETABLE ENTRY"
  ActionExecutor.TOOL_CHALLENGE_DRAFT -> "PROPOSED CHALLENGE DRAFT"
  ActionExecutor.TOOL_OPEN_SCREEN -> "NAVIGATION"
  else -> "PROPOSED ACTION"
}
