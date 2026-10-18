package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PendingLecturerItem
import com.example.ui.theme.*

@Composable
fun PendingLecturerRequestsSection(
  pendingLecturers: List<PendingLecturerItem>,
  onApprove: (PendingLecturerItem) -> Unit,
  onReject: (PendingLecturerItem) -> Unit,
  modifier: Modifier = Modifier
) {
  if (pendingLecturers.isEmpty()) return

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        Icons.Default.School,
        contentDescription = null,
        tint = GoldLight,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Lecturer Requests (${pendingLecturers.size})",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = GoldLight,
        modifier = Modifier.testTag("lecturer_requests_title")
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      pendingLecturers.forEach { lecturer ->
        ClassRepCard(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_lecturer_card_${lecturer.userId}")
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                  text = lecturer.fullName,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = lecturer.email,
                  fontSize = 12.sp,
                  color = TextSecondary
                )
                if (lecturer.staffNumber.isNotBlank()) {
                  Text(
                    text = "Staff No: ${lecturer.staffNumber}",
                    fontSize = 11.sp,
                    color = GoldLight
                  )
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                  onClick = { onReject(lecturer) },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  modifier = Modifier
                    .height(34.dp)
                    .testTag("reject_lecturer_btn_${lecturer.userId}")
                ) {
                  Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Reject", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                  onClick = { onApprove(lecturer) },
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = DarkTextOnGold
                  ),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  modifier = Modifier
                    .height(34.dp)
                    .testTag("approve_lecturer_btn_${lecturer.userId}")
                ) {
                  Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = DarkTextOnGold,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Approve", color = DarkTextOnGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}
