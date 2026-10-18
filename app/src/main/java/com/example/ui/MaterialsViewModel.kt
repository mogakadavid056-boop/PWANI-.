package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MaterialItem
import com.example.data.MaterialsService
import com.example.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MaterialsUiState(
  val classId: String = "",
  val materials: List<MaterialItem> = emptyList(),
  val searchQuery: String = "",
  val selectedTypeFilter: String = "All",
  val isLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val materialPendingDelete: MaterialItem? = null,
  val materialPendingEdit: MaterialItem? = null,
  val showAddDialog: Boolean = false
) {
  val filteredMaterials: List<MaterialItem>
    get() = materials.filter { item ->
      val matchesSearch = searchQuery.isBlank() || item.title.contains(searchQuery.trim(), ignoreCase = true)
      val matchesType = when (selectedTypeFilter) {
        "All" -> true
        "Notes" -> item.materialType.equals("notes", ignoreCase = true)
        "Slides" -> item.materialType.equals("slides", ignoreCase = true)
        "Past Paper" -> item.materialType.equals("past_paper", ignoreCase = true)
        "Video" -> item.materialType.equals("video", ignoreCase = true)
        "Link" -> item.materialType.equals("link", ignoreCase = true)
        else -> true
      }
      matchesSearch && matchesType
    }
}

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

  fun setSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  fun setSelectedTypeFilter(filter: String) {
    _uiState.update { it.copy(selectedTypeFilter = filter) }
  }

  fun setShowAddDialog(show: Boolean) {
    _uiState.update { it.copy(showAddDialog = show) }
  }

  fun setMaterialPendingEdit(item: MaterialItem?) {
    _uiState.update { it.copy(materialPendingEdit = item) }
  }

  fun setMaterialPendingDelete(item: MaterialItem?) {
    _uiState.update { it.copy(materialPendingDelete = item) }
  }

  fun addMaterial(
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
      val res = materialsService.addMaterial(
        classId = cid, title = title, description = description,
        materialType = materialType, externalUrl = cleanUrl, accessToken = session.accessToken
      )
      res.fold(
        onSuccess = {
          _uiState.update { it.copy(isActionLoading = false, showAddDialog = false, successMessage = "Material added") }
          loadMaterials(cid, session)
          onSuccess()
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun updateMaterial(
    materialId: String,
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
      val res = materialsService.updateMaterial(
        materialId = materialId, title = title, description = description,
        materialType = materialType, externalUrl = cleanUrl, accessToken = session.accessToken
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
      val res = materialsService.deleteMaterial(pending.id, session.accessToken)
      res.fold(
        onSuccess = {
          _uiState.update { it.copy(isActionLoading = false, successMessage = "Material deleted") }
          loadMaterials(cid, session)
        },
        onFailure = { err -> _uiState.update { it.copy(isActionLoading = false, errorMessage = err.message) } }
      )
    }
  }

  fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
  fun clearSuccess() { _uiState.update { it.copy(successMessage = null) } }
}
