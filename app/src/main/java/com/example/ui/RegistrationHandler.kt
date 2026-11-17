package com.example.ui

import com.example.data.*
import com.example.util.RoleHelper
import org.json.JSONObject

class RegistrationHandler(
  private val authService: SupabaseAuthService,
  private val classesService: ClassesService,
  private val sessionManager: SessionManager,
  private val onSubscribeRealtime: (UserSession) -> Unit = {},
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
      requestedRole = RoleHelper.ROLE_STUDENT,
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

        val session = resolveAndSaveSession(auth, cleanName, cleanAdm) ?: return@fold
        if (RoleHelper.isPendingApproval(session.roleStatus)) {
          updateState { it.copy(currentScreen = Screen.PendingApproval, isLoading = false) }
          return@fold
        }

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
      requestedRole = RoleHelper.ROLE_STUDENT,
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

        val session = resolveAndSaveSession(auth, cleanName, cleanAdm) ?: return@fold
        if (RoleHelper.isPendingApproval(session.roleStatus)) {
          updateState { it.copy(currentScreen = Screen.PendingApproval, isLoading = false) }
          return@fold
        }

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

  suspend fun registerLecturer(
    fullName: String,
    staffNumber: String,
    department: String,
    email: String,
    password: String,
    onActiveLecturer: (UserSession) -> Unit,
    onPendingApproval: (UserSession) -> Unit
  ) {
    val cleanName = fullName.trim()
    val cleanStaff = staffNumber.trim()
    val cleanDepartment = department.trim()
    val cleanEmail = email.trim().lowercase()
    val cleanPassword = password.trim()

    if (cleanName.isBlank() || cleanStaff.isBlank() || cleanDepartment.isBlank() || cleanEmail.isBlank() || cleanPassword.isBlank()) {
      updateState { it.copy(errorMessage = "Please fill in full name, staff number, department, email, and password.") }
      return
    }

    updateState { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

    val signupResult = authService.signUp(
      fullName = cleanName,
      email = cleanEmail,
      password = cleanPassword,
      requestedRole = RoleHelper.ROLE_LECTURER,
      staffNumber = cleanStaff,
      department = cleanDepartment
    )
    signupResult.fold(
      onSuccess = { auth ->
        if (!auth.hasSession || auth.accessToken.isBlank()) {
          updateState {
            it.copy(
              isLoading = false,
              currentScreen = Screen.Login,
              infoMessage = "Lecturer account created. Please verify your email and sign in."
            )
          }
          return@fold
        }

        val session = resolveAndSaveSession(
          auth = auth,
          fallbackName = cleanName,
          fallbackAdmission = "",
          fallbackStaff = cleanStaff,
          fallbackDept = cleanDepartment
        ) ?: return@fold

        if (RoleHelper.isPendingApproval(session.roleStatus)) {
          updateState { it.copy(currentScreen = Screen.PendingApproval, isLoading = false, errorMessage = null) }
          onPendingApproval(session)
        } else {
          updateState { it.copy(currentScreen = Screen.Home, isLoading = false, errorMessage = null) }
          onActiveLecturer(session)
        }
      },
      onFailure = { error ->
        updateState { it.copy(isLoading = false, errorMessage = error.message ?: "Registration failed.") }
      }
    )
  }

  private suspend fun resolveAndSaveSession(
    auth: SupabaseAuthService.AuthResult,
    fallbackName: String,
    fallbackAdmission: String,
    fallbackStaff: String = "",
    fallbackDept: String = ""
  ): UserSession? {
    val profileRes = authService.fetchProfileRoleInfo(auth.userId, auth.accessToken)
    val profile = profileRes.getOrElse { err ->
      updateState {
        it.copy(
          isLoading = false,
          errorMessage = err.message ?: "Unable to verify account role from profile."
        )
      }
      return null
    }

    if (RoleHelper.isRejectedStatus(profile.roleStatus)) {
      sessionManager.clearSession()
      updateState {
        it.copy(
          session = null,
          isLoading = false,
          currentScreen = Screen.Login,
          errorMessage = "Your account application was rejected by an administrator."
        )
      }
      return null
    }

    val session = UserSession(
      userId = auth.userId,
      email = auth.email,
      fullName = profile.fullName.ifBlank { fallbackName },
      admissionNumber = profile.admissionNumber.ifBlank { fallbackAdmission },
      role = profile.role,
      accessToken = auth.accessToken,
      refreshToken = auth.refreshToken,
      isVerified = profile.isVerified,
      roleStatus = profile.roleStatus,
      staffNumber = profile.staffNumber.ifBlank { fallbackStaff },
      department = profile.department.ifBlank { fallbackDept }
    )
    sessionManager.saveSession(session)
    OfflineCache.currentUserId = session.userId
    onSubscribeRealtime(session)
    updateState { it.copy(session = session, isLoading = false) }
    return session
  }
}
