package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AcademicYearItem
import com.example.data.ProfileItem
import com.example.data.ProgrammeItem
import com.example.data.SchoolItem
import com.example.data.UniversityService
import com.example.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

data class UniversityUiState(
  val schools: List<SchoolItem> = emptyList(),
  val programmes: List<ProgrammeItem> = emptyList(),
  val academicYears: List<AcademicYearItem> = emptyList(),
  val profile: ProfileItem? = null,
  val isSchoolsLoading: Boolean = false,
  val isProgrammesLoading: Boolean = false,
  val isProfileLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null
)

class UniversityViewModel : ViewModel() {

  private val universityService = UniversityService()

  private val _uiState = MutableStateFlow(UniversityUiState())
  val uiState: StateFlow<UniversityUiState> = _uiState.asStateFlow()

  fun loadSchools(token: String? = null) {
    _uiState.update { it.copy(isSchoolsLoading = true) }
    viewModelScope.launch {
      val res = universityService.fetchSchools(token)
      res.fold(
        onSuccess = { list -> _uiState.update { it.copy(isSchoolsLoading = false, schools = list) } },
        onFailure = { err -> _uiState.update { it.copy(isSchoolsLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun loadProgrammesForSchool(schoolId: String, token: String? = null) {
    if (schoolId.isBlank()) {
      _uiState.update { it.copy(programmes = emptyList()) }
      return
    }
    _uiState.update { it.copy(isProgrammesLoading = true) }
    viewModelScope.launch {
      val res = universityService.fetchProgrammes(schoolId, token)
      res.fold(
        onSuccess = { list -> _uiState.update { it.copy(isProgrammesLoading = false, programmes = list) } },
        onFailure = { err -> _uiState.update { it.copy(isProgrammesLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun loadAcademicYears(session: UserSession?) {
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      val res = universityService.fetchAcademicYears(session.accessToken)
      res.fold(
        onSuccess = { list -> _uiState.update { it.copy(academicYears = list) } },
        onFailure = { err -> _uiState.update { it.copy(errorMessage = err.message) } }
      )
    }
  }

  fun loadProfile(session: UserSession?) {
    if (session == null || session.userId.isBlank() || session.accessToken.isBlank()) return
    _uiState.update { it.copy(isProfileLoading = true, errorMessage = null) }
    viewModelScope.launch {
      val res = universityService.fetchProfile(session.userId, session.accessToken)
      res.fold(
        onSuccess = { prof -> _uiState.update { it.copy(isProfileLoading = false, profile = prof) } },
        onFailure = { err -> _uiState.update { it.copy(isProfileLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun updateProfile(
    userId: String,
    token: String,
    fullName: String,
    school: SchoolItem?,
    programme: ProgrammeItem?,
    yearOfStudy: Int?,
    onSuccess: () -> Unit = {}
  ) {
    if (userId.isBlank() || token.isBlank()) return
    val cleanName = fullName.trim()
    if (cleanName.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Name cannot be empty.") }
      return
    }
    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }

    val payload = JSONObject().apply {
      put("full_name", cleanName)
      if (school != null) {
        put("school_id", school.id)
        if (school.universityId.isNotBlank()) put("university_id", school.universityId)
      }
      if (programme != null) {
        put("programme_id", programme.id)
      }
      if (yearOfStudy != null) {
        put("year_of_study", yearOfStudy)
      }
    }

    viewModelScope.launch {
      val res = universityService.patchProfile(userId, token, payload)
      res.fold(
        onSuccess = {
          _uiState.update { state ->
            val updatedProfile = state.profile?.copy(
              fullName = cleanName,
              schoolId = school?.id ?: state.profile.schoolId,
              schoolName = school?.name ?: state.profile.schoolName,
              programmeId = programme?.id ?: state.profile.programmeId,
              programmeName = programme?.name ?: state.profile.programmeName,
              yearOfStudy = yearOfStudy ?: state.profile.yearOfStudy,
              universityId = school?.universityId ?: state.profile.universityId
            )
            state.copy(
              isActionLoading = false,
              profile = updatedProfile,
              successMessage = "Profile updated successfully!"
            )
          }
          onSuccess()
        },
        onFailure = { err ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) }
        }
      )
    }
  }

  private val lecturerService = com.example.data.LecturerService()

  fun becomeLecturer(
    passcode: String,
    session: UserSession?,
    onSuccess: () -> Unit
  ) {
    val cleanPasscode = passcode.trim()
    if (cleanPasscode.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Please enter the lecturer passcode.") }
      return
    }
    if (session == null || session.accessToken.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Session expired. Please sign in again.") }
      return
    }

    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }

    viewModelScope.launch {
      val res = lecturerService.becomeLecturer(cleanPasscode, session.accessToken)
      when (res) {
        is com.example.data.BecomeLecturerResult.Success -> {
          _uiState.update {
            it.copy(
              isActionLoading = false,
              successMessage = "Congratulations! You are now a Lecturer."
            )
          }
          onSuccess()
        }
        is com.example.data.BecomeLecturerResult.WrongPasscode -> {
          _uiState.update {
            it.copy(
              isActionLoading = false,
              errorMessage = "Wrong passcode. Please check and try again."
            )
          }
        }
        is com.example.data.BecomeLecturerResult.Error -> {
          _uiState.update {
            it.copy(
              isActionLoading = false,
              errorMessage = res.message
            )
          }
        }
      }
    }
  }

  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun clearSuccess() {
    _uiState.update { it.copy(successMessage = null) }
  }
}
