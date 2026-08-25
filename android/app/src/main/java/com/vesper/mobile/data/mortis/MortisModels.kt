package com.vesper.mobile.data.mortis

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

@Serializable
data class HealthResponse(
    val ok: Boolean = false,
    val service: String? = null,
)

@Serializable
data class UnlockRequest(
    val passphrase: String,
    val operator_id: String,
)

@Serializable
data class StepUpRequest(
    val passphrase: String,
    val op: String,
)

@Serializable
data class IntakePushRequest(
    val files: List<IntakeFile>,
)

@Serializable
data class IntakeFile(
    val filename: String,
    val content: String,
    val source_type: String,
    val source_name: String,
    val submitter: String,
)

@Serializable
data class EditBody(
    val content: String,
    val title: String? = null,
)

@Serializable
data class ScheduleSealBody(
    val run_at: String,
    val operation: String = "SEAL",
)

@Serializable
data class ChannelAssignBody(
    val application_id: String,
    val channel: String,
    val artifact_id: String? = null,
    val release_id: String? = null,
)

@Serializable
data class ChannelPromoteBody(
    val application_id: String,
    val from_channel: String,
    val to_channel: String,
)

@Serializable
data class ChannelScheduleBody(
    val application_id: String,
    val channel: String,
    val run_at: String,
    val artifact_id: String? = null,
)

@Serializable
data class PublishBody(
    val confirm_phrase: String,
    val target_channel: String,
)

@Serializable
data class InboxQuery(
    val q: String = "",
    val state: String = "",
    val source: String = "",
    @SerialName("class") val className: String = "",
    val sensitivity: String = "",
    val sort: String = "",
    val attention: String = "",
    val since: String = "",
    val limit: Int = 40,
    val offset: Int = 0,
) {
    fun toQueryMap(): Map<String, String> = buildMap {
        if (q.isNotBlank()) put("q", q)
        if (state.isNotBlank()) put("state", state)
        if (source.isNotBlank()) put("source", source)
        if (className.isNotBlank()) put("class", className)
        if (sensitivity.isNotBlank()) put("sensitivity", sensitivity)
        if (sort.isNotBlank()) put("sort", sort)
        if (attention.isNotBlank()) put("attention", attention)
        if (since.isNotBlank()) put("since", since)
        put("limit", limit.toString())
        put("offset", offset.toString())
    }
}

@Serializable
data class AuditQuery(
    val q: String = "",
    val actor: String = "",
    val action: String = "",
    val target: String = "",
    val result: String = "",
    val since: String = "",
    val until: String = "",
    val limit: Int = 40,
    val offset: Int = 0,
) {
    fun toQueryMap(): Map<String, String> = buildMap {
        if (q.isNotBlank()) put("q", q)
        if (actor.isNotBlank()) put("actor", actor)
        if (action.isNotBlank()) put("action", action)
        if (target.isNotBlank()) put("target", target)
        if (result.isNotBlank()) put("result", result)
        if (since.isNotBlank()) put("since", since)
        if (until.isNotBlank()) put("until", until)
        put("limit", limit.toString())
        put("offset", offset.toString())
    }
}

data class MortisEnvelope(
    val code: Int,
    val raw: String,
    val json: JsonElement?,
) {
    val okHttp: Boolean get() = code in 200..299
    val obj: JsonObject? get() = json as? JsonObject

    fun string(vararg keys: String): String? {
        val o = obj ?: return null
        for (k in keys) {
            val v = o[k] ?: continue
            when (v) {
                is JsonPrimitive -> {
                    val c = v.contentOrNull
                    if (!c.isNullOrBlank()) return c
                }
                JsonNull -> continue
                else -> return v.toString()
            }
        }
        return null
    }

    fun bool(vararg keys: String): Boolean? {
        val o = obj ?: return null
        for (k in keys) {
            val v = o[k] as? JsonPrimitive ?: continue
            v.booleanOrNull?.let { return it }
        }
        return null
    }

    fun int(vararg keys: String): Int? {
        val o = obj ?: return null
        for (k in keys) {
            val v = o[k] as? JsonPrimitive ?: continue
            v.intOrNull?.let { return it }
            v.longOrNull?.let { return it.toInt() }
            v.contentOrNull?.toIntOrNull()?.let { return it }
        }
        return null
    }

    fun items(): List<JsonObject> {
        val o = obj ?: return emptyList()
        val candidates = listOf("items", "data", "results", "proposals", "records", "rows", "entries")
        for (k in candidates) {
            val arr = o[k] as? JsonArray ?: continue
            return arr.mapNotNull { it as? JsonObject }
        }
        val arrRoot = json as? JsonArray
        if (arrRoot != null) return arrRoot.mapNotNull { it as? JsonObject }
        return emptyList()
    }

    fun total(): Int? = int("total", "count", "total_count") ?: items().size
}

