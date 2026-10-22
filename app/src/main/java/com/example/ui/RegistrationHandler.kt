package com.example.ui

import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

class RegistrationHandler(
  private val authService: SupabaseAuthService,
  private val classesService: ClassesService,
  private val lecturerService: LecturerService,
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

    updateState { it.copy(isLoading = true, errorMessage = null) }

    val signupResult = authService.signUp(cleanName, cleanAdm, cleanEmail, cleanPassword)
    signupResult.fold(
      onSuccess = { auth ->
        val patchJson = JSONObject().apply {
          put("admission_number", cleanAdm)
          put("university_id", school.universityId)
          put("school_id", school.id)
          put("programme_id", programme.id)
          put("year_of_study", yearOfStudy)
        }
        authService.patchProfile(auth.userId, auth.accessToken, patchJson)
        val role = authService.fetchUserRole(auth.userId, auth.accessToken).getOrDefault("Student")
        val session = UserSession(
          userId = auth.userId, email = auth.email, fullName = cleanName,
          admissionNumber = cleanAdm, role = role, accessToken = auth.accessToken, refreshToken = auth.refreshToken
        )
        sessionManager.saveSession(session)
        com.example.data.OfflineCache.currentUserId = session.userId
        updateState { it.copy(session = session, isLoading = false) }

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

    updateState { it.copy(isLoading = true, errorMessage = null) }

    val signupResult = authService.signUp(cleanName, cleanAdm, cleanEmail, cleanPassword)
    signupResult.fold(
      onSuccess = { auth ->
        authService.patchAdmissionNumber(auth.userId, auth.accessToken, cleanAdm)
        val role = authService.fetchUserRole(auth.userId, auth.accessToken).getOrDefault("Student")
        val session = UserSession(
          userId = auth.userId, email = auth.email, fullName = cleanName,
          admissionNumber = cleanAdm, role = role, accessToken = auth.accessToken, refreshToken = auth.refreshToken
        )
        sessionManager.saveSession(session)
        com.example.data.OfflineCache.currentUserId = session.userId
        updateState { it.copy(session = session, isLoading = false) }

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
    email: String,
    password: String,
    passcode: String,
    onSuccessLecturer: (UserSession) -> Unit,
    onWrongPasscode: (UserSession, String) -> Unit
  ) {
    val cleanName = fullName.trim()
    val cleanStaff = staffNumber.trim()
    val cleanEmail = email.trim().lowercase()
    val cleanPassword = password.trim()
    val cleanPasscode = passcode.trim()

    if (cleanName.isBlank() || cleanEmail.isBlank() || cleanPassword.isBlank()) {
      updateState { it.copy(errorMessage = "Please enter your name, email, and password.") }
      return
    }
    if (cleanPasscode.isBlank()) {
      updateState { it.copy(errorMessage = "Please enter the lecturer passcode.") }
      return
    }

    updateState { it.copy(isLoading = true, errorMessage = null) }

    val signupResult = authService.signUp(cleanName, cleanStaff, cleanEmail, cleanPassword)
    signupResult.fold(
      onSuccess = { auth ->
        val patchJson = JSONObject().apply {
          put("admission_number", cleanStaff)
        }
        authService.patchProfile(auth.userId, auth.accessToken, patchJson)

        val becomeRes = lecturerService.becomeLecturer(cleanPasscode, auth.accessToken)
        when (becomeRes) {
          is BecomeLecturerResult.Success -> {
            val session = UserSession(
              userId = auth.userId, email = auth.email, fullName = cleanName,
              admissionNumber = cleanStaff, role = "Lecturer", accessToken = auth.accessToken, refreshToken = auth.refreshToken
            )
            sessionManager.saveSession(session)
            com.example.data.OfflineCache.currentUserId = session.userId
            updateState { it.copy(session = session, isLoading = false, errorMessage = null, currentScreen = Screen.Home) }
            onSuccessLecturer(session)
          }
          is BecomeLecturerResult.WrongPasscode -> {
            val session = UserSession(
              userId = auth.userId, email = auth.email, fullName = cleanName,
              admissionNumber = cleanStaff, role = "Student", accessToken = auth.accessToken, refreshToken = auth.refreshToken
            )
            sessionManager.saveSession(session)
            com.example.data.OfflineCache.currentUserId = session.userId
            val infoMsg = "Wrong passcode. Your account was created as a student. You can try again from your Profile."
            updateState {
              it.copy(
                session = session,
                isLoading = false,
                errorMessage = infoMsg,
                currentScreen = Screen.Waiting
              )
            }
            onWrongPasscode(session, infoMsg)
          }
          is BecomeLecturerResult.Error -> {
            updateState { it.copy(isLoading = false, errorMessage = becomeRes.message) }
          }
        }
      },
      onFailure = { error ->
        updateState { it.copy(isLoading = false, errorMessage = error.message ?: "Registration failed.") }
      }
    )
  }
}
