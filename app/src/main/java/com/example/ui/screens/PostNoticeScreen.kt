package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.draw.shadow
import com.example.ui.components.ErrorBanner
import com.example.ui.components.MetallicGoldText
import com.example.ui.components.PrimaryButton
import com.example.ui.components.RoyalCard
import com.example.ui.components.RoyalButtonGradientBrush
import com.example.ui.components.royalOutlinedTextFieldColors
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.Lavender
import com.example.ui.theme.Midnight
import com.example.ui.theme.MontserratFamily
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.ScreenTitleStyle
import com.example.ui.theme.SilverWhite
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
    modifier = Modifier.fillMaxSize().testTag("post_notice_screen")
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
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("post_notice_back_button")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          MetallicGoldText("Post Notice", style = ScreenTitleStyle, maxLines = 1)
          Text(className.ifBlank { "Class" }, style = CaptionStyle.copy(color = Lavender))
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      if (!errorMessage.isNullOrBlank()) {
        ErrorBanner(message = errorMessage, onDismiss = onDismissError)
        Spacer(modifier = Modifier.height(14.dp))
      }

      // Title input
      Text("Notice Title", style = FieldLabelStyle, modifier = Modifier.fillMaxWidth())
      Spacer(modifier = Modifier.height(6.dp))
      val titleInteraction = remember { MutableInteractionSource() }
      val titleFocused by titleInteraction.collectIsFocusedAsState()
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        interactionSource = titleInteraction,
        placeholder = { Text("e.g. Midterm exam timetable announced", style = CaptionStyle.copy(color = Lavender.copy(alpha = 0.7f))) },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .then(if (titleFocused) Modifier.shadow(10.dp, RoundedCornerShape(ButtonRadius), ambientColor = ElectricViolet, spotColor = ElectricViolet) else Modifier)
          .testTag("post_notice_title_input"),
        shape = RoundedCornerShape(ButtonRadius),
        colors = royalOutlinedTextFieldColors()
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Category Selector
      Text("Category", style = FieldLabelStyle, modifier = Modifier.fillMaxWidth())
      Spacer(modifier = Modifier.height(8.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { cat ->
          val isSelected = selectedCategory.equals(cat, ignoreCase = true)
          val chipModifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
              if (isSelected) Modifier.background(RoyalButtonGradientBrush)
              else Modifier.background(RoyalSurfaceHigh)
            )
            .border(1.dp, if (isSelected) ChampagneGold else DarkBorder, RoundedCornerShape(12.dp))
            .clickable { selectedCategory = cat }
            .padding(vertical = 12.dp)
            .testTag("category_chip_$cat")
          Box(
            modifier = chipModifier,
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = cat.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
              fontFamily = MontserratFamily,
              fontSize = 13.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
              color = if (isSelected) RoyalPurple else Lavender,
              maxLines = 1
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Content input
      Text("Notice Content", style = FieldLabelStyle, modifier = Modifier.fillMaxWidth())
      Spacer(modifier = Modifier.height(6.dp))
      val contentInteraction = remember { MutableInteractionSource() }
      val contentFocused by contentInteraction.collectIsFocusedAsState()
      OutlinedTextField(
        value = content,
        onValueChange = { content = it },
        interactionSource = contentInteraction,
        placeholder = { Text("Type all details, room numbers, instructions or dates...", style = CaptionStyle.copy(color = Lavender.copy(alpha = 0.7f))) },
        minLines = 4,
        maxLines = 8,
        modifier = Modifier
          .fillMaxWidth()
          .then(if (contentFocused) Modifier.shadow(10.dp, RoundedCornerShape(ButtonRadius), ambientColor = ElectricViolet, spotColor = ElectricViolet) else Modifier)
          .testTag("post_notice_content_input"),
        shape = RoundedCornerShape(ButtonRadius),
        colors = royalOutlinedTextFieldColors()
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
      val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsStateWithLifecycle()
      val context = androidx.compose.ui.platform.LocalContext.current
      PrimaryButton(
        text = "Post Notice",
        leadingIcon = Icons.Default.Campaign,
        onClick = {
          if (!isOnline) {
            com.example.ui.components.OfflineToastHelper.show(context)
          } else {
            onSubmit(title, content, selectedCategory, isUrgent, isPinned)
          }
        },
        enabled = if (!isOnline) true else (!isLoading && title.isNotBlank() && content.isNotBlank()),
        isLoading = isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .then(if (!isOnline) Modifier.alpha(0.5f) else Modifier)
          .testTag("post_notice_submit_button")
      )
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
  RoyalCard {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(icon, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(title, fontFamily = PlayfairDisplayFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SilverWhite)
          Text(subtitle, style = CaptionStyle.copy(color = Lavender))
        }
      }
      Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.testTag(testTag),
        colors = SwitchDefaults.colors(
          checkedThumbColor = Midnight,
          checkedTrackColor = GoldPrimary,
          uncheckedThumbColor = Lavender,
          uncheckedTrackColor = DarkSurfaceVariant
        )
      )
    }
  }
}
