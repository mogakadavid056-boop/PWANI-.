package com.example.data

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.example.util.JsonHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
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

data class VideoBackgroundSource(
  val localFilePath: String? = null,
  val remoteUrl: String? = null,
  val apikeyHeader: String? = null,
  val bearerHeader: String? = null,
  val versionKey: String = ""
) {
  val isLocalCached: Boolean
    get() = !localFilePath.isNullOrBlank() && File(localFilePath).let { it.exists() && it.length() > 0L }

  val playbackUri: String?
    get() = if (isLocalCached) Uri.fromFile(File(localFilePath!!)).toString() else remoteUrl
}

data class AppBackgroundState(
  val bg001: VideoBackgroundSource? = null,
  val bg002: VideoBackgroundSource? = null
)

@OptIn(UnstableApi::class)
internal object VideoBackgroundCache {
  private const val MEDIA3_CACHE_DIR = "video_bg_stream_cache"
  private const val MAX_STREAM_CACHE_BYTES = 150L * 1024L * 1024L // 150 MB

  @Volatile
  private var simpleCache: SimpleCache? = null

  @Synchronized
  fun getSimpleCache(context: Context): SimpleCache {
    simpleCache?.let { return it }
    val appCtx = context.applicationContext
    val dir = File(appCtx.cacheDir, MEDIA3_CACHE_DIR).apply { mkdirs() }
    val dbProvider = StandaloneDatabaseProvider(appCtx)
    val evictor = LeastRecentlyUsedCacheEvictor(MAX_STREAM_CACHE_BYTES)
    return SimpleCache(dir, evictor, dbProvider).also { simpleCache = it }
  }

  fun createDataSourceFactory(
    context: Context,
    source: VideoBackgroundSource?,
    useStreamCache: Boolean = true
  ): DataSource.Factory {
    val appCtx = context.applicationContext
    val isLocalFile = source?.playbackUri?.startsWith("file://") == true
    if (isLocalFile || !useStreamCache) {
      val httpFactory = DefaultHttpDataSource.Factory()
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(30_000)
        .setAllowCrossProtocolRedirects(true)
      applyHeaders(httpFactory, source)
      return DefaultDataSource.Factory(appCtx, httpFactory)
    }

    val httpFactory = DefaultHttpDataSource.Factory()
      .setConnectTimeoutMs(15_000)
      .setReadTimeoutMs(30_000)
      .setAllowCrossProtocolRedirects(true)
    applyHeaders(httpFactory, source)

    val upstreamFactory = DefaultDataSource.Factory(appCtx, httpFactory)
    return CacheDataSource.Factory()
      .setCache(getSimpleCache(appCtx))
      .setUpstreamDataSourceFactory(upstreamFactory)
      .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
  }

  private fun applyHeaders(httpFactory: DefaultHttpDataSource.Factory, source: VideoBackgroundSource?) {
    val headers = buildMap {
      if (!source?.apikeyHeader.isNullOrBlank()) put("apikey", source!!.apikeyHeader!!)
      if (!source?.bearerHeader.isNullOrBlank()) put("Authorization", "Bearer ${source!!.bearerHeader!!}")
    }
    if (headers.isNotEmpty()) {
      httpFactory.setDefaultRequestProperties(headers)
    }
  }
}

object AppBackgroundManager {
  private const val LEGACY_PREFS_NAME = "classrep_app_backgrounds"
  private const val LEGACY_DIR_NAME = "app_backgrounds"

  private const val PREFS_NAME = "classrep_video_backgrounds"
  private const val DIR_NAME = "video_backgrounds"
  private const val FILE_001 = "Background001.mp4"
  private const val FILE_002 = "Background002.mp4"
  private const val MIN_REFRESH_INTERVAL_MS = 45_000L
  private const val MISSING_VIDEO_RETRY_INTERVAL_MS = 3_000L

