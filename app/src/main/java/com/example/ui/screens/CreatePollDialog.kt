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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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
      shape = RoundedCornerShape(16.dp),
      color = DarkCard,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
        .testTag("create_poll_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp)
      ) {
        Text("Create Poll", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldLight)
        Spacer(modifier = Modifier.height(14.dp))

        if (localError != null) {
          Text(text = localError.orEmpty(), fontSize = 12.sp, color = Color(0xFFEF5350))
          Spacer(modifier = Modifier.height(8.dp))
        }

        // Question field
        OutlinedTextField(
          value = question,
          onValueChange = { question = it; localError = null },
          label = { Text("Poll Question") },
          placeholder = { Text("e.g. When should we hold the revision session?") },
          modifier = Modifier.fillMaxWidth().testTag("poll_question_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = DarkBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          ),
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
          Text("Options (${options.size}/10)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
          if (options.size < 10) {
            OutlinedButton(
              onClick = { options.add(""); localError = null },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.height(32.dp).testTag("add_poll_option_btn")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, tint = GoldLight, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add", fontSize = 11.sp, color = GoldLight)
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
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              ),
              singleLine = true
            )
            if (options.size > 2) {
              IconButton(
                onClick = { options.removeAt(idx); localError = null },
                modifier = Modifier.size(36.dp).testTag("remove_poll_option_$idx")
              ) {
                Icon(
                  Icons.Default.RemoveCircleOutline,
                  contentDescription = "Remove option",
                  tint = Color(0xFFEF5350),
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Duration dropdown
        Text("Duration", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(DarkSurface)
              .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
              .clickable { durationDropdownExpanded = true }
              .padding(horizontal = 14.dp, vertical = 12.dp)
              .testTag("poll_duration_dropdown"),
            contentAlignment = Alignment.CenterStart
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(selectedDuration, fontSize = 14.sp, color = TextPrimary)
              Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = GoldLight)
            }
          }

          DropdownMenu(
            expanded = durationDropdownExpanded,
            onDismissRequest = { durationDropdownExpanded = false },
            modifier = Modifier.background(DarkCard).border(1.dp, DarkBorder)
          ) {
            durations.forEach { d ->
              DropdownMenuItem(
                text = { Text(d, color = TextPrimary) },
                onClick = {
                  selectedDuration = d
                  durationDropdownExpanded = false
                }
              )
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
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("create_poll_cancel_btn"),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = com.example.ui.theme.DarkSurface, contentColor = com.example.ui.theme.TextPrimary),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, com.example.ui.theme.DarkBorder)
          ) {
            Text("Cancel", color = com.example.ui.theme.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = {
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
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = com.example.ui.theme.DarkTextOnGold),
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("create_poll_submit_btn")
          ) {
            if (isLoading) {
              CircularProgressIndicator(color = com.example.ui.theme.DarkTextOnGold, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
              Text("Create", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = com.example.ui.theme.DarkTextOnGold)
            }
          }
        }
      }
    }
  }
}
