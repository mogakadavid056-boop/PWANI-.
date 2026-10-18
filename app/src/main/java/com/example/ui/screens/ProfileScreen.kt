package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProfileItem
import com.example.data.ProgrammeItem
import com.example.data.SchoolItem
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
  profile: ProfileItem?,
  schools: List<SchoolItem>,
  programmes: List<ProgrammeItem>,
  isStaff: Boolean = false,
  isStudentInClass: Boolean = false,
  isLoading: Boolean,
  isActionLoading: Boolean,
  errorMessage: String?,
  successMessage: String?,
  onDismissError: () -> Unit,
  onDismissSuccess: () -> Unit,
  onSchoolSelected: (SchoolItem) -> Unit,
  onSaveProfile: (fullName: String, school: SchoolItem?, programme: ProgrammeItem?, yearOfStudy: Int?) -> Unit,
  onBecomeLecturer: ((passcode: String) -> Unit)? = null,
  onLogout: () -> Unit,
  onBack: () -> Unit
) {
  var isEditing by remember { mutableStateOf(false) }
  var showLogoutConfirmDialog by remember { mutableStateOf(false) }
  var showBecomeLecturerDialog by remember { mutableStateOf(false) }
  var editedName by remember { mutableStateOf("") }
  var selectedSchool by remember { mutableStateOf<SchoolItem?>(null) }
  var selectedProgramme by remember { mutableStateOf<ProgrammeItem?>(null) }
  var selectedYearOfStudy by remember { mutableStateOf<Int?>(null) }

  LaunchedEffect(profile) {
    if (profile != null && !isEditing) {
      editedName = profile.fullName
      selectedYearOfStudy = profile.yearOfStudy
      selectedSchool = schools.find { it.id == profile.schoolId || it.name == profile.schoolName }
      if (selectedSchool != null) onSchoolSelected(selectedSchool!!)
      selectedProgramme = programmes.find { it.id == profile.programmeId || it.name == profile.programmeName }
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(DarkBackground).testTag("profile_screen")) {
    Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("Profile", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GoldLight)
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (!errorMessage.isNullOrBlank()) {
        ErrorBanner(message = errorMessage, onDismiss = onDismissError)
        Spacer(modifier = Modifier.height(14.dp))
      }
      if (!successMessage.isNullOrBlank()) {
        Box(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SuccessGreen.copy(alpha = 0.15f))
            .border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(12.dp)
        ) {
          Text(text = successMessage, color = SuccessGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(14.dp))
      }

      if (isLoading && profile == null) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = GoldPrimary, strokeWidth = 2.5.dp, modifier = Modifier.size(32.dp))
        }
      } else {
        if (!isEditing) {
          // View Mode
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileFieldCard(Icons.Default.Person, "Full Name", profile?.fullName.orEmpty().ifBlank { "Not set" })
            ProfileFieldCard(Icons.Default.Email, "Email", profile?.email.orEmpty().ifBlank { "Not set" })
            ProfileFieldCard(Icons.Default.Badge, "Admission Number", profile?.admissionNumber.orEmpty().ifBlank { "Not set" })
            ProfileFieldCard(
              Icons.Default.CalendarMonth, "Year of Study",
              profile?.yearOfStudy?.let { "Year $it" } ?: "Not set"
            )
            ProfileFieldCard(Icons.Default.AccountBalance, "School", profile?.schoolName?.ifBlank { null } ?: "Not set")
            ProfileFieldCard(Icons.Default.School, "Programme", profile?.programmeName?.ifBlank { null } ?: "Not set")
          }

          if (!isStaff && isStudentInClass) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
              modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(10.dp)).padding(12.dp)
            ) {
              Text(
                text = "Note: To change class, ask your class prefect or lecturer",
                fontSize = 12.sp, color = GoldLight, fontWeight = FontWeight.Medium
              )
            }
          }

          Spacer(modifier = Modifier.height(24.dp))

          // For a student who is NOT in a class: show "I am a lecturer" button
          if (!isStaff && !isStudentInClass && onBecomeLecturer != null) {
            ClassRepOutlinedButton(
              text = "I am a lecturer",
              leadingIcon = Icons.Default.School,
              onClick = { showBecomeLecturerDialog = true },
              modifier = Modifier.fillMaxWidth().testTag("profile_become_lecturer_button")
            )
            Spacer(modifier = Modifier.height(12.dp))
          }

          GoldButton(
            text = "Edit Profile",
            onClick = {
              editedName = profile?.fullName.orEmpty()
              selectedYearOfStudy = profile?.yearOfStudy
              selectedSchool = schools.find { it.id == profile?.schoolId || it.name == profile?.schoolName }
              if (selectedSchool != null) onSchoolSelected(selectedSchool!!)
              selectedProgramme = programmes.find { it.id == profile?.programmeId || it.name == profile?.programmeName }
              isEditing = true
            },
            testTag = "profile_edit_button"
          )
        } else {
          // Edit Mode
          Text("Edit Profile Details", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = GoldLight)
          Spacer(modifier = Modifier.height(14.dp))

          DarkTextField(
            value = editedName, onValueChange = { editedName = it }, label = "Full Name",
            leadingIcon = Icons.Default.Person,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            testTag = "profile_fullname_input"
          )
          Spacer(modifier = Modifier.height(12.dp))

          val canEditAcademicDetails = !isStaff && !isStudentInClass
          if (canEditAcademicDetails) {
            AppDropdownField(
              label = "School", selectedValueText = selectedSchool?.name.orEmpty(),
              items = schools, itemText = { it.name },
              onItemSelected = { selectedSchool = it; selectedProgramme = null; onSchoolSelected(it) },
              leadingIcon = Icons.Default.AccountBalance, placeholder = "Select school",
              testTag = "profile_school_dropdown"
            )
            Spacer(modifier = Modifier.height(12.dp))

            AppDropdownField(
              label = "Programme", selectedValueText = selectedProgramme?.name.orEmpty(),
              items = programmes, itemText = { it.name },
              onItemSelected = { prog ->
                selectedProgramme = prog
                val maxYears = prog.durationYears ?: 4
                if (selectedYearOfStudy != null && selectedYearOfStudy!! > maxYears) {
                  selectedYearOfStudy = null
                }
              },
              leadingIcon = Icons.Default.School,
              placeholder = if (selectedSchool == null) "Select school first" else "Select programme",
              testTag = "profile_programme_dropdown"
            )
            Spacer(modifier = Modifier.height(12.dp))

            val maxYears = selectedProgramme?.durationYears ?: 4
            val availableYears = (1..maxYears).toList()
            AppDropdownField(
              label = "Year of Study", selectedValueText = selectedYearOfStudy?.let { "Year $it" }.orEmpty(),
              items = availableYears, itemText = { "Year $it" },
              onItemSelected = { selectedYearOfStudy = it },
              leadingIcon = Icons.Default.CalendarMonth, placeholder = "Select year (1 to $maxYears)",
              testTag = "profile_year_dropdown"
            )
          } else {
            // Read-only info for students in a class or staff
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              ProfileFieldCard(Icons.Default.AccountBalance, "School", profile?.schoolName.orEmpty().ifBlank { "Not set" })
              ProfileFieldCard(Icons.Default.School, "Programme", profile?.programmeName.orEmpty().ifBlank { "Not set" })
              if (!isStaff) {
                ProfileFieldCard(Icons.Default.CalendarMonth, "Year of Study", profile?.yearOfStudy?.let { "Year $it" } ?: "Not set")
                Text(
                  text = "To change class, ask your class prefect or lecturer",
                  fontSize = 12.sp, color = GoldLight, modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(24.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
              onClick = { isEditing = false }, modifier = Modifier.weight(1f).height(46.dp),
              shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel", color = TextSecondary) }

            GoldButton(
              text = "Save",
              onClick = {
                onSaveProfile(editedName, selectedSchool, selectedProgramme, selectedYearOfStudy)
                isEditing = false
              },
              isLoading = isActionLoading, modifier = Modifier.weight(1.5f), testTag = "profile_save_button"
            )
          }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Red outlined Logout button at bottom ONLY on Profile screen
        OutlinedButton(
          onClick = { showLogoutConfirmDialog = true },
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_logout_button"),
          shape = RoundedCornerShape(ButtonRadius),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, ErrorRed.copy(alpha = 0.8f))
        ) {
          Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp), tint = ErrorRed)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Log Out", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  if (showLogoutConfirmDialog) {
    ConfirmActionDialog(
      title = "Confirm Logout",
      message = "Are you sure you want to log out of ClassRep?",
      confirmButtonText = "Log Out",
      confirmButtonColor = ErrorRed,
      onConfirm = {
        showLogoutConfirmDialog = false
        onLogout()
      },
      onDismiss = { showLogoutConfirmDialog = false }
    )
  }

  if (showBecomeLecturerDialog && onBecomeLecturer != null) {
    BecomeLecturerDialog(
      isLoading = isActionLoading,
      onDismiss = { showBecomeLecturerDialog = false },
      onSubmit = { code ->
        showBecomeLecturerDialog = false
        onBecomeLecturer(code)
      }
    )
  }
}
