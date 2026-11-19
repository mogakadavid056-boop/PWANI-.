package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AttendanceTrackerUiState(
  val classId: String = "",
  val students: List<StudentRiskItem> = emptyList(),
  val selectedLevel: RiskLevel = RiskLevel.ALL,
  val searchQuery: String = "",
  val isLoading: Boolean = false,
  val selectedStudent: StudentRiskItem? = null,
  val timeline: List<StudentSessionTimelineItem> = emptyList(),
  val isDetailLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val myStatus: MyAttendanceStatus? = null,
  val isMyStatusLoading: Boolean = false
) {
  val filteredStudents: List<StudentRiskItem>
    get() {
      val q = searchQuery.trim()
      return students.filter { item ->
        val matchesLevel = selectedLevel == RiskLevel.ALL || item.riskLevel == selectedLevel
        val matchesQuery = q.isBlank() ||
          item.fullName.contains(q, ignoreCase = true) ||
          item.admissionNumber.contains(q, ignoreCase = true)
        matchesLevel && matchesQuery
      }
    }

  fun countFor(level: RiskLevel): Int = when (level) {
    RiskLevel.ALL -> students.size
    else -> students.count { it.riskLevel == level }
  }
}

class AttendanceTrackerViewModel : ViewModel() {
  private val service = AttendanceTrackerService()
  private val _uiState = MutableStateFlow(AttendanceTrackerUiState())
  val uiState: StateFlow<AttendanceTrackerUiState> = _uiState.asStateFlow()

  fun loadClassRisk(classId: String, session: UserSession?) {
    if (classId.isBlank() || session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(classId = classId, isLoading = true, errorMessage = null) }
      service.getClassAttendanceRisk(classId, session.accessToken).fold(
        onSuccess = { list ->
          _uiState.update { st ->
            val updatedSel = st.selectedStudent?.let { sel -> list.find { it.studentId == sel.studentId } ?: sel }
            st.copy(students = list, isLoading = false, selectedStudent = updatedSel)
          }
        },
        onFailure = { err ->
          _uiState.update { it.copy(isLoading = false, errorMessage = err.message ?: "Failed to load tracker") }
        }
      )
    }
  }

  fun loadMyStatus(classId: String, session: UserSession?) {
    if (classId.isBlank() || session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(classId = classId, isMyStatusLoading = true) }
      service.getMyAttendanceStatus(classId, session.accessToken).fold(
        onSuccess = { status -> _uiState.update { it.copy(myStatus = status, isMyStatusLoading = false) } },
        onFailure = { _uiState.update { it.copy(isMyStatusLoading = false) } }
      )
    }
  }

  fun openStudentDetail(student: StudentRiskItem, classId: String, session: UserSession?) {
    if (session == null || session.accessToken.isBlank()) return
    _uiState.update { it.copy(selectedStudent = student, timeline = emptyList(), isDetailLoading = true, errorMessage = null) }
    viewModelScope.launch {
      service.getStudentAttendanceDetail(classId, student.studentId, session.accessToken).fold(
        onSuccess = { rows -> _uiState.update { it.copy(timeline = rows, isDetailLoading = false) } },
        onFailure = { err -> _uiState.update { it.copy(isDetailLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun closeStudentDetail() {
    _uiState.update { it.copy(selectedStudent = null, timeline = emptyList(), errorMessage = null, successMessage = null) }
  }

  fun setAbsenceExcused(
    sessionId: String,
    excused: Boolean,
    reason: String?,
    classId: String,
    session: UserSession?
  ) {
    val student = _uiState.value.selectedStudent ?: return
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      service.setAbsenceExcused(sessionId, student.studentId, excused, reason, session.accessToken).fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              isActionLoading = false,
              successMessage = if (excused) "Absence marked as excused." else "Excuse removed."
            )
          }
          openStudentDetail(student, classId, session)
          loadClassRisk(classId, session)
        },
        onFailure = { err ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message ?: "Action failed") }
        }
      )
    }
  }

  fun selectLevel(level: RiskLevel) { _uiState.update { it.copy(selectedLevel = level) } }
  fun setSearchQuery(query: String) { _uiState.update { it.copy(searchQuery = query) } }
  fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
  fun clearSuccess() { _uiState.update { it.copy(successMessage = null) } }
}
