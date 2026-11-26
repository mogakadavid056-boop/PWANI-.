package com.example.admin.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.sp
import com.example.admin.data.AcademicYearItem
import com.example.admin.data.ProgrammeItem
import com.example.admin.data.SchoolItem
import com.example.admin.ui.UniversitySection
import com.example.admin.ui.components.AdminEmptyState
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminMetallicGoldText
import com.example.admin.ui.components.AdminNotificationBellButton
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.AdminBorder
import com.example.admin.ui.theme.AdminCaptionStyle
import com.example.admin.ui.theme.AdminCardTitleStyle
import com.example.admin.ui.theme.AdminNavLabelStyle
import com.example.admin.ui.theme.AdminScreenTitleStyle
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.ChampagneGold
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.Lavender
import com.example.admin.ui.theme.MontserratFamily

@Composable
fun AdminUniversityScreen(
  section: UniversitySection,
  schools: List<SchoolItem>,
  programmes: List<ProgrammeItem>,
  academicYears: List<AcademicYearItem>,
  unreadNotifications: Int,
  isLoading: Boolean,
  isActionLoading: Boolean,
  onSelectSection: (UniversitySection) -> Unit,
  onRefresh: () -> Unit,
  onSaveSchool: (String?, String) -> Unit,
  onDeleteSchool: (SchoolItem) -> Unit,
  onSaveProgramme: (String?, String, String, Int) -> Unit,
  onDeleteProgramme: (ProgrammeItem) -> Unit,
  onCreateYear: (String) -> Unit,
  onSetCurrentYear: (AcademicYearItem) -> Unit,
  onOpenNotifications: () -> Unit
) {
  var showSchoolDialog by remember { mutableStateOf(false) }
  var editSchool by remember { mutableStateOf<SchoolItem?>(null) }
  var showProgrammeDialog by remember { mutableStateOf(false) }
  var editProgramme by remember { mutableStateOf<ProgrammeItem?>(null) }
  var showYearDialog by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp).testTag("admin_university_screen")) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        AdminMetallicGoldText(text = "University Structure", style = AdminScreenTitleStyle)
        Text("Shared directly with the student & lecturer app", style = AdminCaptionStyle)
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp)) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ChampagneGold)
        }
        AdminNotificationBellButton(unreadCount = unreadNotifications, onClick = onOpenNotifications)
        Spacer(modifier = Modifier.width(4.dp))
        Button(
          onClick = {
            when (section) {
              UniversitySection.SCHOOLS -> { editSchool = null; showSchoolDialog = true }
              UniversitySection.PROGRAMMES -> { editProgramme = null; showProgrammeDialog = true }
              UniversitySection.YEARS -> showYearDialog = true
            }
          },
          modifier = Modifier.defaultMinSize(minHeight = 48.dp),
          shape = RoundedCornerShape(14.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add", fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      UniversitySection.entries.forEach { sec ->
        FilterChip(
          selected = section == sec,
          onClick = { onSelectSection(sec) },
          modifier = Modifier.defaultMinSize(minHeight = 48.dp),
          label = { Text(sec.label, style = AdminNavLabelStyle) },
          border = FilterChipDefaults.filterChipBorder(enabled = true, selected = section == sec, borderColor = AdminBorder, selectedBorderColor = ChampagneGold),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ChampagneGold,
            selectedLabelColor = DarkTextOnGold,
            containerColor = AdminSurface,
            labelColor = Lavender
          )
        )
      }
    }

    if (isLoading) {
      Spacer(modifier = Modifier.height(6.dp))
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = GoldPrimary)
    }

    Spacer(modifier = Modifier.height(10.dp))

    when (section) {
      UniversitySection.SCHOOLS -> {
        if (schools.isEmpty() && !isLoading) {
          AdminEmptyState(Icons.Default.AccountBalance, "No schools yet", "Tap Add to create a faculty or school.")
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(schools, key = { it.id }) { s ->
              AdminGlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(s.name, style = AdminCardTitleStyle)
                    Text("${programmes.count { it.schoolId == s.id }} programmes", style = AdminCaptionStyle)
                  }
                  Row {
                    IconButton(onClick = { editSchool = s; showSchoolDialog = true }, modifier = Modifier.size(48.dp)) {
                      Icon(Icons.Default.Edit, contentDescription = "Edit school", tint = ChampagneGold)
                    }
                    IconButton(onClick = { onDeleteSchool(s) }, modifier = Modifier.size(48.dp)) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete school", tint = CrimsonError)
                    }
                  }
                }
              }
            }
          }
        }
      }
      UniversitySection.PROGRAMMES -> {
        if (programmes.isEmpty() && !isLoading) {
          AdminEmptyState(Icons.Default.MenuBook, "No programmes yet", "Add a school first, then add programmes.")
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(programmes, key = { it.id }) { p ->
              AdminGlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(p.name, style = AdminCardTitleStyle)
                    Text(
                      text = "${p.schoolName.ifBlank { "School" }} • ${p.durationYears ?: 4} yrs",
                      style = AdminCaptionStyle
                    )
                  }
                  Row {
                    IconButton(onClick = { editProgramme = p; showProgrammeDialog = true }, modifier = Modifier.size(48.dp)) {
                      Icon(Icons.Default.Edit, contentDescription = "Edit programme", tint = ChampagneGold)
                    }
                    IconButton(onClick = { onDeleteProgramme(p) }, modifier = Modifier.size(48.dp)) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete programme", tint = CrimsonError)
                    }
                  }
                }
              }
            }
          }
        }
      }
      UniversitySection.YEARS -> {
        if (academicYears.isEmpty() && !isLoading) {
          AdminEmptyState(Icons.Default.CalendarMonth, "No academic years", "Tap Add to create an academic year.")
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(academicYears, key = { it.id }) { y ->
              AdminGlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column {
                    Text(y.name, style = AdminCardTitleStyle)
                    if (y.isCurrent) {
                      Spacer(modifier = Modifier.height(4.dp))
                      AdminStatusPill("Current Active Year", EmeraldSuccess)
                    }
                  }
                  if (!y.isCurrent) {
                    OutlinedButton(
                      onClick = { onSetCurrentYear(y) },
                      border = BorderStroke(1.dp, GoldPrimary),
                      modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                      shape = RoundedCornerShape(14.dp)
                    ) {
                      Text("Set Current", fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, color = ChampagneGold, fontSize = 13.sp)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showSchoolDialog) {
    SchoolFormDialog(
      existing = editSchool,
      isActionLoading = isActionLoading,
      onSave = { id, name -> onSaveSchool(id, name); showSchoolDialog = false },
      onDismiss = { showSchoolDialog = false }
    )
  }

  if (showProgrammeDialog) {
    ProgrammeFormDialog(
      existing = editProgramme,
      schools = schools,
      isActionLoading = isActionLoading,
      onSave = { id, name, schoolId, dur -> onSaveProgramme(id, name, schoolId, dur); showProgrammeDialog = false },
      onDismiss = { showProgrammeDialog = false }
    )
  }

  if (showYearDialog) {
    AcademicYearFormDialog(
      isActionLoading = isActionLoading,
      onCreate = { name -> onCreateYear(name); showYearDialog = false },
      onDismiss = { showYearDialog = false }
    )
  }
}
