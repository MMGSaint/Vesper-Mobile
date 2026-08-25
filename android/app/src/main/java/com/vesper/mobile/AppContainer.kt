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

class AppContainer(context: Context) {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }

    val settings = SettingsStore(context)
    val session = SessionStore(context)
    val connectivity = ConnectivityMonitor(context)
    val notifications = NotificationHelper(context)
    val mortisApi = MortisApi(json)
    val mortis = MortisRepository(mortisApi, settings, session, json)
    val chat = ChatStore(context, json)
    val vesper = VesperEnvironment(settings, json)
}
