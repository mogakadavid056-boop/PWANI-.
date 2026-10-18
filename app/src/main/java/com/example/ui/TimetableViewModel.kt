package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ScheduleItem
import com.example.data.TimetableService
import com.example.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class TimetableUiState(
  val classId: String = "",
  val schedules: List<ScheduleItem> = emptyList(),
  val selectedDay: Int = 0, // 0 = All days, 1..7 = Monday..Sunday
  val todayDayOfWeek: Int = 1,
  val isLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val schedulePendingDelete: ScheduleItem? = null,
  val schedulePendingEdit: ScheduleItem? = null,
  val showAddDialog: Boolean = false
)

class TimetableViewModel : ViewModel() {

  private val timetableService = TimetableService()
  private val _uiState = MutableStateFlow(
    TimetableUiState(todayDayOfWeek = computeTodayDayOfWeek(), selectedDay = 0)
  )
  val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

  companion object {
    fun computeTodayDayOfWeek(): Int {
      val cal = Calendar.getInstance()
      return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
      }
    }
  }

  fun loadSchedules(classId: String, session: UserSession?) {
    val today = computeTodayDayOfWeek()
    _uiState.update {
      it.copy(
        classId = classId,
        todayDayOfWeek = today,
        isLoading = true,
        errorMessage = null
      )
    }
    if (session == null || session.accessToken.isBlank()) {
      _uiState.update { it.copy(isLoading = false) }
      return
    }

    viewModelScope.launch {
      val result = timetableService.fetchSchedules(classId, session.accessToken)
      result.fold(
        onSuccess = { list ->
          _uiState.update { it.copy(isLoading = false, schedules = list) }
        },
        onFailure = { error ->
          _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
        }
      )
    }
  }

  fun refreshSchedules(session: UserSession?) {
    val cid = _uiState.value.classId
    if (cid.isNotBlank()) loadSchedules(cid, session)
  }

  fun setSelectedDay(day: Int) {
    _uiState.update { it.copy(selectedDay = day) }
  }

  fun setShowAddDialog(show: Boolean) {
    _uiState.update { it.copy(showAddDialog = show) }
  }

  fun setSchedulePendingEdit(item: ScheduleItem?) {
    _uiState.update { it.copy(schedulePendingEdit = item) }
  }

  fun setSchedulePendingDelete(item: ScheduleItem?) {
    _uiState.update { it.copy(schedulePendingDelete = item) }
  }

  fun addSchedule(
    title: String,
    dayOfWeek: Int,
    startTime: String,
    endTime: String,
    room: String,
    lecturerName: String,
    session: UserSession?,
    onSuccess: () -> Unit
  ) {
    val cid = _uiState.value.classId
    if (session == null || cid.isBlank()) return

    if (title.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Title cannot be empty") }
      return
    }
    if (endTime <= startTime) {
      _uiState.update { it.copy(errorMessage = "End time must be after start time") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      val result = timetableService.addSchedule(
        classId = cid,
        title = title,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        room = room,
        lecturerName = lecturerName,
        accessToken = session.accessToken
      )

      result.fold(
        onSuccess = {
          _uiState.update { it.copy(isActionLoading = false, showAddDialog = false, successMessage = "Class added to timetable") }
          loadSchedules(cid, session)
          onSuccess()
        },
        onFailure = { error ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = error.message) }
        }
      )
    }
  }

  fun updateSchedule(
    scheduleId: String,
    title: String,
    dayOfWeek: Int,
    startTime: String,
    endTime: String,
    room: String,
    lecturerName: String,
    session: UserSession?,
    onSuccess: () -> Unit
  ) {
    val cid = _uiState.value.classId
    if (session == null || cid.isBlank()) return

    if (title.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Title cannot be empty") }
      return
    }
    if (endTime <= startTime) {
      _uiState.update { it.copy(errorMessage = "End time must be after start time") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      val result = timetableService.updateSchedule(
        scheduleId = scheduleId,
        title = title,
        dayOfWeek = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        room = room,
        lecturerName = lecturerName,
        accessToken = session.accessToken
      )

      result.fold(
        onSuccess = {
          _uiState.update { it.copy(isActionLoading = false, schedulePendingEdit = null, successMessage = "Timetable class updated") }
          loadSchedules(cid, session)
          onSuccess()
        },
        onFailure = { error ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = error.message) }
        }
      )
    }
  }

  fun confirmDeleteSchedule(session: UserSession?) {
    val pending = _uiState.value.schedulePendingDelete ?: return
    val cid = _uiState.value.classId
    if (session == null || cid.isBlank()) return

    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, schedulePendingDelete = null, errorMessage = null) }
      val result = timetableService.deleteSchedule(pending.id, session.accessToken)
      result.fold(
        onSuccess = {
          _uiState.update { it.copy(isActionLoading = false, successMessage = "Class deleted from timetable") }
          loadSchedules(cid, session)
        },
        onFailure = { error ->
          _uiState.update { it.copy(isActionLoading = false, errorMessage = error.message) }
        }
      )
    }
  }

  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun clearSuccess() {
    _uiState.update { it.copy(successMessage = null) }
  }
}
