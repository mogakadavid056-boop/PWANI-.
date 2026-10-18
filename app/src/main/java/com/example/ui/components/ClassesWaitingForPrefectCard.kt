package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassWaitingForPrefect
import com.example.ui.theme.*

@Composable
fun ClassesWaitingForPrefectCard(
  classes: List<ClassWaitingForPrefect>,
  isLoading: Boolean,
  onAppointClick: (ClassWaitingForPrefect) -> Unit,
  modifier: Modifier = Modifier
) {
  ClassRepCard(modifier = modifier.fillMaxWidth().testTag("classes_waiting_for_prefect_card")) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.HourglassEmpty,
            contentDescription = null,
            tint = GoldLight,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Classes waiting for a prefect",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight,
            modifier = Modifier.testTag("classes_waiting_card_title")
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GoldPrimary)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .testTag("classes_waiting_count_badge")
        ) {
          Text(
            text = "${classes.size}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = DarkTextOnGold
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (isLoading && classes.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          LoadingSkeleton(height = 44)
          LoadingSkeleton(height = 44)
        }
      } else if (classes.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 12.dp, horizontal = 10.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Every class has a prefect",
            fontSize = 13.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.testTag("every_class_has_prefect_text")
          )
        }
      } else {
        val displayRows = classes.take(10)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          displayRows.forEachIndexed { index, item ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("waiting_prefect_row_${item.classId}"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                  text = item.className,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TextPrimary,
                  maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "${item.studentCount} students",
                  fontSize = 12.sp,
                  color = TextSecondary
                )
              }

              Button(
                onClick = { onAppointClick(item) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = GoldPrimary,
                  contentColor = DarkTextOnGold
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                  .height(34.dp)
                  .testTag("appoint_prefect_btn_${item.classId}")
              ) {
                Icon(
                  Icons.Default.PersonAdd,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                  tint = DarkTextOnGold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Appoint",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = DarkTextOnGold
                )
              }
            }
          }
        }
      }
    }
  }
}
