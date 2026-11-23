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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.defaultMinSize
import com.example.ui.components.ClassRepCard
import com.example.ui.components.DarkTextField
import com.example.ui.components.FriendlyErrorSnackbar
import com.example.ui.components.MetallicGoldText
import com.example.ui.components.PrimaryButton
import com.example.ui.components.RoyalDivider
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CardTitleStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.ScreenTitleStyle
import com.example.ui.theme.SecondaryTextStyle
import com.example.ui.theme.SectionHeadingStyle

@Composable
fun WaitingScreen(
  isLoading: Boolean,
  errorMessage: String?,
  unreadNotificationCount: Int = 0,
  onOpenNotifications: () -> Unit = {},
  onDismissError: () -> Unit,
  onJoinWithCode: (String) -> Unit,
  onTryAutoJoin: () -> Unit,
  onNavigateToProfile: () -> Unit,
  onContinueHome: () -> Unit = {}
) {
  var joinCode by remember { mutableStateOf("") }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .testTag("waiting_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top bar with Avatar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        MetallicGoldText(
          text = "ClassRep",
          style = ScreenTitleStyle
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          com.example.ui.components.NotificationBellButton(
            unreadCount = unreadNotificationCount,
            onClick = onOpenNotifications
          )
          IconButton(
            onClick = onNavigateToProfile,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(RoyalSurfaceHigh)
              .border(1.dp, DarkBorder, CircleShape)
              .testTag("waiting_profile_avatar_button")
          ) {
            Icon(
              Icons.Default.Person,
              contentDescription = "Profile & Logout",
              tint = ChampagneGold,
              modifier = Modifier.size(24.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Status icon
      Box(
        modifier = Modifier
          .size(80.dp)
          .clip(CircleShape)
          .background(MetallicGold.copy(alpha = 0.14f))
          .border(1.5.dp, MetallicGold.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.HourglassEmpty,
          contentDescription = null,
          tint = ChampagneGold,
          modifier = Modifier.size(40.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "You are not in a class yet",
        style = SectionHeadingStyle.copy(fontSize = 22.sp),
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("waiting_title_text")
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "We could not find your class. Check your programme and year in your Profile, or use a class code.",
        style = SecondaryTextStyle,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(16.dp))
      RoyalDivider()
      Spacer(modifier = Modifier.height(20.dp))

      if (!errorMessage.isNullOrBlank()) {
        FriendlyErrorSnackbar(errorMessage = errorMessage, onDismiss = onDismissError)
        Spacer(modifier = Modifier.height(16.dp))
      }

      // Join code card
      ClassRepCard {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Join with Class Code",
            style = CardTitleStyle.copy(color = ChampagneGold)
          )
          Spacer(modifier = Modifier.height(12.dp))

          DarkTextField(
            value = joinCode,
            onValueChange = { joinCode = it.uppercase() },
            label = "Enter Class Join Code",
            leadingIcon = Icons.Default.Key,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
            testTag = "waiting_join_code_input"
          )

          Spacer(modifier = Modifier.height(16.dp))

          PrimaryButton(
            text = "Join Class",
            onClick = { onJoinWithCode(joinCode) },
            isLoading = isLoading,
            enabled = joinCode.isNotBlank() && !isLoading,
            modifier = Modifier.testTag("waiting_join_code_button")
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Try again button
      OutlinedButton(
        onClick = onTryAutoJoin,
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 52.dp)
          .testTag("waiting_try_again_button"),
        shape = RoundedCornerShape(ButtonRadius),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampagneGold),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MetallicGold.copy(alpha = 0.65f))
      ) {
        if (isLoading) {
          CircularProgressIndicator(color = ChampagneGold, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
        } else {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = ChampagneGold)
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = "Try again (Auto-match my class)",
          style = ButtonTextStyle.copy(color = ChampagneGold, fontSize = 14.sp)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      androidx.compose.material3.TextButton(
        onClick = onContinueHome,
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 48.dp)
          .testTag("waiting_continue_home_button")
      ) {
        Text(
          text = "Go to Home Dashboard",
          style = ButtonTextStyle.copy(color = ChampagneGold, fontSize = 14.sp)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
