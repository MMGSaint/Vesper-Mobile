package com.vesper.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vesper.mobile.data.mortis.InboxQuery
import com.vesper.mobile.data.mortis.str
import com.vesper.mobile.domain.LegalAction
import com.vesper.mobile.domain.LegalCopy
import com.vesper.mobile.domain.Transitions
import com.vesper.mobile.ui.AppStateViewModel
import com.vesper.mobile.ui.components.ConfirmSheet
import com.vesper.mobile.ui.components.DangerButton
import com.vesper.mobile.ui.components.ErrorLine
import com.vesper.mobile.ui.components.FilterChip
import com.vesper.mobile.ui.components.GhostButton
import com.vesper.mobile.ui.components.JsonPreview
import com.vesper.mobile.ui.components.MonoId
import com.vesper.mobile.ui.components.Panel
import com.vesper.mobile.ui.components.SectionLabel
import com.vesper.mobile.ui.components.StateGrid
import com.vesper.mobile.ui.components.StatusRow
import com.vesper.mobile.ui.components.SteelButton
import com.vesper.mobile.ui.components.UnavailableState
import com.vesper.mobile.ui.components.VesperField
import com.vesper.mobile.ui.components.VesperScaffold
import com.vesper.mobile.ui.theme.Elevated
import com.vesper.mobile.ui.theme.Hairline
import com.vesper.mobile.ui.theme.LabelStyle
import com.vesper.mobile.ui.theme.MonoStyle
import com.vesper.mobile.ui.theme.Muted
import com.vesper.mobile.ui.theme.Parchment
import com.vesper.mobile.ui.theme.Steel