fun JsonObject.str(vararg keys: String): String? {
    for (k in keys) {
        val v = this[k] ?: continue
        when (v) {
            is JsonPrimitive -> {
                val c = v.contentOrNull
                if (!c.isNullOrBlank() && c != "null") return c
            }
            is JsonObject -> {
                v.str("id", "name", "title", "value", "label")?.let { return it }
            }
            else -> Unit
        }
    }
    return null
}

fun JsonObject.int(vararg keys: String): Int? {
    for (k in keys) {
        val v = this[k] as? JsonPrimitive ?: continue
        v.intOrNull?.let { return it }
        v.longOrNull?.let { return it.toInt() }
        v.contentOrNull?.toIntOrNull()?.let { return it }
    }
    return null
}

fun JsonObject.bool(vararg keys: String): Boolean? {
    for (k in keys) {
        val v = this[k] as? JsonPrimitive ?: continue
        v.booleanOrNull?.let { return it }
        when (v.contentOrNull?.lowercase()) {
            "true", "1", "yes" -> return true
            "false", "0", "no" -> return false
        }
    }
    return null
}

fun JsonObject.child(vararg keys: String): JsonObject? {
    for (k in keys) {
        val v = this[k] as? JsonObject
        if (v != null) return v
    }
    return null
}

fun JsonObject.prettyField(key: String): String? {
    val v = this[key] ?: return null
    return when (v) {
        JsonNull -> null
        is JsonPrimitive -> v.contentOrNull
        else -> v.toString()
    }
}

fun JsonElement.asDisplayMap(): List<Pair<String, String>> {
    val o = this as? JsonObject ?: return emptyList()
    return o.entries.mapNotNull { (k, v) ->
        when (v) {
            JsonNull -> null
            is JsonPrimitive -> k to (v.contentOrNull ?: v.toString())
            is JsonArray -> k to "${v.size} items"
            is JsonObject -> k to (v.str("id", "name", "title", "state", "status") ?: "{…}")
        }
    }
}

data class StagingRow(
    val id: String,
    val state: String,
    val title: String,
    val source: String?,
    val className: String?,
    val sensitivity: String?,
    val attention: Boolean,
    val updatedAt: String?,
    val createdAt: String?,
    val raw: JsonObject,
) {
    companion object {
        fun from(obj: JsonObject): StagingRow {
            val id = obj.str("id", "proposal_id", "staging_id", "uuid") ?: ""
            val state = obj.str("state", "status", "staging_state") ?: ""
            val title = obj.str("title", "name", "filename", "subject", "label") ?: id.ifBlank { "(untitled)" }
            return StagingRow(
                id = id,
                state = state,
                title = title,
                source = obj.str("source", "source_name", "source_type"),
                className = obj.str("class", "classification", "kind"),
                sensitivity = obj.str("sensitivity"),
                attention = obj.bool("attention", "needs_attention") ?: false,
                updatedAt = obj.str("updated_at", "updated", "modified_at"),
                createdAt = obj.str("created_at", "created", "ingested_at"),
                raw = obj,
            )
        }
    }
}

data class DashboardSnapshot(
    val attention: Int?,
    val lastEvent: String?,
    val lastEventAt: String?,
    val tiles: List<DashboardTile>,
    val raw: JsonObject?,
)

data class DashboardTile(
    val key: String,
    val label: String,
    val value: String,
    val routeHint: String?,
)

data class ConfirmNeed(
    val op: String,
    val detail: String?,
)

sealed class MortisResult<out T> {
    data class Ok<T>(val value: T) : MortisResult<T>()
    data class ConfirmRequired(val need: ConfirmNeed) : MortisResult<Nothing>()
    data class Unauthorized(val detail: String) : MortisResult<Nothing>()
    data class HttpError(val code: Int, val detail: String, val raw: String) : MortisResult<Nothing>()
    data class NetworkError(val detail: String) : MortisResult<Nothing>()
    data class Misconfigured(val detail: String) : MortisResult<Nothing>()
    data class Locked(val detail: String) : MortisResult<Nothing>()

    fun detailOrNull(): String? = when (this) {
        is Ok -> null
        is ConfirmRequired -> need.detail ?: need.op
        is Unauthorized -> detail
        is HttpError -> detail
        is NetworkError -> detail
        is Misconfigured -> detail
        is Locked -> detail
    }
}
