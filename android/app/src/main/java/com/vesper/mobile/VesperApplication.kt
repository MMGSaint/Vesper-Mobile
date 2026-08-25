package com.vesper.mobile

import android.app.Application
import com.vesper.mobile.notify.HealthCheckWorker
import com.vesper.mobile.notify.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VesperApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifications.ensureChannels()
        scope.launch {
            container.settings.flow.collectLatest { s ->
                HealthCheckWorker.reconcile(this@VesperApplication, s.healthPoll && s.notifySystem)
            }
        }
    }
}
