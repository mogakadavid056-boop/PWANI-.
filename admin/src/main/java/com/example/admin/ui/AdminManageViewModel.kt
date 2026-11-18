package com.example.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.admin.data.AcademicYearItem
import com.example.admin.data.AdminClassItem
import com.example.admin.data.AdminOversightRepository
import com.example.admin.data.AdminRegisterDetail
import com.example.admin.data.AdminRegisterSummary
import com.example.admin.data.AdminSession
import com.example.admin.data.AdminUniversityRepository
import com.example.admin.data.ProgrammeItem
import com.example.admin.data.SchoolItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UniversitySection(val label: String) {
  SCHOOLS("Schools"), PROGRAMMES("Programmes"), YEARS("Academic Years")
}

data class AdminManageState(
  val universitySection: UniversitySection = UniversitySection.SCHOOLS,
  val schools: List<SchoolItem> = emptyList(),
  val programmes: List<ProgrammeItem> = emptyList(),
  val academicYears: List<AcademicYearItem> = emptyList(),
  val isUniLoading: Boolean = false,
  val classes: List<AdminClassItem> = emptyList(),
  val classSearch: String = "",
  val isClassesLoading: Boolean = false,
  val selectedClass: AdminClassItem? = null,
  val registers: List<AdminRegisterSummary> = emptyList(),
  val isRegistersLoading: Boolean = false,
  val selectedRegister: AdminRegisterDetail? = null,
  val isRegisterDetailLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null
)

