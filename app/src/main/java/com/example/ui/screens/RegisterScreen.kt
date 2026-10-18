package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun RegisterScreen(
  isLoading: Boolean,
  errorMessage: String?,
  schools: List<SchoolItem>,
  programmes: List<ProgrammeItem>,
  onSchoolSelected: (SchoolItem) -> Unit,
  onDismissError: () -> Unit,
  onRegisterWithKnown: (String, String, String, String, SchoolItem?, ProgrammeItem?, Int?) -> Unit,
  onRegisterWithCode: (String, String, String, String, String) -> Unit,
  onRegisterLecturer: (fullName: String, staffNumber: String, email: String, password: String, passcode: String) -> Unit = { _, _, _, _, _ -> },
  onNavigateToLogin: () -> Unit,
  onBack: () -> Unit
) {
  var isLecturerMode by remember { mutableStateOf(false) }
  var fullName by remember { mutableStateOf("") }
  var admissionNumber by remember { mutableStateOf("") }
  var staffNumber by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var lecturerPasscode by remember { mutableStateOf("") }
  var knowsDetails by remember { mutableStateOf(true) }
  var classCode by remember { mutableStateOf("") }
  var selectedSchool by remember { mutableStateOf<SchoolItem?>(null) }
  var selectedProgramme by remember { mutableStateOf<ProgrammeItem?>(null) }
  var selectedYearOfStudy by remember { mutableStateOf<Int?>(null) }
  var localValidationMessage by remember { mutableStateOf<String?>(null) }

  Box(modifier = Modifier.fillMaxSize().background(DarkBackground).testTag("register_screen")) {
    Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("Create Account", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GoldLight)
      }

      Spacer(modifier = Modifier.height(14.dp))
      Text("Join ClassRep for your class", fontSize = 14.sp, color = TextSecondary)
      Spacer(modifier = Modifier.height(16.dp))

      RegisterRoleSelector(
        isLecturerMode = isLecturerMode,
        onSelectStudent = { isLecturerMode = false; localValidationMessage = null },
        onSelectLecturer = { isLecturerMode = true; localValidationMessage = null }
      )
      Spacer(modifier = Modifier.height(18.dp))

      val activeError = errorMessage ?: localValidationMessage
      if (!activeError.isNullOrBlank()) {
        ErrorBanner(message = activeError, onDismiss = { localValidationMessage = null; onDismissError() })
        Spacer(modifier = Modifier.height(14.dp))
      }

      DarkTextField(
        value = fullName, onValueChange = { fullName = it }, label = "Full Name *",
        leadingIcon = Icons.Default.Person, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        testTag = "register_fullname_input"
      )
      Spacer(modifier = Modifier.height(12.dp))

      if (isLecturerMode) {
        LecturerRegisterFields(
          staffNumber = staffNumber,
          onStaffNumberChange = { staffNumber = it },
          email = email,
          onEmailChange = { email = it },
          password = password,
          onPasswordChange = { password = it },
          lecturerPasscode = lecturerPasscode,
          onLecturerPasscodeChange = { lecturerPasscode = it }
        )
      } else {
        DarkTextField(
          value = admissionNumber, onValueChange = { admissionNumber = it }, label = "Admission Number *",
          leadingIcon = Icons.Default.Badge, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
          testTag = "register_admission_input"
        )
        Spacer(modifier = Modifier.height(12.dp))

        DarkTextField(
          value = email, onValueChange = { email = it }, label = "University Email *",
          leadingIcon = Icons.Default.Email, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
          testTag = "register_email_input"
        )
        Spacer(modifier = Modifier.height(12.dp))

        PasswordTextField(
          value = password, onValueChange = { password = it }, label = "Password *",
          leadingIcon = Icons.Default.Lock, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
          testTag = "register_password_input"
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Switch
        Box(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)).padding(14.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
              Text(
                text = if (knowsDetails) "I know my school, programme and year" else "I'm not sure, I'll use a class code",
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = if (knowsDetails) "Tap switch to use class code instead" else "Tap switch to choose programme & year",
                fontSize = 11.sp, color = TextSecondary
              )
            }

            Switch(
              checked = knowsDetails,
              onCheckedChange = { knowsDetails = it },
              colors = SwitchDefaults.colors(
                checkedThumbColor = GoldPrimary, checkedTrackColor = GoldPrimary.copy(alpha = 0.3f),
                uncheckedThumbColor = TextSecondary, uncheckedTrackColor = DarkSurface
              ),
              modifier = Modifier.testTag("register_mode_switch")
            )
          }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (knowsDetails) {
          AppDropdownField(
            label = "School *", selectedValueText = selectedSchool?.name.orEmpty(),
            items = schools, itemText = { it.name },
            onItemSelected = { selectedSchool = it; selectedProgramme = null; onSchoolSelected(it) },
            leadingIcon = Icons.Default.AccountBalance, placeholder = "Select your school",
            testTag = "register_school_dropdown"
          )
          Spacer(modifier = Modifier.height(12.dp))

          AppDropdownField(
            label = "Programme *", selectedValueText = selectedProgramme?.name.orEmpty(),
            items = programmes, itemText = { it.name },
            onItemSelected = { prog ->
              selectedProgramme = prog
              val maxYears = prog.durationYears ?: 4
              if (selectedYearOfStudy != null && selectedYearOfStudy!! > maxYears) {
                selectedYearOfStudy = null
              }
            },
            leadingIcon = Icons.Default.School, placeholder = if (selectedSchool == null) "Select school first" else "Select your programme",
            testTag = "register_programme_dropdown"
          )
          Spacer(modifier = Modifier.height(12.dp))

          val maxYears = selectedProgramme?.durationYears ?: 4
          val availableYears = (1..maxYears).toList()
          AppDropdownField(
            label = "Year of Study *", selectedValueText = selectedYearOfStudy?.let { "Year $it" }.orEmpty(),
            items = availableYears, itemText = { "Year $it" }, onItemSelected = { selectedYearOfStudy = it },
            leadingIcon = Icons.Default.CalendarMonth, placeholder = "Select year (1 to $maxYears)",
            testTag = "register_year_dropdown"
          )
        } else {
          DarkTextField(
            value = classCode, onValueChange = { classCode = it.uppercase() }, label = "Class Join Code *",
            leadingIcon = Icons.Default.Key, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
            testTag = "register_class_code_input"
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      GoldButton(
        text = "Register",
        onClick = {
          localValidationMessage = null
          if (isLecturerMode) {
            if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
              localValidationMessage = "Please enter your full name, email, and password."
              return@GoldButton
            }
            if (lecturerPasscode.isBlank()) {
              localValidationMessage = "Please enter the lecturer passcode."
              return@GoldButton
            }
            onRegisterLecturer(fullName, staffNumber, email, password, lecturerPasscode)
          } else {
            if (fullName.isBlank() || admissionNumber.isBlank() || email.isBlank() || password.isBlank()) {
              localValidationMessage = "Please fill in all registration fields."
              return@GoldButton
            }
            if (knowsDetails) {
              if (selectedSchool == null || selectedProgramme == null || selectedYearOfStudy == null) {
                localValidationMessage = "Please select your School, Programme, and Year of Study."
                return@GoldButton
              }
              onRegisterWithKnown(fullName, admissionNumber, email, password, selectedSchool, selectedProgramme, selectedYearOfStudy)
            } else {
              if (classCode.isBlank()) {
                localValidationMessage = "Please enter your class code."
                return@GoldButton
              }
              onRegisterWithCode(fullName, admissionNumber, email, password, classCode)
            }
          }
        },
        isLoading = isLoading,
        testTag = "register_submit_button"
      )

      Spacer(modifier = Modifier.height(20.dp))

      Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Already have an account? ", fontSize = 14.sp, color = TextSecondary)
        Text(
          text = "Sign In", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldPrimary,
          modifier = Modifier.clickable { onNavigateToLogin() }.testTag("register_to_login_link")
        )
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
