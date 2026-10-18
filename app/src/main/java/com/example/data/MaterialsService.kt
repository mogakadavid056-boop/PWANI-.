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

class MaterialsService(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun fetchMaterials(classId: String, accessToken: String): Result<List<MaterialItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))

    val url = "${SupabaseConfig.url}/rest/v1/materials?class_id=eq.$classId&select=id,title,description,material_type,external_url,created_at&order=created_at.desc"
    val req = Request.Builder().url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Accept", "application/json")
      .get().build()

    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
        val arr = JSONArray(body)
        val list = mutableListOf<MaterialItem>()
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          list.add(
            MaterialItem(
              id = obj.optString("id"),
              title = obj.optString("title"),
              description = obj.optString("description"),
              materialType = obj.optString("material_type", "notes"),
              externalUrl = obj.optString("external_url"),
              createdAt = obj.optString("created_at")
            )
          )
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to load materials"))
    }
  }

  suspend fun addMaterial(
    classId: String,
    title: String,
    description: String,
    materialType: String,
    externalUrl: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))

    val json = JSONObject().apply {
      put("class_id", classId)
      put("title", title.trim())
      put("description", description.trim())
      put("material_type", materialType.trim().lowercase())
      put("external_url", externalUrl.trim())
    }
    val url = "${SupabaseConfig.url}/rest/v1/materials"
    val req = Request.Builder().url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .post(json.toString().toRequestBody(jsonMediaType)).build()

    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (resp.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to add material"))
    }
  }

  suspend fun updateMaterial(
    materialId: String,
    title: String,
    description: String,
    materialType: String,
    externalUrl: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))

    val json = JSONObject().apply {
      put("title", title.trim())
      put("description", description.trim())
      put("material_type", materialType.trim().lowercase())
      put("external_url", externalUrl.trim())
    }
    val url = "${SupabaseConfig.url}/rest/v1/materials?id=eq.$materialId"
    val req = Request.Builder().url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .patch(json.toString().toRequestBody(jsonMediaType)).build()

    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (resp.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to update material"))
    }
  }

  suspend fun deleteMaterial(materialId: String, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))

    val url = "${SupabaseConfig.url}/rest/v1/materials?id=eq.$materialId"
    val req = Request.Builder().url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .delete().build()

    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (resp.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to delete material"))
    }
  }
}
