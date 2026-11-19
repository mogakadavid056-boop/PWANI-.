package com.example.data

import com.example.util.JsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class UnitsService(
  private val client: OkHttpClient = OfflineCache.getClient()
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun listClassUnits(
    classId: String,
    accessToken: String
  ): Result<List<CourseUnit>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val payload = JSONObject().apply { put("target_class_id", classId) }
    val req = Request.Builder()
      .url("${SupabaseConfig.url}/rest/v1/rpc/list_class_units")
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(payload.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
        }
        val arr = JSONArray(body)
        val list = buildList {
          for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val idsArr = obj.optJSONArray("lecturer_ids")
            val lecturerIds = buildList {
              if (idsArr != null) {
                for (j in 0 until idsArr.length()) {
                  val uid = idsArr.optString(j)
                  if (uid.isNotBlank() && !uid.equals("null", true)) add(uid)
                }
              }
            }
            add(
              CourseUnit(
                courseId = JsonHelper.firstStr(obj, "course_id", "id"),
                code = JsonHelper.firstStr(obj, "code", "course_code").uppercase(),
                name = JsonHelper.firstStr(obj, "name", "course_name", "title"),
                description = JsonHelper.str(obj, "description"),
                status = JsonHelper.str(obj, "status").ifBlank { "approved" }.lowercase(),
                lecturers = JsonHelper.firstStr(obj, "lecturers", "lecturer_name"),
                lecturerIds = lecturerIds,
                materialCount = JsonHelper.intVal(obj, "material_count", 0),
                rejectionReason = JsonHelper.str(obj, "rejection_reason")
              )
            )
          }
        }.sortedBy { it.code }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to load units")))
    }
  }

  suspend fun requestUnit(
    classId: String,
    unitCode: String,
    unitName: String,
    unitDescription: String,
    accessToken: String
  ): Result<CourseUnit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val payload = JSONObject().apply {
      put("target_class_id", classId)
      put("unit_code", unitCode.trim().uppercase())
      put("unit_name", unitName.trim())
      put("unit_description", unitDescription.trim())
    }
    val req = Request.Builder()
      .url("${SupabaseConfig.url}/rest/v1/rpc/request_unit")
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(payload.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
        }
        val obj = runCatching { JSONObject(body) }.getOrDefault(JSONObject())
        Result.success(
          CourseUnit(
            courseId = JsonHelper.firstStr(obj, "course_id", "id"),
            code = JsonHelper.str(obj, "code").ifBlank { unitCode.trim().uppercase() },
            name = JsonHelper.str(obj, "name").ifBlank { unitName.trim() },
            description = unitDescription.trim(),
            status = JsonHelper.str(obj, "status").ifBlank { "pending" }.lowercase()
          )
        )
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to submit unit request")))
    }
  }
}
