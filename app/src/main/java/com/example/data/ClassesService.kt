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

class ClassesService(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()
) {

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun fetchClasses(
    userId: String,
    role: String,
    accessToken: String
  ): Result<List<ClassItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val isLecturer = role.equals("lecturer", ignoreCase = true)
    val url = "${SupabaseConfig.url}/rest/v1/classes?select=id,name,code,join_code,year_of_study,created_by,programmes(name),class_members(user_id,status)&order=created_at.desc"

    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .get()
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val bodyString = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
        }

        val array = JSONArray(bodyString)
        val list = mutableListOf<ClassItem>()

        for (i in 0 until array.length()) {
          val item = array.getJSONObject(i)
          val id = item.optString("id")
          val name = item.optString("name")
          val code = item.optString("code")
          val joinCode = item.optString("join_code").takeIf { it.isNotBlank() }
          val yearOfStudy = if (item.has("year_of_study") && !item.isNull("year_of_study")) item.optInt("year_of_study") else null
          val progObj = item.optJSONObject("programmes")
          val progName = progObj?.optString("name")?.takeIf { it.isNotBlank() }
          val createdBy = item.optString("created_by")

          if (isLecturer && userId.isNotBlank()) {
            val isCreator = createdBy.equals(userId, ignoreCase = true)
            var isApprovedMember = false
            val membersArr = item.optJSONArray("class_members")
            if (membersArr != null) {
              for (m in 0 until membersArr.length()) {
                val mObj = membersArr.getJSONObject(m)
                val mUid = mObj.optString("user_id")
                val mStatus = mObj.optString("status", "active")
                if (mUid.equals(userId, ignoreCase = true) && mStatus.equals("active", ignoreCase = true)) {
                  isApprovedMember = true
                  break
                }
              }
            }
            if (!isCreator && !isApprovedMember) {
              continue
            }
          }

          if (id.isNotBlank() || name.isNotBlank()) {
            list.add(
              ClassItem(
                id = id,
                name = name,
                code = code,
                joinCode = joinCode,
                yearOfStudy = yearOfStudy,
                programmeName = progName
              )
            )
          }
        }

        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to load classes"))
    }
  }

  suspend fun joinMyClass(accessToken: String): Result<String?> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val url = "${SupabaseConfig.url}/rest/v1/rpc/join_my_class"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .post("{}".toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty().trim()
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, response.code)))
        }
        val classId = parseClassIdFromRpc(body)
        Result.success(classId)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to find your class"))
    }
  }

  suspend fun joinClass(
    joinCode: String,
    accessToken: String
  ): Result<String?> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val requestBody = JSONObject().apply {
      put("class_join_code", joinCode.trim())
    }

    val url = "${SupabaseConfig.url}/rest/v1/rpc/join_class_by_code"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .post(requestBody.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val bodyString = response.body?.string().orEmpty().trim()
        if (response.isSuccessful) {
          val classId = parseClassIdFromRpc(bodyString)
          Result.success(classId)
        } else {
          Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to join class"))
    }
  }

  suspend fun createClass(
    className: String,
    classCode: String,
    targetUniversityId: String? = null,
    targetSchoolId: String? = null,
    targetProgrammeId: String? = null,
    targetAcademicYearId: String? = null,
    targetYearOfStudy: Int? = null,
    accessToken: String
  ): Result<String> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val requestBody = JSONObject().apply {
      put("class_name", className)
      put("class_code", classCode)
      put("target_university_id", if (!targetUniversityId.isNullOrBlank()) targetUniversityId else JSONObject.NULL)
      put("target_school_id", if (!targetSchoolId.isNullOrBlank()) targetSchoolId else JSONObject.NULL)
      put("target_programme_id", if (!targetProgrammeId.isNullOrBlank()) targetProgrammeId else JSONObject.NULL)
      put("target_academic_year_id", if (!targetAcademicYearId.isNullOrBlank()) targetAcademicYearId else JSONObject.NULL)
      put("target_year_of_study", if (targetYearOfStudy != null) targetYearOfStudy else JSONObject.NULL)
    }

    val url = "${SupabaseConfig.url}/rest/v1/rpc/create_class"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .post(requestBody.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val bodyString = response.body?.string().orEmpty().trim()
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
        }

        val parsedJoinCode = parseGeneratedJoinCode(bodyString)
        Result.success(parsedJoinCode)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to create class"))
    }
  }

  private fun parseClassIdFromRpc(body: String): String? {
    val trimmed = body.trim().removeSurrounding("\"").removeSurrounding("'")
    if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) return null
    if (trimmed.startsWith("{")) {
      return runCatching { JSONObject(trimmed).optString("id", JSONObject(trimmed).optString("class_id", "")) }.getOrNull()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    }
    return trimmed.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
  }

  private fun parseGeneratedJoinCode(responseBody: String): String {
    if (responseBody.isBlank()) return "SUCCESS"
    val trimmed = responseBody.trim().removeSurrounding("\"").removeSurrounding("'")
    if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
      return trimmed
    }
    return try {
      if (trimmed.startsWith("{")) {
        val json = JSONObject(trimmed)
        json.optString("join_code", json.optString("class_join_code", json.optString("code", trimmed)))
      } else {
        val arr = JSONArray(trimmed)
        if (arr.length() > 0) {
          val first = arr.get(0)
          if (first is JSONObject) {
            first.optString("join_code", first.optString("code", ""))
          } else {
            first.toString().removeSurrounding("\"")
          }
        } else trimmed
      }
    } catch (_: Exception) {
      trimmed
    }
  }

  suspend fun updateClassName(
    classId: String,
    newName: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val requestBody = JSONObject().apply {
      put("name", newName.trim())
    }

    val url = "${SupabaseConfig.url}/rest/v1/classes?id=eq.$classId"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .patch(requestBody.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val bodyString = response.body?.string().orEmpty()
        if (response.isSuccessful) {
          Result.success(Unit)
        } else {
          Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to update class name"))
    }
  }

  private val prefectService = ClassPrefectService(client)

  suspend fun listClassReps(
    classId: String,
    accessToken: String
  ): Result<List<ClassPrefectInfo>> = prefectService.listClassReps(classId, accessToken)

  suspend fun listClassesWithoutRep(
    accessToken: String
  ): Result<List<ClassWaitingForPrefect>> = prefectService.listClassesWithoutRep(accessToken)
}
