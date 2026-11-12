package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class AttendanceViewModel : ViewModel() {
  private val attendanceService = AttendanceService()
  private val _uiState = MutableStateFlow(AttendanceUiState())
  val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

  private var countdownJob: Job? = null
  private var lockoutJob: Job? = null

  private fun getIsoUtc(date: Date = Date()): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(date)
  }

  fun calculateRemainingSeconds(expiresAtIso: String): Long {
    if (expiresAtIso.isBlank()) return 0L
    val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss")
    val clean = expiresAtIso.replace("Z", "").substringBefore("+")
    for (pat in patterns) {
      try {
        val sdf = SimpleDateFormat(pat, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val date = sdf.parse(clean)
        if (date != null) return ((date.time - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
      } catch (_: Exception) {}
    }
    return 0L
  }

  fun initForClass(classId: String, isStaff: Boolean, session: UserSession?) {
    _uiState.update { it.copy(classId = classId, isStaff = isStaff, errorMessage = null, successMessage = null, sessionPendingDelete = null, currentSubScreen = AttendanceSubScreen.None) }
    loadSessions(classId, isStaff, session)
  }

  fun setStaff(isStaff: Boolean, session: UserSession?) {
    val changed = _uiState.value.isStaff != isStaff
    _uiState.update { it.copy(isStaff = isStaff) }
    if (changed) loadSessions(_uiState.value.classId, isStaff, session)
  }

  fun loadSessions(classId: String = _uiState.value.classId, isStaff: Boolean = _uiState.value.isStaff, session: UserSession?) {
    if (classId.isBlank() || session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      if (isStaff) {
        attendanceService.fetchStaffSessions(classId, session.accessToken).fold(
          onSuccess = { list ->
            _uiState.update { it.copy(isLoading = false, staffSessions = list) }
            val openSession = list.firstOrNull { it.active && calculateRemainingSeconds(it.expiresAt) > 0 }
            if (openSession != null && _uiState.value.activeSessionCode.isNullOrBlank()) {
              fetchActiveSessionCode(openSession.id, openSession.expiresAt, session)
            }
          },
          onFailure = { err -> _uiState.update { it.copy(isLoading = false, errorMessage = err.message) } }
        )
      } else {
        attendanceService.fetchOpenSessions(classId, getIsoUtc(), session.accessToken).fold(
          onSuccess = { list -> _uiState.update { it.copy(isLoading = false, openSessions = list) } },
          onFailure = { err -> _uiState.update { it.copy(isLoading = false, errorMessage = err.message) } }
        )
      }
    }
  }

  fun startAttendanceSession(durationMinutes: Int, session: UserSession?, sessionTitle: String = "Class Attendance", onCreated: () -> Unit = {}) {
    val currentClassId = _uiState.value.classId
    if (currentClassId.isBlank() || session == null || session.accessToken.isBlank()) return
    val expiryIso = getIsoUtc(Date(System.currentTimeMillis() + (durationMinutes * 60 * 1000L)))

    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      attendanceService.createAttendanceSessionV2(currentClassId, null, expiryIso, sessionTitle, session.accessToken).fold(
        onSuccess = { res ->
          val rem = calculateRemainingSeconds(res.expiresAt).let { if (it > 0) it else (durationMinutes * 60L) }
          _uiState.update { it.copy(isActionLoading = false, activeSessionId = res.sessionId, activeSessionCode = res.sessionCode, activeSessionRemainingSeconds = rem) }
          startCountdown(rem)
          loadSessions(currentClassId, isStaff = true, session)
          onCreated()
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun fetchActiveSessionCode(sessionId: String, expiresAtIso: String, session: UserSession?) {
    if (sessionId.isBlank() || session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      attendanceService.getAttendanceCode(sessionId, session.accessToken).onSuccess { code ->
        val remaining = calculateRemainingSeconds(expiresAtIso)
        if (code.isNotBlank() && remaining > 0) {
          _uiState.update { it.copy(activeSessionId = sessionId, activeSessionCode = code, activeSessionRemainingSeconds = remaining) }
          startCountdown(remaining)
        }
      }
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

  fun setSignLockout(durationMillis: Long = 10 * 60 * 1000L) {
    val until = System.currentTimeMillis() + durationMillis
    _uiState.update { it.copy(signLockoutUntilMillis = until) }
    lockoutJob?.cancel()
    lockoutJob = viewModelScope.launch {
      while (System.currentTimeMillis() < until) {
        delay(1000L)
        _uiState.update { it.copy() }
      }
      _uiState.update { it.copy(signLockoutUntilMillis = 0L) }
    }
  }

  fun openViewAttendance(sessionId: String, session: UserSession?) {
    _uiState.update { it.copy(currentSubScreen = AttendanceSubScreen.ViewAttendance, selectedSessionId = sessionId, attendanceList = emptyList(), isAttendanceListLoading = true, errorMessage = null) }
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      attendanceService.getAttendanceList(sessionId, session.accessToken).fold(
        onSuccess = { list -> _uiState.update { it.copy(isAttendanceListLoading = false, attendanceList = list) } },
        onFailure = { err -> _uiState.update { it.copy(isAttendanceListLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun openSignAttendance(sessionId: String) {
    _uiState.update { it.copy(currentSubScreen = AttendanceSubScreen.SignAttendance, selectedSessionId = sessionId, errorMessage = null, successMessage = null) }
  }

  fun promptDeleteSession(session: AttendanceSessionItem) { _uiState.update { it.copy(sessionPendingDelete = session) } }
  fun dismissPromptDeleteSession() { _uiState.update { it.copy(sessionPendingDelete = null) } }

  fun confirmDeleteSession(session: UserSession?) {
    val pending = _uiState.value.sessionPendingDelete ?: return
    val cid = _uiState.value.classId
    if (session == null || session.accessToken.isBlank() || cid.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, sessionPendingDelete = null, errorMessage = null) }
      attendanceService.deleteAttendanceSession(pending.id, session.accessToken).fold(
        onSuccess = {
          _uiState.update { it.copy(isActionLoading = false, successMessage = "Session deleted", activeSessionId = if (it.activeSessionId == pending.id) null else it.activeSessionId, activeSessionCode = if (it.activeSessionId == pending.id) null else it.activeSessionCode) }
          loadSessions(cid, isStaff = true, session)
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun signAttendance(code: String, signatureBase64: String, session: UserSession?, onSuccess: () -> Unit) {
    val targetSessionId = _uiState.value.selectedSessionId.orEmpty()
    if (targetSessionId.isBlank() || session == null || session.accessToken.isBlank()) return
    if (_uiState.value.isSignLockedOut) {
      _uiState.update { it.copy(errorMessage = "Too many attempts. Please wait before trying again.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      attendanceService.signAttendanceV2(targetSessionId, code, signatureBase64, session.accessToken).fold(
        onSuccess = { res ->
          if (res.ok) {
            val msg = res.message.ifBlank { "Attendance signed successfully!" }
            _uiState.update { it.copy(isActionLoading = false, successMessage = msg) }
            loadSessions(session = session)
            onSuccess()
          } else {
            val msg = res.message.ifBlank { "Incorrect attendance code" }
            _uiState.update { it.copy(isActionLoading = false, errorMessage = msg) }
            if (msg.startsWith("Too many attempts", ignoreCase = true)) {
              setSignLockout(10 * 60 * 1000L)
            }
          }
        },
        onFailure = { error ->
          val msg = error.message.orEmpty()
          _uiState.update { it.copy(isActionLoading = false, errorMessage = msg) }
          if (msg.startsWith("Too many attempts", ignoreCase = true)) {
            setSignLockout(10 * 60 * 1000L)
          }
        }
      )
    }
  }

  fun closeSubScreen() {
    _uiState.update { it.copy(currentSubScreen = AttendanceSubScreen.None, selectedSessionId = null, errorMessage = null, successMessage = null) }
  }

  fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
  fun clearSuccess() { _uiState.update { it.copy(successMessage = null) } }
  fun openAttendanceSummary() { _uiState.update { it.copy(currentSubScreen = AttendanceSubScreen.AttendanceSummary, errorMessage = null) } }
  fun openMyAttendance() { _uiState.update { it.copy(currentSubScreen = AttendanceSubScreen.MyAttendance, errorMessage = null) } }
  fun dismissActiveSessionBanner() { _uiState.update { it.copy(activeSessionCode = null, activeSessionId = null) } }
}
