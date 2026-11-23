package com.example

import com.example.data.AppBackgroundManager
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppBackgroundManagerTest {

  @Test
  fun parseRpcBackgrounds_parsesBothSlotsPublicAndPrivate() {
    val json = """
      {
        "bg_001": {
          "bucket": "app-backgrounds",
          "path": "themes/background001.png",
          "public": true,
          "updated_at": "2026-11-21T10:00:00Z"
        },
        "bg_002": {
          "bucket": "private-assets",
          "path": "Background002.png",
          "public": false,
          "updated_at": "2026-11-21T11:30:00Z"
        }
      }
    """.trimIndent()

    val parsed = AppBackgroundManager.parseRpcBackgrounds(json)
    assertNotNull(parsed.bg001)
    assertEquals("app-backgrounds", parsed.bg001!!.bucket)
    assertEquals("themes/background001.png", parsed.bg001!!.path)
    assertTrue(parsed.bg001!!.isPublic)
    assertEquals("2026-11-21T10:00:00Z", parsed.bg001!!.updatedAt)

    assertNotNull(parsed.bg002)
    assertEquals("private-assets", parsed.bg002!!.bucket)
    assertEquals("Background002.png", parsed.bg002!!.path)
    assertFalse(parsed.bg002!!.isPublic)
    assertEquals("2026-11-21T11:30:00Z", parsed.bg002!!.updatedAt)
  }

  @Test
  fun parseRpcBackgrounds_handlesPartialNullsAndInvalidPayloads() {
    val partial = """{"bg_001":{"bucket":"media","path":"background001.png","public":true},"bg_002":null}"""
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
  fun buildObjectUrl_buildsPublicPrivateAndSubfolderUrlsSafely() {
    val pubUrl = AppBackgroundManager.buildObjectUrl(
      baseUrl = "https://example.supabase.co/",
      bucket = "app-backgrounds",
      path = "folder one/background001.png",
      isPublic = true
    )
    assertEquals(
      "https://example.supabase.co/storage/v1/object/public/app-backgrounds/folder%20one/background001.png",
      pubUrl
    )

    val privUrl = AppBackgroundManager.buildObjectUrl(
      baseUrl = "https://example.supabase.co",
      bucket = "assets",
      path = "Background002.png",
      isPublic = false
    )
    assertEquals(
      "https://example.supabase.co/storage/v1/object/assets/Background002.png",
      privUrl
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
      assertTrue("Expected $screen to use background001", isAuthBackgroundScreen(screen))
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
      assertFalse("Expected $screen to use Background002", isAuthBackgroundScreen(screen))
    }
  }
}
