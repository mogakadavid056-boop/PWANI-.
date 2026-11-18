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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.AdminCsvExporter
import com.example.admin.data.AdminRegisterDetail
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.AdminCardElevated
import com.example.admin.ui.theme.AmberWarning
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.TextMuted
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary
import com.example.admin.util.AdminJsonHelper

@Composable
fun AdminRegisterDetailView(
  detail: AdminRegisterDetail,
  isActionLoading: Boolean,
  onBack: () -> Unit,
  onCloseSession: (String) -> Unit,
  onVoidSession: (String, String) -> Unit
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current
  var showVoidDialog by remember { mutableStateOf(false) }
  var voidReason by remember { mutableStateOf("") }

  val statusColor = when (detail.status.lowercase()) {
    "open" -> EmeraldSuccess
    "voided" -> CrimsonError
    else -> GoldLight
  }

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldPrimary)
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = AdminJsonHelper.orDash(detail.registerNo),
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Text(AdminJsonHelper.orDash(detail.courseName), fontSize = 12.sp, color = GoldLight)
      }
      IconButton(onClick = { AdminCsvExporter.shareRegisterCsv(context, detail) }) {
        Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = GoldPrimary, modifier = Modifier.size(22.dp))
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    AdminGlassCard {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(AdminJsonHelper.orDash(detail.title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        AdminStatusPill(detail.status.uppercase(), statusColor)
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text("Class: ${AdminJsonHelper.orDash(detail.className)} • Lecturer: ${AdminJsonHelper.orDash(detail.lecturerName)}", fontSize = 12.sp, color = TextSecondary)
      Text("Date: ${AdminJsonHelper.orDash(detail.dateText)} • Time: ${AdminJsonHelper.orDash(detail.timeText)}", fontSize = 12.sp, color = TextSecondary)
      Text("Venue: ${AdminJsonHelper.orDash(detail.venue)} • Present: ${detail.presentCount} / ${detail.total}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = EmeraldSuccess)
      if (detail.voidReason.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text("Void Reason: ${detail.voidReason}", fontSize = 12.sp, color = CrimsonError)
      }
      if (detail.canClose || detail.canVoid) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (detail.canClose) {
            OutlinedButton(
              onClick = { onCloseSession(detail.sessionId) },
              enabled = !isActionLoading,
              modifier = Modifier.weight(1f)
            ) { Text("Close Session", color = GoldLight, fontSize = 12.sp) }
          }
          if (detail.canVoid) {
            OutlinedButton(
              onClick = { showVoidDialog = true },
              enabled = !isActionLoading,
              modifier = Modifier.weight(1f)
            ) { Text("Void Session", color = CrimsonError, fontSize = 12.sp) }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Text("Attendance Rows (${detail.rows.size})", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GoldLight)
    Spacer(modifier = Modifier.height(6.dp))

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(bottom = 24.dp)
    ) {
      items(detail.rows) { row ->
        AdminGlassCard {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
              Text("${row.n}. ${AdminJsonHelper.orDash(row.fullName)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
              Text(
                text = "Adm: ${AdminJsonHelper.orDash(row.admissionNumber)} • In: ${AdminJsonHelper.orDash(row.timeInText)} • Out: ${AdminJsonHelper.orDash(row.timeOutText)}",
                fontSize = 12.sp,
                color = TextMuted
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            AdminStatusPill(
              text = if (row.present) "Present" else "Absent",
              color = if (row.present) EmeraldSuccess else AmberWarning
            )
          }
        }
      }
    }
  }

  if (showVoidDialog) {
    AlertDialog(
      onDismissRequest = { showVoidDialog = false },
      containerColor = AdminCardElevated,
      title = { Text("Void Register", color = CrimsonError, fontWeight = FontWeight.Bold) },
      text = {
        OutlinedTextField(
          value = voidReason,
          onValueChange = { voidReason = it },
          label = { Text("Reason for voiding") },
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onVoidSession(detail.sessionId, voidReason)
            showVoidDialog = false
            voidReason = ""
          },
          enabled = voidReason.isNotBlank() && !isActionLoading,
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonError, contentColor = DarkTextOnGold)
        ) { Text("Void") }
      },
      dismissButton = {
        TextButton(onClick = { showVoidDialog = false }) { Text("Cancel", color = TextSecondary) }
      }
    )
  }
}
