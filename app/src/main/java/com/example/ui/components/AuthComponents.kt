package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.PoppinsFamily

@Composable
fun GoldButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isLoading: Boolean = false,
  testTag: String = "gold_button"
) {
  Button(
    onClick = onClick,
    enabled = enabled && !isLoading,
    modifier = modifier.fillMaxWidth().height(52.dp).testTag(testTag),
    shape = RoundedCornerShape(14.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = GoldPrimary,
      contentColor = DarkTextOnGold,
      disabledContainerColor = GoldDark.copy(alpha = 0.4f),
      disabledContentColor = DarkTextOnGold.copy(alpha = 0.6f)
    )
  ) {
    if (isLoading) {
      CircularProgressIndicator(modifier = Modifier.size(24.dp), color = DarkTextOnGold, strokeWidth = 2.5.dp)
    } else {
      Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PoppinsFamily,
        color = DarkTextOnGold,
        letterSpacing = 0.5.sp
      )
    }
  }
}

@Composable
fun GoldOutlinedButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = "gold_outlined_button"
) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier.fillMaxWidth().height(52.dp).testTag(testTag),
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = DarkSurface,
      contentColor = GoldPrimary
    )
  ) {
    Text(
      text = text,
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = PoppinsFamily,
      color = GoldPrimary,
      letterSpacing = 0.3.sp
    )
  }
}

@Composable
fun DarkTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  leadingIcon: ImageVector,
  modifier: Modifier = Modifier,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  testTag: String = "dark_text_field"
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    leadingIcon = { Icon(imageVector = leadingIcon, contentDescription = null, tint = GoldPrimary) },
    modifier = modifier.fillMaxWidth().testTag(testTag),
    shape = RoundedCornerShape(14.dp),
    singleLine = true,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    colors = OutlinedTextFieldDefaults.colors(
      focusedContainerColor = DarkSurface,
      unfocusedContainerColor = DarkSurface,
      focusedBorderColor = GoldPrimary,
      unfocusedBorderColor = DarkBorder,
      focusedLabelColor = GoldLight,
      unfocusedLabelColor = TextSecondary,
      focusedTextColor = TextPrimary,
      unfocusedTextColor = TextPrimary,
      cursorColor = GoldPrimary
    )
  )
}

@Composable
fun PasswordTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String = "Password",
  leadingIcon: ImageVector,
  modifier: Modifier = Modifier,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  testTag: String = "password_text_field"
) {
  var passwordVisible by remember { mutableStateOf(false) }

  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    leadingIcon = { Icon(imageVector = leadingIcon, contentDescription = null, tint = GoldPrimary) },
    trailingIcon = {
      IconButton(onClick = { passwordVisible = !passwordVisible }) {
        Icon(
          imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
          contentDescription = if (passwordVisible) "Hide password" else "Show password",
          tint = TextSecondary
        )
      }
    },
    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
    modifier = modifier.fillMaxWidth().testTag(testTag),
    shape = RoundedCornerShape(14.dp),
    singleLine = true,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    colors = OutlinedTextFieldDefaults.colors(
      focusedContainerColor = DarkSurface,
      unfocusedContainerColor = DarkSurface,
      focusedBorderColor = GoldPrimary,
      unfocusedBorderColor = DarkBorder,
      focusedLabelColor = GoldLight,
      unfocusedLabelColor = TextSecondary,
      focusedTextColor = TextPrimary,
      unfocusedTextColor = TextPrimary,
      cursorColor = GoldPrimary
    )
  )
}

@Composable
fun ErrorBanner(
  message: String,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val displayMessage = remember(message) {
    val lower = message.lowercase()
    if (lower.contains("unable to resolve host") ||
      lower.contains("failed to connect") ||
      lower.contains("network is unreachable") ||
      lower.contains("connection refused") ||
      lower.contains("software caused connection abort") ||
      lower.contains("timeout") ||
      lower.contains("cannot reach the server")
    ) {
      "Cannot reach the server. Check your internet and try again."
    } else {
      message
    }
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(ErrorContainer)
      .border(1.dp, ErrorRed.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
      .padding(horizontal = 14.dp, vertical = 10.dp)
      .testTag("error_banner")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = "Error", tint = ErrorRed, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = displayMessage, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
      }
      IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
        Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = TextSecondary, modifier = Modifier.size(16.dp))
      }
    }
  }
}

@Composable
fun AppBrandHeader(
  title: String = "ClassRep",
  subtitle: String = "University Class Coordination"
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
    Box(
      modifier = Modifier.size(80.dp).clip(CircleShape).background(DarkSurfaceVariant).border(2.dp, GoldPrimary, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Image(painter = painterResource(id = R.drawable.ic_classrep_logo), contentDescription = "ClassRep Logo", modifier = Modifier.size(68.dp).clip(CircleShape))
    }
    Spacer(modifier = Modifier.height(14.dp))
    Text(text = title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = GoldLight, letterSpacing = 1.sp)
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = subtitle, fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Normal)
  }
}
