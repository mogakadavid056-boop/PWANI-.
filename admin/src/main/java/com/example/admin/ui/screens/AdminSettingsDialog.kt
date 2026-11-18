package com.example.admin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.AdminSession
import com.example.admin.data.SupabaseAdminConfig
import com.example.admin.ui.AdminSqlSnippet
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.AdminCardElevated
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary

@Composable
fun AdminSettingsDialog(
  session: AdminSession,
  rpcSqlInstalled: Boolean,
  onRefreshRole: () -> Unit,
  onLogout: () -> Unit,
  onDismiss: () -> Unit
) {
  val clipboard = LocalClipboardManager.current
  var copied by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = AdminCardElevated,
    shape = RoundedCornerShape(18.dp),
    title = {
      Column {
        Text("Admin Account & Database", fontWeight = FontWeight.Bold, color = GoldLight, fontSize = 18.sp)
        Text(session.email, fontSize = 12.sp, color = TextSecondary)
      }
    },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        AdminStatusPill(text = "Shared Supabase: ${SupabaseAdminConfig.hostLabel}", color = EmeraldSuccess)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = if (rpcSqlInstalled) {
            "All optional Admin RPCs are installed on Supabase."
          } else {
            "To enable full KPI stats, Schools/Programmes editing, and university-wide Class browsing, tap 'Copy SQL Setup' below, open Supabase Dashboard -> SQL Editor, paste, and click Run."
          },
          fontSize = 13.sp,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
          onClick = {
            clipboard.setText(AnnotatedString(AdminSqlSnippet.SETUP_SQL))
            copied = true
          },
          modifier = Modifier.fillMaxWidth().height(46.dp),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (copied) "SQL Copied to Clipboard!" else "Copy Supabase SQL Setup", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = onRefreshRole,
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = GoldLight)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Verify Role", fontSize = 12.sp, color = TextPrimary)
          }
          OutlinedButton(
            onClick = {
              onDismiss()
              onLogout()
            },
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp), tint = CrimsonError)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Sign Out", fontSize = 12.sp, color = CrimsonError)
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done", color = GoldPrimary, fontWeight = FontWeight.Bold)
      }
    }
  )
}
