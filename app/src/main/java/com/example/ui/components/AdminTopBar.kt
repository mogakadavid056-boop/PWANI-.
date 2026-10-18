package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AdminTopBar(
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.testTag("admin_back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = GoldLight
        )
      }
      Spacer(modifier = Modifier.width(4.dp))
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Manage Users",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = GoldLight
          )
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = GoldPrimary,
            modifier = Modifier.size(16.dp)
          )
        }
        Text(
          text = "Super Admin Portal",
          fontSize = 12.sp,
          color = TextSecondary
        )
      }
    }

    IconButton(
      onClick = onRefresh,
      modifier = Modifier.testTag("admin_refresh_button")
    ) {
      Icon(
        imageVector = Icons.Default.Refresh,
        contentDescription = "Refresh Users",
        tint = GoldLight
      )
    }
  }
}
