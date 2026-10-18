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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardRadius
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ClassMoreMenu(
  isStaff: Boolean,
  materialsCount: Int,
  pollsCount: Int,
  onOpenMaterials: () -> Unit,
  onOpenPolls: () -> Unit,
  onOpenManageMembers: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.fillMaxWidth().testTag("class_more_menu"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      text = "Additional Features",
      style = SectionHeadingStyle,
      color = GoldLight,
      modifier = Modifier.padding(bottom = 4.dp)
    )

    MoreMenuItem(
      title = "Course Materials",
      subtitle = "$materialsCount study files & resources",
      icon = Icons.Default.Description,
      onClick = onOpenMaterials,
      tag = "more_item_materials"
    )

    MoreMenuItem(
      title = "Class Polls",
      subtitle = "$pollsCount student voting polls",
      icon = Icons.Default.Poll,
      onClick = onOpenPolls,
      tag = "more_item_polls"
    )

    if (isStaff) {
      MoreMenuItem(
        title = "Manage Members",
        subtitle = "Class rosters, lecturers & Class Prefects",
        icon = Icons.Default.People,
        onClick = onOpenManageMembers,
        tag = "more_item_manage_members"
      )
    }

    // Welfare (Coming soon)
    MoreMenuItem(
      title = "Welfare",
      subtitle = "Class support & welfare services (Coming soon)",
      icon = Icons.Default.Favorite,
      onClick = {},
      enabled = false,
      tag = "more_item_welfare"
    )
  }
}

@Composable
private fun MoreMenuItem(
  title: String,
  subtitle: String,
  icon: ImageVector,
  onClick: () -> Unit,
  enabled: Boolean = true,
  tag: String
) {
  val shape = RoundedCornerShape(CardRadius)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .background(DarkCard)
      .border(1.dp, DarkBorder, shape)
      .clickable(enabled = enabled) { onClick() }
      .padding(16.dp)
      .testTag(tag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f)
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(DarkSurfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          icon,
          contentDescription = null,
          tint = if (enabled) GoldPrimary else TextSecondary.copy(alpha = 0.5f),
          modifier = Modifier.size(24.dp)
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column {
        Text(
          text = title,
          style = BodyStyle.copy(fontWeight = FontWeight.SemiBold),
          color = if (enabled) TextPrimary else TextSecondary
        )
        Text(
          text = subtitle,
          style = CaptionStyle.copy(color = TextSecondary, fontSize = 12.sp)
        )
      }
    }

    if (enabled) {
      Icon(
        Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = null,
        tint = TextSecondary,
        modifier = Modifier.size(14.dp)
      )
    }
  }
}
