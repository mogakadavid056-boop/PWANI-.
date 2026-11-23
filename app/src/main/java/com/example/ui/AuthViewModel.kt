package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BecomeLecturerResult
import com.example.data.ClassesService
import com.example.data.LecturerService
import com.example.data.NetworkMonitor
import com.example.data.OfflineCache
import com.example.data.OfflineMaterialsManager
import com.example.data.SessionManager
import com.example.data.SupabaseAuthService
import com.example.data.UserRoleInfo
import com.example.data.UserSession
import com.example.util.RoleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
  application: Application
) : AndroidViewModel(application) {

  private val authService = SupabaseAuthService()
  private val classesService = ClassesService()
  private val lecturerService = LecturerService()
  val sessionManager = SessionManager(application)

  private val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  private val registrationHandler = RegistrationHandler(
    authService = authService,
    classesService = classesService,
    lecturerService = lecturerService,
    sessionManager = sessionManager,
    onSessionReady = { session -> triggerAlarmSync(session) },
    updateState = { transform -> _uiState.update(transform) }
  )

  init {
    OfflineCache.appContext = application.applicationContext
    NetworkMonitor.initialize(application.applicationContext)
    checkInitialSession()
  }

  fun checkInitialSession() {
    viewModelScope.launch {
      try {
        val savedSession = sessionManager.getSession()
        kotlinx.coroutines.delay(950L)
        if (savedSession != null && savedSession.userId.isNotBlank() && savedSession.accessToken.isNotBlank()) {
          OfflineCache.currentUserId = savedSession.userId
          val initialRole = savedSession.role.ifBlank { RoleHelper.ROLE_STUDENT }
          val readySession = savedSession.copy(role = initialRole)
          _uiState.update { it.copy(session = readySession, currentScreen = Screen.Home, isLoading = false) }
          triggerAlarmSync(readySession)
          refreshUserRole()
        } else {
          _uiState.update { it.copy(session = null, currentScreen = Screen.Welcome, isLoading = false) }
        }
      } catch (_: Throwable) {
        _uiState.update { it.copy(session = null, currentScreen = Screen.Welcome, isLoading = false) }
      }
    }
  }

  fun refreshUserRole() {
    val currentSession = _uiState.value.session ?: return
    viewModelScope.launch {
      authService.fetchUserRole(currentSession.userId, currentSession.accessToken).onSuccess { roleInfo ->
        val updatedSession = currentSession.copy(role = roleInfo.role, isVerified = roleInfo.isVerified)
        sessionManager.updateRoleAndVerification(roleInfo.role, roleInfo.isVerified)
        _uiState.update { it.copy(session = updatedSession) }
      }
    }
  }

  private fun triggerAlarmSync(session: UserSession) {
    viewModelScope.launch {
      com.example.alarms.TimetableAlarmSync.onAuthenticated(getApplication(), session)
      com.example.notifications.PushTokenManager.syncCurrentToken(getApplication(), session.accessToken)
    }
  }

  fun login(email: String, password: String, onLoginSuccess: (UserSession) -> Unit) {
    val cleanEmail = email.trim().lowercase()
    val cleanPassword = password.trim()
    if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Please enter both email and password.") }
      return
    }
    _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

    viewModelScope.launch {
      authService.login(cleanEmail, cleanPassword).fold(
        onSuccess = { auth ->
          val roleInfo = authService.fetchUserRole(auth.userId, auth.accessToken)
            .getOrElse { UserRoleInfo() }
          val (profileName, profileAdm) = authService.fetchBasicProfile(auth.userId, auth.accessToken)
          val session = UserSession(
            userId = auth.userId,
            email = auth.email,
            fullName = profileName.ifBlank { auth.fullName }.ifBlank { auth.email.substringBefore('@') },
            admissionNumber = profileAdm.ifBlank { auth.admissionNumber },
            role = roleInfo.role,
            accessToken = auth.accessToken,
            refreshToken = auth.refreshToken,
            isVerified = roleInfo.isVerified
          )
          sessionManager.saveSession(session)
          OfflineCache.currentUserId = session.userId
          _uiState.update { it.copy(session = session, isLoading = false, errorMessage = null) }
          triggerAlarmSync(session)
          val handledPending = registrationHandler.completePendingOnLogin(session)
          if (!handledPending && (profileName.isBlank() || profileAdm.isBlank())) {
            val healPayload = org.json.JSONObject().apply {
              if (profileName.isBlank() && session.fullName.isNotBlank()) put("full_name", session.fullName)
              if (profileAdm.isBlank() && session.admissionNumber.isNotBlank()) put("admission_number", session.admissionNumber)
            }
            if (healPayload.length() > 0) {
              authService.patchProfile(session.userId, session.accessToken, healPayload)
            }
          }
          if (!handledPending) onLoginSuccess(session)
        },
        onFailure = { error ->
          _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Authentication failed.") }
        }
      )
    }
  }

  fun becomeLecturer(passcode: String, onResult: (Result<String>) -> Unit = {}) {
    val currentSession = _uiState.value.session ?: return
    viewModelScope.launch {
      when (val res = lecturerService.becomeLecturer(passcode, currentSession.accessToken)) {
        is BecomeLecturerResult.Success -> {
          val roleInfo = authService.fetchUserRole(currentSession.userId, currentSession.accessToken)
            .getOrElse { UserRoleInfo(role = RoleHelper.ROLE_LECTURER, isVerified = true) }
          val newRole = roleInfo.role.ifBlank { RoleHelper.ROLE_LECTURER }
          val verified = roleInfo.isVerified || RoleHelper.isLecturer(newRole)
          val updated = currentSession.copy(role = newRole, isVerified = verified)
          sessionManager.updateRoleAndVerification(newRole, verified)
          _uiState.update { it.copy(session = updated) }
          com.example.alarms.TimetableAlarmSync.syncAndReschedule(getApplication(), updated)
          onResult(Result.success("Upgraded to Lecturer account!"))
        }
        is BecomeLecturerResult.WrongPasscode -> {
          onResult(Result.failure(Exception("Invalid lecturer passcode.")))
        }
        is BecomeLecturerResult.Error -> {
          onResult(Result.failure(Exception(res.message)))
        }
      }
    }
  }

  fun logout() {
    val uid = _uiState.value.session?.userId.orEmpty()
    val accessToken = _uiState.value.session?.accessToken.orEmpty()
    com.example.notifications.PushTokenManager.unregisterOnLogout(getApplication(), accessToken)
    com.example.alarms.TimetableAlarmSync.onLoggedOut(getApplication())
    com.example.notifications.NotificationSyncWorker.cancel(getApplication())
    com.example.notifications.NotificationPreferences.clearOnLogout(getApplication())
    com.example.assistant.AssistantStorage.clearOnLogout(getApplication())
    viewModelScope.launch {
      if (uid.isNotBlank()) {
        OfflineCache.clearUserCache(getApplication(), uid)
        OfflineMaterialsManager.clearAllUserDownloads(getApplication(), uid)
      }
      OfflineCache.currentUserId = ""
      OfflineCache.clearLastUpdated()
      sessionManager.clearSession()
      _uiState.update { it.copy(session = null, currentScreen = Screen.Welcome, errorMessage = null, infoMessage = null) }
    }
  }

  fun submitRegistrationWizard(wizard: com.example.ui.screens.register.RegisterWizardState) = viewModelScope.launch {
    registrationHandler.submitWizard(wizard)
  }

  fun resendConfirmationEmail() {
    val email = _uiState.value.pendingConfirmationEmail.orEmpty()
    viewModelScope.launch { registrationHandler.resendConfirmationEmail(email) }
  }

  fun retryStudentAutoJoin() {
    val session = _uiState.value.session ?: return
    viewModelScope.launch { registrationHandler.retryStudentAutoJoin(session) }
  }

  fun retryStudentJoinByCode(code: String) {
    val session = _uiState.value.session ?: return
    viewModelScope.launch { registrationHandler.retryStudentJoinByCode(code, session) }
  }

  fun navigateTo(screen: Screen, clearMessages: Boolean = true) {
    _uiState.update {
      if (clearMessages) it.copy(currentScreen = screen, errorMessage = null, infoMessage = null)
      else it.copy(currentScreen = screen)
    }
  }

  fun updateMemberDetails(fullName: String, admissionNumber: String) {
    val cleanName = fullName.trim()
    val cleanAdm = admissionNumber.trim()
    viewModelScope.launch {
      sessionManager.updateMemberDetails(cleanName, cleanAdm)
      _uiState.update { state ->
        val current = state.session ?: return@update state
        state.copy(session = current.copy(fullName = cleanName, admissionNumber = cleanAdm))
      }
    }
  }

  fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
  fun clearInfo() { _uiState.update { it.copy(infoMessage = null) } }
  fun setError(message: String) { _uiState.update { it.copy(errorMessage = message) } }
  fun clearPendingOpenClass() { _uiState.update { it.copy(pendingOpenClassId = null) } }
}
