package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SilverWhite

@Composable
fun ClassRepSnackbarHost(
  hostState: SnackbarHostState,
  isError: Boolean = true,
  modifier: Modifier = Modifier
) {
  SnackbarHost(
    hostState = hostState,
    modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)
  ) { data: SnackbarData ->
    Surface(
      shape = RoundedCornerShape(ButtonRadius),
      color = if (isError) ErrorContainer else RoyalSurfaceHigh,
      border = BorderStroke(1.2.dp, if (isError) ErrorRed else MetallicGold)
    ) {
      Snackbar(
        containerColor = Color.Transparent,
        contentColor = SilverWhite,
        action = {
          val actionLabel = data.visuals.actionLabel
          if (!actionLabel.isNullOrBlank()) {
            TextButton(onClick = { data.performAction() }) {
              Text(
                text = actionLabel,
                style = ButtonTextStyle.copy(
                  color = if (isError) ErrorRed else ChampagneGold,
                  fontSize = 14.sp
                ),
                maxLines = 1
              )
            }
          }
        }
      ) {
        Text(
          text = data.visuals.message,
          style = CaptionStyle.copy(color = SilverWhite, fontWeight = FontWeight.Medium)
        )
      }
    }
  }
}
