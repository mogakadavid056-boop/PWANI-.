package com.example.assistant

import android.content.Context
import com.example.data.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AssistantStorage {
  private const val PREFS_NAME = "classrep_assistant_prefs"
  private const val KEY_MESSAGES_PREFIX = "messages_"
  private const val KEY_CONSENT_PREFIX = "consent_"
  const val MAX_LOCAL_MESSAGES = 30
  const val MAX_SENT_MESSAGES = 10

  fun hasLocalConsent(context: Context, userId: String): Boolean {
    if (userId.isBlank()) return false
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean("$KEY_CONSENT_PREFIX$userId", false)
  }

  fun setLocalConsent(context: Context, userId: String, accepted: Boolean) {
    if (userId.isBlank()) return
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean("$KEY_CONSENT_PREFIX$userId", accepted)
      .apply()
  }

  fun loadMessages(context: Context, userId: String): List<AssistantMessage> {
    if (userId.isBlank()) return emptyList()
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString("$KEY_MESSAGES_PREFIX$userId", null)
      .orEmpty()
    return deserializeMessages(raw).takeLast(MAX_LOCAL_MESSAGES)
  }

  fun saveMessages(context: Context, userId: String, messages: List<AssistantMessage>) {
    if (userId.isBlank()) return
    val capped = messages.takeLast(MAX_LOCAL_MESSAGES)
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString("$KEY_MESSAGES_PREFIX$userId", serializeMessages(capped))
      .apply()
  }

  fun clearMessages(context: Context, userId: String) {
    if (userId.isBlank()) return
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .remove("$KEY_MESSAGES_PREFIX$userId")
      .apply()
  }

  fun clearOnLogout(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .clear()
      .apply()
  }

  fun serializeMessages(messages: List<AssistantMessage>): String {
    val arr = JSONArray()
    messages.takeLast(MAX_LOCAL_MESSAGES).forEach { msg ->
      arr.put(
        JSONObject().apply {
          put("id", msg.id)
          put("role", msg.role.apiValue)
          put("content", msg.content)
          put("timestampMs", msg.timestampMs)
          put("actions", serializeActions(msg.actions))
        }
      )
    }
    return arr.toString()
  }

  fun deserializeMessages(raw: String): List<AssistantMessage> {
    if (raw.isBlank()) return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val role = if (obj.optString("role") == AssistantRole.USER.apiValue) {
            AssistantRole.USER
          } else {
            AssistantRole.ASSISTANT
          }
          add(
            AssistantMessage(
              id = obj.optString("id").ifBlank { "msg_$i" },
              role = role,
              content = obj.optString("content"),
              actions = parseActions(obj.optJSONArray("actions")),
              timestampMs = obj.optLong("timestampMs", 0L)
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun serializeActions(actions: List<AssistantAction>): JSONArray {
    val arr = JSONArray()
    actions.forEach { act ->
      arr.put(
        JSONObject().apply {
          put("id", act.id)
          put("tool", act.tool)
          put("summary", act.summary)
          put("status", act.status.name)
          put("resultMessage", act.resultMessage ?: JSONObject.NULL)
          put("args", mapToJsonObject(act.args))
        }
      )
    }
    return arr
  }

  private fun parseActions(arr: JSONArray?): List<AssistantAction> {
    if (arr == null) return emptyList()
    return buildList {
      for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        val tool = obj.optString("tool").trim()
        if (tool.isBlank()) continue
        val statusName = obj.optString("status")
        val status = ActionCardStatus.entries.firstOrNull { it.name == statusName }
          ?: ActionCardStatus.PENDING
        add(
          AssistantAction(
            id = obj.optString("id").ifBlank { "act_$i" },
            tool = tool,
            args = jsonObjectToMap(obj.optJSONObject("args")),
            summary = obj.optString("summary"),
            status = status,
            resultMessage = obj.optString("resultMessage").takeIf { it.isNotBlank() && it != "null" }
          )
        )
      }
    }
  }

  fun jsonObjectToMap(obj: JSONObject?): Map<String, Any?> {
    if (obj == null) return emptyMap()
    val map = linkedMapOf<String, Any?>()
    val keys = obj.keys()
    while (keys.hasNext()) {
      val key = keys.next()
      map[key] = fromJsonValue(obj.opt(key))
    }
    return map
  }

  private fun fromJsonValue(value: Any?): Any? = when (value) {
    null, JSONObject.NULL -> null
    is JSONObject -> jsonObjectToMap(value)
    is JSONArray -> buildList {
      for (i in 0 until value.length()) add(fromJsonValue(value.opt(i)))
    }
    else -> value
  }

  private fun mapToJsonObject(map: Map<String, Any?>): JSONObject {
    val obj = JSONObject()
    map.forEach { (k, v) -> obj.put(k, toJsonValue(v)) }
    return obj
  }

  private fun toJsonValue(value: Any?): Any = when (value) {
    null -> JSONObject.NULL
    is Map<*, *> -> JSONObject().apply {
      value.forEach { (k, v) -> if (k != null) put(k.toString(), toJsonValue(v)) }
    }
    is Iterable<*> -> JSONArray().apply {
      value.forEach { put(toJsonValue(it)) }
    }
    else -> value
  }
}

class AssistantApi(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(45, TimeUnit.SECONDS)
    .readTimeout(45, TimeUnit.SECONDS)
    .writeTimeout(45, TimeUnit.SECONDS)
    .callTimeout(60, TimeUnit.SECONDS)
    .build()
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun hasAiConsent(accessToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    if (accessToken.isBlank()) {
      return@withContext Result.failure(IllegalStateException("Please sign in first."))
    }
    val req = Request.Builder()
      .url("${SupabaseConfig.url}/rest/v1/rpc/has_ai_consent")
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post("{}".toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty().trim()
        if (!resp.isSuccessful) {
          return@withContext Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
        }
        Result.success(parseConsentResponse(body))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to verify consent status.")))
    }
  }

  suspend fun acceptAiTerms(accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    if (accessToken.isBlank()) {
      return@withContext Result.failure(IllegalStateException("Please sign in first."))
    }
    val req = Request.Builder()
      .url("${SupabaseConfig.url}/rest/v1/rpc/accept_ai_terms")
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post("{}".toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty().trim()
        if (resp.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(SupabaseConfig.extractErrorMessage(body, resp.code)))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Failed to save AI terms acceptance.")))
    }
  }

  suspend fun sendChat(
    messages: List<AssistantMessage>,
    context: AssistantContext,
    accessToken: String
  ): Result<AssistantChatResponse> = withContext(Dispatchers.IO) {
    if (!SupabaseConfig.isConfigured()) {
      return@withContext Result.failure(IllegalStateException(SupabaseConfig.MISSING_CONFIG_ERROR))
    }
    if (accessToken.isBlank()) {
      return@withContext Result.failure(IllegalStateException("Please sign in first."))
    }
    val payload = buildChatRequestBody(messages, context)
    val req = Request.Builder()
      .url("${SupabaseConfig.url}/functions/v1/assistant")
      .addHeader("apikey", SupabaseConfig.anonKey)
      .addHeader("Authorization", "Bearer $accessToken")
      .addHeader("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(payload.toString().toRequestBody(jsonMediaType))
      .build()
    try {
      client.newCall(req).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          val msg = SupabaseConfig.extractErrorMessage(body, resp.code)
          return@withContext Result.failure(Exception(msg))
        }
        Result.success(parseChatResponse(body))
      }
    } catch (e: Exception) {
      Result.failure(Exception(SupabaseConfig.formatExceptionMessage(e, "Assistant request failed. Please try again.")))
    }
  }

  companion object {
    fun parseConsentResponse(raw: String): Boolean {
      val clean = raw.trim()
      if (clean.equals("true", ignoreCase = true)) return true
      if (clean.equals("false", ignoreCase = true)) return false
      return runCatching {
        if (clean.startsWith("{")) {
          val obj = JSONObject(clean)
          obj.optBoolean("has_ai_consent", obj.optBoolean("accepted", obj.optBoolean("consent", false)))
        } else {
          false
        }
      }.getOrDefault(false)
    }

    fun buildChatRequestBody(
      messages: List<AssistantMessage>,
      context: AssistantContext
    ): JSONObject {
      val msgArr = JSONArray()
      messages
        .filter { it.content.isNotBlank() }
        .takeLast(AssistantStorage.MAX_SENT_MESSAGES)
        .forEach { m ->
          msgArr.put(
            JSONObject().apply {
              put("role", m.role.apiValue)
              put("text", m.content.take(2000))
            }
          )
        }
      val ctxObj = JSONObject().apply {
        put("app", context.app)
        put("screen", context.screen)
        put("class_id", if (context.classId.isNullOrBlank()) JSONObject.NULL else context.classId)
      }
      return JSONObject().apply {
        put("messages", msgArr)
        put("context", ctxObj)
      }
    }

    fun parseChatResponse(rawBody: String): AssistantChatResponse {
      val trimmed = rawBody.trim()
      if (trimmed.isEmpty()) {
        throw IllegalStateException("Assistant returned an empty response. Check Supabase 'assistant' Edge Function logs.")
      }

      if (trimmed.startsWith("data:")) {
        return parseSseResponse(trimmed)
      }

      if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
        return AssistantChatResponse(reply = trimmed, actions = emptyList())
      }

      val root: JSONObject = if (trimmed.startsWith("[")) {
        val arr = JSONArray(trimmed)
        var firstObj: JSONObject? = null
        val textParts = mutableListOf<String>()
        for (i in 0 until arr.length()) {
          val item = arr.opt(i)
          if (item is JSONObject && firstObj == null) {
            firstObj = item
          } else if (item is String && item.isNotBlank()) {
            textParts.add(item.trim())
          }
        }
        firstObj ?: if (textParts.isNotEmpty()) {
          return AssistantChatResponse(reply = textParts.joinToString("\n"), actions = emptyList())
        } else {
          throw IllegalStateException("Assistant returned an empty response array.")
        }
      } else {
        JSONObject(trimmed)
      }

      val containers = buildList {
        root.optJSONObject("data")?.let { add(it) }
        root.optJSONObject("result")?.let { add(it) }
        root.optJSONObject("response")?.let { add(it) }
        add(root)
      }

      var sawRawActions = false
      val actions = mutableListOf<AssistantAction>()
      for (obj in containers) {
        val collected = extractActionsFromObject(obj)
        if (collected.sawAnyRawActions) sawRawActions = true
        if (collected.actions.isNotEmpty()) {
          actions.addAll(collected.actions)
          break
        }
      }

      val embeddedError = extractEmbeddedError(containers)
      if (embeddedError.isNotBlank()) {
        throw IllegalStateException(embeddedError)
      }

      val reply = extractReplyText(containers)
      if (reply.isNotBlank()) {
        if (actions.isEmpty() && isStarterTemplateReply(reply)) {
          throw IllegalStateException(
            "Your Supabase 'assistant' Edge Function is still running the default starter template (\"$reply\"). " +
              "In Supabase > Edge Functions > assistant > Code, replace all code with assistant-edge-function.ts and click Deploy."
          )
        }
        return AssistantChatResponse(reply = reply, actions = actions)
      }

      if (actions.isNotEmpty()) {
        return AssistantChatResponse(
          reply = "Here is a suggested action you can review and confirm:",
          actions = actions
        )
      }

      if (sawRawActions) {
        throw IllegalStateException("Assistant proposed an unsupported action without a text reply. Please try asking again.")
      }

      val keys = buildList {
        val it = root.keys()
        while (it.hasNext()) add(it.next())
      }.joinToString(", ").ifBlank { "none" }
      throw IllegalStateException(
        "Assistant returned no reply text (fields: $keys). Check Supabase 'assistant' Edge Function logs and GEMINI_API_KEY / GEMINI_MODEL secrets."
      )
    }

    private fun isStarterTemplateReply(reply: String): Boolean {
      val clean = reply.trim()
      return clean.equals("Hello undefined!", ignoreCase = true) ||
        clean.equals("Hello null!", ignoreCase = true) ||
        clean.equals("Hello undefined", ignoreCase = true) ||
        clean.equals("Hello null", ignoreCase = true)
    }

    private data class ExtractedActionsResult(
      val actions: List<AssistantAction>,
      val sawAnyRawActions: Boolean
    )

    private fun parseSseResponse(rawSse: String): AssistantChatResponse {
      val textBuilder = StringBuilder()
      val actions = mutableListOf<AssistantAction>()
      rawSse.lines().forEach { line ->
        val t = line.trim()
        if (!t.startsWith("data:")) return@forEach
        val payload = t.removePrefix("data:").trim()
        if (payload.isBlank() || payload == "[DONE]") return@forEach
        if (payload.startsWith("{")) {
          runCatching { JSONObject(payload) }.getOrNull()?.let { obj ->
            val chunkText = extractReplyText(listOf(obj))
            if (chunkText.isNotBlank()) textBuilder.append(chunkText)
            val collected = extractActionsFromObject(obj)
            if (collected.actions.isNotEmpty()) actions.addAll(collected.actions)
          }
        } else {
          textBuilder.append(payload)
        }
      }
      val reply = textBuilder.toString().trim()
      if (reply.isBlank() && actions.isEmpty()) {
        throw IllegalStateException("Assistant stream returned no reply text.")
      }
      return AssistantChatResponse(
        reply = reply.ifBlank { "Here is a suggested action you can review and confirm:" },
        actions = actions
      )
    }

    private fun optNonNullString(obj: JSONObject?, key: String): String {
      if (obj == null || !obj.has(key) || obj.isNull(key)) return ""
      val raw = obj.opt(key) ?: return ""
      if (raw is JSONObject || raw is JSONArray) return ""
      return raw.toString().trim()
        .takeUnless { it.equals("null", ignoreCase = true) || it.equals("undefined", ignoreCase = true) }
        .orEmpty()
    }

    private fun extractReplyText(containers: List<JSONObject>): String {
      val directKeys = listOf(
        "reply", "answer", "text", "response", "output",
        "content", "assistant_reply", "assistantReply",
        "assistant_message", "assistantMessage", "message", "result"
      )
      for (obj in containers) {
        for (key in directKeys) {
          val v = optNonNullString(obj, key)
          if (v.isNotBlank()) return v
        }
        obj.optJSONObject("message")?.let { msgObj ->
          val nested = optNonNullString(msgObj, "content")
            .ifBlank { optNonNullString(msgObj, "text") }
            .ifBlank { optNonNullString(msgObj, "reply") }
          if (nested.isNotBlank()) return nested
        }
        val candidates = obj.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
          val cand = candidates.optJSONObject(0)
          val parts = cand?.optJSONObject("content")?.optJSONArray("parts")
          if (parts != null) {
            val nonThought = mutableListOf<String>()
            val allText = mutableListOf<String>()
            for (i in 0 until parts.length()) {
              val p = parts.optJSONObject(i) ?: continue
              val t = optNonNullString(p, "text")
              if (t.isNotBlank()) {
                allText.add(t)
                if (!p.optBoolean("thought", false)) nonThought.add(t)
              }
            }
            val joined = (if (nonThought.isNotEmpty()) nonThought else allText).joinToString("\n").trim()
            if (joined.isNotBlank()) return joined
          }
        }
        val choices = obj.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
          val ch = choices.optJSONObject(0)
          val choiceText = optNonNullString(ch?.optJSONObject("message"), "content")
            .ifBlank { optNonNullString(ch?.optJSONObject("delta"), "content") }
            .ifBlank { optNonNullString(ch, "text") }
          if (choiceText.isNotBlank()) return choiceText
        }
      }
      return ""
    }

    private fun extractEmbeddedError(containers: List<JSONObject>): String {
      val errKeys = listOf("error", "error_description", "err", "detail", "details", "msg", "reason")
      for (obj in containers) {
        for (k in errKeys) {
          val s = optNonNullString(obj, k)
          if (s.isNotBlank()) return s
          obj.optJSONObject(k)?.let { errObj ->
            val nested = optNonNullString(errObj, "message")
              .ifBlank { optNonNullString(errObj, "error") }
              .ifBlank { optNonNullString(errObj, "details") }
            if (nested.isNotBlank()) return nested
          }
        }
        if (obj.has("ok") && !obj.optBoolean("ok", true)) {
          val msg = optNonNullString(obj, "message").ifBlank { optNonNullString(obj, "status") }
          if (msg.isNotBlank()) return msg
        }
      }
      return ""
    }

    private fun extractActionsFromObject(obj: JSONObject): ExtractedActionsResult {
      val arrayKeys = listOf("actions", "proposed_actions", "proposals", "tool_calls")
      val rawItems = mutableListOf<JSONObject>()
      for (k in arrayKeys) {
        val arr = obj.optJSONArray(k) ?: continue
        for (i in 0 until arr.length()) {
          arr.optJSONObject(i)?.let { rawItems.add(it) }
        }
      }
      obj.optJSONObject("action")?.let { rawItems.add(it) }
      obj.optJSONObject("proposal")?.let { rawItems.add(it) }

      val sawAny = rawItems.isNotEmpty()
      val parsed = buildList {
        rawItems.forEachIndexed { idx, actObj ->
          val fnObj = actObj.optJSONObject("function")
          val tool = optNonNullString(actObj, "tool")
            .ifBlank { optNonNullString(actObj, "name") }
            .ifBlank { optNonNullString(actObj, "type") }
            .ifBlank { optNonNullString(actObj, "action") }
            .ifBlank { optNonNullString(fnObj, "name") }
          if (tool.isBlank() || !ActionExecutor.isSupportedMainTool(tool)) return@forEachIndexed

          val argsObj = actObj.optJSONObject("args")
            ?: actObj.optJSONObject("arguments")
            ?: actObj.optJSONObject("params")
            ?: actObj.optJSONObject("parameters")
            ?: actObj.optJSONObject("input")
            ?: actObj.optJSONObject("payload")
            ?: fnObj?.optJSONObject("arguments")
            ?: listOf("args", "arguments", "params", "parameters", "input")
              .firstNotNullOfOrNull { key ->
                optNonNullString(actObj, key).takeIf { it.startsWith("{") }
                  ?.let { runCatching { JSONObject(it) }.getOrNull() }
              }
            ?: optNonNullString(fnObj, "arguments").takeIf { it.startsWith("{") }
              ?.let { runCatching { JSONObject(it) }.getOrNull() }

          val summary = optNonNullString(actObj, "summary")
            .ifBlank { optNonNullString(actObj, "description") }
            .ifBlank { optNonNullString(actObj, "label") }
            .ifBlank { optNonNullString(actObj, "title") }
            .ifBlank { tool }

          add(
            AssistantAction(
              id = optNonNullString(actObj, "id").ifBlank { "action_$idx" },
              tool = tool,
              args = AssistantStorage.jsonObjectToMap(argsObj),
              summary = summary
            )
          )
        }
      }
      return ExtractedActionsResult(actions = parsed, sawAnyRawActions = sawAny)
    }
  }
}
