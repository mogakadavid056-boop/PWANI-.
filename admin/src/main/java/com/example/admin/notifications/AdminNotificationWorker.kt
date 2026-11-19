package com.example.admin.notifications

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.admin.data.AdminSessionManager
import java.util.concurrent.TimeUnit

class AdminNotificationWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    if (!isNetworkAvailable(applicationContext)) return Result.success()
    val session = AdminSessionManager(applicationContext).getSession() ?: return Result.success()
    if (session.accessToken.isBlank()) return Result.success()

    AdminNotificationService().fetchNotifications(session.accessToken, limit = 25, offset = 0)
      .onSuccess { list ->
        AdminNotificationDispatcher.dispatchUnseen(applicationContext, list)
      }
    return Result.success()
  }

  private fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  companion object {
    private const val WORK_NAME = "classrep_admin_notification_sync"

    fun schedule(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
      val req = PeriodicWorkRequestBuilder<AdminNotificationWorker>(15, TimeUnit.MINUTES)
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
