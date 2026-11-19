package com.example.notifications

import com.example.data.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ClassRepFirebaseMessagingService : FirebaseMessagingService() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    val appCtx = applicationContext
    NotificationPreferences.saveFcmToken(appCtx, token)
    serviceScope.launch {
      val session = SessionManager(appCtx).getSession()
      if (session != null && session.accessToken.isNotBlank()) {
        PushTokenManager.registerToken(appCtx, session.accessToken, token)
      }
    }
  }

  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)
    val data = message.data
    val title = data["title"]?.takeIf { it.isNotBlank() }
      ?: message.notification?.title?.takeIf { it.isNotBlank() }
      ?: "ClassRep"
    val body = data["body"]?.takeIf { it.isNotBlank() }
      ?: message.notification?.body
      ?: ""

    AppNotificationDispatcher.dispatchRemoteMessage(
      context = applicationContext,
      title = title,
      body = body,
      data = data,
      explicitChannelId = message.notification?.channelId
    )
  }

  override fun onDestroy() {
    serviceScope.cancel()
    super.onDestroy()
  }
}
