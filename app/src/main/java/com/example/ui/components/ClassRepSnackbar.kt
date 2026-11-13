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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary

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
      shape = RoundedCornerShape(12.dp),
      color = if (isError) ErrorContainer else DarkSurfaceElevated,
      border = BorderStroke(1.2.dp, if (isError) ErrorRed else GoldPrimary)
    ) {
      Snackbar(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = TextPrimary,
        action = {
          val actionLabel = data.visuals.actionLabel
          if (!actionLabel.isNullOrBlank()) {
            TextButton(onClick = { data.performAction() }) {
              Text(
                text = actionLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isError) ErrorRed else GoldLight,
                maxLines = 1
              )
            }
          }
        }
      ) {
        Text(
          text = data.visuals.message,
          fontSize = 13.5.sp,
          fontWeight = FontWeight.Medium,
          color = TextPrimary
        )
      }
    }
  }
}
