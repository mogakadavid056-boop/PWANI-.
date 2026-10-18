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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ScheduleItem
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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
  onDeleteSchedule: (ScheduleItem) -> Unit
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
          style = SectionHeadingStyle,
          color = TextPrimary,
          modifier = Modifier.testTag("timetable_section_title")
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "(${schedules.size})", fontSize = 14.sp, color = TextSecondary)
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onRefresh,
          modifier = Modifier.size(36.dp).testTag("refresh_timetable_btn")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GoldLight, modifier = Modifier.size(20.dp))
        }

        if (isStaff) {
          Spacer(modifier = Modifier.width(6.dp))
          Button(
            onClick = onAddClassClick,
            modifier = Modifier.height(36.dp).testTag("add_class_schedule_btn"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = DarkTextOnGold)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Class", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Day filter pills
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      dayFilters.forEach { (d, label) ->
        val isSelected = selectedDay == d
        val isToday = d == todayDayOfWeek
        val pillText = if (isToday && d != 0) "$label •" else label

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) GoldPrimary else DarkCard)
            .border(
              width = if (isToday) 1.5.dp else 1.dp,
              color = if (isSelected) GoldPrimary else if (isToday) GoldPrimary else DarkBorder,
              shape = RoundedCornerShape(10.dp)
            )
            .clickable { onSelectDay(d) }
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("day_filter_$d"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = pillText,
            fontSize = 12.sp,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) DarkTextOnGold else if (isToday) GoldLight else TextSecondary
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

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Day header with Today highlighted in gold
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isToday) GoldPrimary.copy(alpha = 0.12f) else DarkSurface.copy(alpha = 0.4f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text(
                text = dayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isToday) GoldPrimary else TextSecondary
              )
              if (isToday) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(GoldPrimary)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text("TODAY", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = DarkTextOnGold)
                }
              }
            }

            dayList.forEach { scheduleItem ->
              ScheduleCard(
                schedule = scheduleItem,
                isStaff = isStaff,
                onEdit = { onEditSchedule(scheduleItem) },
                onDelete = { onDeleteSchedule(scheduleItem) }
              )
            }
          }
        }
      }
    }
  }
}
