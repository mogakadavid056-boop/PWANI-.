package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SchoolItem
import com.example.ui.theme.*

@Composable
fun FindClassesSearchBar(
  query: String,
  onQueryChange: (String) -> Unit
) {
  var text by remember(query) { mutableStateOf(query) }
  OutlinedTextField(
    value = text,
    onValueChange = {
      text = it
      onQueryChange(it)
    },
    placeholder = { Text("Search class, programme, school...", color = TextSecondary, fontSize = 13.sp) },
    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) },
    trailingIcon = {
      if (text.isNotEmpty()) {
        IconButton(onClick = { text = ""; onQueryChange("") }) {
          Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
      }
    },
    singleLine = true,
    shape = RoundedCornerShape(12.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedContainerColor = DarkSurface,
      unfocusedContainerColor = DarkSurface,
      focusedBorderColor = GoldPrimary,
      unfocusedBorderColor = DarkBorder,
      focusedTextColor = TextPrimary,
      unfocusedTextColor = TextPrimary
    ),
    modifier = Modifier.fillMaxWidth().testTag("find_classes_search_input")
  )
}

@Composable
fun FindClassesSchoolDropdown(
  selectedSchool: SchoolItem?,
  schools: List<SchoolItem>,
  onSchoolSelected: (SchoolItem?) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxWidth()) {
    OutlinedCard(
      onClick = { expanded = true },
      colors = CardDefaults.outlinedCardColors(containerColor = DarkSurface),
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.fillMaxWidth().testTag("find_classes_school_dropdown")
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Default.AccountBalance, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = selectedSchool?.name ?: "All Schools",
            fontSize = 13.sp,
            color = if (selectedSchool != null) TextPrimary else TextSecondary,
            maxLines = 1
          )
        }
        Text("▼", fontSize = 10.sp, color = TextSecondary)
      }
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.background(DarkCard).border(1.dp, DarkBorder)
    ) {
      DropdownMenuItem(
        text = { Text("All Schools", color = GoldLight, fontWeight = FontWeight.SemiBold) },
        onClick = {
          onSchoolSelected(null)
          expanded = false
        }
      )
      schools.forEach { school ->
        DropdownMenuItem(
          text = { Text(school.name, color = TextPrimary) },
          onClick = {
            onSchoolSelected(school)
            expanded = false
          }
        )
      }
    }
  }
}

@Composable
fun FindClassesYearChips(
  selectedYear: Int?,
  onYearSelected: (Int?) -> Unit
) {
  val options = listOf(null to "All", 1 to "Year 1", 2 to "Year 2", 3 to "Year 3", 4 to "Year 4", 5 to "Year 5", 6 to "Year 6")
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    options.forEach { (yearNum, label) ->
      val isSelected = selectedYear == yearNum
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(if (isSelected) GoldPrimary else DarkSurface)
          .border(1.dp, if (isSelected) GoldPrimary else DarkBorder, RoundedCornerShape(8.dp))
          .clickable { onYearSelected(yearNum) }
          .padding(horizontal = 12.dp, vertical = 6.dp)
          .testTag("year_chip_${yearNum ?: 0}")
      ) {
        Text(
          text = label,
          fontSize = 12.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          color = if (isSelected) DarkTextOnGold else TextPrimary
        )
      }
    }
  }
}

@Composable
fun FindClassesWithoutPrefectSwitch(
  onlyWithoutPrefect: Boolean,
  onToggle: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(DarkSurface)
      .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
      .padding(horizontal = 14.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text("Without prefect only", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    Switch(
      checked = onlyWithoutPrefect,
      onCheckedChange = onToggle,
      colors = SwitchDefaults.colors(
        checkedThumbColor = GoldPrimary,
        checkedTrackColor = GoldDark,
        uncheckedThumbColor = TextSecondary,
        uncheckedTrackColor = DarkSurfaceVariant
      ),
      modifier = Modifier.testTag("without_prefect_switch")
    )
  }
}
