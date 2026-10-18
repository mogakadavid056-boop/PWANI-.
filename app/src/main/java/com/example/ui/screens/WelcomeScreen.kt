package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseConfig
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.PoppinsFamily
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Composable
fun WelcomeScreen(
  onNavigateToLogin: () -> Unit
) {
  val context = LocalContext.current
  var isConnected by remember { mutableStateOf<Boolean?>(null) }
  var retryTrigger by remember { mutableStateOf(0) }

  LaunchedEffect(retryTrigger) {
    isConnected = null
    var attempts = 0
    while (attempts < 5) {
      val ok = checkServerConnection()
      if (ok) {
        isConnected = true
        break
      } else {
        attempts++
        if (attempts >= 5) {
          isConnected = false
        } else {
          kotlinx.coroutines.delay(1500L)
        }
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .testTag("welcome_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // 1. Logo: drawable logo_classrep if it exists, otherwise a graduation-cap icon in a gold circle
      val logoResId = context.resources.getIdentifier("logo_classrep", "drawable", context.packageName)
      if (logoResId != 0) {
        Image(
          painter = painterResource(id = logoResId),
          contentDescription = "ClassRep Logo",
          modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .border(2.5.dp, GoldPrimary, CircleShape)
        )
      } else {
        Box(
          modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .background(Color(0xD90B2A1E))
            .border(2.5.dp, GoldPrimary, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.School,
            contentDescription = "Graduation Cap Logo",
            tint = GoldPrimary,
            modifier = Modifier.size(52.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // 2. Large title "Pwani University ClassRep" (Poppins SemiBold, white, soft shadow)
      Text(
        text = "Pwani University ClassRep",
        style = TextStyle(
          fontFamily = PoppinsFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 24.sp,
          color = Color.White,
          shadow = Shadow(
            color = Color.Black.copy(alpha = 0.65f),
            offset = Offset(0f, 3f),
            blurRadius = 6f
          )
        ),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(8.dp))

      // 3. Subtitle "One Campus. One Community." in gold
      Text(
        text = "One Campus. One Community.",
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = GoldPrimary,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(48.dp))

      // 4. Large gold button "Enter" (gold gradient, dark bold text, 56dp high)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(
            Brush.horizontalGradient(
              listOf(GoldPrimary, GoldLight)
            )
          )
          .clickable(onClick = onNavigateToLogin)
          .testTag("welcome_enter_btn"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Enter",
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF0B2A1E)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 5. Status line under the button: "● Server connected" in green or "● Not connected" in red
      val statusText = when (isConnected) {
        true -> "● Server connected"
        false -> "● Not connected (tap to retry)"
        null -> "● Connecting..."
      }
      val statusColor = when (isConnected) {
        true -> SuccessGreen
        false -> ErrorRed
        null -> GoldPrimary
      }

      Text(
        text = statusText,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = statusColor,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .clickable {
            if (isConnected != true) {
              retryTrigger++
            }
          }
          .testTag("welcome_connection_status")
      )
    }
  }
}

private suspend fun checkServerConnection(): Boolean = withContext(Dispatchers.IO) {
  if (!SupabaseConfig.isConfigured()) return@withContext false
  try {
    val baseUrl = SupabaseConfig.url.trim().trimEnd('/')
    val url = "$baseUrl/rest/v1/universities?select=id&limit=1"
    val anonKey = SupabaseConfig.anonKey.trim()
    val req = Request.Builder()
      .url(url)
      .addHeader("apikey", anonKey)
      .addHeader("Authorization", "Bearer $anonKey")
      .addHeader("Accept", "application/json")
      .get()
      .build()
    val client = OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .build()
    client.newCall(req).execute().use { resp ->
      resp.isSuccessful || resp.code in 200..399
    }
  } catch (_: Throwable) {
    false
  }
}
