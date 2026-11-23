package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PendingLecturerItem
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardTitleStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.SilverWhite

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
        tint = ChampagneGold,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Lecturer Requests (${pendingLecturers.size})",
        style = SectionHeadingStyle.copy(color = ChampagneGold),
        modifier = Modifier.testTag("lecturer_requests_title")
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      pendingLecturers.forEach { lecturer ->
        RoyalCard(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_lecturer_card_${lecturer.userId}")
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = lecturer.fullName,
                style = CardTitleStyle.copy(color = SilverWhite)
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = lecturer.email,
                style = CaptionStyle.copy(color = Lavender)
              )
              if (lecturer.staffNumber.isNotBlank()) {
                Text(
                  text = "Staff No: ${lecturer.staffNumber}",
                  style = CaptionStyle.copy(color = ChampagneGold)
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              OutlinedButton(
                onClick = { onReject(lecturer) },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                  .height(38.dp)
                  .testTag("reject_lecturer_btn_${lecturer.userId}")
              ) {
                Icon(
                  Icons.Default.Close,
                  contentDescription = null,
                  tint = ErrorRed,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reject", style = ButtonTextStyle.copy(color = ErrorRed, fontSize = 12.sp))
              }

              Spacer(modifier = Modifier.width(8.dp))

              Button(
                onClick = { onApprove(lecturer) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MetallicGold,
                  contentColor = Midnight
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                  .height(38.dp)
                  .testTag("approve_lecturer_btn_${lecturer.userId}")
              ) {
                Icon(
                  Icons.Default.Check,
                  contentDescription = null,
                  tint = Midnight,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Approve", style = ButtonTextStyle.copy(color = Midnight, fontSize = 12.sp))
              }
            }
          }
        }
      }
    }
  }
}
