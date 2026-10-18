package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Key
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DarkTextField
import com.example.ui.components.ErrorBanner
import com.example.ui.components.GoldButton
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GoldLight
import com.example.ui.theme.TextSecondary

@Composable
fun JoinClassScreen(
  isLoading: Boolean,
  errorMessage: String?,
  onDismissError: () -> Unit,
  onJoinSubmit: (code: String) -> Unit,
  onBack: () -> Unit
) {
  var joinCode by remember { mutableStateOf("") }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
      .testTag("join_class_screen")
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
          text = "Join Class",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = GoldLight
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Enter the class join code provided by your class prefect or instructor.",
        fontSize = 14.sp,
        color = TextSecondary,
        lineHeight = 20.sp
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
        value = joinCode,
        onValueChange = { joinCode = it },
        label = "Class Join Code",
        leadingIcon = Icons.Default.Key,
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.Characters,
          imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
          onDone = {
            if (joinCode.isNotBlank()) onJoinSubmit(joinCode)
          }
        ),
        testTag = "join_code_input"
      )

      Spacer(modifier = Modifier.height(28.dp))

      GoldButton(
        text = "Join Class",
        onClick = { onJoinSubmit(joinCode) },
        isLoading = isLoading,
        testTag = "join_class_submit_button"
      )
    }
  }
}
