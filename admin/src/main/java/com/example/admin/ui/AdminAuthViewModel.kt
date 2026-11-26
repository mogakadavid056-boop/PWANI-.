package com.example.admin.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.admin.data.AdminAuthService
import com.example.admin.data.AdminSession
import com.example.admin.data.AdminSessionManager
import com.example.admin.notifications.AdminPushTokenManager
import com.example.admin.util.AdminRoleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminAuthState(
  val isInitializing: Boolean = true,
  val isLoading: Boolean = false,
  val session: AdminSession? = null,
  val errorMessage: String? = null
)

class AdminAuthViewModel(application: Application) : AndroidViewModel(application) {
  private val sessionManager = AdminSessionManager(application)
  private val authService = AdminAuthService()

  private val _state = MutableStateFlow(AdminAuthState())
  val state: StateFlow<AdminAuthState> = _state.asStateFlow()

  init {
    restoreSession()
  }

  private fun restoreSession() {
    viewModelScope.launch {
      val saved = sessionManager.getSession()
      if (saved == null) {
        _state.update { it.copy(isInitializing = false, session = null) }
        return@launch
      }
      verifyActiveSession(saved)
    }
  }

  fun refreshAdminAccess() {
    val current = _state.value.session ?: return
    viewModelScope.launch {
      verifyActiveSession(current)
    }
  }

  private suspend fun verifyActiveSession(saved: AdminSession) {
    var active = saved
    val roleRes = authService.fetchUserRole(active.userId, active.accessToken)
    val resolvedRole = if (roleRes.isSuccess) {
      roleRes.getOrThrow()
    } else if (!active.refreshToken.isNullOrBlank()) {
      val refreshed = authService.refreshSession(active.refreshToken).getOrNull()
      if (refreshed != null) {
        active = active.copy(
          accessToken = refreshed.accessToken,
          refreshToken = refreshed.refreshToken
        )
        authService.fetchUserRole(active.userId, active.accessToken).getOrNull()
      } else null
    } else null

    if (resolvedRole != null && !AdminRoleHelper.isAdmin(resolvedRole)) {
      AdminPushTokenManager.unregisterOnLogout(getApplication(), active.accessToken)
      sessionManager.clearSession()
      _state.update {
        it.copy(
          isInitializing = false,
          session = null,
          errorMessage = "Admin access was revoked for this account."
        )
      }
      return
    }

    val updated = if (resolvedRole != null) active.copy(role = resolvedRole) else active
    sessionManager.saveSession(updated)
    _state.update { it.copy(isInitializing = false, session = updated) }
    AdminPushTokenManager.syncCurrentToken(getApplication(), updated.accessToken)
  }

  fun login(email: String, password: String) {
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank() || password.isBlank()) {
      _state.update { it.copy(errorMessage = "Enter both your admin email and password.") }
      return
    }
    _state.update { it.copy(isLoading = true, errorMessage = null) }
    viewModelScope.launch {
      val signInRes = authService.signIn(cleanEmail, password)
      signInRes.fold(
        onSuccess = { payload ->
          val roleRes = authService.fetchUserRole(payload.userId, payload.accessToken)
          roleRes.fold(
            onSuccess = { role ->
              if (!AdminRoleHelper.isAdmin(role)) {
                sessionManager.clearSession()
                _state.update {
                  it.copy(
                    isLoading = false,
                    session = null,
                    errorMessage = "Access denied. This account ($cleanEmail) has role '${AdminRoleHelper.formatDisplayName(role)}'. Only Admins can sign in."
                  )
                }
              } else {
                val profileName = authService.fetchDisplayName(payload.userId, payload.accessToken)
                val session = AdminSession(
                  userId = payload.userId,
                  email = payload.email,
                  fullName = profileName.ifBlank { payload.fullName.ifBlank { "Administrator" } },
                  role = role,
                  accessToken = payload.accessToken,
                  refreshToken = payload.refreshToken
                )
                sessionManager.saveSession(session)
                _state.update { it.copy(isLoading = false, session = session, errorMessage = null) }
                AdminPushTokenManager.syncCurrentToken(getApplication(), session.accessToken)
              }
            },
            onFailure = { err ->
              _state.update {
                it.copy(isLoading = false, errorMessage = err.message ?: "Failed to verify admin role.")
              }
            }
          )
        },
        onFailure = { err ->
          _state.update {
            it.copy(isLoading = false, errorMessage = err.message ?: "Sign in failed.")
          }
        }
      )
    }
  }

  fun logout() {
    val token = _state.value.session?.accessToken.orEmpty()
    AdminPushTokenManager.unregisterOnLogout(getApplication(), token)
    viewModelScope.launch {
      sessionManager.clearSession()
      _state.update { AdminAuthState(isInitializing = false, session = null) }
    }
  }

  fun clearError() {
    _state.update { it.copy(errorMessage = null) }
  }
}
