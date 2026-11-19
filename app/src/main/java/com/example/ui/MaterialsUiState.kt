package com.example.ui

import com.example.data.CourseUnit
import com.example.data.MaterialItem

data class MaterialUnitGroup(
  val courseId: String,
  val headerTitle: String,
  val sortKey: String,
  val items: List<MaterialItem>
)

data class MaterialsUiState(
  val classId: String = "",
  val units: List<CourseUnit> = emptyList(),
  val materials: List<MaterialItem> = emptyList(),
  val searchQuery: String = "",
  val selectedTypeFilter: String = "All",
  val selectedUnitFilterId: String? = null,
  val isLoading: Boolean = false,
  val isUnitsLoading: Boolean = false,
  val isActionLoading: Boolean = false,
  val uploadProgress: Float? = null,
  val downloadingMaterialId: String? = null,
  val downloadProgress: Float? = null,
  val errorMessage: String? = null,
  val unitDialogError: String? = null,
  val successMessage: String? = null,
  val materialPendingDelete: MaterialItem? = null,
  val materialPendingEdit: MaterialItem? = null,
  val showAddDialog: Boolean = false,
  val showRegisterUnitDialog: Boolean = false,
  val downloadedRevision: Long = 0L
) {
  val approvedUnits: List<CourseUnit>
    get() = units.filter { it.isApproved }.sortedBy { it.code }

  val filteredMaterials: List<MaterialItem>
    get() = materials.filter { item ->
      val matchesType = selectedTypeFilter == "All" || item.typeLabel.equals(selectedTypeFilter, ignoreCase = true)
      val matchesSearch = searchQuery.isBlank() ||
        item.title.contains(searchQuery.trim(), ignoreCase = true) ||
        item.fileName.contains(searchQuery.trim(), ignoreCase = true) ||
        item.description.contains(searchQuery.trim(), ignoreCase = true)
      val matchesUnit = selectedUnitFilterId == null || item.courseId == selectedUnitFilterId
      matchesType && matchesSearch && matchesUnit
    }

  val groupedMaterials: List<MaterialUnitGroup>
    get() {
      val unitMap = units.associateBy { it.courseId }
      val byCourse = filteredMaterials.groupBy { it.courseId }
      return byCourse.map { (courseId, list) ->
        val unit = unitMap[courseId]
        val header = unit?.displayLabel?.ifBlank { "General Materials" } ?: "General Materials"
        val sortKey = unit?.code?.ifBlank { "ZZZZ" } ?: "ZZZZ"
        MaterialUnitGroup(
          courseId = courseId,
          headerTitle = header,
          sortKey = sortKey,
          items = list.sortedByDescending { it.createdAt }
        )
      }.sortedBy { it.sortKey }
    }
}
