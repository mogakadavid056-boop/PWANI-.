package com.example.admin.data

import com.example.admin.util.AdminJsonHelper
import com.example.admin.util.AdminRoleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject

class AdminMonitorContentRepository(
  private val client: OkHttpClient = SupabaseAdminConfig.sharedHttpClient
) {
  suspend fun fetchNotices(classId: String, accessToken: String): Result<List<AdminNoticeItem>> =
    withContext(Dispatchers.IO) {
      val q = "notices?class_id=eq.$classId&select=id,title,content,category,is_urgent,is_pinned,created_at&order=is_pinned.desc,created_at.desc"
      AdminHttpHelper.getRest(client, q, accessToken).mapCatching { raw ->
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
          val o = arr.getJSONObject(i)
          AdminNoticeItem(
            id = AdminJsonHelper.str(o, "id"),
            title = AdminJsonHelper.str(o, "title"),
            content = AdminJsonHelper.str(o, "content"),
            category = AdminJsonHelper.str(o, "category").ifBlank { "general" },
            isUrgent = AdminJsonHelper.bool(o, "is_urgent", false),
            isPinned = AdminJsonHelper.bool(o, "is_pinned", false),
            createdAt = AdminJsonHelper.str(o, "created_at")
          )
        }
      }
    }

  suspend fun createNotice(classId: String, title: String, content: String, category: String, urgent: Boolean, pinned: Boolean, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply {
        put("target_class_id", classId)
        put("target_course_id", JSONObject.NULL)
        put("notice_title", title.trim())
        put("notice_content", content.trim())
        put("notice_category", category.trim().ifBlank { "general" })
        put("urgent", urgent)
        put("pinned", pinned)
      }
      AdminHttpHelper.postRpc(client, "create_notice", body, accessToken).map { }
    }

  suspend fun editNotice(noticeId: String, title: String, content: String, category: String, urgent: Boolean, pinned: Boolean, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply {
        put("title", title.trim())
        put("content", content.trim())
        put("category", category.trim().ifBlank { "general" })
        put("is_urgent", urgent)
        put("is_pinned", pinned)
      }
      AdminHttpHelper.patchRest(client, "notices?id=eq.$noticeId", body, accessToken).map { }
    }

  suspend fun deleteNotice(noticeId: String, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      AdminHttpHelper.deleteRest(client, "notices?id=eq.$noticeId", accessToken).map { }
    }

  suspend fun listClassMembers(classId: String, accessToken: String): Result<List<AdminClassMemberItem>> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("target_class_id", classId) }
      AdminHttpHelper.postRpc(client, "list_class_members", body, accessToken).mapCatching { raw ->
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
          val o = arr.getJSONObject(i)
          val rawRole = AdminJsonHelper.str(o, "role")
          val memberRole = AdminJsonHelper.firstStr(o, "member_role", "role").ifBlank { "student" }
          val isRep = AdminJsonHelper.bool(o, "is_rep", false) ||
            AdminJsonHelper.bool(o, "is_representative", false) ||
            AdminRoleHelper.isClassRep(memberRole) || AdminRoleHelper.isClassRep(rawRole)
          AdminClassMemberItem(
            userId = AdminJsonHelper.firstStr(o, "user_id", "id", "student_id"),
            fullName = AdminJsonHelper.firstStr(o, "full_name", "name", "student_name").ifBlank { "Member" },
            admissionNumber = AdminJsonHelper.firstStr(o, "admission_number", "admission_no", "reg_no").ifBlank { "-" },
            isRep = isRep,
            role = rawRole.ifBlank { memberRole },
            memberRole = memberRole
          )
        }
      }
    }

  suspend fun setClassRep(classId: String, userId: String, appoint: Boolean, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val rpc = if (appoint) "appoint_class_rep" else "remove_class_rep"
      val body = JSONObject().apply { put("target_class_id", classId); put("target_user_id", userId) }
      AdminHttpHelper.postRpc(client, rpc, body, accessToken).map { }
    }

  suspend fun removeFromClass(classId: String, userId: String, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      AdminHttpHelper.deleteRest(client, "class_members?class_id=eq.$classId&user_id=eq.$userId", accessToken).map { }
    }

  suspend fun listPendingLecturers(classId: String, accessToken: String): Result<List<AdminPendingLecturerItem>> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("target_class_id", classId) }
      AdminHttpHelper.postRpc(client, "list_pending_lecturers", body, accessToken).mapCatching { raw ->
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
          val o = arr.getJSONObject(i)
          AdminPendingLecturerItem(
            userId = AdminJsonHelper.firstStr(o, "user_id", "id"),
            fullName = AdminJsonHelper.firstStr(o, "full_name", "name").ifBlank { "Lecturer" },
            email = AdminJsonHelper.str(o, "email"),
            staffNumber = AdminJsonHelper.firstStr(o, "staff_number", "admission_number", "staff_no")
          )
        }
      }
    }

  suspend fun reviewJoinRequest(classId: String, userId: String, approve: Boolean, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val q = "class_members?class_id=eq.$classId&user_id=eq.$userId&status=eq.pending"
      if (approve) {
        AdminHttpHelper.patchRest(client, q, JSONObject().apply { put("status", "active") }, accessToken).map { }
      } else {
        AdminHttpHelper.deleteRest(client, q, accessToken).map { }
      }
    }

  suspend fun fetchSchedules(classId: String, accessToken: String): Result<List<AdminScheduleItem>> =
    withContext(Dispatchers.IO) {
      val q = "schedules?class_id=eq.$classId&select=*&order=day_of_week.asc,start_time.asc"
      AdminHttpHelper.getRest(client, q, accessToken).mapCatching { raw ->
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
          val o = arr.getJSONObject(i)
          AdminScheduleItem(
            id = AdminJsonHelper.str(o, "id"),
            title = AdminJsonHelper.str(o, "title"),
            lecturerName = AdminJsonHelper.str(o, "lecturer_name"),
            dayOfWeek = AdminJsonHelper.intVal(o, "day_of_week", 1),
            startTime = AdminJsonHelper.str(o, "start_time"),
            endTime = AdminJsonHelper.str(o, "end_time"),
            room = AdminJsonHelper.str(o, "room"),
            status = AdminJsonHelper.str(o, "status").ifBlank { "scheduled" },
            statusNote = AdminJsonHelper.firstStr(o, "status_note", "note")
          )
        }
      }
    }

  suspend fun saveSchedule(scheduleId: String?, classId: String, title: String, day: Int, start: String, end: String, room: String, lecturer: String, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply {
        if (scheduleId == null) put("class_id", classId)
        put("title", title.trim())
        put("day_of_week", day)
        put("start_time", start.trim())
        put("end_time", end.trim())
        put("room", room.trim())
        put("lecturer_name", lecturer.trim())
      }
      if (scheduleId == null) {
        AdminHttpHelper.postRest(client, "schedules", body, accessToken).map { }
      } else {
        AdminHttpHelper.patchRest(client, "schedules?id=eq.$scheduleId", body, accessToken).map { }
      }
    }

  suspend fun deleteSchedule(scheduleId: String, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      AdminHttpHelper.deleteRest(client, "schedules?id=eq.$scheduleId", accessToken).map { }
    }
}
