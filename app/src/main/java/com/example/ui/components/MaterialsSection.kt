package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MaterialItem
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MaterialsSection(
  materials: List<MaterialItem>,
  searchQuery: String,
  selectedTypeFilter: String,
  isStaff: Boolean,
  isLoading: Boolean,
  downloadingMaterialId: String? = null,
  downloadProgress: Float? = null,
  onSearchChange: (String) -> Unit,
  onSelectTypeFilter: (String) -> Unit,
  onRefresh: () -> Unit,
  onAddMaterialClick: () -> Unit,
  onOpenFileMaterial: (MaterialItem) -> Unit = {},
  onDownloadMaterial: (MaterialItem) -> Unit = {},
  onRemoveDownloadMaterial: (MaterialItem) -> Unit = {},
  onEditMaterial: (MaterialItem) -> Unit,
  onDeleteMaterial: (MaterialItem) -> Unit
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val userId = com.example.data.OfflineCache.currentUserId
  val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
  val filterTypes = listOf("All", "Files", "Notes", "Slides", "Past Paper", "Video", "Link")

  Column(modifier = Modifier.fillMaxWidth()) {
    // Header Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "Learning Materials",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          modifier = Modifier.testTag("materials_section_title")
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "(${materials.size})", fontSize = 14.sp, color = TextSecondary)
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onRefresh,
          modifier = Modifier.size(36.dp).testTag("refresh_materials_btn")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GoldLight, modifier = Modifier.size(20.dp))
        }

        if (isStaff) {
          Spacer(modifier = Modifier.width(6.dp))
          Button(
            onClick = {
              if (!isOnline) {
                com.example.ui.components.OfflineToastHelper.show(context)
              } else {
                onAddMaterialClick()
              }
            },
            modifier = Modifier.height(36.dp).testTag("add_material_btn")
              .then(if (!isOnline) Modifier.alpha(0.5f) else Modifier),
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

    Spacer(modifier = Modifier.height(12.dp))

    // Search bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchChange,
      modifier = Modifier.fillMaxWidth().testTag("materials_search_input"),
      placeholder = { Text("Search materials by title or file name...", color = TextSecondary, fontSize = 13.sp) },
      leadingIcon = {
        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
      },
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

    Spacer(modifier = Modifier.height(10.dp))

    // Filter chips
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      filterTypes.forEach { type ->
        val isSelected = selectedTypeFilter.equals(type, ignoreCase = true)
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) GoldPrimary else DarkSurface)
            .border(1.dp, if (isSelected) GoldPrimary else DarkBorder, RoundedCornerShape(10.dp))
            .clickable { onSelectTypeFilter(type) }
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("filter_chip_${type.lowercase().replace(" ", "_")}"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = type,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) DarkTextOnGold else TextSecondary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (isLoading) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LoadingSkeleton(height = 90)
        LoadingSkeleton(height = 90)
      }
    } else if (materials.isEmpty()) {
      EmptyState(
        icon = Icons.Default.FolderOpen,
        title = "No Materials Found",
        message = if (searchQuery.isNotBlank() || selectedTypeFilter != "All") "No materials match your current filters."
        else if (isStaff) "Tap Add Material above to share notes, slides, files, or links."
        else "Nothing here yet. Waiting for your class prefect to update this.",
        actionLabel = if (isStaff && searchQuery.isBlank() && selectedTypeFilter == "All") "Add Material" else null,
        onActionClick = if (isStaff && searchQuery.isBlank() && selectedTypeFilter == "All") onAddMaterialClick else null,
        modifier = Modifier.testTag("materials_empty_state")
      )
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        materials.forEach { item ->
          val isDownloaded = remember(item.id, userId, downloadingMaterialId) {
            com.example.data.OfflineMaterialsManager.isDownloaded(context, userId, item)
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
