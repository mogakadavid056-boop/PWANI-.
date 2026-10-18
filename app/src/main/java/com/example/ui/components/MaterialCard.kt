package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MaterialItem
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.PoppinsFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

fun getMaterialTypeIcon(type: String): ImageVector {
  return when (type.lowercase()) {
    "notes" -> Icons.Default.Description
    "slides" -> Icons.Default.Slideshow
    "past_paper" -> Icons.AutoMirrored.Filled.Assignment
    "video" -> Icons.Default.PlayCircle
    "link" -> Icons.Default.Link
    else -> Icons.Default.Folder
  }
}

@Composable
fun MaterialCard(
  material: MaterialItem,
  isStaff: Boolean,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  val context = LocalContext.current
  var showMenu by remember { mutableStateOf(false) }

  fun openExternalUrl() {
    if (material.externalUrl.isBlank()) return
    try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(material.externalUrl))
      context.startActivity(intent)
    } catch (_: Exception) {}
  }

  ClassRepCard(
    modifier = Modifier.testTag("material_card_${material.id}"),
    onClick = { openExternalUrl() }
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
              .size(38.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = getMaterialTypeIcon(material.materialType),
              contentDescription = material.typeLabel,
              tint = GoldPrimary,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = material.typeLabel.uppercase(),
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
          Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Open link",
            tint = TextSecondary,
            modifier = Modifier.size(16.dp).padding(end = 4.dp)
          )
          if (isStaff) {
            Box {
              IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(28.dp).testTag("material_more_btn_${material.id}")
              ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary, modifier = Modifier.size(16.dp))
              }
              DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(DarkSurface).border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
              ) {
                DropdownMenuItem(
                  text = { Text("Edit Material", color = TextPrimary, fontSize = 13.sp) },
                  leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp)) },
                  onClick = { showMenu = false; onEdit() },
                  modifier = Modifier.testTag("edit_material_btn_${material.id}")
                )
                DropdownMenuItem(
                  text = { Text("Delete Material", color = ErrorRed, fontSize = 13.sp) },
                  leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp)) },
                  onClick = { showMenu = false; onDelete() },
                  modifier = Modifier.testTag("delete_material_btn_${material.id}")
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = material.title,
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = TextPrimary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      if (material.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = material.description,
          fontSize = 13.sp,
          color = TextSecondary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 18.sp
        )
      }
    }
  }
}
