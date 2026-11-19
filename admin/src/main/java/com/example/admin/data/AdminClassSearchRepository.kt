package com.example.admin.data

import com.example.admin.util.AdminJsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject

class AdminClassSearchRepository(
  private val client: OkHttpClient = SupabaseAdminConfig.sharedHttpClient
) {
  suspend fun searchClasses(
    searchText: String,
    schoolFilter: String?,
    yearFilter: Int?,
    onlyWithoutPrefect: Boolean,
    maxRows: Int = 30,
    offsetRows: Int = 0,
    accessToken: String
  ): Result<List<AdminSearchClassItem>> = withContext(Dispatchers.IO) {
    val payload = JSONObject().apply {
      put("search_text", searchText.trim())
      put("school_filter", if (!schoolFilter.isNullOrBlank()) schoolFilter else JSONObject.NULL)
      put("year_filter", yearFilter ?: JSONObject.NULL)
      put("only_without_prefect", onlyWithoutPrefect)
      put("max_rows", maxRows)
      put("offset_rows", offsetRows)
    }
    val rpc = AdminHttpHelper.postRpc(client, "search_classes", payload, accessToken)
    if (rpc.isSuccess) {
      val parsed = runCatching { parseSearchClasses(rpc.getOrThrow()) }
      if (parsed.isSuccess) return@withContext parsed
    }
    if (offsetRows > 0) return@withContext Result.success(emptyList())
    val restRes = AdminHttpHelper.getRest(
      client,
      "classes?select=id,name,code,year_of_study,programmes(name,schools(name)),class_members(user_id),student_representatives(user_id,active)&order=created_at.desc&limit=200",
      accessToken
    )
    val plainRes = if (restRes.isSuccess) restRes else AdminHttpHelper.getRest(
      client,
      "classes?select=id,name,code,year_of_study,programmes(name,schools(name))&order=created_at.desc&limit=200",
      accessToken
    )
    plainRes.mapCatching { body ->
      val q = searchText.trim().lowercase()
      val arr = JSONArray(body)
      buildList {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          val yr = if (o.has("year_of_study") && !o.isNull("year_of_study")) o.optInt("year_of_study") else null
          val prog = o.optJSONObject("programmes")
          val progName = AdminJsonHelper.str(prog, "name")
          val schName = AdminJsonHelper.str(prog?.optJSONObject("schools"), "name")
          val reps = o.optJSONArray("student_representatives")
          var hasPrefect = false
          if (reps != null) for (j in 0 until reps.length()) {
            if (AdminJsonHelper.bool(reps.optJSONObject(j), "active", true)) { hasPrefect = true; break }
          }
          val item = AdminSearchClassItem(
            id = AdminJsonHelper.str(o, "id"),
            className = AdminJsonHelper.str(o, "name").ifBlank { "Class" },
            classCode = AdminJsonHelper.str(o, "code"),
            schoolName = schName,
            programmeName = progName,
            studentCount = o.optJSONArray("class_members")?.length() ?: 0,
            hasPrefect = hasPrefect
          )
          val matchQ = q.isBlank() || item.className.lowercase().contains(q) || item.classCode.lowercase().contains(q) || item.programmeName.lowercase().contains(q)
          val matchSch = schoolFilter.isNullOrBlank() || item.schoolName.equals(schoolFilter, ignoreCase = true)
          val matchYr = yearFilter == null || yr == yearFilter
          val matchRep = !onlyWithoutPrefect || !item.hasPrefect
          if (matchQ && matchSch && matchYr && matchRep) add(item)
        }
      }.take(maxRows)
    }
  }

  suspend fun getClassOverview(classId: String, accessToken: String): Result<AdminClassOverview> =
    withContext(Dispatchers.IO) {
      val payload = JSONObject().apply { put("target_class_id", classId) }
      val rpc = AdminHttpHelper.postRpc(client, "admin_class_overview", payload, accessToken)
      if (rpc.isSuccess) {
        val parsed = runCatching { parseClassOverview(rpc.getOrThrow(), classId) }
        if (parsed.isSuccess) return@withContext parsed
      }
      val rest = AdminHttpHelper.getRest(
        client,
        "classes?id=eq.$classId&select=id,name,code,join_code,year_of_study,programmes(name,schools(name))",
        accessToken
      )
      rest.mapCatching { raw ->
        val o = JSONArray(raw).optJSONObject(0) ?: throw Exception("Class not found.")
        val prog = o.optJSONObject("programmes")
        AdminClassOverview(
          classId = AdminJsonHelper.str(o, "id").ifBlank { classId },
          className = AdminJsonHelper.str(o, "name").ifBlank { "Class" },
          classCode = AdminJsonHelper.str(o, "code"),
          joinCode = AdminJsonHelper.str(o, "join_code"),
          yearOfStudy = if (o.has("year_of_study") && !o.isNull("year_of_study")) o.optInt("year_of_study") else null,
          schoolName = AdminJsonHelper.str(prog?.optJSONObject("schools"), "name"),
          programmeName = AdminJsonHelper.str(prog, "name"),
          studentCount = countRows("class_members?class_id=eq.$classId&select=user_id", accessToken),
          prefectCount = countRows("student_representatives?class_id=eq.$classId&active=eq.true&select=user_id", accessToken),
          unitCount = countRows("courses?class_id=eq.$classId&select=id", accessToken),
          materialCount = countRows("materials?class_id=eq.$classId&select=id", accessToken),
          noticeCount = countRows("notices?class_id=eq.$classId&select=id", accessToken),
          sessionCount = countRows("attendance_sessions?class_id=eq.$classId&select=id", accessToken),
          pollCount = countRows("polls?class_id=eq.$classId&select=id", accessToken)
        )
      }
    }

  private fun countRows(query: String, accessToken: String): Int =
    AdminHttpHelper.getRest(client, query, accessToken).getOrNull()?.let { raw ->
      runCatching { JSONArray(raw).length() }.getOrDefault(0)
    } ?: 0

  suspend fun regenerateJoinCode(classId: String, accessToken: String): Result<String> =
    withContext(Dispatchers.IO) {
      val payload = JSONObject().apply { put("target_class_id", classId) }
      AdminHttpHelper.postRpc(client, "regenerate_join_code", payload, accessToken).mapCatching { raw ->
        val clean = raw.trim().removeSurrounding("\"").trim()
        if (clean.isBlank() || clean.equals("null", true)) {
          throw Exception("Server did not return a new class code.")
        }
        clean
      }
    }

  companion object {
    fun parseSearchClasses(body: String): List<AdminSearchClassItem> {
      val trimmed = body.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyList()
      val arr = JSONArray(trimmed)
      return buildList {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          add(
            AdminSearchClassItem(
              id = AdminJsonHelper.firstStr(o, "class_id", "id"),
              className = AdminJsonHelper.firstStr(o, "class_name", "name").ifBlank { "Class" },
              classCode = AdminJsonHelper.firstStr(o, "class_code", "code"),
              schoolName = AdminJsonHelper.str(o, "school_name"),
              programmeName = AdminJsonHelper.str(o, "programme_name"),
              studentCount = AdminJsonHelper.intVal(o, "student_count", AdminJsonHelper.intVal(o, "students_count", 0)),
              hasPrefect = AdminJsonHelper.bool(o, "has_prefect", false)
            )
          )
        }
      }
    }

    fun parseClassOverview(body: String, fallbackClassId: String = ""): AdminClassOverview {
      val o = runCatching { JSONObject(body) }.getOrDefault(JSONObject())
      return AdminClassOverview(
        classId = AdminJsonHelper.str(o, "class_id").ifBlank { fallbackClassId },
        className = AdminJsonHelper.str(o, "class_name").ifBlank { "Class" },
        classCode = AdminJsonHelper.str(o, "class_code"),
        joinCode = AdminJsonHelper.str(o, "join_code"),
        yearOfStudy = if (o.has("year_of_study") && !o.isNull("year_of_study")) o.optInt("year_of_study") else null,
        schoolName = AdminJsonHelper.str(o, "school_name"),
        programmeName = AdminJsonHelper.str(o, "programme_name"),
        studentCount = AdminJsonHelper.intVal(o, "student_count", 0),
        lecturerCount = AdminJsonHelper.intVal(o, "lecturer_count", 0),
        prefectCount = AdminJsonHelper.intVal(o, "prefect_count", 0),
        unitCount = AdminJsonHelper.intVal(o, "unit_count", 0),
        materialCount = AdminJsonHelper.intVal(o, "material_count", 0),
        noticeCount = AdminJsonHelper.intVal(o, "notice_count", 0),
        sessionCount = AdminJsonHelper.intVal(o, "session_count", 0),
        pollCount = AdminJsonHelper.intVal(o, "poll_count", 0)
      )
    }
  }
}
