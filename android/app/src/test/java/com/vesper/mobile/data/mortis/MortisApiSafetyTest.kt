package com.vesper.mobile.data.mortis

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard for the successful-unlock crash.
 *
 * Root cause on device: synchronous OkHttp calls executed on the main
 * dispatcher raised NetworkOnMainThreadException. The unlock path had no
 * guard, so a valid passphrase attempt killed the process, while the home
 * probe swallowed the null-message exception as "Mortis probe failed."
 *
 * Contract locked in here: MortisApi never throws into a caller. Any
 * failure — including hostile RuntimeExceptions and null-message
 * exceptions — becomes an envelope with code -1 and a non-blank reason.
 */
class MortisApiSafetyTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun clientThat(interceptor: Interceptor): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(interceptor).build()

    private fun stubbed(code: Int, body: String): OkHttpClient =
        clientThat { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("stub")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }

    @Test
    fun hostileRuntimeExceptionBecomesErrorEnvelope() = runBlocking {
        val api = MortisApi(json, clientThat { throw RuntimeException("boom") })
        val env = api.health("https://mortis.invalid")
        assertEquals(-1, env.code)
        assertEquals("boom", env.raw)
        assertNull(env.json)
    }

    @Test
    fun nullMessageExceptionReportsItsClassInsteadOfSilence() = runBlocking {
        // NetworkOnMainThreadException carries a null message; the class name
        // must survive so the operator sees a real reason, not a blank.
        val api = MortisApi(json, clientThat { throw IllegalStateException() })
        val env = api.health("https://mortis.invalid")
        assertEquals(-1, env.code)
        assertTrue(env.raw.isNotBlank())
        assertEquals("IllegalStateException", env.raw)
    }

    @Test
    fun unlockStyleSuccessEnvelopeParses() = runBlocking {
        val api = MortisApi(json, stubbed(200, """{"token":"t123","operator_id":"wolf"}"""))
        val env = api.adminPost("https://mortis.invalid", "seg", "api/session/unlock", "{}", bearer = null)
        assertEquals(200, env.code)
        assertNotNull(env.json)
    }

    @Test
    fun malformedSuccessBodyDoesNotThrow() = runBlocking {
        val api = MortisApi(json, stubbed(200, "<html>not json at all"))
        val env = api.health("https://mortis.invalid")
        assertEquals(200, env.code)
        assertNull(env.json)
        assertTrue(env.raw.startsWith("<html>"))
    }

    @Test
    fun emptyAdminSegmentIsMisconfigurationNotCrash() = runBlocking {
        val api = MortisApi(json, stubbed(200, "{}"))
        val env = api.adminGet("https://mortis.invalid", "  ", "api/dashboard", emptyMap(), bearer = null)
        assertEquals(-2, env.code)
    }
}
