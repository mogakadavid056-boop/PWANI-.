package com.example.ui

import com.example.data.MaterialItem

data class MaterialsUiState(
  val classId: String = "",
  val materials: List<MaterialItem> = emptyList(),
  val searchQuery: String = "",
  val selectedTypeFilter: String = "All",
  val isLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val uploadProgress: Float? = null,
  val downloadingMaterialId: String? = null,
  val downloadProgress: Float? = null,
  val errorMessage: String? = null,
  val successMessage: String? = null,
  val materialPendingDelete: MaterialItem? = null,
  val materialPendingEdit: MaterialItem? = null,
  val showAddDialog: Boolean = false,
  val downloadedRevision: Long = 0L
) {
  val filteredMaterials: List<MaterialItem>
    get() = materials.filter { item ->
      val matchesSearch = searchQuery.isBlank() ||
        item.title.contains(searchQuery.trim(), ignoreCase = true) ||
        item.fileName.contains(searchQuery.trim(), ignoreCase = true) ||
        item.description.contains(searchQuery.trim(), ignoreCase = true)
      val matchesType = when (selectedTypeFilter) {
        "All" -> true
        "Notes" -> item.materialType.equals("notes", ignoreCase = true)
        "Slides" -> item.materialType.equals("slides", ignoreCase = true)
        "Past Paper" -> item.materialType.equals("past_paper", ignoreCase = true)
        "Video" -> item.materialType.equals("video", ignoreCase = true)
        "Link" -> item.materialType.equals("link", ignoreCase = true)
        "Files" -> item.isFile
        else -> true
      }
      matchesSearch && matchesType
    }
}
