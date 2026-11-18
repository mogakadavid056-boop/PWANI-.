package com.example.admin.data

import com.example.admin.util.AdminJsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class AdminOversightRepository(
  private val client: OkHttpClient = SupabaseAdminConfig.sharedHttpClient
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  private fun postRpc(rpcName: String, body: JSONObject, accessToken: String): Result<String> {
    if (!SupabaseAdminConfig.isConfigured()) {
      return Result.failure(IllegalStateException(SupabaseAdminConfig.MISSING_CONFIG_ERROR))
    }
    val req = Request.Builder()
      .url("${SupabaseAdminConfig.url}/rest/v1/rpc/$rpcName")
      .addHeader("apikey", SupabaseAdminConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(body.toString().toRequestBody(jsonMediaType))
      .build()
    return try {
      client.newCall(req).execute().use { resp ->
        val raw = resp.body?.string().orEmpty().trim()
        if (resp.isSuccessful) Result.success(raw)
        else Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(raw, resp.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, "Request failed")))
    }
  }

  suspend fun listClasses(searchText: String, accessToken: String): Result<List<AdminClassItem>> =
    withContext(Dispatchers.IO) {
      val payload = JSONObject().apply {
        put("search_text", searchText.trim())
        put("max_rows", 80)
      }
      val rpcRes = postRpc("admin_list_classes", payload, accessToken)
      if (rpcRes.isSuccess) {
        return@withContext runCatching {
          val arr = JSONArray(rpcRes.getOrThrow())
          buildList {
            for (i in 0 until arr.length()) {
              val o = arr.getJSONObject(i)
              add(
                AdminClassItem(
                  id = AdminJsonHelper.str(o, "id"),
                  name = AdminJsonHelper.str(o, "name"),
                  code = AdminJsonHelper.str(o, "code"),
                  joinCode = AdminJsonHelper.str(o, "join_code"),
                  yearOfStudy = if (o.has("year_of_study") && !o.isNull("year_of_study")) o.optInt("year_of_study") else null,
                  programmeName = AdminJsonHelper.str(o, "programme_name"),
                  memberCount = if (o.has("member_count") && !o.isNull("member_count")) o.optInt("member_count") else null,
                  registerCount = if (o.has("register_count") && !o.isNull("register_count")) o.optInt("register_count") else null
                )
              )
            }
          }
        }
      }
      // Fallback to direct classes query
      val req = Request.Builder()
        .url("${SupabaseAdminConfig.url}/rest/v1/classes?select=id,name,code,join_code,year_of_study,programmes(name)&order=created_at.desc&limit=80")
        .addHeader("apikey", SupabaseAdminConfig.anonKey)
        .addHeader("Authorization", "Bearer $accessToken")
        .addHeader("Accept", "application/json")
        .get()
        .build()
      try {
        client.newCall(req).execute().use { resp ->
          val raw = resp.body?.string().orEmpty()
          if (!resp.isSuccessful) {
            return@withContext Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(raw, resp.code)))
          }
          val arr = JSONArray(raw)
          val q = searchText.trim().lowercase()
          val list = buildList {
            for (i in 0 until arr.length()) {
              val o = arr.getJSONObject(i)
              val item = AdminClassItem(
                id = AdminJsonHelper.str(o, "id"),
                name = AdminJsonHelper.str(o, "name"),
                code = AdminJsonHelper.str(o, "code"),
                joinCode = AdminJsonHelper.str(o, "join_code"),
                yearOfStudy = if (o.has("year_of_study") && !o.isNull("year_of_study")) o.optInt("year_of_study") else null,
                programmeName = AdminJsonHelper.str(o.optJSONObject("programmes"), "name")
              )
              if (q.isBlank() ||
                item.name.lowercase().contains(q) ||
                item.code.lowercase().contains(q) ||
                item.programmeName.lowercase().contains(q)
              ) {
                add(item)
              }
            }
          }
          Result.success(list)
        }
      } catch (e: Exception) {
        Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, "Failed to list classes")))
      }
    }

  suspend fun listRegisters(classId: String, accessToken: String): Result<List<AdminRegisterSummary>> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("target_class_id", classId) }
      postRpc("list_registers", body, accessToken).mapCatching { raw ->
        val arr = JSONArray(raw)
        buildList {
          for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            add(
              AdminRegisterSummary(
                sessionId = AdminJsonHelper.str(o, "session_id"),
                registerNo = AdminJsonHelper.str(o, "register_no"),
                title = AdminJsonHelper.str(o, "title"),
                courseName = AdminJsonHelper.str(o, "course_name"),
                dateText = AdminJsonHelper.firstStr(o, "date_text", "starts_at"),
                timeText = AdminJsonHelper.str(o, "time_text"),
                status = AdminJsonHelper.str(o, "status").ifBlank { "open" },
                presentCount = AdminJsonHelper.intVal(o, "present_count")
              )
            )
          }
        }
      }
    }

  suspend fun getRegisterDetail(sessionId: String, accessToken: String): Result<AdminRegisterDetail> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("target_session_id", sessionId) }
      postRpc("get_attendance_register", body, accessToken).mapCatching { raw ->
        val o = if (raw.startsWith("[")) JSONArray(raw).optJSONObject(0) ?: JSONObject() else JSONObject(raw)
        val perms = o.optJSONObject("permissions")
        val rowsArr = o.optJSONArray("rows") ?: JSONArray()
        val rows = buildList {
          for (i in 0 until rowsArr.length()) {
            val r = rowsArr.getJSONObject(i)
            add(
              AdminRegisterRow(
                n = AdminJsonHelper.intVal(r, "n", i + 1),
                fullName = AdminJsonHelper.str(r, "full_name"),
                admissionNumber = AdminJsonHelper.str(r, "admission_number"),
                isPrefect = AdminJsonHelper.bool(r, "is_prefect"),
                present = AdminJsonHelper.bool(r, "present"),
                timeInText = AdminJsonHelper.firstStr(r, "time_in_text", "time_in"),
                timeOutText = AdminJsonHelper.firstStr(r, "time_out_text", "time_out")
              )
            )
          }
        }
        AdminRegisterDetail(
          sessionId = AdminJsonHelper.firstStr(o, "session_id").ifBlank { sessionId },
          registerNo = AdminJsonHelper.str(o, "register_no"),
          title = AdminJsonHelper.str(o, "title"),
          status = AdminJsonHelper.str(o, "status").ifBlank { "open" },
          voidReason = AdminJsonHelper.str(o, "void_reason"),
          courseName = AdminJsonHelper.str(o, "course_name"),
          courseCode = AdminJsonHelper.str(o, "course_code"),
          className = AdminJsonHelper.str(o, "class_name"),
          lecturerName = AdminJsonHelper.str(o, "lecturer_name"),
          venue = AdminJsonHelper.str(o, "venue"),
          dateText = AdminJsonHelper.firstStr(o, "date_text", "date"),
          timeText = AdminJsonHelper.str(o, "time_text"),
          presentCount = AdminJsonHelper.intVal(o, "present_count"),
          total = AdminJsonHelper.intVal(o, "total", rows.size),
          canClose = AdminJsonHelper.bool(perms, "can_close", false),
          canVoid = AdminJsonHelper.bool(perms, "can_void", false),
          rows = rows
        )
      }
    }

  suspend fun closeSession(sessionId: String, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
    postRpc("close_attendance_session", JSONObject().apply { put("target_session_id", sessionId) }, accessToken).map { }
  }

  suspend fun voidSession(sessionId: String, reason: String, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply {
        put("target_session_id", sessionId)
        put("reason", reason.trim())
      }
      postRpc("void_attendance_session", body, accessToken).map { }
    }
}
