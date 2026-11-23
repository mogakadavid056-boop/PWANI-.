package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import com.example.ui.theme.*

@Composable
fun ProfileFieldCard(
  icon: ImageVector,
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  RoyalCard(modifier = modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(RoyalSurfaceHigh)
          .border(1.dp, MetallicGold.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(label, style = FieldLabelStyle.copy(color = Lavender, fontSize = 12.sp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = BodyStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium))
      }
    }
  }
}
