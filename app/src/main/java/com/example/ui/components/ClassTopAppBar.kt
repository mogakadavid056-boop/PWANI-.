package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassItem
import com.example.ui.theme.*

@Composable
fun ClassTopAppBar(
  classItem: ClassItem,
  isStaff: Boolean,
  onBack: () -> Unit,
  onEditClassNameClick: () -> Unit = {},
  onOpenClassSwitcher: (() -> Unit)? = null,
  onNavigateToProfile: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(56.dp)
      .background(DarkSurface)
      .border(1.dp, DarkBorder)
      .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f)
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.size(40.dp).testTag("class_top_back_button")
      ) {
        Icon(
          Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = GoldLight,
          modifier = Modifier.size(22.dp)
        )
      }
      Spacer(modifier = Modifier.width(4.dp))

      // Class name with dropdown arrow for staff
      val clickableModifier = if (isStaff && onOpenClassSwitcher != null) {
        Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable { onOpenClassSwitcher() }
          .padding(horizontal = 4.dp, vertical = 4.dp)
          .testTag("class_top_switcher_trigger")
      } else {
        Modifier
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = clickableModifier
      ) {
        Text(
          text = classItem.name,
          style = TitleStyle.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
          color = GoldLight,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false).testTag("class_top_name_title")
        )
        if (isStaff && onOpenClassSwitcher != null) {
          Spacer(modifier = Modifier.width(2.dp))
          Icon(
            Icons.Default.ArrowDropDown,
            contentDescription = "Switch class",
            tint = GoldPrimary,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      if (isStaff) {
        IconButton(
          onClick = onEditClassNameClick,
          modifier = Modifier.size(38.dp).testTag("class_top_edit_button")
        ) {
          Icon(
            Icons.Default.Edit,
            contentDescription = "Edit Class Name",
            tint = GoldPrimary,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      // Avatar button on top right to reach Profile
      IconButton(
        onClick = onNavigateToProfile,
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(DarkSurfaceVariant)
          .border(1.dp, DarkBorder, CircleShape)
          .testTag("class_top_avatar_button")
      ) {
        Icon(
          Icons.Default.Person,
          contentDescription = "Profile",
          tint = GoldLight,
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}
