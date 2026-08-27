package com.vesper.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vesper.mobile.AppContainer
import com.vesper.mobile.data.mortis.DashboardSnapshot
import com.vesper.mobile.data.mortis.MortisResult
import com.vesper.mobile.data.settings.VesperSettings
import com.vesper.mobile.domain.AttentionView
import com.vesper.mobile.domain.LastEventView
import com.vesper.mobile.domain.LinkStatus
import com.vesper.mobile.domain.OperatorSessionView
import com.vesper.mobile.domain.SurfaceStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val ready: Boolean = false,
    val settings: VesperSettings = VesperSettings(),
    val surface: SurfaceStatus = SurfaceStatus(
        network = false,
        vesper = LinkStatus.unknown,
        mortis = LinkStatus.unknown,
        operator = OperatorSessionView(false, null, null, null, "Not checked."),
        attention = AttentionView(null, "—"),
        lastEvent = LastEventView(null, null),
    ),
    val dashboard: DashboardSnapshot? = null,
    val probing: Boolean = false,
)

class AppStateViewModel(private val c: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private var tick: Job? = null

    init {
        viewModelScope.launch {
            c.settings.flow
                .catch { emit(VesperSettings()) }
                .collect { s -> _state.update { it.copy(settings = s) } }
        }
        viewModelScope.launch {
            c.connectivity.online.collect { online ->
                _state.update { cur ->
                    cur.copy(surface = cur.surface.copy(network = online))
                }
            }
        }
        refresh(initial = true)
        viewModelScope.launch {
            delay(2_000)
            _state.update { cur -> if (cur.ready) cur else cur.copy(ready = true, probing = false) }
        }
        tick = viewModelScope.launch {
            while (true) {
                delay(15_000)
                applySession()
            }
        }
    }

    fun refresh(initial: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(probing = true) }
            val network = runCatching { c.connectivity.isCurrentlyOnline() }.getOrDefault(false)
            val mortis = if (network) {
                runCatching { c.mortis.health() }.getOrElse {
                    LinkStatus(false, "UNAVAILABLE", it.message ?: "Mortis probe failed (${it.javaClass.simpleName}).")
                }
            } else {
                LinkStatus.offline.copy(label = "UNAVAILABLE")
            }
            val (vesperLabel, vesperDetail) = runCatching {
                c.vesper.statusLine()
            }.getOrDefault("NOT CONNECTED" to "Vesper core probe failed.")
            val vesper = LinkStatus(
                reachable = vesperLabel == "REACHABLE",
                label = vesperLabel,
                detail = vesperDetail,
            )
            val op = runCatching { c.session.view() }.getOrElse {
                OperatorSessionView(false, null, null, null, "Session unreadable. Locked.")
            }
            var attention = AttentionView(null, if (op.unlocked) "Waiting on dashboard." else "Locked.")
            var last = LastEventView(null, null)
            var dash: DashboardSnapshot? = null
            if (op.unlocked && mortis.reachable) {
                when (val d = runCatching { c.mortis.dashboard() }.getOrNull()) {
                    is MortisResult.Ok -> {
                        dash = d.value
                        attention = AttentionView(
                            d.value.attention,
                            d.value.attention?.toString() ?: "none reported",
                        )
                        last = LastEventView(d.value.lastEvent, d.value.lastEventAt)
                    }
                    null -> attention = AttentionView(null, "Dashboard unread.")
                    else -> attention = AttentionView(null, d.detailOrNull() ?: "Dashboard unread.")
                }
            }
            _state.update {
                it.copy(
                    ready = true,
                    probing = false,
                    dashboard = dash,
                    surface = SurfaceStatus(
                        network = network,
                        vesper = vesper,
                        mortis = mortis,
                        operator = op,
                        attention = attention,
                        lastEvent = last,
                    ),
                )
            }
        }
    }

    fun applySession() {
        val op = runCatching { c.session.view() }.getOrElse {
            OperatorSessionView(false, null, null, null, "Session unreadable. Locked.")
        }
        _state.update { cur ->
            cur.copy(surface = cur.surface.copy(operator = op))
        }
        if (!op.unlocked && curDashboardRequiresLock()) {
            _state.update { it.copy(dashboard = null) }
        }
    }

    private fun curDashboardRequiresLock(): Boolean = _state.value.dashboard != null

    fun logout() {
        viewModelScope.launch {
            c.mortis.logout()
            refresh()
        }
    }
}
