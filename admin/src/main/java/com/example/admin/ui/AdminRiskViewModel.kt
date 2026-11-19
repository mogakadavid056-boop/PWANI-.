package com.example.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.admin.data.AdminAtRiskStudentItem
import com.example.admin.data.AdminClassRiskSummary
import com.example.admin.data.AdminRiskLevel
import com.example.admin.data.AdminRiskRepository
import com.example.admin.data.AdminSession
import com.example.admin.data.AdminStudentTimelineItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminRiskState(
  val classSummaries: List<AdminClassRiskSummary> = emptyList(),
  val hasMoreClasses: Boolean = false,
  val students: List<AdminAtRiskStudentItem> = emptyList(),
  val hasMoreStudents: Boolean = false,
  val selectedClassIdFilter: String? = null,
  val selectedClassNameFilter: String = "",
  val minLevel: AdminRiskLevel = AdminRiskLevel.WATCH,
  val searchQuery: String = "",
  val selectedStudent: AdminAtRiskStudentItem? = null,
  val timeline: List<AdminStudentTimelineItem> = emptyList(),
  val isLoadingOverview: Boolean = false,
  val isLoadingStudents: Boolean = false,
  val isLoadingMore: Boolean = false,
  val isTimelineLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null
)

@OptIn(FlowPreview::class)
class AdminRiskViewModel(
  private val repo: AdminRiskRepository = AdminRiskRepository()
) : ViewModel() {
  private val _state = MutableStateFlow(AdminRiskState())
  val state: StateFlow<AdminRiskState> = _state.asStateFlow()

  private val searchFlow = MutableStateFlow("")
  private var session: AdminSession? = null
  private val pageSize = 30

  init {
    viewModelScope.launch {
      searchFlow.debounce(400L).distinctUntilChanged().collectLatest {
        loadStudents(reset = true)
      }
    }
  }

  fun setSession(newSession: AdminSession?) {
    val changed = session?.userId != newSession?.userId
    session = newSession
    if (newSession != null && changed) refreshAll() else if (newSession == null) _state.value = AdminRiskState()
  }

  fun refreshAll() {
    loadOverview(reset = true)
    loadStudents(reset = true)
  }

  fun setMinLevel(level: AdminRiskLevel) {
    _state.update { it.copy(minLevel = level) }
    loadStudents(reset = true)
  }

  fun onSearchChanged(q: String) {
    _state.update { it.copy(searchQuery = q) }
    searchFlow.value = q
  }

  fun selectClassFilter(summary: AdminClassRiskSummary?) {
    _state.update { it.copy(selectedClassIdFilter = summary?.classId, selectedClassNameFilter = summary?.className.orEmpty()) }
    loadStudents(reset = true)
  }

  fun openClassRiskDirectly(classId: String, className: String) {
    _state.update { it.copy(selectedClassIdFilter = classId, selectedClassNameFilter = className) }
    loadStudents(reset = true)
  }

  fun loadOverview(reset: Boolean = true) {
    val token = session?.accessToken ?: return
    val offset = if (reset) 0 else _state.value.classSummaries.size
    viewModelScope.launch {
      _state.update { it.copy(isLoadingOverview = reset, isLoadingMore = !reset) }
      repo.getRiskOverview(pageSize, offset, token).fold(
        onSuccess = { list ->
          val merged = if (reset) list else (_state.value.classSummaries + list).distinctBy { it.classId }
          _state.update { it.copy(classSummaries = merged, hasMoreClasses = list.size >= pageSize, isLoadingOverview = false, isLoadingMore = false) }
        },
        onFailure = { e -> _state.update { it.copy(isLoadingOverview = false, isLoadingMore = false, errorMessage = e.message) } }
      )
    }
  }

  fun loadStudents(reset: Boolean = true) {
    val token = session?.accessToken ?: return
    val s = _state.value
    val offset = if (reset) 0 else s.students.size
    viewModelScope.launch {
      _state.update { it.copy(isLoadingStudents = reset, isLoadingMore = !reset) }
      repo.getAtRiskStudents(s.selectedClassIdFilter, s.minLevel.key, s.searchQuery, pageSize, offset, token).fold(
        onSuccess = { list ->
          val merged = if (reset) list else (s.students + list).distinctBy { "${it.classId}:${it.studentId}" }
          _state.update { it.copy(students = merged, hasMoreStudents = list.size >= pageSize, isLoadingStudents = false, isLoadingMore = false) }
        },
        onFailure = { e -> _state.update { it.copy(isLoadingStudents = false, isLoadingMore = false, errorMessage = e.message) } }
      )
    }
  }

  fun openStudentDetail(student: AdminAtRiskStudentItem) {
    val token = session?.accessToken ?: return
    _state.update { it.copy(selectedStudent = student, timeline = emptyList(), isTimelineLoading = true) }
    viewModelScope.launch {
      repo.getStudentTimeline(student.classId, student.studentId, token).fold(
        onSuccess = { tl -> _state.update { it.copy(timeline = tl, isTimelineLoading = false) } },
        onFailure = { e -> _state.update { it.copy(isTimelineLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun closeStudentDetail() = _state.update { it.copy(selectedStudent = null, timeline = emptyList()) }

  fun setExcuse(sessionId: String, excused: Boolean, reason: String?) {
    val token = session?.accessToken ?: return
    val student = _state.value.selectedStudent ?: return
    viewModelScope.launch {
      _state.update { it.copy(isActionLoading = true, errorMessage = null) }
      repo.setAbsenceExcused(sessionId, student.studentId, excused, reason, token).fold(
        onSuccess = {
          _state.update { it.copy(isActionLoading = false, successMessage = if (excused) "Absence excused" else "Excuse removed") }
          openStudentDetail(student)
          loadStudents(reset = true)
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun runWarningsNow() {
    val token = session?.accessToken ?: return
    viewModelScope.launch {
      _state.update { it.copy(isActionLoading = true, errorMessage = null) }
      repo.runAttendanceAlerts(token).fold(
        onSuccess = { res ->
          _state.update {
            it.copy(
              isActionLoading = false,
              successMessage = "Processed ${res.processedClasses} classes • Sent ${res.warningsSent} student warnings and ${res.alertsSent} alerts."
            )
          }
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun clearError() = _state.update { it.copy(errorMessage = null) }
  fun clearSuccess() = _state.update { it.copy(successMessage = null) }
}
