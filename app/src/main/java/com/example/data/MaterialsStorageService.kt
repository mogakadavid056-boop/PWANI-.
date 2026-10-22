package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

class MaterialsStorageService(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun uploadFile(
    storagePath: String,
    fileSize: Long,
    mimeType: String,
    inputStreamProvider: () -> InputStream,
    accessToken: String,
    onProgress: (Float) -> Unit
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val uploadUrl = "${SupabaseConfig.url}/storage/v1/object/class-materials/$storagePath"
    val reqBody = CountingRequestBody(
      inputStreamProvider = inputStreamProvider,
      totalBytes = fileSize,
      contentType = mimeType.toMediaTypeOrNull(),
      onProgress = onProgress
    )
    val uploadReq = Request.Builder().url(uploadUrl)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", mimeType)
      .post(reqBody).build()

    try {
      client.newCall(uploadReq).execute().use { resp ->
        if (resp.isSuccessful) Result.success(Unit)
        else {
          val err = resp.body?.string().orEmpty()
          Result.failure(Exception(SupabaseConfig.extractErrorMessage(err, resp.code)))
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Upload network failed"))
    }
  }

  suspend fun deleteFile(storagePath: String, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
    if (storagePath.isBlank()) return@withContext Result.success(Unit)
    val url = "${SupabaseConfig.url}/storage/v1/object/class-materials/$storagePath"
    val req = Request.Builder().url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .delete().build()

    try {
      client.newCall(req).execute().use { resp ->
        if (resp.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(resp.message))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun downloadFile(
    storagePath: String,
    destinationFile: File,
    accessToken: String,
    onProgress: (Float) -> Unit
  ): Result<File> = withContext(Dispatchers.IO) {
    val signUrl = "${SupabaseConfig.url}/storage/v1/object/sign/class-materials/$storagePath"
    val signBody = JSONObject().apply { put("expiresIn", 600) }
    val signReq = Request.Builder().url(signUrl)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .post(signBody.toString().toRequestBody(jsonMediaType)).build()

    val signedUrl = try {
      client.newCall(signReq).execute().use { resp ->
        val b = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(b, resp.code)))
        }
        JSONObject(b).optString("signedURL")
      }
    } catch (e: Exception) {
      return@withContext Result.failure(Exception(e.localizedMessage ?: "Failed to sign file URL"))
    }

    if (signedUrl.isBlank()) return@withContext Result.failure(Exception("Failed to get signed download URL"))

    val fullUrl = if (signedUrl.startsWith("http")) signedUrl else "${SupabaseConfig.url}/storage/v1$signedUrl"
    val downloadReq = Request.Builder().url(fullUrl)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .get().build()

    try {
      client.newCall(downloadReq).execute().use { resp ->
        if (!resp.isSuccessful) {
          val b = resp.body?.string().orEmpty()
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(b, resp.code)))
        }
        val respBody = resp.body ?: return@withContext Result.failure(Exception("Empty file response"))
        val totalLength = respBody.contentLength()
        destinationFile.parentFile?.mkdirs()
        if (destinationFile.exists()) destinationFile.delete()

        respBody.byteStream().use { input ->
          destinationFile.outputStream().use { output ->
            val buf = ByteArray(8192)
            var read: Int
            var downloaded = 0L
            while (input.read(buf).also { read = it } != -1) {
              output.write(buf, 0, read)
              downloaded += read
              if (totalLength > 0) onProgress((downloaded.toFloat() / totalLength.toFloat()).coerceIn(0f, 1f))
            }
          }
        }
        Result.success(destinationFile)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Download failed"))
    }
  }
}
