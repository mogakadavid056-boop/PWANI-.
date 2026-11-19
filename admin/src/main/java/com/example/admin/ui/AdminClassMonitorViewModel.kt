package com.example.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.admin.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

enum class MonitorTab(val label: String) {
  NOTICES("Notices"), MEMBERS("Members"), TIMETABLE("Timetable"),
  UNITS("Units"), MATERIALS("Materials"), ATTENDANCE("Attendance"),
  POLLS("Polls"), GROUPS("Groups")
}

data class AdminClassMonitorState(
  val selectedClassId: String? = null,
  val activeTab: MonitorTab = MonitorTab.NOTICES,
  val overview: AdminClassOverview? = null,
  val notices: List<AdminNoticeItem> = emptyList(),
  val members: List<AdminClassMemberItem> = emptyList(),
  val pendingLecturers: List<AdminPendingLecturerItem> = emptyList(),
  val memberSearch: String = "",
  val schedules: List<AdminScheduleItem> = emptyList(),
  val units: List<AdminCourseUnit> = emptyList(),
  val materials: List<AdminMaterialItem> = emptyList(),
  val isLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val uploadProgress: Float? = null,
  val errorMessage: String? = null,
  val successMessage: String? = null
)

class AdminClassMonitorViewModel(
  private val searchRepo: AdminClassSearchRepository = AdminClassSearchRepository(),
  private val contentRepo: AdminMonitorContentRepository = AdminMonitorContentRepository(),
  private val unitsMatRepo: AdminUnitsMaterialsRepository = AdminUnitsMaterialsRepository()
) : ViewModel() {
  private val _state = MutableStateFlow(AdminClassMonitorState())
  val state: StateFlow<AdminClassMonitorState> = _state.asStateFlow()
  private var session: AdminSession? = null

  fun setSession(newSession: AdminSession?) {
    session = newSession
    if (newSession == null) _state.value = AdminClassMonitorState()
  }

  fun openClass(classId: String, initialTab: MonitorTab = MonitorTab.NOTICES) {
    _state.update { it.copy(selectedClassId = classId, activeTab = initialTab, overview = null, memberSearch = "", errorMessage = null, successMessage = null) }
    refreshCurrentClass()
  }

  fun closeClass() = _state.update { AdminClassMonitorState() }
  fun selectTab(tab: MonitorTab) = _state.update { it.copy(activeTab = tab, errorMessage = null) }
  fun onMemberSearchChanged(q: String) = _state.update { it.copy(memberSearch = q) }
  fun clearError() = _state.update { it.copy(errorMessage = null) }
  fun clearSuccess() = _state.update { it.copy(successMessage = null) }

  fun refreshCurrentClass() {
    val cid = _state.value.selectedClassId ?: return
    val token = session?.accessToken ?: return
    viewModelScope.launch {
      _state.update { it.copy(isLoading = true) }
      val ov = searchRepo.getClassOverview(cid, token)
      val nots = contentRepo.fetchNotices(cid, token)
      val mems = contentRepo.listClassMembers(cid, token)
      val pendLecs = contentRepo.listPendingLecturers(cid, token)
      val scheds = contentRepo.fetchSchedules(cid, token)
      val units = unitsMatRepo.listClassUnits(cid, token)
      val mats = unitsMatRepo.fetchMaterials(cid, token)
      _state.update { s ->
        s.copy(
          isLoading = false,
          overview = ov.getOrElse { s.overview },
          notices = nots.getOrElse { s.notices },
          members = mems.getOrElse { s.members },
          pendingLecturers = pendLecs.getOrElse { s.pendingLecturers },
          schedules = scheds.getOrElse { s.schedules },
          units = units.getOrElse { s.units },
          materials = mats.getOrElse { s.materials },
          errorMessage = ov.exceptionOrNull()?.message ?: mems.exceptionOrNull()?.message ?: units.exceptionOrNull()?.message
        )
      }
    }
  }

  private fun runAction(successMsg: String, block: suspend (String, String) -> Result<Unit>) {
    val cid = _state.value.selectedClassId ?: return
    val token = session?.accessToken ?: return
    viewModelScope.launch {
      _state.update { it.copy(isActionLoading = true, errorMessage = null) }
      block(cid, token).fold(
        onSuccess = { _state.update { it.copy(isActionLoading = false, successMessage = successMsg) }; refreshCurrentClass() },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun regenerateJoinCode() {
    val cid = _state.value.selectedClassId ?: return
    val token = session?.accessToken ?: return
    viewModelScope.launch {
      _state.update { it.copy(isActionLoading = true, errorMessage = null) }
      searchRepo.regenerateJoinCode(cid, token).fold(
        onSuccess = { code -> _state.update { it.copy(isActionLoading = false, overview = it.overview?.copy(joinCode = code), successMessage = "New code: $code") } },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }

  fun saveNotice(noticeId: String?, title: String, content: String, cat: String, urgent: Boolean, pinned: Boolean) =
    runAction(if (noticeId == null) "Notice posted" else "Notice updated") { cid, tok ->
      if (noticeId == null) contentRepo.createNotice(cid, title, content, cat, urgent, pinned, tok)
      else contentRepo.editNotice(noticeId, title, content, cat, urgent, pinned, tok)
    }

  fun deleteNotice(noticeId: String) = runAction("Notice deleted") { _, tok -> contentRepo.deleteNotice(noticeId, tok) }
  fun togglePrefect(userId: String, appoint: Boolean) = runAction(if (appoint) "Appointed prefect" else "Removed prefect") { cid, tok -> contentRepo.setClassRep(cid, userId, appoint, tok) }
  fun removeMember(userId: String) = runAction("Member removed from class") { cid, tok -> contentRepo.removeFromClass(cid, userId, tok) }
  fun reviewJoinRequest(userId: String, approve: Boolean) = runAction(if (approve) "Lecturer approved" else "Request rejected") { cid, tok -> contentRepo.reviewJoinRequest(cid, userId, approve, tok) }
  fun saveSchedule(id: String?, title: String, day: Int, start: String, end: String, room: String, lecturer: String) =
    runAction(if (id == null) "Class session added" else "Timetable updated") { cid, tok -> contentRepo.saveSchedule(id, cid, title, day, start, end, room, lecturer, tok) }
  fun deleteSchedule(id: String) = runAction("Timetable slot deleted") { _, tok -> contentRepo.deleteSchedule(id, tok) }
  fun registerUnit(code: String, name: String, desc: String, lecturerId: String?) = runAction("Unit registered (Approved)") { cid, tok -> unitsMatRepo.registerUnit(cid, code, name, desc, lecturerId, tok) }
  fun setUnitLecturer(courseId: String, lecturerId: String, assign: Boolean) = runAction(if (assign) "Lecturer assigned" else "Lecturer removed") { _, tok -> unitsMatRepo.setUnitLecturer(courseId, lecturerId, assign, tok) }
  fun addLinkMaterial(courseId: String, title: String, desc: String, type: String, url: String) = runAction("Material link added") { cid, tok -> unitsMatRepo.addLinkMaterial(cid, courseId, title, desc, type, url, tok) }
  fun updateMaterial(id: String, title: String, desc: String) = runAction("Material updated") { _, tok -> unitsMatRepo.updateMaterial(id, title, desc, tok) }
  fun deleteMaterial(item: AdminMaterialItem) = runAction("Material deleted") { _, tok -> unitsMatRepo.deleteMaterial(item, tok) }

  fun uploadFileMaterial(courseId: String, title: String, desc: String, name: String, size: Long, mime: String, input: () -> InputStream) {
    val cid = _state.value.selectedClassId ?: return
    val token = session?.accessToken ?: return
    AdminFileValidator.validate(name, mime, size)?.let { err -> _state.update { it.copy(errorMessage = err) }; return }
    viewModelScope.launch {
      _state.update { it.copy(isActionLoading = true, uploadProgress = 0f, errorMessage = null) }
      unitsMatRepo.uploadFileMaterial(cid, courseId, title, desc, name, size, mime, input, token) { p -> _state.update { it.copy(uploadProgress = p) } }.fold(
        onSuccess = { _state.update { it.copy(isActionLoading = false, uploadProgress = null, successMessage = "File uploaded") }; refreshCurrentClass() },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, uploadProgress = null, errorMessage = e.message) } }
      )
    }
  }

  fun downloadMaterial(item: AdminMaterialItem, cacheDir: File, onReady: (File, String) -> Unit) {
    val token = session?.accessToken ?: return
    val dest = File(File(cacheDir, "admin_mats").apply { mkdirs() }, "${item.id}_${AdminFileValidator.sanitize(item.fileName.ifBlank { "file" })}")
    viewModelScope.launch {
      _state.update { it.copy(isActionLoading = true) }
      unitsMatRepo.downloadSignedFile(item.storagePath, dest, token).fold(
        onSuccess = { f -> _state.update { it.copy(isActionLoading = false) }; onReady(f, item.mimeType) },
        onFailure = { e -> _state.update { it.copy(isActionLoading = false, errorMessage = e.message) } }
      )
    }
  }
}
