package com.example.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.DarkBorder
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
  var showPicker by remember { mutableStateOf(false) }

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
        .border(1.dp, if (showPicker) GoldPrimary else DarkBorder, RoundedCornerShape(12.dp))
        .clickable { showPicker = true }
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
    }
  }

  if (showPicker) {
    AppOptionPickerDialog(
      title = label.removeSuffix("*").trim(),
      items = items,
      itemText = itemText,
      selectedText = selectedValueText,
      onItemSelected = { item ->
        onItemSelected(item)
        showPicker = false
      },
      onDismiss = { showPicker = false }
    )
  }
}

@Composable
fun <T> AppOptionPickerDialog(
  title: String,
  items: List<T>,
  itemText: (T) -> String,
  selectedText: String,
  onItemSelected: (T) -> Unit,
  onDismiss: () -> Unit,
  emptyMessage: String = "No options available"
) {
  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(Color(0xFF0B2A1E))
        .border(1.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
        .padding(18.dp)
    ) {
      Text(
        text = title,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = GoldLight
      )
      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = DarkBorder)

      if (items.isEmpty()) {
        Text(
          text = emptyMessage,
          color = TextSecondary,
          fontSize = 14.sp,
          modifier = Modifier.padding(vertical = 20.dp)
        )
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .padding(vertical = 6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          items(items) { item ->
            val label = itemText(item)
            val isSelected = label == selectedText
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) GoldPrimary.copy(alpha = 0.14f) else Color.Transparent)
                .clickable { onItemSelected(item) }
                .padding(horizontal = 12.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = label,
                color = if (isSelected) GoldLight else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
              )
              if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Check, contentDescription = "Selected", tint = GoldPrimary, modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }

      HorizontalDivider(color = DarkBorder)
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.End
      ) {
        TextButton(onClick = onDismiss) {
          Text("Close", color = GoldPrimary, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}
