package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.GoldAccent

object OfflineToastHelper {
  fun show(context: Context) {
    Toast.makeText(context, "You need internet to do this.", Toast.LENGTH_SHORT).show()
  }
}

@Composable
fun OfflineBanner(
  isOffline: Boolean,
  lastUpdatedText: String? = null,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = isOffline,
    enter = expandVertically(),
    exit = shrinkVertically()
  ) {
    Box(
      modifier = modifier
        .fillMaxWidth()
        .background(Color(0xFF261C08))
        .border(1.dp, DarkBorder)
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .testTag("offline_status_banner")
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.WifiOff,
          contentDescription = null,
          tint = GoldAccent,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (!lastUpdatedText.isNullOrBlank()) {
            "You are offline. Showing saved information. Last updated $lastUpdatedText"
          } else {
            "You are offline. Showing saved information."
          },
          color = GoldAccent,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}
