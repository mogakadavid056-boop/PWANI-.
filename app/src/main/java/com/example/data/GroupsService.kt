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

class GroupsService(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()
) {

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  private suspend fun postRpc(
    rpcName: String,
    body: JSONObject,
    accessToken: String
  ): Result<String> = withContext(Dispatchers.IO) {
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
          Result.success(bodyString)
        } else {
          Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyString, response.code)))
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Request failed"))
    }
  }

  suspend fun listGroups(
    classId: String,
    accessToken: String
  ): Result<List<DiscussionGroupItem>> {
    val body = JSONObject().apply { put("target_class_id", classId) }
    val result = postRpc("list_groups", body, accessToken)
    return result.mapCatching { bodyString ->
      val array = JSONArray(bodyString)
      val list = mutableListOf<DiscussionGroupItem>()
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        list.add(
          DiscussionGroupItem(
            id = obj.optString("id").ifBlank { obj.optString("group_id") },
            name = obj.optString("group_name").ifBlank { obj.optString("name", "Group") },
            description = obj.optString("description").ifBlank { obj.optString("group_description", "") },
            memberCount = obj.optInt("member_count", obj.optInt("members_count", 1)),
            maxMembers = obj.optInt("max_members", obj.optInt("max_size", 50)),
            isMember = obj.optBoolean("is_member", false),
            creatorId = obj.optString("creator_id").ifBlank { if (obj.has("created_by")) obj.optString("created_by") else null },
            isOwner = obj.optBoolean("is_owner", false)
          )
        )
      }
      list
    }
  }

  suspend fun joinGroup(groupId: String, accessToken: String): Result<Unit> {
    val body = JSONObject().apply { put("target_group_id", groupId) }
    return postRpc("join_group", body, accessToken).map { }
  }

  suspend fun createGroup(
    classId: String,
    name: String,
    description: String,
    maxSize: Int,
    accessToken: String
  ): Result<Unit> {
    val body = JSONObject().apply {
      put("target_class_id", classId)
      put("group_name", name.trim())
      put("group_description", description.trim())
      put("max_size", maxSize)
    }
    return postRpc("create_group", body, accessToken).map { }
  }

  suspend fun getGroupMessages(
    groupId: String,
    since: String? = null,
    maxRows: Int? = 100,
    accessToken: String
  ): Result<List<GroupMessageItem>> {
    val body = JSONObject().apply {
      put("target_group_id", groupId)
      if (!since.isNullOrBlank()) {
        put("since", since)
      }
      if (maxRows != null && since.isNullOrBlank()) {
        put("max_rows", maxRows)
      }
    }
    val result = postRpc("get_group_messages", body, accessToken)
    return result.mapCatching { bodyString ->
      val array = JSONArray(bodyString)
      val list = mutableListOf<GroupMessageItem>()
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        list.add(
          GroupMessageItem(
            id = obj.optString("id").ifBlank { obj.optString("message_id") },
            groupId = obj.optString("group_id", groupId),
            senderId = obj.optString("sender_id").ifBlank { obj.optString("user_id") },
            senderName = obj.optString("sender_name").ifBlank { obj.optString("full_name", "Student") },
            messageText = obj.optString("message_text").ifBlank { obj.optString("content", obj.optString("text", "")) },
            createdAt = obj.optString("created_at").ifBlank { obj.optString("sent_at", "") }
          )
        )
      }
      list
    }
  }

  suspend fun sendGroupMessage(
    groupId: String,
    messageText: String,
    accessToken: String
  ): Result<Unit> {
    val body = JSONObject().apply {
      put("target_group_id", groupId)
      put("message_text", messageText.trim())
    }
    return postRpc("send_group_message", body, accessToken).map { }
  }

  suspend fun deleteGroupMessage(messageId: String, accessToken: String): Result<Unit> {
    val body = JSONObject().apply { put("target_message_id", messageId) }
    return postRpc("delete_group_message", body, accessToken).map { }
  }

  suspend fun listGroupMembers(groupId: String, accessToken: String): Result<List<GroupMemberItem>> {
    val body = JSONObject().apply { put("target_group_id", groupId) }
    val result = postRpc("list_group_members", body, accessToken)
    return result.mapCatching { bodyString ->
      val array = JSONArray(bodyString)
      val list = mutableListOf<GroupMemberItem>()
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        list.add(
          GroupMemberItem(
            userId = obj.optString("user_id").ifBlank { obj.optString("id") },
            fullName = obj.optString("full_name").ifBlank { obj.optString("name", "Student") },
            admissionNumber = obj.optString("admission_number").ifBlank { obj.optString("adm_no", "N/A") },
            isOwner = obj.optBoolean("is_owner", obj.optBoolean("is_creator", false)),
            role = if (obj.has("role")) obj.optString("role") else null
          )
        )
      }
      list
    }
  }

  suspend fun leaveGroup(groupId: String, accessToken: String): Result<Unit> {
    val body = JSONObject().apply { put("target_group_id", groupId) }
    return postRpc("leave_group", body, accessToken).map { }
  }

  suspend fun updateGroup(
    groupId: String,
    newName: String,
    newDescription: String,
    newMaxSize: Int,
    accessToken: String
  ): Result<Unit> {
    val body = JSONObject().apply {
      put("target_group_id", groupId)
      put("new_name", newName.trim())
      put("new_description", newDescription.trim())
      put("new_max_size", newMaxSize)
    }
    return postRpc("update_group", body, accessToken).map { }
  }

  suspend fun removeGroupMember(
    groupId: String,
    targetUserId: String,
    accessToken: String
  ): Result<Unit> {
    val body = JSONObject().apply {
      put("target_group_id", groupId)
      put("target_user_id", targetUserId)
    }
    return postRpc("remove_group_member", body, accessToken).map { }
  }

  suspend fun deleteGroup(groupId: String, accessToken: String): Result<Unit> {
    val body = JSONObject().apply { put("target_group_id", groupId) }
    return postRpc("delete_group", body, accessToken).map { }
  }
}
