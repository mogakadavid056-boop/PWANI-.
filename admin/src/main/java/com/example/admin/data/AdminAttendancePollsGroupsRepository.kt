package com.example.admin.data

import com.example.admin.util.AdminJsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject

class AdminAttendancePollsGroupsRepository(
  private val client: OkHttpClient = SupabaseAdminConfig.sharedHttpClient
) {
  suspend fun fetchSessions(classId: String, accessToken: String): Result<List<AdminAttendanceSession>> =
    withContext(Dispatchers.IO) {
      val q = "attendance_sessions?class_id=eq.$classId&select=id,class_id,course_id,session_title,course_title,course_code,lecturer_name,venue_text,starts_at,expires_at,active,status&order=starts_at.desc"
      AdminHttpHelper.getRest(client, q, accessToken).mapCatching { raw ->
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
          val o = arr.getJSONObject(i)
          AdminAttendanceSession(
            id = AdminJsonHelper.str(o, "id"),
            classId = AdminJsonHelper.str(o, "class_id").ifBlank { classId },
            courseId = AdminJsonHelper.str(o, "course_id"),
            sessionTitle = AdminJsonHelper.firstStr(o, "session_title", "course_title").ifBlank { "Class Session" },
            courseTitle = AdminJsonHelper.str(o, "course_title"),
            courseCode = AdminJsonHelper.str(o, "course_code"),
            lecturerName = AdminJsonHelper.str(o, "lecturer_name"),
            venueText = AdminJsonHelper.str(o, "venue_text"),
            startsAt = AdminJsonHelper.str(o, "starts_at"),
            expiresAt = AdminJsonHelper.str(o, "expires_at"),
            active = AdminJsonHelper.bool(o, "active", false),
            status = AdminJsonHelper.str(o, "status").ifBlank { if (AdminJsonHelper.bool(o, "active", false)) "open" else "closed" }
          )
        }
      }
    }

  suspend fun startSession(classId: String, courseId: String?, title: String, expiryIso: String, accessToken: String): Result<Pair<String, String>> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply {
        put("target_class_id", classId)
        put("target_course_id", if (courseId.isNullOrBlank()) JSONObject.NULL else courseId)
        put("session_expiry", expiryIso)
        put("session_title", title.trim().ifBlank { "Class Attendance" })
      }
      AdminHttpHelper.postRpc(client, "create_attendance_session_v2", body, accessToken).mapCatching { raw ->
        val o = if (raw.startsWith("[")) JSONArray(raw).optJSONObject(0) ?: JSONObject() else JSONObject(raw)
        Pair(AdminJsonHelper.firstStr(o, "session_id", "id"), AdminJsonHelper.firstStr(o, "session_code", "code"))
      }
    }

  suspend fun fetchOrRotateLiveCode(sessionId: String, accessToken: String): Result<String> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("target_session_id", sessionId) }
      val rotated = AdminHttpHelper.postRpc(client, "rotate_attendance_code", body, accessToken)
      val rawRes = if (rotated.isSuccess) rotated else AdminHttpHelper.postRpc(client, "get_attendance_code", body, accessToken)
      rawRes.mapCatching { raw ->
        val code = when {
          raw.startsWith("[") -> {
            val first = JSONArray(raw).opt(0)
            if (first is JSONObject) AdminJsonHelper.firstStr(first, "session_code", "code") else first?.toString()?.removeSurrounding("\"").orEmpty()
          }
          raw.startsWith("{") -> AdminJsonHelper.firstStr(JSONObject(raw), "session_code", "code")
          else -> raw.removeSurrounding("\"")
        }.trim()
        if (code.equals("null", true)) "" else code
      }
    }

  suspend fun getAttendanceList(sessionId: String, accessToken: String): Result<List<AdminAttendanceRecord>> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("target_session_id", sessionId) }
      AdminHttpHelper.postRpc(client, "get_attendance_list", body, accessToken).mapCatching { raw ->
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
          val o = arr.getJSONObject(i)
          AdminAttendanceRecord(
            fullName = AdminJsonHelper.firstStr(o, "full_name", "name").ifBlank { "Student" },
            admissionNumber = AdminJsonHelper.firstStr(o, "admission_number", "admission_no").ifBlank { "-" },
            timeSigned = AdminJsonHelper.firstStr(o, "time_in_text", "signed_at", "created_at")
          )
        }
      }
    }

  suspend fun deleteSession(sessionId: String, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val body = buildDeleteSessionBody(sessionId)
      AdminHttpHelper.postRpc(client, "delete_attendance_session", body, accessToken).map { }
    }

  suspend fun deleteEmptyExpiredSessions(targetClassId: String? = null, accessToken: String): Result<Int> =
    withContext(Dispatchers.IO) {
      val body = buildDeleteEmptyExpiredSessionsBody(targetClassId)
      AdminHttpHelper.postRpc(client, "admin_delete_empty_expired_sessions", body, accessToken).mapCatching { raw ->
        parseScalarInt(raw)
      }
    }

  suspend fun listPolls(classId: String, accessToken: String): Result<List<AdminPollItem>> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("target_class_id", classId) }
      AdminHttpHelper.postRpc(client, "list_polls", body, accessToken).mapCatching { raw ->
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
          val o = arr.getJSONObject(i)
          AdminPollItem(
            id = AdminJsonHelper.firstStr(o, "id", "poll_id"),
            question = AdminJsonHelper.firstStr(o, "question", "poll_question"),
            totalVotes = AdminJsonHelper.intVal(o, "total_votes", AdminJsonHelper.intVal(o, "votes", 0)),
            active = AdminJsonHelper.bool(o, "active", AdminJsonHelper.bool(o, "is_active", true)),
            expiresAt = AdminJsonHelper.firstStr(o, "expires_at", "poll_expiry", "expiry"),
            createdAt = AdminJsonHelper.str(o, "created_at")
          )
        }
      }
    }

  suspend fun getPollResults(pollId: String, accessToken: String): Result<AdminPollResults> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("target_poll_id", pollId) }
      AdminHttpHelper.postRpc(client, "get_poll_results", body, accessToken).mapCatching { raw ->
        var total = 0; val opts = mutableListOf<AdminPollResultOption>()
        val trimmed = raw.trim()
        val arr = if (trimmed.startsWith("[")) JSONArray(trimmed) else {
          val root = JSONObject(trimmed); total = root.optInt("total_votes", 0)
          root.optJSONArray("results") ?: root.optJSONArray("options") ?: JSONArray()
        }
        var sum = 0
        for (i in 0 until arr.length()) {
          val o = arr.getJSONObject(i)
          val votes = AdminJsonHelper.intVal(o, "votes", AdminJsonHelper.intVal(o, "vote_count", 0))
          sum += votes
          opts.add(AdminPollResultOption(AdminJsonHelper.firstStr(o, "id", "option_id"), AdminJsonHelper.firstStr(o, "option_text", "text"), votes, o.optDouble("percentage", -1.0)))
        }
        if (total <= 0) total = sum
        AdminPollResults(total, opts.map { it.copy(percentage = if (it.percentage >= 0) it.percentage else if (total > 0) (it.votes * 100.0 / total) else 0.0) })
      }
    }

  suspend fun createPoll(classId: String, question: String, options: List<String>, expiryIso: String, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val arr = JSONArray().apply { options.forEach { put(it.trim()) } }
      val body = JSONObject().apply { put("target_class_id", classId); put("poll_question", question.trim()); put("poll_options", arr); put("poll_expiry", expiryIso) }
      AdminHttpHelper.postRpc(client, "create_poll", body, accessToken).map { }
    }

  suspend fun closePoll(pollId: String, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) { AdminHttpHelper.patchRest(client, "polls?id=eq.$pollId", JSONObject().apply { put("active", false) }, accessToken).map { } }

  suspend fun deletePoll(pollId: String, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) { AdminHttpHelper.deleteRest(client, "polls?id=eq.$pollId", accessToken).map { } }

  suspend fun listGroups(classId: String, accessToken: String): Result<List<AdminGroupItem>> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("target_class_id", classId) }
      AdminHttpHelper.postRpc(client, "list_groups", body, accessToken).mapCatching { raw ->
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
          val o = arr.getJSONObject(i)
          AdminGroupItem(
            id = AdminJsonHelper.firstStr(o, "id", "group_id"),
            name = AdminJsonHelper.firstStr(o, "group_name", "name").ifBlank { "Group" },
            description = AdminJsonHelper.firstStr(o, "description", "group_description"),
            memberCount = AdminJsonHelper.intVal(o, "member_count", 0),
            maxMembers = AdminJsonHelper.intVal(o, "max_members", AdminJsonHelper.intVal(o, "max_size", 50))
          )
        }
      }
    }

  suspend fun getGroupMessages(groupId: String, accessToken: String): Result<List<AdminGroupMessage>> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("target_group_id", groupId); put("max_rows", 100) }
      AdminHttpHelper.postRpc(client, "get_group_messages", body, accessToken).mapCatching { raw ->
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
          val o = arr.getJSONObject(i)
          AdminGroupMessage(
            id = AdminJsonHelper.firstStr(o, "id", "message_id"),
            senderName = AdminJsonHelper.firstStr(o, "sender_name", "full_name").ifBlank { "Student" },
            messageText = AdminJsonHelper.firstStr(o, "message_text", "content", "text"),
            createdAt = AdminJsonHelper.firstStr(o, "created_at", "sent_at")
          )
        }
      }
    }

  suspend fun listGroupMembers(groupId: String, accessToken: String): Result<List<AdminGroupMember>> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("target_group_id", groupId) }
      AdminHttpHelper.postRpc(client, "list_group_members", body, accessToken).mapCatching { raw ->
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
          val o = arr.getJSONObject(i)
          AdminGroupMember(
            userId = AdminJsonHelper.firstStr(o, "user_id", "id"),
            fullName = AdminJsonHelper.firstStr(o, "full_name", "name").ifBlank { "Student" },
            admissionNumber = AdminJsonHelper.firstStr(o, "admission_number", "adm_no").ifBlank { "-" },
            isOwner = AdminJsonHelper.bool(o, "is_owner", AdminJsonHelper.bool(o, "is_creator", false))
          )
        }
      }
    }

  suspend fun deleteGroupMessage(messageId: String, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) { AdminHttpHelper.postRpc(client, "delete_group_message", JSONObject().apply { put("target_message_id", messageId) }, accessToken).map { } }

  suspend fun removeGroupMember(groupId: String, userId: String, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) { AdminHttpHelper.postRpc(client, "remove_group_member", JSONObject().apply { put("target_group_id", groupId); put("target_user_id", userId) }, accessToken).map { } }

  suspend fun deleteGroup(groupId: String, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) { AdminHttpHelper.postRpc(client, "delete_group", JSONObject().apply { put("target_group_id", groupId) }, accessToken).map { } }

  companion object {
    fun buildDeleteSessionBody(sessionId: String): JSONObject =
      JSONObject().apply { put("target_session_id", sessionId) }

    fun buildDeleteEmptyExpiredSessionsBody(targetClassId: String? = null): JSONObject =
      JSONObject().apply {
        put("target_class_id", if (targetClassId.isNullOrBlank()) JSONObject.NULL else targetClassId)
      }

    fun parseScalarInt(raw: String): Int =
      raw.trim().removeSurrounding("\"").toIntOrNull() ?: 0
  }
}
