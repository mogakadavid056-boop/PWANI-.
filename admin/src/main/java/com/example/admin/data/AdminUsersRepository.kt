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
    postRpc("list_users", payload, accessToken).mapCatching { body ->
      val arr = JSONArray(body)
      buildList {
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
    postRpc("admin_assign_role", payload, accessToken).map { }
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
    postRpc("admin_set_lecturer_verified", payload, accessToken).map { }
  }
}
