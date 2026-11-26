package com.example.admin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.admin.data.AdminSearchClassItem
import com.example.admin.data.SchoolItem
import com.example.admin.ui.AdminClassesSearchState
import com.example.admin.ui.components.AdminEmptyState
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminMetallicGoldText
import com.example.admin.ui.components.AdminNotificationBellButton
import com.example.admin.ui.components.AdminOptionPickerDialog
import com.example.admin.ui.components.AdminSearchField
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.AdminBorder
import com.example.admin.ui.theme.AdminCaptionStyle
import com.example.admin.ui.theme.AdminCardTitleStyle
import com.example.admin.ui.theme.AdminNavLabelStyle
import com.example.admin.ui.theme.AdminScreenTitleStyle
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.AmberWarning
import com.example.admin.ui.theme.ChampagneGold
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.Lavender
import com.example.admin.ui.theme.MontserratFamily
import com.example.admin.ui.theme.SilverWhite
import com.example.admin.util.AdminJsonHelper

@Composable
fun AdminClassesSearchScreen(
  state: AdminClassesSearchState,
  schools: List<SchoolItem>,
  unreadNotifications: Int,
  onSearchChanged: (String) -> Unit,
  onSchoolFilterChanged: (String?) -> Unit,
  onYearFilterChanged: (Int?) -> Unit,
  onToggleNoPrefect: (Boolean) -> Unit,
  onRefresh: () -> Unit,
  onLoadMore: () -> Unit,
  onOpenClass: (AdminSearchClassItem) -> Unit,
  onOpenNotifications: () -> Unit
) {
  var showSchoolPicker by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp).testTag("admin_classes_screen")) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        AdminMetallicGoldText(text = "Classes", style = AdminScreenTitleStyle)
        Text("Tap any class to open Class Monitor", style = AdminCaptionStyle)
      }
      Row {
        IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp)) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh classes", tint = ChampagneGold)
        }
        AdminNotificationBellButton(unreadCount = unreadNotifications, onClick = onOpenNotifications)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    AdminSearchField(
      value = state.searchText,
      onValueChange = onSearchChanged,
      placeholder = "Search class name, code, or programme...",
      tag = "admin_classes_search"
    )

    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = state.schoolFilter != null,
        onClick = { showSchoolPicker = true },
        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) },
        label = { Text(state.schoolFilter ?: "All Schools", style = AdminNavLabelStyle) },
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = state.schoolFilter != null, borderColor = AdminBorder, selectedBorderColor = ChampagneGold),
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = ChampagneGold, selectedLabelColor = DarkTextOnGold,
          containerColor = AdminSurface, labelColor = Lavender
        )
      )
      FilterChip(
        selected = state.onlyWithoutPrefect,
        onClick = { onToggleNoPrefect(!state.onlyWithoutPrefect) },
        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        label = { Text("Without Prefect", style = AdminNavLabelStyle) },
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = state.onlyWithoutPrefect, borderColor = AdminBorder, selectedBorderColor = AmberWarning),
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = AmberWarning, selectedLabelColor = DarkTextOnGold,
          containerColor = AdminSurface, labelColor = Lavender
        )
      )
      listOf(null to "All Years", 1 to "Yr 1", 2 to "Yr 2", 3 to "Yr 3", 4 to "Yr 4", 5 to "Yr 5").forEach { (yr, label) ->
        FilterChip(
          selected = state.yearFilter == yr,
          onClick = { onYearFilterChanged(yr) },
          modifier = Modifier.defaultMinSize(minHeight = 48.dp),
          label = { Text(label, style = AdminNavLabelStyle) },
          border = FilterChipDefaults.filterChipBorder(enabled = true, selected = state.yearFilter == yr, borderColor = AdminBorder, selectedBorderColor = ChampagneGold),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ChampagneGold, selectedLabelColor = DarkTextOnGold,
            containerColor = AdminSurface, labelColor = Lavender
          )
        )
      }
    }

    if (state.isLoading) {
      Spacer(modifier = Modifier.height(6.dp))
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = GoldPrimary)
    }

    Spacer(modifier = Modifier.height(10.dp))
    if (state.items.isEmpty() && !state.isLoading) {
      AdminEmptyState(Icons.Default.Class, "No classes found", "Try clearing filters or pull to refresh.")
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        items(state.items, key = { it.id }) { cls ->
          AdminGlassCard(onClick = { onOpenClass(cls) }) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
              Column(modifier = Modifier.weight(1f)) {
                Text(cls.className, style = AdminCardTitleStyle)
                Spacer(modifier = Modifier.height(3.dp))
                val meta = listOf(cls.schoolName, cls.programmeName).filter { it.isNotBlank() }.joinToString(" • ")
                if (meta.isNotBlank()) Text(meta, style = AdminCaptionStyle)
                Text("Code: ${AdminJsonHelper.orDash(cls.classCode)} • ${cls.studentCount} students", style = AdminCaptionStyle.copy(color = SilverWhite.copy(alpha = 0.85f)))
              }
              Spacer(modifier = Modifier.width(8.dp))
              AdminStatusPill(
                text = if (cls.hasPrefect) "Prefect OK" else "No Prefect",
                color = if (cls.hasPrefect) EmeraldSuccess else AmberWarning
              )
            }
          }
        }
        if (state.hasMore) {
          item {
            OutlinedButton(
              onClick = onLoadMore,
              enabled = !state.isLoadingMore,
              border = BorderStroke(1.dp, GoldPrimary),
              modifier = Modifier.fillMaxWidth().height(48.dp),
              shape = RoundedCornerShape(14.dp)
            ) {
              Text(if (state.isLoadingMore) "Loading..." else "Load more classes", fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, color = ChampagneGold)
            }
          }
        }
      }
    }
  }

  if (showSchoolPicker) {
    val options = listOf("") + schools.map { it.name }.distinct().sorted()
    AdminOptionPickerDialog(
      title = "Filter by School",
      options = options,
      selectedPredicate = { (it.isBlank() && state.schoolFilter == null) || it.equals(state.schoolFilter, true) },
      labelProvider = { if (it.isBlank()) "All Schools" else it },
      onSelect = { chosen ->
        onSchoolFilterChanged(chosen.ifBlank { null })
        showSchoolPicker = false
      },
      onDismiss = { showSchoolPicker = false }
    )
  }
}
