package com.example.alarms

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.OfflineCache
import com.example.data.SessionManager
import java.util.concurrent.TimeUnit

class SyncWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  companion object {
    private const val WORK_NAME = "class_alarms_periodic_sync"

    fun enqueuePeriodicSync(context: Context) {
      val appCtx = context.applicationContext
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
        .setConstraints(constraints)
        .build()

      runCatching {
        WorkManager.getInstance(appCtx).enqueueUniquePeriodicWork(
          WORK_NAME,
          ExistingPeriodicWorkPolicy.KEEP,
          request
        )
      }
    }

    fun cancelPeriodicSync(context: Context) {
      runCatching {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
      }
    }
  }

  override suspend fun doWork(): Result {
    val appCtx = applicationContext
    OfflineCache.appContext = appCtx
    val session = SessionManager(appCtx).getSession()
    if (session == null || session.userId.isBlank() || session.accessToken.isBlank()) {
      AlarmScheduler.cancelAllAlarms(appCtx)
      return Result.success()
    }
    OfflineCache.currentUserId = session.userId

    val syncResult = TimetableAlarmSync.syncAndReschedule(appCtx, session)
    return if (syncResult.isSuccess) {
      Result.success()
    } else {
      AlarmScheduler.rescheduleAll(appCtx)
      Result.retry()
    }
  }
}
