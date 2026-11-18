package com.example.admin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.ui.theme.AdminBorder
import com.example.admin.ui.theme.AdminCard
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.CrimsonContainer
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.EmeraldContainer
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.ObsidianBgBottom
import com.example.admin.ui.theme.ObsidianBgMid
import com.example.admin.ui.theme.ObsidianBgTop
import com.example.admin.ui.theme.TextMuted
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary

@Composable
fun AdminBackdrop(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(ObsidianBgTop, ObsidianBgMid, ObsidianBgBottom)
        )
      )
  ) {
    content()
  }
}

@Composable
fun AdminGlassCard(
  modifier: Modifier = Modifier,
  borderColor: Color = AdminBorder,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  val shape = RoundedCornerShape(16.dp)
  val base = modifier
    .fillMaxWidth()
    .clip(shape)
    .background(AdminCard)
    .border(1.dp, borderColor, shape)
    .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    .padding(16.dp)
  Column(modifier = base, content = content)
}

@Composable
fun KpiBentoCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null
) {
  AdminGlassCard(
    modifier = modifier,
    borderColor = accentColor.copy(alpha = 0.45f),
    onClick = onClick
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(accentColor.copy(alpha = 0.16f))
          .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(20.dp))
      }
      Text(
        text = value,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GoldLight)
    Text(text = subtitle, fontSize = 12.sp, color = TextMuted)
  }
}

@Composable
fun AdminStatusPill(
  text: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(color.copy(alpha = 0.15f))
      .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
      .padding(horizontal = 9.dp, vertical = 3.dp)
  ) {
    Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
  }
}

@Composable
fun AdminSearchField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  tag: String = "admin_search",
  modifier: Modifier = Modifier
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier.fillMaxWidth().testTag(tag),
    placeholder = { Text(placeholder, color = TextMuted, fontSize = 13.sp) },
    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) },
    trailingIcon = {
      if (value.isNotEmpty()) {
        IconButton(onClick = { onValueChange("") }) {
          Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
      }
    },
    singleLine = true,
    shape = RoundedCornerShape(12.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedContainerColor = AdminSurface,
      unfocusedContainerColor = AdminSurface,
      focusedBorderColor = GoldPrimary,
      unfocusedBorderColor = AdminBorder,
      focusedTextColor = TextPrimary,
      unfocusedTextColor = TextPrimary
    )
  )
}

@Composable
fun AdminFeedbackBanner(
  message: String,
  isError: Boolean,
  onDismiss: () -> Unit
) {
  val bg = if (isError) CrimsonContainer else EmeraldContainer
  val fg = if (isError) CrimsonError else EmeraldSuccess
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(bg)
      .border(1.dp, fg.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = message, color = fg, fontSize = 13.sp, modifier = Modifier.weight(1f))
    Spacer(modifier = Modifier.width(8.dp))
    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
      Icon(Icons.Default.Clear, contentDescription = "Dismiss", tint = fg, modifier = Modifier.size(16.dp))
    }
  }
}

@Composable
fun AdminEmptyState(
  icon: ImageVector,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier
) {
  AdminGlassCard(modifier = modifier) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(icon, contentDescription = null, tint = GoldPrimary.copy(alpha = 0.7f), modifier = Modifier.size(40.dp))
      Spacer(modifier = Modifier.height(10.dp))
      Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = GoldLight)
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = subtitle, fontSize = 13.sp, color = TextSecondary)
    }
  }
}
