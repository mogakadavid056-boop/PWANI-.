package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ClassUnitItem
import com.example.ui.theme.*

@Composable
fun StartAttendanceDialog(
  units: List<ClassUnitItem> = emptyList(),
  isUnitsLoading: Boolean = false,
  isLoading: Boolean,
  onDismiss: () -> Unit,
  onStart: (
    courseId: String?, courseTitle: String, courseCode: String,
    lecturerName: String, venueText: String, sessionTitle: String, durationMinutes: Int
  ) -> Unit
) {
  var unitMenuExpanded by remember { mutableStateOf(false) }
  var selectedUnit by remember(units) { mutableStateOf(units.firstOrNull()) }
  var isManual by remember(units) { mutableStateOf(units.isEmpty()) }
  var courseTitle by remember(units) { mutableStateOf(units.firstOrNull()?.courseName.orEmpty()) }
  var courseCode by remember(units) { mutableStateOf(units.firstOrNull()?.courseCode.orEmpty()) }
  var lecturerName by remember(units) { mutableStateOf(units.firstOrNull()?.lecturerName.orEmpty()) }
  var venueText by remember { mutableStateOf("") }
  var sessionTitle by remember { mutableStateOf("Class Attendance") }
  var selectedDuration by remember { mutableIntStateOf(10) }
  val durations = listOf(10, 20, 30)

  val unitLabel = when {
    isManual -> "Other (type manually)"
    selectedUnit != null -> listOfNotNull(
      selectedUnit?.courseCode?.takeIf { it.isNotBlank() },
      selectedUnit?.courseName?.takeIf { it.isNotBlank() }
    ).joinToString(" - ").ifBlank { "Selected Unit" }
    else -> "Select Unit"
  }

  Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurface)
        .border(1.dp, DarkBorder, RoundedCornerShape(18.dp))
        .padding(20.dp)
        .testTag("start_attendance_dialog")
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = GoldLight, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Start Attendance Session", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldLight)
        Spacer(modifier = Modifier.height(14.dp))

        Text("Unit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(4.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(DarkSurfaceVariant)
              .border(1.dp, if (unitMenuExpanded) GoldPrimary else DarkBorder, RoundedCornerShape(10.dp))
              .clickable(enabled = !isLoading) { unitMenuExpanded = !unitMenuExpanded }
              .padding(horizontal = 12.dp, vertical = 12.dp)
              .testTag("unit_dropdown_trigger"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              if (isUnitsLoading) "Loading units..." else unitLabel,
              fontSize = 14.sp, color = TextPrimary, maxLines = 1, modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Unit", tint = GoldPrimary)
          }
          androidx.compose.animation.AnimatedVisibility(visible = unitMenuExpanded) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceElevated).border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            ) {
              units.forEach { u ->
                val label = listOfNotNull(
                  u.courseCode.takeIf { it.isNotBlank() },
                  u.courseName.takeIf { it.isNotBlank() }
                ).joinToString(" - ")
                Text(
                  text = label,
                  color = TextPrimary,
                  fontSize = 14.sp,
                  modifier = Modifier.fillMaxWidth().clickable {
                    selectedUnit = u
                    isManual = false
                    courseTitle = u.courseName
                    courseCode = u.courseCode
                    lecturerName = u.lecturerName
                    unitMenuExpanded = false
                  }.padding(horizontal = 12.dp, vertical = 10.dp)
                )
                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
              }
              Text(
                text = "Other (type manually)",
                color = GoldLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().clickable {
                  selectedUnit = null
                  isManual = true
                  courseTitle = ""
                  courseCode = ""
                  lecturerName = ""
                  unitMenuExpanded = false
                }.padding(horizontal = 12.dp, vertical = 10.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        RegisterDialogField(courseTitle, { courseTitle = it }, "Course title", "start_course_title_input")
        Spacer(modifier = Modifier.height(8.dp))
        RegisterDialogField(courseCode, { courseCode = it }, "Course code", "start_course_code_input")
        Spacer(modifier = Modifier.height(8.dp))
        RegisterDialogField(lecturerName, { lecturerName = it }, "Lecturer in charge", "start_lecturer_input")
        Spacer(modifier = Modifier.height(8.dp))
        RegisterDialogField(venueText, { venueText = it }, "Venue", "start_venue_input")
        Spacer(modifier = Modifier.height(8.dp))
        RegisterDialogField(sessionTitle, { sessionTitle = it }, "Session title", "start_session_title_input")

        Spacer(modifier = Modifier.height(14.dp))
        Text("Session Duration", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          durations.forEach { duration ->
            val isSelected = selectedDuration == duration
            Box(
              modifier = Modifier
                .weight(1f).height(42.dp).clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) GoldPrimary else DarkSurfaceVariant)
                .border(1.dp, if (isSelected) GoldPrimary else DarkBorder, RoundedCornerShape(10.dp))
                .clickable(enabled = !isLoading) { selectedDuration = duration }
                .testTag("duration_${duration}_btn"),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = if (isSelected) DarkTextOnGold else TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("$duration min", fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) DarkTextOnGold else TextPrimary, maxLines = 1)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(
            onClick = onDismiss,
            enabled = !isLoading,
            modifier = Modifier.weight(1f).height(46.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.5.dp, GoldPrimary),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldLight, disabledContentColor = GoldLight.copy(alpha = 0.5f))
          ) {
            Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GoldLight, maxLines = 1, softWrap = false)
          }

          val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
          val context = LocalContext.current
          Button(
            onClick = {
              if (!isOnline) com.example.ui.components.OfflineToastHelper.show(context)
              else onStart(
                if (isManual) null else selectedUnit?.courseId,
                courseTitle, courseCode, lecturerName, venueText,
                sessionTitle.ifBlank { "Class Attendance" }, selectedDuration
              )
            },
            enabled = if (!isOnline) true else !isLoading,
            modifier = Modifier.weight(1.2f).height(46.dp).then(if (!isOnline) Modifier.alpha(0.5f) else Modifier).testTag("confirm_start_attendance_btn"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
          ) {
            if (isLoading) {
              CircularProgressIndicator(color = DarkTextOnGold, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
              Text("Start Session", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold, maxLines = 1, softWrap = false)
            }
          }
        }
      }
    }
  }
}

@Composable
fun RegisterDialogField(value: String, onValueChange: (String) -> Unit, label: String, tag: String) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label, fontSize = 13.sp) },
    singleLine = true,
    modifier = Modifier.fillMaxWidth().testTag(tag),
    shape = RoundedCornerShape(10.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = GoldPrimary, unfocusedBorderColor = DarkBorder,
      focusedContainerColor = DarkSurfaceVariant, unfocusedContainerColor = DarkSurfaceVariant,
      focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
      focusedLabelColor = GoldLight, unfocusedLabelColor = TextSecondary
    )
  )
}
