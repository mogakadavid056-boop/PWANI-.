package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CourseUnit
import com.example.data.NetworkMonitor
import com.example.ui.theme.*

@Composable
fun UnitsSectionCard(
  units: List<CourseUnit>,
  isStaff: Boolean,
  isLecturer: Boolean,
  isLoading: Boolean,
  onRegisterUnitClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val isOnline = NetworkMonitor.isOnline.value

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.MenuBook, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Units", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.testTag("units_section_title"))
        Spacer(modifier = Modifier.width(6.dp))
        Text("(${units.size})", fontSize = 13.sp, color = TextSecondary)
      }

      if (isLecturer) {
        Button(
          onClick = {
            if (!isOnline) OfflineToastHelper.show(context) else onRegisterUnitClick()
          },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
          modifier = Modifier.height(36.dp).testTag("register_unit_btn").then(if (!isOnline) Modifier.alpha(0.5f) else Modifier)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = DarkTextOnGold, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Register a unit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold)
        }
      }
    }

    if (isLoading && units.isEmpty()) {
      LoadingSkeleton(height = 68)
    } else if (units.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(DarkSurface)
          .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
          .padding(14.dp)
      ) {
        Text(
          text = if (isLecturer) "No units registered yet. Tap Register a unit to submit one for admin approval."
          else "No approved units in this class yet.",
          fontSize = 13.sp,
          color = TextSecondary
        )
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        units.forEach { unit ->
          UnitCardRow(unit = unit, showStatus = isStaff)
        }
      }
    }
  }
}

@Composable
private fun UnitCardRow(unit: CourseUnit, showStatus: Boolean) {
  val (statusBg, statusFg, statusText) = when {
    unit.isApproved -> Triple(SuccessGreen.copy(alpha = 0.16f), SuccessGreen, "Approved")
    unit.isPending -> Triple(Color(0x33FFB300), Color(0xFFFFB300), "Pending approval")
    else -> Triple(ErrorRed.copy(alpha = 0.16f), ErrorRed, "Rejected")
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(DarkCard)
      .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
      .padding(12.dp)
      .testTag("unit_card_${unit.code}")
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = unit.displayLabel,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = GoldLight,
          modifier = Modifier.weight(1f)
        )
        if (showStatus) {
          Spacer(modifier = Modifier.width(8.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(statusBg)
              .padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Text(statusText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusFg)
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (unit.lecturers.isNotBlank()) "Lecturer: ${unit.lecturers}" else "Lecturer: TBA",
          fontSize = 12.sp,
          color = TextSecondary,
          modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "${unit.materialCount} ${if (unit.materialCount == 1) "material" else "materials"}",
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          color = GoldPrimary
        )
      }

      if (showStatus && unit.isRejected && unit.rejectionReason.isNotBlank()) {
        Text(
          text = "Reason: ${unit.rejectionReason}",
          fontSize = 11.sp,
          color = ErrorRed
        )
      }
    }
  }
}
