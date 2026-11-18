package com.example

import com.example.data.MaterialItem
import com.example.data.ScheduleItem
import com.example.data.SupabaseConfig
import com.example.ui.MaterialsUiState
import com.example.ui.TimetableViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testUrlSanitization() {
    val raw1 = "  https://example.supabase.co/  "
    assertEquals("https://example.supabase.co", SupabaseConfig.sanitizeUrl(raw1))

    val raw2 = "\"https://example.supabase.co/rest/v1\""
    assertEquals("https://example.supabase.co", SupabaseConfig.sanitizeUrl(raw2))

    val raw3 = "'https://example.supabase.co/auth/v1/'"
    assertEquals("https://example.supabase.co", SupabaseConfig.sanitizeUrl(raw3))

    val raw4 = "https://example.supabase.co/rest/v1/"
    assertEquals("https://example.supabase.co", SupabaseConfig.sanitizeUrl(raw4))
  }

  @Test
  fun testKeySanitization() {
    val key1 = "  \"my-anon-key\"  "
    assertEquals("my-anon-key", SupabaseConfig.sanitizeKey(key1))
  }

  @Test
  fun testExtractErrorMessage() {
    val jsonMsg = """{"message":"End time must be after start time"}"""
    assertEquals("End time must be after start time", SupabaseConfig.extractErrorMessage(jsonMsg, 400))

    val jsonError = """{"error_description":"Invalid token"}"""
    assertEquals("Invalid token", SupabaseConfig.extractErrorMessage(jsonError, 401))

    val jsonHint = """{"details":"Field missing"}"""
    assertEquals("Field missing", SupabaseConfig.extractErrorMessage(jsonHint, 422))

    val blankResponse = ""
    assertEquals("Server responded with status code 500", SupabaseConfig.extractErrorMessage(blankResponse, 500))
  }

  @Test
  fun testScheduleItemProperties() {
    val item = ScheduleItem(
      id = "s1", title = "Data Structures", lecturerName = "Dr. Smith",
      dayOfWeek = 1, startTime = "08:00:00", endTime = "10:00:00", room = "Lab 3"
    )

    assertEquals("Monday", item.dayName)
    assertEquals("08:00 - 10:00", item.formattedTimeRange)

    val itemSunday = item.copy(dayOfWeek = 7, startTime = "14:30", endTime = "16:00")
    assertEquals("Sunday", itemSunday.dayName)
    assertEquals("14:30 - 16:00", itemSunday.formattedTimeRange)
  }

  @Test
  fun testTodayDayOfWeek() {
    val day = TimetableViewModel.computeTodayDayOfWeek()
    assertTrue(day in 1..7)
  }

  @Test
  fun testScheduleValidation() {
    fun isValidSchedule(title: String, start: String, end: String): Boolean {
      if (title.isBlank()) return false
      if (end <= start) return false
      return true
    }

    assertTrue(isValidSchedule("Networks", "08:00", "10:00"))
    assertFalse(isValidSchedule("", "08:00", "10:00"))
    assertFalse(isValidSchedule("Networks", "10:00", "08:00"))
    assertFalse(isValidSchedule("Networks", "10:00", "10:00"))
  }

  @Test
  fun testIsStaffLogic() {
    fun evaluateIsStaff(role: String?, isRep: Boolean): Boolean {
      val isAdminOrLecturer = role.equals("admin", ignoreCase = true) || role.equals("lecturer", ignoreCase = true)
      return isAdminOrLecturer || isRep
    }

    assertTrue(evaluateIsStaff("admin", false))
    assertTrue(evaluateIsStaff("Admin", false))
    assertTrue(evaluateIsStaff("lecturer", false))
    assertTrue(evaluateIsStaff("Lecturer", false))
    assertTrue(evaluateIsStaff("student", true))
    assertFalse(evaluateIsStaff("student", false))
    assertFalse(evaluateIsStaff(null, false))
  }

  @Test
  fun testMaterialItemAndTypeLabels() {
    val mNotes = MaterialItem(id = "1", title = "Lecture 1", description = "Basics", materialType = "notes", externalUrl = "https://example.com/notes.pdf", createdAt = "2026-10-10T12:00:00Z")
    assertEquals("Notes", mNotes.typeLabel)
    assertEquals("2026-10-10", mNotes.formattedDate)

    val mSlides = MaterialItem(id = "2", title = "Deck 2", description = "", materialType = "slides", externalUrl = "https://example.com/deck.pptx", createdAt = "2026-10-11")
    assertEquals("Slides", mSlides.typeLabel)

    val mPaper = MaterialItem(id = "3", title = "Midterm 2025", description = "", materialType = "past_paper", externalUrl = "https://example.com/paper.pdf", createdAt = "")
    assertEquals("Past Paper", mPaper.typeLabel)

    val mVideo = MaterialItem(id = "4", title = "Recap", description = "", materialType = "video", externalUrl = "https://youtube.com/watch?v=123", createdAt = "")
    assertEquals("Video", mVideo.typeLabel)

    val mLink = MaterialItem(id = "5", title = "Doc Reference", description = "", materialType = "link", externalUrl = "https://developer.android.com", createdAt = "")
    assertEquals("Link", mLink.typeLabel)
  }

  @Test
  fun testMaterialUrlValidation() {
    fun isValidUrl(url: String): Boolean {
      val trimmed = url.trim()
      return trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)
    }

    assertTrue(isValidUrl("https://example.com/doc.pdf"))
    assertTrue(isValidUrl("http://example.com"))
    assertTrue(isValidUrl("HTTPS://EXAMPLE.COM"))
    assertFalse(isValidUrl("ftp://example.com"))
    assertFalse(isValidUrl("example.com/doc.pdf"))
    assertFalse(isValidUrl(""))
  }

  @Test
  fun testMaterialsUiStateFiltering() {
    val m1 = MaterialItem(id = "1", title = "Computer Systems Lecture", description = "", materialType = "notes", externalUrl = "https://a.com", createdAt = "")
    val m2 = MaterialItem(id = "2", title = "Database Design", description = "", materialType = "slides", externalUrl = "https://b.com", createdAt = "")
    val m3 = MaterialItem(id = "3", title = "Calculus Past Paper", description = "", materialType = "past_paper", externalUrl = "https://c.com", createdAt = "")

    val state = MaterialsUiState(materials = listOf(m1, m2, m3))

    // Search query filter
    val searchState = state.copy(searchQuery = "database")
    assertEquals(1, searchState.filteredMaterials.size)
    assertEquals("Database Design", searchState.filteredMaterials[0].title)

    // Type filter
    val notesState = state.copy(selectedTypeFilter = "Notes")
    assertEquals(1, notesState.filteredMaterials.size)
    assertEquals("Computer Systems Lecture", notesState.filteredMaterials[0].title)

    // Combined filter
    val combinedState = state.copy(searchQuery = "Paper", selectedTypeFilter = "Past Paper")
    assertEquals(1, combinedState.filteredMaterials.size)
    assertEquals("Calculus Past Paper", combinedState.filteredMaterials[0].title)

    val noMatchState = state.copy(searchQuery = "Physics")
    assertEquals(0, noMatchState.filteredMaterials.size)
  }

  @Test
  fun testUniversityModelsAndClassProgramme() {
    val school = com.example.data.SchoolItem("sch-1", "School of Computing", "uni-1")
    assertEquals("School of Computing", school.name)
    assertEquals("uni-1", school.universityId)

    val programme = com.example.data.ProgrammeItem("prg-1", "BSc Computer Science", "sch-1")
    assertEquals("BSc Computer Science", programme.name)
    assertEquals("sch-1", programme.schoolId)

    val academicYear = com.example.data.AcademicYearItem("ay-1", "2025/2026", isCurrent = true)
    assertEquals("2025/2026", academicYear.name)
    assertTrue(academicYear.isCurrent)

    val classItem = com.example.data.ClassItem("c-1", "Algorithms", "CS201", "JOIN123", programmeName = "BSc Computer Science")
    assertEquals("BSc Computer Science", classItem.programmeName)
  }

  @Test
  fun testOrDashNeverShowsNull() {
    assertEquals("-", com.example.util.JsonHelper.orDash(null))
    assertEquals("-", com.example.util.JsonHelper.orDash("null"))
    assertEquals("-", com.example.util.JsonHelper.orDash("NULL"))
    assertEquals("-", com.example.util.JsonHelper.orDash("   "))
    assertEquals("Room 4", com.example.util.JsonHelper.orDash("Room 4"))
  }

  @Test
  fun testRoleHelperPermissionsAndNormalization() {
    assertEquals("class_rep", com.example.util.RoleHelper.normalizeRole("class_rep"))
    assertEquals("class_rep", com.example.util.RoleHelper.normalizeRole("prefect"))
    assertEquals("admin", com.example.util.RoleHelper.normalizeRole("super_admin"))
    assertEquals("lecturer", com.example.util.RoleHelper.normalizeRole("Lecturer"))
    assertEquals("student", com.example.util.RoleHelper.normalizeRole("STUDENT"))
    assertEquals("", com.example.util.RoleHelper.normalizeRole(null))

    assertTrue(com.example.util.RoleHelper.canManageClassContent("lecturer"))
    assertTrue(com.example.util.RoleHelper.canManageClassContent("class_rep"))
    assertTrue(com.example.util.RoleHelper.canManageClassContent("admin"))
    assertTrue(com.example.util.RoleHelper.canManageClassContent("student", isRepInClass = true))
    assertFalse(com.example.util.RoleHelper.canManageClassContent("student", isRepInClass = false))

    assertTrue(com.example.util.RoleHelper.canSignAttendance("student"))
    assertTrue(com.example.util.RoleHelper.canSignAttendance("class_rep"))
    assertFalse(com.example.util.RoleHelper.canSignAttendance("lecturer"))
    assertFalse(com.example.util.RoleHelper.canSignAttendance("admin"))

    assertEquals("Class Rep", com.example.util.RoleHelper.formatDisplayName("class_rep"))
    assertEquals("Super Admin", com.example.util.RoleHelper.formatDisplayName("admin"))
    assertEquals("Lecturer", com.example.util.RoleHelper.formatDisplayName("lecturer"))
    assertEquals("Student", com.example.util.RoleHelper.formatDisplayName("student"))
  }

  @Test
  fun testScheduleItemCancelledAndMovedStatus() {
    val active = ScheduleItem("1", "AI", "Dr. A", 1, "08:00", "10:00", "LT1", status = "scheduled")
    assertFalse(active.isCancelled)
    assertFalse(active.isMoved)

    val cancelled = active.copy(status = "cancelled", statusNote = "Public holiday")
    assertTrue(cancelled.isCancelled)
    assertFalse(cancelled.isMoved)
    assertEquals("cancelled", cancelled.normalizedStatus)

    val moved = active.copy(status = "Moved", statusNote = "Moved to Room 204")
    assertTrue(moved.isMoved)
    assertFalse(moved.isCancelled)
    assertEquals("moved", moved.normalizedStatus)
  }

  @Test
  fun testAlarmOffsetsAndRequestCodes() {
    assertEquals(listOf(15, 0), com.example.alarms.AlarmScheduler.getOffsetsToSchedule(15))
    assertEquals(listOf(30, 0), com.example.alarms.AlarmScheduler.getOffsetsToSchedule(30))
    assertEquals(listOf(0), com.example.alarms.AlarmScheduler.getOffsetsToSchedule(0))

    val code15 = com.example.alarms.AlarmScheduler.computeAlarmRequestCode("sched-1", 15)
    val code0 = com.example.alarms.AlarmScheduler.computeAlarmRequestCode("sched-1", 0, setOf(code15))
    assertTrue(code15 > 0)
    assertTrue(code0 > 0)
    assertTrue(code15 != code0)
    val resolvedCollision = com.example.alarms.AlarmScheduler.computeAlarmRequestCode("sched-1", 15, setOf(code15))
    assertTrue(resolvedCollision != code15)
  }

  @Test
  fun testComputeNextTriggerMillis() {
    val cal = java.util.Calendar.getInstance().apply {
      set(2026, java.util.Calendar.NOVEMBER, 9, 7, 0, 0) // Monday 07:00
      set(java.util.Calendar.MILLISECOND, 0)
    }
    val beforeTrigger = com.example.alarms.AlarmScheduler.computeNextTriggerMillis(1, "08:00:00", 15, cal.timeInMillis)!!
    val outCal1 = java.util.Calendar.getInstance().apply { timeInMillis = beforeTrigger }
    assertEquals(java.util.Calendar.MONDAY, outCal1.get(java.util.Calendar.DAY_OF_WEEK))
    assertEquals(7, outCal1.get(java.util.Calendar.HOUR_OF_DAY))
    assertEquals(45, outCal1.get(java.util.Calendar.MINUTE))

    // After 07:45 -> rolls over 7 days to next Monday 07:45
    cal.set(java.util.Calendar.MINUTE, 50)
    val nextWeek = com.example.alarms.AlarmScheduler.computeNextTriggerMillis(1, "08:00", 15, cal.timeInMillis)!!
    val outCal2 = java.util.Calendar.getInstance().apply { timeInMillis = nextWeek }
    assertEquals(java.util.Calendar.MONDAY, outCal2.get(java.util.Calendar.DAY_OF_WEEK))
    assertEquals(16, outCal2.get(java.util.Calendar.DAY_OF_MONTH))
    assertEquals(7, outCal2.get(java.util.Calendar.HOUR_OF_DAY))
    assertEquals(45, outCal2.get(java.util.Calendar.MINUTE))

    // Midnight rollover (Monday 00:05 with 15 min offset -> Sunday 23:50)
    cal.set(2026, java.util.Calendar.NOVEMBER, 8, 22, 0, 0) // Sunday 22:00
    val midnightCross = com.example.alarms.AlarmScheduler.computeNextTriggerMillis(1, "00:05", 15, cal.timeInMillis)!!
    val outCal3 = java.util.Calendar.getInstance().apply { timeInMillis = midnightCross }
    assertEquals(java.util.Calendar.SUNDAY, outCal3.get(java.util.Calendar.DAY_OF_WEEK))
    assertEquals(23, outCal3.get(java.util.Calendar.HOUR_OF_DAY))
    assertEquals(50, outCal3.get(java.util.Calendar.MINUTE))

    // Exact start time (0 offset) and invalid inputs
    assertEquals(null, com.example.alarms.AlarmScheduler.computeNextTriggerMillis(9, "08:00", 0, cal.timeInMillis))
    assertEquals(null, com.example.alarms.AlarmScheduler.computeNextTriggerMillis(1, "bad", 0, cal.timeInMillis))
  }

  @Test
  fun testFormatAlarmTextNeverShowsNull() {
    val (title, body) = com.example.alarms.AlarmNotificationHelper.formatAlarmText(
      courseCode = "MGE B101",
      title = "Applied Geomatics",
      startTime = "08:00:00",
      endTime = "10:00:00",
      room = "LTW",
      lecturerName = "Dr. B. Othoche",
      offsetMinutes = 15,
      isSnooze = false
    )
    assertEquals("MGE B101 starts in 15 min - LTW - Dr. B. Othoche", body)
    assertTrue(title.contains("MGE B101"))
    assertTrue(title.contains("08:00 - 10:00"))

    val (nullTitle, nullBody) = com.example.alarms.AlarmNotificationHelper.formatAlarmText(
      courseCode = null,
      title = "null",
      startTime = "null",
      endTime = null,
      room = "null",
      lecturerName = null,
      offsetMinutes = 0,
      isSnooze = false
    )
    assertEquals("Class starts now", nullBody)
    assertFalse(nullTitle.contains("null", ignoreCase = true))
    assertFalse(nullBody.contains("null", ignoreCase = true))
  }
}
