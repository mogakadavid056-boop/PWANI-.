package com.example.admin.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object AdminHttpHelper {
  val jsonMediaType = "application/json; charset=utf-8".toMediaType()
  private val nairobiFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)

  fun postRpc(client: OkHttpClient, rpcName: String, body: JSONObject, accessToken: String): Result<String> {
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
    return execute(client, req, "RPC $rpcName failed")
  }

  fun getRest(client: OkHttpClient, pathAndQuery: String, accessToken: String): Result<String> {
    if (!SupabaseAdminConfig.isConfigured()) {
      return Result.failure(IllegalStateException(SupabaseAdminConfig.MISSING_CONFIG_ERROR))
    }
    val req = Request.Builder()
      .url("${SupabaseAdminConfig.url}/rest/v1/$pathAndQuery")
      .addHeader("apikey", SupabaseAdminConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Accept", "application/json")
      .get()
      .build()
    return execute(client, req, "Request failed")
  }

  fun postRest(client: OkHttpClient, pathAndQuery: String, body: JSONObject, accessToken: String): Result<String> {
    if (!SupabaseAdminConfig.isConfigured()) {
      return Result.failure(IllegalStateException(SupabaseAdminConfig.MISSING_CONFIG_ERROR))
    }
    val req = Request.Builder()
      .url("${SupabaseAdminConfig.url}/rest/v1/$pathAndQuery")
      .addHeader("apikey", SupabaseAdminConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(body.toString().toRequestBody(jsonMediaType))
      .build()
    return execute(client, req, "Request failed")
  }

  fun patchRest(client: OkHttpClient, pathAndQuery: String, body: JSONObject, accessToken: String): Result<String> {
    if (!SupabaseAdminConfig.isConfigured()) {
      return Result.failure(IllegalStateException(SupabaseAdminConfig.MISSING_CONFIG_ERROR))
    }
    val req = Request.Builder()
      .url("${SupabaseAdminConfig.url}/rest/v1/$pathAndQuery")
      .addHeader("apikey", SupabaseAdminConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .patch(body.toString().toRequestBody(jsonMediaType))
      .build()
    return execute(client, req, "Update failed")
  }

  fun deleteRest(client: OkHttpClient, pathAndQuery: String, accessToken: String): Result<String> {
    if (!SupabaseAdminConfig.isConfigured()) {
      return Result.failure(IllegalStateException(SupabaseAdminConfig.MISSING_CONFIG_ERROR))
    }
    val req = Request.Builder()
      .url("${SupabaseAdminConfig.url}/rest/v1/$pathAndQuery")
      .addHeader("apikey", SupabaseAdminConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .delete()
      .build()
    return execute(client, req, "Delete failed")
  }

  private fun execute(client: OkHttpClient, req: Request, fallback: String): Result<String> {
    return try {
      client.newCall(req).execute().use { resp ->
        val raw = resp.body?.string().orEmpty().trim()
        if (resp.isSuccessful) Result.success(raw)
        else Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(raw, resp.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, fallback)))
    }
  }

  fun formatIsoNairobi(iso: String?): String {
    val raw = iso?.trim().orEmpty()
    if (raw.isBlank() || raw.equals("null", true)) return "-"
    return try {
      Instant.parse(raw).atZone(ZoneId.of("Africa/Nairobi")).format(nairobiFmt)
    } catch (_: Throwable) {
      raw.replace("T", " ").take(16).ifBlank { "-" }
    }
  }
}
