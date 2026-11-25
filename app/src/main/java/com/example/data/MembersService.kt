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
import java.util.concurrent.TimeUnit

class MembersService(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()
) {

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun listClassMembers(
    classId: String,
    accessToken: String
  ): Result<List<ClassMemberItem>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val body = JSONObject().apply {
      put("target_class_id", classId)
    }

    val url = "${SupabaseConfig.url}/rest/v1/rpc/list_class_members"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .post(body.toString().toRequestBody(jsonMediaType))
      .build()

    try {
      client.newCall(request).execute().use { response ->
        val bodyString = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
        }

        val array = JSONArray(bodyString)
        val list = mutableListOf<ClassMemberItem>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val userId = JsonHelper.firstStr(obj, "user_id", "id", "student_id", "target_user_id")
          val fullName = JsonHelper.firstStr(obj, "full_name", "name", "student_name").ifBlank { "Student" }
          val admNo = JsonHelper.firstStr(obj, "admission_number", "admission_no", "reg_no").ifBlank { "-" }
          val rawIsRep = JsonHelper.bool(obj, "is_rep", false) ||
            JsonHelper.bool(obj, "is_representative", false) ||
            JsonHelper.bool(obj, "rep", false)
          val rawRole = JsonHelper.strOrNull(obj, "role")
          val memberRole = JsonHelper.firstStr(obj, "member_role", "role")
            .ifBlank { if (rawIsRep) "prefect" else "student" }
          val isRep = rawIsRep ||
            RoleHelper.isClassRep(memberRole) ||
            RoleHelper.isClassRep(rawRole)

          list.add(
            ClassMemberItem(
              userId = userId,
              fullName = fullName,
              admissionNumber = admNo,
              isRep = isRep,
              role = rawRole,
              memberRole = memberRole
            )
          )
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to load class members")))
    }
  }

  private suspend fun postRpcRaw(rpcName: String, body: JSONObject, accessToken: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      if (!SupabaseConfig.isConfigured()) {
        return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
      }
      val url = "${SupabaseConfig.url}/rest/v1/rpc/$rpcName"
      val request = Request.Builder()
        .url(url)
        .addHeader("apikey", SupabaseConfig.anonKey)
        .addHeader("Authorization", "Bearer $accessToken")
        .addHeader("Content-Type", "application/json")
        .post(body.toString().toRequestBody(jsonMediaType))
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
        Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Network request failed")))
      }
    }

  suspend fun appointClassRep(
    classId: String,
    targetUserId: String,
    accessToken: String
  ): Result<Unit> {
    val payload = JSONObject().apply {
      put("target_class_id", classId)
      put("target_user_id", targetUserId)
    }
    return postRpcRaw("appoint_class_rep", payload, accessToken)
  }

  suspend fun removeClassRep(
    classId: String,
    targetUserId: String,
    accessToken: String
  ): Result<Unit> {
    val payload = JSONObject().apply {
      put("target_class_id", classId)
      put("target_user_id", targetUserId)
    }
    return postRpcRaw("remove_class_rep", payload, accessToken)
  }

  suspend fun removeFromClass(
    classId: String,
    targetUserId: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }

    val url = "${SupabaseConfig.url}/rest/v1/class_members?class_id=eq.$classId&user_id=eq.$targetUserId"
    val request = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .delete()
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
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to remove member from class")))
    }
  }

  suspend fun adminRemoveMember(
    classId: String,
    targetUserId: String,
    removalReason: String? = null,
    accessToken: String
  ): Result<Unit> {
    val payload = JSONObject().apply {
      put("target_class_id", classId)
      put("target_user_id", targetUserId)
      put("removal_reason", if (removalReason.isNullOrBlank()) JSONObject.NULL else removalReason.trim())
    }
    return postRpcRaw("admin_remove_member", payload, accessToken)
  }
}
