package com.example.admin.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.AdminNoticeItem
import com.example.admin.notifications.AdminNotificationTimeFormatter
import com.example.admin.ui.components.AdminConfirmDialog
import com.example.admin.ui.components.AdminEmptyState
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminOptionPickerDialog
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.AdminBorder
import com.example.admin.ui.theme.AdminCardElevated
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.InfoBlue
import com.example.admin.ui.theme.TextMuted
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary

@Composable
fun AdminMonitorNoticesTab(
  notices: List<AdminNoticeItem>,
  isActionLoading: Boolean,
  onSaveNotice: (String?, String, String, String, Boolean, Boolean) -> Unit,
  onDeleteNotice: (String) -> Unit
) {
  var editing by remember { mutableStateOf<AdminNoticeItem?>(null) }
  var showCreate by remember { mutableStateOf(false) }
  var deleteTarget by remember { mutableStateOf<AdminNoticeItem?>(null) }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("Class Notices (${notices.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldLight)
      Button(
        onClick = { editing = null; showCreate = true },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Post", fontWeight = FontWeight.Bold)
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    if (notices.isEmpty()) {
      AdminEmptyState(Icons.Default.Campaign, "No notices yet", "Tap Post to share an announcement with this class.")
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        items(notices, key = { it.id }) { n ->
          AdminGlassCard(borderColor = if (n.isUrgent) CrimsonError.copy(alpha = 0.7f) else AdminBorder) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
              Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  AdminStatusPill(n.category.uppercase(), InfoBlue)
                  if (n.isUrgent) AdminStatusPill("URGENT", CrimsonError)
                  if (n.isPinned) AdminStatusPill("PINNED", GoldPrimary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(n.title.ifBlank { "Notice" }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                if (n.content.isNotBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(n.content, fontSize = 13.sp, color = TextSecondary)
                }
                val ts = AdminNotificationTimeFormatter.formatRelative(n.createdAt)
                if (ts.isNotBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(ts, fontSize = 11.sp, color = TextMuted)
                }
              }
              Row {
                IconButton(onClick = { editing = n; showCreate = true }) {
                  Icon(Icons.Default.Edit, contentDescription = "Edit notice", tint = GoldLight, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { deleteTarget = n }) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete notice", tint = CrimsonError, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      }
    }
  }

  if (showCreate) {
    NoticeFormDialog(
      existing = editing,
      isActionLoading = isActionLoading,
      onSave = { title, content, cat, urgent, pinned ->
        onSaveNotice(editing?.id, title, content, cat, urgent, pinned)
        showCreate = false
      },
      onDismiss = { showCreate = false }
    )
  }

  deleteTarget?.let { target ->
    AdminConfirmDialog(
      title = "Delete Notice?",
      message = "Delete \"${target.title}\" permanently?",
      confirmLabel = "Delete",
      isDestructive = true,
      isActionLoading = isActionLoading,
      onConfirm = { onDeleteNotice(target.id); deleteTarget = null },
      onDismiss = { deleteTarget = null }
    )
  }
}

@Composable
private fun NoticeFormDialog(
  existing: AdminNoticeItem?,
  isActionLoading: Boolean,
  onSave: (String, String, String, Boolean, Boolean) -> Unit,
  onDismiss: () -> Unit
) {
  var title by remember { mutableStateOf(existing?.title.orEmpty()) }
  var content by remember { mutableStateOf(existing?.content.orEmpty()) }
  var category by remember { mutableStateOf(existing?.category?.ifBlank { "general" } ?: "general") }
  var urgent by remember { mutableStateOf(existing?.isUrgent ?: false) }
  var pinned by remember { mutableStateOf(existing?.isPinned ?: false) }
  var showCatPicker by remember { mutableStateOf(false) }

  val fieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = AdminSurface, unfocusedContainerColor = AdminSurface,
    focusedBorderColor = GoldPrimary, unfocusedBorderColor = AdminBorder,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
  )

  AlertDialog(
    onDismissRequest = { if (!isActionLoading) onDismiss() },
    containerColor = AdminCardElevated,
    shape = RoundedCornerShape(16.dp),
    title = { Text(if (existing == null) "Post Notice" else "Edit Notice", fontWeight = FontWeight.Bold, color = GoldLight) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Message") }, maxLines = 4, colors = fieldColors, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { showCatPicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
          Text("Category: ${category.uppercase()}", color = GoldLight)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("Mark as Urgent", color = TextPrimary, fontSize = 14.sp)
          Switch(checked = urgent, onCheckedChange = { urgent = it }, colors = SwitchDefaults.colors(checkedTrackColor = CrimsonError))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("Pin to top", color = TextPrimary, fontSize = 14.sp)
          Switch(checked = pinned, onCheckedChange = { pinned = it }, colors = SwitchDefaults.colors(checkedTrackColor = GoldPrimary))
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(title.trim(), content.trim(), category, urgent, pinned) },
        enabled = !isActionLoading && title.isNotBlank() && content.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) { Text("Save", fontWeight = FontWeight.Bold) }
    },
    dismissButton = { TextButton(onClick = onDismiss, enabled = !isActionLoading) { Text("Cancel", color = TextSecondary) } }
  )

  if (showCatPicker) {
    AdminOptionPickerDialog(
      title = "Select Category",
      options = listOf("general", "exam", "assignment", "timetable", "urgent"),
      selectedPredicate = { it == category },
      labelProvider = { it.uppercase() },
      onSelect = { category = it; showCatPicker = false },
      onDismiss = { showCatPicker = false }
    )
  }
}
