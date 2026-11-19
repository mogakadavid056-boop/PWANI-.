package com.example.admin.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.AdminPollItem
import com.example.admin.ui.AdminMonitorExtraState
import com.example.admin.ui.AdminMonitorExtraViewModel
import com.example.admin.ui.components.AdminConfirmDialog
import com.example.admin.ui.components.AdminEmptyState
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.AdminBorder
import com.example.admin.ui.theme.AdminCardElevated
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.TextMuted
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary

@Composable
fun AdminMonitorPollsTab(
  extraState: AdminMonitorExtraState,
  extraVm: AdminMonitorExtraViewModel
) {
  var showCreate by remember { mutableStateOf(false) }
  var deleteTarget by remember { mutableStateOf<AdminPollItem?>(null) }

  val selected = extraState.selectedPoll
  if (selected != null) {
    BackHandler(onBack = extraVm::closePollDetail)
    val res = extraState.pollResults
    Column(modifier = Modifier.fillMaxSize()) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = extraVm::closePollDetail) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldPrimary)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(selected.question, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
          Text("Total Votes: ${res?.totalVotes ?: selected.totalVotes}", fontSize = 12.sp, color = GoldLight)
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      if (res == null) {
        AdminEmptyState(Icons.Default.Poll, "Loading results", "Fetching live breakdown...")
      } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
          items(res.options, key = { it.id }) { opt ->
            AdminGlassCard {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(opt.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                Text("${opt.votes} (${opt.percentage.toInt()}%)", fontSize = 13.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
              }
              Spacer(modifier = Modifier.height(6.dp))
              LinearProgressIndicator(
                progress = { (opt.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = GoldPrimary
              )
            }
          }
        }
      }
    }
    return
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("Polls (${extraState.polls.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldLight)
      Button(
        onClick = { showCreate = true },
        enabled = !extraState.isActionLoading,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Create Poll", fontWeight = FontWeight.Bold)
      }
    }
    Spacer(modifier = Modifier.height(8.dp))

    if (extraState.polls.isEmpty()) {
      AdminEmptyState(Icons.Default.Poll, "No polls yet", "Create a poll to collect class feedback.")
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        items(extraState.polls, key = { it.id }) { p ->
          AdminGlassCard(onClick = { extraVm.openPoll(p) }) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
              Column(modifier = Modifier.weight(1f)) {
                Text(p.question, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("${p.totalVotes} votes", fontSize = 12.sp, color = TextMuted)
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                AdminStatusPill(if (p.active) "OPEN" else "CLOSED", if (p.active) EmeraldSuccess else GoldLight)
                if (p.active) {
                  IconButton(onClick = { extraVm.closePoll(p.id) }) {
                    Icon(Icons.Default.Lock, contentDescription = "Close poll", tint = GoldLight, modifier = Modifier.size(18.dp))
                  }
                }
                IconButton(onClick = { deleteTarget = p }) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete poll", tint = CrimsonError, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      }
    }
  }

  if (showCreate) {
    CreatePollDialog(extraState.isActionLoading, { q, opts, hrs -> extraVm.createPoll(q, opts, hrs); showCreate = false }) { showCreate = false }
  }
  deleteTarget?.let { target ->
    AdminConfirmDialog("Delete Poll?", "Delete \"${target.question}\" permanently?", "Delete", isDestructive = true,
      isActionLoading = extraState.isActionLoading, onConfirm = { extraVm.deletePoll(target.id); deleteTarget = null }, onDismiss = { deleteTarget = null })
  }
}

@Composable
private fun CreatePollDialog(loading: Boolean, onCreate: (String, List<String>, Int) -> Unit, onDismiss: () -> Unit) {
  var question by remember { mutableStateOf("") }
  val options = remember { mutableStateListOf("", "") }
  var hours by remember { mutableIntStateOf(24) }
  val fc = OutlinedTextFieldDefaults.colors(focusedContainerColor = AdminSurface, unfocusedContainerColor = AdminSurface, focusedBorderColor = GoldPrimary, unfocusedBorderColor = AdminBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)

  AlertDialog(
    onDismissRequest = { if (!loading) onDismiss() }, containerColor = AdminCardElevated, shape = RoundedCornerShape(16.dp),
    title = { Text("Create Poll", fontWeight = FontWeight.Bold, color = GoldLight) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("Question") }, colors = fc, modifier = Modifier.fillMaxWidth())
        options.forEachIndexed { i, opt ->
          OutlinedTextField(value = opt, onValueChange = { options[i] = it }, label = { Text("Option ${i + 1}") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
        }
        if (options.size < 6) {
          TextButton(onClick = { options.add("") }) { Text("+ Add Option", color = GoldPrimary, fontSize = 12.sp) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(1, 6, 24, 72).forEach { h ->
            FilterChip(selected = hours == h, onClick = { hours = h }, label = { Text("${h}h", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPrimary, selectedLabelColor = DarkTextOnGold, containerColor = AdminSurface, labelColor = TextSecondary))
          }
        }
      }
    },
    confirmButton = {
      val validOpts = options.map { it.trim() }.filter { it.isNotBlank() }
      Button(onClick = { onCreate(question.trim(), validOpts, hours) }, enabled = !loading && question.isNotBlank() && validOpts.size >= 2,
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)) { Text("Create") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
  )
}
