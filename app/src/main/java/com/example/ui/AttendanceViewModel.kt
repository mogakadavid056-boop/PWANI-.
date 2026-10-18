package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AttendanceRecordItem
import com.example.data.AttendanceService
import com.example.data.AttendanceSessionItem
import com.example.data.AttendanceSubScreen
import com.example.data.UserSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class AttendanceUiState(
  val classId: String = "",
  val isStaff: Boolean = false,
  val isLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val staffSessions: List<AttendanceSessionItem> = emptyList(),
  val openSessions: List<AttendanceSessionItem> = emptyList(),
  val activeSessionId: String? = null,
  val activeSessionCode: String? = null,
  val activeSessionRemainingSeconds: Long = 0L,
  val selectedSessionId: String? = null,
  val attendanceList: List<AttendanceRecordItem> = emptyList(),
  val isAttendanceListLoading: Boolean = false,
  val currentSubScreen: AttendanceSubScreen = AttendanceSubScreen.None,
  val sessionPendingDelete: AttendanceSessionItem? = null
)

class AttendanceViewModel : ViewModel() {

  private val attendanceService = AttendanceService()
  private val _uiState = MutableStateFlow(AttendanceUiState())
  val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

  private var countdownJob: Job? = null

  private fun getIsoUtc(date: Date = Date()): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(date)
  }

  fun initForClass(classId: String, isStaff: Boolean, session: UserSession?) {
    _uiState.update {
      it.copy(
        classId = classId,
        isStaff = isStaff,
        errorMessage = null,
        successMessage = null,
        sessionPendingDelete = null,
        currentSubScreen = AttendanceSubScreen.None
      )
    }
    loadSessions(classId, isStaff, session)
  }

  fun setStaff(isStaff: Boolean, session: UserSession?) {
    val changed = _uiState.value.isStaff != isStaff
    _uiState.update { it.copy(isStaff = isStaff) }
    if (changed) {
      loadSessions(_uiState.value.classId, isStaff, session)
    }
  }

  fun loadSessions(
    classId: String = _uiState.value.classId,
    isStaff: Boolean = _uiState.value.isStaff,
    session: UserSession?
  ) {
    if (classId.isBlank() || session == null || session.accessToken.isBlank()) return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      if (isStaff) {
        val result = attendanceService.fetchStaffSessions(classId, session.accessToken)
        result.fold(
          onSuccess = { list -> _uiState.update { it.copy(isLoading = false, staffSessions = list) } },
          onFailure = { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        )
      } else {
        val nowIso = getIsoUtc()
        val result = attendanceService.fetchOpenSessions(classId, nowIso, session.accessToken)
        result.fold(
          onSuccess = { list -> _uiState.update { it.copy(isLoading = false, openSessions = list) } },
          onFailure = { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        )
      }
    }
  }

  fun startAttendanceSession(
    durationMinutes: Int,
    session: UserSession?,
    onCreated: () -> Unit = {}
  ) {
    val currentClassId = _uiState.value.classId
    if (currentClassId.isBlank() || session == null || session.accessToken.isBlank()) return

    val randomCode = (100000..999999).random().toString()
    val expiryMillis = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
    val expiryIso = getIsoUtc(Date(expiryMillis))

    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      val result = attendanceService.createAttendanceSession(
        classId = currentClassId,
        sessionCode = randomCode,
        sessionExpiryIso = expiryIso,
        accessToken = session.accessToken
      )

      result.fold(
        onSuccess = { sessionId ->
          val totalSeconds = durationMinutes * 60L
          _uiState.update {
            it.copy(
              isActionLoading = false,
              activeSessionId = sessionId,
              activeSessionCode = randomCode,
              activeSessionRemainingSeconds = totalSeconds
            )
          }
          startCountdown(totalSeconds)
          loadSessions(currentClassId, isStaff = true, session)
          onCreated()
        },
        onFailure = { error ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = error.message) }
        }
      )
    }
  }

  private fun startCountdown(totalSeconds: Long) {
    countdownJob?.cancel()
    countdownJob = viewModelScope.launch {
      var remaining = totalSeconds
      while (remaining > 0) {
        delay(1000L)
        remaining -= 1
        _uiState.update { it.copy(activeSessionRemainingSeconds = remaining) }
      }
      _uiState.update { it.copy(activeSessionRemainingSeconds = 0L) }
    }
  }

  fun openViewAttendance(sessionId: String, session: UserSession?) {
    _uiState.update {
      it.copy(
        currentSubScreen = AttendanceSubScreen.ViewAttendance,
        selectedSessionId = sessionId,
        attendanceList = emptyList(),
        isAttendanceListLoading = true,
        errorMessage = null
      )
    }
    if (session == null || session.accessToken.isBlank()) return

    viewModelScope.launch {
      val result = attendanceService.getAttendanceList(sessionId, session.accessToken)
      result.fold(
        onSuccess = { list -> _uiState.update { it.copy(isAttendanceListLoading = false, attendanceList = list) } },
        onFailure = { error -> _uiState.update { it.copy(isAttendanceListLoading = false, errorMessage = error.message) } }
      )
    }
  }

  fun openSignAttendance(sessionId: String) {
    _uiState.update {
      it.copy(
        currentSubScreen = AttendanceSubScreen.SignAttendance,
        selectedSessionId = sessionId,
        errorMessage = null,
        successMessage = null
      )
    }
  }

  fun promptDeleteSession(session: AttendanceSessionItem) {
    _uiState.update { it.copy(sessionPendingDelete = session) }
  }

  fun dismissPromptDeleteSession() {
    _uiState.update { it.copy(sessionPendingDelete = null) }
  }

  fun confirmDeleteSession(session: UserSession?) {
    val pending = _uiState.value.sessionPendingDelete ?: return
    val cid = _uiState.value.classId
    if (session == null || session.accessToken.isBlank() || cid.isBlank()) return

    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, sessionPendingDelete = null, errorMessage = null) }
      val result = attendanceService.deleteAttendanceSession(pending.id, session.accessToken)
      result.fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              isActionLoading = false,
              successMessage = "Session deleted",
              activeSessionId = if (it.activeSessionId == pending.id) null else it.activeSessionId,
              activeSessionCode = if (it.activeSessionId == pending.id) null else it.activeSessionCode
            )
          }
          loadSessions(cid, isStaff = true, session)
        },
        onFailure = { error ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = error.message) }
        }
      )
    }
  }

  fun signAttendance(
    code: String,
    signatureBase64: String,
    session: UserSession?,
    onSuccess: () -> Unit
  ) {
    val targetSessionId = _uiState.value.selectedSessionId.orEmpty()
    if (targetSessionId.isBlank() || session == null || session.accessToken.isBlank()) return

    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      val result = attendanceService.signAttendance(
        sessionId = targetSessionId,
        providedCode = code,
        signatureBase64 = signatureBase64,
        accessToken = session.accessToken
      )

      result.fold(
        onSuccess = {
          _uiState.update { it.copy(isActionLoading = false, successMessage = "Attendance signed successfully!") }
          loadSessions(session = session)
          onSuccess()
        },
        onFailure = { error ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = error.message) }
        }
      )
    }
  }

  fun closeSubScreen() {
    _uiState.update {
      it.copy(
        currentSubScreen = AttendanceSubScreen.None,
        selectedSessionId = null,
        errorMessage = null,
        successMessage = null
      )
    }
  }

  fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
  fun clearSuccess() { _uiState.update { it.copy(successMessage = null) } }
  fun openAttendanceSummary() {
    _uiState.update { it.copy(currentSubScreen = AttendanceSubScreen.AttendanceSummary, errorMessage = null) }
  }

  fun openMyAttendance() {
    _uiState.update { it.copy(currentSubScreen = AttendanceSubScreen.MyAttendance, errorMessage = null) }
  }

  fun dismissActiveSessionBanner() {
    _uiState.update { it.copy(activeSessionCode = null, activeSessionId = null) }
  }
}
