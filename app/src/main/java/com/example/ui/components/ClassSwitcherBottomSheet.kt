package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassItem
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassSwitcherBottomSheet(
  classes: List<ClassItem>,
  currentClassId: String,
  isAdmin: Boolean = false,
  isLecturer: Boolean = false,
  onSelectClass: (ClassItem) -> Unit,
  onJoinWithCode: () -> Unit = {},
  onCreateClass: () -> Unit = {},
  onFindMoreClasses: () -> Unit = {},
  onDismiss: () -> Unit
) {
  val isStaff = isAdmin || isLecturer
  var searchQuery by remember { mutableStateOf("") }
  val filteredClasses = remember(classes, searchQuery, isStaff) {
    if (isStaff && classes.size > 8 && searchQuery.isNotBlank()) {
      classes.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.code.contains(searchQuery, ignoreCase = true)
      }
    } else {
      classes
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = DarkCard,
    contentColor = TextPrimary,
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .testTag("class_switcher_bottom_sheet")
    ) {
      Text(
        text = if (isStaff) "My Classes" else "Switch Class",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = GoldLight,
        modifier = Modifier.padding(bottom = 12.dp)
      )

      if (isStaff && classes.size > 8) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search my classes...", color = TextSecondary, fontSize = 13.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
              }
            }
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("switcher_search_input"),
          shape = RoundedCornerShape(10.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = DarkBorder,
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )
        Spacer(modifier = Modifier.height(10.dp))
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 300.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(filteredClasses) { item ->
          val isCurrent = item.id == currentClassId
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(if (isCurrent) GoldPrimary.copy(alpha = 0.15f) else DarkSurface)
              .border(
                1.dp,
                if (isCurrent) GoldPrimary else DarkBorder,
                RoundedCornerShape(12.dp)
              )
              .clickable {
                onSelectClass(item)
                onDismiss()
              }
              .padding(14.dp)
              .testTag("switch_class_item_${item.id}")
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                  Icons.Default.Class,
                  contentDescription = null,
                  tint = if (isCurrent) GoldPrimary else TextSecondary,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrent) GoldLight else TextPrimary
                  )
                  if (item.code.isNotBlank()) {
                    Text(
                      text = item.code,
                      fontSize = 11.sp,
                      color = TextSecondary
                    )
                  }
                }
              }

              if (isCurrent) {
                Icon(
                  Icons.Default.Check,
                  contentDescription = "Active",
                  tint = GoldPrimary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(color = DarkBorder, thickness = 1.dp)
      Spacer(modifier = Modifier.height(12.dp))

      if (isStaff) {
        // Staff action: Find more classes
        Button(
          onClick = {
            onDismiss()
            onFindMoreClasses()
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("switcher_find_more_classes_button"),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = com.example.ui.theme.DarkTextOnGold)
        ) {
          Icon(Icons.Default.Search, contentDescription = null, tint = com.example.ui.theme.DarkTextOnGold, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Find more classes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.DarkTextOnGold)
        }

        if (isAdmin) {
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedButton(
            onClick = {
              onDismiss()
              onCreateClass()
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("switcher_create_class_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldPrimary),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create class", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
          }
        }
      } else {
        // Student: Join with code button
        OutlinedButton(
          onClick = {
            onDismiss()
            onJoinWithCode()
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("switcher_join_code_button"),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldPrimary),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary)
        ) {
          Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp), tint = GoldPrimary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Join with code", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
