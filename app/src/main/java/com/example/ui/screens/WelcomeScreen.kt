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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseConfig
import com.example.ui.components.GoldButton
import com.example.ui.components.MetallicGoldText
import com.example.ui.components.RoyalCrownIcon
import com.example.ui.components.RoyalDivider
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.GoldShadow
import com.example.ui.theme.GreetingStyle
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SubtitleItalicStyle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WordmarkStyle
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
      val logoResId = context.resources.getIdentifier("logo_classrep", "drawable", context.packageName)
      if (logoResId != 0) {
        Image(
          painter = painterResource(id = logoResId),
          contentDescription = "ClassRep Logo",
          modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .border(2.5.dp, Brush.linearGradient(listOf(ChampagneGold, MetallicGold, GoldShadow)), CircleShape)
        )
      } else {
        Box(
          modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(RoyalSurfaceHigh)
            .border(2.5.dp, Brush.linearGradient(listOf(ChampagneGold, MetallicGold, GoldShadow)), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          RoyalCrownIcon(size = 46.dp, tint = ChampagneGold)
        }
      }

      Spacer(modifier = Modifier.height(22.dp))

      MetallicGoldText(
        text = "ClassRep",
        style = WordmarkStyle.copy(fontSize = 42.sp, lineHeight = 48.sp),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Pwani University",
        style = SubtitleItalicStyle.copy(fontSize = 18.sp),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(10.dp))

      RoyalDivider(modifier = Modifier.fillMaxWidth(0.7f))

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "One Campus. One Community.",
        style = GreetingStyle.copy(fontSize = 30.sp, lineHeight = 36.sp),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(40.dp))

      GoldButton(
        text = "Enter",
        onClick = onNavigateToLogin,
        testTag = "welcome_enter_btn"
      )

      Spacer(modifier = Modifier.height(16.dp))

      val statusText = when (isConnected) {
        true -> "● Server connected"
        false -> "● Not connected (tap to retry)"
        null -> "● Connecting..."
      }
      val statusColor = when (isConnected) {
        true -> SuccessGreen
        false -> ErrorRed
        null -> ChampagneGold
      }

      Text(
        text = statusText,
        style = FieldLabelStyle.copy(color = statusColor, fontWeight = FontWeight.SemiBold),
        textAlign = TextAlign.Center,
        modifier = Modifier
          .clickable {
            if (isConnected != true) {
              retryTrigger++
            }
          }
          .padding(horizontal = 12.dp, vertical = 8.dp)
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
