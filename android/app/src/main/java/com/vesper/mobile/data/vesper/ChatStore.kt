package com.vesper.mobile.data.vesper

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class StoredTurn(
    val id: String,
    val role: String,
    val text: String,
    val atEpochMs: Long,
    val status: String = "ok",
    val error: String? = null,
)

class ChatStore(
    context: Context,
    private val json: Json,
) {
    private val file = File(context.applicationContext.filesDir, "vesper_chat.json")

    suspend fun load(): List<ChatTurn> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        val raw = runCatching { file.readText() }.getOrNull().orEmpty()
        if (raw.isBlank()) return@withContext emptyList()
        runCatching {
            json.decodeFromString<List<StoredTurn>>(raw).map {
                ChatTurn(it.id, it.role, it.text, it.atEpochMs, it.status, it.error)
            }
        }.getOrElse { emptyList() }
    }

    suspend fun save(turns: List<ChatTurn>) = withContext(Dispatchers.IO) {
        val payload = turns.map {
            StoredTurn(it.id, it.role, it.text, it.atEpochMs, it.status, it.error)
        }
        file.writeText(json.encodeToString(payload))
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        if (file.exists()) file.delete()
    }

    fun newId(): String = UUID.randomUUID().toString()
}