class AdminManageViewModel(
  private val uniRepo: AdminUniversityRepository = AdminUniversityRepository(),
  private val oversightRepo: AdminOversightRepository = AdminOversightRepository()
) : ViewModel() {

  private val _state = MutableStateFlow(AdminManageState())
  val state: StateFlow<AdminManageState> = _state.asStateFlow()

  private var session: AdminSession? = null

  fun setSession(newSession: AdminSession?) {
    val changed = session?.userId != newSession?.userId
    session = newSession
    if (newSession != null && changed) {
      refreshUniversityData()
      refreshClasses()
    } else if (newSession == null) {
      _state.value = AdminManageState()
    }
  }

  fun selectUniversitySection(section: UniversitySection) {
    _state.update { it.copy(universitySection = section) }
  }

  fun refreshUniversityData() {
    val token = session?.accessToken ?: return
    viewModelScope.launch {
      _state.update { it.copy(isUniLoading = true) }
      val s = uniRepo.fetchSchools(token).getOrElse { emptyList() }
      val p = uniRepo.fetchProgrammes(token).getOrElse { emptyList() }
      val y = uniRepo.fetchAcademicYears(token).getOrElse { emptyList() }
      _state.update { it.copy(schools = s, programmes = p, academicYears = y, isUniLoading = false) }
    }
  }

  fun saveSchool(id: String?, name: String) {
    val token = session?.accessToken ?: return
    if (name.isBlank()) return
    _state.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      uniRepo.saveSchool(id, name, token).fold(
        onSuccess = {
          _state.update { it.copy(isActionLoading = false, successMessage = if (id == null) "School added" else "School updated") }
          refreshUniversityData()
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun deleteSchool(school: SchoolItem) {
    val token = session?.accessToken ?: return
    _state.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      uniRepo.deleteSchool(school.id, token).fold(
        onSuccess = {
          _state.update { it.copy(isActionLoading = false, successMessage = "Deleted ${school.name}") }
          refreshUniversityData()
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun saveProgramme(id: String?, name: String, schoolId: String, durationYears: Int) {
    val token = session?.accessToken ?: return
    if (name.isBlank() || schoolId.isBlank()) return
    _state.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      uniRepo.saveProgramme(id, name, schoolId, durationYears, token).fold(
        onSuccess = {
          _state.update { it.copy(isActionLoading = false, successMessage = if (id == null) "Programme added" else "Programme updated") }
          refreshUniversityData()
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun deleteProgramme(prog: ProgrammeItem) {
    val token = session?.accessToken ?: return
    _state.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      uniRepo.deleteProgramme(prog.id, token).fold(
        onSuccess = {
          _state.update { it.copy(isActionLoading = false, successMessage = "Deleted ${prog.name}") }
          refreshUniversityData()
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun createAcademicYear(name: String) {
    val token = session?.accessToken ?: return
    if (name.isBlank()) return
    _state.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      uniRepo.createAcademicYear(name, token).fold(
        onSuccess = {
          _state.update { it.copy(isActionLoading = false, successMessage = "Academic year added") }
          refreshUniversityData()
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun setCurrentAcademicYear(year: AcademicYearItem) {
    val token = session?.accessToken ?: return
    _state.update { it.copy(isActionLoading = true, errorMessage = null) }
    viewModelScope.launch {
      uniRepo.setCurrentAcademicYear(year.id, token).fold(
        onSuccess = {
          _state.update { it.copy(isActionLoading = false, successMessage = "${year.name} set as current year") }
          refreshUniversityData()
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun onClassSearchChanged(query: String) {
    _state.update { it.copy(classSearch = query) }
    refreshClasses()
  }

  fun refreshClasses() {
    val token = session?.accessToken ?: return
    viewModelScope.launch {
      _state.update { it.copy(isClassesLoading = true) }
      oversightRepo.listClasses(_state.value.classSearch, token).fold(
        onSuccess = { list -> _state.update { it.copy(classes = list, isClassesLoading = false) } },
        onFailure = { e -> _state.update { it.copy(isClassesLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun openClassRegisters(classItem: AdminClassItem) {
    val token = session?.accessToken ?: return
    _state.update { it.copy(selectedClass = classItem, registers = emptyList(), isRegistersLoading = true) }
    viewModelScope.launch {
      oversightRepo.listRegisters(classItem.id, token).fold(
        onSuccess = { list -> _state.update { it.copy(registers = list, isRegistersLoading = false) } },
        onFailure = { e -> _state.update { it.copy(isRegistersLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun closeClassRegisters() {
    _state.update { it.copy(selectedClass = null, registers = emptyList(), selectedRegister = null) }
  }

  fun openRegisterDetail(sessionId: String) {
    val token = session?.accessToken ?: return
    _state.update { it.copy(isRegisterDetailLoading = true) }
    viewModelScope.launch {
      oversightRepo.getRegisterDetail(sessionId, token).fold(
        onSuccess = { d -> _state.update { it.copy(selectedRegister = d, isRegisterDetailLoading = false) } },
        onFailure = { e -> _state.update { it.copy(isRegisterDetailLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun closeRegisterDetail() = _state.update { it.copy(selectedRegister = null) }

  fun closeAttendanceSession(sessionId: String) {
    val token = session?.accessToken ?: return
    _state.update { it.copy(isActionLoading = true) }
    viewModelScope.launch {
      oversightRepo.closeSession(sessionId, token).fold(
        onSuccess = {
          _state.update { it.copy(isActionLoading = false, successMessage = "Session closed") }
          openRegisterDetail(sessionId)
          _state.value.selectedClass?.let { openClassRegisters(it) }
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun voidAttendanceSession(sessionId: String, reason: String) {
    val token = session?.accessToken ?: return
    if (reason.isBlank()) return
    _state.update { it.copy(isActionLoading = true) }
    viewModelScope.launch {
      oversightRepo.voidSession(sessionId, reason, token).fold(
        onSuccess = {
          _state.update { it.copy(isActionLoading = false, successMessage = "Register voided") }
          openRegisterDetail(sessionId)
          _state.value.selectedClass?.let { openClassRegisters(it) }
        },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun clearError() = _state.update { it.copy(errorMessage = null) }
  fun clearSuccess() = _state.update { it.copy(successMessage = null) }
}
