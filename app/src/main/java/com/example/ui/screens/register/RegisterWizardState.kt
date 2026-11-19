package com.example.ui.screens.register

import com.example.data.ProgrammeItem
import com.example.data.SchoolItem
import com.example.util.PasswordStrengthHelper

enum class RegisterWizardRole(val label: String) {
  STUDENT("Student"),
  LECTURER("Lecturer")
}

data class RegisterWizardState(
  val step: Int = 1,
  val slideDirection: Int = 1,
  val role: RegisterWizardRole? = null,
  val fullName: String = "",
  val nameTouched: Boolean = false,
  val idNumber: String = "",
  val idTouched: Boolean = false,
  val email: String = "",
  val emailTouched: Boolean = false,
  val phone: String = "",
  val password: String = "",
  val passwordTouched: Boolean = false,
  val confirmPassword: String = "",
  val confirmTouched: Boolean = false,
  val knowsDetails: Boolean = true,
  val selectedSchool: SchoolItem? = null,
  val selectedProgramme: ProgrammeItem? = null,
  val selectedYearOfStudy: Int? = null,
  val classCode: String = "",
  val lecturerPasscode: String = "",
  val agreedToTerms: Boolean = false
) {
  val isLecturer: Boolean get() = role == RegisterWizardRole.LECTURER

  val isStep1Valid: Boolean get() = role != null

  val isStep2Valid: Boolean
    get() = PasswordStrengthHelper.isValidName(fullName) &&
      (isLecturer || idNumber.trim().isNotBlank()) &&
      PasswordStrengthHelper.isValidEmail(email) &&
      PasswordStrengthHelper.isValidPassword(password) &&
      PasswordStrengthHelper.passwordsMatch(password, confirmPassword)

  val isStep3Valid: Boolean
    get() = if (isLecturer) {
      lecturerPasscode.isNotBlank()
    } else if (knowsDetails) {
      selectedSchool != null && selectedProgramme != null && selectedYearOfStudy != null
    } else {
      classCode.trim().isNotBlank()
    }

  val isStep4Valid: Boolean get() = agreedToTerms

  val isCurrentStepValid: Boolean
    get() = when (step) {
      1 -> isStep1Valid
      2 -> isStep2Valid
      3 -> isStep3Valid
      4 -> isStep4Valid
      else -> false
    }
}
