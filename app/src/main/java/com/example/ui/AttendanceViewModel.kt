package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.NairobiTimeHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

class AttendanceViewModel : ViewModel() {
  private val attendanceService = AttendanceService()
  private val registerService = AttendanceRegisterService()
  private val _uiState = MutableStateFlow(AttendanceUiState())
  val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

  private var countdownJob: Job? = null
  private var lockoutJob: Job? = null

  fun initForClass(classId: String, isStaff: Boolean, session: UserSession?) {
    _uiState.update {
      it.copy(
        classId = classId, isStaff = isStaff, errorMessage = null, successMessage = null,
        sessionPendingVoid = null, registerPendingDelete = null,
        activeSessionId = null, activeSessionCode = null, canManageActiveSession = false,
        currentSubScreen = AttendanceSubScreen.None, signNeedsDetails = false
      )
    }
    loadSessions(classId, isStaff, session)
    loadClassUnits(classId, session)
  }

  fun setStaff(isStaff: Boolean, session: UserSession?) {
    val changed = _uiState.value.isStaff != isStaff
    _uiState.update { it.copy(isStaff = isStaff) }
    if (changed) loadSessions(_uiState.value.classId, isStaff, session)
  }

  fun loadClassUnits(classId: String = _uiState.value.classId, session: UserSession?) {
    if (classId.isBlank() || session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isUnitsLoading = true) }
      registerService.getClassUnits(classId, session.accessToken).fold(
        onSuccess = { units -> _uiState.update { it.copy(isUnitsLoading = false, classUnits = units) } },
        onFailure = { _uiState.update { it.copy(isUnitsLoading = false) } }
      )
    }
  }

  fun loadSessions(classId: String = _uiState.value.classId, isStaff: Boolean = _uiState.value.isStaff, session: UserSession?) {
    if (classId.isBlank() || session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val regList = registerService.listRegisters(classId, session.accessToken).getOrElse { emptyList() }
      val openManageableReg = regList.firstOrNull { it.status == "open" && it.canManage }
      val canManageCur = _uiState.value.activeSessionId?.let { aid -> regList.find { it.sessionId == aid }?.canManage } ?: (openManageableReg != null)
      _uiState.update { it.copy(registers = regList, canManageActiveSession = canManageCur) }

      if (isStaff || openManageableReg != null) {
        attendanceService.fetchStaffSessions(classId, session.accessToken).fold(
          onSuccess = { list ->
            _uiState.update { it.copy(isLoading = false, staffSessions = list) }
            val targetId = openManageableReg?.sessionId
            val openSession = if (targetId != null) list.find { it.id == targetId } else list.firstOrNull { it.active && NairobiTimeHelper.calculateRemainingSeconds(it.expiresAt) > 0 }
            if (openSession != null && regList.any { it.sessionId == openSession.id && it.canManage } && _uiState.value.activeSessionCode.isNullOrBlank()) {
              fetchActiveSessionCode(openSession.id, openSession.expiresAt, session)
            }
          },
          onFailure = { err -> _uiState.update { it.copy(isLoading = false, errorMessage = err.message) } }
        )
      }
      if (!isStaff) {
        attendanceService.fetchOpenSessions(classId, NairobiTimeHelper.toIsoUtc(), session.accessToken).fold(
          onSuccess = { list -> _uiState.update { it.copy(isLoading = false, openSessions = list) } },
          onFailure = { err -> _uiState.update { it.copy(isLoading = false, errorMessage = err.message) } }
        )
      }
    }
  }

  fun startAttendanceSession(
    courseId: String?, courseTitle: String, courseCode: String, lecturerName: String,
    venueText: String, sessionTitle: String, durationMinutes: Int, session: UserSession?, onCreated: () -> Unit = {}
  ) {
    val currentClassId = _uiState.value.classId
    if (currentClassId.isBlank() || session == null || session.accessToken.isBlank()) return
    val expiryIso = NairobiTimeHelper.toIsoUtc(Date(System.currentTimeMillis() + (durationMinutes * 60 * 1000L)))
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      attendanceService.createAttendanceSessionV2(currentClassId, courseId, expiryIso, sessionTitle, session.accessToken).fold(
        onSuccess = { res ->
          val rem = NairobiTimeHelper.calculateRemainingSeconds(res.expiresAt).let { if (it > 0) it else (durationMinutes * 60L) }
          registerService.updateRegisterDetails(res.sessionId, courseTitle, courseCode, lecturerName, venueText, sessionTitle, session.accessToken)
          _uiState.update { it.copy(isActionLoading = false, activeSessionId = res.sessionId, activeSessionCode = res.sessionCode, activeSessionRemainingSeconds = rem, canManageActiveSession = true) }
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
        val remaining = NairobiTimeHelper.calculateRemainingSeconds(expiresAtIso)
        if (code.isNotBlank() && remaining > 0) {
          val canManage = _uiState.value.registers.find { it.sessionId == sessionId }?.canManage == true
          _uiState.update { it.copy(activeSessionId = sessionId, activeSessionCode = code, activeSessionRemainingSeconds = remaining, canManageActiveSession = canManage) }
          startCountdown(remaining)
        }
      }
    }
  }

  private fun startCountdown(totalSeconds: Long) {
    countdownJob?.cancel()
    countdownJob = viewModelScope.launch {
      var remaining = totalSeconds
      while (remaining > 0) { delay(1000L); remaining -= 1; _uiState.update { it.copy(activeSessionRemainingSeconds = remaining) } }
      _uiState.update { it.copy(activeSessionRemainingSeconds = 0L) }
    }
  }

  fun setSignLockout(durationMillis: Long = 10 * 60 * 1000L) {
    val until = System.currentTimeMillis() + durationMillis
    _uiState.update { it.copy(signLockoutUntilMillis = until) }
    lockoutJob?.cancel()
    lockoutJob = viewModelScope.launch {
      while (System.currentTimeMillis() < until) { delay(1000L); _uiState.update { it.copy() } }
      _uiState.update { it.copy(signLockoutUntilMillis = 0L) }
    }
  }

  fun openRegister(sessionId: String, session: UserSession?) {
    _uiState.update { it.copy(currentSubScreen = AttendanceSubScreen.AttendanceRegister, selectedSessionId = sessionId, isRegisterLoading = true, selectedRegisterDetail = null, errorMessage = null) }
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      registerService.getAttendanceRegister(sessionId, session.accessToken).fold(
        onSuccess = { detail -> _uiState.update { it.copy(isRegisterLoading = false, selectedRegisterDetail = detail) } },
        onFailure = { err -> _uiState.update { it.copy(isRegisterLoading = false, errorMessage = err.message) } }
      )
    }
  }

  private fun runRegisterAction(session: UserSession?, successText: String, reloadSessions: Boolean = false, refreshSessionId: String? = null, clearPendingVoid: Boolean = false, clearActiveCodeFor: String? = null, block: suspend (String) -> Result<Unit>) {
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null, sessionPendingVoid = if (clearPendingVoid) null else it.sessionPendingVoid) }
      block(session.accessToken).fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              isActionLoading = false, successMessage = successText,
              activeSessionCode = if (clearActiveCodeFor != null && it.activeSessionId == clearActiveCodeFor) null else it.activeSessionCode
            )
          }
          if (reloadSessions) loadSessions(session = session)
          val regToOpen = refreshSessionId ?: _uiState.value.selectedSessionId
          if (regToOpen != null && _uiState.value.currentSubScreen == AttendanceSubScreen.AttendanceRegister) openRegister(regToOpen, session)
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun updateRegisterDetails(sessionId: String, courseTitle: String, courseCode: String, lecturerName: String, venueText: String, sessionTitle: String, session: UserSession?) {
    if (sessionId.isNotBlank()) runRegisterAction(session, "Register details updated", true, sessionId) { registerService.updateRegisterDetails(sessionId, courseTitle, courseCode, lecturerName, venueText, sessionTitle, it) }
  }
  fun closeSession(sessionId: String, session: UserSession?) = runRegisterAction(session, "Session closed", reloadSessions = true, clearActiveCodeFor = sessionId) { registerService.closeAttendanceSession(sessionId, it) }
  fun voidSession(sessionId: String, reason: String, session: UserSession?) = runRegisterAction(session, "Session voided", reloadSessions = true, clearPendingVoid = true, clearActiveCodeFor = sessionId) { registerService.voidAttendanceSession(sessionId, reason, it) }
  fun correctRecord(recordId: String, newTimeIn: String, newTimeOut: String, reason: String, session: UserSession?) = runRegisterAction(session, "Record corrected") { registerService.correctAttendanceRecord(recordId, newTimeIn, newTimeOut, reason, it) }
  fun markStudentPresent(sessionId: String, studentId: String, newTimeIn: String, newTimeOut: String, reason: String, session: UserSession?) = runRegisterAction(session, "Marked present", true, sessionId) { registerService.markPresent(sessionId, studentId, newTimeIn, newTimeOut, reason, it) }

  fun deleteRegisterSession(sessionId: String, reason: String, session: UserSession?) {
    if (sessionId.isBlank() || session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null, registerPendingDelete = null) }
      registerService.deleteAttendanceSession(sessionId, reason, session.accessToken).fold(
        onSuccess = {
          _uiState.update { st ->
            val wasOpen = st.selectedSessionId == sessionId && st.currentSubScreen == AttendanceSubScreen.AttendanceRegister
            st.copy(
              isActionLoading = false, successMessage = "Register deleted",
              registers = st.registers.filterNot { it.sessionId == sessionId },
              activeSessionId = if (st.activeSessionId == sessionId) null else st.activeSessionId,
              activeSessionCode = if (st.activeSessionId == sessionId) null else st.activeSessionCode,
              currentSubScreen = if (wasOpen) AttendanceSubScreen.None else st.currentSubScreen,
              selectedRegisterDetail = if (wasOpen) null else st.selectedRegisterDetail,
              selectedSessionId = if (wasOpen) null else st.selectedSessionId
            )
          }
          loadSessions(session = session)
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  private fun runOkMsgAction(session: UserSession?, reloadList: Boolean, refreshRegId: String?, block: suspend (String) -> Result<Pair<Boolean, String>>) {
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      block(session.accessToken).fold(
        onSuccess = { (ok, msg) ->
          _uiState.update { it.copy(isActionLoading = false, successMessage = if (ok) msg else null, errorMessage = if (!ok) msg else null) }
          if (ok) {
            if (reloadList) loadSessions(session = session)
            val regId = refreshRegId ?: _uiState.value.selectedSessionId
            if (regId != null && (refreshRegId != null || _uiState.value.selectedSessionId == regId)) openRegister(regId, session)
          }
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun signOutSession(sessionId: String, session: UserSession?) =
    runOkMsgAction(session, reloadList = true, refreshRegId = null) { token -> registerService.signOutAttendance(sessionId, token) }

  fun signRegister(sessionId: String, signerRole: String, signatureBase64: String, session: UserSession?) =
    runOkMsgAction(session, reloadList = false, refreshRegId = sessionId) { token -> registerService.signRegister(sessionId, signerRole, signatureBase64, token) }

  fun updateMyDetails(fullName: String, admissionNo: String, phone: String, session: UserSession?, onSaved: (String, String) -> Unit) {
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      registerService.updateMyDetails(fullName, admissionNo, phone, session.accessToken).fold(
        onSuccess = { _uiState.update { it.copy(isActionLoading = false, signNeedsDetails = false) }; onSaved(fullName.trim(), admissionNo.trim()) },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun signAttendance(code: String, signatureBase64: String, memberFullName: String?, memberAdmissionNo: String?, session: UserSession?, onSuccess: () -> Unit) {
    val targetSessionId = _uiState.value.selectedSessionId.orEmpty()
    if (targetSessionId.isBlank() || session == null || session.accessToken.isBlank()) return
    if (_uiState.value.isSignLockedOut) { _uiState.update { it.copy(errorMessage = "Too many attempts. Please wait before trying again.") }; return }
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      attendanceService.signAttendanceV2(targetSessionId, code, signatureBase64, session.accessToken, memberFullName, memberAdmissionNo).fold(
        onSuccess = { res ->
          if (res.ok) {
            _uiState.update { it.copy(isActionLoading = false, signNeedsDetails = false, successMessage = res.message.ifBlank { "Attendance signed successfully!" }) }
            loadSessions(session = session); onSuccess()
          } else {
            val msg = res.message.ifBlank { "Could not sign attendance" }
            _uiState.update { it.copy(isActionLoading = false, signNeedsDetails = res.needsDetails, errorMessage = msg) }
            if (msg.startsWith("Too many attempts", ignoreCase = true)) setSignLockout(10 * 60 * 1000L)
          }
        },
        onFailure = { err ->
          val msg = err.message.orEmpty()
          _uiState.update { it.copy(isActionLoading = false, errorMessage = msg) }
          if (msg.startsWith("Too many attempts", ignoreCase = true)) setSignLockout(10 * 60 * 1000L)
        }
      )
    }
  }

  fun promptVoidSession(sessionItem: AttendanceSessionItem) { _uiState.update { it.copy(sessionPendingVoid = sessionItem) } }
  fun dismissPromptVoidSession() { _uiState.update { it.copy(sessionPendingVoid = null) } }
  fun promptDeleteRegister(item: AttendanceRegisterListItem) { _uiState.update { it.copy(registerPendingDelete = item) } }
  fun dismissPromptDeleteRegister() { _uiState.update { it.copy(registerPendingDelete = null) } }
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
  fun openSignAttendance(sessionId: String) { _uiState.update { it.copy(currentSubScreen = AttendanceSubScreen.SignAttendance, selectedSessionId = sessionId, errorMessage = null, successMessage = null, signNeedsDetails = false) } }
  fun promptDeleteSession(session: AttendanceSessionItem) { promptVoidSession(session) }
  fun dismissPromptDeleteSession() { dismissPromptVoidSession() }
  fun closeSubScreen() { _uiState.update { it.copy(currentSubScreen = AttendanceSubScreen.None, selectedSessionId = null, selectedRegisterDetail = null, errorMessage = null, successMessage = null, signNeedsDetails = false) } }
  fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
  fun clearSuccess() { _uiState.update { it.copy(successMessage = null) } }
  fun showError(message: String) { _uiState.update { it.copy(errorMessage = message) } }
  fun openAttendanceSummary() { _uiState.update { it.copy(currentSubScreen = AttendanceSubScreen.AttendanceSummary, errorMessage = null) } }
  fun openMyAttendance() { _uiState.update { it.copy(currentSubScreen = AttendanceSubScreen.MyAttendance, errorMessage = null) } }
  fun dismissActiveSessionBanner() { _uiState.update { it.copy(activeSessionCode = null, activeSessionId = null, canManageActiveSession = false) } }
}
