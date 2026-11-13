package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary

@Composable
fun RegisterExportActionBar(
  onDownload: () -> Unit,
  onPrint: () -> Unit,
  onShare: () -> Unit
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(
      onClick = onDownload,
      modifier = Modifier.weight(1f).height(60.dp).testTag("download_register_pdf_btn"),
      colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
      shape = RoundedCornerShape(10.dp),
      contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Download, contentDescription = null, tint = DarkTextOnGold, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text("Download", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold, maxLines = 1, softWrap = false)
      }
    }

    OutlinedButton(
      onClick = onPrint,
      modifier = Modifier.weight(1f).height(60.dp).testTag("print_register_btn"),
      shape = RoundedCornerShape(10.dp),
      border = BorderStroke(1.5.dp, GoldPrimary),
      colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldLight),
      contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Print, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text("Print", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldLight, maxLines = 1, softWrap = false)
      }
    }

    OutlinedButton(
      onClick = onShare,
      modifier = Modifier.weight(1f).height(60.dp).testTag("share_register_btn"),
      shape = RoundedCornerShape(10.dp),
      border = BorderStroke(1.5.dp, GoldPrimary),
      colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldLight),
      contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Share, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text("Share", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldLight, maxLines = 1, softWrap = false)
      }
    }
  }
}
