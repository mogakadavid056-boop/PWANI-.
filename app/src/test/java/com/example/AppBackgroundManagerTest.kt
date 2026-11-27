package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppBackgroundManager
import com.example.data.VideoBackgroundSource
import com.example.ui.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppBackgroundManagerTest {

  @Test
  fun parseRpcBackgrounds_parsesBothVideoSlotsPublicAndPrivate() {
    val json = """
      {
        "bg_001": {
          "bucket": "app-backgrounds",
          "path": "themes/Background001.mp4",
          "public": true,
          "updated_at": "2026-11-27T10:00:00Z"
        },
        "bg_002": {
          "bucket": "private-assets",
          "path": "Background002.mp4",
          "public": false,
          "updated_at": "2026-11-27T11:30:00Z"
        }
      }
    """.trimIndent()

    val parsed = AppBackgroundManager.parseRpcBackgrounds(json)
    assertNotNull(parsed.bg001)
    assertEquals("app-backgrounds", parsed.bg001!!.bucket)
    assertEquals("themes/Background001.mp4", parsed.bg001!!.path)
    assertTrue(parsed.bg001!!.isPublic)
    assertEquals("2026-11-27T10:00:00Z", parsed.bg001!!.updatedAt)

    assertNotNull(parsed.bg002)
    assertEquals("private-assets", parsed.bg002!!.bucket)
    assertEquals("Background002.mp4", parsed.bg002!!.path)
    assertFalse(parsed.bg002!!.isPublic)
    assertEquals("2026-11-27T11:30:00Z", parsed.bg002!!.updatedAt)
  }

  @Test
  fun parseRpcBackgrounds_handlesPartialNullsAndInvalidPayloads() {
    val partial = """{"bg_001":{"bucket":"media","path":"Background001.mp4","public":true},"bg_002":null}"""
    val parsedPartial = AppBackgroundManager.parseRpcBackgrounds(partial)
    assertNotNull(parsedPartial.bg001)
    assertNull(parsedPartial.bg002)

    val blankParsed = AppBackgroundManager.parseRpcBackgrounds("   ")
    assertNull(blankParsed.bg001)
    assertNull(blankParsed.bg002)

    val invalidParsed = AppBackgroundManager.parseRpcBackgrounds("[]")
    assertNull(invalidParsed.bg001)
    assertNull(invalidParsed.bg002)
  }

  @Test
  fun buildObjectUrl_buildsPublicPrivateAndSubfolderVideoUrlsSafely() {
    val pubUrl = AppBackgroundManager.buildObjectUrl(
      baseUrl = "https://example.supabase.co/",
      bucket = "app-backgrounds",
      path = "folder one/Background001.mp4",
      isPublic = true
    )
    assertEquals(
      "https://example.supabase.co/storage/v1/object/public/app-backgrounds/folder%20one/Background001.mp4",
      pubUrl
    )

    val privUrl = AppBackgroundManager.buildObjectUrl(
      baseUrl = "https://example.supabase.co",
      bucket = "assets",
      path = "Background002.mp4",
      isPublic = false
    )
    assertEquals(
      "https://example.supabase.co/storage/v1/object/assets/Background002.mp4",
      privUrl
    )
  }

  @Test
  fun isVideoObjectName_acceptsMp4AndWebmAndRejectsOldImages() {
    assertTrue(AppBackgroundManager.isVideoObjectName("Background001.mp4"))
    assertTrue(AppBackgroundManager.isVideoObjectName("Background002.webm"))
    assertTrue(AppBackgroundManager.isVideoObjectName("Background001"))
    assertFalse(AppBackgroundManager.isVideoObjectName("background001.png"))
    assertFalse(AppBackgroundManager.isVideoObjectName("Background002.jpg"))
    assertFalse(AppBackgroundManager.isVideoObjectName("Background002.webp"))
  }

  @Test
  fun videoBackgroundSource_prefersExistingLocalFileOverRemoteForOfflineSupport() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val tmpVideo = File(context.cacheDir, "test_bg001.mp4").apply {
      writeBytes(byteArrayOf(1, 2, 3, 4))
    }
    try {
      val cachedSource = VideoBackgroundSource(
        localFilePath = tmpVideo.absolutePath,
        remoteUrl = "https://example.supabase.co/Background001.mp4"
      )
      assertTrue(cachedSource.playbackUri!!.startsWith("file://"))

      tmpVideo.delete()
      assertEquals(
        "https://example.supabase.co/Background001.mp4",
        cachedSource.playbackUri
      )
    } finally {
      tmpVideo.delete()
    }
  }

  @Test
  fun cleanupLegacyImageBackgrounds_removesOldPngDirectoryAndPrefs() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val legacyDir = File(context.filesDir, "app_backgrounds").apply { mkdirs() }
    val legacyPng = File(legacyDir, "bg_001.png").apply { writeBytes(byteArrayOf(9, 8, 7)) }
    context.getSharedPreferences("classrep_app_backgrounds", Context.MODE_PRIVATE)
      .edit()
      .putString("version_001", "old")
      .commit()

    assertTrue(legacyPng.exists())
    AppBackgroundManager.cleanupLegacyImageBackgrounds(context)
    assertFalse(legacyDir.exists())
    assertNull(
      context.getSharedPreferences("classrep_app_backgrounds", Context.MODE_PRIVATE)
        .getString("version_001", null)
    )
  }

  @Test
  fun isAuthBackgroundScreen_routes001ToWelcomeRegisterLoginAnd002ToOtherScreens() {
    val authScreens = listOf(
      Screen.Splash,
      Screen.Welcome,
      Screen.Landing,
      Screen.Register,
      Screen.CheckEmail,
      Screen.RegisterResult,
      Screen.Login
    )
    authScreens.forEach { screen ->
      assertTrue("Expected $screen to use Background001.mp4", isAuthBackgroundScreen(screen))
    }

    val appScreens = listOf(
      Screen.Home,
      Screen.JoinClass,
      Screen.CreateClass,
      Screen.ClassDetail,
      Screen.PostNotice,
      Screen.ManageMembers,
      Screen.GroupChat,
      Screen.GroupInfo,
      Screen.Profile,
      Screen.ManageUsers,
      Screen.Waiting,
      Screen.FindClasses,
      Screen.Notifications
    )
    appScreens.forEach { screen ->
      assertFalse("Expected $screen to use Background002.mp4", isAuthBackgroundScreen(screen))
    }
  }
}
