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
import com.example.ui.components.ClassRepCard
import com.example.ui.components.DarkTextField
import com.example.ui.components.FriendlyErrorSnackbar
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun WaitingScreen(
  isLoading: Boolean,
  errorMessage: String?,
  onDismissError: () -> Unit,
  onJoinWithCode: (String) -> Unit,
  onTryAutoJoin: () -> Unit,
  onNavigateToProfile: () -> Unit
) {
  var joinCode by remember { mutableStateOf("") }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
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
        Text(
          text = "ClassRep",
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          color = GoldLight
        )

        IconButton(
          onClick = onNavigateToProfile,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, CircleShape)
            .testTag("waiting_profile_avatar_button")
        ) {
          Icon(
            Icons.Default.Person,
            contentDescription = "Profile & Logout",
            tint = GoldLight,
            modifier = Modifier.size(24.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Status card
      Box(
        modifier = Modifier
          .size(80.dp)
          .clip(CircleShape)
          .background(GoldPrimary.copy(alpha = 0.12f))
          .border(1.5.dp, GoldPrimary.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.HourglassEmpty,
          contentDescription = null,
          tint = GoldPrimary,
          modifier = Modifier.size(40.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "You are not in a class yet",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("waiting_title_text")
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "We could not find your class. Check your programme and year in your Profile, or use a class code.",
        fontSize = 14.sp,
        color = TextSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
      )

      Spacer(modifier = Modifier.height(28.dp))

      if (!errorMessage.isNullOrBlank()) {
        FriendlyErrorSnackbar(errorMessage = errorMessage, onDismiss = onDismissError)
        Spacer(modifier = Modifier.height(16.dp))
      }

      // Join code card
      ClassRepCard {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Join with Class Code",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = GoldLight
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
          .height(48.dp)
          .testTag("waiting_try_again_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f))
      ) {
        if (isLoading) {
          CircularProgressIndicator(color = GoldPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
        } else {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = GoldPrimary)
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = "Try again (Auto-match my class)",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = GoldLight
        )
      }

      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}
