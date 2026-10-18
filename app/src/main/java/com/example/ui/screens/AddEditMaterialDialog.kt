package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MaterialItem
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AddEditMaterialDialog(
  initialMaterial: MaterialItem? = null,
  isLoading: Boolean = false,
  onDismiss: () -> Unit,
  onSave: (title: String, description: String, materialType: String, externalUrl: String) -> Unit
) {
  val isEditing = initialMaterial != null

  var title by remember { mutableStateOf(initialMaterial?.title.orEmpty()) }
  var description by remember { mutableStateOf(initialMaterial?.description.orEmpty()) }
  var materialType by remember {
    mutableStateOf(initialMaterial?.materialType?.lowercase() ?: "notes")
  }
  var externalUrl by remember { mutableStateOf(initialMaterial?.externalUrl.orEmpty()) }
  var validationError by remember { mutableStateOf<String?>(null) }
  var typeDropdownExpanded by remember { mutableStateOf(false) }

  val typeOptions = listOf(
    "notes" to "Notes",
    "slides" to "Slides",
    "past_paper" to "Past Paper",
    "video" to "Video",
    "link" to "Link"
  )
  val selectedTypeLabel = typeOptions.firstOrNull { it.first == materialType }?.second ?: "Notes"

  val fieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GoldPrimary, unfocusedBorderColor = DarkBorder,
    focusedLabelColor = GoldPrimary, unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedContainerColor = DarkSurfaceVariant, unfocusedContainerColor = DarkSurfaceVariant
  )

  Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(DarkSurface)
        .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
        .padding(20.dp)
        .verticalScroll(rememberScrollState())
        .testTag("add_edit_material_dialog")
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Description, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isEditing) "Edit Material" else "Add Learning Material",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = title,
        onValueChange = { title = it; validationError = null },
        label = { Text("Title") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("material_title_input"),
        colors = fieldColors
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text("Material Type", fontSize = 12.sp, color = TextSecondary)
      Spacer(modifier = Modifier.height(4.dp))
      Box(modifier = Modifier.fillMaxWidth()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .clickable { typeDropdownExpanded = true }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("material_type_dropdown"),
          contentAlignment = Alignment.CenterStart
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(selectedTypeLabel, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Type", tint = GoldLight)
          }
        }

        DropdownMenu(
          expanded = typeDropdownExpanded,
          onDismissRequest = { typeDropdownExpanded = false },
          modifier = Modifier.background(DarkSurface).border(1.dp, DarkBorder)
        ) {
          typeOptions.forEach { (value, label) ->
            DropdownMenuItem(
              text = {
                Text(
                  label,
                  color = if (value == materialType) GoldPrimary else TextPrimary,
                  fontWeight = if (value == materialType) FontWeight.Bold else FontWeight.Normal
                )
              },
              onClick = {
                materialType = value
                typeDropdownExpanded = false
              },
              modifier = Modifier.testTag("type_option_$value")
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = externalUrl,
        onValueChange = { externalUrl = it; validationError = null },
        label = { Text("Resource URL (starts with http:// or https://)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("material_url_input"),
        colors = fieldColors
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Description (optional)") },
        maxLines = 4,
        modifier = Modifier.fillMaxWidth().testTag("material_desc_input"),
        colors = fieldColors
      )

      if (validationError != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(validationError!!, color = ErrorRed, fontSize = 12.sp)
      }

      Spacer(modifier = Modifier.height(18.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = onDismiss,
          enabled = !isLoading,
          modifier = Modifier.weight(1f).height(48.dp).testTag("material_cancel_button"),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkBorder),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = TextPrimary)
        ) {
          Text("Cancel", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Button(
          onClick = {
            val cleanTitle = title.trim()
            val cleanUrl = externalUrl.trim()
            if (cleanTitle.isBlank()) {
              validationError = "Please enter a material title."
              return@Button
            }
            if (!cleanUrl.startsWith("http://", ignoreCase = true) && !cleanUrl.startsWith("https://", ignoreCase = true)) {
              validationError = "URL must start with http:// or https://"
              return@Button
            }
            onSave(cleanTitle, description.trim(), materialType, cleanUrl)
          },
          enabled = !isLoading,
          modifier = Modifier.weight(1f).height(48.dp).testTag("material_submit_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = com.example.ui.theme.DarkTextOnGold)
        ) {
          if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = com.example.ui.theme.DarkTextOnGold, strokeWidth = 2.dp)
          } else {
            Text(if (isEditing) "Save" else "Add", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = com.example.ui.theme.DarkTextOnGold)
          }
        }
      }
    }
  }
}
