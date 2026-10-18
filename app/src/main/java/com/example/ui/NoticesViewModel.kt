package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ClassItem
import com.example.data.NoticeItem
import com.example.data.NoticesService
import com.example.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.data.ClassPrefectInfo

data class NoticesUiState(
  val selectedClass: ClassItem? = null,
  val notices: List<NoticeItem> = emptyList(),
  val readNoticeIds: Set<String> = emptySet(),
  val prefects: List<ClassPrefectInfo> = emptyList(),
  val isRep: Boolean = false,
  val isStaff: Boolean = false,
  val canPostNotice: Boolean = false,
  val isLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val activeNoticeDetail: NoticeItem? = null,
  val noticePendingEdit: NoticeItem? = null,
  val noticePendingDelete: NoticeItem? = null,
  val latestAnnouncements: List<NoticeItem> = emptyList(),
  val isLatestLoading: Boolean = false
)

class NoticesViewModel : ViewModel() {

  private val noticesService = NoticesService()
  private val classesService = com.example.data.ClassesService()

  private val _uiState = MutableStateFlow(NoticesUiState())
  val uiState: StateFlow<NoticesUiState> = _uiState.asStateFlow()

  fun selectClassAndLoad(classItem: ClassItem, session: UserSession?) {
    val isRoleStaff = session?.role?.equals("admin", ignoreCase = true) == true ||
      session?.role?.equals("lecturer", ignoreCase = true) == true
    _uiState.update {
      it.copy(
        selectedClass = classItem,
        notices = emptyList(),
        readNoticeIds = emptySet(),
        isRep = false,
        isStaff = isRoleStaff,
        canPostNotice = isRoleStaff,
        errorMessage = null,
        successMessage = null,
        activeNoticeDetail = null,
        noticePendingEdit = null,
        noticePendingDelete = null
      )
    }
    loadNoticesAndRepStatus(classItem.id, session)
  }

  fun updateSelectedClassName(newName: String) {
    _uiState.update { state ->
      val current = state.selectedClass ?: return@update state
      state.copy(selectedClass = current.copy(name = newName))
    }
  }

  fun refreshNotices(session: UserSession?) {
    val currentClass = _uiState.value.selectedClass ?: return
    loadNoticesAndRepStatus(currentClass.id, session)
  }

  private fun loadNoticesAndRepStatus(classId: String, session: UserSession?) {
    if (session == null || session.accessToken.isBlank()) return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }

      val isRoleStaff = session.role.equals("admin", ignoreCase = true) ||
        session.role.equals("lecturer", ignoreCase = true)

      val noticesResult = noticesService.fetchNotices(classId, session.accessToken)
      val readsResult = noticesService.fetchReadNoticeIds(session.userId, session.accessToken)
      val prefectsResult = classesService.listClassReps(classId, session.accessToken)
      val prefects = prefectsResult.getOrDefault(emptyList())

      var isRep = false
      if (!isRoleStaff) {
        val repFromList = prefects.any { it.userId == session.userId }
        if (repFromList) {
          isRep = true
        } else {
          val repResult = noticesService.checkIsStudentRep(classId, session.userId, session.accessToken)
          isRep = repResult.getOrDefault(false)
        }
      }

      val isStaff = isRoleStaff || isRep

