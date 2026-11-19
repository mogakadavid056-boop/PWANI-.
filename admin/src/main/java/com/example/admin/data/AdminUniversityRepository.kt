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

class AdminUniversityRepository(
  private val client: OkHttpClient = SupabaseAdminConfig.sharedHttpClient
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  private fun execute(req: Request): Result<String> {
    if (!SupabaseAdminConfig.isConfigured()) {
      return Result.failure(IllegalStateException(SupabaseAdminConfig.MISSING_CONFIG_ERROR))
    }
    return try {
      client.newCall(req).execute().use { resp ->
        val raw = resp.body?.string().orEmpty()
        if (resp.isSuccessful) Result.success(raw)
        else Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(raw, resp.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, "Request failed")))
    }
  }

  private fun baseRequest(url: String, accessToken: String): Request.Builder = Request.Builder()
    .url(url)
    .addHeader("apikey", SupabaseAdminConfig.anonKey)
    .addHeader("Authorization", "Bearer $accessToken")
    .addHeader("Content-Type", "application/json")
    .addHeader("Accept", "application/json")

  suspend fun fetchSchools(accessToken: String): Result<List<SchoolItem>> = withContext(Dispatchers.IO) {
    val req = baseRequest("${SupabaseAdminConfig.url}/rest/v1/schools?select=id,name,university_id&order=name.asc", accessToken).get().build()
    execute(req).mapCatching { body ->
      val arr = JSONArray(body)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          add(SchoolItem(AdminJsonHelper.str(obj, "id"), AdminJsonHelper.str(obj, "name"), AdminJsonHelper.str(obj, "university_id")))
        }
      }
    }
  }

  suspend fun saveSchool(id: String?, name: String, universityId: String = "", accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
    val resolvedUniId = if (id.isNullOrBlank() && universityId.isBlank()) fetchDefaultUniversityId(accessToken) else universityId
    val body = JSONObject().apply {
      put("name", name.trim())
      if (id.isNullOrBlank() && resolvedUniId.isNotBlank()) put("university_id", resolvedUniId)
    }
    val req = if (id.isNullOrBlank()) {
      baseRequest("${SupabaseAdminConfig.url}/rest/v1/schools", accessToken)
        .addHeader("Prefer", "return=minimal")
        .post(body.toString().toRequestBody(jsonMediaType)).build()
    } else {
      baseRequest("${SupabaseAdminConfig.url}/rest/v1/schools?id=eq.$id", accessToken)
        .addHeader("Prefer", "return=minimal")
        .patch(body.toString().toRequestBody(jsonMediaType)).build()
    }
    execute(req).map { }
  }

  private fun fetchDefaultUniversityId(accessToken: String): String {
    val req = baseRequest("${SupabaseAdminConfig.url}/rest/v1/universities?select=id&limit=1", accessToken).get().build()
    val raw = execute(req).getOrNull() ?: return ""
    return runCatching {
      val arr = JSONArray(raw)
      if (arr.length() > 0) AdminJsonHelper.str(arr.getJSONObject(0), "id") else ""
    }.getOrDefault("")
  }

  suspend fun deleteSchool(id: String, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
    val req = baseRequest("${SupabaseAdminConfig.url}/rest/v1/schools?id=eq.$id", accessToken).delete().build()
    execute(req).map { }
  }

  suspend fun fetchProgrammes(accessToken: String): Result<List<ProgrammeItem>> = withContext(Dispatchers.IO) {
    val req = baseRequest(
      "${SupabaseAdminConfig.url}/rest/v1/programmes?select=id,name,school_id,duration_years,schools(name)&order=name.asc",
      accessToken
    ).get().build()
    execute(req).mapCatching { body ->
      val arr = JSONArray(body)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          val dur = if (obj.has("duration_years") && !obj.isNull("duration_years")) obj.optInt("duration_years", 4) else null
          add(
            ProgrammeItem(
              id = AdminJsonHelper.str(obj, "id"),
              name = AdminJsonHelper.str(obj, "name"),
              schoolId = AdminJsonHelper.str(obj, "school_id"),
              schoolName = AdminJsonHelper.str(obj.optJSONObject("schools"), "name"),
              durationYears = dur
            )
          )
        }
      }
    }
  }

  suspend fun saveProgramme(
    id: String?,
    name: String,
    schoolId: String,
    durationYears: Int,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val body = JSONObject().apply {
      put("name", name.trim())
      put("school_id", schoolId)
      put("duration_years", durationYears)
    }
    val req = if (id.isNullOrBlank()) {
      baseRequest("${SupabaseAdminConfig.url}/rest/v1/programmes", accessToken)
        .addHeader("Prefer", "return=minimal")
        .post(body.toString().toRequestBody(jsonMediaType)).build()
    } else {
      baseRequest("${SupabaseAdminConfig.url}/rest/v1/programmes?id=eq.$id", accessToken)
        .addHeader("Prefer", "return=minimal")
        .patch(body.toString().toRequestBody(jsonMediaType)).build()
    }
    execute(req).map { }
  }

  suspend fun deleteProgramme(id: String, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
    val req = baseRequest("${SupabaseAdminConfig.url}/rest/v1/programmes?id=eq.$id", accessToken).delete().build()
    execute(req).map { }
  }

  suspend fun fetchAcademicYears(accessToken: String): Result<List<AcademicYearItem>> = withContext(Dispatchers.IO) {
    val req = baseRequest("${SupabaseAdminConfig.url}/rest/v1/academic_years?select=id,name,is_current&order=name.desc", accessToken)
      .get().build()
    execute(req).mapCatching { body ->
      val arr = JSONArray(body)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          add(
            AcademicYearItem(
              id = AdminJsonHelper.str(obj, "id"),
              name = AdminJsonHelper.str(obj, "name"),
              isCurrent = AdminJsonHelper.bool(obj, "is_current", false)
            )
          )
        }
      }
    }
  }

  suspend fun createAcademicYear(name: String, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
    val body = JSONObject().apply {
      put("name", name.trim())
      put("is_current", false)
    }
    val req = baseRequest("${SupabaseAdminConfig.url}/rest/v1/academic_years", accessToken)
      .addHeader("Prefer", "return=minimal")
      .post(body.toString().toRequestBody(jsonMediaType)).build()
    execute(req).map { }
  }

  suspend fun setCurrentAcademicYear(yearId: String, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
    val rpcBody = JSONObject().apply { put("target_year_id", yearId) }
    val rpcReq = baseRequest("${SupabaseAdminConfig.url}/rest/v1/rpc/admin_set_current_academic_year", accessToken)
      .post(rpcBody.toString().toRequestBody(jsonMediaType)).build()
    val rpcRes = execute(rpcReq)
    if (rpcRes.isSuccess) return@withContext Result.success(Unit)
    // Fallback if RPC not yet created: unset existing current years, then mark target true
    val unsetReq = baseRequest("${SupabaseAdminConfig.url}/rest/v1/academic_years?is_current=eq.true", accessToken)
      .patch(JSONObject().apply { put("is_current", false) }.toString().toRequestBody(jsonMediaType)).build()
    execute(unsetReq)
    val patchBody = JSONObject().apply { put("is_current", true) }
    val patchReq = baseRequest("${SupabaseAdminConfig.url}/rest/v1/academic_years?id=eq.$yearId", accessToken)
      .patch(patchBody.toString().toRequestBody(jsonMediaType)).build()
    execute(patchReq).map { }
  }
}
