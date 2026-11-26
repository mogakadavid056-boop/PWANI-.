package com.example.assistant

import com.example.BuildConfig
import com.example.data.SupabaseConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiConfig {
  const val DEFAULT_API_KEY_PLACEHOLDER = "GEMINI_API_KEY_DEFAULT_VALUE"
  const val MISSING_API_KEY_MESSAGE =
    "Gemini API key not configured. Please add GEMINI_API_KEY in the AI Studio Secrets panel."
  const val SYSTEM_INSTRUCTION =
    "You are Gemini, a helpful, clear, and general-purpose AI assistant for students and lecturers. Answer questions accurately, explain concepts clearly, help draft academic or class materials, and assist naturally with any request."

  val SUPPORTED_MODELS: List<String> = listOf(
    "gemini-3.5-flash",
    "gemini-3.8-flash",
    "gemini-2.5-flash"
  )

  fun sanitizeApiKey(raw: String?): String {
    if (raw == null) return ""
    val cleaned = raw.trim()
      .removeSurrounding("\"")
      .removeSurrounding("'")
      .trim()
    if (cleaned.isEmpty() || cleaned == DEFAULT_API_KEY_PLACEHOLDER) return ""
    return cleaned
  }

  val apiKey: String
    get() = try {
      sanitizeApiKey(BuildConfig.GEMINI_API_KEY)
    } catch (_: Throwable) {
      ""
    }

  fun isConfigured(): Boolean = apiKey.isNotEmpty()
}

object GeminiChatService {
  private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
  private const val MAX_CONTEXT_MESSAGES = 20

  private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

  fun streamUrl(model: String): String =
    "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse"

  fun buildRequestBody(
    history: List<GeminiMessage>,
    userMessage: String
  ): JSONObject {
    val contents = JSONArray()
    val recent = history
      .filter { !it.isStreaming && it.text.isNotBlank() }
      .takeLast(MAX_CONTEXT_MESSAGES)

    for (msg in recent) {
      contents.put(
        JSONObject().apply {
          put("role", if (msg.role == ChatRole.USER) "user" else "model")
          put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
        }
      )
    }
    contents.put(
      JSONObject().apply {
        put("role", "user")
        put("parts", JSONArray().put(JSONObject().put("text", userMessage.trim())))
      }
    )

    return JSONObject().apply {
      put(
        "systemInstruction",
        JSONObject().put(
          "parts",
          JSONArray().put(JSONObject().put("text", GeminiConfig.SYSTEM_INSTRUCTION))
        )
      )
      put("contents", contents)
      put("generationConfig", JSONObject().put("temperature", 0.7))
    }
  }

  fun extractChunkText(rawJsonLine: String): String {
    if (rawJsonLine.isBlank()) return ""
    return try {
      val root = JSONObject(rawJsonLine)
      val candidates = root.optJSONArray("candidates") ?: return ""
      val first = candidates.optJSONObject(0) ?: return ""
      val content = first.optJSONObject("content") ?: return ""
      val parts = content.optJSONArray("parts") ?: return ""
      buildString {
        for (i in 0 until parts.length()) {
          val part = parts.optJSONObject(i) ?: continue
          if (part.optBoolean("thought", false)) continue
          val text = part.optString("text", "")
          if (text.isNotEmpty()) append(text)
        }
      }
    } catch (_: Throwable) {
      ""
    }
  }

  fun extractBlockOrFinishNotice(rawJsonLine: String): String? {
    return try {
      val root = JSONObject(rawJsonLine)
      val promptFeedback = root.optJSONObject("promptFeedback")
      val blockReason = promptFeedback?.optString("blockReason").orEmpty()
      if (blockReason.isNotBlank()) {
        return "Response was blocked ($blockReason). Try rephrasing your prompt."
      }
      null
    } catch (_: Throwable) {
      null
    }
  }