      if (noticesResult.isSuccess) {
        val notices = noticesResult.getOrDefault(emptyList())
        val readIds = readsResult.getOrDefault(emptySet())
        _uiState.update {
          it.copy(
            notices = notices,
            readNoticeIds = readIds,
            prefects = prefects,
            isRep = isRep,
            isStaff = isStaff,
            canPostNotice = isStaff,
            isLoading = false
          )
        }
      } else {
        val error = noticesResult.exceptionOrNull()?.message ?: "Failed to load notices"
        _uiState.update {
          it.copy(
            isLoading = false,
            errorMessage = error,
            prefects = prefects,
            isRep = isRep,
            isStaff = isStaff,
            canPostNotice = isStaff
          )
        }
      }
    }
  }

  fun openNotice(notice: NoticeItem, session: UserSession?) {
    _uiState.update {
      it.copy(activeNoticeDetail = notice, readNoticeIds = it.readNoticeIds + notice.id)
    }
    if (session != null && session.accessToken.isNotBlank() && session.userId.isNotBlank()) {
      viewModelScope.launch {
        noticesService.markNoticeAsRead(notice.id, session.userId, session.accessToken)
      }
    }
  }

  fun closeNoticeDetail() {
    _uiState.update { it.copy(activeNoticeDetail = null) }
  }

  fun setNoticePendingEdit(notice: NoticeItem?) {
    _uiState.update { it.copy(noticePendingEdit = notice) }
  }

  fun setNoticePendingDelete(notice: NoticeItem?) {
    _uiState.update { it.copy(noticePendingDelete = notice) }
  }

  fun editNotice(
    noticeId: String,
    title: String,
    content: String,
    category: String,
    urgent: Boolean,
    pinned: Boolean,
    session: UserSession?,
    onSuccess: () -> Unit
  ) {
    val currentClass = _uiState.value.selectedClass ?: return
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      val result = noticesService.editNotice(
        noticeId = noticeId,
        title = title.trim(),
        content = content.trim(),
        category = category.trim().lowercase(),
        urgent = urgent,
        pinned = pinned,
        accessToken = session.accessToken
      )
      if (result.isSuccess) {
        _uiState.update { it.copy(isActionLoading = false, noticePendingEdit = null) }
        loadNoticesAndRepStatus(currentClass.id, session)
        onSuccess()
      } else {
        val error = result.exceptionOrNull()?.message ?: "Failed to update notice"
        _uiState.update { it.copy(isActionLoading = false, errorMessage = error) }
      }
    }
  }

  fun confirmDeleteNotice(session: UserSession?) {
    val notice = _uiState.value.noticePendingDelete ?: return
    val currentClass = _uiState.value.selectedClass ?: return
    if (session == null || session.accessToken.isBlank()) return

    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, noticePendingDelete = null, errorMessage = null) }
      val result = noticesService.deleteNotice(notice.id, session.accessToken)
      if (result.isSuccess) {
        _uiState.update {
          it.copy(
            isActionLoading = false,
            activeNoticeDetail = if (it.activeNoticeDetail?.id == notice.id) null else it.activeNoticeDetail,
            successMessage = "Notice deleted successfully"
          )
        }
        loadNoticesAndRepStatus(currentClass.id, session)
      } else {
        val error = result.exceptionOrNull()?.message ?: "Failed to delete notice"
        _uiState.update { it.copy(isActionLoading = false, errorMessage = error) }
      }
    }
  }

  fun postNotice(
    title: String,
    content: String,
    category: String,
    urgent: Boolean,
    pinned: Boolean,
    session: UserSession?,
    onSuccess: () -> Unit
  ) {
    val currentClass = _uiState.value.selectedClass ?: return
    if (session == null || session.accessToken.isBlank()) {
      _uiState.update { it.copy(errorMessage = "You must be signed in to post a notice") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      val result = noticesService.createNotice(
        classId = currentClass.id,
        title = title.trim(),
        content = content.trim(),
        category = category.trim().lowercase(),
        urgent = urgent,
        pinned = pinned,
        accessToken = session.accessToken
      )
      if (result.isSuccess) {
        _uiState.update { it.copy(isActionLoading = false) }
        loadNoticesAndRepStatus(currentClass.id, session)
        onSuccess()
      } else {
        val error = result.exceptionOrNull()?.message ?: "Failed to post notice"
        _uiState.update { it.copy(isActionLoading = false, errorMessage = error) }
      }
    }
  }

  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun clearSuccess() {
    _uiState.update { it.copy(successMessage = null) }
  }

  fun loadLatestAnnouncements(session: UserSession?) {
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isLatestLoading = true) }
      val result = noticesService.fetchLatestAnnouncements(session.accessToken)
      if (result.isSuccess) {
        _uiState.update { it.copy(isLatestLoading = false, latestAnnouncements = result.getOrDefault(emptyList())) }
      } else {
        _uiState.update { it.copy(isLatestLoading = false) }
      }
    }
  }

  fun clearSelectedClass() {
    _uiState.update {
      it.copy(
        selectedClass = null,
        notices = emptyList(),
        activeNoticeDetail = null,
        errorMessage = null,
        successMessage = null
      )
    }
  }
}
