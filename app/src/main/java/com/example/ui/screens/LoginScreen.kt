package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DarkTextField
import com.example.ui.components.ErrorBanner
import com.example.ui.components.GoldButton
import com.example.ui.components.PasswordTextField
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LoginScreen(
  isLoading: Boolean,
  errorMessage: String?,
  onDismissError: () -> Unit,
  onLoginSubmit: (email: String, password: String) -> Unit,
  onNavigateToRegister: () -> Unit,
  onBack: () -> Unit
) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
      .testTag("login_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.size(40.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = GoldLight
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Sign In",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = GoldLight
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Welcome back to ClassRep",
        fontSize = 14.sp,
        color = TextSecondary
      )

      Spacer(modifier = Modifier.height(24.dp))

      if (!errorMessage.isNullOrBlank()) {
        ErrorBanner(
          message = errorMessage,
          onDismiss = onDismissError
        )
        Spacer(modifier = Modifier.height(16.dp))
      }

      DarkTextField(
        value = email,
        onValueChange = { email = it },
        label = "Email Address",
        leadingIcon = Icons.Default.Email,
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Email,
          imeAction = ImeAction.Next
        ),
        testTag = "login_email_input"
      )

      Spacer(modifier = Modifier.height(16.dp))

      PasswordTextField(
        value = password,
        onValueChange = { password = it },
        label = "Password",
        leadingIcon = Icons.Default.Lock,
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Password,
          imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
          onDone = {
            onLoginSubmit(email, password)
          }
        ),
        testTag = "login_password_input"
      )

      Spacer(modifier = Modifier.height(28.dp))

      GoldButton(
        text = "Sign In",
        onClick = {
          onLoginSubmit(email, password)
        },
        isLoading = isLoading,
        testTag = "login_submit_button"
      )

      Spacer(modifier = Modifier.height(14.dp))

      com.example.ui.components.GoldOutlinedButton(
        text = "Create Account",
        onClick = onNavigateToRegister,
        modifier = Modifier.fillMaxWidth().testTag("login_create_account_button")
      )

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "New student? ",
          fontSize = 13.sp,
          color = TextSecondary
        )
        Text(
          text = "Create Account",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = GoldPrimary,
          modifier = Modifier
            .clickable { onNavigateToRegister() }
            .testTag("login_to_register_link")
        )
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
