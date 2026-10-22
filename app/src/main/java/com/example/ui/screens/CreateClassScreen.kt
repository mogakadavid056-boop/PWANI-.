package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AcademicYearItem
import com.example.data.ProgrammeItem
import com.example.data.SchoolItem
import com.example.ui.components.AppDropdownField
import com.example.ui.components.DarkTextField
import com.example.ui.components.ErrorBanner
import com.example.ui.components.GoldButton
import com.example.ui.theme.*

@Composable
fun CreateClassScreen(
  isLoading: Boolean,
  errorMessage: String?,
  createdJoinCode: String?,
  schools: List<SchoolItem>,
  programmes: List<ProgrammeItem>,
  academicYears: List<AcademicYearItem>,
  onSchoolSelected: (SchoolItem) -> Unit,
  onDismissError: () -> Unit,
  onDismissSuccessDialog: () -> Unit,
  onCreateSubmit: (name: String, code: String, uId: String?, sId: String?, pId: String?, aId: String?, yearOfStudy: Int?) -> Unit,
  onBack: () -> Unit
) {
  var className by remember { mutableStateOf("") }
  var classCode by remember { mutableStateOf("") }
  var selectedSchool by remember { mutableStateOf<SchoolItem?>(null) }
  var selectedProgramme by remember { mutableStateOf<ProgrammeItem?>(null) }
  var selectedAcademicYear by remember { mutableStateOf<AcademicYearItem?>(null) }
  var selectedYearOfStudy by remember { mutableStateOf<Int?>(null) }

  LaunchedEffect(academicYears) {
    if (selectedAcademicYear == null && academicYears.isNotEmpty()) {
      selectedAcademicYear = academicYears.find { it.isCurrent } ?: academicYears.firstOrNull()
    }
  }

  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current

  Box(modifier = Modifier.fillMaxSize().background(DarkBackground).testTag("create_class_screen")) {
    Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("Create Class", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GoldLight)
      }

      Spacer(modifier = Modifier.height(14.dp))
      Text("Set up a new university class. A unique join code will be generated for students.", fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
      Spacer(modifier = Modifier.height(18.dp))

      if (!errorMessage.isNullOrBlank()) {
        ErrorBanner(message = errorMessage, onDismiss = onDismissError)
        Spacer(modifier = Modifier.height(14.dp))
      }

      DarkTextField(
        value = className, onValueChange = { className = it }, label = "Class Name (e.g. Distributed Systems)",
        leadingIcon = Icons.Default.Class, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        testTag = "create_class_name_input"
      )
      Spacer(modifier = Modifier.height(12.dp))

      DarkTextField(
        value = classCode, onValueChange = { classCode = it }, label = "Class Code (e.g. CS401)",
        leadingIcon = Icons.Default.Numbers, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
        testTag = "create_class_code_input"
      )
      Spacer(modifier = Modifier.height(12.dp))

      AppDropdownField(
        label = "School", selectedValueText = selectedSchool?.name.orEmpty(), items = schools, itemText = { it.name },
        onItemSelected = { selectedSchool = it; selectedProgramme = null; onSchoolSelected(it) },
        leadingIcon = Icons.Default.AccountBalance, placeholder = "Select school", testTag = "create_class_school_dropdown"
      )
      Spacer(modifier = Modifier.height(12.dp))

      AppDropdownField(
        label = "Programme", selectedValueText = selectedProgramme?.name.orEmpty(), items = programmes, itemText = { it.name },
        onItemSelected = { selectedProgramme = it }, leadingIcon = Icons.Default.School,
        placeholder = if (selectedSchool == null) "Select school first" else "Select programme", testTag = "create_class_programme_dropdown"
      )
      Spacer(modifier = Modifier.height(12.dp))

      // Year of study dropdown (1 to 6)
      AppDropdownField(
        label = "Year of Study", selectedValueText = selectedYearOfStudy?.let { "Year $it" }.orEmpty(),
        items = listOf(1, 2, 3, 4, 5, 6), itemText = { "Year $it" },
        onItemSelected = { selectedYearOfStudy = it },
        leadingIcon = Icons.Default.CalendarMonth, placeholder = "Select year of study (1 to 6)",
        testTag = "create_class_year_of_study_dropdown"
      )
      Spacer(modifier = Modifier.height(12.dp))

      AppDropdownField(
        label = "Academic Year", selectedValueText = selectedAcademicYear?.let { if (it.isCurrent) "${it.name} (Current)" else it.name }.orEmpty(),
        items = academicYears, itemText = { if (it.isCurrent) "${it.name} (Current)" else it.name },
        onItemSelected = { selectedAcademicYear = it }, leadingIcon = Icons.Default.DateRange,
        placeholder = "Select academic year", testTag = "create_class_year_dropdown"
      )
      Spacer(modifier = Modifier.height(10.dp))

      // Note for automatic student enrollment
      Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(DarkSurface)
          .border(1.dp, DarkBorder, RoundedCornerShape(8.dp)).padding(10.dp)
      ) {
        Text(
          text = "Note: Students who registered for this programme and year will be added automatically.",
          fontSize = 12.sp, color = GoldLight, lineHeight = 16.sp
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
      val context = LocalContext.current
      GoldButton(
        text = "Create Class",
        onClick = {
          if (!isOnline) {
            com.example.ui.components.OfflineToastHelper.show(context)
          } else {
            onCreateSubmit(
              className, classCode, selectedSchool?.universityId, selectedSchool?.id,
              selectedProgramme?.id, selectedAcademicYear?.id, selectedYearOfStudy
            )
          }
        },
        isLoading = isLoading,
        modifier = Modifier.then(if (!isOnline) Modifier.alpha(0.5f) else Modifier),
        testTag = "create_class_submit_button"
      )
    }

    if (!createdJoinCode.isNullOrBlank()) {
      AlertDialog(
        onDismissRequest = onDismissSuccessDialog,
        containerColor = DarkSurface,
        title = { Text("Class Created!", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
          Column {
            Text("Share this join code with your class members:", color = TextSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Box(
              modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DarkSurfaceVariant)
                .border(1.5.dp, GoldPrimary, RoundedCornerShape(12.dp)).padding(vertical = 14.dp, horizontal = 12.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = createdJoinCode, color = GoldLight, fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold, fontFamily = com.example.ui.theme.PoppinsFamily, letterSpacing = 2.sp
              )
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              clipboardManager.setText(AnnotatedString(createdJoinCode))
              Toast.makeText(context, "Join code copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = com.example.ui.theme.DarkTextOnGold),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = com.example.ui.theme.DarkTextOnGold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy Code", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = com.example.ui.theme.DarkTextOnGold)
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = onDismissSuccessDialog,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldPrimary)
          ) {
            Text("Done", color = GoldPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
          }
        }
      )
    }
  }
}
