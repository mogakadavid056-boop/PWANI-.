package com.example.admin

import com.example.admin.data.AdminClassSearchRepository
import com.example.admin.data.AdminFileValidator
import com.example.admin.data.AdminRiskLevel
import com.example.admin.data.AdminRiskRepository
import com.example.admin.data.AdminUnitsMaterialsRepository
import com.example.admin.notifications.AdminNotificationDispatcher
import com.example.admin.notifications.AdminNotificationService
import com.example.admin.notifications.AdminNotificationTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdminFeaturesTest {

  @Test
  fun adminNotificationService_parsesSafelyAndRoutesChannels() {
    val json = """
      [
        {
          "id": "an-1",
          "class_id": "null",
          "kind": "attendance_alert",
          "title": "Critical Attendance Alert",
          "body": "Student fell below 60%",
          "data": {"class_id": "cls-9", "extra": "null"},
          "read_at": null,
          "created_at": "2026-11-18T11:30:00Z"
        }
      ]
    """.trimIndent()
    val items = AdminNotificationService.parseNotificationsJson(json)
    assertEquals(1, items.size)
    assertNull(items[0].classId)
    assertEquals("cls-9", items[0].targetClassId)
    assertFalse(items[0].data.containsKey("extra"))
    assertTrue(items[0].isUnread)

    assertEquals(AdminNotificationDispatcher.CHANNEL_URGENT, AdminNotificationDispatcher.channelForKind("attendance_alert"))
    assertEquals(AdminNotificationDispatcher.CHANNEL_APPROVALS, AdminNotificationDispatcher.channelForKind("unit_pending"))
    assertEquals(AdminNotificationDispatcher.CHANNEL_APPROVALS, AdminNotificationDispatcher.channelForKind("lecturer_registered"))
    assertEquals(AdminNotificationDispatcher.CHANNEL_UPDATES, AdminNotificationDispatcher.channelForKind("general"))

    val now = java.time.Instant.parse("2026-11-18T12:00:00Z").toEpochMilli()
    assertEquals("30 min ago", AdminNotificationTimeFormatter.formatRelative("2026-11-18T11:30:00Z", now))
  }

  @Test
  fun classSearchAndOverview_parseCleanlyWithoutNullStrings() {
    val searchJson = """
      [
        {"class_id":"c1","class_name":"BSc Geomatics Y1","class_code":"GEO1","school_name":"Engineering","programme_name":"null","student_count":42,"has_prefect":false}
      ]
    """.trimIndent()
    val classes = AdminClassSearchRepository.parseSearchClasses(searchJson)
    assertEquals(1, classes.size)
    assertEquals("BSc Geomatics Y1", classes[0].className)
    assertEquals("", classes[0].programmeName)
    assertEquals(42, classes[0].studentCount)
    assertFalse(classes[0].hasPrefect)

    val overviewJson = """
      {"class_id":"c1","class_name":"BSc Geomatics Y1","join_code":"XYZ999","year_of_study":1,"student_count":42,"lecturer_count":3,"prefect_count":1,"unit_count":6,"material_count":14,"notice_count":5,"session_count":8,"poll_count":2}
    """.trimIndent()
    val ov = AdminClassSearchRepository.parseClassOverview(overviewJson)
    assertEquals("XYZ999", ov.joinCode)
    assertEquals(1, ov.yearOfStudy)
    assertEquals(6, ov.unitCount)
    assertEquals(14, ov.materialCount)
  }

  @Test
  fun pendingUnitsAndFileValidator_workCorrectly() {
    val pendingJson = """
      [{"course_id":"u1","class_id":"c1","class_name":"CS Y2","code":"csc 201","name":"Algorithms","description":null,"requested_by":"lec1","requester_name":"Dr. Ada","created_at":"2026-11-18T08:00:00Z"}]
    """.trimIndent()
    val units = AdminUnitsMaterialsRepository.parsePendingUnits(pendingJson)
    assertEquals(1, units.size)
    assertEquals("CSC 201", units[0].code)
    assertEquals("", units[0].description)

    assertNull(AdminFileValidator.validate("lecture1.pdf", "application/pdf", 1024L))
    assertNotNull(AdminFileValidator.validate("huge.pdf", "application/pdf", AdminFileValidator.MAX_BYTES + 1))
    assertNotNull(AdminFileValidator.validate("virus.exe", "application/x-msdownload", 500L))
  }

  @Test
  fun riskRepository_parsesOverviewStudentsAndTimeline() {
    assertEquals(AdminRiskLevel.CRITICAL, AdminRiskLevel.classify(55.0, 0, 5))
    assertEquals(AdminRiskLevel.HIGH, AdminRiskLevel.classify(82.0, 3, 5))
    assertEquals(AdminRiskLevel.WATCH, AdminRiskLevel.classify(82.0, 1, 5))
    assertEquals(AdminRiskLevel.OK, AdminRiskLevel.classify(95.0, 0, 5))

    val ov = AdminRiskRepository.parseRiskOverview(
      """[{"class_id":"c1","class_name":"IT Y3","school_name":"Computing","student_count":30,"total_sessions":10,"critical_count":2,"high_count":4,"watch_count":5,"ok_count":19}]"""
    )
    assertEquals(1, ov.size)
    assertEquals(2, ov[0].criticalCount)

    val students = AdminRiskRepository.parseAtRiskStudents(
      """[{"class_id":"c1","class_name":"IT Y3","student_id":"s1","full_name":"Bob","admission_number":"null","attended":4,"excused":1,"total_sessions":10,"percentage":50.0,"miss_streak":4,"last_attended_at":null,"risk_level":"critical"}]"""
    )
    assertEquals(1, students.size)
    assertEquals("-", students[0].admissionNumber)
    assertEquals(AdminRiskLevel.CRITICAL, students[0].riskLevel)

    val tl = AdminRiskRepository.parseTimeline(
      """[{"session_id":"sess1","title":"Database Systems","session_type":"lecture","starts_at":"2026-11-12T08:00:00Z","status":"absent","excuse_reason":null}]"""
    )
    assertEquals(1, tl.size)
    assertEquals("absent", tl[0].status)
    assertEquals("", tl[0].excuseReason)
  }

  @Test
  fun mainActivity_launchesUnderRobolectric() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    assertNotNull(controller.get())
  }

  @Test
  fun adminFcmPush_channelsDeduplicationDeepLinkAndTokenPayloadWork() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    com.example.admin.notifications.AdminNotificationPrefs.clearOnLogout(context)

    AdminNotificationDispatcher.ensureChannels(context)
    val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    assertEquals(android.app.NotificationManager.IMPORTANCE_HIGH, nm.getNotificationChannel("urgent")?.importance)
    assertEquals(android.app.NotificationManager.IMPORTANCE_DEFAULT, nm.getNotificationChannel("updates")?.importance)

    assertEquals("urgent", AdminNotificationDispatcher.pushChannelForKind("attendance_alert"))
    assertEquals("urgent", AdminNotificationDispatcher.pushChannelForKind("notice_urgent"))
    assertEquals("updates", AdminNotificationDispatcher.pushChannelForKind("unit_pending"))

    val payloadData = mapOf(
      "notification_id" to "adm-fcm-11",
      "kind" to "attendance_alert",
      "class_id" to "cls-77"
    )
    assertTrue(AdminNotificationDispatcher.dispatchRemoteMessage(context, "Alert", "Critical student attendance", payloadData))
    assertFalse(AdminNotificationDispatcher.dispatchRemoteMessage(context, "Alert", "Critical student attendance", payloadData))
    assertTrue(com.example.admin.notifications.AdminNotificationPrefs.isAlreadyShown(context, "adm-fcm-11"))

    val bgTapIntent = android.content.Intent().apply {
      putExtra("kind", "lecturer_registered")
      putExtra("class_id", "null")
      putExtra("notification_id", "adm-bg-99")
      putExtra("user_id", "usr-8")
    }
    val link = AdminNotificationDispatcher.parseIntentDeepLink(bgTapIntent, context)
    assertEquals("lecturer_registered", link?.kind)
    assertEquals("", link?.classId)
    assertEquals("adm-bg-99", link?.data?.get("notification_id"))
    assertEquals("usr-8", link?.data?.get("user_id"))
    assertTrue(com.example.admin.notifications.AdminNotificationPrefs.isAlreadyShown(context, "adm-bg-99"))

    val regJson = org.json.JSONObject(com.example.admin.notifications.AdminPushTokenManager.buildRegisterPayload("adm-tok"))
    assertEquals("adm-tok", regJson.getString("device_token"))
    assertEquals("admin", regJson.getString("app_name"))

    val unregJson = org.json.JSONObject(com.example.admin.notifications.AdminPushTokenManager.buildUnregisterPayload("adm-tok"))
    assertEquals("adm-tok", unregJson.getString("device_token"))
  }

  @Test
  fun adminUsersAndMembersParsers_handleRpcAndRestFallbacks() {
    val rpcUsers = com.example.admin.data.AdminUsersRepository.parseRpcUsers(
      """[{"id":"u1","full_name":"Dr. Jane","email":"jane@uni.edu","admission_number":"ST-101","role":"lecturer","verified":false}]"""
    )
    assertEquals(1, rpcUsers.size)
    assertTrue(rpcUsers[0].isLecturer)
    assertFalse(rpcUsers[0].isVerified)

    val mergedUsers = com.example.admin.data.AdminUsersRepository.mergeProfilesAndRoles(
      profilesJson = """[{"id":"u1","full_name":"Dr. Jane","email":"jane@uni.edu","staff_number":"ST-101"},{"id":"u2","full_name":"Sam Student","email":"sam@uni.edu","admission_number":"ADM-2"}]""",
      rolesJson = """[{"user_id":"u1","role":"lecturer","verified":false},{"user_id":"u2","role":"student","verified":true}]""",
      searchText = "jane",
      maxRows = 50
    )
    assertEquals(1, mergedUsers.size)
    assertEquals("ST-101", mergedUsers[0].admissionNumber)
    assertTrue(mergedUsers[0].isLecturer)
    assertFalse(mergedUsers[0].isVerified)

    val restMembers = com.example.admin.data.AdminMonitorContentRepository.mergeRestMembers(
      membersJson = """[{"user_id":"u1","status":"active","role":"lecturer"},{"user_id":"u2","status":"pending","role":"lecturer"},{"user_id":"u3","status":"active","role":"student"}]""",
      profilesJson = """[{"id":"u1","full_name":"Dr. Jane","staff_number":"ST-1"},{"id":"u2","full_name":"Pending Lec"},{"id":"u3","full_name":"Alice","admission_number":"ADM-9"}]""",
      rolesJson = """[{"user_id":"u1","role":"lecturer"},{"user_id":"u3","role":"student"}]""",
      repsJson = """[{"user_id":"u3"}]"""
    )
    assertEquals(2, restMembers.size)
    assertEquals("u1", restMembers[0].userId)
    assertEquals("u3", restMembers[1].userId)
    assertTrue(restMembers[1].isRep)
  }

  @Test
  fun adminPowersRpcs_buildExpectedPayloadsWithoutLegacyReason() {
    val delBody = com.example.admin.data.AdminAttendancePollsGroupsRepository.buildDeleteSessionBody("sess-9")
    assertEquals("sess-9", delBody.getString("target_session_id"))
    assertFalse(delBody.has("reason"))

    val cleanClassBody = com.example.admin.data.AdminAttendancePollsGroupsRepository.buildDeleteEmptyExpiredSessionsBody("cls-1")
    assertEquals("cls-1", cleanClassBody.getString("target_class_id"))
    val cleanGlobalBody = com.example.admin.data.AdminAttendancePollsGroupsRepository.buildDeleteEmptyExpiredSessionsBody(null)
    assertTrue(cleanGlobalBody.isNull("target_class_id"))
    assertEquals(4, com.example.admin.data.AdminAttendancePollsGroupsRepository.parseScalarInt(" 4 "))

    val remWithReason = com.example.admin.data.AdminMonitorContentRepository.buildAdminRemoveMemberBody("cls-1", "usr-2", "Transferred unit")
    assertEquals("cls-1", remWithReason.getString("target_class_id"))
    assertEquals("usr-2", remWithReason.getString("target_user_id"))
    assertEquals("Transferred unit", remWithReason.getString("removal_reason"))

    val remNoReason = com.example.admin.data.AdminMonitorContentRepository.buildAdminRemoveMemberBody("cls-1", "usr-2", "  ")
    assertTrue(remNoReason.isNull("removal_reason"))
  }
}
