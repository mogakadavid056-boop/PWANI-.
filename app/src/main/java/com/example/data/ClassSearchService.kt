package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class ClassSearchService(
  private val client: OkHttpClient = OfflineCache.getClient()
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun searchClasses(
    searchText: String,
    schoolFilter: String?,
    yearFilter: Int?,
    onlyWithoutPrefect: Boolean,
    maxRows: Int = 30,
    offsetRows: Int = 0,
    accessToken: String
  ): Result<List<SearchClassItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val payload = JSONObject().apply {
      put("search_text", searchText)
      put("school_filter", if (!schoolFilter.isNullOrBlank()) schoolFilter else JSONObject.NULL)
      put("year_filter", if (yearFilter != null) yearFilter else JSONObject.NULL)
      put("only_without_prefect", onlyWithoutPrefect)
      put("max_rows", maxRows)
      put("offset_rows", offsetRows)
    }

    val url = "${SupabaseConfig.url}/rest/v1/rpc/search_classes"
    val tokenHeader = if (accessToken.isNotBlank() && accessToken != SupabaseConfig.anonKey) accessToken else SupabaseConfig.anonKey

    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $tokenHeader")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(payload.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, response.code)))
        }

        val arr = JSONArray(body)
        val list = mutableListOf<SearchClassItem>()
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          list.add(
            SearchClassItem(
              id = obj.optString("class_id").ifBlank { obj.optString("id") },
              className = obj.optString("class_name").ifBlank { obj.optString("name", "Class") },
              classCode = obj.optString("class_code").ifBlank { obj.optString("code") },
              schoolName = obj.optString("school_name"),
              programmeName = obj.optString("programme_name"),
              studentCount = obj.optInt("student_count", obj.optInt("students_count", obj.optInt("student_total", 0))),
              hasPrefect = obj.optBoolean("has_prefect", false),
              isMine = obj.optBoolean("is_mine", false)
            )
          )
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to search classes"))
    }
  }

  suspend fun claimClass(
    targetClassId: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val payload = JSONObject().apply {
      put("target_class_id", targetClassId)
    }

    val url = "${SupabaseConfig.url}/rest/v1/rpc/claim_class"
    val tokenHeader = if (accessToken.isNotBlank() && accessToken != SupabaseConfig.anonKey) accessToken else SupabaseConfig.anonKey

    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $tokenHeader")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(payload.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, response.code)))
        }
        Result.success(Unit)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to claim class"))
    }
  }
}
