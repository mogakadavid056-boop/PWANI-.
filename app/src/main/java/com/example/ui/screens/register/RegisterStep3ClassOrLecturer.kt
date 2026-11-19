package com.example.ui.screens.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProgrammeItem
import com.example.data.SchoolItem
import com.example.ui.components.AppDropdownField
import com.example.ui.components.DarkTextField
import com.example.ui.components.PasswordTextField
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RegisterStep3ClassOrLecturer(
  state: RegisterWizardState,
  schools: List<SchoolItem>,
  programmes: List<ProgrammeItem>,
  onSchoolSelected: (SchoolItem) -> Unit,
  onUpdate: ((RegisterWizardState) -> RegisterWizardState) -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    if (state.isLecturer) {
      LecturerAccessStep(
        state = state,
        schools = schools,
        onSchoolSelected = onSchoolSelected,
        onUpdate = onUpdate
      )
    } else {
      StudentClassStep(
        state = state,
        schools = schools,
        programmes = programmes,
        onSchoolSelected = onSchoolSelected,
        onUpdate = onUpdate
      )
    }
  }
}

@Composable
private fun StudentClassStep(
  state: RegisterWizardState,
  schools: List<SchoolItem>,
  programmes: List<ProgrammeItem>,
  onSchoolSelected: (SchoolItem) -> Unit,
  onUpdate: ((RegisterWizardState) -> RegisterWizardState) -> Unit
) {
  Text("Your class", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GoldLight)
  Spacer(modifier = Modifier.height(6.dp))
  Text("Choose how you want to join your class.", fontSize = 14.sp, color = TextSecondary)
  Spacer(modifier = Modifier.height(14.dp))

  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    ModeChoiceButton(
      title = "Join with a class code",
      selected = !state.knowsDetails,
      onClick = { onUpdate { it.copy(knowsDetails = false) } },
      modifier = Modifier.weight(1f).testTag("register_mode_code_btn")
    )
    ModeChoiceButton(
      title = "Join with my details",
      selected = state.knowsDetails,
      onClick = { onUpdate { it.copy(knowsDetails = true) } },
      modifier = Modifier.weight(1f).testTag("register_mode_details_btn")
    )
  }

  Spacer(modifier = Modifier.height(16.dp))

  if (state.knowsDetails) {
    AppDropdownField(
      label = "School *",
      selectedValueText = state.selectedSchool?.name.orEmpty(),
      items = schools,
      itemText = { it.name },
      onItemSelected = { s ->
        onUpdate { it.copy(selectedSchool = s, selectedProgramme = null, selectedYearOfStudy = null) }
        onSchoolSelected(s)
      },
      leadingIcon = Icons.Default.AccountBalance,
      placeholder = "Select your school",
      testTag = "register_school_dropdown"
    )
    Spacer(modifier = Modifier.height(12.dp))

    AppDropdownField(
      label = "Programme *",
      selectedValueText = state.selectedProgramme?.name.orEmpty(),
      items = programmes,
      itemText = { it.name },
      onItemSelected = { prog ->
        val maxYears = prog.durationYears ?: 4
        onUpdate {
          val keptYear = it.selectedYearOfStudy?.takeIf { y -> y <= maxYears }
          it.copy(selectedProgramme = prog, selectedYearOfStudy = keptYear)
        }
      },
      leadingIcon = Icons.Default.School,
      placeholder = if (state.selectedSchool == null) "Select school first" else "Select your programme",
      testTag = "register_programme_dropdown"
    )
    Spacer(modifier = Modifier.height(12.dp))

    val maxYears = state.selectedProgramme?.durationYears ?: 4
    val years = (1..maxYears).toList()
    AppDropdownField(
      label = "Year of Study *",
      selectedValueText = state.selectedYearOfStudy?.let { "Year $it" }.orEmpty(),
      items = years,
      itemText = { "Year $it" },
      onItemSelected = { y -> onUpdate { it.copy(selectedYearOfStudy = y) } },
      leadingIcon = Icons.Default.CalendarMonth,
      placeholder = "Select year (1 to $maxYears)",
      testTag = "register_year_dropdown"
    )
  } else {
    DarkTextField(
      value = state.classCode,
      onValueChange = { v -> onUpdate { it.copy(classCode = v.uppercase()) } },
      label = "Class Join Code *",
      leadingIcon = Icons.Default.Key,
      keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
      testTag = "register_class_code_input"
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = "Ask your class prefect, lecturer, or classmate for the 6-character class code.",
      fontSize = 12.sp,
      color = TextSecondary
    )
  }
}

@Composable
private fun ModeChoiceButton(
  title: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(if (selected) GoldPrimary.copy(alpha = 0.16f) else DarkCard)
      .border(if (selected) 1.5.dp else 1.dp, if (selected) GoldPrimary else DarkBorder, RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = title,
      fontSize = 13.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      color = if (selected) GoldLight else TextSecondary
    )
  }
}

@Composable
private fun LecturerAccessStep(
  state: RegisterWizardState,
  schools: List<SchoolItem>,
  onSchoolSelected: (SchoolItem) -> Unit,
  onUpdate: ((RegisterWizardState) -> RegisterWizardState) -> Unit
) {
  Text("Lecturer access", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GoldLight)
  Spacer(modifier = Modifier.height(6.dp))
  Text("Verify your lecturer access using the university passcode.", fontSize = 14.sp, color = TextSecondary)
  Spacer(modifier = Modifier.height(18.dp))

  AppDropdownField(
    label = "School (optional)",
    selectedValueText = state.selectedSchool?.name.orEmpty(),
    items = schools,
    itemText = { it.name },
    onItemSelected = { s ->
      onUpdate { it.copy(selectedSchool = s) }
      onSchoolSelected(s)
    },
    leadingIcon = Icons.Default.AccountBalance,
    placeholder = "Select your school (optional)",
    testTag = "register_lecturer_school_dropdown"
  )

  Spacer(modifier = Modifier.height(14.dp))

  PasswordTextField(
    value = state.lecturerPasscode,
    onValueChange = { v -> onUpdate { it.copy(lecturerPasscode = v) } },
    label = "Lecturer passcode *",
    leadingIcon = Icons.Default.VpnKey,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
    testTag = "register_lecturer_passcode_input"
  )

  Spacer(modifier = Modifier.height(14.dp))

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(DarkCard)
      .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
      .padding(14.dp)
  ) {
    Row(verticalAlignment = Alignment.Top) {
      Icon(Icons.Default.Info, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = "Ask the administration for the passcode. An administrator verifies your account.",
        fontSize = 13.sp,
        color = TextSecondary,
        lineHeight = 19.sp
      )
    }
  }
}
