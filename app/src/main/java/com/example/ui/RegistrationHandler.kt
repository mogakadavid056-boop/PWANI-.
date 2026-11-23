package com.example.ui

import com.example.data.BecomeLecturerResult
import com.example.data.ClassesService
import com.example.data.LecturerService
import com.example.data.OfflineCache
import com.example.data.SessionManager
import com.example.data.SupabaseAuthService
import com.example.data.UserRoleInfo
import com.example.data.UserSession
import com.example.ui.screens.register.RegisterWizardState
import com.example.util.PasswordStrengthHelper
import com.example.util.RoleHelper
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class RegistrationHandler(
  private val authService: SupabaseAuthService,
  private val classesService: ClassesService,
  private val lecturerService: LecturerService,
  private val sessionManager: SessionManager,
  private val onSessionReady: (UserSession) -> Unit,
  private val updateState: ((AuthUiState) -> AuthUiState) -> Unit
) {
  private val isSubmitting = AtomicBoolean(false)

  suspend fun submitWizard(wizard: RegisterWizardState) {
    if (!isSubmitting.compareAndSet(false, true)) return
    try {
      val cleanName = wizard.fullName.trim()
      val cleanId = wizard.idNumber.trim()
      val cleanEmail = wizard.email.trim().lowercase()
      val cleanPhone = wizard.phone.trim()
      val cleanPassword = wizard.password
      val acceptedAt = PasswordStrengthHelper.currentUtcIso8601()

      updateState { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

      val signupResult = authService.signUp(
        fullName = cleanName,
        email = cleanEmail,
        password = cleanPassword,
        admissionNumber = cleanId.ifBlank { null }
      )

      signupResult.fold(
        onSuccess = { auth ->
          val universityId = if (!wizard.isLecturer && !wizard.knowsDetails) "" else wizard.selectedSchool?.universityId.orEmpty()
          val schoolId = if (!wizard.isLecturer && !wizard.knowsDetails) "" else wizard.selectedSchool?.id.orEmpty()
          val programmeId = if (!wizard.isLecturer && wizard.knowsDetails) wizard.selectedProgramme?.id.orEmpty() else ""
          val yearOfStudy = if (!wizard.isLecturer && wizard.knowsDetails) wizard.selectedYearOfStudy else null
          val cleanCode = wizard.classCode.trim().uppercase()

          if (!auth.hasSession || auth.accessToken.isBlank()) {
            sessionManager.savePendingRegistration(
              SessionManager.PendingRegistrationData(
                email = cleanEmail,
                fullName = cleanName,
                isLecturer = wizard.isLecturer,
                idNumber = cleanId,
                phone = cleanPhone,
                universityId = universityId,
                schoolId = schoolId,
                programmeId = programmeId,
                yearOfStudy = yearOfStudy,
                knowsDetails = wizard.knowsDetails,
                classCode = cleanCode,
                acceptedTermsAt = acceptedAt,
                termsVersion = "1.0"
              )
            )
            updateState {
              it.copy(
                isLoading = false,
                currentScreen = Screen.CheckEmail,
                pendingConfirmationEmail = cleanEmail,
                errorMessage = null,
                infoMessage = null
              )
            }
            return@fold
          }

          val patchJson = buildProfilePatch(
            fullName = cleanName,
            idNumber = cleanId,
            phone = cleanPhone,
            universityId = universityId,
            schoolId = schoolId,
            programmeId = programmeId,
            yearOfStudy = yearOfStudy,
            acceptedTermsAt = acceptedAt,
            termsVersion = "1.0"
          )
          authService.patchProfile(auth.userId, auth.accessToken, patchJson)

          val session = resolveAndSaveSession(auth, cleanName, cleanId)
          onSessionReady(session)

          if (wizard.isLecturer) {
            completeLecturerSignUp(wizard.lecturerPasscode, session)
          } else {
            completeStudentClassJoin(wizard.knowsDetails, cleanCode, session)
          }
        },
        onFailure = { error ->
          val friendly = PasswordStrengthHelper.formatRegistrationError(error.message)
          updateState { it.copy(isLoading = false, errorMessage = friendly) }
        }
      )
    } finally {
      isSubmitting.set(false)
    }
  }

  suspend fun completePendingOnLogin(session: UserSession): Boolean {
    val pending = sessionManager.getPendingRegistration(session.email) ?: return false
    val patchJson = buildProfilePatch(
      fullName = pending.fullName.ifBlank { session.fullName },
      idNumber = pending.idNumber,
      phone = pending.phone,
      universityId = pending.universityId,
      schoolId = pending.schoolId,
      programmeId = pending.programmeId,
      yearOfStudy = pending.yearOfStudy,
      acceptedTermsAt = pending.acceptedTermsAt.ifBlank { PasswordStrengthHelper.currentUtcIso8601() },
      termsVersion = pending.termsVersion.ifBlank { "1.0" }
    )
    authService.patchProfile(session.userId, session.accessToken, patchJson)
    sessionManager.clearPendingRegistration()

    if (pending.isLecturer) {
      updateState {
        it.copy(
          infoMessage = "Email confirmed. Enter your lecturer passcode from Profile to complete verification.",
          isLoading = false
        )
      }
      return false
    }

    completeStudentClassJoin(pending.knowsDetails, pending.classCode, session)
    return true
  }

  suspend fun resendConfirmationEmail(email: String) {
    if (email.isBlank()) return
    updateState { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
    authService.resendSignUpEmail(email).fold(
      onSuccess = {
        updateState {
          it.copy(isLoading = false, infoMessage = "Confirmation link sent again to $email.")
        }
      },
      onFailure = { err ->
        updateState {
          it.copy(isLoading = false, errorMessage = PasswordStrengthHelper.formatRegistrationError(err.message))
        }
      }
    )
  }

  suspend fun retryStudentAutoJoin(session: UserSession) {
    updateState { it.copy(isLoading = true, errorMessage = null) }
    completeStudentClassJoin(knowsDetails = true, classCode = "", session = session)
  }

  suspend fun retryStudentJoinByCode(code: String, session: UserSession) {
    val cleanCode = code.trim().uppercase()
    if (cleanCode.isBlank()) {
      updateState { it.copy(errorMessage = "Please enter your class code.") }
      return
    }
    updateState { it.copy(isLoading = true, errorMessage = null) }
    completeStudentClassJoin(knowsDetails = false, classCode = cleanCode, session = session)
  }

  private suspend fun completeStudentClassJoin(
    knowsDetails: Boolean,
    classCode: String,
    session: UserSession
  ) {
    val joinResult = if (knowsDetails) {
      classesService.joinMyClass(session.accessToken)
    } else {
      classesService.joinClass(classCode, session.accessToken)
    }
    var classId = joinResult.getOrNull()
    if (classId.isNullOrBlank()) {
      classId = classesService.fetchClasses(session.userId, session.role, session.accessToken)
        .getOrNull()?.firstOrNull()?.id
    }
    if (!classId.isNullOrBlank()) {
      updateState {
        it.copy(
          isLoading = false,
          currentScreen = Screen.Home,
          registerResult = null,
          errorMessage = null,
          infoMessage = "Welcome to your class!",
          pendingOpenClassId = classId
        )
      }
    } else {
      val joinErr = joinResult.exceptionOrNull()?.message?.ifBlank { null }
      val note = if (!joinErr.isNullOrBlank()) {
        "Account created! $joinErr You can join with a class code below."
      } else {
        "Account created! Use Join Class when you have a class code."
      }
      updateState {
        it.copy(
          isLoading = false,
          currentScreen = Screen.Home,
          registerResult = null,
          errorMessage = null,
          infoMessage = note
        )
      }
    }
  }

  private suspend fun completeLecturerSignUp(passcode: String, session: UserSession) {
    val res = lecturerService.becomeLecturer(passcode, session.accessToken)
    val roleInfo = authService.fetchUserRole(session.userId, session.accessToken).getOrNull()
    val isLecturerRole = res is BecomeLecturerResult.Success || RoleHelper.isLecturer(roleInfo?.role)
    if (isLecturerRole) {
      val newRole = roleInfo?.role?.takeIf { RoleHelper.isLecturer(it) } ?: RoleHelper.ROLE_LECTURER
      val verified = roleInfo?.isVerified == true
      val updated = session.copy(role = newRole, isVerified = verified)
      sessionManager.updateRoleAndVerification(newRole, verified)
      val note = if (verified) {
        "Lecturer account ready! Tap Find my classes to add your classes."
      } else {
        "Lecturer account created! An administrator will verify you shortly."
      }
      updateState {
        it.copy(
          session = updated,
          isLoading = false,
          errorMessage = null,
          infoMessage = note,
          currentScreen = Screen.Home,
          registerResult = null
        )
      }
    } else {
      val note = when (res) {
        is BecomeLecturerResult.WrongPasscode ->
          "Passcode did not match. Signed in as Student — open Profile and tap I am a Lecturer to retry."
        is BecomeLecturerResult.Error ->
          "Signed in! Could not verify lecturer passcode (${res.message}). Retry anytime from Profile."
        else -> "Signed in! Open Profile to complete lecturer access."
      }
      updateState {
        it.copy(
          isLoading = false,
          errorMessage = null,
          infoMessage = note,
          currentScreen = Screen.Home,
          registerResult = null
        )
      }
    }
  }

  private fun buildProfilePatch(
    fullName: String,
    idNumber: String,
    phone: String,
    universityId: String,
    schoolId: String,
    programmeId: String,
    yearOfStudy: Int?,
    acceptedTermsAt: String,
    termsVersion: String
  ): JSONObject = JSONObject().apply {
    if (fullName.isNotBlank()) put("full_name", fullName.trim())
    if (idNumber.isNotBlank()) put("admission_number", idNumber)
    if (phone.isNotBlank()) put("phone", phone)
    if (universityId.isNotBlank()) put("university_id", universityId)
    if (schoolId.isNotBlank()) put("school_id", schoolId)
    if (programmeId.isNotBlank()) put("programme_id", programmeId)
    if (yearOfStudy != null) put("year_of_study", yearOfStudy)
    put("accepted_terms_at", acceptedTermsAt)
    put("terms_version", termsVersion)
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
    updateState { it.copy(session = session) }
    return session
  }
}
