package com.example.admin

import com.example.admin.data.AdminAuthService
import com.example.admin.data.AdminCsvExporter
import com.example.admin.data.AdminRegisterDetail
import com.example.admin.data.AdminRegisterRow
import com.example.admin.data.SupabaseAdminConfig
import com.example.admin.util.AdminJsonHelper
import com.example.admin.util.AdminRoleHelper
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdminUnitTest {
  @Test
  fun parseRole_identifiesAdminAndDefaultsMissingToStudent() {
    assertEquals("admin", AdminAuthService.parseRole("""[{"role":"admin","verified":true}]"""))
    assertEquals("admin", AdminAuthService.parseRole("""[{"role":"super_admin","verified":true}]"""))
    assertEquals("lecturer", AdminAuthService.parseRole("""[{"role":"lecturer","verified":false}]"""))
    assertEquals("student", AdminAuthService.parseRole("[]"))
    assertEquals("student", AdminAuthService.parseRole("invalid"))
  }

  @Test
  fun roleHelper_adminCheckIsStrict() {
    assertTrue(AdminRoleHelper.isAdmin("admin"))
    assertTrue(AdminRoleHelper.isAdmin("Super_Admin"))
    assertFalse(AdminRoleHelper.isAdmin("lecturer"))
    assertFalse(AdminRoleHelper.isAdmin("class_rep"))
    assertFalse(AdminRoleHelper.isAdmin("student"))
    assertFalse(AdminRoleHelper.isAdmin(""))
  }

  @Test
  fun jsonHelper_neverReturnsNullLiteral() {
    val obj = JSONObject("""{"a":null,"b":"null","c":" Valid "}""")
    assertEquals("", AdminJsonHelper.str(obj, "a"))
    assertEquals("", AdminJsonHelper.str(obj, "b"))
    assertEquals("Valid", AdminJsonHelper.str(obj, "c"))
    assertEquals("-", AdminJsonHelper.orDash("null"))
    assertEquals("-", AdminJsonHelper.orDash(null))
  }

  @Test
  fun csvExporter_formatsRegisterCleanlyWithoutNull() {
    val detail = AdminRegisterDetail(
      sessionId = "s1",
      registerNo = "REG-001",
      title = "Week 4 Lecture",
      status = "closed",
      voidReason = "",
      courseName = "Algorithms",
      courseCode = "CSC 301",
      className = "BSc CS Y3",
      lecturerName = "Dr. Otieno",
      venue = "LT1",
      dateText = "2026-11-18",
      timeText = "09:00 - 11:00",
      presentCount = 1,
      total = 2,
      canClose = false,
      canVoid = false,
      rows = listOf(
        AdminRegisterRow(1, "Alice Wanjiku", "CS/001", isPrefect = true, present = true, timeInText = "09:02", timeOutText = "11:00"),
        AdminRegisterRow(2, "Bob Kariuki", "CS/002", isPrefect = false, present = false, timeInText = "", timeOutText = "")
      )
    )
    val csv = AdminCsvExporter.buildRegisterCsv(detail)
    assertTrue(csv.contains("REG-001"))
    assertTrue(csv.contains("Alice Wanjiku,CS/001,Class Rep,Present,09:02,11:00"))
    assertTrue(csv.contains("Bob Kariuki,CS/002,Student,Absent,-,-"))
    assertFalse(csv.contains(",null,"))
  }

  @Test
  fun config_isConfiguredAndSanitizesUrl() {
    assertTrue(SupabaseAdminConfig.isConfigured())
    assertEquals("https://demo.supabase.co", SupabaseAdminConfig.sanitizeUrl("https://demo.supabase.co/rest/v1/"))
  }
}
