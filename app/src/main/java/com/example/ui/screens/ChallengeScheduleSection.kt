package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CourseUnit
import com.example.ui.components.royalOutlinedTextFieldColors
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.RoyalSurface
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SilverWhite
import com.example.util.DurationHelper
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun ScheduleOptionsSection(
  expanded: Boolean,
  onToggleExpanded: () -> Unit,
  description: String,
  onDescriptionChange: (String) -> Unit,
  shuffleQuestions: Boolean,
  onShuffleChange: (Boolean) -> Unit,
  approvedUnits: List<CourseUnit>,
  selectedUnit: CourseUnit?,
  onSelectUnit: (CourseUnit?) -> Unit,
  asExam: Boolean,
  openPresets: List<String>,
  selectedOpenPreset: String,
  onSelectOpenPreset: (String) -> Unit,
  customOpenDelaySeconds: Long?,
  onCustomOpenDelayChange: (Long?) -> Unit,
  closePresets: List<String>,
  selectedClosePreset: String,
  onSelectClosePreset: (String) -> Unit,
  customCloseWindowSeconds: Long?,
  onCustomCloseWindowChange: (Long?) -> Unit,
  timeLimitSeconds: Long?,
  onSelectTimeLimitSeconds: (Long?) -> Unit
) {
  var unitMenuExpanded by remember { mutableStateOf(false) }

  val openSummary = if (selectedOpenPreset == "Custom") {
    customOpenDelaySeconds?.let { "In ${DurationHelper.formatSummary(it)}" } ?: "Opens Now"
  } else selectedOpenPreset

  val closeSummary = if (selectedClosePreset == "Custom") {
    customCloseWindowSeconds?.let { DurationHelper.formatSummary(it) } ?: "24 hr"
  } else selectedClosePreset

  val limitSummary = DurationHelper.formatSummary(timeLimitSeconds)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(RoyalSurface)
      .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 48.dp)
        .clickable(onClick = onToggleExpanded)
        .padding(horizontal = 12.dp, vertical = 10.dp)
        .testTag("schedule_options_toggle"),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Schedule, Unit & Duration", style = FieldLabelStyle.copy(color = ChampagneGold))
        Text(
          text = "$openSummary • $closeSummary • $limitSummary" +
            (if (shuffleQuestions) " • Shuffled" else "") +
            (selectedUnit?.let { " • ${it.code}" } ?: ""),
          style = CaptionStyle.copy(fontSize = 12.sp, color = Lavender)
        )
      }
      Icon(
        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
        contentDescription = "Toggle schedule and options",
        tint = ChampagneGold
      )
    }

    AnimatedVisibility(visible = expanded) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        HorizontalDivider(color = DarkBorder.copy(alpha = 0.6f))
        OutlinedTextField(
          value = description,
          onValueChange = onDescriptionChange,
          label = { Text("Instructions (optional)") },
          placeholder = { Text("Short note for students...") },
          colors = royalOutlinedTextFieldColors(),
          modifier = Modifier.fillMaxWidth().testTag("challenge_desc_input"),
          maxLines = 3
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Shuffle question order", style = BodyStyle)
            Text("Randomizes question sequence per student", style = CaptionStyle)
          }
          Switch(
            checked = shuffleQuestions,
            onCheckedChange = onShuffleChange,
            colors = SwitchDefaults.colors(
              checkedThumbColor = Midnight,
              checkedTrackColor = MetallicGold,
              uncheckedThumbColor = Lavender,
              uncheckedTrackColor = RoyalSurfaceHigh
            ),
            modifier = Modifier.testTag("shuffle_questions_switch")
          )
        }

        if (approvedUnits.isNotEmpty()) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text("Course Unit (optional)", style = FieldLabelStyle)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ButtonRadius))
                .background(RoyalSurfaceHigh)
                .border(1.dp, DarkBorder, RoundedCornerShape(ButtonRadius))
                .clickable { unitMenuExpanded = !unitMenuExpanded }
                .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = selectedUnit?.let { "${it.code} • ${it.name}" } ?: "General Class (All Units)",
                  style = BodyStyle
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = ChampagneGold)
              }
            }
            AnimatedVisibility(visible = unitMenuExpanded) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 4.dp)
                  .clip(RoundedCornerShape(ButtonRadius))
                  .background(RoyalSurfaceHigh)
                  .border(1.dp, DarkBorder, RoundedCornerShape(ButtonRadius))
              ) {
                Text(
                  text = "General Class (All Units)",
                  style = BodyStyle.copy(color = if (selectedUnit == null) ChampagneGold else SilverWhite),
                  modifier = Modifier.fillMaxWidth().clickable { onSelectUnit(null); unitMenuExpanded = false }.padding(12.dp)
                )
                approvedUnits.forEach { unit ->
                  HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
                  Text(
                    text = "${unit.code} • ${unit.name}",
                    style = BodyStyle.copy(color = if (selectedUnit?.courseId == unit.courseId) ChampagneGold else SilverWhite),
                    modifier = Modifier.fillMaxWidth().clickable { onSelectUnit(unit); unitMenuExpanded = false }.padding(12.dp)
                  )
                }
              }
            }
          }
        }

        Text("Opens", style = FieldLabelStyle)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          openPresets.forEach { preset ->
            RoyalPresetPill(label = preset, selected = selectedOpenPreset == preset, onClick = { onSelectOpenPreset(preset) })
          }
        }
        if (selectedOpenPreset == "Custom") {
          DurationAmountUnitPicker(
            title = "Start Delay from Now",
            totalSeconds = customOpenDelaySeconds,
            allowOff = false,
            tagPrefix = "custom_open_delay",
            onSecondsChange = onCustomOpenDelayChange
          )
        }

        Text("Duration Open (Availability Window)", style = FieldLabelStyle)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          closePresets.forEach { preset ->
            RoyalPresetPill(label = preset, selected = selectedClosePreset == preset, onClick = { onSelectClosePreset(preset) })
          }
        }
        if (selectedClosePreset == "Custom") {
          DurationAmountUnitPicker(
            title = "Window Open Length (Min 1 min)",
            totalSeconds = customCloseWindowSeconds,
            allowOff = false,
            tagPrefix = "custom_close_window",
            onSecondsChange = onCustomCloseWindowChange
          )
        }

        val quickSeconds: List<Long?> = if (asExam) {
          listOf(900L, 1800L, 2700L, 3600L, 5400L, 7200L)
        } else {
          listOf(null, 300L, 600L, 900L, 1800L, 3600L)
        }
        Text(if (asExam) "Overall Exam Time Limit" else "Overall Quiz Time Limit", style = FieldLabelStyle)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          quickSeconds.forEach { secs ->
            RoyalPresetPill(
              label = DurationHelper.formatSummary(secs),
              selected = timeLimitSeconds == secs,
              onClick = { onSelectTimeLimitSeconds(secs) }
            )
          }
        }
        DurationAmountUnitPicker(
          title = "Custom Time Limit (30 sec – 24 hr)",
          totalSeconds = timeLimitSeconds,
          allowOff = !asExam,
          tagPrefix = "overall_time_limit",
          onSecondsChange = onSelectTimeLimitSeconds
        )
      }
    }
  }
}

