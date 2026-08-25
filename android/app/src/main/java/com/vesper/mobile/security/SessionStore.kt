package com.vesper.mobile.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vesper.mobile.domain.OperatorSessionView
import java.util.concurrent.TimeUnit

/**
 * Operator session token only. Passphrase is never written.
 * Idle 15 min / absolute 60 min, enforced client-side in addition to the worker.
 */
class SessionStore(context: Context) {

    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    @Volatile
    private var lastTouchElapsed: Long = 0L

    val idleLimitMs: Long = TimeUnit.MINUTES.toMillis(15)
    val absLimitMs: Long = TimeUnit.MINUTES.toMillis(60)

    val token: String?
        get() = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    val operatorId: String?
        get() = prefs.getString(KEY_OPERATOR, null)?.takeIf { it.isNotBlank() }

    val issuedAt: Long
        get() = prefs.getLong(KEY_ISSUED, 0L)

    fun isUnlocked(now: Long = System.currentTimeMillis()): Boolean {
        val t = token ?: return false
        if (t.isBlank()) return false
        if (now - issuedAt >= absLimitMs) {
            clear()
            return false
        }
        val last = prefs.getLong(KEY_LAST, issuedAt)
        if (now - last >= idleLimitMs) {
            clear()
            return false
        }
        return true
    }

    fun remainingIdleMs(now: Long = System.currentTimeMillis()): Long? {
        if (!isUnlocked(now)) return null
        val last = prefs.getLong(KEY_LAST, issuedAt)
        return (idleLimitMs - (now - last)).coerceAtLeast(0L)
    }

    fun remainingAbsMs(now: Long = System.currentTimeMillis()): Long? {
        if (!isUnlocked(now)) return null
        return (absLimitMs - (now - issuedAt)).coerceAtLeast(0L)
    }

    fun view(now: Long = System.currentTimeMillis()): OperatorSessionView {
        val unlocked = isUnlocked(now)
        return OperatorSessionView(
            unlocked = unlocked,
            operatorId = if (unlocked) operatorId else null,
            remainingIdleMs = remainingIdleMs(now),
            remainingAbsMs = remainingAbsMs(now),
            detail = when {
                unlocked -> {
                    val idleMin = (remainingIdleMs(now) ?: 0L) / 60_000L
                    val absMin = (remainingAbsMs(now) ?: 0L) / 60_000L
                    "Idle ${idleMin}m · abs ${absMin}m"
                }
                token != null -> "Session expired."
                else -> "No operator session on this device."
            },
        )
    }

    @Synchronized
    fun put(token: String, operatorId: String?, now: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_OPERATOR, operatorId.orEmpty())
            .putLong(KEY_ISSUED, now)
            .putLong(KEY_LAST, now)
            .apply()
        lastTouchElapsed = now
    }

    @Synchronized
    fun touch(now: Long = System.currentTimeMillis()): Boolean {
        if (!isUnlocked(now)) return false
        prefs.edit().putLong(KEY_LAST, now).apply()
        lastTouchElapsed = now
        return true
    }

    @Synchronized
    fun clear() {
        prefs.edit().clear().apply()
        lastTouchElapsed = 0L
    }

    companion object {
        private const val FILE = "vesper_operator_session"
        private const val KEY_TOKEN = "session_token"
        private const val KEY_OPERATOR = "operator_id"
        private const val KEY_ISSUED = "issued_at"
        private const val KEY_LAST = "last_activity"

        private fun createPrefs(context: Context): SharedPreferences {
            val master = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                FILE,
                master,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}
