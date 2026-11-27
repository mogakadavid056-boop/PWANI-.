package com.example.ui.components

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.example.data.VideoBackgroundCache
import com.example.data.VideoBackgroundSource
import com.example.ui.theme.Midnight
import com.example.ui.theme.RoyalPurple

@OptIn(UnstableApi::class)
@Composable
fun LiveVideoBackground(
  source: VideoBackgroundSource?,
  useAuthScrim: Boolean,
  modifier: Modifier = Modifier
) {
  val playbackUri = source?.playbackUri ?: return
  if (LocalInspectionMode.current) return

  val context = LocalContext.current
  val appContext = remember(context) { context.applicationContext }

  // Keep current slot smooth without restarting mid-loop if the background download finishes
  // after remote streaming already started for the same version.
  val slotLogicalKey = remember(useAuthScrim, source.versionKey) {
    "${if (useAuthScrim) "001" else "002"}|${source.versionKey}"
  }
  var activeUri by remember(slotLogicalKey) { mutableStateOf(playbackUri) }
  var retryAttempt by remember(slotLogicalKey) { mutableIntStateOf(0) }
  var useStreamCache by remember(slotLogicalKey) { mutableStateOf(true) }

  LaunchedEffect(slotLogicalKey, playbackUri) {
    if (activeUri.isBlank() || (retryAttempt > 0 && playbackUri.startsWith("file://"))) {
      activeUri = playbackUri
    }
  }

  val player = remember(appContext) {
    runCatching {
      ExoPlayer.Builder(appContext)
        .build()
        .apply {
          volume = 0f
          repeatMode = Player.REPEAT_MODE_ONE
          videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
          trackSelectionParameters = trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()
          setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(C.USAGE_MEDIA)
              .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
              .build(),
            /* handleAudioFocus = */ false
          )
          playWhenReady = true
        }
    }.getOrNull()
  }

  if (player == null) return

  DisposableEffect(player, slotLogicalKey) {
    val listener = object : Player.Listener {
      override fun onPlayerError(error: PlaybackException) {
        Log.w("LiveVideoBackground", "Playback error on $activeUri (attempt $retryAttempt)", error)
        if (retryAttempt == 0) {
          useStreamCache = false
          retryAttempt = 1
          source.localFilePath?.takeIf { source.isLocalCached }?.let { local ->
            activeUri = "file://$local"
          }
        }
      }
    }
    player.addListener(listener)
    onDispose {
      player.removeListener(listener)
    }
  }

  LaunchedEffect(activeUri, retryAttempt, useStreamCache) {
    runCatching {
      val dsFactory = VideoBackgroundCache.createDataSourceFactory(
        context = appContext,
        source = source,
        useStreamCache = useStreamCache
      )
      val mediaSource = DefaultMediaSourceFactory(dsFactory)
        .createMediaSource(MediaItem.fromUri(activeUri))
      player.setMediaSource(mediaSource)
      player.prepare()
      player.playWhenReady = true
    }.onFailure { err ->
      Log.w("LiveVideoBackground", "Failed preparing video $activeUri", err)
    }
  }

  LifecycleEventEffect(Lifecycle.Event.ON_START) {
    runCatching {
      if (player.playbackState == Player.STATE_IDLE) {
        player.prepare()
      }
      player.playWhenReady = true
      player.play()
    }
  }

  LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
    runCatching {
      player.pause()
    }
  }

  DisposableEffect(player) {
    onDispose {
      runCatching { player.release() }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .clipToBounds()
  ) {
    ContentFrame(
      player = player,
      modifier = Modifier.fillMaxSize(),
      surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
      contentScale = ContentScale.Crop,
      keepContentOnReset = true,
      shutter = {}
    )

    val scrimColors = if (useAuthScrim) {
      listOf(
        Midnight.copy(alpha = 0.25f),
        Midnight.copy(alpha = 0.42f),
        Midnight.copy(alpha = 0.56f)
      )
    } else {
      listOf(
        Midnight.copy(alpha = 0.44f),
        Midnight.copy(alpha = 0.52f),
        RoyalPurple.copy(alpha = 0.48f)
      )
    }
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(scrimColors))
    )
  }
}
