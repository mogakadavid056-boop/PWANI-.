package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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

  @Test
  fun `buildSignUpMetadata includes full name and admission number only`() {
    val stuMeta = com.example.data.SupabaseAuthService.buildSignUpMetadata(
      fullName = "John Student",
      admissionNumber = "SCT221-0100/2024"
    )
    assertEquals("John Student", stuMeta.getString("full_name"))
    assertEquals("SCT221-0100/2024", stuMeta.getString("admission_number"))
    org.junit.Assert.assertFalse(stuMeta.has("requested_role"))
    org.junit.Assert.assertFalse(stuMeta.has("staff_number"))
    org.junit.Assert.assertFalse(stuMeta.has("role"))
  }

  @Test
  fun `parseUserRoleInfo reads user_roles and safely defaults to student`() {
    val emptyInfo = com.example.data.SupabaseAuthService.parseUserRoleInfo("[]")
    assertEquals("student", emptyInfo.role)
    org.junit.Assert.assertFalse(emptyInfo.isVerified)

    val unverifiedLec = com.example.data.SupabaseAuthService.parseUserRoleInfo(
      """[{"role":"lecturer","verified":false}]"""
    )
    assertEquals("lecturer", unverifiedLec.role)
    org.junit.Assert.assertFalse(unverifiedLec.isVerified)

    val verifiedLec = com.example.data.SupabaseAuthService.parseUserRoleInfo(
      """[{"role":"lecturer","verified":true}]"""
    )
    assertEquals("lecturer", verifiedLec.role)
    org.junit.Assert.assertTrue(verifiedLec.isVerified)
  }

  @Test
  fun `adminUserItem exposes restored role helpers`() {
    val lec = com.example.data.AdminUserItem("1", "Dr Ada", "p@uni.edu", "", "lecturer", isVerified = false)
    val repUser = com.example.data.AdminUserItem("2", "Rep Alice", "r@uni.edu", "A1", "class_rep", isVerified = false)
    val studentUser = com.example.data.AdminUserItem("3", "Stu Bob", "b@uni.edu", "A2", "student", isVerified = false)

    org.junit.Assert.assertTrue(lec.isLecturer)
    org.junit.Assert.assertFalse(lec.isVerified)
    org.junit.Assert.assertTrue(repUser.isClassRep)
    org.junit.Assert.assertTrue(studentUser.isStudent)
  }

  @Test
  fun `alarmPreferences persists master, reminder, course toggles, and schedules`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.alarms.AlarmPreferences.setMasterEnabled(context, false)
    com.example.alarms.AlarmPreferences.setReminderMinutes(context, 30)
    com.example.alarms.AlarmPreferences.setCourseEnabled(context, "CS101", false)
    val item = com.example.data.ScheduleItem(
      id = "s-alarm-1", title = "Algorithms", lecturerName = "Dr. Ada",
      dayOfWeek = 2, startTime = "09:00", endTime = "11:00", room = "LT1",
      classId = "cls-1", courseId = "c-1", courseCode = "CS101"
    )
    com.example.alarms.AlarmPreferences.saveCachedSchedules(context, listOf(item))

    val state = com.example.alarms.AlarmPreferences.refreshState(context)
    org.junit.Assert.assertFalse(state.masterEnabled)
    assertEquals(30, state.reminderMinutes)
    org.junit.Assert.assertFalse(state.isCourseEnabled("CS101"))
    assertEquals(1, state.cachedSchedules.size)
    assertEquals("CS101", state.cachedSchedules[0].courseCode)
  }

  @Test
  fun `timetableAlarmSync parseSchedulesArray handles null fields cleanly`() {
    val json = """
      [
        {
          "id": "sch-1",
          "class_id": "cls-1",
          "course_id": "crs-1",
          "title": null,
          "lecturer_name": "null",
          "day_of_week": 3,
          "start_time": "10:30:00",
          "end_time": "12:30:00",
          "room": "LTW",
          "status": null,
          "courses": { "id": "crs-1", "code": "MGE B101", "name": "Surveying" }
        }
      ]
    """.trimIndent()
    val items = com.example.alarms.TimetableAlarmSync.parseSchedulesArray(json)
    assertEquals(1, items.size)
    assertEquals("MGE B101", items[0].courseCode)
    assertEquals("Surveying", items[0].title)
    assertEquals("", items[0].lecturerName)
    assertEquals("scheduled", items[0].status)
  }

  @Test
  fun `alarmScheduler schedules active classes, skips cancelled, and handles snooze and boot`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.alarms.AlarmPreferences.setMasterEnabled(context, true)
    com.example.alarms.AlarmPreferences.setReminderMinutes(context, 15)
    val active = com.example.data.ScheduleItem(
      id = "sch-active", title = "Geomatics", lecturerName = "Dr. B. Othoche",
      dayOfWeek = 1, startTime = "08:00", endTime = "10:00", room = "LTW",
      status = "scheduled", classId = "cls-1", courseId = "crs-1", courseCode = "MGE B101"
    )
    val cancelled = active.copy(id = "sch-cancel", status = "cancelled")
    com.example.alarms.AlarmScheduler.rescheduleAll(context, listOf(active, cancelled))

    val registry = com.example.alarms.AlarmPreferences.getScheduledRegistry(context)
    assertEquals(2, registry.size) // 15 min before + 0 min start; cancelled excluded

    val receiver = com.example.alarms.AlarmReceiver()
    val alarmIntent = com.example.alarms.AlarmReceiver.buildAlarmIntent(context, registry[0])
    receiver.onReceive(context, alarmIntent)

    val snoozeIntent = com.example.alarms.AlarmReceiver.buildActionIntent(
      context, com.example.alarms.AlarmReceiver.ACTION_SNOZE, registry[0], registry[0].requestCode
    )
    receiver.onReceive(context, snoozeIntent)

    val dismissIntent = com.example.alarms.AlarmReceiver.buildActionIntent(
      context, com.example.alarms.AlarmReceiver.ACTION_DISMISS, registry[0], registry[0].requestCode
    )
    receiver.onReceive(context, dismissIntent)

    com.example.alarms.BootReceiver().onReceive(context, android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED))
    assertEquals(2, com.example.alarms.AlarmPreferences.getScheduledRegistry(context).size)

    com.example.alarms.AlarmScheduler.cancelAllAlarms(context)
    assertEquals(0, com.example.alarms.AlarmPreferences.getScheduledRegistry(context).size)
  }

  @Test
  fun `sessionManager saves and clears pending registration by email`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.data.SessionManager(context)
    val data = com.example.data.SessionManager.PendingRegistrationData(
      email = "Pending@Uni.Edu",
      fullName = "Jane Pending",
      isLecturer = false,
      idNumber = "ADM-101",
      phone = "+254700000000",
      universityId = "uni-1",
      schoolId = "sch-1",
      programmeId = "prg-1",
      yearOfStudy = 2,
      knowsDetails = true,
      classCode = "",
      acceptedTermsAt = "2026-11-18T12:00:00Z",
      termsVersion = "1.0"
    )
    manager.savePendingRegistration(data)
    val loaded = manager.getPendingRegistration("pending@uni.edu")
    org.junit.Assert.assertNotNull(loaded)
    assertEquals("Jane Pending", loaded!!.fullName)
    assertEquals("ADM-101", loaded.idNumber)
    assertEquals(2, loaded.yearOfStudy)
    org.junit.Assert.assertNull(manager.getPendingRegistration("other@uni.edu"))
    manager.clearPendingRegistration()
    org.junit.Assert.assertNull(manager.getPendingRegistration("pending@uni.edu"))
  }
}

