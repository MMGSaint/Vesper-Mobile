package com.vesper.mobile.data.vesper

/**
 * Future companion transport to the PC Vesper core (`vesper.client` v1).
 *
 * Vesper-AI currently exposes that contract in-process only. There is no
 * network listener and this APK must not invent one. The phone shell has
 * to open even when every transport is unavailable.
 */
enum class TransportKind {
    UNAVAILABLE,
    FUTURE_LOCAL,
    FUTURE_LAN,
}

data class TransportStatus(
    val kind: TransportKind,
    val connected: Boolean,
    val label: String,
    val detail: String,
) {
    companion object {
        val NotConnected = TransportStatus(
            kind = TransportKind.UNAVAILABLE,
            connected = false,
            label = "NOT CONNECTED",
            detail = "vesper.client v1 lives in the PC core as an in-process gateway. No Android transport is implemented.",
        )
    }
}

interface VesperTransport {
    val kind: TransportKind
    suspend fun status(): TransportStatus
}

class UnavailableTransport : VesperTransport {
    override val kind: TransportKind = TransportKind.UNAVAILABLE
    override suspend fun status(): TransportStatus = TransportStatus.NotConnected
}

class FutureLocalTransport : VesperTransport {
    override val kind: TransportKind = TransportKind.FUTURE_LOCAL
    override suspend fun status(): TransportStatus = TransportStatus(
        kind = kind,
        connected = false,
        label = "NOT CONNECTED",
        detail = "Future on-device / loopback transport is reserved. The PC core has no listener yet.",
    )
}

class FutureLanTransport : VesperTransport {
    override val kind: TransportKind = TransportKind.FUTURE_LAN
    override suspend fun status(): TransportStatus = TransportStatus(
        kind = kind,
        connected = false,
        label = "NOT CONNECTED",
        detail = "Future authenticated LAN transport is reserved. Do not treat a phone as a remote OS authority.",
    )
}
