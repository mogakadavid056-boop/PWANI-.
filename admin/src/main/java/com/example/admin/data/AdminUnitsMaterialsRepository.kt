package com.example.admin.data

import com.example.admin.util.AdminJsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.UUID

object AdminFileValidator {
  const val MAX_BYTES = 15L * 1024L * 1024L
  private val ALLOWED_EXT = setOf("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "jpg", "jpeg", "png", "webp")
  private val ALLOWED_MIME = setOf(
    "application/pdf", "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/plain", "image/jpeg", "image/png", "image/webp"
  )

  fun sanitize(name: String): String = name.replace(Regex("[^a-zA-Z0-9.\\-_]"), "_").take(100)

  fun validate(name: String, mime: String, sizeBytes: Long): String? {
    if (sizeBytes > MAX_BYTES) return "File is too large. Maximum allowed size is 15 MB."
    val ext = name.substringAfterLast('.', "").lowercase()
    if (ext !in ALLOWED_EXT && mime.lowercase() !in ALLOWED_MIME) {
      return "Unsupported file type. Choose PDF, Office docs, TXT, or image."
    }
    return null
  }
}

class AdminUnitsMaterialsRepository(
  private val client: OkHttpClient = SupabaseAdminConfig.sharedHttpClient
) {
  suspend fun listClassUnits(classId: String, accessToken: String): Result<List<AdminCourseUnit>> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("target_class_id", classId) }
      val rpc = AdminHttpHelper.postRpc(client, "list_class_units", body, accessToken)
      if (rpc.isSuccess) {
        val parsed = runCatching { parseClassUnits(rpc.getOrThrow()) }
        if (parsed.isSuccess) return@withContext parsed
      }
      AdminHttpHelper.getRest(
        client,
        "courses?class_id=eq.$classId&select=id,code,name,description,status,rejection_reason&order=code.asc",
        accessToken
      ).mapCatching { raw -> parseClassUnits(raw) }
    }

  suspend fun registerUnit(classId: String, code: String, name: String, desc: String, lecturerId: String?, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val cleanCode = code.trim().uppercase()
      val cleanName = name.trim()
      val cleanDesc = desc.trim()
      val body = JSONObject().apply {
        put("target_class_id", classId)
        put("unit_code", cleanCode)
        put("unit_name", cleanName)
        put("unit_description", cleanDesc)
        put("target_lecturer_id", if (lecturerId.isNullOrBlank()) JSONObject.NULL else lecturerId)
      }
      val rpc = AdminHttpHelper.postRpc(client, "request_unit", body, accessToken)
      if (rpc.isSuccess) return@withContext Result.success(Unit)
      val insertBody = JSONObject().apply {
        put("class_id", classId)
        put("code", cleanCode)
        put("name", cleanName)
        put("description", cleanDesc)
        put("status", "approved")
        if (!lecturerId.isNullOrBlank()) put("lecturer_id", lecturerId)
      }
      val insReq = Request.Builder()
        .url("${SupabaseAdminConfig.url}/rest/v1/courses")
        .addHeader("apikey", SupabaseAdminConfig.anonKey)
        .addHeader("Authorization", "Bearer $accessToken")
        .addHeader("Content-Type", "application/json")
        .addHeader("Prefer", "return=representation")
        .post(RequestBody.create(AdminHttpHelper.jsonMediaType, insertBody.toString()))
        .build()
      try {
        client.newCall(insReq).execute().use { resp ->
          val raw = resp.body?.string().orEmpty()
          if (!resp.isSuccessful) {
            return@withContext Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(raw, resp.code)))
          }
          if (!lecturerId.isNullOrBlank()) {
            val courseId = runCatching { JSONArray(raw).optJSONObject(0)?.optString("id").orEmpty() }.getOrDefault("")
            if (courseId.isNotBlank()) {
              AdminHttpHelper.postRest(
                client,
                "course_lecturers",
                JSONObject().apply { put("course_id", courseId); put("lecturer_id", lecturerId) },
                accessToken
              )
            }
          }
          Result.success(Unit)
        }
      } catch (e: Exception) {
        Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, "Failed to register unit")))
      }
    }

  suspend fun setUnitLecturer(courseId: String, lecturerId: String, assign: Boolean, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply {
        put("target_course_id", courseId)
        put("target_lecturer_id", lecturerId)
        put("assign", assign)
      }
      val rpc = AdminHttpHelper.postRpc(client, "admin_set_unit_lecturer", body, accessToken)
      if (rpc.isSuccess) return@withContext Result.success(Unit)
      if (assign) {
        AdminHttpHelper.postRest(
          client,
          "course_lecturers",
          JSONObject().apply { put("course_id", courseId); put("lecturer_id", lecturerId) },
          accessToken
        ).map { }
      } else {
        AdminHttpHelper.deleteRest(
          client,
          "course_lecturers?course_id=eq.$courseId&lecturer_id=eq.$lecturerId",
          accessToken
        ).map { }
      }
    }

  suspend fun listPendingUnits(accessToken: String): Result<List<AdminPendingUnitItem>> =
    withContext(Dispatchers.IO) {
      val rpc = AdminHttpHelper.postRpc(client, "list_pending_units", JSONObject(), accessToken)
      if (rpc.isSuccess) {
        val parsed = runCatching { parsePendingUnits(rpc.getOrThrow()) }
        if (parsed.isSuccess) return@withContext parsed
      }
      AdminHttpHelper.getRest(
        client,
        "courses?status=eq.pending&select=id,class_id,code,name,description,requested_by,created_at,classes(name)&order=created_at.asc",
        accessToken
      ).mapCatching { raw ->
        val profilesMap = fetchProfileNamesMap(accessToken)
        val arr = JSONArray(raw)
        buildList {
          for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val reqBy = AdminJsonHelper.str(o, "requested_by")
            add(
              AdminPendingUnitItem(
                courseId = AdminJsonHelper.firstStr(o, "course_id", "id"),
                classId = AdminJsonHelper.str(o, "class_id"),
                className = AdminJsonHelper.str(o.optJSONObject("classes"), "name").ifBlank { "Class" },
                code = AdminJsonHelper.str(o, "code").uppercase(),
                name = AdminJsonHelper.str(o, "name"),
                description = AdminJsonHelper.str(o, "description"),
                requestedBy = reqBy,
                requesterName = profilesMap[reqBy] ?: "Lecturer",
                createdAt = AdminJsonHelper.str(o, "created_at")
              )
            )
          }
        }
      }
    }

  suspend fun reviewUnit(courseId: String, approve: Boolean, reason: String?, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply {
        put("target_course_id", courseId)
        put("approve", approve)
        put("reason", if (reason.isNullOrBlank()) JSONObject.NULL else reason.trim())
      }
      val rpc = AdminHttpHelper.postRpc(client, "review_unit", body, accessToken)
      if (rpc.isSuccess) return@withContext Result.success(Unit)
      val patch = JSONObject().apply {
        put("status", if (approve) "approved" else "rejected")
        put("rejection_reason", if (approve || reason.isNullOrBlank()) JSONObject.NULL else reason.trim())
      }
      AdminHttpHelper.patchRest(client, "courses?id=eq.$courseId", patch, accessToken).map { }
    }

  private fun parseClassUnits(raw: String): List<AdminCourseUnit> {
    val arr = JSONArray(raw)
    return List(arr.length()) { i ->
      val o = arr.getJSONObject(i)
      val idsArr = o.optJSONArray("lecturer_ids")
      val ids = buildList {
        if (idsArr != null) for (j in 0 until idsArr.length()) {
          val id = idsArr.optString(j)
          if (id.isNotBlank() && !id.equals("null", true)) add(id)
        }
      }
      AdminCourseUnit(
        courseId = AdminJsonHelper.firstStr(o, "course_id", "id"),
        code = AdminJsonHelper.firstStr(o, "code", "course_code").uppercase(),
        name = AdminJsonHelper.firstStr(o, "name", "course_name", "title"),
        description = AdminJsonHelper.str(o, "description"),
        status = AdminJsonHelper.str(o, "status").ifBlank { "approved" }.lowercase(),
        lecturers = AdminJsonHelper.firstStr(o, "lecturers", "lecturer_name"),
        lecturerIds = ids,
        materialCount = AdminJsonHelper.intVal(o, "material_count", 0),
        rejectionReason = AdminJsonHelper.str(o, "rejection_reason")
      )
    }.sortedBy { it.code }
  }

  private fun fetchProfileNamesMap(accessToken: String): Map<String, String> {
    val raw = AdminHttpHelper.getRest(client, "profiles?select=id,full_name&limit=500", accessToken).getOrNull() ?: return emptyMap()
    val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyMap()
    return buildMap {
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val id = AdminJsonHelper.str(o, "id")
        val name = AdminJsonHelper.str(o, "full_name")
        if (id.isNotBlank() && name.isNotBlank()) put(id, name)
      }
    }
  }

  suspend fun fetchMaterials(classId: String, accessToken: String): Result<List<AdminMaterialItem>> =
    withContext(Dispatchers.IO) {
      val q = "materials?class_id=eq.$classId&select=id,class_id,course_id,title,description,material_type,external_url,storage_path,file_name,file_size,mime_type,created_at&order=created_at.desc"
      AdminHttpHelper.getRest(client, q, accessToken).mapCatching { raw ->
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
          val o = arr.getJSONObject(i)
          AdminMaterialItem(
            id = AdminJsonHelper.str(o, "id"),
            classId = AdminJsonHelper.str(o, "class_id"),
            courseId = AdminJsonHelper.str(o, "course_id"),
            title = AdminJsonHelper.str(o, "title"),
            description = AdminJsonHelper.str(o, "description"),
            materialType = AdminJsonHelper.str(o, "material_type").ifBlank { "notes" },
            externalUrl = AdminJsonHelper.str(o, "external_url"),
            storagePath = AdminJsonHelper.str(o, "storage_path"),
            fileName = AdminJsonHelper.str(o, "file_name"),
            fileSize = o.optLong("file_size", 0L),
            mimeType = AdminJsonHelper.str(o, "mime_type"),
            createdAt = AdminJsonHelper.str(o, "created_at")
          )
        }
      }
    }

  suspend fun addLinkMaterial(classId: String, courseId: String, title: String, desc: String, type: String, url: String, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply {
        put("class_id", classId); put("course_id", courseId)
        put("title", title.trim()); put("description", desc.trim())
        put("material_type", type.trim().lowercase()); put("external_url", url.trim())
      }
      AdminHttpHelper.postRest(client, "materials", body, accessToken).map { }
    }

  suspend fun uploadFileMaterial(
    classId: String, courseId: String, title: String, desc: String,
    fileName: String, fileSize: Long, mimeType: String, inputProvider: () -> InputStream,
    accessToken: String, onProgress: (Float) -> Unit
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val safeName = AdminFileValidator.sanitize(fileName)
    val storagePath = "$classId/${UUID.randomUUID()}_$safeName"
    val body = object : RequestBody() {
      override fun contentType() = mimeType.toMediaTypeOrNull()
      override fun contentLength() = fileSize
      override fun writeTo(sink: BufferedSink) {
        val buf = ByteArray(8192); var sent = 0L
        inputProvider().use { inp ->
          var n: Int
          while (inp.read(buf).also { n = it } != -1) {
            sink.write(buf, 0, n); sent += n
            if (fileSize > 0) onProgress((sent.toFloat() / fileSize).coerceIn(0f, 1f))
          }
        }
      }
    }
    val uploadReq = Request.Builder()
      .url("${SupabaseAdminConfig.url}/storage/v1/object/class-materials/$storagePath")
      .addHeader("apikey", SupabaseAdminConfig.anonKey).addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", mimeType).post(body).build()
    try {
      client.newCall(uploadReq).execute().use { resp ->
        if (!resp.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(resp.body?.string().orEmpty(), resp.code)))
        }
      }
    } catch (e: Exception) {
      return@withContext Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, "Upload failed")))
    }
    val row = JSONObject().apply {
      put("class_id", classId); put("course_id", courseId); put("title", title.trim())
      put("description", desc.trim()); put("material_type", "notes"); put("storage_path", storagePath)
      put("file_name", safeName); put("file_size", fileSize); put("mime_type", mimeType)
    }
    val res = AdminHttpHelper.postRest(client, "materials", row, accessToken)
    if (res.isFailure) deleteStorageObject(storagePath, accessToken)
    res.map { }
  }

  suspend fun updateMaterial(materialId: String, title: String, desc: String, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val body = JSONObject().apply { put("title", title.trim()); put("description", desc.trim()) }
      AdminHttpHelper.patchRest(client, "materials?id=eq.$materialId", body, accessToken).map { }
    }

  suspend fun deleteMaterial(item: AdminMaterialItem, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      if (item.storagePath.isNotBlank()) deleteStorageObject(item.storagePath, accessToken)
      AdminHttpHelper.deleteRest(client, "materials?id=eq.${item.id}", accessToken).map { }
    }

  suspend fun downloadSignedFile(storagePath: String, destFile: File, accessToken: String): Result<File> =
    withContext(Dispatchers.IO) {
      val signReq = Request.Builder()
        .url("${SupabaseAdminConfig.url}/storage/v1/object/sign/class-materials/$storagePath")
        .addHeader("apikey", SupabaseAdminConfig.anonKey).addHeader("Authorization", "Bearer $accessToken")
        .addHeader("Content-Type", "application/json")
        .post(RequestBody.create(AdminHttpHelper.jsonMediaType, JSONObject().apply { put("expiresIn", 600) }.toString()))
        .build()
      val signed = try {
        client.newCall(signReq).execute().use { r ->
          val b = r.body?.string().orEmpty()
          if (!r.isSuccessful) return@withContext Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(b, r.code)))
          JSONObject(b).optString("signedURL")
        }
      } catch (e: Exception) {
        return@withContext Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, "Failed to sign download")))
      }
      val fullUrl = if (signed.startsWith("http")) signed else "${SupabaseAdminConfig.url}/storage/v1$signed"
      val dlReq = Request.Builder().url(fullUrl)
        .addHeader("apikey", SupabaseAdminConfig.anonKey).addHeader("Authorization", "Bearer $accessToken").get().build()
      try {
        client.newCall(dlReq).execute().use { r ->
          if (!r.isSuccessful) return@withContext Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(r.body?.string().orEmpty(), r.code)))
          destFile.parentFile?.mkdirs()
          r.body?.byteStream()?.use { inp -> destFile.outputStream().use { out -> inp.copyTo(out) } }
          Result.success(destFile)
        }
      } catch (e: Exception) {
        Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, "Download failed")))
      }
    }

  private fun deleteStorageObject(path: String, accessToken: String) {
    runCatching {
      val req = Request.Builder().url("${SupabaseAdminConfig.url}/storage/v1/object/class-materials/$path")
        .addHeader("apikey", SupabaseAdminConfig.anonKey).addHeader("Authorization", "Bearer $accessToken").delete().build()
      client.newCall(req).execute().close()
    }
  }

  companion object {
    fun parsePendingUnits(raw: String): List<AdminPendingUnitItem> {
      val trimmed = raw.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("[")) return emptyList()
      val arr = JSONArray(trimmed)
      return buildList {
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          add(
            AdminPendingUnitItem(
              courseId = AdminJsonHelper.firstStr(o, "course_id", "id"),
              classId = AdminJsonHelper.str(o, "class_id"),
              className = AdminJsonHelper.str(o, "class_name").ifBlank { "Class" },
              code = AdminJsonHelper.str(o, "code").uppercase(),
              name = AdminJsonHelper.str(o, "name"),
              description = AdminJsonHelper.str(o, "description"),
              requestedBy = AdminJsonHelper.str(o, "requested_by"),
              requesterName = AdminJsonHelper.str(o, "requester_name").ifBlank { "Lecturer" },
              createdAt = AdminJsonHelper.str(o, "created_at")
            )
          )
        }
      }
    }
  }
}
