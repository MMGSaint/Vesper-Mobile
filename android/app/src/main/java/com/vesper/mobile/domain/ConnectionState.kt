package com.vesper.mobile.domain

data class LinkStatus(
    val reachable: Boolean,
    val label: String,
    val detail: String,
) {
    companion object {
        val unknown = LinkStatus(
            reachable = false,
            label = "UNKNOWN",
            detail = "Not probed this session.",
        )
        val offline = LinkStatus(
            reachable = false,
            label = "UNAVAILABLE",
            detail = "No network path.",
        )
    }
}

data class OperatorSessionView(
    val unlocked: Boolean,
    val operatorId: String?,
    val remainingIdleMs: Long?,
    val remainingAbsMs: Long?,
    val detail: String,
) {
    val label: String
        get() = if (unlocked) "UNLOCKED" else "LOCKED"
}

data class AttentionView(
    val count: Int?,
    val detail: String,
)

data class LastEventView(
    val summary: String?,
    val at: String?,
)

data class SurfaceStatus(
    val network: Boolean,
    val vesper: LinkStatus,
    val mortis: LinkStatus,
    val operator: OperatorSessionView,
    val attention: AttentionView,
    val lastEvent: LastEventView,
) {
    val offlineBanner: String? = when {
        !network -> "VESPER OFFLINE"
        !mortis.reachable -> "MORTIS UNAVAILABLE"
        !operator.unlocked -> "OPERATOR LOCKED"
        else -> null
    }
}
