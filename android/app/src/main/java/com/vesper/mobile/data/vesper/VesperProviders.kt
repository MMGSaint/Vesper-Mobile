package com.vesper.mobile.data.vesper

import com.vesper.mobile.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

sealed class ProviderAvailability {
    data object Available : ProviderAvailability()
    data class Unavailable(val reason: String) : ProviderAvailability()
}

data class CompletionRequest(
    val text: String,
    val history: List<ChatTurn>,
)

data class ChatTurn(
    val id: String,
    val role: String,
    val text: String,
    val atEpochMs: Long,
    val status: String = "ok",
    val error: String? = null,
)

sealed class CompletionResult {
    data class Text(val text: String) : CompletionResult()
    data class Failed(val reason: String) : CompletionResult()
}

interface AIProvider {
    val id: String
    val displayName: String
    suspend fun availability(): ProviderAvailability
    suspend fun complete(request: CompletionRequest): CompletionResult
}

class LocalVesper : AIProvider {
    override val id: String = "local"
    override val displayName: String = "Local Vesper"
    override suspend fun availability(): ProviderAvailability =
        ProviderAvailability.Unavailable(
            "Local Vesper core runs on the PC host. It is not packaged in this Android client.",
        )

    override suspend fun complete(request: CompletionRequest): CompletionResult =
        CompletionResult.Failed("Local Vesper is unavailable on this device.")
}

class RemoteProvider(
    private val settings: SettingsStore,
    private val json: Json,
) : AIProvider {
    override val id: String = "remote"
    override val displayName: String = "Remote Vesper"

    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    override suspend fun availability(): ProviderAvailability {
        val endpoint = settings.snapshot().vesperEndpoint
        if (endpoint.isBlank()) {
            return ProviderAvailability.Unavailable(
                "No remote Vesper endpoint configured. Set one in Settings, or use the PC core.",
            )
        }
        return withContext(Dispatchers.IO) {
            val urls = listOf("$endpoint/health", "$endpoint/v1/health", endpoint)
            var last = "No response."
            for (url in urls) {
                val req = Request.Builder().url(url).get().header("Accept", "application/json").build()
                val env = runCatching { http.newCall(req).execute().use { it.code to (it.body?.string().orEmpty()) } }
                    .getOrElse {
                        last = it.message ?: "network error"
                        continue
                    }
                if (env.first in 200..299) {
                    return@withContext ProviderAvailability.Available
                }
                last = "HTTP ${env.first} from $url"
            }
            ProviderAvailability.Unavailable("Remote Vesper endpoint did not report healthy. $last")
        }
    }

    override suspend fun complete(request: CompletionRequest): CompletionResult {
        val endpoint = settings.snapshot().vesperEndpoint
        if (endpoint.isBlank()) {
            return CompletionResult.Failed("No remote Vesper endpoint configured.")
        }
        return withContext(Dispatchers.IO) {
            val payload = JsonObject(
                mapOf(
                    "message" to JsonPrimitive(request.text),
                    "source" to JsonPrimitive("vesper-android"),
                ),
            )
            val body = payload.toString().toRequestBody(JSON)
            val urls = listOf("$endpoint/v1/chat", "$endpoint/chat", endpoint)
            var last = "No conversation path answered."
            for (url in urls) {
                val req = Request.Builder()
                    .url(url)
                    .post(body)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .build()
                val env = runCatching {
                    http.newCall(req).execute().use { it.code to (it.body?.string().orEmpty()) }
                }.getOrElse {
                    last = it.message ?: "network error"
                    continue
                }
                if (env.first in 200..299) {
                    val parsed = runCatching { json.parseToJsonElement(env.second) }.getOrNull() as? JsonObject
                    val text = parsed?.let { o ->
                        listOf("reply", "content", "text", "message").firstNotNullOfOrNull { k ->
                            (o[k] as? JsonPrimitive)?.contentOrNull
                        }
                    } ?: env.second.takeIf { it.isNotBlank() }
                    if (!text.isNullOrBlank()) return@withContext CompletionResult.Text(text)
                    last = "Empty body from $url"
                    continue
                }
                last = "HTTP ${env.first} from $url: ${env.second.take(160)}"
            }
            CompletionResult.Failed(
                "Remote conversation is not a documented Vesper PC protocol. Last error: $last",
            )
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

class FutureProvider : AIProvider {
    override val id: String = "future"
    override val displayName: String = "Future provider"
    override suspend fun availability(): ProviderAvailability =
        ProviderAvailability.Unavailable("Reserved. No future provider is wired on this client.")

    override suspend fun complete(request: CompletionRequest): CompletionResult =
        CompletionResult.Failed("Future provider is not available.")
}

class VesperCoreClient {
    val availability: ProviderAvailability =
        ProviderAvailability.Unavailable(
            "VesperCoreClient speaks the PC host protocol. That protocol is not implemented on Android.",
        )
}

class VesperConversationProvider {
    val availability: ProviderAvailability =
        ProviderAvailability.Unavailable(
            "Conversation provider requires the PC Vesper core. This APK does not embed it.",
        )
}

class VesperToolProvider {
    val availability: ProviderAvailability =
        ProviderAvailability.Unavailable(
            "Tool execution is a PC-host concern. Android is a control surface only.",
        )
}

class VesperMemoryProvider {
    val availability: ProviderAvailability =
        ProviderAvailability.Unavailable(
            "Persistent Vesper memory lives with the PC core. Not present on this device.",
        )
}

class VesperPresenceProvider {
    val availability: ProviderAvailability =
        ProviderAvailability.Unavailable(
            "Presence is not implemented on Android. Settings expose the preference only.",
        )
}

class VesperEnvironment(
    settings: SettingsStore,
    json: Json,
) {
    val local = LocalVesper()
    val remote = RemoteProvider(settings, json)
    val future = FutureProvider()
    val core = VesperCoreClient()
    val conversation = VesperConversationProvider()
    val tools = VesperToolProvider()
    val memory = VesperMemoryProvider()
    val presence = VesperPresenceProvider()

    val providers: List<AIProvider> = listOf(local, remote, future)

    suspend fun firstAvailable(): AIProvider? {
        providers.forEach { p ->
            if (p.availability() is ProviderAvailability.Available) return p
        }
        return null
    }

    suspend fun statusLine(): Pair<String, String> {
        val remoteState = remote.availability()
        return when (remoteState) {
            is ProviderAvailability.Available -> "REACHABLE" to "Remote endpoint answered health."
            is ProviderAvailability.Unavailable -> {
                val localState = local.availability()
                "UNAVAILABLE" to when (localState) {
                    is ProviderAvailability.Unavailable -> localState.reason
                    else -> remoteState.reason
                }
            }
        }
    }
}
