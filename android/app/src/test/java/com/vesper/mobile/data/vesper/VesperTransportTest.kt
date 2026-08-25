package com.vesper.mobile.data.vesper

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VesperTransportTest {

    @Test
    fun unavailableTransportIsHonest() = runBlocking {
        val status = UnavailableTransport().status()
        assertEquals(TransportKind.UNAVAILABLE, status.kind)
        assertFalse(status.connected)
        assertEquals("NOT CONNECTED", status.label)
    }

    @Test
    fun futureLocalTransportStaysDisconnected() = runBlocking {
        val status = FutureLocalTransport().status()
        assertEquals(TransportKind.FUTURE_LOCAL, status.kind)
        assertFalse(status.connected)
        assertEquals("NOT CONNECTED", status.label)
    }

    @Test
    fun futureLanTransportStaysDisconnected() = runBlocking {
        val status = FutureLanTransport().status()
        assertEquals(TransportKind.FUTURE_LAN, status.kind)
        assertFalse(status.connected)
        assertEquals("NOT CONNECTED", status.label)
    }
}
