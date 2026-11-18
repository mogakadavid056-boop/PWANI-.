package com.example.admin.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.R
import com.example.admin.data.SupabaseAdminConfig
import com.example.admin.ui.components.AdminBackdrop
import com.example.admin.ui.components.AdminFeedbackBanner
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.AdminBorder
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.TextMuted
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary

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
        Image(
          painter = painterResource(id = R.drawable.admin_launcher_icon),
          contentDescription = "ClassRep Admin Crest",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(1.5.dp, GoldPrimary, RoundedCornerShape(22.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "ClassRep Admin",
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "University Governance & Oversight Console",
          fontSize = 14.sp,
          color = GoldLight,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        AdminStatusPill(text = "Shared DB: ${SupabaseAdminConfig.hostLabel}", color = EmeraldSuccess)

        Spacer(modifier = Modifier.height(24.dp))

        AdminGlassCard {
          Text(
            text = "Administrator Sign In",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Only accounts with Admin role in Supabase can access this app.",
            fontSize = 13.sp,
            color = TextMuted
          )

          if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            AdminFeedbackBanner(message = errorMessage, isError = true, onDismiss = onDismissError)
          }

          Spacer(modifier = Modifier.height(16.dp))

          OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Admin Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = GoldPrimary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().testTag("admin_login_email"),
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

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary) },
            trailingIcon = {
              IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                  imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (showPassword) "Hide password" else "Show password",
                  tint = TextSecondary
                )
              }
            },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().testTag("admin_login_password"),
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

          Spacer(modifier = Modifier.height(20.dp))

          Button(
            onClick = { onLogin(email, password) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("admin_login_submit"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
          ) {
            if (isLoading) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DarkTextOnGold, strokeWidth = 2.dp)
            } else {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign In as Admin", fontWeight = FontWeight.Bold, fontSize = 15.sp)
              }
            }
          }
        }
      }
    }
  }
}
