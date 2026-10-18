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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseConfig
import com.example.ui.components.AppBrandHeader
import com.example.ui.components.GoldButton
import com.example.ui.components.GoldOutlinedButton
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LandingScreen(
  onNavigateToLogin: () -> Unit,
  onNavigateToRegister: () -> Unit
) {
  val isConfigured = SupabaseConfig.isConfigured()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
      .testTag("landing_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      // App Header with Logo
      AppBrandHeader()

      Spacer(modifier = Modifier.height(16.dp))

      // Connection Status Badge
      if (isConfigured) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("landing_supabase_connected_badge")
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(SuccessGreen)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Connected to Supabase",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = SuccessGreen
          )
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, GoldDark, RoundedCornerShape(12.dp))
            .padding(12.dp)
        ) {
          Row(verticalAlignment = Alignment.Top) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = GoldLight,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Supabase Setup Required",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = GoldLight
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Configure SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets to connect live backend.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "Unified Class Leadership & Student Sync",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Empowering university class representatives and students with real-time academic coordination.",
        fontSize = 13.sp,
        color = TextSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp
      )

      Spacer(modifier = Modifier.height(24.dp))

      // PROMINENT, FRONT-AND-CENTER ACTION BUTTONS
      GoldButton(
        text = "Sign In",
        onClick = onNavigateToLogin,
        testTag = "landing_login_button"
      )

      Spacer(modifier = Modifier.height(12.dp))

      GoldOutlinedButton(
        text = "Create Account",
        onClick = onNavigateToRegister,
        testTag = "landing_register_button"
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "Protected with Supabase Authentication",
        fontSize = 12.sp,
        color = TextMuted
      )

      Spacer(modifier = Modifier.height(28.dp))

      // FEATURE HIGHLIGHT CARDS (CLICKABLE FOR EASY DISCOVERY)
      FeatureHighlightRow(
        icon = Icons.Default.School,
        title = "University Sync",
        subtitle = "Track academic schedules & rep notices",
        onClick = onNavigateToLogin
      )

      Spacer(modifier = Modifier.height(10.dp))

      FeatureHighlightRow(
        icon = Icons.Default.Groups,
        title = "Role-Based Access",
        subtitle = "Representatives & Students permissions",
        onClick = onNavigateToLogin
      )

      Spacer(modifier = Modifier.height(10.dp))

      FeatureHighlightRow(
        icon = Icons.Default.Campaign,
        title = "Direct Channels",
        subtitle = "Official announcements & course updates",
        onClick = onNavigateToLogin
      )

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun FeatureHighlightRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(DarkSurface)
      .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(DarkSurfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = GoldPrimary,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column {
        Text(
          text = title,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextPrimary
        )
        Text(
          text = subtitle,
          fontSize = 12.sp,
          color = TextSecondary
        )
      }
    }

    Icon(
      imageVector = Icons.Default.ChevronRight,
      contentDescription = null,
      tint = TextMuted,
      modifier = Modifier.size(18.dp)
    )
  }
}
