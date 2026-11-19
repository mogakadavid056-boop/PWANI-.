package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.util.UUID

class MaterialsViewModel : ViewModel() {
  private val materialsService = MaterialsService()
  private val unitsService = UnitsService()
  private val _uiState = MutableStateFlow(MaterialsUiState())
  val uiState: StateFlow<MaterialsUiState> = _uiState.asStateFlow()

  fun loadMaterials(classId: String, session: UserSession?) {
    _uiState.update { it.copy(classId = classId, isLoading = true, isUnitsLoading = true, errorMessage = null) }
    if (session == null || session.accessToken.isBlank()) {
      _uiState.update { it.copy(isLoading = false, isUnitsLoading = false) }
      return
    }
    viewModelScope.launch {
      val materialsDeferred = async { materialsService.fetchMaterials(classId, session.accessToken) }
      val unitsDeferred = async { unitsService.listClassUnits(classId, session.accessToken) }
      val matRes = materialsDeferred.await()
      val unitsRes = unitsDeferred.await()
      _uiState.update { state ->
        state.copy(
          isLoading = false,
          isUnitsLoading = false,
          materials = matRes.getOrDefault(state.materials),
          units = unitsRes.getOrDefault(state.units),
          errorMessage = matRes.exceptionOrNull()?.message ?: unitsRes.exceptionOrNull()?.message
        )
      }
    }
  }

  fun refreshMaterials(session: UserSession?) {
    val cid = _uiState.value.classId
    if (cid.isNotBlank()) loadMaterials(cid, session)
  }

  fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }
  fun setSelectedTypeFilter(filter: String) = _uiState.update { it.copy(selectedTypeFilter = filter) }
  fun setSelectedUnitFilter(courseId: String?) = _uiState.update { it.copy(selectedUnitFilterId = courseId) }
  fun setShowAddDialog(show: Boolean) = _uiState.update { it.copy(showAddDialog = show, uploadProgress = null) }
  fun setShowRegisterUnitDialog(show: Boolean) = _uiState.update { it.copy(showRegisterUnitDialog = show, unitDialogError = null) }
  fun setMaterialPendingEdit(item: MaterialItem?) = _uiState.update { it.copy(materialPendingEdit = item) }
  fun setMaterialPendingDelete(item: MaterialItem?) = _uiState.update { it.copy(materialPendingDelete = item) }

  fun registerUnit(code: String, name: String, description: String, session: UserSession?) {
    val cid = _uiState.value.classId
    if (session == null || cid.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, unitDialogError = null) }
      unitsService.requestUnit(cid, code, name, description, session.accessToken).fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              isActionLoading = false,
              showRegisterUnitDialog = false,
              unitDialogError = null,
              successMessage = "Sent to the admin for approval. You will be notified."
            )
          }
          loadMaterials(cid, session)
        },
        onFailure = { err ->
          _uiState.update { it.copy(isActionLoading = false, unitDialogError = err.message ?: "Failed to request unit") }
        }
      )
    }
  }

  fun addLinkMaterial(
    courseId: String,
    title: String,
    description: String,
    materialType: String,
    externalUrl: String,
    session: UserSession?,
    onSuccess: () -> Unit
  ) {
    val cid = _uiState.value.classId
    if (session == null || cid.isBlank()) return
    if (courseId.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Please select an approved unit first") }
      return
    }
    if (title.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Title cannot be empty") }
      return
    }
    val cleanUrl = externalUrl.trim()
    if (!cleanUrl.startsWith("http://", ignoreCase = true) && !cleanUrl.startsWith("https://", ignoreCase = true)) {
      _uiState.update { it.copy(errorMessage = "URL must start with http:// or https://") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      materialsService.addLinkMaterial(cid, courseId, title, description, materialType, cleanUrl, session.accessToken).fold(
        onSuccess = {
          _uiState.update { it.copy(isActionLoading = false, showAddDialog = false, successMessage = "Material link added") }
          loadMaterials(cid, session)
          onSuccess()
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun uploadFileMaterial(
    courseId: String,
    title: String,
    description: String,
    fileName: String,
    fileSize: Long,
    mimeType: String,
    inputStreamProvider: () -> InputStream,
    session: UserSession?,
    onSuccess: () -> Unit
  ) {
    val cid = _uiState.value.classId
    if (session == null || cid.isBlank()) return
    if (courseId.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Please select an approved unit first") }
      return
    }
    if (title.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Title cannot be empty") }
      return
    }
    val validationError = MaterialFileValidator.validate(fileName, mimeType, fileSize)
    if (validationError != null) {
      _uiState.update { it.copy(errorMessage = validationError) }
      return
    }
    val safeFileName = MaterialFileValidator.sanitizeFileName(fileName)
    val storagePath = "$cid/${UUID.randomUUID()}_$safeFileName"
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, uploadProgress = 0f, errorMessage = null) }
      materialsService.uploadFileAndCreateMaterial(
        cid, courseId, title, description, storagePath, safeFileName, fileSize, mimeType, inputStreamProvider, session.accessToken
      ) { p -> _uiState.update { it.copy(uploadProgress = p) } }.fold(
        onSuccess = {
          _uiState.update { it.copy(isActionLoading = false, uploadProgress = null, showAddDialog = false, successMessage = "File uploaded successfully") }
          loadMaterials(cid, session)
          onSuccess()
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, uploadProgress = null, errorMessage = err.message) } }
      )
    }
  }

  fun updateMaterial(materialId: String, title: String, description: String, session: UserSession?, onSuccess: () -> Unit) {
    val cid = _uiState.value.classId
    if (session == null || cid.isBlank()) return
    if (title.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Title cannot be empty") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      materialsService.updateMaterial(materialId, title, description, session.accessToken).fold(
        onSuccess = {
          _uiState.update { it.copy(isActionLoading = false, materialPendingEdit = null, successMessage = "Material updated") }
          loadMaterials(cid, session)
          onSuccess()
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun confirmDeleteMaterial(session: UserSession?) {
    val pending = _uiState.value.materialPendingDelete ?: return
    val cid = _uiState.value.classId
    if (session == null || cid.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, materialPendingDelete = null, errorMessage = null) }
      materialsService.deleteMaterial(pending, session.accessToken).fold(
        onSuccess = {
          _uiState.update { it.copy(isActionLoading = false, successMessage = "Material deleted") }
          loadMaterials(cid, session)
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun downloadAndOpenFile(material: MaterialItem, context: Context, session: UserSession?, onOpen: (File, String) -> Unit) {
    if (session == null || session.accessToken.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Please sign in to view materials") }
      return
    }
    OfflineMaterialsManager.getDownloadedFile(context, session.userId, material)?.let { onOpen(it, material.mimeType); return }
    val safeName = MaterialFileValidator.sanitizeFileName(material.fileName.ifBlank { "material_${material.id}" })
    val dest = File(File(context.cacheDir, "materials").apply { mkdirs() }, "${material.id}_$safeName")
    if (dest.exists() && dest.length() > 0) { onOpen(dest, material.mimeType); return }
    if (!NetworkMonitor.isOnline.value) {
      _uiState.update { it.copy(errorMessage = "You are offline and this file has not been saved yet.") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(downloadingMaterialId = material.id, downloadProgress = 0f, errorMessage = null) }
      materialsService.downloadFileMaterial(material.storagePath, dest, session.accessToken) { p ->
        _uiState.update { it.copy(downloadProgress = p) }
      }.fold(
        onSuccess = { f -> _uiState.update { it.copy(downloadingMaterialId = null, downloadProgress = null) }; onOpen(f, material.mimeType) },
        onFailure = { err -> _uiState.update { it.copy(downloadingMaterialId = null, downloadProgress = null, errorMessage = err.message) } }
      )
    }
  }

  fun downloadMaterialForOffline(context: Context, material: MaterialItem, session: UserSession?) {
    if (session == null || session.accessToken.isBlank() || session.userId.isBlank() || !material.isFile) return
    if (!NetworkMonitor.isOnline.value) { _uiState.update { it.copy(errorMessage = "You need internet to do this.") }; return }
    if (!OfflineMaterialsManager.canDownload(context, session.userId, material.fileSize)) {
      _uiState.update { it.copy(errorMessage = "Storage limit reached (300 MB). Please remove some downloads.") }; return
    }
    val dest = OfflineMaterialsManager.getFileForMaterial(context, session.userId, material)
    viewModelScope.launch {
      _uiState.update { it.copy(downloadingMaterialId = material.id, downloadProgress = 0f, errorMessage = null) }
      materialsService.downloadFileMaterial(material.storagePath, dest, session.accessToken) { p ->
        _uiState.update { it.copy(downloadProgress = p) }
      }.fold(
        onSuccess = { _uiState.update { it.copy(downloadingMaterialId = null, downloadProgress = null, downloadedRevision = it.downloadedRevision + 1, successMessage = "Saved on phone") } },
        onFailure = { err -> _uiState.update { it.copy(downloadingMaterialId = null, downloadProgress = null, errorMessage = err.message) } }
      )
    }
  }

  fun removeDownloadedMaterial(context: Context, material: MaterialItem, session: UserSession?) {
    OfflineMaterialsManager.removeDownload(context, session?.userId.orEmpty(), material)
    _uiState.update { it.copy(downloadedRevision = it.downloadedRevision + 1, successMessage = "Download removed") }
  }

  fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
  fun clearSuccess() { _uiState.update { it.copy(successMessage = null) } }
}
