package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassItem
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ClassHeaderBar(
  classItem: ClassItem,
  isStaff: Boolean,
  onBack: () -> Unit,
  onEditClassNameClick: () -> Unit,
  onManageMembersClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current

  Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    IconButton(onClick = onBack, modifier = Modifier.size(40.dp).testTag("class_screen_back_button")) {
      Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
    }
    Spacer(modifier = Modifier.width(6.dp))
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = classItem.name.ifBlank { "Class Details" },
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = GoldLight,
          modifier = Modifier.testTag("class_screen_title")
        )
        if (isStaff) {
          Spacer(modifier = Modifier.width(4.dp))
          IconButton(onClick = onEditClassNameClick, modifier = Modifier.size(28.dp).testTag("edit_class_name_btn")) {
            Icon(Icons.Default.Edit, contentDescription = "Edit name", tint = GoldPrimary, modifier = Modifier.size(16.dp))
          }
        }
      }
      if (classItem.code.isNotBlank()) {
        Text("Code: ${classItem.code}", fontSize = 13.sp, color = TextSecondary)
      }
      // Only staff see the join_code (with a Copy button) in the class header
      if (isStaff && !classItem.joinCode.isNullOrBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Join Code: ${classItem.joinCode}",
            fontSize = 13.sp,
            color = GoldLight,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.width(4.dp))
          IconButton(
            onClick = {
              clipboardManager.setText(AnnotatedString(classItem.joinCode))
              Toast.makeText(context, "Join code copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(22.dp).testTag("copy_class_join_code_btn")
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Join Code", tint = GoldPrimary, modifier = Modifier.size(14.dp))
          }
        }
      }
    }
    if (isStaff) {
      IconButton(onClick = onManageMembersClick, modifier = Modifier.size(38.dp).testTag("manage_members_btn")) {
        Icon(Icons.Default.Group, contentDescription = "Manage Members", tint = GoldLight, modifier = Modifier.size(22.dp))
      }
    }
  }
}
