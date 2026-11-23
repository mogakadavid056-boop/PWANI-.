package com.example

import com.example.data.AttendanceTrackerService
import com.example.data.RiskLevel
import com.example.notifications.AppNotificationDispatcher
import com.example.notifications.NotificationService
import com.example.notifications.NotificationTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppFeaturesTest {

  @Test
  fun notificationService_parsesJsonAndHandlesNullsSafely() {
    val json = """
      [
        {
          "id": "n-1",
          "class_id": null,
          "kind": "attendance_warning",
          "title": "null",
          "body": "You have missed 3 sessions",
          "data": {"class_id": "cls-7", "session_id": "null", "course_id": "crs-2"},
          "read_at": null,
          "created_at": "2026-11-18T10:00:00Z"
        },
        {
          "id": "n-2",
          "class_id": "cls-8",
          "kind": "notice",
          "title": "Room Change",
          "body": "Moved to LTW",
          "data": {},
          "read_at": "2026-11-18T11:00:00Z",
          "created_at": "2026-11-18T10:30:00Z"
        }
      ]
    """.trimIndent()
    val list = NotificationService.parseNotificationsJson(json)
    assertEquals(2, list.size)
    assertEquals("Notification", list[0].title)
    assertNull(list[0].classId)
    assertEquals("cls-7", list[0].targetClassId)
    assertFalse(list[0].data.containsKey("session_id"))
    assertTrue(list[0].isUnread)
    assertFalse(list[1].isUnread)
  }

  @Test
  fun notificationTimeFormatter_formatsRelativeBuckets() {
    val now = java.time.Instant.parse("2026-11-18T12:00:00Z").toEpochMilli()
    assertEquals("Just now", NotificationTimeFormatter.formatRelative("2026-11-18T11:59:30Z", now))
    assertEquals("5 min ago", NotificationTimeFormatter.formatRelative("2026-11-18T11:55:00Z", now))
    assertEquals("1 hr ago", NotificationTimeFormatter.formatRelative("2026-11-18T11:00:00Z", now))
    assertEquals("3 hr ago", NotificationTimeFormatter.formatRelative("2026-11-18T09:00:00Z", now))
    assertEquals("Yesterday", NotificationTimeFormatter.formatRelative("2026-11-17T12:00:00Z", now))
    assertEquals("", NotificationTimeFormatter.formatRelative("null", now))
  }

  @Test
  fun dispatcher_routesKindsToExpectedChannels() {
    assertEquals(AppNotificationDispatcher.CHANNEL_URGENT, AppNotificationDispatcher.channelForKind("notice_urgent"))
    assertEquals(AppNotificationDispatcher.CHANNEL_URGENT, AppNotificationDispatcher.channelForKind("attendance_open"))
    assertEquals(AppNotificationDispatcher.CHANNEL_URGENT, AppNotificationDispatcher.channelForKind("attendance_warning"))
    assertEquals(AppNotificationDispatcher.CHANNEL_APPROVALS, AppNotificationDispatcher.channelForKind("unit_approved"))
    assertEquals(AppNotificationDispatcher.CHANNEL_UPDATES, AppNotificationDispatcher.channelForKind("material"))
  }

  @Test
  fun attendanceTracker_riskClassificationAndParsers() {
    assertEquals(RiskLevel.OK, RiskLevel.classify(50.0, 5, 0))
    assertEquals(RiskLevel.CRITICAL, RiskLevel.classify(59.9, 1, 10))
    assertEquals(RiskLevel.CRITICAL, RiskLevel.classify(80.0, 4, 10))
    assertEquals(RiskLevel.HIGH, RiskLevel.classify(74.9, 1, 10))
    assertEquals(RiskLevel.HIGH, RiskLevel.classify(90.0, 3, 10))
    assertEquals(RiskLevel.WATCH, RiskLevel.classify(84.9, 0, 10))
    assertEquals(RiskLevel.WATCH, RiskLevel.classify(92.0, 2, 10))
    assertEquals(RiskLevel.OK, RiskLevel.classify(90.0, 0, 10))

    val riskJson = """
      [{"student_id":"u1","full_name":"Alice","admission_number":"null","attended":5,"excused":1,"total_sessions":10,"percentage":60.0,"miss_streak":3,"last_attended_at":null,"risk_level":"high"}]
    """.trimIndent()
    val riskList = AttendanceTrackerService.parseStudentRiskList(riskJson)
    assertEquals(1, riskList.size)
    assertEquals("-", riskList[0].admissionNumber)
    assertEquals(RiskLevel.HIGH, riskList[0].riskLevel)
    assertEquals("", riskList[0].lastAttendedAt)

    val tlJson = """[{"session_id":"s1","title":null,"session_type":"lecture","starts_at":"2026-11-10T08:00:00Z","status":"excused","excuse_reason":"Medical"}]"""
    val tl = AttendanceTrackerService.parseStudentTimeline(tlJson)
    assertEquals(1, tl.size)
    assertEquals("Class Session", tl[0].title)
    assertEquals("excused", tl[0].status)
    assertEquals("Medical", tl[0].excuseReason)

    val myStatus = AttendanceTrackerService.parseMyAttendanceStatus(
      """{"attended":7,"excused":0,"total_sessions":10,"percentage":70.0,"miss_streak":1,"last_attended_at":"null","risk_level":""}"""
    )
    assertEquals(RiskLevel.HIGH, myStatus.riskLevel)
    assertEquals("", myStatus.lastAttendedAt)
  }

  @Test
  fun fcmPush_channelsDeduplicationDeepLinkAndTokenPayloadWork() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    com.example.notifications.NotificationPreferences.clearOnLogout(context)

    AppNotificationDispatcher.ensureChannels(context)
    val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    assertEquals(android.app.NotificationManager.IMPORTANCE_HIGH, nm.getNotificationChannel("urgent")?.importance)
    assertEquals(android.app.NotificationManager.IMPORTANCE_DEFAULT, nm.getNotificationChannel("updates")?.importance)

    assertEquals("urgent", AppNotificationDispatcher.pushChannelForKind("notice_urgent"))
    assertEquals("urgent", AppNotificationDispatcher.pushChannelForKind("attendance_open"))
    assertEquals("urgent", AppNotificationDispatcher.pushChannelForKind("attendance_warning"))
    assertEquals("updates", AppNotificationDispatcher.pushChannelForKind("material"))

    val payloadData = mapOf(
      "notification_id" to "fcm-nid-101",
      "kind" to "attendance_open",
      "class_id" to "cls-44",
      "session_id" to "sess-9"
    )
    assertTrue(AppNotificationDispatcher.dispatchRemoteMessage(context, "Sign Attendance", "Register open", payloadData))
    assertFalse(AppNotificationDispatcher.dispatchRemoteMessage(context, "Sign Attendance", "Register open", payloadData))
    assertTrue(com.example.notifications.NotificationPreferences.isAlreadyShown(context, "fcm-nid-101"))

    val bgTapIntent = android.content.Intent().apply {
      putExtra("kind", "material")
      putExtra("class_id", "cls-55")
      putExtra("notification_id", "fcm-bg-202")
      putExtra("session_id", "null")
    }
    val link = AppNotificationDispatcher.parseIntentDeepLink(bgTapIntent, context)
    assertEquals("material", link?.kind)
    assertEquals("cls-55", link?.classId)
    assertEquals("fcm-bg-202", link?.data?.get("notification_id"))
    assertFalse(link?.data?.containsKey("session_id") == true)
    assertTrue(com.example.notifications.NotificationPreferences.isAlreadyShown(context, "fcm-bg-202"))

    val regJson = org.json.JSONObject(com.example.notifications.PushTokenManager.buildRegisterPayload("tok-abc"))
    assertEquals("tok-abc", regJson.getString("device_token"))
    assertEquals("main", regJson.getString("app_name"))

    val unregJson = org.json.JSONObject(com.example.notifications.PushTokenManager.buildUnregisterPayload("tok-abc"))
    assertEquals("tok-abc", unregJson.getString("device_token"))
  }

  @Test
  fun lecturerService_parsesBecomeLecturerResponsesAccurately() {
    assertEquals(
      com.example.data.BecomeLecturerResult.Success,
      com.example.data.LecturerService.parseBecomeLecturerResponse(204, "")
    )
    assertEquals(
      com.example.data.BecomeLecturerResult.Success,
      com.example.data.LecturerService.parseBecomeLecturerResponse(200, "ok")
    )
    assertEquals(
      com.example.data.BecomeLecturerResult.Success,
      com.example.data.LecturerService.parseBecomeLecturerResponse(200, """{"role":"lecturer","verified":true}""")
    )
    assertEquals(
      com.example.data.BecomeLecturerResult.WrongPasscode,
      com.example.data.LecturerService.parseBecomeLecturerResponse(200, """{"role":"student","verified":false}""")
    )
    assertEquals(
      com.example.data.BecomeLecturerResult.WrongPasscode,
      com.example.data.LecturerService.parseBecomeLecturerResponse(403, """{"message":"Invalid lecturer passcode"}""")
    )
    val err = com.example.data.LecturerService.parseBecomeLecturerResponse(500, """{"message":"Database timeout"}""")
    assertTrue(err is com.example.data.BecomeLecturerResult.Error)
  }
}