  private val CANDIDATE_BUCKETS = listOf(
    "app-backgrounds", "backgrounds", "background", "videos", "assets",
    "app-assets", "media", "public", "wallpapers", "files", "uploads",
    "classrep", "classrep-assets", "branding", "ui"
  )
  private val SLOT_001_NAMES = listOf(
    "Background001.mp4", "background001.mp4", "Background001.webm", "background001.webm",
    "Background001", "background001"
  )
  private val SLOT_002_NAMES = listOf(
    "Background002.mp4", "background002.mp4", "Background002.webm", "background002.webm",
    "Background002", "background002"
  )

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val syncMutex = Mutex()
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
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
      syncMutex.withLock {
        cleanupLegacyImageBackgrounds(ctx)
        loadFromDiskCache(ctx)
        syncFromSupabase(ctx, accessToken)
      }
    }
  }

  fun refreshIfNeeded(accessToken: String? = null, force: Boolean = false) {
    val ctx = appContext ?: return
    val now = System.currentTimeMillis()
    val tokenChanged = !accessToken.isNullOrBlank() && accessToken != lastTokenUsed
    val missingAnySlot = _state.value.bg001 == null || _state.value.bg002 == null
    val cooldownMs = if (missingAnySlot) MISSING_VIDEO_RETRY_INTERVAL_MS else MIN_REFRESH_INTERVAL_MS

    if (!force && !tokenChanged && (now - lastFetchAtMs) < cooldownMs) {
      return
    }
    lastFetchAtMs = now
    if (!accessToken.isNullOrBlank()) lastTokenUsed = accessToken

    scope.launch {
      syncMutex.withLock {
        syncFromSupabase(ctx, accessToken)
      }
    }
  }

  internal fun cleanupLegacyImageBackgrounds(context: Context) {
    runCatching {
      val legacyDir = File(context.filesDir, LEGACY_DIR_NAME)
      if (legacyDir.exists()) {
        legacyDir.deleteRecursively()
      }
      context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .clear()
        .apply()
    }
  }

  private fun loadFromDiskCache(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val dir = File(context.filesDir, DIR_NAME)
    val f1 = File(dir, FILE_001)
    val f2 = File(dir, FILE_002)

    val src1 = if (f1.exists() && f1.length() > 0L) {
      VideoBackgroundSource(
        localFilePath = f1.absolutePath,
        versionKey = prefs.getString("version_001", "").orEmpty()
      )
    } else null

    val src2 = if (f2.exists() && f2.length() > 0L) {
      VideoBackgroundSource(
        localFilePath = f2.absolutePath,
        versionKey = prefs.getString("version_002", "").orEmpty()
      )
    } else null

    if (src1 != null || src2 != null) {
      _state.update { cur ->
        cur.copy(
          bg001 = src1 ?: cur.bg001,
          bg002 = src2 ?: cur.bg002
        )
      }
    }
  }

  private fun syncFromSupabase(context: Context, accessToken: String?) {
    if (!SupabaseConfig.isConfigured()) return
    lastFetchAtMs = System.currentTimeMillis()
    val resolved = fetchViaRpc(accessToken) ?: discoverViaStorageFallback(accessToken)

    resolved.bg001?.let { spec ->
      syncSlot(
        context = context,
        slotKey = "001",
        fileName = FILE_001,
        spec = spec,
        accessToken = accessToken,
        onReady = { src -> _state.update { it.copy(bg001 = src) } }
      )
    }
    resolved.bg002?.let { spec ->
      syncSlot(
        context = context,
        slotKey = "002",
        fileName = FILE_002,
        spec = spec,
        accessToken = accessToken,
        onReady = { src -> _state.update { it.copy(bg002 = src) } }
      )
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
              if (!isVideoObjectName(name)) continue
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

  internal fun isVideoObjectName(name: String): Boolean {
    val lower = name.trim().lowercase()
    return lower.endsWith(".mp4") || lower.endsWith(".webm") || !lower.contains('.')
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
    accessToken: String?,
    onReady: (VideoBackgroundSource) -> Unit
  ) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
    val targetFile = File(dir, fileName)
    val versionKey = "${spec.bucket}|${spec.path}|${spec.updatedAt}"
    val cachedVersion = prefs.getString("version_$slotKey", null)

    if (cachedVersion == versionKey && targetFile.exists() && targetFile.length() > 0L) {
      onReady(
        VideoBackgroundSource(
          localFilePath = targetFile.absolutePath,
          versionKey = versionKey
        )
      )
      return
    }

    val remoteUrl = buildObjectUrl(SupabaseConfig.url, spec.bucket, spec.path, spec.isPublic)
    val bearer = accessToken?.takeIf { it.isNotBlank() } ?: SupabaseConfig.anonKey

    if (!targetFile.exists() || targetFile.length() == 0L) {
      onReady(
        VideoBackgroundSource(
          localFilePath = null,
          remoteUrl = remoteUrl,
          apikeyHeader = if (spec.isPublic) null else SupabaseConfig.anonKey,
          bearerHeader = if (spec.isPublic) null else bearer,
          versionKey = versionKey
        )
      )
    }

    val cachedEtag = prefs.getString("etag_$slotKey", null)
      ?.takeIf { cachedVersion?.startsWith("${spec.bucket}|${spec.path}|") == true }
    val candidates = buildCandidateDownloadRequests(spec, accessToken, cachedEtag)

    for (req in candidates) {
      val downloaded = runCatching {
        client.newCall(req).execute().use { resp ->
          if (resp.code == 304 && targetFile.exists() && targetFile.length() > 0L) {
            prefs.edit().putString("version_$slotKey", versionKey).apply()
            return@use false
          }
          if (!resp.isSuccessful) return@use null
          val body = resp.body ?: return@use null
          val tmpFile = File(dir, "$fileName.tmp")
          var totalWritten = 0L
          body.byteStream().use { input ->
            FileOutputStream(tmpFile).use { output ->
              val buffer = ByteArray(16 * 1024)
              while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
                totalWritten += read
              }
            }
          }
          if (totalWritten <= 0L) {
            tmpFile.delete()
            return@use null
          }
          if (targetFile.exists()) targetFile.delete()
          if (!tmpFile.renameTo(targetFile)) {
            tmpFile.copyTo(targetFile, overwrite = true)
            tmpFile.delete()
          }

          val newEtag = resp.header("ETag")
          prefs.edit()
            .putString("version_$slotKey", versionKey)
            .apply { if (!newEtag.isNullOrBlank()) putString("etag_$slotKey", newEtag) }
            .apply()
          true
        }
      }.getOrNull()

      if (downloaded == true || downloaded == false) {
        onReady(
          VideoBackgroundSource(
            localFilePath = targetFile.absolutePath,
            remoteUrl = remoteUrl,
            versionKey = versionKey
          )
        )
        return
      }
    }
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
