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

class PollsService(
  private val client: OkHttpClient = OfflineCache.getClient()
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
    val req = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(body.toString().toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val bodyStr = resp.body?.string().orEmpty()
        if (resp.isSuccessful) {
          Result.success(bodyStr)
        } else {
          Result.failure(Exception(SupabaseConfig.extractErrorMessage(bodyStr, resp.code)))
        }
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Network error"))
    }
  }

  suspend fun listPolls(classId: String, accessToken: String): Result<List<PollItem>> {
    val payload = JSONObject().apply { put("target_class_id", classId) }
    val result = postRpc("list_polls", payload, accessToken)
    return result.mapCatching { body ->
      val arr = JSONArray(body)
      val list = mutableListOf<PollItem>()
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val id = obj.optString("id").ifEmpty { obj.optString("poll_id") }
        val question = obj.optString("question").ifEmpty { obj.optString("poll_question") }
        val totalVotes = obj.optInt("total_votes", obj.optInt("votes", 0))
        val hasVoted = obj.optBoolean("has_voted", false)
        val active = obj.optBoolean("active", obj.optBoolean("is_active", true))
        val expiry = obj.optString("expires_at").ifEmpty {
          obj.optString("poll_expiry").ifEmpty { obj.optString("expiry", "") }
        }.takeIf { it.isNotBlank() }
        val created = obj.optString("created_at").takeIf { it.isNotBlank() }
        list.add(
          PollItem(
            id = id,
            question = question,
            totalVotes = totalVotes,
            hasVoted = hasVoted,
            active = active,
            expiresAt = expiry,
            createdAt = created
          )
        )
      }
      list
    }
  }

  suspend fun getPollResults(pollId: String, accessToken: String): Result<PollResultsData> {
    val payload = JSONObject().apply { put("target_poll_id", pollId) }
    val result = postRpc("get_poll_results", payload, accessToken)
    return result.mapCatching { body ->
      val optionsList = mutableListOf<PollResultOption>()
      var totalVotes = 0

      val trimmed = body.trim()
      if (trimmed.startsWith("[")) {
        val arr = JSONArray(trimmed)
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          val optId = obj.optString("id").ifEmpty { obj.optString("option_id") }
          val text = obj.optString("option_text").ifEmpty { obj.optString("text") }
          val votes = obj.optInt("votes", obj.optInt("vote_count", 0))
          val pct = obj.optDouble("percentage", obj.optDouble("pct", -1.0))
          val isMy = obj.optBoolean("is_my_vote", obj.optBoolean("is_user_vote", obj.optBoolean("my_vote", false)))
          optionsList.add(PollResultOption(optId, text, votes, pct, isMy))
          totalVotes += votes
        }
      } else if (trimmed.startsWith("{")) {
        val root = JSONObject(trimmed)
        totalVotes = root.optInt("total_votes", 0)
        val arr = root.optJSONArray("results") ?: root.optJSONArray("options") ?: JSONArray()
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          val optId = obj.optString("id").ifEmpty { obj.optString("option_id") }
          val text = obj.optString("option_text").ifEmpty { obj.optString("text") }
          val votes = obj.optInt("votes", obj.optInt("vote_count", 0))
          val pct = obj.optDouble("percentage", obj.optDouble("pct", -1.0))
          val isMy = obj.optBoolean("is_my_vote", obj.optBoolean("is_user_vote", obj.optBoolean("my_vote", false)))
          optionsList.add(PollResultOption(optId, text, votes, pct, isMy))
          if (root.optInt("total_votes", -1) == -1) totalVotes += votes
        }
      }

      val calculatedOptions = optionsList.map { opt ->
        val finalPct = if (opt.percentage >= 0) opt.percentage
        else if (totalVotes > 0) (opt.votes.toDouble() / totalVotes.toDouble()) * 100.0
        else 0.0
        opt.copy(percentage = finalPct)
      }
      PollResultsData(totalVotes = totalVotes, options = calculatedOptions)
    }
  }

  suspend fun fetchPollOptions(pollId: String, accessToken: String): Result<List<PollOption>> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    val url = "${SupabaseConfig.url}/rest/v1/poll_options?poll_id=eq.$pollId&select=id,option_text&order=position.asc"
    val req = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Accept", "application/json")
      .get()
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
        }
        val arr = JSONArray(body)
        val list = mutableListOf<PollOption>()
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          list.add(
            PollOption(
              id = obj.optString("id"),
              optionText = obj.optString("option_text").ifEmpty { obj.optString("text") },
              position = obj.optInt("position", i)
            )
          )
        }
        Result.success(list)
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to load poll options"))
    }
  }

  suspend fun castVote(pollId: String, optionId: String, accessToken: String): Result<Unit> {
    val payload = JSONObject().apply {
      put("target_poll_id", pollId)
      put("target_option_id", optionId)
    }
    return postRpc("cast_vote", payload, accessToken).map { }
  }

  suspend fun createPoll(
    classId: String,
    question: String,
    options: List<String>,
    expiryIso: String,
    accessToken: String
  ): Result<Unit> {
    val payload = JSONObject().apply {
      put("target_class_id", classId)
      put("poll_question", question.trim())
      val arr = JSONArray()
      options.forEach { arr.put(it.trim()) }
      put("poll_options", arr)
      put("poll_expiry", expiryIso)
    }
    return postRpc("create_poll", payload, accessToken).map { }
  }

  suspend fun closePoll(pollId: String, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    val payload = JSONObject().apply { put("active", false) }
    val url = "${SupabaseConfig.url}/rest/v1/polls?id=eq.$pollId"
    val req = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .patch(payload.toString().toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (resp.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to close poll"))
    }
  }

  suspend fun deletePoll(pollId: String, accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    val url = "${SupabaseConfig.url}/rest/v1/polls?id=eq.$pollId"
    val req = Request.Builder()
      .url(url)
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .delete()
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (resp.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to delete poll"))
    }
  }
}
