package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeIdentityHeader(
  displayName: String,
  displayRole: String,
  onNavigateToProfile: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.clickable(onClick = onNavigateToProfile).testTag("home_profile_header")
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(DarkSurfaceVariant)
          .border(1.5.dp, GoldPrimary, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_classrep_logo),
          contentDescription = "ClassRep Logo",
          modifier = Modifier.size(32.dp).clip(CircleShape)
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text("ClassRep", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GoldLight)
        Text(displayName, fontSize = 12.sp, color = TextSecondary, maxLines = 1)
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(
        onClick = onNavigateToProfile,
        modifier = Modifier.size(36.dp).testTag("home_profile_button")
      ) {
        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = GoldLight, modifier = Modifier.size(24.dp))
      }
      Spacer(modifier = Modifier.width(4.dp))
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(16.dp))
          .background(DarkSurfaceVariant)
          .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
          .padding(horizontal = 10.dp, vertical = 4.dp)
          .testTag("home_user_role")
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Stars, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(displayRole, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldLight)
        }
      }
    }
  }
}