@Composable
fun OperatorHomeScreen(
    vm: OperatorHomeViewModel,
    appVm: AppStateViewModel,
    onBack: (() -> Unit)? = null,
    onInbox: () -> Unit,
    onRelease: () -> Unit,
    onApps: () -> Unit,
    onAudit: () -> Unit,
    onSchedule: () -> Unit,
    onDiscovery: () -> Unit,
    onDrive: () -> Unit,
    onLogout: () -> Unit,
    onTile: (String) -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val app by appVm.state.collectAsStateWithLifecycle()
    VesperScaffold(
        title = "OPERATOR ROOM",
        subtitle = app.surface.operator.operatorId ?: "session",
        onBack = onBack,
        banner = app.surface.offlineBanner,
        actions = {
            Text(text = "REFRESH", style = LabelStyle, modifier = Modifier.clickable { vm.refresh(); appVm.refresh() })
        },
    ) {
        if (state.load.loading) {
            Text("LOADING", style = LabelStyle)
        }
        ErrorLine(state.load.error)
        val dash = state.dash
        if (dash == null && !state.load.loading) {
            UnavailableState(
                title = "DASHBOARD UNREAD",
                reason = state.load.error ?: "No dashboard payload. Unlock and confirm Mortis is reachable.",
            )
        }
        dash?.tiles?.forEach { tile ->
            val hint = tile.routeHint
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (hint.isNullOrBlank()) Modifier else Modifier.clickable { onTile(hint) }),
            ) {
                Panel {
                    StatusRow(tile.label, tile.value)
                    if (!hint.isNullOrBlank()) {
                        Text(
                            text = hint.uppercase(),
                            style = MonoStyle.copy(color = Muted, fontSize = 10.sp),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        SectionLabel("ROOMS")
        SteelButton("INBOX / STAGING", onClick = onInbox)
        Spacer(Modifier.height(8.dp))
        SteelButton("RELEASE CENTER", onClick = onRelease)
        Spacer(Modifier.height(8.dp))
        SteelButton("APPLICATIONS / CHANNELS", onClick = onApps)
        Spacer(Modifier.height(8.dp))
        SteelButton("SCHEDULE", onClick = onSchedule)
        Spacer(Modifier.height(8.dp))
        SteelButton("DISCOVERY / TEASE", onClick = onDiscovery)
        Spacer(Modifier.height(8.dp))
        SteelButton("AUDIT", onClick = onAudit)
        Spacer(Modifier.height(8.dp))
        SteelButton("DRIVE INTAKE", onClick = onDrive)
        Spacer(Modifier.height(20.dp))
        DangerButton("LOCK SESSION", onClick = onLogout)
        Spacer(Modifier.height(16.dp))
        Text(
            text = listOf(
                LegalCopy.STAGING_NE_CANON,
                LegalCopy.APPROVE_NE_PUBLISH,
                LegalCopy.PREPARE_NE_RELEASE,
                LegalCopy.TEASE_NE_REVEAL,
                LegalCopy.TRIGGER_NE_REVEAL,
                LegalCopy.SEAL_NE_PUBLICATION,
                LegalCopy.SCHEDULE_NE_EXECUTE,
                LegalCopy.DATASET_NE_BINARY,
            ).joinToString("\n"),
            style = MonoStyle.copy(color = Muted, fontSize = 11.sp),
        )
        Spacer(Modifier.height(8.dp))
        Text(LegalCopy.SIGNING_KEY_OFF_DEVICE, style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
    }
}

@Composable
fun InboxScreen(
    vm: InboxViewModel,
    appVm: AppStateViewModel,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val app by appVm.state.collectAsStateWithLifecycle()
    var q by remember(state.query) { mutableStateOf(state.query.q) }
    var st by remember(state.query) { mutableStateOf(state.query.state) }
    var source by remember(state.query) { mutableStateOf(state.query.source) }
    var cls by remember(state.query) { mutableStateOf(state.query.className) }
    var sensitivity by remember(state.query) { mutableStateOf(state.query.sensitivity) }
    var attention by remember(state.query) { mutableStateOf(state.query.attention) }
    VesperScaffold(
        title = "INBOX",
        subtitle = "${state.tab} · ${state.rows.size}/${state.total}",
        onBack = onBack,
        banner = app.surface.offlineBanner,
        actions = {
            Text(text = "SYNC", style = LabelStyle, modifier = Modifier.clickable { vm.sync() })
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("INBOX", state.tab == "inbox") { vm.onTab("inbox") }
            FilterChip("STAGING", state.tab == "staging") { vm.onTab("staging") }
        }
        SectionLabel("FILTERS")
        VesperField(q, { q = it }, placeholder = "q")
        Spacer(Modifier.height(8.dp))
        VesperField(st, { st = it }, placeholder = "state", mono = true)
        Spacer(Modifier.height(8.dp))
        VesperField(source, { source = it }, placeholder = "source")
        Spacer(Modifier.height(8.dp))
        VesperField(cls, { cls = it }, placeholder = "class")
        Spacer(Modifier.height(8.dp))
        VesperField(sensitivity, { sensitivity = it }, placeholder = "sensitivity")
        Spacer(Modifier.height(8.dp))
        VesperField(attention, { attention = it }, placeholder = "attention")
        Spacer(Modifier.height(8.dp))
        GhostButton("APPLY") {
            vm.applyFilters(
                InboxQuery(
                    q = q, state = st, source = source, className = cls,
                    sensitivity = sensitivity, attention = attention,
                    sort = state.query.sort, since = state.query.since,
                    limit = state.query.limit, offset = 0,
                ),
            )
        }
        ErrorLine(state.load.error)
        if (state.load.loading) Text("LOADING", style = LabelStyle, modifier = Modifier.padding(top = 12.dp))
        SectionLabel("ITEMS")
        if (state.rows.isEmpty() && !state.load.loading) {
            UnavailableState(title = "EMPTY", reason = "No rows for this filter. That is the live result, not a placeholder.")
        }
        state.rows.forEach { row ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(1.dp, Hairline)
                    .background(Elevated)
                    .clickable { if (row.id.isNotBlank()) onOpen(row.id) }
                    .padding(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(row.state.ifBlank { "—" }, style = LabelStyle.copy(color = Steel, fontSize = 10.sp))
                    if (row.attention) Text("ATTENTION", style = LabelStyle.copy(color = Parchment, fontSize = 10.sp))
                }
                Spacer(Modifier.height(6.dp))
                Text(row.title, style = MonoStyle.copy(color = Parchment, fontSize = 13.sp))
                Spacer(Modifier.height(4.dp))
                MonoId(row.id.ifBlank { "(no id)" })
                Text(
                    listOfNotNull(row.source, row.className, row.sensitivity, row.updatedAt).joinToString(" · "),
                    style = MonoStyle.copy(color = Muted, fontSize = 10.sp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("PREV", compact = true, enabled = state.query.offset > 0) { vm.page(-state.query.limit) }
            GhostButton("NEXT", compact = true, enabled = state.query.offset + state.rows.size < state.total) {
                vm.page(state.query.limit)
            }
        }
    }
}

@Composable
fun ProposalDetailScreen(
    id: String,
    vm: ProposalViewModel,
    appVm: AppStateViewModel,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val app by appVm.state.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(id) { vm.load(id) }
    val row = state.row
    val actions = Transitions.legalActions(row?.state.orEmpty())
    val confirm = state.confirm
    VesperScaffold(
        title = "PROPOSAL",
        subtitle = id,
        onBack = onBack,
        banner = app.surface.offlineBanner,
    ) {
        if (state.load.loading) Text("LOADING", style = LabelStyle)
        ErrorLine(state.load.error)
        ErrorLine(state.notice)
        if (row == null && !state.load.loading) {
            UnavailableState(title = "UNREAD", reason = state.load.error ?: "No proposal body.")
        }
        if (row != null) {
            MonoId(row.id)
            StatusRow("STATE", row.state.ifBlank { "—" })
            StatusRow("TITLE", row.title)
            SectionLabel("PLANES")
            val obj = state.envelope?.obj
            StateGrid(
                listOf(
                    "STAGING" to (row.state.ifBlank { "—" }),
                    "CANON" to (obj?.str("canon", "canon_state") ?: "—"),
                    "PLAYER" to (obj?.str("player", "player_state") ?: LegalCopy.NO_BINARY_RELEASE),
                    "DISCOVERY" to (obj?.str("discovery", "discovery_state") ?: "—"),
                    "TEASE" to (obj?.str("tease", "tease_state") ?: "—"),
                    "RELEASE" to (obj?.str("release", "release_state") ?: "—"),
                ),
            )
            Spacer(Modifier.height(8.dp))
            Text(LegalCopy.STAGING_NE_CANON, style = MonoStyle.copy(color = Muted, fontSize = 10.sp))
            Text(LegalCopy.TEASE_NE_REVEAL, style = MonoStyle.copy(color = Muted, fontSize = 10.sp))
            Text(LegalCopy.SEAL_NE_PUBLICATION, style = MonoStyle.copy(color = Muted, fontSize = 10.sp))
            SectionLabel("LEGAL ACTIONS")
            if (actions.isEmpty()) {
                UnavailableState(title = "NO ACTIONS", reason = "State ${row.state} has an empty legal action set.")
            }
            actions.forEach { action ->
                Spacer(Modifier.height(8.dp))
                if (action.privileged) {
                    DangerButton(action.label) { vm.run(action) }
                } else {
                    SteelButton(action.label) { vm.run(action) }
                }
            }
            if (LegalAction.EDIT in actions) {
                SectionLabel("EDIT BODY")
                VesperField(state.editTitle, vm::onEditTitle, placeholder = "title")
                Spacer(Modifier.height(8.dp))
                VesperField(state.editContent, vm::onEditContent, placeholder = "content", singleLine = false)
            }
            if (LegalAction.SCHEDULE in actions) {
                SectionLabel("SCHEDULE SEAL")
                Text("operation is SEAL only. Cannot schedule PUBLISH or REVEAL.", style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
                Spacer(Modifier.height(8.dp))
                VesperField(state.scheduleAt, vm::onScheduleAt, placeholder = "run_at ISO-8601", mono = true)
            }
        }
        if (confirm.need != null) {
            Spacer(Modifier.height(16.dp))
            ConfirmSheet(
                op = confirm.need.op,
                detail = confirm.need.detail,
                passphrase = confirm.passphrase,
                onPassphrase = vm::onConfirmPass,
                busy = confirm.busy,
                error = confirm.error,
                onCancel = vm::cancelConfirm,
                onConfirm = vm::submitConfirm,
            )
        }
        SectionLabel("DIFF")
        JsonPreview(state.diff ?: "no diff")
        SectionLabel("RAW")
        JsonPreview(state.envelope?.raw ?: "—")
    }
}
