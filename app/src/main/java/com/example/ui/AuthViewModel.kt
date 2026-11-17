package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ClassesService
import com.example.data.NetworkMonitor
import com.example.data.OfflineCache
import com.example.data.OfflineMaterialsManager
import com.example.data.ProfileRealtimeService
import com.example.data.ProgrammeItem
import com.example.data.SchoolItem
import com.example.data.SessionManager
import com.example.data.SupabaseAuthService
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
  private val realtimeService = ProfileRealtimeService()
  val sessionManager = SessionManager(application)

  private val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  private val registrationHandler = RegistrationHandler(
    authService = authService,
    classesService = classesService,
    sessionManager = sessionManager,
    onSubscribeRealtime = { session -> subscribeToProfileChanges(session) },
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
        if (savedSession != null && savedSession.userId.isNotBlank() && savedSession.accessToken.isNotBlank()) {
          OfflineCache.currentUserId = savedSession.userId
          when {
            RoleHelper.isRejectedStatus(savedSession.roleStatus) -> {
              forceLogoutWithMessage("Your account application was rejected by an administrator.")
            }
            RoleHelper.isPendingApproval(savedSession.roleStatus) -> {
              _uiState.update { it.copy(session = savedSession, currentScreen = Screen.PendingApproval, isLoading = false) }
              subscribeToProfileChanges(savedSession)
              refreshProfileStatus()
            }
            savedSession.role.isBlank() -> {
              _uiState.update { it.copy(session = savedSession, isLoading = true) }
              refreshProfileStatus()
            }
            else -> {
              _uiState.update { it.copy(session = savedSession, currentScreen = Screen.Home, isLoading = false) }
              subscribeToProfileChanges(savedSession)
              triggerAlarmSync(savedSession)
              refreshProfileStatus()
            }
          }
        } else {
          _uiState.update { it.copy(session = null, currentScreen = Screen.Welcome, isLoading = false) }
        }
      } catch (_: Throwable) {
        _uiState.update { it.copy(session = null, currentScreen = Screen.Welcome, isLoading = false) }
      }
    }
  }

  private fun subscribeToProfileChanges(session: UserSession) {
    realtimeService.subscribe(session.userId, session.accessToken) { refreshProfileStatus() }
  }

  private fun triggerAlarmSync(session: UserSession) {
    viewModelScope.launch { com.example.alarms.TimetableAlarmSync.onAuthenticated(getApplication(), session) }
  }

  fun refreshProfileStatus(userTriggered: Boolean = false, onApproved: ((UserSession) -> Unit)? = null) {
    val currentSession = _uiState.value.session ?: return
    viewModelScope.launch {
      if (userTriggered) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
      }
      val profileRes = authService.fetchProfileRoleInfo(currentSession.userId, currentSession.accessToken)
      profileRes.fold(
        onSuccess = { info ->
          if (RoleHelper.isRejectedStatus(info.roleStatus)) {
            forceLogoutWithMessage("Your account request was rejected by an administrator.")
            return@fold
          }
          val updatedSession = currentSession.copy(
            role = info.role,
            roleStatus = info.roleStatus,
            isVerified = info.isVerified,
            fullName = info.fullName.ifBlank { currentSession.fullName },
            admissionNumber = info.admissionNumber,
            staffNumber = info.staffNumber,
            department = info.department
          )
          val wasPending = _uiState.value.currentScreen == Screen.PendingApproval
          val nowPending = RoleHelper.isPendingApproval(info.roleStatus)
          sessionManager.saveSession(updatedSession)

          when {
            nowPending -> _uiState.update {
              it.copy(session = updatedSession, currentScreen = Screen.PendingApproval, isLoading = false, infoMessage = if (userTriggered) "Still awaiting administrator approval." else it.infoMessage)
            }
            wasPending || _uiState.value.currentScreen == Screen.Welcome || _uiState.value.currentScreen == Screen.Login -> {
              _uiState.update { it.copy(session = updatedSession, currentScreen = Screen.Home, isLoading = false, errorMessage = null, infoMessage = null) }
              subscribeToProfileChanges(updatedSession)
              triggerAlarmSync(updatedSession)
              onApproved?.invoke(updatedSession)
            }
            else -> {
              _uiState.update { it.copy(session = updatedSession, isLoading = false) }
              if (currentSession.role.isBlank()) triggerAlarmSync(updatedSession)
            }
          }
        },
        onFailure = { err ->
          if (userTriggered || currentSession.role.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = err.message ?: "Failed to refresh profile status.") }
          }
        }
      )
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
          val profileRes = authService.fetchProfileRoleInfo(auth.userId, auth.accessToken)
          profileRes.fold(
            onSuccess = { info ->
              if (RoleHelper.isRejectedStatus(info.roleStatus)) {
                forceLogoutWithMessage("Your account request was rejected by an administrator.", targetScreen = Screen.Login)
                return@fold
              }
              val session = UserSession(
                userId = auth.userId,
                email = auth.email,
                fullName = info.fullName.ifBlank { auth.fullName }.ifBlank { auth.email.substringBefore('@') },
                admissionNumber = info.admissionNumber,
                role = info.role,
                accessToken = auth.accessToken,
                refreshToken = auth.refreshToken,
                isVerified = info.isVerified,
                roleStatus = info.roleStatus,
                staffNumber = info.staffNumber,
                department = info.department
              )
              sessionManager.saveSession(session)
              OfflineCache.currentUserId = session.userId
              subscribeToProfileChanges(session)

              if (RoleHelper.isPendingApproval(info.roleStatus)) {
                _uiState.update { it.copy(session = session, currentScreen = Screen.PendingApproval, isLoading = false, errorMessage = null) }
              } else {
                _uiState.update { it.copy(session = session, isLoading = false, errorMessage = null) }
                triggerAlarmSync(session)
                onLoginSuccess(session)
              }
            },
            onFailure = { err ->
              _uiState.update {
                it.copy(
                  isLoading = false,
                  errorMessage = err.message ?: "Unable to load your profile role. Please try again."
                )
              }
            }
          )
        },
        onFailure = { error ->
          _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Authentication failed.") }
        }
      )
    }
  }

  private suspend fun forceLogoutWithMessage(message: String, targetScreen: Screen = Screen.Login) {
    val uid = _uiState.value.session?.userId.orEmpty()
    realtimeService.unsubscribe()
    com.example.alarms.TimetableAlarmSync.onLoggedOut(getApplication())
    if (uid.isNotBlank()) {
      OfflineCache.clearUserCache(getApplication(), uid)
      OfflineMaterialsManager.clearAllUserDownloads(getApplication(), uid)
    }
    OfflineCache.currentUserId = ""
    OfflineCache.clearLastUpdated()
    sessionManager.clearSession()
    _uiState.update { it.copy(session = null, currentScreen = targetScreen, isLoading = false, errorMessage = message, infoMessage = null) }
  }

  fun logout() {
    val uid = _uiState.value.session?.userId.orEmpty()
    realtimeService.unsubscribe()
    com.example.alarms.TimetableAlarmSync.onLoggedOut(getApplication())
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

  fun registerWithKnown(
    fullName: String, admissionNumber: String, email: String, password: String,
    school: SchoolItem?, programme: ProgrammeItem?, yearOfStudy: Int?, onClassResolved: (String?) -> Unit
  ) = viewModelScope.launch {
    registrationHandler.registerWithKnown(fullName, admissionNumber, email, password, school, programme, yearOfStudy, onClassResolved)
  }

  fun registerWithCode(
    fullName: String, admissionNumber: String, email: String, password: String,
    classCode: String, onClassResolved: (String?) -> Unit
  ) = viewModelScope.launch {
    registrationHandler.registerWithCode(fullName, admissionNumber, email, password, classCode, onClassResolved)
  }

  fun registerLecturer(
    fullName: String, staffNumber: String, department: String, email: String, password: String,
    onActiveLecturer: (UserSession) -> Unit, onPendingApproval: (UserSession) -> Unit
  ) = viewModelScope.launch {
    registrationHandler.registerLecturer(fullName, staffNumber, department, email, password, onActiveLecturer, onPendingApproval)
  }

  fun navigateTo(screen: Screen) {
    _uiState.update { it.copy(currentScreen = screen, errorMessage = null, infoMessage = null) }
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

  override fun onCleared() {
    super.onCleared()
    realtimeService.unsubscribe()
  }
}
