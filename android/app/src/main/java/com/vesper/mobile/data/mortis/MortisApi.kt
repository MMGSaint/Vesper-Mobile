package com.vesper.mobile.data.mortis

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class MortisApi(
    private val json: Json,
    client: OkHttpClient? = null,
) {
    private val http: OkHttpClient = client ?: OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val healthClient: OkHttpClient = http.newBuilder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun health(host: String): MortisEnvelope {
        val url = host.trimEnd('/') + "/v1/health"
        return execute(healthClient, Request.Builder().url(url).get().build())
    }

    suspend fun publicGet(host: String, path: String): MortisEnvelope {
        val url = host.trimEnd('/') + path
        return execute(http, Request.Builder().url(url).get().build())
    }

    suspend fun adminGet(
        host: String,
        adminSeg: String,
        path: String,
        query: Map<String, String> = emptyMap(),
        bearer: String?,
        confirm: String? = null,
    ): MortisEnvelope {
        val url = adminUrl(host, adminSeg, path, query) ?: return badUrl()
        val req = Request.Builder().url(url).get().applyAuth(bearer, confirm).build()
        return execute(http, req)
    }

    suspend fun adminPost(
        host: String,
        adminSeg: String,
        path: String,
        body: String?,
        bearer: String?,
        confirm: String? = null,
    ): MortisEnvelope {
        val url = adminUrl(host, adminSeg, path) ?: return badUrl()
        val media = JSON
        val requestBody: RequestBody = (body ?: "{}").toRequestBody(media)
        val req = Request.Builder().url(url).post(requestBody).applyAuth(bearer, confirm).build()
        return execute(http, req)
    }

    private fun adminUrl(
        host: String,
        adminSeg: String,
        path: String,
        query: Map<String, String> = emptyMap(),
    ): HttpUrl? {
        val base = host.trimEnd('/').toHttpUrlOrNull() ?: return null
        val seg = adminSeg.trim().trim('/')
        if (seg.isEmpty()) return null
        val cleanPath = path.trim().trimStart('/')
        val builder = base.newBuilder()
            .addPathSegment("admin")
            .addPathSegment(seg)
        cleanPath.split('/').filter { it.isNotEmpty() }.forEach { builder.addPathSegment(it) }
        query.forEach { (k, v) -> if (v.isNotEmpty()) builder.addQueryParameter(k, v) }
        return builder.build()
    }

    private fun Request.Builder.applyAuth(bearer: String?, confirm: String?): Request.Builder {
        header("Accept", "application/json")
        if (bearer != null) header("Authorization", "Bearer $bearer")
        if (!confirm.isNullOrBlank()) header(CONFIRM_HEADER, confirm)
        return this
    }

    /**
     * All Mortis network I/O runs on Dispatchers.IO and never throws into the
     * caller. On a physical device, running the synchronous OkHttp call on the
     * main dispatcher raised NetworkOnMainThreadException: the unlock path
     * crashed the process and the health probe surfaced the null-message
     * fallback "Mortis probe failed." A failure here becomes an envelope with
     * code -1 so every ViewModel renders an error state instead of dying.
     */
    private suspend fun execute(client: OkHttpClient, request: Request): MortisEnvelope =
        withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    val parsed: JsonElement? = if (raw.isBlank()) {
                        null
                    } else {
                        runCatching { json.parseToJsonElement(raw) }.getOrNull()
                    }
                    MortisEnvelope(code = resp.code, raw = raw, json = parsed)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                MortisEnvelope(
                    code = -1,
                    raw = failure.message ?: failure.javaClass.simpleName,
                    json = null,
                )
            }
        }

    private fun badUrl(): MortisEnvelope =
        MortisEnvelope(code = -2, raw = "Invalid host or empty admin path segment.", json = null)

    companion object {
        const val CONFIRM_HEADER = "x-mortis-confirm"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
