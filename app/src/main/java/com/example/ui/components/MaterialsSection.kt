package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.MaterialUnitGroup
import com.example.ui.theme.*

@Composable
fun MaterialsSection(
  units: List<CourseUnit> = emptyList(),
  groupedMaterials: List<MaterialUnitGroup> = emptyList(),
  materialsCount: Int = 0,
  searchQuery: String,
  selectedUnitFilterId: String? = null,
  isStaff: Boolean,
  isLecturer: Boolean = false,
  isLoading: Boolean,
  isUnitsLoading: Boolean = false,
  successMessage: String? = null,
  downloadingMaterialId: String? = null,
  downloadProgress: Float? = null,
  onSearchChange: (String) -> Unit,
  onSelectUnitFilter: (String?) -> Unit = {},
  onRegisterUnitClick: () -> Unit = {},
  onRefresh: () -> Unit,
  onAddMaterialClick: () -> Unit,
  onOpenFileMaterial: (MaterialItem) -> Unit = {},
  onDownloadMaterial: (MaterialItem) -> Unit = {},
  onRemoveDownloadMaterial: (MaterialItem) -> Unit = {},
  onEditMaterial: (MaterialItem) -> Unit,
  onDeleteMaterial: (MaterialItem) -> Unit
) {
  val context = LocalContext.current
  val userId = OfflineCache.currentUserId
  val isOnline by NetworkMonitor.isOnline.collectAsStateWithLifecycle()

  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    if (!successMessage.isNullOrBlank()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(SuccessGreen.copy(alpha = 0.14f))
          .border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
          .padding(12.dp)
      ) {
        Text(successMessage, color = SuccessGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
      }
    }

    UnitsSectionCard(
      units = units,
      isStaff = isStaff,
      isLecturer = isLecturer,
      isLoading = isUnitsLoading,
      onRegisterUnitClick = onRegisterUnitClick
    )

    HorizontalDivider(color = DarkBorder.copy(alpha = 0.6f))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Learning Materials", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.testTag("materials_section_title"))
        Spacer(modifier = Modifier.width(6.dp))
        Text("($materialsCount)", fontSize = 14.sp, color = TextSecondary)
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp).testTag("refresh_materials_btn")) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GoldLight, modifier = Modifier.size(20.dp))
        }
        if (isStaff) {
          Spacer(modifier = Modifier.width(6.dp))
          Button(
            onClick = { if (!isOnline) OfflineToastHelper.show(context) else onAddMaterialClick() },
            modifier = Modifier.height(36.dp).testTag("add_material_btn").then(if (!isOnline) Modifier.alpha(0.5f) else Modifier),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = DarkTextOnGold)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Material", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold)
          }
        }
      }
    }

    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchChange,
      modifier = Modifier.fillMaxWidth().testTag("materials_search_input"),
      placeholder = { Text("Search materials by title or file name...", color = TextSecondary, fontSize = 13.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
      trailingIcon = {
        if (searchQuery.isNotBlank()) {
          IconButton(onClick = { onSearchChange("") }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary, modifier = Modifier.size(16.dp))
          }
        }
      },
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = GoldPrimary, unfocusedBorderColor = DarkBorder,
        focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface,
        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
      ),
      shape = RoundedCornerShape(12.dp)
    )

    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      UnitFilterChip("All", selectedUnitFilterId == null) { onSelectUnitFilter(null) }
      units.forEach { unit ->
        UnitFilterChip(
          label = unit.code.ifBlank { unit.name },
          selected = selectedUnitFilterId == unit.courseId,
          onClick = { onSelectUnitFilter(unit.courseId) }
        )
      }
    }

    if (isLoading) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LoadingSkeleton(height = 90)
        LoadingSkeleton(height = 90)
      }
    } else if (groupedMaterials.isEmpty()) {
      EmptyState(
        icon = Icons.Default.FolderOpen,
        title = "No Materials Found",
        message = if (searchQuery.isNotBlank() || selectedUnitFilterId != null) "No materials match your current filters."
        else if (isStaff) "Tap Add Material above to share notes, slides, files, or links."
        else "Nothing here yet. Waiting for your class prefect or lecturer to update this.",
        actionLabel = if (isStaff && searchQuery.isBlank() && selectedUnitFilterId == null) "Add Material" else null,
        onActionClick = if (isStaff && searchQuery.isBlank() && selectedUnitFilterId == null) onAddMaterialClick else null,
        modifier = Modifier.testTag("materials_empty_state")
      )
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        groupedMaterials.forEach { group ->
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.MenuBook, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(group.headerTitle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldLight, modifier = Modifier.weight(1f))
              Text("${group.items.size}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GoldPrimary)
            }
            group.items.forEach { item ->
              val isDownloaded = remember(item.id, userId, downloadingMaterialId) {
                OfflineMaterialsManager.isDownloaded(context, userId, item)
              }
              MaterialCard(
                material = item,
                isStaff = isStaff,
                isDownloaded = isDownloaded,
                isDownloading = item.id == downloadingMaterialId,
                downloadProgress = if (item.id == downloadingMaterialId) downloadProgress else null,
                onOpenFile = { onOpenFileMaterial(item) },
                onDownload = { onDownloadMaterial(item) },
                onRemoveDownload = { onRemoveDownloadMaterial(item) },
                onEdit = { onEditMaterial(item) },
                onDelete = { onDeleteMaterial(item) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun UnitFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (selected) GoldPrimary else DarkSurface)
      .border(1.dp, if (selected) GoldPrimary else DarkBorder, RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 7.dp)
      .testTag("unit_filter_chip_${label.lowercase().replace(" ", "_")}"),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      fontSize = 12.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
      color = if (selected) DarkTextOnGold else TextSecondary
    )
  }
}
