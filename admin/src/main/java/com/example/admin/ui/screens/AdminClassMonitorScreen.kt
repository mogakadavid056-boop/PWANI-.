package com.example.admin.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.assistant.AdminAssistantSparkleButton
import com.example.admin.data.AdminUserItem
import com.example.admin.ui.AdminClassMonitorState
import com.example.admin.ui.AdminClassMonitorViewModel
import com.example.admin.ui.AdminMonitorExtraState
import com.example.admin.ui.AdminMonitorExtraViewModel
import com.example.admin.ui.MonitorTab
import com.example.admin.ui.components.AdminConfirmDialog
import com.example.admin.ui.components.AdminFeedbackBanner
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminNotificationBellButton
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.AdminBorder
import com.example.admin.ui.theme.AdminCaptionStyle
import com.example.admin.ui.theme.AdminNavLabelStyle
import com.example.admin.ui.theme.AdminSectionHeadingStyle
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.ChampagneGold
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.InfoBlue
import com.example.admin.ui.theme.Lavender
import com.example.admin.ui.theme.MontserratFamily
import com.example.admin.ui.theme.PlayfairDisplayFamily
import com.example.admin.ui.theme.TextSecondary
import com.example.admin.util.AdminJsonHelper
import java.net.URLEncoder

@Composable
fun AdminClassMonitorScreen(
  monitorState: AdminClassMonitorState,
  extraState: AdminMonitorExtraState,
  allUsers: List<AdminUserItem>,
  unreadNotifications: Int,
  isOnline: Boolean = true,
  monitorVm: AdminClassMonitorViewModel,
  extraVm: AdminMonitorExtraViewModel,
  onOpenClassRisk: (String, String) -> Unit,
  onOpenAssistant: () -> Unit = {},
  onOpenNotifications: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  var confirmRegen by remember { mutableStateOf(false) }

  val ov = monitorState.overview
  val className = ov?.className?.ifBlank { "Class Monitor" } ?: "Class Monitor"
  val verifiedLecturers = remember(monitorState.lecturers, allUsers) {
    monitorState.lecturers.ifEmpty { allUsers.filter { it.isLecturer } }
  }

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp).testTag("admin_class_monitor_screen")) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to classes", tint = ChampagneGold)
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(className, style = AdminSectionHeadingStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val sub = listOfNotNull(
          ov?.schoolName?.takeIf { it.isNotBlank() },
          ov?.programmeName?.takeIf { it.isNotBlank() },
          ov?.yearOfStudy?.let { "Yr $it" }
        ).joinToString(" • ")
        if (sub.isNotBlank()) Text(sub, style = AdminCaptionStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      AdminAssistantSparkleButton(isOnline = isOnline, onClick = onOpenAssistant)
      IconButton(onClick = { monitorVm.refreshCurrentClass(); extraVm.refreshAll() }, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Default.Refresh, contentDescription = "Refresh monitor", tint = ChampagneGold)
      }
      AdminNotificationBellButton(unreadCount = unreadNotifications, onClick = onOpenNotifications)
    }

    (monitorState.errorMessage ?: extraState.errorMessage)?.let { msg ->
      Spacer(modifier = Modifier.height(6.dp))
      AdminFeedbackBanner(message = msg, isError = true) { monitorVm.clearError(); extraVm.clearError() }
    }
    (monitorState.successMessage ?: extraState.successMessage)?.let { msg ->
      Spacer(modifier = Modifier.height(6.dp))
      AdminFeedbackBanner(message = msg, isError = false) { monitorVm.clearSuccess(); extraVm.clearSuccess() }
    }

    if (monitorState.isLoading || extraState.isLoading) {
      Spacer(modifier = Modifier.height(4.dp))
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = GoldPrimary)
    }

    if (ov != null) {
      Spacer(modifier = Modifier.height(8.dp))
      AdminGlassCard {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          AdminStatusPill("${ov.studentCount} Students", GoldPrimary)
          AdminStatusPill("${ov.lecturerCount} Lecturers", InfoBlue)
          AdminStatusPill("${ov.prefectCount} Prefects", EmeraldSuccess)
          AdminStatusPill("${ov.unitCount} Units", GoldLight)
          AdminStatusPill("${ov.materialCount} Materials", TextSecondary)
          AdminStatusPill("${ov.sessionCount} Sessions", GoldLight)
        }
        if (ov.joinCode.isNotBlank()) {
          Spacer(modifier = Modifier.height(10.dp))
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
              Text("Class Join Code", style = AdminCaptionStyle)
              Text(AdminJsonHelper.orDash(ov.joinCode), fontFamily = PlayfairDisplayFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ChampagneGold, letterSpacing = 2.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
              val shareText = "Join $className on ClassRep. Open the app, choose Join with a class code and enter: ${ov.joinCode}"
              IconButton(
                onClick = {
                  clipboard.setText(AnnotatedString(ov.joinCode))
                  Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(48.dp)
              ) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", tint = ChampagneGold, modifier = Modifier.size(19.dp)) }
              IconButton(
                onClick = {
                  val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }
                  context.startActivity(Intent.createChooser(intent, "Share class code"))
                },
                modifier = Modifier.size(48.dp)
              ) { Icon(Icons.Default.Share, contentDescription = "Share code", tint = ChampagneGold, modifier = Modifier.size(19.dp)) }
              IconButton(
                onClick = {
                  val url = "https://wa.me/?text=${URLEncoder.encode(shareText, "UTF-8")}"
                  runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                },
                modifier = Modifier.size(48.dp)
              ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "WhatsApp", tint = EmeraldSuccess, modifier = Modifier.size(19.dp)) }
              OutlinedButton(
                onClick = { confirmRegen = true },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AdminBorder),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.Sync, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Change", fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = ChampagneGold)
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      MonitorTab.entries.forEach { tab ->
        val badge = if (tab == MonitorTab.MEMBERS && monitorState.pendingLecturers.isNotEmpty()) " (${monitorState.pendingLecturers.size})" else ""
        FilterChip(
          selected = monitorState.activeTab == tab,
          onClick = { monitorVm.selectTab(tab) },
          modifier = Modifier.defaultMinSize(minHeight = 48.dp),
          label = { Text("${tab.label}$badge", style = AdminNavLabelStyle) },
          border = FilterChipDefaults.filterChipBorder(enabled = true, selected = monitorState.activeTab == tab, borderColor = AdminBorder, selectedBorderColor = ChampagneGold),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ChampagneGold, selectedLabelColor = DarkTextOnGold,
            containerColor = AdminSurface, labelColor = Lavender
          )
        )
      }
    }
    Spacer(modifier = Modifier.height(8.dp))

    when (monitorState.activeTab) {
      MonitorTab.NOTICES -> AdminMonitorNoticesTab(monitorState.notices, monitorState.isActionLoading, monitorVm::saveNotice, monitorVm::deleteNotice)
      MonitorTab.MEMBERS -> AdminMonitorMembersTab(
        members = monitorState.members,
        pendingLecturers = monitorState.pendingLecturers,
        search = monitorState.memberSearch,
        isActionLoading = monitorState.isActionLoading,
        onSearchChanged = monitorVm::onMemberSearchChanged,
        onReviewJoinRequest = monitorVm::reviewJoinRequest,
        onTogglePrefect = monitorVm::togglePrefect,
        onRemoveMember = monitorVm::removeMember
      )
      MonitorTab.TIMETABLE -> AdminMonitorTimetableTab(monitorState.schedules, monitorState.isActionLoading, monitorVm::saveSchedule, monitorVm::deleteSchedule)
      MonitorTab.UNITS -> AdminMonitorUnitsTab(monitorState.units, verifiedLecturers, monitorState.isActionLoading, monitorVm::registerUnit, monitorVm::setUnitLecturer)
      MonitorTab.MATERIALS -> AdminMonitorMaterialsTab(
        units = monitorState.units,
        materials = monitorState.materials,
        uploadProgress = monitorState.uploadProgress,
        isActionLoading = monitorState.isActionLoading,
        onAddLink = monitorVm::addLinkMaterial,
        onUploadFile = monitorVm::uploadFileMaterial,
        onUpdateMaterial = monitorVm::updateMaterial,
        onDeleteMaterial = monitorVm::deleteMaterial,
        onDownloadMaterial = monitorVm::downloadMaterial
      )
      MonitorTab.ATTENDANCE -> AdminMonitorAttendanceTab(
        classId = monitorState.selectedClassId.orEmpty(),
        className = className,
        units = monitorState.units,
        extraState = extraState,
        extraVm = extraVm,
        onOpenClassRisk = { onOpenClassRisk(monitorState.selectedClassId.orEmpty(), className) }
      )
      MonitorTab.EXAMS -> AdminMonitorExamsTab(className = className, extraState = extraState, extraVm = extraVm)
      MonitorTab.POLLS -> AdminMonitorPollsTab(extraState, extraVm)
      MonitorTab.GROUPS -> AdminMonitorGroupsTab(extraState, extraVm)
    }
  }

  if (confirmRegen) {
    AdminConfirmDialog(
      title = "Change Class Join Code?",
      message = "The old code will stop working immediately. Students joining later will need the new code.",
      confirmLabel = "Change Code",
      isActionLoading = monitorState.isActionLoading,
      onConfirm = { monitorVm.regenerateJoinCode(); confirmRegen = false },
      onDismiss = { confirmRegen = false }
    )
  }
}
