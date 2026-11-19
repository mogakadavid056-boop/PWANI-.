package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ErrorBanner
import com.example.ui.components.GoldButton
import com.example.ui.components.GoldOutlinedButton
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CheckEmailScreen(
  email: String,
  isLoading: Boolean,
  errorMessage: String?,
  infoMessage: String?,
  onDismissError: () -> Unit,
  onResendEmail: () -> Unit,
  onGoToLogin: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
      .testTag("check_email_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(88.dp)
          .clip(CircleShape)
          .background(GoldPrimary.copy(alpha = 0.14f))
          .border(2.dp, GoldPrimary.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.MarkEmailRead,
          contentDescription = "Check your email",
          tint = GoldPrimary,
          modifier = Modifier.size(44.dp)
        )
      }

      Spacer(modifier = Modifier.height(22.dp))

      Text(
        text = "Check your email",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = GoldLight,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "We sent a confirmation link to $email. Open it, then log in.",
        fontSize = 15.sp,
        color = TextPrimary,
        textAlign = TextAlign.Center,
        lineHeight = 22.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Your profile and class setup will finish automatically right after your first login.",
        fontSize = 13.sp,
        color = TextSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 19.sp
      )

      if (!errorMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(18.dp))
        ErrorBanner(message = errorMessage, onDismiss = onDismissError)
      }

      if (!infoMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(18.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(SuccessContainer)
            .border(1.dp, SuccessGreen.copy(alpha = 0.6f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(12.dp)
        ) {
          Text(text = infoMessage, color = SuccessGreen, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      GoldButton(
        text = "Go to Login",
        onClick = onGoToLogin,
        testTag = "check_email_login_button"
      )

      Spacer(modifier = Modifier.height(14.dp))

      GoldOutlinedButton(
        text = if (isLoading) "Sending..." else "Resend email",
        onClick = { if (!isLoading) onResendEmail() },
        testTag = "check_email_resend_button"
      )
    }
  }
}
