package com.example.data

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.util.JsonHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class AppBackgroundSpec(
  val bucket: String,
  val path: String,
  val isPublic: Boolean,
  val updatedAt: String
)

data class ResolvedAppBackgrounds(
  val bg001: AppBackgroundSpec? = null,
  val bg002: AppBackgroundSpec? = null
)

data class AppBackgroundState(
  val bg001: ImageBitmap? = null,
  val bg002: ImageBitmap? = null
)

object AppBackgroundManager {
  private const val PREFS_NAME = "classrep_app_backgrounds"
  private const val DIR_NAME = "app_backgrounds"
  private const val FILE_001 = "bg_001.png"
  private const val FILE_002 = "bg_002.png"
  private const val MIN_REFRESH_INTERVAL_MS = 45_000L
  private const val MAX_BITMAP_DIM_PX = 1920

  private val CANDIDATE_BUCKETS = listOf(
    "app-backgrounds", "backgrounds", "background", "assets", "app-assets",
    "images", "public", "media", "wallpapers", "class-materials",
    "files", "uploads", "classrep", "classrep-assets", "branding", "ui"
  )
  private val SLOT_001_NAMES = listOf(
    "background001.png", "Background001.png", "background001", "Background001",
    "background001.PNG", "background001.jpg", "background001.webp"
  )
  private val SLOT_002_NAMES = listOf(
    "Background002.png", "background002.png", "Background002", "background002",
    "Background002.PNG", "Background002.jpg", "background002.webp"
  )

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
  private val client = OkHttpClient.Builder()
    .connectTimeout(12, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()

  private val _state = MutableStateFlow(AppBackgroundState())
  val state: StateFlow<AppBackgroundState> = _state.asStateFlow()

  @Volatile
  private var appContext: Context? = null

  @Volatile
  private var lastFetchAtMs: Long = 0L

  @Volatile
  private var lastTokenUsed: String? = null

  fun initialize(context: Context, accessToken: String? = null) {
    val ctx = context.applicationContext
    appContext = ctx
    scope.launch {
      loadFromDiskCache(ctx)
      refreshIfNeeded(accessToken = accessToken, force = true)
    }
  }

  fun refreshIfNeeded(accessToken: String? = null, force: Boolean = false) {
    val ctx = appContext ?: return
    val now = System.currentTimeMillis()
    val tokenChanged = !accessToken.isNullOrBlank() && accessToken != lastTokenUsed
    if (!force && !tokenChanged && (now - lastFetchAtMs) < MIN_REFRESH_INTERVAL_MS) {
      return
    }
    lastFetchAtMs = now
    if (!accessToken.isNullOrBlank()) lastTokenUsed = accessToken

    scope.launch {
      syncFromSupabase(ctx, accessToken)
    }
  }

  private fun loadFromDiskCache(context: Context) {
    val dir = File(context.filesDir, DIR_NAME)
    val f1 = File(dir, FILE_001)
    val f2 = File(dir, FILE_002)
    val b1 = if (f1.exists() && f1.length() > 0L) decodeSampledBitmap(f1) else null
    val b2 = if (f2.exists() && f2.length() > 0L) decodeSampledBitmap(f2) else null
    if (b1 != null || b2 != null) {
      _state.update { cur ->
        cur.copy(
          bg001 = b1 ?: cur.bg001,
          bg002 = b2 ?: cur.bg002
        )
      }
    }
  }

  private fun syncFromSupabase(context: Context, accessToken: String?) {
    if (!SupabaseConfig.isConfigured()) return
    val resolved = fetchViaRpc(accessToken) ?: discoverViaStorageFallback(accessToken)
    resolved.bg001?.let { spec ->
      val bmp = syncSlot(context, "001", FILE_001, spec, accessToken)
      if (bmp != null) _state.update { it.copy(bg001 = bmp) }
    }
    resolved.bg002?.let { spec ->
      val bmp = syncSlot(context, "002", FILE_002, spec, accessToken)
      if (bmp != null) _state.update { it.copy(bg002 = bmp) }
    }
  }

  private fun fetchViaRpc(accessToken: String?): ResolvedAppBackgrounds? {
    val bearer = accessToken?.takeIf { it.isNotBlank() } ?: SupabaseConfig.anonKey
    val req = Request.Builder()
      .url("${SupabaseConfig.url}/rest/v1/rpc/get_app_backgrounds")
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $bearer")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post("{}".toRequestBody(jsonMediaType))
      .build()
    return runCatching {
      client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) return@use null
        val body = resp.body?.string().orEmpty()
        parseRpcBackgrounds(body)
      }
    }.getOrNull()?.takeIf { it.bg001 != null || it.bg002 != null }
  }

  private fun discoverViaStorageFallback(accessToken: String?): ResolvedAppBackgrounds {
    var spec1: AppBackgroundSpec? = null
    var spec2: AppBackgroundSpec? = null
    val bearer = accessToken?.takeIf { it.isNotBlank() } ?: SupabaseConfig.anonKey

    for (bucket in CANDIDATE_BUCKETS) {
      if (spec1 != null && spec2 != null) break

      // First try bucket listing (works when SELECT policy allows listing)
      val listReq = Request.Builder()
        .url("${SupabaseConfig.url}/storage/v1/object/list/$bucket")
        .addHeader("apikey", SupabaseConfig.anonKey)
        .addHeader("Authorization", "Bearer $bearer")
        .addHeader("Content-Type", "application/json")
        .post("""{"prefix":"","limit":100}""".toRequestBody(jsonMediaType))
        .build()

      runCatching {
        client.newCall(listReq).execute().use { resp ->
          if (resp.isSuccessful) {
            val arr = JSONArray(resp.body?.string().orEmpty())
            for (i in 0 until arr.length()) {
              val obj = arr.optJSONObject(i) ?: continue
              val name = obj.optString("name", "")
              val updated = obj.optString("updated_at", "")
              if (spec1 == null && name.contains("background001", ignoreCase = true)) {
                spec1 = AppBackgroundSpec(bucket, name, isPublic = true, updatedAt = updated)
              }
              if (spec2 == null && name.contains("background002", ignoreCase = true)) {
                spec2 = AppBackgroundSpec(bucket, name, isPublic = true, updatedAt = updated)
              }
            }
          }
        }
      }

      // Direct public HEAD probe fallback when bucket listing is blocked by RLS
      if (spec1 == null) {
        for (name in SLOT_001_NAMES) {
          val headSpec = probePublicHead(bucket, name)
          if (headSpec != null) {
            spec1 = headSpec
            break
          }
        }
      }
      if (spec2 == null) {
        for (name in SLOT_002_NAMES) {
          val headSpec = probePublicHead(bucket, name)
          if (headSpec != null) {
            spec2 = headSpec
            break
          }
        }
      }
    }

    return ResolvedAppBackgrounds(bg001 = spec1, bg002 = spec2)
  }

  private fun probePublicHead(bucket: String, path: String): AppBackgroundSpec? {
    val url = buildObjectUrl(SupabaseConfig.url, bucket, path, isPublic = true)
    val req = Request.Builder().url(url).head().build()
    return runCatching {
      client.newCall(req).execute().use { resp ->
        if (resp.isSuccessful) {
          val etag = resp.header("ETag").orEmpty()
          val lastMod = resp.header("Last-Modified").orEmpty()
          AppBackgroundSpec(bucket, path, isPublic = true, updatedAt = "$etag|$lastMod")
        } else null
      }
    }.getOrNull()
  }

  private fun syncSlot(
    context: Context,
    slotKey: String,
    fileName: String,
    spec: AppBackgroundSpec,
    accessToken: String?
  ): ImageBitmap? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
    val targetFile = File(dir, fileName)
    val versionKey = "${spec.bucket}|${spec.path}|${spec.updatedAt}"
    val cachedVersion = prefs.getString("version_$slotKey", null)

    if (cachedVersion == versionKey && targetFile.exists() && targetFile.length() > 0L) {
      return null
    }

    val cachedEtag = prefs.getString("etag_$slotKey", null)
    val candidates = buildCandidateDownloadRequests(spec, accessToken, cachedEtag.takeIf { cachedVersion?.startsWith("${spec.bucket}|${spec.path}|") == true })

    for (req in candidates) {
      val downloaded = runCatching {
        client.newCall(req).execute().use { resp ->
          if (resp.code == 304 && targetFile.exists() && targetFile.length() > 0L) {
            prefs.edit().putString("version_$slotKey", versionKey).apply()
            return@use false
          }
          if (!resp.isSuccessful) return@use null
          val bytes = resp.body?.bytes() ?: return@use null
          if (bytes.isEmpty()) return@use null

          val tmpFile = File(dir, "$fileName.tmp")
          tmpFile.writeBytes(bytes)
          if (targetFile.exists()) targetFile.delete()
          tmpFile.renameTo(targetFile)

          val newEtag = resp.header("ETag")
          prefs.edit()
            .putString("version_$slotKey", versionKey)
            .apply { if (!newEtag.isNullOrBlank()) putString("etag_$slotKey", newEtag) }
            .apply()
          true
        }
      }.getOrNull()

      if (downloaded == true) {
        return decodeSampledBitmap(targetFile)
      } else if (downloaded == false) {
        return null
      }
    }
    return null
  }

  private fun buildCandidateDownloadRequests(
    spec: AppBackgroundSpec,
    accessToken: String?,
    cachedEtag: String?
  ): List<Request> {
    val bearer = accessToken?.takeIf { it.isNotBlank() } ?: SupabaseConfig.anonKey
    val requests = mutableListOf<Request>()

    if (spec.isPublic) {
      val pubUrl = buildObjectUrl(SupabaseConfig.url, spec.bucket, spec.path, isPublic = true)
      requests.add(
        Request.Builder()
          .url(pubUrl)
          .apply { if (!cachedEtag.isNullOrBlank()) addHeader("If-None-Match", cachedEtag) }
          .get()
          .build()
      )
    }

    val directUrl = buildObjectUrl(SupabaseConfig.url, spec.bucket, spec.path, isPublic = false)
    requests.add(
      Request.Builder()
        .url(directUrl)
        .addHeader("apikey", SupabaseConfig.anonKey)
        .addHeader("Authorization", "Bearer $bearer")
        .apply { if (!cachedEtag.isNullOrBlank()) addHeader("If-None-Match", cachedEtag) }
        .get()
        .build()
    )

    if (!spec.isPublic) {
      val pubFallbackUrl = buildObjectUrl(SupabaseConfig.url, spec.bucket, spec.path, isPublic = true)
      requests.add(
        Request.Builder()
          .url(pubFallbackUrl)
          .apply { if (!cachedEtag.isNullOrBlank()) addHeader("If-None-Match", cachedEtag) }
          .get()
          .build()
      )
    }
    return requests
  }

  private fun decodeSampledBitmap(file: File): ImageBitmap? {
    return runCatching {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeFile(file.absolutePath, bounds)
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

      var sampleSize = 1
      while (bounds.outWidth / sampleSize > MAX_BITMAP_DIM_PX || bounds.outHeight / sampleSize > MAX_BITMAP_DIM_PX) {
        sampleSize *= 2
      }

      val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
    }.getOrNull()
  }

  fun parseRpcBackgrounds(body: String): ResolvedAppBackgrounds {
    val trimmed = body.trim()
    if (trimmed.isBlank() || !trimmed.startsWith("{")) return ResolvedAppBackgrounds()
    val root = runCatching { JSONObject(trimmed) }.getOrNull() ?: return ResolvedAppBackgrounds()
    return ResolvedAppBackgrounds(
      bg001 = parseSlot(root.optJSONObject("bg_001")),
      bg002 = parseSlot(root.optJSONObject("bg_002"))
    )
  }

  private fun parseSlot(obj: JSONObject?): AppBackgroundSpec? {
    if (obj == null) return null
    val bucket = JsonHelper.str(obj, "bucket")
    val path = JsonHelper.str(obj, "path")
    if (bucket.isBlank() || path.isBlank()) return null
    return AppBackgroundSpec(
      bucket = bucket,
      path = path,
      isPublic = JsonHelper.bool(obj, "public", false),
      updatedAt = JsonHelper.str(obj, "updated_at")
    )
  }

  fun buildObjectUrl(baseUrl: String, bucket: String, path: String, isPublic: Boolean): String {
    val cleanBase = SupabaseConfig.sanitizeUrl(baseUrl)
    val encodedBucket = encodeSegment(bucket.trim('/'))
    val encodedPath = path.trim('/').split('/').joinToString("/") { encodeSegment(it) }
    val segment = if (isPublic) "public/$encodedBucket/$encodedPath" else "$encodedBucket/$encodedPath"
    return "$cleanBase/storage/v1/object/$segment"
  }

  private fun encodeSegment(segment: String): String {
    return URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
  }
}
