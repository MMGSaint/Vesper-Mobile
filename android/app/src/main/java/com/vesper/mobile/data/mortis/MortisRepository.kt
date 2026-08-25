package com.vesper.mobile.data.mortis

import com.vesper.mobile.data.settings.SettingsStore
import com.vesper.mobile.domain.LinkStatus
import com.vesper.mobile.security.SessionStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class MortisRepository(
    private val api: MortisApi,
    private val settings: SettingsStore,
    private val session: SessionStore,
    private val json: Json,
) {
    suspend fun health(): LinkStatus {
        val host = settings.snapshot().mortisHost
        val env = api.health(host)
        return when {
            env.code == -1 -> LinkStatus(
                reachable = false,
                label = "UNAVAILABLE",
                detail = env.raw.ifBlank { "Network error contacting Mortis." },
            )
            env.okHttp && (env.bool("ok") == true) -> LinkStatus(
                reachable = true,
                label = "REACHABLE",
                detail = env.string("service") ?: "mortis-relay",
            )
            env.okHttp -> LinkStatus(
                reachable = false,
                label = "DEGRADED",
                detail = "HTTP ${env.code}: ${env.string("error", "message") ?: env.raw.take(160)}",
            )
            else -> LinkStatus(
                reachable = false,
                label = "UNAVAILABLE",
                detail = "HTTP ${env.code}: ${env.raw.take(160).ifBlank { "no body" }}",
            )
        }
    }

    suspend fun unlock(passphrase: String, operatorId: String): MortisResult<String> {
        val s = settings.snapshot()
        if (s.adminPathSeg.isBlank()) {
            return MortisResult.Misconfigured("Admin path segment is not set.")
        }
        if (passphrase.isBlank()) {
            return MortisResult.Misconfigured("Passphrase is empty.")
        }
        val body = api.encodeValue(
            UnlockRequest(passphrase = passphrase, operator_id = operatorId),
        )
        val env = api.adminPost(s.mortisHost, s.adminPathSeg, "api/session/unlock", body, bearer = null)
        return when {
            env.code == -1 -> MortisResult.NetworkError(env.raw)
            env.code == -2 -> MortisResult.Misconfigured(env.raw)
            env.code == 401 || env.code == 403 -> MortisResult.Unauthorized(
                env.string("error", "message") ?: "Unlock refused (${env.code}).",
            )
            env.okHttp -> {
                val token = env.string("token", "session_token", "session", "access_token")
                if (token.isNullOrBlank()) {
                    MortisResult.HttpError(env.code, "Unlock response had no session token.", env.raw)
                } else {
                    val op = env.string("operator_id", "operator") ?: operatorId
                    session.put(token, op)
                    MortisResult.Ok(token)
                }
            }
            else -> env.toError()
        }
    }

    suspend fun logout(): MortisResult<Unit> {
        runCatching { adminPost("api/session/logout", "{}") }
        session.clear()
        return MortisResult.Ok(Unit)
    }

    suspend fun refresh(): MortisResult<Unit> {
        val env = adminPost("api/session/refresh", "{}")
        if (env is MortisResult.Ok) {
            val token = env.value.string("token", "session_token", "access_token")
            if (!token.isNullOrBlank()) {
                session.put(token, session.operatorId)
            } else {
                session.touch()
            }
            return MortisResult.Ok(Unit)
        }
        return env.mapEmpty()
    }

    suspend fun stepUp(passphrase: String, op: String): MortisResult<String> {
        val body = api.encodeValue(StepUpRequest(passphrase = passphrase, op = op))
        return when (val env = adminPost("api/session/stepup", body)) {
            is MortisResult.Ok -> {
                val confirm = env.value.string("confirm", "confirm_token", "token", "x-mortis-confirm")
                    ?: op
                MortisResult.Ok(confirm)
            }
            is MortisResult.ConfirmRequired -> MortisResult.HttpError(
                428,
                "Step-up itself required confirm — unexpected.",
                "",
            )
            is MortisResult.Unauthorized -> env
            is MortisResult.HttpError -> env
            is MortisResult.NetworkError -> env
            is MortisResult.Misconfigured -> env
            is MortisResult.Locked -> env
        }
    }

    suspend fun dashboard(): MortisResult<DashboardSnapshot> =
        adminGet("api/dashboard").map { env ->
            val obj = env.obj
            val attention = env.int("attention", "attention_count", "needs_attention")
                ?: obj?.child("inbox", "staging")?.int("attention", "count")
            val last = obj?.child("last_event", "event", "latest")
            val lastSummary = env.string("last_event")
                ?: last?.str("summary", "title", "action", "message", "type")
            val lastAt = last?.str("at", "created_at", "time", "ts")
                ?: env.string("last_event_at")
            DashboardSnapshot(
                attention = attention,
                lastEvent = lastSummary,
                lastEventAt = lastAt,
                tiles = deriveTiles(env),
                raw = obj,
            )
        }

    suspend fun status(): MortisResult<MortisEnvelope> = adminGet("api/status")

    suspend fun inbox(query: InboxQuery): MortisResult<Paged> =
        adminGet("api/inbox", query.toQueryMap()).map { Paged.from(it) }

    suspend fun staging(query: InboxQuery): MortisResult<Paged> =
        adminGet("api/staging", query.toQueryMap()).map { Paged.from(it) }

    suspend fun stagingDetail(id: String): MortisResult<MortisEnvelope> =
        adminGet("api/staging/$id")

    suspend fun stagingDiff(id: String): MortisResult<MortisEnvelope> =
        adminGet("api/staging/$id/diff")

    suspend fun fragments(q: String): MortisResult<MortisEnvelope> =
        adminGet("api/fragments", mapOf("q" to q).filterValues { it.isNotBlank() })

    suspend fun discovery(): MortisResult<MortisEnvelope> = adminGet("api/discovery")

    suspend fun schedule(): MortisResult<MortisEnvelope> = adminGet("api/schedule")

    suspend fun applications(): MortisResult<MortisEnvelope> = adminGet("api/applications")

    suspend fun releases(): MortisResult<MortisEnvelope> = adminGet("api/releases")

    suspend fun releaseCandidate(): MortisResult<MortisEnvelope> = adminGet("api/release/candidate")

    suspend fun releaseDiff(): MortisResult<MortisEnvelope> = adminGet("api/release/diff")

    suspend fun audit(query: AuditQuery): MortisResult<Paged> =
        adminGet("api/audit", query.toQueryMap()).map { Paged.from(it) }

    suspend fun inboxSync(): MortisResult<MortisEnvelope> = adminPost("api/inbox/sync", "{}")

    suspend fun intakePush(files: List<IntakeFile>): MortisResult<MortisEnvelope> {
        val body = api.encodeValue(IntakePushRequest(files))
        return adminPost("api/intake/push", body)
    }

    suspend fun stagingAction(
        id: String,
        action: String,
        body: String? = "{}",
        confirm: String? = null,
    ): MortisResult<MortisEnvelope> =
        adminPost("api/staging/$id/$action", body, confirm)

    suspend fun stagingProposals(body: String = "{}"): MortisResult<MortisEnvelope> =
        adminPost("api/staging/proposals", body)

    suspend fun cancelSchedule(id: String): MortisResult<MortisEnvelope> =
        adminPost("api/schedule/$id/cancel", "{}")

    suspend fun channelsAssign(body: ChannelAssignBody, confirm: String?): MortisResult<MortisEnvelope> =
        adminPost("api/channels/assign", json.encodeToString(body), confirm)

    suspend fun channelsPromote(body: ChannelPromoteBody, confirm: String): MortisResult<MortisEnvelope> =
        adminPost("api/channels/promote", json.encodeToString(body), confirm)

    suspend fun channelsSchedule(body: ChannelScheduleBody): MortisResult<MortisEnvelope> =
        adminPost("api/channels/schedule", json.encodeToString(body))

    suspend fun releaseGenerate(): MortisResult<MortisEnvelope> =
        adminPost("api/release/generate", "{}")

    suspend fun releaseLeakscan(): MortisResult<MortisEnvelope> =
        adminPost("api/release/leakscan", "{}")

    suspend fun releaseExportUnsigned(): MortisResult<MortisEnvelope> =
        adminPost("api/release/export-unsigned", "{}")

    suspend fun releaseImportSigned(body: String, confirm: String): MortisResult<MortisEnvelope> =
        adminPost("api/release/import-signed", body, confirm)

    suspend fun releasePublish(targetChannel: String, confirm: String): MortisResult<MortisEnvelope> {
        val body = json.encodeToString(
            PublishBody(confirm_phrase = "PUBLISH", target_channel = targetChannel),
        )
        return adminPost("api/release/publish", body, confirm)
    }

    data class Paged(
        val items: List<JsonObject>,
        val total: Int,
        val envelope: MortisEnvelope,
    ) {
        companion object {
            fun from(env: MortisEnvelope): Paged {
                val items = env.items()
                return Paged(items = items, total = env.total() ?: items.size, envelope = env)
            }
        }
    }

    private suspend fun adminGet(
        path: String,
        query: Map<String, String> = emptyMap(),
        confirm: String? = null,
    ): MortisResult<MortisEnvelope> {
        val s = settings.snapshot()
        if (s.adminPathSeg.isBlank()) return MortisResult.Misconfigured("Admin path segment is not set.")
        if (!session.isUnlocked()) return MortisResult.Locked("Operator session is locked.")
        val env = api.adminGet(s.mortisHost, s.adminPathSeg, path, query, session.token, confirm)
        return interpret(env)
    }

    private suspend fun adminPost(
        path: String,
        body: String?,
        confirm: String? = null,
    ): MortisResult<MortisEnvelope> {
        val s = settings.snapshot()
        if (s.adminPathSeg.isBlank()) return MortisResult.Misconfigured("Admin path segment is not set.")
        if (!session.isUnlocked()) return MortisResult.Locked("Operator session is locked.")
        val env = api.adminPost(s.mortisHost, s.adminPathSeg, path, body, session.token, confirm)
        return interpret(env)
    }

    private fun interpret(env: MortisEnvelope): MortisResult<MortisEnvelope> {
        return when {
            env.code == -1 -> MortisResult.NetworkError(env.raw)
            env.code == -2 -> MortisResult.Misconfigured(env.raw)
            env.code == 401 || env.code == 403 -> {
                session.clear()
                MortisResult.Unauthorized(env.string("error", "message") ?: "Session rejected (${env.code}).")
            }
            env.code == 428 -> {
                val op = env.string("op", "confirm", "required_op", "action") ?: "confirm"
                MortisResult.ConfirmRequired(ConfirmNeed(op, env.string("error", "message", "detail")))
            }
            env.okHttp -> {
                session.touch()
                MortisResult.Ok(env)
            }
            else -> env.toError()
        }
    }

    private fun deriveTiles(env: MortisEnvelope): List<DashboardTile> {
        val obj = env.obj ?: return emptyList()
        val out = mutableListOf<DashboardTile>()
        fun add(key: String, label: String, value: String?, route: String?) {
            if (!value.isNullOrBlank()) out += DashboardTile(key, label, value, route)
        }
        add(
            "attention",
            "ATTENTION",
            env.int("attention", "attention_count")?.toString() ?: obj.str("attention"),
            "inbox",
        )
        obj.child("inbox")?.let {
            add("inbox", "INBOX", it.str("count", "pending", "total", "open") ?: "present", "inbox")
        }
        obj.child("staging")?.let {
            add("staging", "STAGING", it.str("count", "pending", "total") ?: summarizeStates(it), "inbox")
        }
        obj.child("releases", "release")?.let {
            add("release", "RELEASE", it.str("state", "status", "candidate", "count") ?: "present", "release")
        }
        obj.child("applications")?.let {
            add("apps", "APPLICATIONS", it.str("count", "total") ?: "present", "applications")
        }
        obj.child("schedule")?.let {
            add("schedule", "SCHEDULE", it.str("upcoming", "count", "next") ?: "present", "schedule")
        }
        obj.child("discovery")?.let {
            add("discovery", "DISCOVERY", it.str("count", "state", "status") ?: "present", "discovery")
        }
        if (out.isEmpty()) {
            obj.entries.take(8).forEach { (k, v) ->
                val display = when (v) {
                    is JsonPrimitive -> v.content
                    is JsonObject -> v.str("count", "state", "status", "total") ?: "{…}"
                    else -> v.toString().take(48)
                }
                out += DashboardTile(k, k.uppercase().replace('_', ' '), display, null)
            }
        }
        return out
    }

    private fun summarizeStates(obj: JsonObject): String {
        val keys = listOf(
            "INGESTED", "STAGED", "UNDER_REVIEW", "APPROVED", "PREPARED",
            "SCHEDULED", "SEALED", "HIDDEN", "REJECTED",
        )
        val parts = keys.mapNotNull { k ->
            val n = obj.int(k, k.lowercase())
            if (n != null) "$k $n" else null
        }
        return parts.joinToString(" · ").ifBlank { obj.str("count", "total") ?: "present" }
    }
}

private fun MortisEnvelope.toError(): MortisResult.HttpError =
    MortisResult.HttpError(
        code = code,
        detail = string("error", "message", "detail") ?: "HTTP $code",
        raw = raw,
    )

private fun <T> MortisResult<MortisEnvelope>.map(f: (MortisEnvelope) -> T): MortisResult<T> = when (this) {
    is MortisResult.Ok -> MortisResult.Ok(f(value))
    is MortisResult.ConfirmRequired -> this
    is MortisResult.Unauthorized -> this
    is MortisResult.HttpError -> this
    is MortisResult.NetworkError -> this
    is MortisResult.Misconfigured -> this
    is MortisResult.Locked -> this
}

private fun MortisResult<MortisEnvelope>.mapEmpty(): MortisResult<Unit> = when (this) {
    is MortisResult.Ok -> MortisResult.Ok(Unit)
    is MortisResult.ConfirmRequired -> this
    is MortisResult.Unauthorized -> this
    is MortisResult.HttpError -> this
    is MortisResult.NetworkError -> this
    is MortisResult.Misconfigured -> this
    is MortisResult.Locked -> this
}
