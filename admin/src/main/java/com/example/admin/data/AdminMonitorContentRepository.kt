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
      val rpc = AdminHttpHelper.postRpc(client, "list_class_members", body, accessToken)
      if (rpc.isSuccess) {
        val parsed = runCatching { parseRpcMembers(rpc.getOrThrow()) }
        if (parsed.isSuccess) return@withContext parsed
      }
      val memRes = AdminHttpHelper.getRest(client, "class_members?class_id=eq.$classId&select=*", accessToken)
      if (memRes.isFailure) return@withContext Result.failure(memRes.exceptionOrNull() ?: Exception("Failed to load members"))
      val profiles = AdminHttpHelper.getRest(client, "profiles?select=*&limit=500", accessToken).getOrDefault("[]")
      val roles = AdminHttpHelper.getRest(client, "user_roles?select=user_id,role&limit=500", accessToken).getOrDefault("[]")
      val reps = AdminHttpHelper.getRest(client, "student_representatives?class_id=eq.$classId&active=eq.true&select=user_id", accessToken).getOrDefault("[]")
      runCatching { mergeRestMembers(memRes.getOrThrow(), profiles, roles, reps) }
    }

  suspend fun setClassRep(classId: String, userId: String, appoint: Boolean, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val rpc = if (appoint) "appoint_class_rep" else "remove_class_rep"
      val body = JSONObject().apply { put("target_class_id", classId); put("target_user_id", userId) }
      val res = AdminHttpHelper.postRpc(client, rpc, body, accessToken)
      if (res.isSuccess) return@withContext Result.success(Unit)
      if (appoint) {
        AdminHttpHelper.postRest(client, "student_representatives", JSONObject().apply { put("class_id", classId); put("user_id", userId); put("active", true) }, accessToken)
      } else {
        AdminHttpHelper.deleteRest(client, "student_representatives?class_id=eq.$classId&user_id=eq.$userId", accessToken)
      }.map { }
    }

  suspend fun removeFromClass(classId: String, userId: String, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      AdminHttpHelper.deleteRest(client, "class_members?class_id=eq.$classId&user_id=eq.$userId", accessToken).map { }
    }

  suspend fun listPendingLecturers(classId: String, accessToken: String): Result<List<AdminPendingLecturerItem>> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("target_class_id", classId) }
      val rpc = AdminHttpHelper.postRpc(client, "list_pending_lecturers", body, accessToken)
      if (rpc.isSuccess) {
        val parsed = runCatching {
          val arr = JSONArray(rpc.getOrThrow())
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
        if (parsed.isSuccess) return@withContext parsed
      }
      listGlobalPendingJoinRequests(accessToken).map { all ->
        all.filter { it.classId == classId }.map {
          AdminPendingLecturerItem(it.userId, it.fullName, it.email, it.staffNumber)
        }
      }
    }

  suspend fun listGlobalPendingJoinRequests(accessToken: String): Result<List<AdminGlobalJoinRequestItem>> =
    withContext(Dispatchers.IO) {
      val rpc = AdminHttpHelper.postRpc(client, "admin_list_pending_join_requests", JSONObject(), accessToken)
      if (rpc.isSuccess) {
        val parsed = runCatching {
          val arr = JSONArray(rpc.getOrThrow())
          List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            AdminGlobalJoinRequestItem(
              classId = AdminJsonHelper.str(o, "class_id"),
              className = AdminJsonHelper.str(o, "class_name").ifBlank { "Class" },
              userId = AdminJsonHelper.firstStr(o, "user_id", "id"),
              fullName = AdminJsonHelper.firstStr(o, "full_name", "name").ifBlank { "Lecturer" },
              email = AdminJsonHelper.str(o, "email"),
              staffNumber = AdminJsonHelper.firstStr(o, "staff_number", "admission_number")
            )
          }
        }
        if (parsed.isSuccess) return@withContext parsed
      }
      val memRes = AdminHttpHelper.getRest(client, "class_members?status=eq.pending&select=class_id,user_id,classes(name)", accessToken)
      if (memRes.isFailure) return@withContext Result.failure(memRes.exceptionOrNull() ?: Exception("Failed to load pending requests"))
      val profsRaw = AdminHttpHelper.getRest(client, "profiles?select=*&limit=500", accessToken).getOrDefault("[]")
      runCatching {
        val profMap = mutableMapOf<String, JSONObject>()
        val pArr = JSONArray(profsRaw)
        for (i in 0 until pArr.length()) {
          val p = pArr.optJSONObject(i) ?: continue
          val pid = AdminJsonHelper.str(p, "id")
          if (pid.isNotBlank()) profMap[pid] = p
        }
        val mArr = JSONArray(memRes.getOrThrow())
        List(mArr.length()) { i ->
          val o = mArr.getJSONObject(i)
          val uid = AdminJsonHelper.str(o, "user_id")
          val p = profMap[uid]
          AdminGlobalJoinRequestItem(
            classId = AdminJsonHelper.str(o, "class_id"),
            className = AdminJsonHelper.str(o.optJSONObject("classes"), "name").ifBlank { "Class" },
            userId = uid,
            fullName = AdminJsonHelper.firstStr(p, "full_name", "name").ifBlank { "Lecturer" },
            email = AdminJsonHelper.str(p, "email"),
            staffNumber = AdminJsonHelper.firstStr(p, "staff_number", "admission_number")
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

  companion object {
    fun parseRpcMembers(raw: String): List<AdminClassMemberItem> {
      val trimmed = raw.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyList()
      val arr = JSONArray(trimmed)
      return buildList {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          val roleRaw = AdminRoleHelper.normalizeRole(AdminJsonHelper.firstStr(o, "role", "member_role")).ifBlank { "student" }
          val memberRole = AdminRoleHelper.normalizeRole(AdminJsonHelper.firstStr(o, "member_role", "role")).ifBlank { roleRaw }
          add(
            AdminClassMemberItem(
              userId = AdminJsonHelper.firstStr(o, "user_id", "id"),
              fullName = AdminJsonHelper.firstStr(o, "full_name", "name").ifBlank { "Member" },
              admissionNumber = AdminJsonHelper.firstStr(o, "admission_number", "staff_number"),
              isRep = AdminJsonHelper.bool(o, "is_rep", AdminJsonHelper.bool(o, "is_prefect", false)),
              role = roleRaw,
              memberRole = memberRole
            )
          )
        }
      }
    }

    fun mergeRestMembers(
      membersJson: String,
      profilesJson: String,
      rolesJson: String,
      repsJson: String
    ): List<AdminClassMemberItem> {
      val profMap = mutableMapOf<String, JSONObject>()
      val pArr = runCatching { JSONArray(profilesJson) }.getOrNull() ?: JSONArray()
      for (i in 0 until pArr.length()) {
        val p = pArr.optJSONObject(i) ?: continue
        val pid = AdminJsonHelper.firstStr(p, "id", "user_id")
        if (pid.isNotBlank()) profMap[pid] = p
      }
      val roleMap = mutableMapOf<String, String>()
      val rArr = runCatching { JSONArray(rolesJson) }.getOrNull() ?: JSONArray()
      for (i in 0 until rArr.length()) {
        val r = rArr.optJSONObject(i) ?: continue
        val uid = AdminJsonHelper.firstStr(r, "user_id", "id")
        if (uid.isNotBlank()) roleMap[uid] = AdminRoleHelper.normalizeRole(AdminJsonHelper.str(r, "role")).ifBlank { "student" }
      }
      val repSet = mutableSetOf<String>()
      val repArr = runCatching { JSONArray(repsJson) }.getOrNull() ?: JSONArray()
      for (i in 0 until repArr.length()) {
        val rep = repArr.optJSONObject(i) ?: continue
        val uid = AdminJsonHelper.firstStr(rep, "user_id", "id")
        if (uid.isNotBlank()) repSet.add(uid)
      }
      val mArr = runCatching { JSONArray(membersJson) }.getOrNull() ?: JSONArray()
      val seen = mutableSetOf<String>()
      return buildList {
        for (i in 0 until mArr.length()) {
          val m = mArr.optJSONObject(i) ?: continue
          val status = AdminJsonHelper.str(m, "status").ifBlank { "active" }
          if (!status.equals("active", ignoreCase = true)) continue
          val uid = AdminJsonHelper.firstStr(m, "user_id", "id")
          if (uid.isBlank() || !seen.add(uid)) continue
          val p = profMap[uid]
          val sysRole = roleMap[uid] ?: "student"
          val memRole = AdminRoleHelper.normalizeRole(AdminJsonHelper.firstStr(m, "role", "member_role")).ifBlank { sysRole }
          add(
            AdminClassMemberItem(
              userId = uid,
              fullName = AdminJsonHelper.firstStr(p, "full_name", "name").ifBlank { "Member" },
              admissionNumber = AdminJsonHelper.firstStr(p, "admission_number", "staff_number"),
              isRep = uid in repSet,
              role = sysRole,
              memberRole = memRole
            )
          )
        }
      }.sortedWith(compareByDescending<AdminClassMemberItem> { it.memberRole == "lecturer" || it.role == "lecturer" }
        .thenByDescending { it.isRep }
        .thenBy { it.fullName.lowercase() })
    }
  }
}