  fun extractApiErrorMessage(responseBody: String, statusCode: Int): String {
    val apiMessage = try {
      val json = JSONObject(responseBody)
      val errorObj = json.optJSONObject("error")
      errorObj?.optString("message")?.takeIf { it.isNotBlank() }
        ?: json.optString("message").takeIf { it.isNotBlank() }
        ?: ""
    } catch (_: Throwable) {
      ""
    }

    return when (statusCode) {
      400 -> if (apiMessage.contains("API key", ignoreCase = true)) {
        "Invalid GEMINI_API_KEY. Please check the key in the AI Studio Secrets panel."
      } else {
        apiMessage.ifBlank { "Gemini could not process this request." }
      }
      401, 403 -> "Invalid or unauthorized GEMINI_API_KEY. Update it in the AI Studio Secrets panel."
      404 -> apiMessage.ifBlank { "Selected Gemini model was not found." }
      429 -> "Gemini rate limit reached. Please wait a moment and try again."
      in 500..599 -> "Gemini service is temporarily unavailable. Please try again."
      else -> apiMessage.ifBlank { "Gemini request failed (HTTP $statusCode)." }
    }
  }

  fun streamReply(
    history: List<GeminiMessage>,
    userMessage: String,
    apiKeyOverride: String? = null
  ): Flow<String> = flow {
    val key = GeminiConfig.sanitizeApiKey(apiKeyOverride).ifEmpty { GeminiConfig.apiKey }
    if (key.isEmpty()) {
      throw IllegalStateException(GeminiConfig.MISSING_API_KEY_MESSAGE)
    }

    val payload = buildRequestBody(history, userMessage).toString()
    val models = GeminiConfig.SUPPORTED_MODELS
    var lastError: Throwable? = null

    for ((index, model) in models.withIndex()) {
      currentCoroutineContext().ensureActive()
      val request = Request.Builder()
        .url(streamUrl(model))
        .addHeader("x-goog-api-key", key)
        .addHeader("Accept", "text/event-stream")
        .post(payload.toRequestBody(JSON_MEDIA_TYPE))
        .build()

      val call = client.newCall(request)
      try {
        val response = call.execute()
        response.use { resp ->
          if (!resp.isSuccessful) {
            val errBody = resp.body?.string().orEmpty()
            val message = extractApiErrorMessage(errBody, resp.code)
            if (resp.code == 404 && index < models.lastIndex) {
              lastError = IOException(message)
              return@use
            }
            throw IOException(message)
          }

          val body = resp.body ?: throw IOException("Empty response from Gemini.")
          val reader = body.charStream().buffered()
          var emittedAnyText = false
          var noticeMessage: String? = null

          while (true) {
            currentCoroutineContext().ensureActive()
            val line = reader.readLine() ?: break
            val trimmed = line.trim()
            if (trimmed.isEmpty() || !trimmed.startsWith("data:")) continue
            val jsonPart = trimmed.removePrefix("data:").trim()
            if (jsonPart.isEmpty() || jsonPart == "[DONE]") continue

            val chunk = extractChunkText(jsonPart)
            if (chunk.isNotEmpty()) {
              emittedAnyText = true
              emit(chunk)
            } else if (noticeMessage == null) {
              noticeMessage = extractBlockOrFinishNotice(jsonPart)
            }
          }

          if (!emittedAnyText) {
            throw IOException(noticeMessage ?: "Gemini returned an empty response. Please try again.")
          }
          return@flow
        }
      } catch (e: CancellationException) {
        call.cancel()
        throw e
      } catch (e: Throwable) {
        if (lastError == null || index == models.lastIndex) {
          lastError = e
        }
        if (SupabaseConfig.isNetworkError(e) || index == models.lastIndex || lastError !is IOException) {
          throw e
        }
      }
    }

    throw (lastError ?: IOException("Unable to reach Gemini right now."))
  }.flowOn(Dispatchers.IO)
}
