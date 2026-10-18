package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ClassItem
import com.example.data.ClassesService
import com.example.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.data.ClassPrefectInfo
import com.example.data.ClassWaitingForPrefect

data class ClassesUiState(
  val classes: List<ClassItem> = emptyList(),
  val classesWaitingForRep: List<ClassWaitingForPrefect> = emptyList(),
  val isWaitingLoading: Boolean = false,
  val isLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val createdJoinCode: String? = null
)

class ClassesViewModel : ViewModel() {

  private val classesService = ClassesService()
  private val lecturerService = com.example.data.LecturerService()

  private val _uiState = MutableStateFlow(ClassesUiState())
  val uiState: StateFlow<ClassesUiState> = _uiState.asStateFlow()

  fun loadClasses(session: UserSession?, onLoaded: ((List<ClassItem>) -> Unit)? = null) {
    if (session == null || session.userId.isBlank()) return
    _uiState.update { it.copy(isLoading = true, errorMessage = null) }

    viewModelScope.launch {
      val result = classesService.fetchClasses(
        userId = session.userId,
        role = session.role,
        accessToken = session.accessToken
      )

      result.fold(
        onSuccess = { list ->
          _uiState.update {
            it.copy(
              classes = list,
              isLoading = false,
              errorMessage = null
            )
          }
          onLoaded?.invoke(list)
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.message ?: "Failed to load classes"
            )
          }
          onLoaded?.invoke(emptyList())
        }
      )
    }
  }

  fun joinClass(
    joinCode: String,
    session: UserSession?,
    onSuccess: (String?, Boolean) -> Unit = { _, _ -> },
    onError: ((String) -> Unit)? = null
  ) {
    val cleanCode = joinCode.trim()
    if (cleanCode.isBlank()) {
      val msg = "Please enter a class join code."
      _uiState.update { it.copy(errorMessage = msg) }
      onError?.invoke(msg)
      return
    }
    if (session == null || session.accessToken.isBlank()) {
      val msg = "Authentication session expired. Please sign in again."
      _uiState.update { it.copy(errorMessage = msg) }
      onError?.invoke(msg)
      return
    }

    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }

    viewModelScope.launch {
      val result = classesService.joinClass(
        joinCode = cleanCode,
        accessToken = session.accessToken
      )

      result.fold(
        onSuccess = { classId ->
          val isLecturer = session.role.equals("lecturer", ignoreCase = true)
          if (isLecturer && !classId.isNullOrBlank()) {
            val statusRes = lecturerService.checkClassMemberStatus(classId, session.userId, session.accessToken)
            val status = statusRes.getOrDefault("active")
            if (status.equals("pending", ignoreCase = true)) {
              val pendingMsg = "Request sent. The class rep or admin must approve you."
              _uiState.update { it.copy(isActionLoading = false, errorMessage = pendingMsg) }
              onError?.invoke(pendingMsg)
              onSuccess(classId, true)
              return@launch
            }
          }
          _uiState.update { it.copy(isActionLoading = false, errorMessage = null) }
          loadClasses(session)
          onSuccess(classId, false)
        },
        onFailure = { error ->
          val msg = error.message ?: "Failed to join class."
          _uiState.update {
            it.copy(
              isActionLoading = false,
              errorMessage = msg
            )
          }
          onError?.invoke(msg)
        }
      )
    }
  }

  fun joinMyClass(
    session: UserSession?,
    onResult: (classId: String?) -> Unit,
    onError: ((String) -> Unit)? = null
  ) {
    if (session == null || session.accessToken.isBlank()) {
      onError?.invoke("Authentication session expired.")
      return
    }
    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }

    viewModelScope.launch {
      val result = classesService.joinMyClass(session.accessToken)
      result.fold(
        onSuccess = { classId ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = null) }
          if (classId != null) loadClasses(session)
          onResult(classId)
        },
        onFailure = { error ->
          val msg = error.message ?: "Failed to find matching class."
          _uiState.update { it.copy(isActionLoading = false, errorMessage = msg) }
          onError?.invoke(msg)
        }
      )
    }
  }

  fun createClass(
    className: String,
    classCode: String,
    targetUniversityId: String? = null,
    targetSchoolId: String? = null,
    targetProgrammeId: String? = null,
    targetAcademicYearId: String? = null,
    targetYearOfStudy: Int? = null,
    session: UserSession?,
    onSuccess: (joinCode: String) -> Unit
  ) {
    val cleanName = className.trim()
    val cleanCode = classCode.trim()

    if (cleanName.isBlank() || cleanCode.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Please enter both class name and class code.") }
      return
    }
    if (session == null || session.accessToken.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Authentication session expired. Please sign in again.") }
      return
    }

    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }

    viewModelScope.launch {
      val result = classesService.createClass(
        className = cleanName,
        classCode = cleanCode,
        targetUniversityId = targetUniversityId,
        targetSchoolId = targetSchoolId,
        targetProgrammeId = targetProgrammeId,
        targetAcademicYearId = targetAcademicYearId,
        targetYearOfStudy = targetYearOfStudy,
        accessToken = session.accessToken
      )

      result.fold(
        onSuccess = { code ->
          _uiState.update {
            it.copy(
              isActionLoading = false,
              createdJoinCode = code,
              errorMessage = null
            )
          }
          loadClasses(session)
          onSuccess(code)
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isActionLoading = false,
              errorMessage = error.message ?: "Failed to create class."
            )
          }
        }
      )
    }
  }

  fun updateClassName(
    classId: String,
    newName: String,
    session: UserSession?,
    onSuccess: () -> Unit = {}
  ) {
    val cleanName = newName.trim()
    if (cleanName.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Class name cannot be empty.") }
      return
    }
    if (session == null || session.accessToken.isBlank()) return

    _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      val result = classesService.updateClassName(classId, cleanName, session.accessToken)
      result.fold(
        onSuccess = {
          _uiState.update { state ->
            val updated = state.classes.map { if (it.id == classId) it.copy(name = cleanName) else it }
            state.copy(isActionLoading = false, classes = updated)
          }
          onSuccess()
        },
        onFailure = { error ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = error.message) }
        }
      )
    }
  }

  fun loadClassesWaitingForRep(session: UserSession?) {
    if (session == null || session.accessToken.isBlank()) return
    val isStaff = session.role.equals("admin", ignoreCase = true) ||
      session.role.equals("lecturer", ignoreCase = true)
    if (!isStaff) return

    _uiState.update { it.copy(isWaitingLoading = true) }
    viewModelScope.launch {
      val res = classesService.listClassesWithoutRep(session.accessToken)
      res.fold(
        onSuccess = { list ->
          _uiState.update { it.copy(classesWaitingForRep = list, isWaitingLoading = false) }
        },
        onFailure = {
          _uiState.update { it.copy(isWaitingLoading = false) }
        }
      )
    }
  }

  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun dismissCreatedDialog() {
    _uiState.update { it.copy(createdJoinCode = null) }
  }
}
