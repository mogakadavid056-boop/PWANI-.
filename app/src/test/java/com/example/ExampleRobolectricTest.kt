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
}
