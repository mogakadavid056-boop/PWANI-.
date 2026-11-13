package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ClassRep", appName)
  }

  @Test
  fun `create AuthViewModel via AndroidViewModelFactory`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    val viewModel = factory.create(com.example.ui.AuthViewModel::class.java)
    org.junit.Assert.assertNotNull(viewModel)
  }

  @Test
  fun `classesViewModel initial state is empty`() {
    val viewModel = com.example.ui.ClassesViewModel()
    org.junit.Assert.assertTrue(viewModel.uiState.value.classes.isEmpty())
  }

  @Test
  fun `noticesViewModel initial state is empty`() {
    val viewModel = com.example.ui.NoticesViewModel()
    org.junit.Assert.assertTrue(viewModel.uiState.value.notices.isEmpty())
    org.junit.Assert.assertNull(viewModel.uiState.value.selectedClass)
  }

  @Test
  fun `attendanceViewModel initial state is empty`() {
    val viewModel = com.example.ui.AttendanceViewModel()
    org.junit.Assert.assertTrue(viewModel.uiState.value.staffSessions.isEmpty())
    org.junit.Assert.assertTrue(viewModel.uiState.value.openSessions.isEmpty())
    org.junit.Assert.assertNull(viewModel.uiState.value.activeSessionCode)
  }

  @Test
  fun `membersViewModel initial state is empty`() {
    val viewModel = com.example.ui.MembersViewModel()
    org.junit.Assert.assertTrue(viewModel.uiState.value.members.isEmpty())
    org.junit.Assert.assertEquals("", viewModel.uiState.value.classId)
  }

  @Test
  fun `groupsViewModel initial state is empty`() {
    val viewModel = com.example.ui.GroupsViewModel()
    org.junit.Assert.assertTrue(viewModel.uiState.value.groups.isEmpty())
    org.junit.Assert.assertNull(viewModel.uiState.value.activeGroup)
  }

  @Test
  fun `launch MainActivity with viewModels`() {
    org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
  }

  @Test
  fun `jsonHelper never emits literal null`() {
    val obj = org.json.JSONObject("""{"a": null, "b": "null", "c": "  ", "d": "Valid"}""")
    assertEquals("", com.example.util.JsonHelper.str(obj, "a"))
    assertEquals("", com.example.util.JsonHelper.str(obj, "b"))
    assertEquals("", com.example.util.JsonHelper.str(obj, "c"))
    assertEquals("", com.example.util.JsonHelper.str(obj, "missing"))
    assertEquals("Valid", com.example.util.JsonHelper.str(obj, "d"))
  }

  @Test
  fun `attendanceRegisterParser parses server permissions and null fields safely`() {
    val json = """
      {
        "session_id": "s1",
        "register_no": "REG-001",
        "title": "Class Attendance",
        "status": "open",
        "course_name": null,
        "venue": "null",
        "date_text": "13 Nov 2026",
        "time_text": "08:00 - 10:00",
        "generated_text": "13 Nov 2026 08:05",
        "missing_details": true,
        "permissions": {
          "can_manage": false,
          "can_edit_records": false,
          "can_edit_details": false,
          "can_close": false,
          "can_void": false,
          "can_delete": false,
          "can_sign_out": true,
          "can_sign_prefect": false,
          "can_sign_lecturer": false
        },
        "rows": [
          {
            "n": 1,
            "student_id": "u1",
            "record_id": "r1",
            "full_name": "Jane Doe",
            "admission_number": "SCT221-0001/2023",
            "is_prefect": false,
            "present": true,
            "manual": true,
            "time_in_text": "08:02",
            "time_out_text": null,
            "signature": null
          }
        ]
      }
    """.trimIndent()
    val detail = com.example.data.AttendanceRegisterParser.parseRegisterDetail(json, "s1")
    assertEquals(null, detail.courseName)
    assertEquals(null, detail.venue)
    org.junit.Assert.assertTrue(detail.missingDetails)
    org.junit.Assert.assertFalse(detail.permissions.canManage)
    org.junit.Assert.assertFalse(detail.permissions.canEditRecords)
    org.junit.Assert.assertTrue(detail.permissions.canSignOut)
    assertEquals(1, detail.rows.size)
    assertEquals("r1", detail.rows[0].recordId)
    org.junit.Assert.assertTrue(detail.rows[0].manual)
    assertEquals("", detail.rows[0].timeOutText)
  }
}
