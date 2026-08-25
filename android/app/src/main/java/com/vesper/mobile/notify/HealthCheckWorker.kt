package com.vesper.mobile.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vesper.mobile.VesperApplication
import java.util.concurrent.TimeUnit

class HealthCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? VesperApplication ?: return Result.success()
        val settings = app.container.settings.snapshot()
        if (!settings.healthPoll || !settings.notifySystem) return Result.success()
        val health = app.container.mortis.health()
        if (!health.reachable && settings.notifySystem) {
            app.container.notifications.notify(
                NotificationHelper.Channel.SYSTEM,
                ID_HEALTH,
                "MORTIS UNAVAILABLE",
                health.detail,
            )
        }
        return Result.success()
    }

    companion object {
        private const val UNIQUE = "vesper-health-poll"
        private const val ID_HEALTH = 7101

        fun reconcile(context: Context, enabled: Boolean) {
            val wm = WorkManager.getInstance(context.applicationContext)
            if (!enabled) {
                wm.cancelUniqueWork(UNIQUE)
                return
            }
            val req = PeriodicWorkRequestBuilder<HealthCheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            wm.enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.UPDATE, req)
        }
    }
}
