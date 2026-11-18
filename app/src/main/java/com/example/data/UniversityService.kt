package com.example.data

import com.example.util.JsonHelper
import com.example.util.RoleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class UniversityService(
  private val client: OkHttpClient = OfflineCache.getClient()
) {

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun fetchSchools(token: String? = null): Result<List<SchoolItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val authToken = if (!token.isNullOrBlank()) token else SupabaseConfig.anonKey
    val url = "${SupabaseConfig.url}/rest/v1/schools?select=id,name,university_id&order=name.asc"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $authToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .get()
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, response.code)))
        }
        val array = JSONArray(body)
        val list = mutableListOf<SchoolItem>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          list.add(
            SchoolItem(
              id = JsonHelper.str(obj, "id"),
              name = JsonHelper.str(obj, "name"),
              universityId = JsonHelper.str(obj, "university_id")
            )
          )
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to load schools")))
    }
  }

  suspend fun fetchProgrammes(schoolId: String, token: String? = null): Result<List<ProgrammeItem>> =
    withContext(Dispatchers.IO) {
      if (!SupabaseConfig.isConfigured() || schoolId.isBlank()) {
        return@withContext Result.success(emptyList())
      }
      val authToken = if (!token.isNullOrBlank()) token else SupabaseConfig.anonKey
      val url = "${SupabaseConfig.url}/rest/v1/programmes?school_id=eq.$schoolId&select=id,name,duration_years&order=name.asc"
      val request = Request.Builder()
        .url(url)
        .addHeader("apikey", SupabaseConfig.anonKey)
        .addHeader("Authorization", "Bearer $authToken")
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "application/json")
        .get()
        .build()

      try {
        client.newCall(request).execute().use { response ->
          val body = response.body?.string().orEmpty()
          if (!response.isSuccessful) {
            return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, response.code)))
          }
          val array = JSONArray(body)
          val list = mutableListOf<ProgrammeItem>()
          for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val duration = if (obj.has("duration_years") && !obj.isNull("duration_years")) {
              obj.optInt("duration_years", 4)
            } else null
            list.add(
              ProgrammeItem(
                id = JsonHelper.str(obj, "id"),
                name = JsonHelper.str(obj, "name"),
                schoolId = schoolId,
                durationYears = duration
              )
            )
          }
          Result.success(list)
        }
      } catch (e: Exception) {
        Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to load programmes")))
      }
    }

  suspend fun fetchAcademicYears(token: String): Result<List<AcademicYearItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val url = "${SupabaseConfig.url}/rest/v1/academic_years?select=id,name,is_current&order=name.desc"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $token")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .get()
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, response.code)))
        }
        val array = JSONArray(body)
        val list = mutableListOf<AcademicYearItem>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          list.add(
            AcademicYearItem(
              id = JsonHelper.str(obj, "id"),
              name = JsonHelper.str(obj, "name"),
              isCurrent = JsonHelper.bool(obj, "is_current", false)
            )
          )
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to load academic years")))
    }
  }

  suspend fun fetchProfile(userId: String, token: String): Result<ProfileItem> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured() || userId.isBlank()) {
      return@withContext Result.failure(IllegalStateException("User ID required"))
    }
    val url = "${SupabaseConfig.url}/rest/v1/profiles?id=eq.$userId&select=*,schools:school_id(name),programmes:programme_id(name)"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $token")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .get()
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, response.code)))
        }
        val array = JSONArray(body)
        if (array.length() == 0) {
          return@withContext Result.failure(Exception("Profile not found"))
        }
        val obj = array.getJSONObject(0)
        val schoolsObj = obj.optJSONObject("schools")
        val programmesObj = obj.optJSONObject("programmes")
        val schoolName = JsonHelper.strOrNull(schoolsObj, "name")
        val programmeName = JsonHelper.strOrNull(programmesObj, "name")
        val yearOfStudy = if (obj.has("year_of_study") && !obj.isNull("year_of_study")) {
          obj.optInt("year_of_study")
        } else null

        Result.success(
          ProfileItem(
            id = JsonHelper.str(obj, "id").ifBlank { userId },
            fullName = JsonHelper.str(obj, "full_name"),
            email = JsonHelper.str(obj, "email"),
            admissionNumber = JsonHelper.str(obj, "admission_number"),
            role = RoleHelper.normalizeRole(JsonHelper.str(obj, "role")),
            yearOfStudy = yearOfStudy,
            schoolId = JsonHelper.strOrNull(obj, "school_id"),
            schoolName = schoolName,
            programmeId = JsonHelper.strOrNull(obj, "programme_id"),
            programmeName = programmeName,
            universityId = JsonHelper.strOrNull(obj, "university_id")
          )
        )
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to fetch profile")))
    }
  }

  suspend fun patchProfile(
    userId: String,
    token: String,
    payload: JSONObject
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured() || userId.isBlank()) {
      return@withContext Result.failure(IllegalStateException("User ID required"))
    }
    val url = "${SupabaseConfig.url}/rest/v1/profiles?id=eq.$userId"
    val authToken = if (token.isNotBlank() && token != SupabaseConfig.anonKey) token else SupabaseConfig.anonKey
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $authToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Prefer", "return=minimal")
      .patch(payload.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          Result.success(Unit)
        } else {
          val body = response.body?.string().orEmpty()
          Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, response.code)))
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to update profile")))
    }
  }
}
