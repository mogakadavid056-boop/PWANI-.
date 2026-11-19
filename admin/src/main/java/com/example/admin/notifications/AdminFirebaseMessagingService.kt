package com.example.admin.notifications

import com.example.admin.data.AdminSessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AdminFirebaseMessagingService : FirebaseMessagingService() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    val appCtx = applicationContext
    AdminNotificationPrefs.saveFcmToken(appCtx, token)
    serviceScope.launch {
      val session = AdminSessionManager(appCtx).getSession()
      if (session != null && session.accessToken.isNotBlank()) {
        AdminPushTokenManager.registerToken(appCtx, session.accessToken, token)
      }
    }
  }

  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)
    val data = message.data
    val title = data["title"]?.takeIf { it.isNotBlank() }
      ?: message.notification?.title?.takeIf { it.isNotBlank() }
      ?: "ClassRep Admin"
    val body = data["body"]?.takeIf { it.isNotBlank() }
      ?: message.notification?.body
      ?: ""

    AdminNotificationDispatcher.dispatchRemoteMessage(
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
