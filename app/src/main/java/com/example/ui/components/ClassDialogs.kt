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
  classUnits: List<com.example.data.ClassUnitItem> = emptyList(),
  isUnitsLoading: Boolean = false,
  onDismissStartAttendance: () -> Unit,
  onStartAttendance: (courseId: String?, courseTitle: String, courseCode: String, lecturerName: String, venueText: String, sessionTitle: String, durationMinutes: Int) -> Unit,
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
  onConfirmDeleteSessionWithReason: (String) -> Unit = {},
  registerPendingDelete: com.example.data.AttendanceRegisterListItem? = null,
  onDismissPromptDeleteRegister: () -> Unit = {},
  onConfirmDeleteRegister: (String) -> Unit = {},
  timetableUiState: TimetableUiState,
  onDismissAddEditSchedule: () -> Unit,
  onSaveAddSchedule: (String, Int, String, String, String, String) -> Unit,
  onSaveEditSchedule: (String, String, Int, String, String, String, String) -> Unit,
  onDismissPromptDeleteSchedule: () -> Unit,
  onConfirmDeleteSchedule: () -> Unit,
  onDismissScheduleStatus: () -> Unit = {},
  onSaveScheduleStatus: (String, String, String) -> Unit = { _, _, _ -> },
  materialsUiState: MaterialsUiState,
  onDismissAddEditMaterial: () -> Unit,
  onSaveAddLinkMaterial: (String, String, String, String, String) -> Unit,
  onSaveUploadMaterial: (courseId: String, title: String, description: String, fileName: String, fileSize: Long, mimeType: String, inputStreamProvider: () -> java.io.InputStream) -> Unit = { _, _, _, _, _, _, _ -> },
  onSaveEditMaterial: (materialId: String, title: String, description: String) -> Unit,
  onDismissRegisterUnit: () -> Unit = {},
  onSubmitRegisterUnit: (String, String, String) -> Unit = { _, _, _ -> },
  onDismissPromptDeleteMaterial: () -> Unit,
  onConfirmDeleteMaterial: () -> Unit,
  showCreatePollDialog: Boolean = false,
  isPollActionLoading: Boolean = false,
  onDismissCreatePollDialog: () -> Unit = {},
  onCreatePoll: (question: String, options: List<String>, duration: String) -> Unit = { _, _, _ -> },
  challengesUiState: com.example.ui.ChallengesUiState = com.example.ui.ChallengesUiState(),
  canCreateExam: Boolean = false,
  onDismissCreateChallengeDialog: () -> Unit = {},
  onCreateChallenge: (
    title: String,
    description: String,
    opensAtIso: String,
    closesAtIso: String,
    timeLimitMinutes: Int?,
    questions: List<com.example.data.DraftQuizQuestion>,
    courseId: String?,
    asExam: Boolean,
    shuffleQuestions: Boolean,
    answerFormat: com.example.data.ExamAnswerFormat,
    securityConfig: com.example.data.ExamSecurityConfig
  ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
  onDismissAddQuestionsDialog: () -> Unit = {},
  onAddQuestionsSubmit: (quizId: String, List<com.example.data.DraftQuizQuestion>) -> Unit = { _, _ -> },
  onDismissDeleteChallenge: () -> Unit = {},
  onConfirmDeleteChallenge: () -> Unit = {}
) {
  if (challengesUiState.showCreateDialog) {
    com.example.ui.screens.CreateChallengeDialog(
      approvedUnits = materialsUiState.approvedUnits,
      canCreateExam = canCreateExam,
      initialDraft = challengesUiState.challengeDraftPrefill,
      isLoading = challengesUiState.isActionLoading,
      errorMessage = challengesUiState.errorMessage,
      onDismiss = onDismissCreateChallengeDialog,
      onCreate = onCreateChallenge
    )
  }

  challengesUiState.addQuestionsForQuiz?.let { targetQuiz ->
    com.example.ui.screens.AddQuestionsDialog(
      quiz = targetQuiz,
      isLoading = challengesUiState.isActionLoading,
      errorMessage = challengesUiState.errorMessage,
      onDismiss = onDismissAddQuestionsDialog,
      onSubmit = { qs -> onAddQuestionsSubmit(targetQuiz.quizId, qs) }
    )
  }

  challengesUiState.quizPendingDelete?.let { quiz ->
    ConfirmActionDialog(
      title = "Delete Challenge?",
      message = "Delete \"${quiz.title}\" and all its submitted attempts? This cannot be undone.",
      confirmButtonText = "Delete",
      confirmButtonColor = ErrorRed,
      isLoading = challengesUiState.isActionLoading,
      onConfirm = onConfirmDeleteChallenge,
      onDismiss = onDismissDeleteChallenge
    )
  }

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
      units = classUnits,
      isUnitsLoading = isUnitsLoading,
      isLoading = isAttendanceActionLoading,
      onDismiss = onDismissStartAttendance,
      onStart = { cId, ct, cc, lec, ven, st, dur ->
        onStartAttendance(cId, ct, cc, lec, ven, st, dur)
        onDismissStartAttendance()
      }
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

  sessionPendingDelete?.let { _ ->
    com.example.ui.screens.VoidSessionDialog(
      isLoading = isAttendanceActionLoading,
      onDismiss = onDismissPromptDeleteSession,
      onConfirmVoid = { reason -> onConfirmDeleteSessionWithReason(reason) }
    )
  }

  registerPendingDelete?.let { reg ->
    com.example.ui.screens.DeleteRegisterDialog(
      registerTitle = reg.title.ifBlank { reg.registerNo },
      isLoading = isAttendanceActionLoading,
      onDismiss = onDismissPromptDeleteRegister,
      onConfirmDelete = { reason -> onConfirmDeleteRegister(reason) }
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

  timetableUiState.schedulePendingStatus?.let { schedule ->
    ScheduleStatusDialog(
      schedule = schedule,
      isLoading = timetableUiState.isActionLoading,
      onDismiss = onDismissScheduleStatus,
      onSaveStatus = { status, note -> onSaveScheduleStatus(schedule.id, status, note) }
    )
  }

  if (materialsUiState.showAddDialog) {
    AddEditMaterialDialog(
      initialMaterial = null,
      approvedUnits = materialsUiState.approvedUnits,
      isLoading = materialsUiState.isActionLoading,
      uploadProgress = materialsUiState.uploadProgress,
      onDismiss = onDismissAddEditMaterial,
      onSaveLink = onSaveAddLinkMaterial,
      onSaveUpload = onSaveUploadMaterial,
      onSaveEdit = { _, _, _ -> }
    )
  }

  if (materialsUiState.showRegisterUnitDialog) {
    com.example.ui.screens.RegisterUnitDialog(
      isLoading = materialsUiState.isActionLoading,
      errorMessage = materialsUiState.unitDialogError,
      onDismiss = onDismissRegisterUnit,
      onSubmit = onSubmitRegisterUnit
    )
  }

  materialsUiState.materialPendingEdit?.let { material ->
    AddEditMaterialDialog(
      initialMaterial = material,
      approvedUnits = materialsUiState.approvedUnits,
      isLoading = materialsUiState.isActionLoading,
      onDismiss = onDismissAddEditMaterial,
      onSaveLink = { _, _, _, _, _ -> },
      onSaveUpload = { _, _, _, _, _, _, _ -> },
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
