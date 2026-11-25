package com.example.admin

import com.example.admin.data.AdminExamResultsPdfExporter
import com.example.admin.data.AdminExamsRepository
import com.example.admin.data.AdminQuizResultRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdminExamsTest {
  @Test
  fun computeRankedEntries_appliesCompetitionRankingAndNaturalAdmissionTieBreak() {
    val rows = listOf(
      AdminQuizResultRow("u1", "Student B", "CS/10", score = 40.0, total = 50.0, markingStatus = "released"),
      AdminQuizResultRow("u2", "Student A", "CS/2", score = 40.0, total = 50.0, markingStatus = "released"),
      AdminQuizResultRow("u3", "Student C", "CS/3", score = 30.0, total = 50.0, markingStatus = "released", focusLossCount = 2),
      AdminQuizResultRow("u4", "Student D", "CS/4", score = null, total = 50.0, markingStatus = "pending")
    )

    val ranked = AdminExamResultsPdfExporter.computeRankedEntries(rows, 50.0)
    assertEquals(1, ranked[0].rank)
    assertEquals("CS/2", ranked[0].row.admissionNumber)
    assertEquals("A", ranked[0].gradeLabel)
    assertEquals(1, ranked[1].rank)
    assertEquals("CS/10", ranked[1].row.admissionNumber)
    assertEquals(3, ranked[2].rank)
    assertEquals("C", ranked[2].gradeLabel)
    assertEquals(4, ranked[3].rank)
    assertEquals("Pend", ranked[3].gradeLabel)
  }

  @Test
  fun adminExamsRepository_parsesFlagsAndExpiredSweepCounts() {
    val flags = AdminExamsRepository.parseExamFlags(
      """[{"student_id":"st-1","full_name":"Grace","admission_number":"CS/09","exit_violations":2,"auto_submitted":true,"auto_submit_reason":"exit_limit","late":false,"submitted_at":"2026-11-24T10:00:00Z"}]"""
    )
    assertEquals(1, flags.size)
    assertEquals("Grace", flags[0].fullName)
    assertEquals("exit_limit", flags[0].autoSubmitReason)
    assertTrue(flags[0].autoSubmitted)
    assertEquals(2, flags[0].exitViolations)

    assertEquals(7, AdminExamsRepository.parseScalarInt("7"))
    assertEquals(0, AdminExamsRepository.parseScalarInt("null"))
  }
}
