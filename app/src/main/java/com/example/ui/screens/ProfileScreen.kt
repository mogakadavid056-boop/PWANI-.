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
import com.example.util.JsonHelper
import com.example.util.RoleHelper

@Composable
fun ProfileScreen(
  profile: ProfileItem?,
  schools: List<SchoolItem>,
  programmes: List<ProgrammeItem>,
  role: String = "",
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
  onToggleLeaderboardVisibility: (Boolean) -> Unit = {},
  onBecomeLecturer: (passcode: String, onResult: (Result<String>) -> Unit) -> Unit = { _, cb -> cb(Result.failure(Exception("Unavailable"))) },
  onShowError: (String) -> Unit = {},
  onShowSuccess: (String) -> Unit = {},
  unreadNotificationCount: Int = 0,
  onOpenNotifications: () -> Unit = {},
  onLogout: () -> Unit,
  onBack: () -> Unit
) {
  var isEditing by remember { mutableStateOf(false) }
  var showLogoutConfirmDialog by remember { mutableStateOf(false) }
  var showBecomeLecturerDialog by remember { mutableStateOf(false) }
  var isBecomingLecturer by remember { mutableStateOf(false) }
  var editedName by remember { mutableStateOf("") }
  var selectedSchool by remember { mutableStateOf<SchoolItem?>(null) }
  var selectedProgramme by remember { mutableStateOf<ProgrammeItem?>(null) }
  var selectedYearOfStudy by remember { mutableStateOf<Int?>(null) }

  val effectiveRole = role.ifBlank { profile?.role.orEmpty() }

  LaunchedEffect(profile) {
    if (profile != null && !isEditing) {
      editedName = profile.fullName
      selectedYearOfStudy = profile.yearOfStudy
      selectedSchool = schools.find { it.id == profile.schoolId || it.name == profile.schoolName }
      if (selectedSchool != null && !isStaff) onSchoolSelected(selectedSchool!!)
      selectedProgramme = programmes.find { it.id == profile.programmeId || it.name == profile.programmeName }
    }
  }

  Box(modifier = Modifier.fillMaxSize().testTag("profile_screen")) {
    Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
          }
          Spacer(modifier = Modifier.width(6.dp))
          MetallicGoldText("Profile", style = ScreenTitleStyle)
        }
        NotificationBellButton(unreadCount = unreadNotificationCount, onClick = onOpenNotifications)
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (!errorMessage.isNullOrBlank()) {
        ErrorBanner(message = errorMessage, onDismiss = onDismissError)
        Spacer(modifier = Modifier.height(14.dp))
      }
      if (!successMessage.isNullOrBlank()) {
        Box(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(RoyalSuccessContainer)
            .border(1.dp, SuccessColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)).padding(14.dp)
        ) {
          Text(text = successMessage, style = BodyStyle.copy(color = SuccessColor, fontSize = 14.sp, fontWeight = FontWeight.Medium))
        }
        Spacer(modifier = Modifier.height(14.dp))
      }

      if (isLoading && profile == null) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = MetallicGold, strokeWidth = 2.5.dp, modifier = Modifier.size(32.dp))
        }
      } else {
        if (!isEditing) {
          RoyalCard(isHero = true) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = JsonHelper.orDash(profile?.fullName),
                  style = ProfileNameStyle
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = JsonHelper.orDash(profile?.email),
                  style = SubtitleItalicStyle.copy(fontSize = 14.sp, color = Lavender)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              RoleBadge(roleText = RoleHelper.formatDisplayName(effectiveRole), isGoldFilled = true)
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          RoyalDivider()
          Spacer(modifier = Modifier.height(14.dp))

          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileFieldCard(Icons.Default.Person, "Full Name", JsonHelper.orDash(profile?.fullName))
            ProfileFieldCard(Icons.Default.Email, "Email", JsonHelper.orDash(profile?.email))
            ProfileFieldCard(Icons.Default.VerifiedUser, "Role", RoleHelper.formatDisplayName(effectiveRole))
            ProfileFieldCard(Icons.Default.Badge, "Admission Number", JsonHelper.orDash(profile?.admissionNumber))
            ProfileFieldCard(
              Icons.Default.CalendarMonth, "Year of Study",
              profile?.yearOfStudy?.let { "Year $it" } ?: "-"
            )
            ProfileFieldCard(Icons.Default.AccountBalance, "School", JsonHelper.orDash(profile?.schoolName))
            ProfileFieldCard(Icons.Default.School, "Programme", JsonHelper.orDash(profile?.programmeName))
          }

          if (!isStaff) {
            Spacer(modifier = Modifier.height(14.dp))
            val visibleOnBoard = profile?.showOnLeaderboard ?: true
            RoyalCard {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                  Icon(
                    imageVector = if (visibleOnBoard) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = ChampagneGold,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text("Show my name on leaderboard", style = FieldLabelStyle.copy(color = SilverWhite))
                    Text(
                      text = if (visibleOnBoard) "Visible to classmates on leaderboard" else "Shown as Anonymous on leaderboard",
                      style = CaptionStyle
                    )
                  }
                }
                Switch(
                  checked = visibleOnBoard,
                  onCheckedChange = onToggleLeaderboardVisibility,
                  modifier = Modifier.testTag("profile_leaderboard_visibility_switch"),
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = Midnight,
                    checkedTrackColor = ChampagneGold,
                    uncheckedThumbColor = Lavender,
                    uncheckedTrackColor = RoyalSurfaceHigh
                  )
                )
              }
            }
          }

          if (!isStaff && isStudentInClass) {
            Spacer(modifier = Modifier.height(12.dp))
            RoyalCard {
              Text(
                text = "Note: To change class, ask your class prefect or lecturer",
                style = CaptionStyle.copy(color = ChampagneGold, fontWeight = FontWeight.Medium)
              )
            }
          }

          Spacer(modifier = Modifier.height(24.dp))

          GoldButton(
            text = "Edit Profile",
            onClick = {
              editedName = profile?.fullName.orEmpty()
              selectedYearOfStudy = profile?.yearOfStudy
              selectedSchool = schools.find { it.id == profile?.schoolId || it.name == profile?.schoolName }
              if (selectedSchool != null && !isStaff) onSchoolSelected(selectedSchool!!)
              selectedProgramme = programmes.find { it.id == profile?.programmeId || it.name == profile?.programmeName }
              isEditing = true
            },
            testTag = "profile_edit_button"
          )

          if (!isStaff) {
            Spacer(modifier = Modifier.height(12.dp))
            GoldOutlinedButton(
              text = "I am a Lecturer",
              onClick = { showBecomeLecturerDialog = true },
              modifier = Modifier.fillMaxWidth().testTag("profile_become_lecturer_button")
            )
          }
        } else {
          Text("Edit Profile Details", style = SectionHeadingStyle)
          Spacer(modifier = Modifier.height(14.dp))

          DarkTextField(
            value = editedName, onValueChange = { editedName = it }, label = "Full Name",
            leadingIcon = Icons.Default.Person,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            testTag = "profile_fullname_input"
          )
          Spacer(modifier = Modifier.height(12.dp))

          if (!isStaff && !isStudentInClass) {
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
          } else if (!isStaff) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              ProfileFieldCard(Icons.Default.AccountBalance, "School", JsonHelper.orDash(profile?.schoolName))
              ProfileFieldCard(Icons.Default.School, "Programme", JsonHelper.orDash(profile?.programmeName))
              ProfileFieldCard(Icons.Default.CalendarMonth, "Year of Study", profile?.yearOfStudy?.let { "Year $it" } ?: "-")
              Text(
                text = "To change class, ask your class prefect or lecturer",
                style = CaptionStyle.copy(color = ChampagneGold),
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(24.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
              onClick = { isEditing = false },
              modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp),
              shape = RoundedCornerShape(ButtonRadius),
              border = androidx.compose.foundation.BorderStroke(1.5.dp, MetallicGold.copy(alpha = 0.6f))
            ) { Text("Cancel", style = ButtonTextStyle.copy(color = Lavender)) }

            GoldButton(
              text = "Save",
              onClick = {
                onSaveProfile(
                  editedName,
                  if (isStaff) null else selectedSchool,
                  if (isStaff) null else selectedProgramme,
                  if (isStaff) null else selectedYearOfStudy
                )
                isEditing = false
              },
              isLoading = isActionLoading, modifier = Modifier.weight(1.5f), testTag = "profile_save_button"
            )
          }
        }

        Spacer(modifier = Modifier.height(36.dp))

        OutlinedButton(
          onClick = { showLogoutConfirmDialog = true },
          modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp).testTag("profile_logout_button"),
          shape = RoundedCornerShape(ButtonRadius),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorColor),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, ErrorColor.copy(alpha = 0.8f))
        ) {
          Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp), tint = ErrorColor)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Log Out", style = ButtonTextStyle.copy(color = ErrorColor))
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  if (showBecomeLecturerDialog) {
    BecomeLecturerDialog(
      isLoading = isBecomingLecturer,
      onDismiss = { if (!isBecomingLecturer) showBecomeLecturerDialog = false },
      onSubmit = { passcode ->
        isBecomingLecturer = true
        onBecomeLecturer(passcode) { res ->
          isBecomingLecturer = false
          showBecomeLecturerDialog = false
          res.fold(onSuccess = onShowSuccess, onFailure = { err -> onShowError(err.message ?: "Failed to upgrade to Lecturer.") })
        }
      }
    )
  }

  if (showLogoutConfirmDialog) {
    ConfirmActionDialog(
      title = "Confirm Logout", message = "Are you sure you want to log out of ClassRep?",
      confirmButtonText = "Log Out", confirmButtonColor = ErrorColor,
      onConfirm = { showLogoutConfirmDialog = false; onLogout() }, onDismiss = { showLogoutConfirmDialog = false }
    )
  }
}
