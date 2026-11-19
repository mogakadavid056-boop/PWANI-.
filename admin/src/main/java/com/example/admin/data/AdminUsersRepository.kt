package com.example.admin.data

import com.example.admin.util.AdminJsonHelper
import com.example.admin.util.AdminRoleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class AdminUsersRepository(
  private val client: OkHttpClient = SupabaseAdminConfig.sharedHttpClient
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  private fun postRpc(rpcName: String, body: JSONObject, accessToken: String): Result<String> {
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
    return try {
      client.newCall(req).execute().use { resp ->
        val raw = resp.body?.string().orEmpty()
        if (resp.isSuccessful) Result.success(raw)
        else Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(raw, resp.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, "Network request failed")))
    }
  }

  suspend fun fetchDashboardStats(accessToken: String): Result<AdminDashboardStats> =
    withContext(Dispatchers.IO) {
      val rpcRes = postRpc("admin_dashboard_stats", JSONObject(), accessToken)
      if (rpcRes.isSuccess) {
        return@withContext runCatching {
          val obj = JSONObject(rpcRes.getOrThrow())
          AdminDashboardStats(
            totalUsers = AdminJsonHelper.intVal(obj, "total_users"),
            totalStudents = AdminJsonHelper.intVal(obj, "total_students"),
            totalClassReps = AdminJsonHelper.intVal(obj, "total_class_reps"),
            totalLecturers = AdminJsonHelper.intVal(obj, "total_lecturers"),
            unverifiedLecturers = AdminJsonHelper.intVal(obj, "unverified_lecturers"),
            totalAdmins = AdminJsonHelper.intVal(obj, "total_admins"),
            totalSchools = AdminJsonHelper.intVal(obj, "total_schools"),
            totalProgrammes = AdminJsonHelper.intVal(obj, "total_programmes"),
            totalClasses = AdminJsonHelper.intVal(obj, "total_classes"),
            totalRegisters = AdminJsonHelper.intVal(obj, "total_registers"),
            openRegisters = AdminJsonHelper.intVal(obj, "open_registers"),
            rpcSqlInstalled = true
          )
        }
      }
      // Fallback using existing list_users RPC + public counts if admin_app_setup.sql hasn't been run yet
      val users = listUsers("", 200, accessToken).getOrNull().orEmpty()
      val schoolsCount = fetchCount("schools", accessToken)
      val progCount = fetchCount("programmes", accessToken)
      val classesCount = fetchCount("classes", accessToken)
      Result.success(
        AdminDashboardStats(
          totalUsers = users.size,
          totalStudents = users.count { it.isStudent },
          totalClassReps = users.count { it.isClassRep },
          totalLecturers = users.count { it.isLecturer },
          unverifiedLecturers = users.count { it.isLecturer && !it.isVerified },
          totalAdmins = users.count { it.isAdmin },
          totalSchools = schoolsCount,
          totalProgrammes = progCount,
          totalClasses = classesCount,
          rpcSqlInstalled = false
        )
      )
    }

  private fun fetchCount(table: String, accessToken: String): Int {
    val req = Request.Builder()
      .url("${SupabaseAdminConfig.url}/rest/v1/$table?select=id")
      .addHeader("apikey", SupabaseAdminConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Accept", "application/json")
      .get()
      .build()
    return try {
      client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) 0 else JSONArray(resp.body?.string().orEmpty()).length()
      }
    } catch (_: Exception) {
      0
    }
  }

  suspend fun listUsers(
    searchText: String,
    maxRows: Int = 100,
    accessToken: String
  ): Result<List<AdminUserItem>> = withContext(Dispatchers.IO) {
    val payload = JSONObject().apply {
      put("search_text", searchText)
      put("max_rows", maxRows)
    }
    val rpcRes = postRpc("list_users", payload, accessToken)
    if (rpcRes.isSuccess) {
      val parsed = runCatching { parseRpcUsers(rpcRes.getOrThrow()) }
      if (parsed.isSuccess) return@withContext parsed
    }
    // Fallback: query profiles + user_roles directly over PostgREST
    val profRes = AdminHttpHelper.getRest(client, "profiles?select=*&limit=500", accessToken)
    if (profRes.isFailure) return@withContext Result.failure(profRes.exceptionOrNull() ?: Exception("Failed to load users"))
    val rolesRes = AdminHttpHelper.getRest(client, "user_roles?select=user_id,role,verified&limit=500", accessToken)
    runCatching {
      mergeProfilesAndRoles(profRes.getOrThrow(), rolesRes.getOrDefault("[]"), searchText, maxRows)
    }
  }

  suspend fun assignRole(
    targetUserId: String,
    newRole: String,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val payload = JSONObject().apply {
      put("target_user_id", targetUserId)
      put("new_role", newRole)
    }
    val rpc = postRpc("admin_assign_role", payload, accessToken)
    if (rpc.isSuccess) return@withContext Result.success(Unit)
    upsertUserRoleRest(targetUserId, JSONObject().apply { put("role", newRole) }, accessToken)
  }

  suspend fun setLecturerVerified(
    targetUserId: String,
    isVerified: Boolean,
    accessToken: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val payload = JSONObject().apply {
      put("target_user_id", targetUserId)
      put("is_verified", isVerified)
    }
    val rpc = postRpc("admin_set_lecturer_verified", payload, accessToken)
    if (rpc.isSuccess) return@withContext Result.success(Unit)
    upsertUserRoleRest(
      targetUserId,
      JSONObject().apply { put("role", AdminRoleHelper.ROLE_LECTURER); put("verified", isVerified) },
      accessToken
    )
  }

  private fun upsertUserRoleRest(targetUserId: String, body: JSONObject, accessToken: String): Result<Unit> {
    if (!SupabaseAdminConfig.isConfigured()) return Result.failure(IllegalStateException(SupabaseAdminConfig.MISSING_CONFIG_ERROR))
    val patchReq = Request.Builder()
      .url("${SupabaseAdminConfig.url}/rest/v1/user_roles?user_id=eq.$targetUserId")
      .addHeader("apikey", SupabaseAdminConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Prefer", "return=representation")
      .patch(body.toString().toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(patchReq).execute().use { resp ->
        val raw = resp.body?.string().orEmpty()
        if (resp.isSuccessful && raw.trim().startsWith("[") && JSONArray(raw).length() > 0) {
          return Result.success(Unit)
        }
      }
    } catch (_: Exception) { }
    val upsertBody = JSONObject(body.toString()).apply { put("user_id", targetUserId) }
    val postReq = Request.Builder()
      .url("${SupabaseAdminConfig.url}/rest/v1/user_roles?on_conflict=user_id")
      .addHeader("apikey", SupabaseAdminConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
      .post(upsertBody.toString().toRequestBody(jsonMediaType))
      .build()
    return try {
      client.newCall(postReq).execute().use { resp ->
        val raw = resp.body?.string().orEmpty()
        if (resp.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(SupabaseAdminConfig.extractErrorMessage(raw, resp.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseAdminConfig.formatExceptionMessage(e, "Failed to update user role")))
    }
  }

  companion object {
    fun parseRpcUsers(body: String): List<AdminUserItem> {
      val arr = JSONArray(body)
      return buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          add(
            AdminUserItem(
              id = AdminJsonHelper.firstStr(obj, "id", "user_id"),
              fullName = AdminJsonHelper.firstStr(obj, "full_name", "name"),
              email = AdminJsonHelper.str(obj, "email"),
              admissionNumber = AdminJsonHelper.str(obj, "admission_number"),
              role = AdminRoleHelper.normalizeRole(AdminJsonHelper.str(obj, "role"))
                .ifBlank { AdminRoleHelper.ROLE_STUDENT },
              isVerified = AdminJsonHelper.bool(obj, "verified", false)
            )
          )
        }
      }
    }

    fun mergeProfilesAndRoles(
      profilesJson: String,
      rolesJson: String,
      searchText: String,
      maxRows: Int
    ): List<AdminUserItem> {
      val roleMap = mutableMapOf<String, Pair<String, Boolean>>()
      val rolesArr = runCatching { JSONArray(rolesJson) }.getOrNull() ?: JSONArray()
      for (i in 0 until rolesArr.length()) {
        val o = rolesArr.optJSONObject(i) ?: continue
        val uid = AdminJsonHelper.firstStr(o, "user_id", "id")
        if (uid.isNotBlank()) {
          val role = AdminRoleHelper.normalizeRole(AdminJsonHelper.str(o, "role")).ifBlank { AdminRoleHelper.ROLE_STUDENT }
          val verified = AdminJsonHelper.bool(o, "verified", false)
          roleMap[uid] = role to verified
        }
      }
      val q = searchText.trim().lowercase()
      val seen = mutableSetOf<String>()
      val list = mutableListOf<AdminUserItem>()
      val profArr = runCatching { JSONArray(profilesJson) }.getOrNull() ?: JSONArray()
      for (i in 0 until profArr.length()) {
        val o = profArr.optJSONObject(i) ?: continue
        val uid = AdminJsonHelper.firstStr(o, "id", "user_id")
        if (uid.isBlank() || !seen.add(uid)) continue
        val pair = roleMap[uid]
        val role = pair?.first ?: AdminRoleHelper.normalizeRole(AdminJsonHelper.str(o, "role")).ifBlank { AdminRoleHelper.ROLE_STUDENT }
        val verified = pair?.second ?: AdminJsonHelper.bool(o, "verified", false)
        list.add(
          AdminUserItem(
            id = uid,
            fullName = AdminJsonHelper.firstStr(o, "full_name", "name"),
            email = AdminJsonHelper.str(o, "email"),
            admissionNumber = AdminJsonHelper.firstStr(o, "admission_number", "staff_number"),
            role = role,
            isVerified = verified
          )
        )
      }
      val filtered = if (q.isBlank()) list else list.filter {
        it.fullName.lowercase().contains(q) ||
          it.email.lowercase().contains(q) ||
          it.admissionNumber.lowercase().contains(q) ||
          it.role.lowercase().contains(q)
      }
      return filtered.sortedBy { it.fullName.ifBlank { it.email }.lowercase() }.take(maxRows.coerceIn(1, 500))
    }
  }
}
