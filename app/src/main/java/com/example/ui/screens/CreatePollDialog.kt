package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.RoyalCardBorderBrush
import com.example.ui.components.royalOutlinedTextFieldColors
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardRadius
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.RoyalSurface
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.SilverWhite

@Composable
fun CreatePollDialog(
  isLoading: Boolean,
  onDismiss: () -> Unit,
  onCreatePoll: (question: String, options: List<String>, duration: String) -> Unit
) {
  var question by remember { mutableStateOf("") }
  val options = remember { mutableStateListOf("Option 1", "Option 2") }
  val durations = listOf("1 hour", "6 hours", "24 hours", "3 days", "7 days")
  var selectedDuration by remember { mutableStateOf("24 hours") }
  var durationDropdownExpanded by remember { mutableStateOf(false) }
  var localError by remember { mutableStateOf<String?>(null) }

  Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
    Surface(
      shape = RoundedCornerShape(CardRadius),
      color = RoyalSurfaceHigh,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, RoyalCardBorderBrush, RoundedCornerShape(CardRadius))
        .testTag("create_poll_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp)
      ) {
        Text("Create Poll", style = SectionHeadingStyle)
        Spacer(modifier = Modifier.height(14.dp))

        if (localError != null) {
          Text(text = localError.orEmpty(), style = CaptionStyle.copy(color = ErrorColor))
          Spacer(modifier = Modifier.height(8.dp))
        }

        // Question field
        OutlinedTextField(
          value = question,
          onValueChange = { question = it; localError = null },
          label = { Text("Poll Question") },
          placeholder = { Text("e.g. When should we hold the revision session?") },
          modifier = Modifier.fillMaxWidth().testTag("poll_question_input"),
          colors = royalOutlinedTextFieldColors(),
          singleLine = false,
          maxLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Options header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Options (${options.size}/10)", style = FieldLabelStyle)
          if (options.size < 10) {
            OutlinedButton(
              onClick = { options.add(""); localError = null },
              shape = RoundedCornerShape(ButtonRadius),
              border = androidx.compose.foundation.BorderStroke(1.dp, MetallicGold.copy(alpha = 0.65f)),
              modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("add_poll_option_btn")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add", style = ButtonTextStyle.copy(fontSize = 14.sp, color = ChampagneGold))
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Option list
        options.forEachIndexed { idx, optVal ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = optVal,
              onValueChange = { newText -> options[idx] = newText; localError = null },
              label = { Text("Option ${idx + 1}") },
              modifier = Modifier.weight(1f).testTag("poll_option_input_$idx"),
              colors = royalOutlinedTextFieldColors(),
              singleLine = true
            )
            if (options.size > 2) {
              IconButton(
                onClick = { options.removeAt(idx); localError = null },
                modifier = Modifier.size(48.dp).testTag("remove_poll_option_$idx")
              ) {
                Icon(
                  Icons.Default.RemoveCircleOutline,
                  contentDescription = "Remove option",
                  tint = ErrorColor,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Duration dropdown
        Text("Duration", style = FieldLabelStyle)
        Spacer(modifier = Modifier.height(6.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(ButtonRadius))
              .background(RoyalSurface)
              .border(1.dp, if (durationDropdownExpanded) MetallicGold else DarkBorder, RoundedCornerShape(ButtonRadius))
              .clickable { durationDropdownExpanded = !durationDropdownExpanded }
              .padding(horizontal = 14.dp, vertical = 14.dp)
              .testTag("poll_duration_dropdown"),
            contentAlignment = Alignment.CenterStart
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(selectedDuration, style = com.example.ui.theme.BodyStyle.copy(fontSize = 15.sp))
              Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = ChampagneGold)
            }
          }

          AnimatedVisibility(visible = durationDropdownExpanded) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(ButtonRadius))
                .background(RoyalSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(ButtonRadius))
            ) {
              durations.forEachIndexed { idx, d ->
                Text(
                  text = d,
                  style = com.example.ui.theme.BodyStyle.copy(
                    color = if (d == selectedDuration) ChampagneGold else SilverWhite,
                    fontWeight = if (d == selectedDuration) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      selectedDuration = d
                      durationDropdownExpanded = false
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                )
                if (idx < durations.lastIndex) HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(ButtonRadius),
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("create_poll_cancel_btn"),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = RoyalSurface, contentColor = Lavender),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkBorder)
          ) {
            Text("Cancel", style = ButtonTextStyle.copy(color = Lavender, fontSize = 14.sp))
          }
          Spacer(modifier = Modifier.width(10.dp))
          val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
          val context = androidx.compose.ui.platform.LocalContext.current
          Button(
            onClick = {
              if (!isOnline) {
                com.example.ui.components.OfflineToastHelper.show(context)
                return@Button
              }
              if (question.isBlank()) {
                localError = "Please enter a question"
                return@Button
              }
              val cleanOpts = options.map { it.trim() }.filter { it.isNotBlank() }
              if (cleanOpts.size < 2) {
                localError = "Please enter at least 2 non-empty options"
                return@Button
              }
              onCreatePoll(question, cleanOpts, selectedDuration)
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(ButtonRadius),
            colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).alpha(if (!isOnline) 0.5f else 1f).testTag("create_poll_submit_btn")
          ) {
            if (isLoading) {
              CircularProgressIndicator(color = Midnight, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
              Text("Create", style = ButtonTextStyle.copy(color = Midnight, fontSize = 14.sp))
            }
          }
        }
      }
    }
  }
}
