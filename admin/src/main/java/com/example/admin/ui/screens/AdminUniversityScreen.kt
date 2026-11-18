package com.example.admin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.TextMuted
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary

@Composable
fun AdminUniversityScreen(
  section: UniversitySection,
  schools: List<SchoolItem>,
  programmes: List<ProgrammeItem>,
  academicYears: List<AcademicYearItem>,
  isLoading: Boolean,
  isActionLoading: Boolean,
  onSelectSection: (UniversitySection) -> Unit,
  onRefresh: () -> Unit,
  onSaveSchool: (String?, String) -> Unit,
  onDeleteSchool: (SchoolItem) -> Unit,
  onSaveProgramme: (String?, String, String, Int) -> Unit,
  onDeleteProgramme: (ProgrammeItem) -> Unit,
  onCreateYear: (String) -> Unit,
  onSetCurrentYear: (AcademicYearItem) -> Unit
) {
  var showSchoolDialog by remember { mutableStateOf(false) }
  var editSchool by remember { mutableStateOf<SchoolItem?>(null) }
  var showProgrammeDialog by remember { mutableStateOf(false) }
  var editProgramme by remember { mutableStateOf<ProgrammeItem?>(null) }
  var showYearDialog by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp).testTag("admin_university_screen")) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column {
        Text("University Structure", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Shared directly with the student & lecturer app", fontSize = 12.sp, color = TextSecondary)
      }
      Row {
        IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GoldPrimary) }
        Button(
          onClick = {
            when (section) {
              UniversitySection.SCHOOLS -> { editSchool = null; showSchoolDialog = true }
              UniversitySection.PROGRAMMES -> { editProgramme = null; showProgrammeDialog = true }
              UniversitySection.YEARS -> showYearDialog = true
            }
          },
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
        ) {
          Icon(Icons.Default.Add, contentDescription = null)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add", fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      UniversitySection.entries.forEach { sec ->
        FilterChip(
          selected = section == sec,
          onClick = { onSelectSection(sec) },
          label = { Text(sec.label, fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = GoldPrimary,
            selectedLabelColor = DarkTextOnGold,
            containerColor = AdminSurface,
            labelColor = TextSecondary
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
                    Text(s.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("${programmes.count { it.schoolId == s.id }} programmes", fontSize = 12.sp, color = TextMuted)
                  }
                  Row {
                    IconButton(onClick = { editSchool = s; showSchoolDialog = true }) {
                      Icon(Icons.Default.Edit, contentDescription = "Edit school", tint = GoldLight)
                    }
                    IconButton(onClick = { onDeleteSchool(s) }) {
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
                    Text(p.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(
                      text = "${p.schoolName.ifBlank { "School" }} • ${p.durationYears ?: 4} yrs",
                      fontSize = 12.sp,
                      color = TextSecondary
                    )
                  }
                  Row {
                    IconButton(onClick = { editProgramme = p; showProgrammeDialog = true }) {
                      Icon(Icons.Default.Edit, contentDescription = "Edit programme", tint = GoldLight)
                    }
                    IconButton(onClick = { onDeleteProgramme(p) }) {
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
                    Text(y.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    if (y.isCurrent) {
                      Spacer(modifier = Modifier.height(4.dp))
                      AdminStatusPill("Current Active Year", EmeraldSuccess)
                    }
                  }
                  if (!y.isCurrent) {
                    OutlinedButton(onClick = { onSetCurrentYear(y) }, shape = RoundedCornerShape(8.dp)) {
                      Text("Set Current", color = GoldLight, fontSize = 12.sp)
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
