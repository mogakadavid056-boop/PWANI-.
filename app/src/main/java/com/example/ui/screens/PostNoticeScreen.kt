package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.ui.components.ErrorBanner
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun PostNoticeScreen(
  className: String,
  isLoading: Boolean,
  errorMessage: String?,
  onDismissError: () -> Unit,
  onSubmit: (title: String, content: String, category: String, urgent: Boolean, pinned: Boolean) -> Unit,
  onBack: () -> Unit
) {
  var title by remember { mutableStateOf("") }
  var content by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("general") }
  var isUrgent by remember { mutableStateOf(false) }
  var isPinned by remember { mutableStateOf(false) }

  val categories = listOf("general", "exam", "assignment", "event")

  Box(
    modifier = Modifier.fillMaxSize().background(DarkBackground).testTag("post_notice_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Header
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp).testTag("post_notice_back_button")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text("Post Notice", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GoldLight)
          Text(className.ifBlank { "Class" }, fontSize = 12.sp, color = TextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      if (!errorMessage.isNullOrBlank()) {
        ErrorBanner(message = errorMessage, onDismiss = onDismissError)
        Spacer(modifier = Modifier.height(14.dp))
      }

      // Title input
      Text("Notice Title", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.fillMaxWidth())
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        placeholder = { Text("e.g. Midterm exam timetable announced", color = TextSecondary, fontSize = 14.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("post_notice_title_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = DarkSurface,
          unfocusedContainerColor = DarkSurface,
          focusedBorderColor = GoldPrimary,
          unfocusedBorderColor = DarkBorder,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        )
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Category Selector
      Text("Category", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.fillMaxWidth())
      Spacer(modifier = Modifier.height(8.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { cat ->
          val isSelected = selectedCategory.equals(cat, ignoreCase = true)
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) GoldPrimary else DarkSurface)
              .border(1.dp, if (isSelected) GoldPrimary else DarkBorder, RoundedCornerShape(10.dp))
              .clickable { selectedCategory = cat }
              .padding(vertical = 10.dp)
              .testTag("category_chip_$cat"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = cat.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) DarkTextOnGold else TextSecondary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Content input
      Text("Notice Content", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.fillMaxWidth())
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = content,
        onValueChange = { content = it },
        placeholder = { Text("Type all details, room numbers, instructions or dates...", color = TextSecondary, fontSize = 14.sp) },
        minLines = 4,
        maxLines = 8,
        modifier = Modifier.fillMaxWidth().testTag("post_notice_content_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = DarkSurface,
          unfocusedContainerColor = DarkSurface,
          focusedBorderColor = GoldPrimary,
          unfocusedBorderColor = DarkBorder,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        )
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Switches
      NoticeToggleRow(
        title = "Mark as Urgent",
        subtitle = "Displays high priority red badge",
        icon = Icons.Default.Warning,
        checked = isUrgent,
        onCheckedChange = { isUrgent = it },
        testTag = "post_notice_urgent_switch"
      )

      Spacer(modifier = Modifier.height(10.dp))

      NoticeToggleRow(
        title = "Pin to Top",
        subtitle = "Keeps announcement at the top of list",
        icon = Icons.Default.PushPin,
        checked = isPinned,
        onCheckedChange = { isPinned = it },
        testTag = "post_notice_pinned_switch"
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Submit Button
      Button(
        onClick = { onSubmit(title, content, selectedCategory, isUrgent, isPinned) },
        enabled = !isLoading && title.isNotBlank() && content.isNotBlank(),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("post_notice_submit_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = GoldPrimary,
          contentColor = com.example.ui.theme.DarkTextOnGold,
          disabledContainerColor = GoldDark.copy(alpha = 0.4f),
          disabledContentColor = com.example.ui.theme.DarkTextOnGold.copy(alpha = 0.5f)
        )
      ) {
        if (isLoading) {
          CircularProgressIndicator(color = com.example.ui.theme.DarkTextOnGold, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
          Icon(Icons.Default.Campaign, contentDescription = null, tint = com.example.ui.theme.DarkTextOnGold, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Post Notice", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.DarkTextOnGold)
        }
      }
    }
  }
}

@Composable
private fun NoticeToggleRow(
  title: String,
  subtitle: String,
  icon: ImageVector,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(DarkSurface)
      .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
      .padding(horizontal = 14.dp, vertical = 10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = GoldLight, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
          Text(subtitle, fontSize = 13.sp, color = TextSecondary)
        }
      }
      Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.testTag(testTag),
        colors = SwitchDefaults.colors(
          checkedThumbColor = com.example.ui.theme.DarkTextOnGold,
          checkedTrackColor = GoldPrimary,
          uncheckedThumbColor = TextSecondary,
          uncheckedTrackColor = DarkSurfaceVariant
        )
      )
    }
  }
}
