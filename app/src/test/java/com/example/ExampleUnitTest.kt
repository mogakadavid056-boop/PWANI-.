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
    val mNotes = MaterialItem("1", "Lecture 1", "Basics", "notes", "https://example.com/notes.pdf", "2026-10-10T12:00:00Z")
    assertEquals("Notes", mNotes.typeLabel)
    assertEquals("2026-10-10", mNotes.formattedDate)

    val mSlides = MaterialItem("2", "Deck 2", "", "slides", "https://example.com/deck.pptx", "2026-10-11")
    assertEquals("Slides", mSlides.typeLabel)

    val mPaper = MaterialItem("3", "Midterm 2025", "", "past_paper", "https://example.com/paper.pdf", "")
    assertEquals("Past Paper", mPaper.typeLabel)

    val mVideo = MaterialItem("4", "Recap", "", "video", "https://youtube.com/watch?v=123", "")
    assertEquals("Video", mVideo.typeLabel)

    val mLink = MaterialItem("5", "Doc Reference", "", "link", "https://developer.android.com", "")
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
    val m1 = MaterialItem("1", "Computer Systems Lecture", "", "notes", "https://a.com", "")
    val m2 = MaterialItem("2", "Database Design", "", "slides", "https://b.com", "")
    val m3 = MaterialItem("3", "Calculus Past Paper", "", "past_paper", "https://c.com", "")

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
}
