package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

class MaterialsService(
  private val client: OkHttpClient = OfflineCache.getClient(),
  private val storageService: MaterialsStorageService = MaterialsStorageService()
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun fetchMaterials(classId: String, accessToken: String): Result<List<MaterialItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))

    val url = "${SupabaseConfig.url}/rest/v1/materials?class_id=eq.$classId&select=id,class_id,title,description,material_type,external_url,storage_path,file_name,file_size,mime_type,created_at&order=created_at.desc"
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
              classId = obj.optString("class_id"),
              title = obj.optString("title"),
              description = obj.optString("description"),
              materialType = obj.optString("material_type", "notes"),
              externalUrl = obj.optString("external_url"),
              storagePath = obj.optString("storage_path"),
              fileName = obj.optString("file_name"),
              fileSize = obj.optLong("file_size", 0L),
              mimeType = obj.optString("mime_type"),
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

  suspend fun addLinkMaterial(
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

  suspend fun uploadFileAndCreateMaterial(
    classId: String,
    title: String,
    description: String,
    storagePath: String,
    fileName: String,
    fileSize: Long,
    mimeType: String,
    inputStreamProvider: () -> InputStream,
    accessToken: String,
    onProgress: (Float) -> Unit
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))

    val uploadRes = storageService.uploadFile(
      storagePath = storagePath,
      fileSize = fileSize,
      mimeType = mimeType,
      inputStreamProvider = inputStreamProvider,
      accessToken = accessToken,
      onProgress = onProgress
    )
    if (uploadRes.isFailure) return@withContext uploadRes

    val insertJson = JSONObject().apply {
      put("class_id", classId)
      put("title", title.trim())
      put("description", description.trim())
      put("material_type", "notes")
      put("storage_path", storagePath)
      put("file_name", fileName)
      put("file_size", fileSize)
      put("mime_type", mimeType)
    }
    val insertUrl = "${SupabaseConfig.url}/rest/v1/materials"
    val insertReq = Request.Builder().url(insertUrl)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .post(insertJson.toString().toRequestBody(jsonMediaType)).build()

    try {
      client.newCall(insertReq).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (resp.isSuccessful) {
          Result.success(Unit)
        } else {
          storageService.deleteFile(storagePath, accessToken)
          Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
        }
      }
    } catch (e: Exception) {
      storageService.deleteFile(storagePath, accessToken)
      Result.failure(Exception(e.localizedMessage ?: "Failed to save material record"))
    }
  }

  suspend fun updateMaterial(
    materialId: String,
    title: String,
    description: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))

    val json = JSONObject().apply {
      put("title", title.trim())
      put("description", description.trim())
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

  suspend fun deleteMaterial(material: MaterialItem, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))

    if (material.storagePath.isNotBlank()) {
      storageService.deleteFile(material.storagePath, accessToken)
    }

    val url = "${SupabaseConfig.url}/rest/v1/materials?id=eq.${material.id}"
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

  suspend fun downloadFileMaterial(
    storagePath: String,
    destinationFile: File,
    accessToken: String,
    onProgress: (Float) -> Unit
  ): Result<File> = storageService.downloadFile(storagePath, destinationFile, accessToken, onProgress)
}
