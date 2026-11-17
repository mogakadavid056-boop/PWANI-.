package com.example.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ProfileRealtimeService(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.MILLISECONDS)
    .build()
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var webSocket: WebSocket? = null
  private var heartbeatJob: Job? = null
  private var reconnectJob: Job? = null
  private var activeUserId: String = ""
  private var activeAccessToken: String = ""
  private var isSubscribed: Boolean = false
  private var refCounter = 1

  fun subscribe(userId: String, accessToken: String, onProfileChanged: () -> Unit) {
    if (!SupabaseConfig.isConfigured() || userId.isBlank() || accessToken.isBlank()) return
    if (isSubscribed && activeUserId == userId && activeAccessToken == accessToken && webSocket != null) {
      return
    }
    unsubscribe()
    activeUserId = userId
    activeAccessToken = accessToken
    isSubscribed = true
    connect(userId, accessToken, onProfileChanged)
  }

  private fun connect(userId: String, accessToken: String, onProfileChanged: () -> Unit) {
    if (!isSubscribed) return
    val base = SupabaseConfig.url
    val wsBase = when {
      base.startsWith("https://", ignoreCase = true) -> "wss://" + base.removePrefix("https://")
      base.startsWith("http://", ignoreCase = true) -> "ws://" + base.removePrefix("http://")
      else -> "wss://$base"
    }
    val wsUrl = "$wsBase/realtime/v1/websocket?apikey=${SupabaseConfig.anonKey}&vsn=1.0.0"

    val request = Request.Builder()
      .url(wsUrl)
      .addHeader("Authorization", "Bearer $accessToken")
      .build()

    webSocket = client.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        val topic = "realtime:public:profiles:id=eq.$userId"
        val joinPayload = JSONObject().apply {
          put("config", JSONObject().apply {
            put("broadcast", JSONObject().apply { put("self", false) })
            put("presence", JSONObject().apply { put("key", "") })
            put("postgres_changes", JSONArray().apply {
              put(JSONObject().apply {
                put("event", "*")
                put("schema", "public")
                put("table", "profiles")
                put("filter", "id=eq.$userId")
              })
            })
          })
          put("access_token", accessToken)
        }
        val joinMsg = JSONObject().apply {
          put("topic", topic)
          put("event", "phx_join")
          put("payload", joinPayload)
          put("ref", nextRef())
        }
        webSocket.send(joinMsg.toString())
        startHeartbeat(webSocket)
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        try {
          val json = JSONObject(text)
          val event = json.optString("event")
          if (event == "postgres_changes") {
            onProfileChanged()
          }
        } catch (e: Exception) {
          Log.w("ProfileRealtime", "Failed to parse realtime message", e)
        }
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        heartbeatJob?.cancel()
        if (isSubscribed && code != 1000) {
          scheduleReconnect(onProfileChanged)
        }
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        heartbeatJob?.cancel()
        if (isSubscribed) {
          scheduleReconnect(onProfileChanged)
        }
      }
    })
  }

  private fun startHeartbeat(ws: WebSocket) {
    heartbeatJob?.cancel()
    heartbeatJob = scope.launch {
      while (isActive && isSubscribed) {
        delay(25_000L)
        if (!isSubscribed) break
        val hb = JSONObject().apply {
          put("topic", "phoenix")
          put("event", "heartbeat")
          put("payload", JSONObject())
          put("ref", nextRef())
        }
        ws.send(hb.toString())
      }
    }
  }

  private fun scheduleReconnect(onProfileChanged: () -> Unit) {
    reconnectJob?.cancel()
    reconnectJob = scope.launch {
      delay(5_000L)
      if (isSubscribed && activeUserId.isNotBlank() && activeAccessToken.isNotBlank()) {
        connect(activeUserId, activeAccessToken, onProfileChanged)
      }
    }
  }

  private fun nextRef(): String {
    refCounter += 1
    return refCounter.toString()
  }

  fun unsubscribe() {
    isSubscribed = false
    heartbeatJob?.cancel()
    heartbeatJob = null
    reconnectJob?.cancel()
    reconnectJob = null
    try {
      webSocket?.close(1000, "unsubscribe")
    } catch (_: Throwable) {
    }
    webSocket = null
    activeUserId = ""
    activeAccessToken = ""
  }
}
