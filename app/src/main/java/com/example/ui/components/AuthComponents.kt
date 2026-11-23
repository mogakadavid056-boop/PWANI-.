package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.GoldShadow
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.RoyalErrorContainer
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SilverWhite
import com.example.ui.theme.SubtitleItalicStyle
import com.example.ui.theme.WordmarkStyle

@Composable
fun GoldButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  leadingIcon: ImageVector? = null,
  height: Dp = 52.dp,
  enabled: Boolean = true,
  isLoading: Boolean = false,
  testTag: String = "gold_button"
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val shape = RoundedCornerShape(14.dp)

  Button(
    onClick = onClick,
    enabled = enabled && !isLoading,
    interactionSource = interactionSource,
    contentPadding = PaddingValues(0.dp),
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .then(
        if (isPressed && enabled) {
          Modifier.shadow(14.dp, shape, ambientColor = ElectricViolet, spotColor = ElectricViolet)
        } else {
          Modifier
        }
      )
      .testTag(testTag),
    shape = shape,
    colors = ButtonDefaults.buttonColors(
      containerColor = Color.Transparent,
      contentColor = Midnight,
      disabledContainerColor = GoldShadow.copy(alpha = 0.35f),
      disabledContentColor = Midnight.copy(alpha = 0.6f)
    )
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(height)
        .background(
          if (enabled && !isLoading) {
            RoyalButtonGradientBrush
          } else {
            Brush.horizontalGradient(listOf(GoldShadow.copy(alpha = 0.35f), GoldShadow.copy(alpha = 0.35f)))
          }
        )
        .padding(horizontal = 16.dp),
      contentAlignment = Alignment.Center
    ) {
      if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = RoyalPurple, strokeWidth = 2.5.dp)
      } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
          if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = Midnight, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
          }
          Text(
            text = text,
            style = ButtonTextStyle.copy(color = Midnight, letterSpacing = 0.4.sp)
          )
        }
      }
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
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val shape = RoundedCornerShape(14.dp)

  OutlinedButton(
    onClick = onClick,
    interactionSource = interactionSource,
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp)
      .then(
        if (isPressed) Modifier.shadow(12.dp, shape, ambientColor = ElectricViolet, spotColor = ElectricViolet)
        else Modifier
      )
      .testTag(testTag),
    shape = shape,
    border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(ChampagneGold, MetallicGold, GoldShadow))),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = Color.Transparent,
      contentColor = ChampagneGold
    )
  ) {
    Text(
      text = text,
      style = ButtonTextStyle.copy(color = ChampagneGold, letterSpacing = 0.3.sp)
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
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    leadingIcon = {
      Icon(
        imageVector = leadingIcon,
        contentDescription = null,
        tint = if (isFocused) ChampagneGold else MetallicGold
      )
    },
    interactionSource = interactionSource,
    modifier = modifier
      .fillMaxWidth()
      .then(if (isFocused) Modifier.shadow(10.dp, RoundedCornerShape(14.dp), ambientColor = ElectricViolet, spotColor = ElectricViolet) else Modifier)
      .testTag(testTag),
    shape = RoundedCornerShape(14.dp),
    singleLine = true,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    colors = royalOutlinedTextFieldColors()
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
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    leadingIcon = {
      Icon(
        imageVector = leadingIcon,
        contentDescription = null,
        tint = if (isFocused) ChampagneGold else MetallicGold
      )
    },
    trailingIcon = {
      IconButton(
        onClick = { passwordVisible = !passwordVisible },
        modifier = Modifier.size(48.dp)
      ) {
        Icon(
          imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
          contentDescription = if (passwordVisible) "Hide password" else "Show password",
          tint = Lavender
        )
      }
    },
    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
    interactionSource = interactionSource,
    modifier = modifier
      .fillMaxWidth()
      .then(if (isFocused) Modifier.shadow(10.dp, RoundedCornerShape(14.dp), ambientColor = ElectricViolet, spotColor = ElectricViolet) else Modifier)
      .testTag(testTag),
    shape = RoundedCornerShape(14.dp),
    singleLine = true,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    colors = royalOutlinedTextFieldColors()
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
      .clip(RoundedCornerShape(14.dp))
      .background(RoyalErrorContainer)
      .border(1.dp, ErrorColor.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
      .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
      .testTag("error_banner")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = "Error", tint = ErrorColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = displayMessage,
          style = CaptionStyle.copy(color = SilverWhite, fontWeight = FontWeight.Medium)
        )
      }
      IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
        Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = Lavender, modifier = Modifier.size(18.dp))
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
      modifier = Modifier
        .size(84.dp)
        .clip(CircleShape)
        .background(RoyalSurfaceHigh)
        .border(2.dp, Brush.linearGradient(listOf(ChampagneGold, MetallicGold, GoldShadow)), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      RoyalCrownIcon(size = 42.dp, tint = ChampagneGold)
    }
    Spacer(modifier = Modifier.height(14.dp))
    MetallicGoldText(
      text = title,
      style = WordmarkStyle.copy(fontSize = 40.sp, lineHeight = 46.sp),
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = subtitle,
      style = SubtitleItalicStyle,
      textAlign = TextAlign.Center
    )
  }
}
