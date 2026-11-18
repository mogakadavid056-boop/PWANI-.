package com.example.ui

import com.example.data.ClassesService
import com.example.data.OfflineCache
import com.example.data.ProgrammeItem
import com.example.data.SchoolItem
import com.example.data.SessionManager
import com.example.data.SupabaseAuthService
import com.example.data.UserRoleInfo
import com.example.data.UserSession
import org.json.JSONObject

class RegistrationHandler(
  private val authService: SupabaseAuthService,
  private val classesService: ClassesService,
  private val sessionManager: SessionManager,
  private val updateState: ((AuthUiState) -> AuthUiState) -> Unit
) {

  suspend fun registerWithKnown(
    fullName: String,
    admissionNumber: String,
    email: String,
    password: String,
    school: SchoolItem?,
    programme: ProgrammeItem?,
    yearOfStudy: Int?,
    onClassResolved: (classId: String?) -> Unit
  ) {
    val cleanName = fullName.trim()
    val cleanAdm = admissionNumber.trim()
    val cleanEmail = email.trim().lowercase()
    val cleanPassword = password.trim()

    if (cleanName.isBlank() || cleanAdm.isBlank() || cleanEmail.isBlank() || cleanPassword.isBlank()) {
      updateState { it.copy(errorMessage = "Please fill in all registration fields.") }
      return
    }
    if (school == null || programme == null || yearOfStudy == null) {
      updateState { it.copy(errorMessage = "Please select School, Programme, and Year of Study.") }
      return
    }

    updateState { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

    val signupResult = authService.signUp(
      fullName = cleanName,
      email = cleanEmail,
      password = cleanPassword,
      admissionNumber = cleanAdm
    )
    signupResult.fold(
      onSuccess = { auth ->
        if (!auth.hasSession || auth.accessToken.isBlank()) {
          updateState {
            it.copy(
              isLoading = false,
              currentScreen = Screen.Login,
              infoMessage = "Account created. Please verify your email and sign in."
            )
          }
          return@fold
        }

        val patchJson = JSONObject().apply {
          put("admission_number", cleanAdm)
          put("university_id", school.universityId)
          put("school_id", school.id)
          put("programme_id", programme.id)
          put("year_of_study", yearOfStudy)
        }
        authService.patchProfile(auth.userId, auth.accessToken, patchJson)

        resolveAndSaveSession(auth, cleanName, cleanAdm)

        val joinResult = classesService.joinMyClass(auth.accessToken)
        val classId = joinResult.getOrNull()
        if (!classId.isNullOrBlank()) {
          onClassResolved(classId)
        } else {
          updateState { it.copy(currentScreen = Screen.Waiting) }
          onClassResolved(null)
        }
      },
      onFailure = { error ->
        updateState { it.copy(isLoading = false, errorMessage = error.message ?: "Registration failed.") }
      }
    )
  }

  suspend fun registerWithCode(
    fullName: String,
    admissionNumber: String,
    email: String,
    password: String,
    classCode: String,
    onClassResolved: (classId: String?) -> Unit
  ) {
    val cleanName = fullName.trim()
    val cleanAdm = admissionNumber.trim()
    val cleanEmail = email.trim().lowercase()
    val cleanPassword = password.trim()
    val cleanCode = classCode.trim()

    if (cleanName.isBlank() || cleanAdm.isBlank() || cleanEmail.isBlank() || cleanPassword.isBlank()) {
      updateState { it.copy(errorMessage = "Please fill in all registration fields.") }
      return
    }
    if (cleanCode.isBlank()) {
      updateState { it.copy(errorMessage = "Please enter your class code.") }
      return
    }

    updateState { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

    val signupResult = authService.signUp(
      fullName = cleanName,
      email = cleanEmail,
      password = cleanPassword,
      admissionNumber = cleanAdm
    )
    signupResult.fold(
      onSuccess = { auth ->
        if (!auth.hasSession || auth.accessToken.isBlank()) {
          updateState {
            it.copy(
              isLoading = false,
              currentScreen = Screen.Login,
              infoMessage = "Account created. Please verify your email and sign in."
            )
          }
          return@fold
        }

        val patchJson = JSONObject().apply { put("admission_number", cleanAdm) }
        authService.patchProfile(auth.userId, auth.accessToken, patchJson)

        resolveAndSaveSession(auth, cleanName, cleanAdm)

        val joinResult = classesService.joinClass(cleanCode, auth.accessToken)
        joinResult.fold(
          onSuccess = { classId -> onClassResolved(classId) },
          onFailure = { err ->
            updateState {
              it.copy(currentScreen = Screen.Waiting, errorMessage = err.message ?: "Failed to join class.")
            }
            onClassResolved(null)
          }
        )
      },
      onFailure = { error ->
        updateState { it.copy(isLoading = false, errorMessage = error.message ?: "Registration failed.") }
      }
    )
  }

  private suspend fun resolveAndSaveSession(
    auth: SupabaseAuthService.AuthResult,
    fallbackName: String,
    fallbackAdmission: String
  ): UserSession {
    val roleInfo = authService.fetchUserRole(auth.userId, auth.accessToken)
      .getOrElse { UserRoleInfo() }

    val session = UserSession(
      userId = auth.userId,
      email = auth.email,
      fullName = auth.fullName.ifBlank { fallbackName },
      admissionNumber = auth.admissionNumber.ifBlank { fallbackAdmission },
      role = roleInfo.role,
      accessToken = auth.accessToken,
      refreshToken = auth.refreshToken,
      isVerified = roleInfo.isVerified
    )
    sessionManager.saveSession(session)
    OfflineCache.currentUserId = session.userId
    updateState { it.copy(session = session, isLoading = false) }
    return session
  }
}
