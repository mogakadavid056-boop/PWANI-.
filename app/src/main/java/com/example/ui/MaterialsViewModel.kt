package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MaterialFileValidator
import com.example.data.MaterialItem
import com.example.data.MaterialsService
import com.example.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.util.UUID

import android.content.Context
import com.example.data.NetworkMonitor
import com.example.data.OfflineMaterialsManager

class MaterialsViewModel : ViewModel() {
  private val materialsService = MaterialsService()
  private val _uiState = MutableStateFlow(MaterialsUiState())
  val uiState: StateFlow<MaterialsUiState> = _uiState.asStateFlow()

  fun loadMaterials(classId: String, session: UserSession?) {
    _uiState.update { it.copy(classId = classId, isLoading = true, errorMessage = null) }
    if (session == null || session.accessToken.isBlank()) {
      _uiState.update { it.copy(isLoading = false) }
      return
    }

    viewModelScope.launch {
      val res = materialsService.fetchMaterials(classId, session.accessToken)
      res.fold(
        onSuccess = { list -> _uiState.update { it.copy(isLoading = false, materials = list) } },
        onFailure = { err -> _uiState.update { it.copy(isLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun refreshMaterials(session: UserSession?) {
    val cid = _uiState.value.classId
    if (cid.isNotBlank()) loadMaterials(cid, session)
  }

  fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }
  fun setSelectedTypeFilter(filter: String) = _uiState.update { it.copy(selectedTypeFilter = filter) }
  fun setShowAddDialog(show: Boolean) = _uiState.update { it.copy(showAddDialog = show, uploadProgress = null) }
  fun setMaterialPendingEdit(item: MaterialItem?) = _uiState.update { it.copy(materialPendingEdit = item) }
  fun setMaterialPendingDelete(item: MaterialItem?) = _uiState.update { it.copy(materialPendingDelete = item) }

  fun addLinkMaterial(
    title: String,
    description: String,
    materialType: String,
    externalUrl: String,
    session: UserSession?,
    onSuccess: () -> Unit
  ) {
    val cid = _uiState.value.classId
    if (session == null || cid.isBlank()) return

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
      val res = materialsService.addLinkMaterial(
        classId = cid, title = title, description = description,
        materialType = materialType, externalUrl = cleanUrl, accessToken = session.accessToken
      )
      res.fold(
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
      val res = materialsService.uploadFileAndCreateMaterial(
        classId = cid,
        title = title,
        description = description,
        storagePath = storagePath,
        fileName = safeFileName,
        fileSize = fileSize,
        mimeType = mimeType,
        inputStreamProvider = inputStreamProvider,
        accessToken = session.accessToken,
        onProgress = { p -> _uiState.update { it.copy(uploadProgress = p) } }
      )
      res.fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              isActionLoading = false,
              uploadProgress = null,
              showAddDialog = false,
              successMessage = "File uploaded successfully"
            )
          }
          loadMaterials(cid, session)
          onSuccess()
        },
        onFailure = { err ->
          _uiState.update { it.copy(isActionLoading = false, uploadProgress = null, errorMessage = err.message) }
        }
      )
    }
  }

  fun updateMaterial(
    materialId: String,
    title: String,
    description: String,
    session: UserSession?,
    onSuccess: () -> Unit
  ) {
    val cid = _uiState.value.classId
    if (session == null || cid.isBlank()) return

    if (title.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Title cannot be empty") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
      val res = materialsService.updateMaterial(
        materialId = materialId, title = title, description = description, accessToken = session.accessToken
      )
      res.fold(
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
      val res = materialsService.deleteMaterial(pending, session.accessToken)
      res.fold(
        onSuccess = {
          _uiState.update { it.copy(isActionLoading = false, successMessage = "Material deleted") }
          loadMaterials(cid, session)
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun downloadAndOpenFile(
    material: MaterialItem,
    context: Context,
    session: UserSession?,
    onOpen: (File, String) -> Unit
  ) {
    if (session == null || session.accessToken.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Please sign in to view materials") }
      return
    }

    val downloaded = OfflineMaterialsManager.getDownloadedFile(context, session.userId, material)
    if (downloaded != null) {
      onOpen(downloaded, material.mimeType)
      return
    }

    val materialsDir = File(context.cacheDir, "materials").apply { mkdirs() }
    val safeName = MaterialFileValidator.sanitizeFileName(material.fileName.ifBlank { "material_${material.id}" })
    val destinationFile = File(materialsDir, "${material.id}_$safeName")
    if (destinationFile.exists() && destinationFile.length() > 0) {
      onOpen(destinationFile, material.mimeType)
      return
    }

    if (!NetworkMonitor.isOnline.value) {
      _uiState.update { it.copy(errorMessage = "You are offline and this file has not been saved yet.") }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(downloadingMaterialId = material.id, downloadProgress = 0f, errorMessage = null) }
      val res = materialsService.downloadFileMaterial(
        storagePath = material.storagePath,
        destinationFile = destinationFile,
        accessToken = session.accessToken,
        onProgress = { p -> _uiState.update { it.copy(downloadProgress = p) } }
      )
      res.fold(
        onSuccess = { file ->
          _uiState.update { it.copy(downloadingMaterialId = null, downloadProgress = null) }
          onOpen(file, material.mimeType)
        },
        onFailure = { err ->
          _uiState.update { it.copy(downloadingMaterialId = null, downloadProgress = null, errorMessage = err.message) }
        }
      )
    }
  }

  fun downloadMaterialForOffline(context: Context, material: MaterialItem, session: UserSession?) {
    if (session == null || session.accessToken.isBlank() || session.userId.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Please sign in to download materials") }
      return
    }
    if (!material.isFile) return
    if (!NetworkMonitor.isOnline.value) {
      _uiState.update { it.copy(errorMessage = "You need internet to do this.") }
      return
    }
    if (!OfflineMaterialsManager.canDownload(context, session.userId, material.fileSize)) {
      _uiState.update { it.copy(errorMessage = "Storage limit reached (300 MB). Please remove some downloads.") }
      return
    }

    val destinationFile = OfflineMaterialsManager.getFileForMaterial(context, session.userId, material)
    viewModelScope.launch {
      _uiState.update { it.copy(downloadingMaterialId = material.id, downloadProgress = 0f, errorMessage = null) }
      val res = materialsService.downloadFileMaterial(
        storagePath = material.storagePath,
        destinationFile = destinationFile,
        accessToken = session.accessToken,
        onProgress = { p -> _uiState.update { it.copy(downloadProgress = p) } }
      )
      res.fold(
        onSuccess = {
          _uiState.update {
            it.copy(downloadingMaterialId = null, downloadProgress = null, downloadedRevision = it.downloadedRevision + 1, successMessage = "Saved on phone")
          }
        },
        onFailure = { err ->
          _uiState.update { it.copy(downloadingMaterialId = null, downloadProgress = null, errorMessage = err.message) }
        }
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
