package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.NetworkMonitor
import com.example.data.SessionManager
import java.util.concurrent.TimeUnit

class NotificationSyncWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    if (!NetworkMonitor.isOnline.value) return Result.success()
    val session = SessionManager(applicationContext).getSession() ?: return Result.success()
    if (session.accessToken.isBlank()) return Result.success()

    NotificationService().fetchNotifications(session.accessToken, limit = 25, offset = 0)
      .onSuccess { list ->
        AppNotificationDispatcher.dispatchUnseen(applicationContext, list)
      }
    return Result.success()
  }

  companion object {
    private const val WORK_NAME = "classrep_notification_sync"

    fun schedule(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
      val req = PeriodicWorkRequestBuilder<NotificationSyncWorker>(15, TimeUnit.MINUTES)
        .setConstraints(constraints)
        .build()
      WorkManager.getInstance(context.applicationContext)
        .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun cancel(context: Context) {
      WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }
  }
}
