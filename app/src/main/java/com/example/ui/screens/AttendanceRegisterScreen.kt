package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceRegisterDetail
import com.example.data.AttendanceRegisterRowItem
import com.example.data.ClassUnitItem
import com.example.export.AttendanceRegisterActions
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.JsonHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceRegisterScreen(
  detail: AttendanceRegisterDetail?,
  units: List<ClassUnitItem> = emptyList(),
  canManageRole: Boolean = true,
  canSignAttendanceRole: Boolean = true,
  isLoading: Boolean,
  isActionLoading: Boolean,
  errorMessage: String?,
  successMessage: String? = null,
  onDismissError: () -> Unit,
  onDismissSuccess: () -> Unit = {},
  onCloseSession: () -> Unit,
  onVoidSession: (reason: String) -> Unit,
  onDeleteSession: () -> Unit = {},
  onSignOut: () -> Unit,
  onSignRegister: (signerRole: String, signatureBase64: String) -> Unit,
  onCorrectTime: (recordId: String, newTimeIn: String, newTimeOut: String, reason: String) -> Unit,
  onMarkPresent: (studentId: String, timeIn: String, timeOut: String, reason: String) -> Unit = { _, _, _, _ -> },
  onUpdateDetails: (courseTitle: String, courseCode: String, lecturerName: String, venueText: String, sessionTitle: String) -> Unit = { _, _, _, _, _ -> },
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current
  var showVoidDialog by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  var showEditDetailsDialog by remember { mutableStateOf(false) }
  var signRoleToOpen by remember { mutableStateOf<String?>(null) }
  var rowToCorrect by remember { mutableStateOf<AttendanceRegisterRowItem?>(null) }
  var rowToMarkPresent by remember { mutableStateOf<AttendanceRegisterRowItem?>(null) }
  var localFeedback by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(errorMessage) {
    if (!errorMessage.isNullOrBlank()) {
      localFeedback = errorMessage to true
      snackbarHostState.showSnackbar(errorMessage, actionLabel = "Dismiss", duration = SnackbarDuration.Long)
      onDismissError()
    }
  }
  LaunchedEffect(successMessage) {
    if (!successMessage.isNullOrBlank()) {
      localFeedback = successMessage to false
      snackbarHostState.showSnackbar(successMessage, actionLabel = "OK", duration = SnackbarDuration.Short)
      onDismissSuccess()
    }
  }

  Box(modifier = Modifier.fillMaxSize().testTag("attendance_register_screen")) {
    if (isLoading && detail == null) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ChampagneGold)
      }
    } else if (detail != null) {
      val perms = detail.permissions
      val allowClose = perms.canClose && canManageRole
      val allowVoid = perms.canVoid && canManageRole
      val allowDelete = perms.canDelete && canManageRole
      val allowEditDetails = perms.canEditDetails && canManageRole
      val allowEditRecords = perms.canEditRecords && canManageRole
      val allowSignOut = perms.canSignOut && canSignAttendanceRole
      Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp)
      ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("register_back_btn")) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(JsonHelper.orDash(detail.registerNo), style = CardTitleStyle.copy(color = ChampagneGold))
              Text(JsonHelper.orDash(detail.title), style = CaptionStyle.copy(color = Lavender), maxLines = 1)
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          RegisterStatusChip(detail.status)
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (detail.status.equals("voided", ignoreCase = true)) {
          Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(ButtonRadius)).background(RoyalErrorContainer)
              .border(1.5.dp, ErrorRed, RoundedCornerShape(ButtonRadius)).padding(12.dp).testTag("voided_register_banner")
          ) {
            Column {
              Text("VOIDED REGISTER", fontFamily = MontserratFamily, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ErrorRed)
              if (!detail.voidReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Reason: ${detail.voidReason}", style = CaptionStyle.copy(color = SilverWhite))
              }
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
        }

        if (detail.missingDetails && allowEditDetails) {
          Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(ButtonRadius)).background(RoyalSurfaceHigh)
              .border(1.2.dp, MetallicGold, RoundedCornerShape(ButtonRadius)).padding(12.dp).testTag("missing_details_banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text("Add course and lecturer", style = CardTitleStyle.copy(fontSize = 16.sp, color = ChampagneGold))
              Text("This register is missing unit or lecturer details.", style = CaptionStyle.copy(color = Lavender))
            }
            OutlinedButton(
              onClick = { showEditDetailsDialog = true },
              modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("banner_edit_details_btn"),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.5.dp, MetallicGold),
              colors = ButtonDefaults.outlinedButtonColors(containerColor = RoyalSurface, contentColor = ChampagneGold),
              contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
              Icon(Icons.Default.Edit, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Edit details", fontFamily = MontserratFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ChampagneGold, maxLines = 1, softWrap = false)
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
        }

        RegisterExportActionBar(
          onDownload = {
            val res = AttendanceRegisterActions.downloadToDownloads(context, detail)
            localFeedback = res.fold(
              onSuccess = { msg -> msg to false },
              onFailure = { e -> (e.message ?: "Download failed") to true }
            )
          },
          onPrint = { AttendanceRegisterActions.printRegister(context, detail) },
          onShare = { AttendanceRegisterActions.shareRegister(context, detail) }
        )

        Spacer(modifier = Modifier.height(14.dp))
        RegisterInfoCard(detail, canEditDetails = allowEditDetails, onEditDetails = { showEditDetailsDialog = true })
        Spacer(modifier = Modifier.height(14.dp))

        if (allowClose || allowSignOut || allowVoid || allowDelete) {
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (allowClose) {
              OutlinedButton(
                onClick = onCloseSession, enabled = !isActionLoading,
                modifier = Modifier.height(48.dp).testTag("close_session_btn"),
                shape = RoundedCornerShape(ButtonRadius), border = BorderStroke(1.5.dp, MetallicGold),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = RoyalSurface, contentColor = ChampagneGold, disabledContentColor = ChampagneGold.copy(alpha = 0.5f))
              ) { Text("Close Session", fontFamily = MontserratFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ChampagneGold, maxLines = 1, softWrap = false) }
            }
            if (allowSignOut) {
              OutlinedButton(
                onClick = onSignOut, enabled = !isActionLoading,
                modifier = Modifier.height(48.dp).testTag("sign_out_session_btn"),
                shape = RoundedCornerShape(ButtonRadius), border = BorderStroke(1.5.dp, MetallicGold),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = RoyalSurface, contentColor = ChampagneGold, disabledContentColor = ChampagneGold.copy(alpha = 0.5f))
              ) { Text("Sign Out", fontFamily = MontserratFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ChampagneGold, maxLines = 1, softWrap = false) }
            }
            if (allowVoid) {
              OutlinedButton(
                onClick = { showVoidDialog = true }, enabled = !isActionLoading,
                modifier = Modifier.height(48.dp).testTag("void_session_btn"),
                shape = RoundedCornerShape(ButtonRadius), border = BorderStroke(1.5.dp, ErrorRed),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = RoyalSurface, contentColor = ErrorRed, disabledContentColor = ErrorRed.copy(alpha = 0.5f))
              ) { Text("Void", fontFamily = MontserratFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ErrorRed, maxLines = 1, softWrap = false) }
            }
            if (allowDelete) {
              OutlinedButton(
                onClick = { showDeleteDialog = true }, enabled = !isActionLoading,
                modifier = Modifier.height(48.dp).testTag("delete_register_btn"),
                shape = RoundedCornerShape(ButtonRadius), border = BorderStroke(1.5.dp, ErrorRed),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = RoyalSurface, contentColor = ErrorRed, disabledContentColor = ErrorRed.copy(alpha = 0.5f))
              ) { Text("Delete", fontFamily = MontserratFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ErrorRed, maxLines = 1, softWrap = false) }
            }
          }
          Spacer(modifier = Modifier.height(14.dp))
        }

        RegisterTablePreview(
          rows = detail.rows,
          canEditRecords = allowEditRecords,
          showSignatures = (perms.canManage && canManageRole) || allowEditRecords,
          onCorrectRow = { rowToCorrect = it },
          onMarkPresentRow = { rowToMarkPresent = it }
        )

        Spacer(modifier = Modifier.height(16.dp))
        RegisterSignaturesSection(
          detail = detail,
          onSignPrefect = { signRoleToOpen = "prefect" },
          onSignLecturer = { signRoleToOpen = "lecturer" }
        )
        Spacer(modifier = Modifier.height(64.dp))
      }
    }

    LaunchedEffect(localFeedback) {
      val fb = localFeedback ?: return@LaunchedEffect
      if (fb.first != errorMessage && fb.first != successMessage) {
        snackbarHostState.showSnackbar(fb.first, actionLabel = "OK", duration = SnackbarDuration.Short)
      }
    }

    ClassRepSnackbarHost(
      hostState = snackbarHostState,
      isError = localFeedback?.second ?: true,
      modifier = Modifier.align(Alignment.BottomCenter)
    )
  }

  AttendanceRegisterModals(
    detail = detail,
    units = units,
    isActionLoading = isActionLoading,
    showVoidDialog = showVoidDialog,
    showDeleteDialog = showDeleteDialog,
    showEditDetailsDialog = showEditDetailsDialog,
    signRoleToOpen = signRoleToOpen,
    rowToCorrect = rowToCorrect,
    rowToMarkPresent = rowToMarkPresent,
    onDismissVoid = { showVoidDialog = false },
    onConfirmVoid = { onVoidSession(it); showVoidDialog = false },
    onDismissDelete = { showDeleteDialog = false },
    onConfirmDelete = { onDeleteSession(); showDeleteDialog = false },
    onDismissEditDetails = { showEditDetailsDialog = false },
    onSaveEditDetails = { ct, cc, lec, ven, st -> onUpdateDetails(ct, cc, lec, ven, st); showEditDetailsDialog = false },
    onDismissSignRole = { signRoleToOpen = null },
    onConfirmSignRole = { role, sig -> onSignRegister(role, sig); signRoleToOpen = null },
    onDismissCorrect = { rowToCorrect = null },
    onConfirmCorrect = { id, tIn, tOut, r -> onCorrectTime(id, tIn, tOut, r); rowToCorrect = null },
    onDismissMarkPresent = { rowToMarkPresent = null },
    onConfirmMarkPresent = { sId, tIn, tOut, r -> onMarkPresent(sId, tIn, tOut, r); rowToMarkPresent = null }
  )
}
