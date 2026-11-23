package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.MetallicGoldText
import com.example.ui.components.RoyalCrownIcon
import com.example.ui.components.RoyalDivider
import com.example.ui.components.rememberAnimationsEnabled
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.GreetingStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.NavLabelStyle
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.SilverWhite
import com.example.ui.theme.SubtitleItalicStyle
import com.example.ui.theme.WordmarkStyle

@Composable
fun SplashScreen() {
  val animEnabled = rememberAnimationsEnabled()
  var targetShimmer by remember { mutableFloatStateOf(if (animEnabled) 0.1f else 0.5f) }
  LaunchedEffect(animEnabled) {
    if (animEnabled) targetShimmer = 0.9f
  }
  val shimmerFraction by animateFloatAsState(
    targetValue = targetShimmer,
    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
    label = "splash_gold_shimmer"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 28.dp, vertical = 32.dp)
      .testTag("splash_screen")
  ) {
    Column(
      modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      RoyalFlourishRule()
      Spacer(modifier = Modifier.height(22.dp))

      Box(
        modifier = Modifier
          .size(96.dp)
          .clip(CircleShape)
          .background(RoyalSurfaceHigh.copy(alpha = 0.92f))
          .border(2.dp, MetallicGold, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_classrep_logo),
          contentDescription = "ClassRep Emblem",
          modifier = Modifier.size(82.dp).clip(CircleShape)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
      RoyalCrownIcon(size = 24.dp)
      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "One Campus, One Community",
        style = GreetingStyle.copy(fontSize = 30.sp, color = ChampagneGold),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(6.dp))

      MetallicGoldText(
        text = "ClassRep",
        style = WordmarkStyle,
        textAlign = TextAlign.Center,
        shimmerFraction = if (animEnabled) shimmerFraction else null
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "University Class Coordination",
        style = SubtitleItalicStyle.copy(color = SilverWhite),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(22.dp))
      RoyalDivider()
      Spacer(modifier = Modifier.height(28.dp))

      CircularProgressIndicator(
        modifier = Modifier.size(28.dp),
        color = MetallicGold,
        trackColor = ChampagneGold.copy(alpha = 0.2f),
        strokeWidth = 2.dp
      )
    }

    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      RoyalCrownIcon(size = 14.dp)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "CONNECT · ORGANIZE · ACHIEVE",
        style = NavLabelStyle.copy(
          color = Lavender,
          letterSpacing = 2.sp
        ),
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
internal fun RoyalFlourishRule(modifier: Modifier = Modifier) {
  Canvas(
    modifier = modifier
      .fillMaxWidth(0.76f)
      .height(10.dp)
  ) {
    val y = size.height / 2f
    drawLine(
      brush = Brush.horizontalGradient(
        listOf(Color.Transparent, MetallicGold.copy(alpha = 0.7f), ChampagneGold, MetallicGold.copy(alpha = 0.7f), Color.Transparent)
      ),
      start = Offset(0f, y),
      end = Offset(size.width, y),
      strokeWidth = 1.2.dp.toPx()
    )
  }
}
