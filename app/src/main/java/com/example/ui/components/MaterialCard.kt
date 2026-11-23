package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FileCategory
import com.example.data.MaterialItem
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardTitleStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.SilverWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

fun getMaterialDisplayIcon(material: MaterialItem): Pair<ImageVector, Color> {
  if (material.isFile) {
    return when (material.fileCategory) {
      FileCategory.Pdf -> Icons.Default.PictureAsPdf to ErrorRed
      FileCategory.Word -> Icons.Default.Article to ElectricViolet
      FileCategory.PowerPoint -> Icons.Default.Slideshow to ChampagneGold
      FileCategory.Excel -> Icons.Default.TableChart to SuccessGreen
      FileCategory.Image -> Icons.Default.Image to Lavender
      FileCategory.Text -> Icons.Default.Description to MetallicGold
      FileCategory.Generic -> Icons.Default.AttachFile to GoldPrimary
    }
  }
  return when (material.materialType.lowercase()) {
    "slides" -> Icons.Default.Slideshow to ChampagneGold
    "video" -> Icons.Default.PlayCircle to ErrorRed
    "notes" -> Icons.Default.Description to GoldPrimary
    else -> Icons.Default.Link to ElectricViolet
  }
}

@Composable
fun MaterialCard(
  material: MaterialItem,
  isStaff: Boolean,
  isDownloaded: Boolean = false,
  isDownloading: Boolean = false,
  downloadProgress: Float? = null,
  onOpenFile: () -> Unit = {},
  onDownload: () -> Unit = {},
  onRemoveDownload: () -> Unit = {},
  onEdit: () -> Unit = {},
  onDelete: () -> Unit = {}
) {
  val context = LocalContext.current
  var showMenu by remember { mutableStateOf(false) }

  fun handleClick() {
    if (material.isFile) {
      onOpenFile()
    } else if (material.externalUrl.isNotBlank()) {
      try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(material.externalUrl))
        context.startActivity(intent)
      } catch (_: Exception) {}
    }
  }

  val (iconVector, iconTint) = getMaterialDisplayIcon(material)

  ClassRepCard(
    modifier = Modifier.testTag("material_card_${material.id}"),
    onClick = { handleClick() }
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(iconTint.copy(alpha = 0.15f))
              .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = iconVector,
              contentDescription = material.typeLabel,
              tint = iconTint,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = if (material.isFile) material.fileCategory.name.uppercase() else material.typeLabel.uppercase(),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = GoldLight,
              letterSpacing = 1.sp
            )
            if (material.formattedDate.isNotBlank()) {
              Text(
                text = material.formattedDate,
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isDownloading) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = GoldPrimary,
              strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
          } else {
            Icon(
              imageVector = if (material.isFile) Icons.Default.Download else Icons.AutoMirrored.Filled.OpenInNew,
              contentDescription = if (material.isFile) "Download file" else "Open link",
              tint = TextSecondary,
              modifier = Modifier.size(18.dp).padding(end = 4.dp)
            )
          }

          if (isStaff) {
            Box {
              IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(48.dp).testTag("material_more_btn_${material.id}")
              ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary, modifier = Modifier.size(18.dp))
              }
              if (showMenu) {
                val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
                androidx.compose.ui.window.Dialog(onDismissRequest = { showMenu = false }) {
                  Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                      .background(RoyalSurfaceHigh)
                      .border(1.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(18.dp)).padding(16.dp)
                  ) {
                    Text("Material Options", style = SectionHeadingStyle)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                      modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                        showMenu = false; if (!isOnline) OfflineToastHelper.show(context) else onEdit()
                      }.padding(vertical = 12.dp, horizontal = 8.dp).testTag("edit_material_btn_${material.id}"),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.Default.Edit, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(12.dp))
                      Text("Edit Material", color = TextPrimary, fontSize = 15.sp)
                    }
                    Row(
                      modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                        showMenu = false; if (!isOnline) OfflineToastHelper.show(context) else onDelete()
                      }.padding(vertical = 12.dp, horizontal = 8.dp).testTag("delete_material_btn_${material.id}"),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.width(12.dp))
                      Text("Delete Material", color = ErrorRed, fontSize = 15.sp)
                    }
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = material.title,
        style = CardTitleStyle.copy(color = SilverWhite),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      if (material.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = material.description,
          style = CaptionStyle,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      if (material.isFile) {
        Spacer(modifier = Modifier.height(10.dp))
        MaterialDownloadRow(
          material = material,
          isDownloaded = isDownloaded,
          isDownloading = isDownloading,
          downloadProgress = downloadProgress,
          onDownload = onDownload,
          onRemoveDownload = onRemoveDownload
        )
      }
    }
  }
}
