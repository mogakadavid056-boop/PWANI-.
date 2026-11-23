package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PaperToMarkItem
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
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.SilverWhite
import com.example.ui.theme.SuccessColor

@Composable
fun PapersToMarkCard(
  papers: List<PaperToMarkItem>,
  isLoading: Boolean,
  onOpenExamMarking: (PaperToMarkItem) -> Unit,
  modifier: Modifier = Modifier
) {
  if (!isLoading && papers.isEmpty()) return

  val totalPending = papers.sumOf { it.waiting + it.inProgress }

  RoyalCard(
    borderColor = ElectricViolet.copy(alpha = 0.65f),
    modifier = modifier.fillMaxWidth().testTag("papers_to_mark_card")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(
          imageVector = Icons.Default.RateReview,
          contentDescription = null,
          tint = ChampagneGold,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Papers to Mark",
          style = SectionHeadingStyle.copy(fontSize = 18.sp),
          modifier = Modifier.testTag("papers_to_mark_title")
        )
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(MetallicGold)
          .padding(horizontal = 9.dp, vertical = 3.dp)
          .testTag("papers_to_mark_badge")
      ) {
        Text(
          text = "$totalPending",
          style = FieldLabelStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Midnight)
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    if (isLoading && papers.isEmpty()) {
      LoadingSkeleton(height = 56)
      LoadingSkeleton(height = 56)
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        papers.take(6).forEach { item ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(RoyalSurfaceHigh)
              .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
              .clickable { onOpenExamMarking(item) }
              .padding(horizontal = 12.dp, vertical = 10.dp)
              .testTag("paper_to_mark_row_${item.quizId}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
              Text(
                text = item.title,
                style = CardTitleStyle.copy(fontSize = 16.sp, color = SilverWhite),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = item.className,
                style = CaptionStyle.copy(color = ChampagneGold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Spacer(modifier = Modifier.height(3.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${item.waiting} waiting", style = CaptionStyle.copy(color = ElectricViolet))
                if (item.inProgress > 0) {
                  Text("${item.inProgress} in progress", style = CaptionStyle.copy(color = Lavender))
                }
                if (item.returned > 0) {
                  Text("${item.returned} returned", style = CaptionStyle.copy(color = SuccessColor))
                }
              }
            }

            Button(
              onClick = { onOpenExamMarking(item) },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("open_marking_btn_${item.quizId}")
            ) {
              Icon(Icons.Default.Draw, contentDescription = null, tint = Midnight, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Mark", style = ButtonTextStyle.copy(fontSize = 13.sp, color = Midnight))
            }
          }
        }
      }
    }
  }
}
