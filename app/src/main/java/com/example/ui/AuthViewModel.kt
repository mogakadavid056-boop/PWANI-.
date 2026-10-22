package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ClassesService
import com.example.data.ProgrammeItem
import com.example.data.SchoolItem
import com.example.data.SessionManager
import com.example.data.SupabaseAuthService
import com.example.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

class AuthViewModel(
  application: Application
) : AndroidViewModel(application) {

  private val authService = SupabaseAuthService()
  private val classesService = ClassesService()
  val sessionManager = SessionManager(application)

  private val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  init {
    com.example.data.OfflineCache.appContext = application.applicationContext
    com.example.data.NetworkMonitor.initialize(application.applicationContext)
    checkInitialSession()
  }

  fun checkInitialSession() {
    viewModelScope.launch {
      try {
        val savedSession = sessionManager.getSession()
        if (savedSession != null && savedSession.userId.isNotBlank()) {
          com.example.data.OfflineCache.currentUserId = savedSession.userId
          _uiState.update {
            it.copy(session = savedSession, currentScreen = Screen.Home, isLoading = false)
          }
          refreshRoleAndProfile(savedSession)
        } else {
          _uiState.update { it.copy(session = null, currentScreen = Screen.Welcome, isLoading = false) }
        }
      } catch (_: Throwable) {
        _uiState.update { it.copy(session = null, currentScreen = Screen.Welcome, isLoading = false) }
      }
    }
  }

  private val lecturerService = com.example.data.LecturerService()
  private val registrationHandler = RegistrationHandler(
    authService = authService,
    classesService = classesService,
    lecturerService = lecturerService,
    sessionManager = sessionManager,
    updateState = { transform -> _uiState.update(transform) }
  )

  fun registerWithKnown(
    fullName: String,
    admissionNumber: String,
    email: String,
    password: String,
    school: SchoolItem?,
    programme: ProgrammeItem?,
    yearOfStudy: Int?,
    onClassResolved: (classId: String?) -> Unit
  ) {
    viewModelScope.launch {
      registrationHandler.registerWithKnown(
        fullName, admissionNumber, email, password, school, programme, yearOfStudy, onClassResolved
      )
    }
  }

  fun registerWithCode(
    fullName: String,
    admissionNumber: String,
    email: String,
    password: String,
    classCode: String,
    onClassResolved: (classId: String?) -> Unit
  ) {
    viewModelScope.launch {
      registrationHandler.registerWithCode(
        fullName, admissionNumber, email, password, classCode, onClassResolved
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

    _uiState.update { it.copy(isLoading = true, errorMessage = null) }

    viewModelScope.launch {
      val loginResult = authService.login(cleanEmail, cleanPassword)
      loginResult.fold(
        onSuccess = { auth ->
          val adm = authService.fetchAdmissionNumber(auth.userId, auth.accessToken).getOrDefault("")
          val role = authService.fetchUserRole(auth.userId, auth.accessToken).getOrDefault("Student")
          val session = UserSession(
            userId = auth.userId, email = auth.email,
            fullName = auth.fullName.ifBlank { auth.email.substringBefore('@') },
            admissionNumber = adm, role = role, accessToken = auth.accessToken, refreshToken = auth.refreshToken
          )
          sessionManager.saveSession(session)
          com.example.data.OfflineCache.currentUserId = session.userId
          _uiState.update { it.copy(session = session, isLoading = false, errorMessage = null) }
          onLoginSuccess(session)
        },
        onFailure = { error ->
          _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Authentication failed.") }
        }
      )
    }
  }

  private fun refreshRoleAndProfile(session: UserSession) {
    viewModelScope.launch {
      val roleResult = authService.fetchUserRole(session.userId, session.accessToken)
      val updatedRole = roleResult.getOrNull()
      if (!updatedRole.isNullOrBlank() && updatedRole != session.role) {
        val updated = session.copy(role = updatedRole)
        sessionManager.updateRole(updatedRole)
        _uiState.update { it.copy(session = updated) }
      }
    }
  }

  fun logout() {
    val uid = _uiState.value.session?.userId.orEmpty()
    viewModelScope.launch {
      if (uid.isNotBlank()) {
        com.example.data.OfflineCache.clearUserCache(getApplication(), uid)
        com.example.data.OfflineMaterialsManager.clearAllUserDownloads(getApplication(), uid)
      }
      com.example.data.OfflineCache.currentUserId = ""
      com.example.data.OfflineCache.clearLastUpdated()
      sessionManager.clearSession()
      _uiState.update {
        it.copy(session = null, currentScreen = Screen.Welcome, errorMessage = null, infoMessage = null)
      }
    }
  }

  fun navigateTo(screen: Screen) {
    _uiState.update { it.copy(currentScreen = screen, errorMessage = null, infoMessage = null) }
  }

  fun registerLecturer(
    fullName: String,
    staffNumber: String,
    email: String,
    password: String,
    passcode: String,
    onSuccessLecturer: (UserSession) -> Unit,
    onWrongPasscode: (UserSession, String) -> Unit
  ) {
    viewModelScope.launch {
      registrationHandler.registerLecturer(
        fullName, staffNumber, email, password, passcode, onSuccessLecturer, onWrongPasscode
      )
    }
  }

  fun updateSessionRole(newRole: String) {
    viewModelScope.launch {
      sessionManager.updateRole(newRole)
      _uiState.update { state ->
        val current = state.session ?: return@update state
        state.copy(session = current.copy(role = newRole))
      }
    }
  }

  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun setError(message: String) {
    _uiState.update { it.copy(errorMessage = message) }
  }
}
