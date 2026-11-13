package com.example.data

import com.example.util.JsonHelper
import org.json.JSONArray
import org.json.JSONObject

object AttendanceRegisterParser {
  fun parseRegisterList(bodyStr: String): List<AttendanceRegisterListItem> {
    if (bodyStr.isBlank()) return emptyList()
    val array = JSONArray(bodyStr)
    return List(array.length()) { i ->
      val obj = array.getJSONObject(i)
      AttendanceRegisterListItem(
        sessionId = JsonHelper.str(obj, "session_id"),
        registerNo = JsonHelper.str(obj, "register_no").ifBlank { "REG-${i + 1}" },
        title = JsonHelper.str(obj, "title").ifBlank { "Attendance Register" },
        courseName = JsonHelper.firstStr(obj, "course_name", "course_title").ifBlank { null },
        startsAt = JsonHelper.str(obj, "starts_at"),
        dateText = JsonHelper.str(obj, "date_text"),
        timeText = JsonHelper.str(obj, "time_text"),
        status = JsonHelper.str(obj, "status").ifBlank { "open" }.lowercase(),
        presentCount = JsonHelper.intVal(obj, "present_count", 0),
        canManage = JsonHelper.bool(obj, "can_manage", false),
        canDelete = JsonHelper.bool(obj, "can_delete", false)
      )
    }
  }

  fun parseRegisterDetail(bodyStr: String, fallbackSessionId: String): AttendanceRegisterDetail {
    val obj = if (bodyStr.startsWith("[")) {
      JSONArray(bodyStr).optJSONObject(0) ?: JSONObject()
    } else {
      JSONObject(bodyStr)
    }

    val permObj = obj.optJSONObject("permissions")
    val permissions = RegisterPermissions(
      canManage = JsonHelper.bool(permObj, "can_manage", false),
      canEditRecords = JsonHelper.bool(permObj, "can_edit_records", false),
      canEditDetails = JsonHelper.bool(permObj, "can_edit_details", false),
      canClose = JsonHelper.bool(permObj, "can_close", false),
      canVoid = JsonHelper.bool(permObj, "can_void", false),
      canDelete = JsonHelper.bool(permObj, "can_delete", false),
      canSignOut = JsonHelper.bool(permObj, "can_sign_out", false),
      canSignPrefect = JsonHelper.bool(permObj, "can_sign_prefect", false),
      canSignLecturer = JsonHelper.bool(permObj, "can_sign_lecturer", false)
    )

    val rowsArray = obj.optJSONArray("rows") ?: JSONArray()
    val rowsList = mutableListOf<AttendanceRegisterRowItem>()
    for (i in 0 until rowsArray.length()) {
      val r = rowsArray.getJSONObject(i)
      rowsList.add(
        AttendanceRegisterRowItem(
          n = JsonHelper.intVal(r, "n", i + 1),
          studentId = JsonHelper.str(r, "student_id"),
          recordId = JsonHelper.firstStr(r, "record_id", "id"),
          fullName = JsonHelper.str(r, "full_name").ifBlank { "Student" },
          admissionNumber = JsonHelper.str(r, "admission_number").ifBlank { "-" },
          isPrefect = JsonHelper.bool(r, "is_prefect", false),
          present = JsonHelper.bool(r, "present", false),
          manual = JsonHelper.bool(r, "manual", false),
          timeIn = JsonHelper.strOrNull(r, "time_in"),
          timeOut = JsonHelper.strOrNull(r, "time_out"),
          timeInText = JsonHelper.str(r, "time_in_text"),
          timeOutText = JsonHelper.str(r, "time_out_text"),
          signature = JsonHelper.strOrNull(r, "signature")
        )
      )
    }

    return AttendanceRegisterDetail(
      sessionId = JsonHelper.str(obj, "session_id").ifBlank { fallbackSessionId },
      registerNo = JsonHelper.str(obj, "register_no").ifBlank { "REG" },
      title = JsonHelper.str(obj, "title").ifBlank { "Class Attendance Register" },
      status = JsonHelper.str(obj, "status").ifBlank { "open" }.lowercase(),
      voidReason = JsonHelper.strOrNull(obj, "void_reason"),
      courseName = JsonHelper.firstStr(obj, "course_name", "course_title").ifBlank { null },
      courseCode = JsonHelper.strOrNull(obj, "course_code"),
      className = JsonHelper.strOrNull(obj, "class_name"),
      yearOfStudy = JsonHelper.strOrNull(obj, "year_of_study"),
      lecturerName = JsonHelper.strOrNull(obj, "lecturer_name"),
      venue = JsonHelper.firstStr(obj, "venue", "venue_text").ifBlank { null },
      dateText = JsonHelper.str(obj, "date_text"),
      timeText = JsonHelper.str(obj, "time_text"),
      generatedText = JsonHelper.str(obj, "generated_text"),
      prefectName = JsonHelper.strOrNull(obj, "prefect_name"),
      prefectSignature = JsonHelper.strOrNull(obj, "prefect_signature"),
      prefectSignedText = JsonHelper.str(obj, "prefect_signed_text"),
      lecturerSignature = JsonHelper.strOrNull(obj, "lecturer_signature"),
      lecturerSignedText = JsonHelper.str(obj, "lecturer_signed_text"),
      presentCount = JsonHelper.intVal(obj, "present_count", 0),
      total = JsonHelper.intVal(obj, "total", rowsList.size),
      missingDetails = JsonHelper.bool(obj, "missing_details", false),
      permissions = permissions,
      rows = rowsList
    )
  }

  fun parseClassUnits(bodyStr: String): List<ClassUnitItem> {
    if (bodyStr.isBlank()) return emptyList()
    val array = JSONArray(bodyStr)
    return List(array.length()) { i ->
      val obj = array.getJSONObject(i)
      ClassUnitItem(
        courseId = JsonHelper.firstStr(obj, "course_id", "id"),
        courseName = JsonHelper.firstStr(obj, "course_name", "course_title", "name"),
        courseCode = JsonHelper.str(obj, "course_code"),
        lecturerName = JsonHelper.str(obj, "lecturer_name")
      )
    }
  }
}
