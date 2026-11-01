package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.People
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
import com.example.data.SearchClassItem
import com.example.ui.theme.*

@Composable
fun SearchClassCard(
  item: SearchClassItem,
  isClaiming: Boolean,
  onOpen: () -> Unit,
  onAdd: () -> Unit,
  modifier: Modifier = Modifier
) {
  ClassRepCard(
    modifier = modifier.testTag("search_class_card_${item.id}"),
    onClick = { if (item.isMine) onOpen() }
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.className,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          if (item.classCode.isNotBlank()) {
            Text(
              text = item.classCode,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = GoldLight
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (item.isMine) {
          Button(
            onClick = onOpen,
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp).testTag("search_class_open_btn_${item.id}")
          ) {
            Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        } else {
          OutlinedButton(
            onClick = onAdd,
            enabled = !isClaiming,
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp).testTag("search_class_add_btn_${item.id}")
          ) {
            if (isClaiming) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GoldPrimary, strokeWidth = 2.dp)
            } else {
              Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Programme and School
      if (item.programmeName.isNotBlank() || item.schoolName.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.School, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          val text = listOf(item.programmeName, item.schoolName).filter { it.isNotBlank() }.joinToString(" • ")
          Text(text = text, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
        }
        Spacer(modifier = Modifier.height(6.dp))
      }

      // Metadata Row: Students count and Prefect status
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.People, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${item.studentCount} students",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }

        if (!item.hasPrefect) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(androidx.compose.ui.graphics.Color(0xFF332306))
              .border(1.dp, androidx.compose.ui.graphics.Color(0xFFB27B17), RoundedCornerShape(6.dp))
              .padding(horizontal = 7.dp, vertical = 2.dp)
              .testTag("no_prefect_chip_${item.id}")
          ) {
            Text(
              text = "No prefect yet",
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = androidx.compose.ui.graphics.Color(0xFFFFC043)
            )
          }
        }
      }
    }
  }
}
