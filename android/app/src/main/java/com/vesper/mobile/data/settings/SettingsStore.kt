package com.vesper.mobile.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.vesperSettings: DataStore<Preferences> by preferencesDataStore(name = "vesper_settings")

data class VesperSettings(
    val mortisHost: String = SettingsStore.DEFAULT_HOST,
    val adminPathSeg: String = "",
    val operatorId: String = "",
    val vesperEndpoint: String = "",
    val notifyIntake: Boolean = true,
    val notifyReview: Boolean = true,
    val notifyRelease: Boolean = true,
    val notifySigning: Boolean = true,
    val notifySystem: Boolean = true,
    val healthPoll: Boolean = false,
    val presenceEnabled: Boolean = false,
)

class SettingsStore(private val context: Context) {

    val flow: Flow<VesperSettings> = context.vesperSettings.data.map { it.toModel() }

    suspend fun snapshot(): VesperSettings = flow.first()

    suspend fun update(transform: (VesperSettings) -> VesperSettings) {
        val current = snapshot()
        val next = transform(current)
        context.vesperSettings.edit { p ->
            p[HOST] = next.mortisHost.trim().trimEnd('/')
            p[ADMIN] = next.adminPathSeg.trim().trim('/')
            p[OPERATOR] = next.operatorId.trim()
            p[VESPER] = next.vesperEndpoint.trim().trimEnd('/')
            p[N_INTAKE] = next.notifyIntake
            p[N_REVIEW] = next.notifyReview
            p[N_RELEASE] = next.notifyRelease
            p[N_SIGNING] = next.notifySigning
            p[N_SYSTEM] = next.notifySystem
            p[HEALTH] = next.healthPoll
            p[PRESENCE] = next.presenceEnabled
        }
    }

    private fun Preferences.toModel(): VesperSettings = VesperSettings(
        mortisHost = this[HOST]?.ifBlank { null } ?: DEFAULT_HOST,
        adminPathSeg = this[ADMIN].orEmpty(),
        operatorId = this[OPERATOR].orEmpty(),
        vesperEndpoint = this[VESPER].orEmpty(),
        notifyIntake = this[N_INTAKE] ?: true,
        notifyReview = this[N_REVIEW] ?: true,
        notifyRelease = this[N_RELEASE] ?: true,
        notifySigning = this[N_SIGNING] ?: true,
        notifySystem = this[N_SYSTEM] ?: true,
        healthPoll = this[HEALTH] ?: false,
        presenceEnabled = this[PRESENCE] ?: false,
    )

    companion object {
        const val DEFAULT_HOST = "https://mortis-relay.mmg-wolfpoolyt.workers.dev"
        private val HOST = stringPreferencesKey("mortis_host")
        private val ADMIN = stringPreferencesKey("admin_path_seg")
        private val OPERATOR = stringPreferencesKey("operator_id")
        private val VESPER = stringPreferencesKey("vesper_endpoint")
        private val N_INTAKE = booleanPreferencesKey("notify_intake")
        private val N_REVIEW = booleanPreferencesKey("notify_review")
        private val N_RELEASE = booleanPreferencesKey("notify_release")
        private val N_SIGNING = booleanPreferencesKey("notify_signing")
        private val N_SYSTEM = booleanPreferencesKey("notify_system")
        private val HEALTH = booleanPreferencesKey("health_poll")
        private val PRESENCE = booleanPreferencesKey("presence_enabled")
    }
}
