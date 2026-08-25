package com.vesper.mobile.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vesper.mobile.debug.StartupCrashLog
import com.vesper.mobile.domain.OperatorSessionView
import java.util.concurrent.TimeUnit

/**
 * Operator session token only. Passphrase is never written.
 * Idle 15 min / absolute 60 min, enforced client-side in addition to the worker.
 *
 * EncryptedSharedPreferences + Android Keystore can throw on some devices
 * (Samsung Keystore / corrupted keyset). That must log the operator out,
 * never kill the process.
 */
class SessionStore(context: Context) {

    private val appContext = context.applicationContext
    private val opened = openPrefs(appContext)
    private val prefs: SharedPreferences = opened.prefs

    val storageKind: String = opened.kind
    val storageDetail: String = opened.detail

    @Volatile
    private var lastTouchElapsed: Long = 0L

    val idleLimitMs: Long = TimeUnit.MINUTES.toMillis(15)
    val absLimitMs: Long = TimeUnit.MINUTES.toMillis(60)

    val token: String?
        get() = runCatching { prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() } }.getOrNull()

    val operatorId: String?
        get() = runCatching { prefs.getString(KEY_OPERATOR, null)?.takeIf { it.isNotBlank() } }.getOrNull()

    val issuedAt: Long
        get() = runCatching { prefs.getLong(KEY_ISSUED, 0L) }.getOrDefault(0L)

    fun isUnlocked(now: Long = System.currentTimeMillis()): Boolean {
        val t = token ?: return false
        if (t.isBlank()) return false
        if (now - issuedAt >= absLimitMs) {
            clear()
            return false
        }
        val last = runCatching { prefs.getLong(KEY_LAST, issuedAt) }.getOrDefault(issuedAt)
        if (now - last >= idleLimitMs) {
            clear()
            return false
        }
        return true
    }

    fun remainingIdleMs(now: Long = System.currentTimeMillis()): Long? {
        if (!isUnlocked(now)) return null
        val last = runCatching { prefs.getLong(KEY_LAST, issuedAt) }.getOrDefault(issuedAt)
        return (idleLimitMs - (now - last)).coerceAtLeast(0L)
    }

    fun remainingAbsMs(now: Long = System.currentTimeMillis()): Long? {
        if (!isUnlocked(now)) return null
        return (absLimitMs - (now - issuedAt)).coerceAtLeast(0L)
    }

    fun view(now: Long = System.currentTimeMillis()): OperatorSessionView {
        return runCatching {
            val unlocked = isUnlocked(now)
            OperatorSessionView(
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
                    storageKind == KIND_FALLBACK && token == null ->
                        "Encrypted session store unavailable. Logged out. $storageDetail"
                    token != null -> "Session expired."
                    else -> "No operator session on this device."
                },
            )
        }.getOrElse {
            clear()
            OperatorSessionView(
                unlocked = false,
                operatorId = null,
                remainingIdleMs = null,
                remainingAbsMs = null,
                detail = "Session store unreadable. Logged out.",
            )
        }
    }

    @Synchronized
    fun put(token: String, operatorId: String?, now: Long = System.currentTimeMillis()) {
        runCatching {
            prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_OPERATOR, operatorId.orEmpty())
                .putLong(KEY_ISSUED, now)
                .putLong(KEY_LAST, now)
                .apply()
            lastTouchElapsed = now
        }
    }

    @Synchronized
    fun touch(now: Long = System.currentTimeMillis()): Boolean {
        if (!isUnlocked(now)) return false
        runCatching { prefs.edit().putLong(KEY_LAST, now).apply() }
        lastTouchElapsed = now
        return true
    }

    @Synchronized
    fun clear() {
        runCatching { prefs.edit().clear().apply() }
        lastTouchElapsed = 0L
    }

    companion object {
        private const val TAG = "VesperSession"
        private const val FILE = "vesper_operator_session"
        private const val FALLBACK_FILE = "vesper_operator_session_fallback"
        private const val KEY_TOKEN = "session_token"
        private const val KEY_OPERATOR = "operator_id"
        private const val KEY_ISSUED = "issued_at"
        private const val KEY_LAST = "last_activity"
        const val KIND_ENCRYPTED = "encrypted"
        const val KIND_FALLBACK = "fallback"

        data class OpenedPrefs(
            val prefs: SharedPreferences,
            val kind: String,
            val detail: String,
        )

        internal fun openPrefs(context: Context): OpenedPrefs {
            val first = runCatching { encrypted(context) }
            first.getOrNull()?.let {
                return OpenedPrefs(it, KIND_ENCRYPTED, "Android Keystore session store.")
            }
            val err = first.exceptionOrNull()
            Log.e(TAG, "Encrypted session store failed; rebuilding.", err)
            err?.let { StartupCrashLog.write(context, it, "session-store first open") }
            runCatching { context.deleteSharedPreferences(FILE) }
            val retry = runCatching { encrypted(context) }
            retry.getOrNull()?.let {
                return OpenedPrefs(it, KIND_ENCRYPTED, "Session store rebuilt after a Keystore error.")
            }
            Log.e(TAG, "Encrypted session store rebuild failed; using private fallback.", retry.exceptionOrNull())
            val fallback = context.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)
            fallback.edit().clear().apply()
            return OpenedPrefs(
                fallback,
                KIND_FALLBACK,
                retry.exceptionOrNull()?.javaClass?.simpleName ?: "Keystore unavailable",
            )
        }

        private fun encrypted(context: Context): SharedPreferences {
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
