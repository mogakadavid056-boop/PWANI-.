package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.UploadFile
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
import com.example.data.formatFileSize
import com.example.ui.theme.*

fun queryFileInfo(context: Context, uri: Uri): Pair<String, Long> {
  var name = "file"
  var size = 0L
  try {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
      if (cursor.moveToFirst()) {
        if (nameIdx != -1) name = cursor.getString(nameIdx) ?: "file"
        if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
      }
    }
  } catch (_: Exception) {}
  return name to size
}

@Composable
fun MaterialModeSelector(
  isFileUploadMode: Boolean,
  onSelectLink: () -> Unit,
  onSelectUpload: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(DarkSurfaceVariant)
      .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
      .padding(3.dp)
  ) {
    Box(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(8.dp))
        .background(if (!isFileUploadMode) GoldPrimary else DarkSurfaceVariant)
        .clickable { onSelectLink() }
        .padding(vertical = 8.dp)
        .testTag("material_choice_link"),
      contentAlignment = Alignment.Center
    ) {
      Text("Link", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (!isFileUploadMode) DarkTextOnGold else TextSecondary)
    }
    Box(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(8.dp))
        .background(if (isFileUploadMode) GoldPrimary else DarkSurfaceVariant)
        .clickable { onSelectUpload() }
        .padding(vertical = 8.dp)
        .testTag("material_choice_upload"),
      contentAlignment = Alignment.Center
    ) {
      Text("Upload file", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isFileUploadMode) DarkTextOnGold else TextSecondary)
    }
  }
}

@Composable
fun MaterialFilePickerBox(
  selectedUri: Uri?,
  fileName: String,
  fileSize: Long,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(DarkSurfaceVariant)
      .border(1.dp, if (selectedUri != null) GoldPrimary else DarkBorder, RoundedCornerShape(10.dp))
      .clickable { onClick() }
      .padding(14.dp)
      .testTag("material_file_picker_btn")
  ) {
    if (selectedUri == null) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.UploadFile, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text("Select file to upload", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
          Text("PDF, Word, PPT, Excel, TXT, images (Max 15 MB)", fontSize = 11.sp, color = TextSecondary)
        }
      }
    } else {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Default.AttachFile, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(fileName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatFileSize(fileSize), fontSize = 11.sp, color = GoldLight)
          }
        }
        Text("Change", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary, modifier = Modifier.padding(start = 8.dp))
      }
    }
  }
}
