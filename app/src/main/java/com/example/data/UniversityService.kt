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

class UniversityService(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()
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
              id = obj.optString("id"),
              name = obj.optString("name"),
              universityId = obj.optString("university_id")
            )
          )
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to load schools"))
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
                id = obj.optString("id"),
                name = obj.optString("name"),
                schoolId = schoolId,
                durationYears = duration
              )
            )
          }
          Result.success(list)
        }
      } catch (e: Exception) {
        Result.failure(Exception(e.localizedMessage ?: "Failed to load programmes"))
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
              id = obj.optString("id"),
              name = obj.optString("name"),
              isCurrent = obj.optBoolean("is_current", false)
            )
          )
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to load academic years"))
    }
  }

  suspend fun fetchProfile(userId: String, token: String): Result<ProfileItem> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured() || userId.isBlank()) {
      return@withContext Result.failure(IllegalStateException("User ID required"))
    }
    val url = "${SupabaseConfig.url}/rest/v1/profiles?id=eq.$userId&select=id,full_name,email,admission_number,year_of_study,school_id,programme_id,university_id,schools:school_id(name),programmes:programme_id(name)"
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
        val schoolName = schoolsObj?.optString("name")?.takeIf { it.isNotBlank() }
        val programmeName = programmesObj?.optString("name")?.takeIf { it.isNotBlank() }
        val yearOfStudy = if (obj.has("year_of_study") && !obj.isNull("year_of_study")) {
          obj.optInt("year_of_study")
        } else null

        Result.success(
          ProfileItem(
            id = obj.optString("id", userId),
            fullName = obj.optString("full_name"),
            email = obj.optString("email"),
            admissionNumber = obj.optString("admission_number"),
            yearOfStudy = yearOfStudy,
            schoolId = obj.optString("school_id").takeIf { it.isNotBlank() },
            schoolName = schoolName,
            programmeId = obj.optString("programme_id").takeIf { it.isNotBlank() },
            programmeName = programmeName,
            universityId = obj.optString("university_id").takeIf { it.isNotBlank() }
          )
        )
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to fetch profile"))
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
      Result.failure(Exception(e.localizedMessage ?: "Failed to update profile"))
    }
  }
}
