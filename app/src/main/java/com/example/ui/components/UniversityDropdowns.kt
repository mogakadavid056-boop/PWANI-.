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
import androidx.compose.foundation.layout.defaultMinSize
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CardRadius
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RoyalSurface
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.SilverWhite

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
      style = FieldLabelStyle,
      modifier = Modifier.padding(bottom = 6.dp)
    )

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 52.dp)
        .clip(RoundedCornerShape(ButtonRadius))
        .background(RoyalSurface)
        .border(1.dp, if (showPicker) ElectricViolet else DarkBorder, RoundedCornerShape(ButtonRadius))
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
            tint = if (selectedValueText.isNotBlank()) ChampagneGold else Lavender,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.size(10.dp))
        }

        Text(
          text = if (selectedValueText.isNotBlank()) selectedValueText else placeholder,
          style = com.example.ui.theme.BodyStyle.copy(
            color = if (selectedValueText.isNotBlank()) SilverWhite else Lavender.copy(alpha = 0.7f),
            fontSize = 15.sp
          ),
          modifier = Modifier.weight(1f)
        )

        Icon(
          imageVector = Icons.Default.ArrowDropDown,
          contentDescription = "Dropdown",
          tint = ChampagneGold
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
        .clip(RoundedCornerShape(CardRadius))
        .background(RoyalSurfaceHigh)
        .border(1.dp, RoyalCardBorderBrush, RoundedCornerShape(CardRadius))
        .padding(18.dp)
    ) {
      Text(
        text = title,
        style = SectionHeadingStyle
      )
      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = DarkBorder)

      if (items.isEmpty()) {
        Text(
          text = emptyMessage,
          style = com.example.ui.theme.SecondaryTextStyle,
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
                .background(if (isSelected) ElectricViolet.copy(alpha = 0.18f) else Color.Transparent)
                .clickable { onItemSelected(item) }
                .padding(horizontal = 12.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = label,
                style = com.example.ui.theme.BodyStyle.copy(
                  color = if (isSelected) ChampagneGold else SilverWhite,
                  fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                  fontSize = 15.sp
                ),
                modifier = Modifier.weight(1f)
              )
              if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Check, contentDescription = "Selected", tint = ChampagneGold, modifier = Modifier.size(18.dp))
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
        TextButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
          Text("Close", style = ButtonTextStyle.copy(color = ChampagneGold, fontSize = 14.sp))
        }
      }
    }
  }
}
