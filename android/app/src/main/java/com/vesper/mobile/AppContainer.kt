package com.vesper.mobile

import android.content.Context
import com.vesper.mobile.data.mortis.MortisApi
import com.vesper.mobile.data.mortis.MortisRepository
import com.vesper.mobile.data.settings.SettingsStore
import com.vesper.mobile.data.vesper.ChatStore
import com.vesper.mobile.data.vesper.VesperEnvironment
import com.vesper.mobile.notify.ConnectivityMonitor
import com.vesper.mobile.notify.NotificationHelper
import com.vesper.mobile.security.SessionStore
import kotlinx.serialization.json.Json

class AppContainer(
    val settings: SettingsStore,
    val session: SessionStore,
    val connectivity: ConnectivityMonitor,
    val notifications: NotificationHelper,
    val mortisApi: MortisApi,
    val mortis: MortisRepository,
    val chat: ChatStore,
    val vesper: VesperEnvironment,
    val bootstrapError: String? = null,
) {
    companion object {
        val json: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
            coerceInputValues = true
        }

        fun create(context: Context, bootstrapError: String? = null): AppContainer {
            val app = context.applicationContext
            val settings = SettingsStore(app)
            val session = SessionStore(app)
            val connectivity = ConnectivityMonitor(app)
            val notifications = NotificationHelper(app)
            val mortisApi = MortisApi(json)
            return AppContainer(
                settings = settings,
                session = session,
                connectivity = connectivity,
                notifications = notifications,
                mortisApi = mortisApi,
                mortis = MortisRepository(mortisApi, settings, session, json),
                chat = ChatStore(app, json),
                vesper = VesperEnvironment(settings, json),
                bootstrapError = bootstrapError,
            )
        }

        fun degraded(context: Context, error: Throwable): AppContainer {
            return runCatching { create(context, error.toString()) }.getOrElse { second ->
                create(context, listOf(error.toString(), second.toString()).joinToString(" | "))
            }
        }
    }
}
