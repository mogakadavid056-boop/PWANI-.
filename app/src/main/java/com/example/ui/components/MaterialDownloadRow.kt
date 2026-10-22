package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MaterialItem
import com.example.ui.theme.*

@Composable
fun MaterialDownloadRow(
  material: MaterialItem,
  isDownloaded: Boolean,
  isDownloading: Boolean,
  downloadProgress: Float?,
  onDownload: () -> Unit,
  onRemoveDownload: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(DarkSurfaceVariant)
      .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
      .padding(horizontal = 10.dp, vertical = 8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(Icons.Default.AttachFile, contentDescription = null, tint = GoldLight, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = material.fileName,
          fontSize = 12.sp,
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = material.formattedFileSize,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = GoldPrimary
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      if (isDownloading) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = GoldPrimary
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Downloading ${(downloadProgress?.times(100))?.toInt() ?: 0}%...",
            fontSize = 11.sp,
            color = GoldLight
          )
        }
      } else if (isDownloaded) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Saved on phone",
            tint = SuccessGreen,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Saved on phone",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = SuccessGreen
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clickable { onRemoveDownload() }
            .padding(vertical = 2.dp, horizontal = 4.dp)
            .testTag("remove_download_${material.id}")
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Remove", fontSize = 11.sp, color = TextSecondary)
        }
      } else {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(GoldPrimary.copy(alpha = 0.15f))
            .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .clickable { onDownload() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("download_btn_${material.id}")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Download, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Download", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldLight)
          }
        }
      }
    }

    if (isDownloading && downloadProgress != null && downloadProgress > 0f) {
      Spacer(modifier = Modifier.height(6.dp))
      LinearProgressIndicator(
        progress = { downloadProgress },
        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
        color = GoldPrimary,
        trackColor = DarkBorder
      )
    }
  }
}
