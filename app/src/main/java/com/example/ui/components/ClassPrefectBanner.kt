package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassPrefectInfo
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary

@Composable
fun ClassPrefectBanner(
  prefects: List<ClassPrefectInfo>,
  modifier: Modifier = Modifier
) {
  if (prefects.isNotEmpty()) {
    val prefectNames = when (prefects.size) {
      1 -> prefects[0].fullName
      2 -> "${prefects[0].fullName} and ${prefects[1].fullName}"
      else -> prefects.take(2).joinToString(" and ") { it.fullName }
    }
    Box(
      modifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(DarkSurface)
        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
        .padding(horizontal = 14.dp, vertical = 10.dp)
        .testTag("class_prefect_banner")
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(GoldPrimary.copy(alpha = 0.15f))
            .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = GoldPrimary,
            modifier = Modifier.size(16.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Class Prefect: $prefectNames",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight,
            modifier = Modifier.testTag("class_prefect_names_text")
          )
        }
      }
    }
  } else {
    Box(
      modifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(DarkSurface)
        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
        .padding(14.dp)
        .testTag("class_no_prefect_banner")
    ) {
      Row(verticalAlignment = Alignment.Top) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(GoldPrimary.copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = GoldLight,
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Your class prefect has not been appointed yet. Your admin or lecturer will appoint one. Notices, the timetable and other updates will appear here once your class is updated.",
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White,
            lineHeight = 19.sp,
            modifier = Modifier.testTag("no_prefect_banner_text")
          )
        }
      }
    }
  }
}
