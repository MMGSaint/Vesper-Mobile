package com.vesper.mobile

import android.app.Application
import com.vesper.mobile.debug.StartupCrashLog
import com.vesper.mobile.notify.HealthCheckWorker
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VesperApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val handler = CoroutineExceptionHandler { _, error ->
        StartupCrashLog.write(this, error, "application scope")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)

    override fun onCreate() {
        super.onCreate()
        StartupCrashLog.install(this)
        container = runCatching { AppContainer.create(this) }.getOrElse { error ->
            StartupCrashLog.write(this, error, "AppContainer constructor")
            AppContainer.degraded(this, error)
        }
        runCatching { container.notifications.ensureChannels() }
            .onFailure { StartupCrashLog.write(this, it, "notification channels") }
        scope.launch {
            runCatching {
                container.settings.flow.collectLatest { s ->
                    runCatching {
                        HealthCheckWorker.reconcile(this@VesperApplication, s.healthPoll && s.notifySystem)
                    }.onFailure { HealthCheckWorker.reconcileFailed(it) }
                }
            }.onFailure { StartupCrashLog.write(this@VesperApplication, it, "settings collector") }
        }
    }
}
