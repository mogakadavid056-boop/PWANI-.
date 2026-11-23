package com.example.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ScheduleItem
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardTitleStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.RoyalCardSurface
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.SilverWhite

@Composable
fun TimetableSection(
  schedules: List<ScheduleItem>,
  selectedDay: Int,
  todayDayOfWeek: Int,
  isStaff: Boolean,
  isLoading: Boolean,
  onRefresh: () -> Unit,
  onSelectDay: (Int) -> Unit,
  onAddClassClick: () -> Unit,
  onEditSchedule: (ScheduleItem) -> Unit,
  onDeleteSchedule: (ScheduleItem) -> Unit,
  onChangeScheduleStatus: (ScheduleItem) -> Unit = {}
) {
  val dayFilters = listOf(
    0 to "All",
    1 to "Mon",
    2 to "Tue",
    3 to "Wed",
    4 to "Thu",
    5 to "Fri",
    6 to "Sat",
    7 to "Sun"
  )

  val filteredSchedules = if (selectedDay == 0) schedules else schedules.filter { it.dayOfWeek == selectedDay }

  Column(modifier = Modifier.fillMaxWidth().testTag("timetable_section")) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "Timetable",
          style = SectionHeadingStyle.copy(color = ChampagneGold),
          modifier = Modifier.testTag("timetable_section_title")
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "(${schedules.size})", style = CaptionStyle.copy(color = Lavender))
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onRefresh,
          modifier = Modifier.size(40.dp).testTag("refresh_timetable_btn")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ChampagneGold, modifier = Modifier.size(20.dp))
        }

        if (isStaff) {
          Spacer(modifier = Modifier.width(6.dp))
          Button(
            onClick = onAddClassClick,
            modifier = Modifier.height(40.dp).testTag("add_class_schedule_btn"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Midnight)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Class", style = ButtonTextStyle.copy(color = Midnight, fontSize = 13.sp))
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      dayFilters.forEach { (d, label) ->
        val isSelected = selectedDay == d
        val isToday = d == todayDayOfWeek
        val pillText = if (isToday && d != 0) "$label •" else label
        val pillShape = RoundedCornerShape(10.dp)
        val glowMod = if (isSelected || isToday) {
          Modifier.shadow(8.dp, pillShape, ambientColor = ElectricViolet, spotColor = ElectricViolet)
        } else {
          Modifier
        }

        Box(
          modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .then(glowMod)
            .clip(pillShape)
            .background(
              if (isSelected) Brush.horizontalGradient(listOf(ChampagneGold, MetallicGold))
              else Brush.horizontalGradient(listOf(RoyalCardSurface, RoyalCardSurface))
            )
            .border(
              width = if (isToday || isSelected) 1.5.dp else 1.dp,
              color = when {
                isSelected -> ChampagneGold
                isToday -> ElectricViolet
                else -> DarkBorder
              },
              shape = pillShape
            )
            .clickable { onSelectDay(d) }
            .padding(horizontal = 13.dp, vertical = 8.dp)
            .testTag("day_filter_$d"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = pillText,
            style = FieldLabelStyle.copy(
              color = when {
                isSelected -> Midnight
                isToday -> ChampagneGold
                else -> Lavender
              }
            )
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (isLoading) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LoadingSkeleton(height = 90)
        LoadingSkeleton(height = 90)
        LoadingSkeleton(height = 90)
      }
    } else if (filteredSchedules.isEmpty()) {
      EmptyState(
        icon = Icons.Default.CalendarMonth,
        title = "No Classes Scheduled",
        message = if (isStaff) "Tap Add Class above to create the timetable." else "Nothing here yet. Waiting for your class prefect to update this.",
        actionLabel = if (isStaff) "Add Class" else null,
        onActionClick = if (isStaff) onAddClassClick else null,
        modifier = Modifier.testTag("timetable_empty_state")
      )
    } else {
      val grouped = filteredSchedules.groupBy { it.dayOfWeek }
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        grouped.keys.sorted().forEach { dayInt ->
          val dayList = grouped[dayInt].orEmpty()
          val isToday = dayInt == todayDayOfWeek
          val dayName = dayList.firstOrNull()?.dayName ?: "Day $dayInt"
          val headerShape = RoundedCornerShape(10.dp)
          val todayGlow = if (isToday) {
            Modifier.shadow(10.dp, headerShape, ambientColor = ElectricViolet, spotColor = ElectricViolet)
          } else {
            Modifier
          }

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .then(todayGlow)
                .clip(headerShape)
                .background(if (isToday) RoyalPurple.copy(alpha = 0.65f) else RoyalSurfaceHigh.copy(alpha = 0.7f))
                .border(
                  width = if (isToday) 1.dp else 0.5.dp,
                  color = if (isToday) ElectricViolet else DarkBorder,
                  shape = headerShape
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Text(
                text = dayName,
                style = CardTitleStyle.copy(
                  color = if (isToday) ChampagneGold else SilverWhite
                )
              )
              if (isToday) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ElectricViolet)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text(
                    "TODAY",
                    style = FieldLabelStyle.copy(color = SilverWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  )
                }
              }
            }

            dayList.forEach { scheduleItem ->
              ScheduleCard(
                schedule = scheduleItem,
                isStaff = isStaff,
                onEdit = { onEditSchedule(scheduleItem) },
                onDelete = { onDeleteSchedule(scheduleItem) },
                onChangeStatus = { onChangeScheduleStatus(scheduleItem) }
              )
            }
          }
        }
      }
    }
  }
}
