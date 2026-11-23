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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CountdownMode
import com.example.data.CountdownStyle
import com.example.data.ExamExitAction
import com.example.data.ExamSecurityConfig
import com.example.ui.components.royalOutlinedTextFieldColors
import com.example.ui.theme.BodyStyle
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
import com.example.util.DurationUnit

@Composable
fun ExamSecuritySettingsCard(
  expanded: Boolean,
  onToggleExpanded: () -> Unit,
  asExam: Boolean,
  securityConfig: ExamSecurityConfig,
  onUpdateConfig: (ExamSecurityConfig) -> Unit
) {
  val summaryParts = buildList {
    add(securityConfig.countdownMode.label)
    securityConfig.perQuestionSeconds?.let { add("${DurationHelper.formatSummary(it)}/Q") }
    if (securityConfig.oneWayNavigation) add("No back")
    if (securityConfig.noCopyPaste) add("No copy")
    if (securityConfig.onExitAction != ExamExitAction.OFF) add(securityConfig.onExitAction.label)
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(RoyalSurface)
      .border(1.dp, ChampagneGold.copy(alpha = if (asExam) 0.5f else 0.25f), RoundedCornerShape(12.dp))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 48.dp)
        .clickable(onClick = onToggleExpanded)
        .padding(horizontal = 12.dp, vertical = 10.dp)
        .testTag("exam_security_toggle"),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Security, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text("Timer Mode & Exam Security", style = FieldLabelStyle.copy(color = ChampagneGold))
          Text(
            text = summaryParts.joinToString(" • ").ifBlank { "Standard mode" },
            style = CaptionStyle.copy(fontSize = 12.sp, color = Lavender),
            maxLines = 2
          )
        }
      }
      Icon(
        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
        contentDescription = "Toggle exam security settings",
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

        Text("Countdown Mode", style = FieldLabelStyle)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          CountdownMode.entries.forEach { mode ->
            RoyalPresetPill(
              label = mode.label,
              selected = securityConfig.countdownMode == mode,
              onClick = { onUpdateConfig(securityConfig.copy(countdownMode = mode)) },
              modifier = Modifier.testTag("countdown_mode_${mode.apiValue}")
            )
          }
        }
        Text(securityConfig.countdownMode.shortHint, style = CaptionStyle.copy(color = ChampagneGold))

        Text("Countdown Display Style", style = FieldLabelStyle)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          CountdownStyle.entries.forEach { style ->
            RoyalPresetPill(
              label = style.label,
              selected = securityConfig.countdownStyle == style,
              onClick = { onUpdateConfig(securityConfig.copy(countdownStyle = style)) },
              modifier = Modifier.testTag("countdown_style_${style.apiValue}")
            )
          }
        }

        DurationAmountUnitPicker(
          title = "Default Per-Question Timer (optional)",
          subtitle = "Auto-locks question & moves to next when timer hits 0",
          totalSeconds = securityConfig.perQuestionSeconds,
          allowOff = true,
          tagPrefix = "per_question_timer",
          onSecondsChange = { secs ->
            val nextMode = if (secs != null && securityConfig.countdownMode == CountdownMode.OVERALL) {
              if (securityConfig.timeLimitSeconds != null) CountdownMode.BOTH else CountdownMode.PER_QUESTION
            } else {
              securityConfig.countdownMode
            }
            onUpdateConfig(securityConfig.copy(perQuestionSeconds = secs, countdownMode = nextMode))
          }
        )

        HorizontalDivider(color = DarkBorder.copy(alpha = 0.45f))
        Text("Exam Security & Integrity", style = FieldLabelStyle.copy(color = ChampagneGold))

        SecuritySwitchRow(
          title = "One-way navigation (No turning back)",
          subtitle = "Once you move to the next question, previous questions stay locked",
          checked = securityConfig.oneWayNavigation,
          tag = "security_one_way_switch",
          onCheckedChange = { onUpdateConfig(securityConfig.copy(oneWayNavigation = it)) }
        )

        SecuritySwitchRow(
          title = "Lock question after leaving/answering",
          subtitle = "Prevents editing any previously visited question",
          checked = securityConfig.lockOnAnswer,
          tag = "security_lock_answer_switch",
          onCheckedChange = { onUpdateConfig(securityConfig.copy(lockOnAnswer = it)) }
        )

        SecuritySwitchRow(
          title = "No copy-paste & block screenshots",
          subtitle = "Blocks screenshots, text selection, and clipboard paste",
          checked = securityConfig.noCopyPaste,
          tag = "security_no_copy_switch",
          onCheckedChange = { onUpdateConfig(securityConfig.copy(noCopyPaste = it)) }
        )

        SecuritySwitchRow(
          title = "Strict deadline enforcement",
          subtitle = "Server rejects late MCQ scoring after time limit/close window",
          checked = securityConfig.strictDeadline,
          tag = "security_strict_deadline_switch",
          onCheckedChange = { onUpdateConfig(securityConfig.copy(strictDeadline = it)) }
        )

        Text("When Student Leaves / Exits App", style = FieldLabelStyle)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ExamExitAction.entries.forEach { action ->
            RoyalPresetPill(
              label = action.label,
              selected = securityConfig.onExitAction == action,
              onClick = { onUpdateConfig(securityConfig.copy(onExitAction = action)) },
              modifier = Modifier.testTag("exit_action_${action.apiValue}")
            )
          }
        }

        if (securityConfig.onExitAction == ExamExitAction.WARN) {
          Text("Max exits before auto-submit", style = CaptionStyle)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(1, 2, 3, 5).forEach { count ->
              RoyalPresetPill(
                label = "$count exit${if (count == 1) "" else "s"}",
                selected = securityConfig.maxExitViolations == count,
                onClick = { onUpdateConfig(securityConfig.copy(maxExitViolations = count)) },
                modifier = Modifier.testTag("max_exit_$count")
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun DurationAmountUnitPicker(
  title: String,
  subtitle: String? = null,
  totalSeconds: Long?,
  allowOff: Boolean = true,
  tagPrefix: String,
  onSecondsChange: (Long?) -> Unit
) {
  val decomposed = remember(totalSeconds) {
    if (totalSeconds != null && totalSeconds > 0L) DurationHelper.decomposeBestUnit(totalSeconds) else null
  }
  var enabled by remember { mutableStateOf(totalSeconds != null) }
  var amountText by remember { mutableStateOf(decomposed?.first?.toString() ?: "") }
  var selectedUnit by remember { mutableStateOf(decomposed?.second ?: DurationUnit.MINUTES) }

  androidx.compose.runtime.LaunchedEffect(totalSeconds) {
    if (totalSeconds != null && totalSeconds > 0L) {
      enabled = true
      val currentComputed = DurationHelper.toSeconds(amountText.toLongOrNull(), selectedUnit)
      if (currentComputed != totalSeconds && decomposed != null) {
        amountText = decomposed.first.toString()
        selectedUnit = decomposed.second
      }
    }
  }

  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = FieldLabelStyle)
        if (!subtitle.isNullOrBlank()) {
          Text(subtitle, style = CaptionStyle)
        }
      }
      if (allowOff) {
        Switch(
          checked = enabled,
          onCheckedChange = { next ->
            enabled = next
            if (!next) {
              amountText = ""
              onSecondsChange(null)
            } else {
              val valNum = amountText.toLongOrNull()?.takeIf { it > 0L } ?: 1L
              amountText = valNum.toString()
              onSecondsChange(DurationHelper.toSeconds(valNum, selectedUnit))
            }
          },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Midnight,
            checkedTrackColor = MetallicGold,
            uncheckedThumbColor = Lavender,
            uncheckedTrackColor = RoyalSurfaceHigh
          ),
          modifier = Modifier.testTag("${tagPrefix}_switch")
        )
      }
    }

    if (enabled || !allowOff) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = amountText,
          onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(6)
            amountText = digits
            val num = digits.toLongOrNull()?.takeIf { it > 0L }
            onSecondsChange(if (num != null) DurationHelper.toSeconds(num, selectedUnit) else null)
          },
          label = { Text("Duration") },
          placeholder = { Text("Any") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = royalOutlinedTextFieldColors(),
          singleLine = true,
          modifier = Modifier.width(116.dp).testTag("${tagPrefix}_amount_input")
        )

        Row(
          modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          DurationUnit.entries.forEach { unit ->
            val selected = selectedUnit == unit
            Box(
              modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) MetallicGold else RoyalSurfaceHigh)
                .border(1.dp, if (selected) ChampagneGold else DarkBorder, RoundedCornerShape(10.dp))
                .clickable {
                  selectedUnit = unit
                  val num = amountText.toLongOrNull()?.takeIf { it > 0L }
                  if (num != null) {
                    onSecondsChange(DurationHelper.toSeconds(num, unit))
                  }
                }
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .testTag("${tagPrefix}_unit_${unit.shortLabel}"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = unit.label,
                style = FieldLabelStyle.copy(
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (selected) Midnight else SilverWhite
                )
              )
            }
          }
        }
      }
      if (totalSeconds != null && totalSeconds > 0L) {
        Text(
          text = "Set to: ${DurationHelper.formatSummary(totalSeconds)} (${totalSeconds}s)",
          style = CaptionStyle.copy(color = ChampagneGold)
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
    }
  }
}

@Composable
private fun SecuritySwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  tag: String,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
      Text(title, style = BodyStyle)
      Text(subtitle, style = CaptionStyle)
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Midnight,
        checkedTrackColor = MetallicGold,
        uncheckedThumbColor = Lavender,
        uncheckedTrackColor = RoyalSurfaceHigh
      ),
      modifier = Modifier.testTag(tag)
    )
  }
}
