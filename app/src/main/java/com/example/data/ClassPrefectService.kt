package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ClassPrefectService(
  private val client: OkHttpClient = OfflineCache.getClient()
) {

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun listClassReps(
    classId: String,
    accessToken: String
  ): Result<List<ClassPrefectInfo>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val body = JSONObject().apply {
      put("target_class_id", classId)
    }

    val url = "${SupabaseConfig.url}/rest/v1/rpc/list_class_reps"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .post(body.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val bodyString = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
        }

        val array = JSONArray(bodyString)
        val list = mutableListOf<ClassPrefectInfo>()
        for (i in 0 until array.length()) {
          val item = array.getJSONObject(i)
          val uid = item.optString("user_id").ifBlank { item.optString("id") }
          val name = item.optString("full_name").ifBlank { item.optString("name", "Prefect") }
          val adm = item.optString("admission_number").ifBlank { item.optString("admission_no", "") }
          list.add(ClassPrefectInfo(userId = uid, fullName = name, admissionNumber = adm))
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to load class prefects"))
    }
  }

  suspend fun listClassesWithoutRep(
    accessToken: String
  ): Result<List<ClassWaitingForPrefect>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val url = "${SupabaseConfig.url}/rest/v1/rpc/list_classes_without_rep"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .post("{}".toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val bodyString = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
        }

        val array = JSONArray(bodyString)
        val list = mutableListOf<ClassWaitingForPrefect>()
        for (i in 0 until array.length()) {
          val item = array.getJSONObject(i)
          val cid = item.optString("class_id").ifBlank { item.optString("id") }
          val cname = item.optString("class_name").ifBlank { item.optString("name", "Unnamed Class") }
          val count = item.optInt("student_count", item.optInt("students_count", item.optInt("count", 0)))
          list.add(ClassWaitingForPrefect(classId = cid, className = cname, studentCount = count))
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to load classes waiting for prefect"))
    }
  }
}
