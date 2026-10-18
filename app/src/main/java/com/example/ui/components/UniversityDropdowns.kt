package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun <T> AppDropdownField(
  label: String,
  selectedValueText: String,
  items: List<T>,
  itemText: (T) -> String,
  onItemSelected: (T) -> Unit,
  modifier: Modifier = Modifier,
  leadingIcon: ImageVector? = null,
  placeholder: String = "Select an option",
  testTag: String = "app_dropdown"
) {
  var expanded by remember { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = label,
      color = TextSecondary,
      fontSize = 13.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.padding(bottom = 6.dp)
    )

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(DarkSurfaceVariant)
        .border(1.dp, if (expanded) GoldPrimary else DarkBorder, RoundedCornerShape(12.dp))
        .clickable { expanded = true }
        .padding(horizontal = 14.dp, vertical = 14.dp)
        .testTag(testTag)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (leadingIcon != null) {
          Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = if (selectedValueText.isNotBlank()) GoldPrimary else TextSecondary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.size(10.dp))
        }

        Text(
          text = if (selectedValueText.isNotBlank()) selectedValueText else placeholder,
          color = if (selectedValueText.isNotBlank()) TextPrimary else TextSecondary,
          fontSize = 15.sp,
          modifier = Modifier.weight(1f)
        )

        Icon(
          imageVector = Icons.Default.ArrowDropDown,
          contentDescription = "Dropdown",
          tint = GoldLight
        )
      }

      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier
          .background(DarkSurface)
          .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
      ) {
        if (items.isEmpty()) {
          DropdownMenuItem(
            text = { Text("No options available", color = TextSecondary, fontSize = 14.sp) },
            onClick = { expanded = false },
            colors = MenuDefaults.itemColors()
          )
        } else {
          items.forEach { item ->
            DropdownMenuItem(
              text = {
                Text(
                  text = itemText(item),
                  color = if (itemText(item) == selectedValueText) GoldLight else TextPrimary,
                  fontWeight = if (itemText(item) == selectedValueText) FontWeight.Bold else FontWeight.Normal,
                  fontSize = 14.sp
                )
              },
              onClick = {
                onItemSelected(item)
                expanded = false
              },
              colors = MenuDefaults.itemColors()
            )
          }
        }
      }
    }
  }
}