@Composable
fun RoyalPresetPill(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .defaultMinSize(minHeight = 48.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(if (selected) MetallicGold else RoyalSurface)
      .border(1.dp, if (selected) ChampagneGold else DarkBorder, RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      style = FieldLabelStyle.copy(
        fontWeight = FontWeight.SemiBold,
        color = if (selected) Midnight else Lavender
      )
    )
  }
}

fun computeChallengeOpenCloseIso(
  openPreset: String,
  closePreset: String,
  customOpenDelaySeconds: Long? = null,
  customCloseWindowSeconds: Long? = null
): Pair<String, String> {
  val now = Instant.now()
  val opensAt = when (openPreset) {
    "In 1 hour" -> now.plusSeconds(3600L)
    "Tomorrow 8 AM" -> ZonedDateTime.now(ZoneId.systemDefault()).plusDays(1).with(LocalTime.of(8, 0, 0)).toInstant()
    "Custom" -> now.plusSeconds((customOpenDelaySeconds ?: 0L).coerceAtLeast(0L))
    else -> now
  }
  val durationSeconds = when (closePreset) {
    "6 hours" -> 6L * 3600L
    "3 days" -> 3L * 86_400L
    "7 days" -> 7L * 86_400L
    "Custom" -> (customCloseWindowSeconds ?: 3600L).coerceAtLeast(60L)
    else -> 24L * 3600L
  }
  return opensAt.toString() to opensAt.plusSeconds(durationSeconds).toString()
}
