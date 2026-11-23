package com.example.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.SupabaseAdminConfig
import com.example.admin.ui.components.AdminBackdrop
import com.example.admin.ui.components.AdminCrownIcon
import com.example.admin.ui.components.AdminFeedbackBanner
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminGoldButton
import com.example.admin.ui.components.AdminMetallicGoldText
import com.example.admin.ui.components.AdminRoyalDivider
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.components.royalAdminTextFieldColors
import com.example.admin.ui.theme.AdminCaptionStyle
import com.example.admin.ui.theme.AdminSectionHeadingStyle
import com.example.admin.ui.theme.AdminSubtitleItalicStyle
import com.example.admin.ui.theme.AdminWordmarkStyle
import com.example.admin.ui.theme.ChampagneGold
import com.example.admin.ui.theme.ElectricViolet
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldShadow
import com.example.admin.ui.theme.Lavender
import com.example.admin.ui.theme.MetallicGold
import com.example.admin.ui.theme.RoyalSurfaceHigh

@Composable
fun AdminLoginScreen(
  isLoading: Boolean,
  errorMessage: String?,
  onLogin: (String, String) -> Unit,
  onDismissError: () -> Unit
) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }

  AdminBackdrop {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Column(
        modifier = Modifier.widthIn(max = 460.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(RoyalSurfaceHigh)
            .border(2.dp, Brush.linearGradient(listOf(ChampagneGold, MetallicGold, GoldShadow)), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          AdminCrownIcon(size = 42.dp, tint = ChampagneGold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        AdminMetallicGoldText(
          text = "ClassRep Admin",
          style = AdminWordmarkStyle.copy(fontSize = 36.sp, lineHeight = 42.sp),
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "University Governance & Oversight Console",
          style = AdminSubtitleItalicStyle,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        AdminStatusPill(text = "Shared DB: ${SupabaseAdminConfig.hostLabel}", color = EmeraldSuccess)

        Spacer(modifier = Modifier.height(14.dp))
        AdminRoyalDivider()
        Spacer(modifier = Modifier.height(18.dp))

        AdminGlassCard(isHero = true) {
          Text(
            text = "Administrator Sign In",
            style = AdminSectionHeadingStyle
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Only accounts with Admin role in Supabase can access this app.",
            style = AdminCaptionStyle.copy(color = Lavender)
          )

          if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            AdminFeedbackBanner(message = errorMessage, isError = true, onDismiss = onDismissError)
          }

          Spacer(modifier = Modifier.height(16.dp))

          val emailInteraction = remember { MutableInteractionSource() }
          val emailFocused by emailInteraction.collectIsFocusedAsState()
          OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            interactionSource = emailInteraction,
            label = { Text("Admin Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = if (emailFocused) ChampagneGold else MetallicGold) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier
              .fillMaxWidth()
              .then(if (emailFocused) Modifier.shadow(10.dp, RoundedCornerShape(14.dp), ambientColor = ElectricViolet, spotColor = ElectricViolet) else Modifier)
              .testTag("admin_login_email"),
            shape = RoundedCornerShape(14.dp),
            colors = royalAdminTextFieldColors()
          )

          Spacer(modifier = Modifier.height(12.dp))

          val passInteraction = remember { MutableInteractionSource() }
          val passFocused by passInteraction.collectIsFocusedAsState()
          OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            interactionSource = passInteraction,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = if (passFocused) ChampagneGold else MetallicGold) },
            trailingIcon = {
              IconButton(onClick = { showPassword = !showPassword }, modifier = Modifier.size(48.dp)) {
                Icon(
                  imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (showPassword) "Hide password" else "Show password",
                  tint = Lavender
                )
              }
            },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier
              .fillMaxWidth()
              .then(if (passFocused) Modifier.shadow(10.dp, RoundedCornerShape(14.dp), ambientColor = ElectricViolet, spotColor = ElectricViolet) else Modifier)
              .testTag("admin_login_password"),
            shape = RoundedCornerShape(14.dp),
            colors = royalAdminTextFieldColors()
          )

          Spacer(modifier = Modifier.height(20.dp))

          AdminGoldButton(
            text = "Sign In as Admin",
            leadingIcon = Icons.Default.Shield,
            onClick = { onLogin(email, password) },
            enabled = !isLoading,
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth().testTag("admin_login_submit")
          )
        }
      }
    }
  }
}
