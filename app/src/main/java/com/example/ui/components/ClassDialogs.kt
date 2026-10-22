package com.example.ui.components

import androidx.compose.runtime.Composable
import com.example.data.AttendanceSessionItem
import com.example.data.NoticeItem
import com.example.ui.MaterialsUiState
import com.example.ui.TimetableUiState
import com.example.ui.screens.AddEditMaterialDialog
import com.example.ui.screens.AddEditScheduleDialog
import com.example.ui.screens.CreateGroupDialog
import com.example.ui.screens.EditClassNameDialog
import com.example.ui.screens.EditNoticeDialog
import com.example.ui.screens.StartAttendanceDialog
import com.example.ui.theme.ErrorRed

@Composable
fun ClassDialogs(
  activeNoticeDetail: NoticeItem?,
  onCloseNoticeDetail: () -> Unit,
  showStartAttendanceDialog: Boolean,
  onDismissStartAttendance: () -> Unit,
  onStartAttendance: (Int) -> Unit,
  isAttendanceActionLoading: Boolean,
  showEditClassNameDialog: Boolean,
  currentClassName: String,
  onDismissEditClassName: () -> Unit,
  onSaveClassName: (String) -> Unit,
  showCreateGroupDialog: Boolean,
  isGroupActionLoading: Boolean,
  onDismissCreateGroup: () -> Unit,
  onCreateGroup: (String, String, Int) -> Unit,
  noticePendingEdit: NoticeItem?,
  onDismissEditNotice: () -> Unit,
  onSaveEditNotice: (String, String, String, String, Boolean, Boolean) -> Unit,
  noticePendingDelete: NoticeItem?,
  onDismissPromptDeleteNotice: () -> Unit,
  onConfirmDeleteNotice: () -> Unit,
  sessionPendingDelete: AttendanceSessionItem?,
  onDismissPromptDeleteSession: () -> Unit,
  onConfirmDeleteSession: () -> Unit,
  timetableUiState: TimetableUiState,
  onDismissAddEditSchedule: () -> Unit,
  onSaveAddSchedule: (String, Int, String, String, String, String) -> Unit,
  onSaveEditSchedule: (String, String, Int, String, String, String, String) -> Unit,
  onDismissPromptDeleteSchedule: () -> Unit,
  onConfirmDeleteSchedule: () -> Unit,
  materialsUiState: MaterialsUiState,
  onDismissAddEditMaterial: () -> Unit,
  onSaveAddLinkMaterial: (String, String, String, String) -> Unit,
  onSaveUploadMaterial: (title: String, description: String, fileName: String, fileSize: Long, mimeType: String, inputStreamProvider: () -> java.io.InputStream) -> Unit = { _, _, _, _, _, _ -> },
  onSaveEditMaterial: (materialId: String, title: String, description: String) -> Unit,
  onDismissPromptDeleteMaterial: () -> Unit,
  onConfirmDeleteMaterial: () -> Unit,
  showCreatePollDialog: Boolean = false,
  isPollActionLoading: Boolean = false,
  onDismissCreatePollDialog: () -> Unit = {},
  onCreatePoll: (question: String, options: List<String>, duration: String) -> Unit = { _, _, _ -> }
) {
  if (showCreatePollDialog) {
    com.example.ui.screens.CreatePollDialog(
      isLoading = isPollActionLoading,
      onDismiss = onDismissCreatePollDialog,
      onCreatePoll = onCreatePoll
    )
  }

  if (activeNoticeDetail != null) {
    NoticeDetailDialog(notice = activeNoticeDetail, onDismiss = onCloseNoticeDetail)
  }

  if (showStartAttendanceDialog) {
    StartAttendanceDialog(
      isLoading = isAttendanceActionLoading,
      onDismiss = onDismissStartAttendance,
      onStart = { duration -> onStartAttendance(duration); onDismissStartAttendance() }
    )
  }

  if (showEditClassNameDialog) {
    EditClassNameDialog(
      currentName = currentClassName,
      onDismiss = onDismissEditClassName,
      onSave = { newName -> onSaveClassName(newName); onDismissEditClassName() }
    )
  }

  if (showCreateGroupDialog) {
    CreateGroupDialog(
      isLoading = isGroupActionLoading,
      onDismiss = onDismissCreateGroup,
      onCreate = { name, desc, size -> onCreateGroup(name, desc, size); onDismissCreateGroup() }
    )
  }

  noticePendingEdit?.let { notice ->
    EditNoticeDialog(
      notice = notice,
      onDismiss = onDismissEditNotice,
      onSave = { title, content, cat, urgent, pinned -> onSaveEditNotice(notice.id, title, content, cat, urgent, pinned) }
    )
  }

  noticePendingDelete?.let { notice ->
    ConfirmActionDialog(
      title = "Delete Notice",
      message = "Are you sure you want to delete \"${notice.title}\"? This action cannot be undone.",
      confirmButtonText = "Delete",
      confirmButtonColor = ErrorRed,
      onConfirm = onConfirmDeleteNotice,
      onDismiss = onDismissPromptDeleteNotice
    )
  }

  sessionPendingDelete?.let {
    ConfirmActionDialog(
      title = "Delete Attendance Session",
      message = "Are you sure you want to delete this attendance session? All records will be removed.",
      confirmButtonText = "Delete",
      confirmButtonColor = ErrorRed,
      isLoading = isAttendanceActionLoading,
      onConfirm = onConfirmDeleteSession,
      onDismiss = onDismissPromptDeleteSession
    )
  }

  if (timetableUiState.showAddDialog) {
    AddEditScheduleDialog(
      initialSchedule = null,
      isLoading = timetableUiState.isActionLoading,
      onDismiss = onDismissAddEditSchedule,
      onSave = { title, day, start, end, room, lecturer ->
        onSaveAddSchedule(title, day, start, end, room, lecturer)
      }
    )
  }

  timetableUiState.schedulePendingEdit?.let { schedule ->
    AddEditScheduleDialog(
      initialSchedule = schedule,
      isLoading = timetableUiState.isActionLoading,
      onDismiss = onDismissAddEditSchedule,
      onSave = { title, day, start, end, room, lecturer ->
        onSaveEditSchedule(schedule.id, title, day, start, end, room, lecturer)
      }
    )
  }

  timetableUiState.schedulePendingDelete?.let { schedule ->
    ConfirmActionDialog(
      title = "Delete Class Schedule",
      message = "Are you sure you want to delete \"${schedule.title}\"? This action cannot be undone.",
      confirmButtonText = "Delete",
      confirmButtonColor = ErrorRed,
      isLoading = timetableUiState.isActionLoading,
      onConfirm = onConfirmDeleteSchedule,
      onDismiss = onDismissPromptDeleteSchedule
    )
  }

  if (materialsUiState.showAddDialog) {
    AddEditMaterialDialog(
      initialMaterial = null,
      isLoading = materialsUiState.isActionLoading,
      uploadProgress = materialsUiState.uploadProgress,
      onDismiss = onDismissAddEditMaterial,
      onSaveLink = onSaveAddLinkMaterial,
      onSaveUpload = onSaveUploadMaterial,
      onSaveEdit = { _, _, _ -> }
    )
  }

  materialsUiState.materialPendingEdit?.let { material ->
    AddEditMaterialDialog(
      initialMaterial = material,
      isLoading = materialsUiState.isActionLoading,
      onDismiss = onDismissAddEditMaterial,
      onSaveLink = { _, _, _, _ -> },
      onSaveUpload = { _, _, _, _, _, _ -> },
      onSaveEdit = onSaveEditMaterial
    )
  }

  materialsUiState.materialPendingDelete?.let { material ->
    ConfirmActionDialog(
      title = "Delete Material",
      message = "Are you sure you want to delete \"${material.title}\"? This action cannot be undone.",
      confirmButtonText = "Delete",
      confirmButtonColor = ErrorRed,
      isLoading = materialsUiState.isActionLoading,
      onConfirm = onConfirmDeleteMaterial,
      onDismiss = onDismissPromptDeleteMaterial
    )
  }
}
