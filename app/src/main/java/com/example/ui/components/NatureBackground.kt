package com.example.ui.components

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

private fun decodeSampledBitmap(
  res: Resources,
  resId: Int,
  maxDimension: Int = 1600
): Bitmap? {
  if (resId == 0) return null
  return try {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(res, resId, boundsOptions)
    val width = boundsOptions.outWidth
    val height = boundsOptions.outHeight
    if (width <= 0 || height <= 0) return null

    var inSampleSize = 1
    val longestSide = maxOf(width, height)
    while ((longestSide / inSampleSize) > maxDimension) {
      inSampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply {
      this.inSampleSize = inSampleSize
      inPreferredConfig = Bitmap.Config.RGB_565
    }
    BitmapFactory.decodeResource(res, resId, decodeOptions)
  } catch (e: Throwable) {
    null
  }
}

@Composable
fun NatureBackground(
  isFirstScreen: Boolean = true,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit
) {
  val context = LocalContext.current
  val resources = context.resources
  val packageName = context.packageName

  val campusResId = remember(packageName) {
    resources.getIdentifier("bg_campus", "drawable", packageName)
  }
  val landscapeResId = remember(packageName) {
    resources.getIdentifier("bg_landscape", "drawable", packageName)
  }

  val campusBitmap: ImageBitmap? = remember(campusResId) {
    decodeSampledBitmap(resources, campusResId, 1600)?.asImageBitmap()
  }
  val landscapeBitmap: ImageBitmap? = remember(landscapeResId) {
    decodeSampledBitmap(resources, landscapeResId, 1600)?.asImageBitmap()
  }

  val transitionAlpha by animateFloatAsState(
    targetValue = if (isFirstScreen) 1f else 0f,
    animationSpec = tween(durationMillis = 400),
    label = "BackgroundCrossfade"
  )

  Box(modifier = modifier.fillMaxSize()) {
    // 1. Fallback Canvas background if drawables are missing
    if (campusBitmap == null && landscapeBitmap == null) {
      HillsCanvasFallback()
    }

    // 2. Layer 2: All Other Screens (Landscape photo + 60% overlay + subtle top/bottom gradient)
    if (transitionAlpha < 0.999f) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer { alpha = 1f - transitionAlpha }
      ) {
        if (landscapeBitmap != null) {
          Image(
            bitmap = landscapeBitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
          )
        }
        // Dark green overlay of about 60% (#0B2A1E)
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0x990B2A1E))
        )
        // Slightly darker gradient at top and bottom
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                0.0f to Color(0x6605150E),
                0.25f to Color.Transparent,
                0.75f to Color.Transparent,
                1.0f to Color(0x7305150E)
              )
            )
        )
      }
    }

    // 3. Layer 1: First Screens (Splash and Welcome) (Campus photo + 35% overlay + gradient to 65% at bottom)
    if (transitionAlpha > 0.001f) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer { alpha = transitionAlpha }
      ) {
        if (campusBitmap != null) {
          Image(
            bitmap = campusBitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
          )
        }
        // Dark overlay of about 35% (#0B2A1E)
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0x590B2A1E))
        )
        // Gradient that grows to about 65% dark at the bottom
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                0.0f to Color.Transparent,
                0.45f to Color(0x330B2A1E),
                1.0f to Color(0xA60B2A1E)
              )
            )
        )
      }
    }

    content()
  }
}

@Composable
private fun HillsCanvasFallback() {
  Canvas(modifier = Modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    val horizonY = h * 0.46f
    val skyBrush = Brush.verticalGradient(
      0.0f to Color(0xFF0B3D2E),
      0.50f to Color(0xFFF2C14E),
      1.0f to Color(0xFF7EAA76),
      startY = 0f,
      endY = horizonY + (h * 0.14f)
    )
    drawRect(brush = skyBrush, size = size)

    val sunCenter = Offset(w * 0.65f, horizonY * 0.94f)
    val sunRadius = w * 0.42f
    val sunBrush = Brush.radialGradient(
      listOf(
        Color(0x88FFF4B8),
        Color(0x33F2C14E),
        Color(0x00F2C14E)
      ),
      center = sunCenter,
      radius = sunRadius
    )
    drawCircle(brush = sunBrush, radius = sunRadius, center = sunCenter)

    // Back Layer
    val path1 = Path().apply {
      moveTo(0f, horizonY + 12f)
      cubicTo(w * 0.22f, horizonY - 42f, w * 0.52f, horizonY + 28f, w * 0.78f, horizonY - 18f)
      cubicTo(w * 0.88f, horizonY - 8f, w * 0.95f, horizonY + 12f, w, horizonY + 4f)
      lineTo(w, h)
      lineTo(0f, h)
      close()
    }
    drawPath(path = path1, color = Color(0xFF1F6B4A))

    // Middle Layer
    val midY = horizonY + (h * 0.12f)
    val path2 = Path().apply {
      moveTo(0f, midY + 38f)
      cubicTo(w * 0.20f, midY - 22f, w * 0.50f, midY + 48f, w * 0.75f, midY - 12f)
      cubicTo(w * 0.88f, midY - 26f, w * 0.95f, midY + 8f, w, midY + 28f)
      lineTo(w, h)
      lineTo(0f, h)
      close()
    }
    drawPath(path = path2, color = Color(0xFF2E8B57))

    // Front Layer
    val frontY = horizonY + (h * 0.25f)
    val path3 = Path().apply {
      moveTo(0f, frontY + 16f)
      cubicTo(w * 0.30f, frontY + 52f, w * 0.60f, frontY - 32f, w * 0.85f, frontY + 34f)
      cubicTo(w * 0.92f, frontY + 44f, w * 0.97f, frontY + 18f, w, frontY + 22f)
      lineTo(w, h)
      lineTo(0f, h)
      close()
    }
    drawPath(path = path3, color = Color(0xFF145A3A))

    val darkOverlay = Brush.verticalGradient(
      0.0f to Color(0x33000000),
      0.45f to Color(0x2205150E),
      1.0f to Color(0x5505150E)
    )
    drawRect(brush = darkOverlay, size = size)
  }
}
