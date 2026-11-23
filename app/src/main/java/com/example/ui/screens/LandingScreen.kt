package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.data.SupabaseConfig
import com.example.ui.components.AppBrandHeader
import com.example.ui.components.GoldButton
import com.example.ui.components.GoldOutlinedButton
import com.example.ui.components.RoyalCard
import com.example.ui.components.RoyalDivider
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardTitleStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SectionHeadingStyle
import com.example.ui.theme.SubtitleItalicStyle
import com.example.ui.theme.SuccessGreen

@Composable
fun LandingScreen(
  onNavigateToLogin: () -> Unit,
  onNavigateToRegister: () -> Unit
) {
  val isConfigured = SupabaseConfig.isConfigured()

  Box(
    modifier = Modifier
      .fillMaxSize()
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

      AppBrandHeader()

      Spacer(modifier = Modifier.height(16.dp))

      if (isConfigured) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(RoyalSurfaceHigh)
            .border(1.dp, SuccessGreen.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
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
            style = FieldLabelStyle.copy(color = SuccessGreen)
          )
        }
      } else {
        RoyalCard(borderColor = MetallicGold.copy(alpha = 0.6f)) {
          Row(verticalAlignment = Alignment.Top) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = ChampagneGold,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Supabase Setup Required",
                style = FieldLabelStyle.copy(color = ChampagneGold)
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Configure SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets to connect live backend.",
                style = CaptionStyle
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      Text(
        text = "Unified Class Leadership & Student Sync",
        style = SectionHeadingStyle.copy(textAlign = TextAlign.Center)
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Empowering university class representatives and students with real-time academic coordination.",
        style = SubtitleItalicStyle.copy(textAlign = TextAlign.Center)
      )

      Spacer(modifier = Modifier.height(18.dp))
      RoyalDivider()
      Spacer(modifier = Modifier.height(18.dp))

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
        style = CaptionStyle.copy(color = Lavender)
      )

      Spacer(modifier = Modifier.height(24.dp))

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
  RoyalCard(onClick = onClick) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(RoyalSurfaceHigh)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ChampagneGold,
            modifier = Modifier.size(22.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
          Text(
            text = title,
            style = CardTitleStyle.copy(fontWeight = FontWeight.SemiBold)
          )
          Text(
            text = subtitle,
            style = CaptionStyle
          )
        }
      }

      Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = null,
        tint = Lavender,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}
