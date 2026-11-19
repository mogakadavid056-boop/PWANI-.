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
    AdminHttpHelper.postRpc(client, "search_classes", payload, accessToken).mapCatching { body ->
      parseSearchClasses(body)
    }
  }

  suspend fun getClassOverview(classId: String, accessToken: String): Result<AdminClassOverview> =
    withContext(Dispatchers.IO) {
      val payload = JSONObject().apply { put("target_class_id", classId) }
      AdminHttpHelper.postRpc(client, "admin_class_overview", payload, accessToken).mapCatching { body ->
        parseClassOverview(body, classId)
      }
    }

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
