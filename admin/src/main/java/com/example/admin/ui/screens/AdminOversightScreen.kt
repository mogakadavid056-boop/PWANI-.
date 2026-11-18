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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.AdminClassItem
import com.example.admin.data.AdminRegisterDetail
import com.example.admin.data.AdminRegisterSummary
import com.example.admin.ui.components.AdminEmptyState
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminSearchField
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.TextMuted
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary
import com.example.admin.util.AdminJsonHelper

@Composable
fun AdminOversightScreen(
  classes: List<AdminClassItem>,
  classSearch: String,
  isClassesLoading: Boolean,
  selectedClass: AdminClassItem?,
  registers: List<AdminRegisterSummary>,
  isRegistersLoading: Boolean,
  selectedRegister: AdminRegisterDetail?,
  isRegisterDetailLoading: Boolean,
  isActionLoading: Boolean,
  onSearchChanged: (String) -> Unit,
  onRefreshClasses: () -> Unit,
  onOpenClass: (AdminClassItem) -> Unit,
  onCloseClass: () -> Unit,
  onOpenRegister: (String) -> Unit,
  onCloseRegisterDetail: () -> Unit,
  onCloseSession: (String) -> Unit,
  onVoidSession: (String, String) -> Unit
) {
  if (selectedRegister != null) {
    AdminRegisterDetailView(
      detail = selectedRegister,
      isActionLoading = isActionLoading,
      onBack = onCloseRegisterDetail,
      onCloseSession = onCloseSession,
      onVoidSession = onVoidSession
    )
    return
  }

  if (selectedClass != null) {
    BackHandler(onBack = onCloseClass)
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onCloseClass) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to classes", tint = GoldPrimary)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(selectedClass.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
          Text("Attendance Registers • Code: ${AdminJsonHelper.orDash(selectedClass.code)}", fontSize = 12.sp, color = GoldLight)
        }
        IconButton(onClick = { onOpenClass(selectedClass) }) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh registers", tint = GoldPrimary)
        }
      }
      if (isRegistersLoading || isRegisterDetailLoading) {
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = GoldPrimary)
      }
      Spacer(modifier = Modifier.height(10.dp))
      if (registers.isEmpty() && !isRegistersLoading) {
        AdminEmptyState(Icons.Default.FactCheck, "No registers yet", "No attendance sessions recorded for this class.")
      } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
          items(registers, key = { it.sessionId }) { reg ->
            val stColor = when (reg.status.lowercase()) {
              "open" -> EmeraldSuccess
              "voided" -> CrimsonError
              else -> GoldLight
            }
            AdminGlassCard(onClick = { onOpenRegister(reg.sessionId) }) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(AdminJsonHelper.orDash(reg.registerNo), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                  Text(AdminJsonHelper.orDash(reg.courseName.ifBlank { reg.title }), fontSize = 14.sp, color = TextPrimary)
                  Text("${AdminJsonHelper.orDash(reg.dateText)} • Present: ${reg.presentCount}", fontSize = 12.sp, color = TextSecondary)
                }
                AdminStatusPill(reg.status.uppercase(), stColor)
              }
            }
          }
        }
      }
    }
    return
  }

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp).testTag("admin_oversight_screen")) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column {
        Text("Classes & Oversight", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Tap any class to audit attendance registers", fontSize = 12.sp, color = TextSecondary)
      }
      IconButton(onClick = onRefreshClasses) {
        Icon(Icons.Default.Refresh, contentDescription = "Refresh classes", tint = GoldPrimary)
      }
    }
    Spacer(modifier = Modifier.height(10.dp))
    AdminSearchField(value = classSearch, onValueChange = onSearchChanged, placeholder = "Search class name, code, or programme...", tag = "admin_class_search")
    if (isClassesLoading) {
      Spacer(modifier = Modifier.height(6.dp))
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = GoldPrimary)
    }
    Spacer(modifier = Modifier.height(10.dp))
    if (classes.isEmpty() && !isClassesLoading) {
      AdminEmptyState(Icons.Default.Class, "No classes found", "No classes matched or RLS setup SQL has not been run yet.")
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        items(classes, key = { it.id }) { cls ->
          AdminGlassCard(onClick = { onOpenClass(cls) }) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text(cls.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                  text = "${AdminJsonHelper.orDash(cls.programmeName)} • Code: ${AdminJsonHelper.orDash(cls.code)}",
                  fontSize = 12.sp,
                  color = TextSecondary
                )
                if (cls.memberCount != null || cls.registerCount != null) {
                  Text(
                    text = "${cls.memberCount ?: 0} members • ${cls.registerCount ?: 0} registers",
                    fontSize = 12.sp,
                    color = TextMuted
                  )
                }
              }
              if (cls.yearOfStudy != null) {
                AdminStatusPill("Yr ${cls.yearOfStudy}", GoldLight)
              }
            }
          }
        }
      }
    }
  }
}
