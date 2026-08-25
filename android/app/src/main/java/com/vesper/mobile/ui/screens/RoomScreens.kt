package com.vesper.mobile.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vesper.mobile.data.mortis.AuditQuery
import com.vesper.mobile.data.mortis.str
import com.vesper.mobile.domain.LegalCopy
import com.vesper.mobile.domain.ReleaseRules
import com.vesper.mobile.ui.AppStateViewModel
import com.vesper.mobile.ui.components.ConfirmSheet
import com.vesper.mobile.ui.components.DangerButton
import com.vesper.mobile.ui.components.ErrorLine
import com.vesper.mobile.ui.components.FilterChip
import com.vesper.mobile.ui.components.GhostButton
import com.vesper.mobile.ui.components.JsonPreview
import com.vesper.mobile.ui.components.KeyValue
import com.vesper.mobile.ui.components.MonoId
import com.vesper.mobile.ui.components.Panel
import com.vesper.mobile.ui.components.SectionLabel
import com.vesper.mobile.ui.components.SecureWindow
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
import kotlinx.serialization.json.JsonObject

@Composable
fun ReleaseCenterScreen(
    vm: ReleaseViewModel,
    appVm: AppStateViewModel,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val app by appVm.state.collectAsStateWithLifecycle()
    VesperScaffold(
        title = "RELEASE CENTER",
        onBack = onBack,
        banner = app.surface.offlineBanner,
        actions = { Text("REFRESH", style = LabelStyle, modifier = Modifier.clickable { vm.refresh() }) },
    ) {
        SecureWindow()
        Text(LegalCopy.SIGNING_KEY_OFF_DEVICE, style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
        Text(LegalCopy.WINDOWS_NO_BINARY, style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
        Text(LegalCopy.ANDROID_PLAYER_NO_BINARY, style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
        Text(LegalCopy.DATASET_NE_BINARY, style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
        SectionLabel("SIX-STEP HANDOFF")
        ReleaseRules.handoffSteps.forEachIndexed { i, step ->
            Text("${i + 1}. $step", style = MonoStyle.copy(color = Parchment, fontSize = 12.sp), modifier = Modifier.padding(vertical = 2.dp))
        }
        ErrorLine(state.load.error)
        ErrorLine(state.notice)
        Spacer(Modifier.height(12.dp))
        SteelButton("GENERATE CANDIDATE", onClick = vm::generate)
        Spacer(Modifier.height(8.dp))
        SteelButton("LEAK SCAN", onClick = vm::leakscan)
        Spacer(Modifier.height(8.dp))
        SteelButton("EXPORT UNSIGNED", onClick = vm::exportUnsigned)
        SectionLabel("IMPORT SIGNED")
        Text("Paste the signed payload JSON returned from the PC signing step.", style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
        Spacer(Modifier.height(8.dp))
        VesperField(state.importBody, vm::onImport, placeholder = "{ signed payload }", singleLine = false, mono = true)
        Spacer(Modifier.height(8.dp))
        DangerButton("IMPORT SIGNED", onClick = vm::importSigned)
        SectionLabel("PUBLISH")
        Text("confirm_phrase is PUBLISH. Requires step-up ${ReleaseRules.PUBLISH_CONFIRM}.", style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("DEV", "TEST", "PREVIEW", "STABLE").forEach { ch ->
                FilterChip(ch, state.targetChannel == ch) { vm.onChannel(ch) }
            }
        }
        Spacer(Modifier.height(8.dp))
        DangerButton("PUBLISH ${state.targetChannel}", onClick = vm::publish)
        state.confirm.need?.let { need ->
            Spacer(Modifier.height(16.dp))
            ConfirmSheet(
                op = need.op,
                detail = need.detail,
                passphrase = state.confirm.passphrase,
                onPassphrase = vm::onConfirmPass,
                busy = state.confirm.busy,
                error = state.confirm.error,
                onCancel = vm::cancelConfirm,
                onConfirm = vm::submitConfirm,
            )
        }
        SectionLabel("CANDIDATE")
        JsonPreview(state.candidate ?: "unreadable")
        SectionLabel("DIFF")
        JsonPreview(state.diff ?: "unreadable")
        SectionLabel("RELEASES")
        JsonPreview(state.releases ?: "unreadable")
    }
}

@Composable
fun ApplicationsScreen(
    vm: ApplicationsViewModel,
    appVm: AppStateViewModel,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val app by appVm.state.collectAsStateWithLifecycle()
    VesperScaffold(
        title = "APPLICATIONS",
        onBack = onBack,
        banner = app.surface.offlineBanner,
        actions = { Text("REFRESH", style = LabelStyle, modifier = Modifier.clickable { vm.refresh() }) },
    ) {
        Text(LegalCopy.DM_NOT_PLAYER, style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
        Text("Player update detection uses admin /api/applications. RELEASE_TOKEN is not in this APK.", style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
        ErrorLine(state.load.error)
        ErrorLine(state.notice)
        SectionLabel("LIVE APPLICATIONS")
        if (state.items.isEmpty() && !state.load.loading) {
            UnavailableState(title = "NONE REPORTED", reason = "Worker returned no application items.")
        }
        state.items.forEach { obj ->
            Panel {
                MonoId(obj.str("id", "application_id") ?: "—")
                KeyValue("name", obj.str("name", "title", "slug") ?: "—")
                KeyValue("channel", obj.str("channel", "current_channel") ?: "—")
            }
            Spacer(Modifier.height(8.dp))
        }
        SectionLabel("ASSIGN")
        VesperField(state.applicationId, vm::onApp, placeholder = "application_id", mono = true)
        Spacer(Modifier.height(8.dp))
        VesperField(state.artifactId, vm::onArtifact, placeholder = "artifact_id (optional)", mono = true)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("DEV", "TEST", "PREVIEW", "STABLE").forEach { ch ->
                FilterChip(ch, state.channel == ch) { vm.onChannel(ch) }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (state.channel == "STABLE") {
            DangerButton("ASSIGN STABLE (confirm channel.assign)", onClick = vm::assign)
        } else {
            SteelButton("ASSIGN ${state.channel}", onClick = vm::assign)
        }
        SectionLabel("PROMOTE")
        Text("Always requires confirm channel.promote.", style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
        Spacer(Modifier.height(8.dp))
        VesperField(state.fromChannel, vm::onFrom, placeholder = "from_channel", mono = true)
        Spacer(Modifier.height(8.dp))
        VesperField(state.toChannel, vm::onTo, placeholder = "to_channel", mono = true)
        Spacer(Modifier.height(8.dp))
        DangerButton("PROMOTE", onClick = vm::promote)
        SectionLabel("SCHEDULE CHANNEL")
        Text("DEV | TEST | PREVIEW only. Never STABLE.", style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
        Spacer(Modifier.height(8.dp))
        VesperField(state.runAt, vm::onRunAt, placeholder = "run_at ISO-8601", mono = true)
        Spacer(Modifier.height(8.dp))
        SteelButton("SCHEDULE ${state.channel}", enabled = state.channel != "STABLE", onClick = vm::schedule)
        state.confirm.need?.let { need ->
            Spacer(Modifier.height(16.dp))
            ConfirmSheet(
                op = need.op,
                detail = need.detail,
                passphrase = state.confirm.passphrase,
                onPassphrase = vm::onConfirmPass,
                busy = state.confirm.busy,
                error = state.confirm.error,
                onCancel = vm::cancelConfirm,
                onConfirm = vm::submitConfirm,
            )
        }
        SectionLabel("RAW")
        JsonPreview(state.raw ?: "—")
    }
}

@Composable
fun AuditScreen(
    vm: AuditViewModel,
    appVm: AppStateViewModel,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val app by appVm.state.collectAsStateWithLifecycle()
    var q by remember { mutableStateOf(state.query.q) }
    var actor by remember { mutableStateOf(state.query.actor) }
    var action by remember { mutableStateOf(state.query.action) }
    var target by remember { mutableStateOf(state.query.target) }
    var result by remember { mutableStateOf(state.query.result) }
    VesperScaffold(
        title = "AUDIT",
        subtitle = "${state.rows.size}/${state.total}",
        onBack = onBack,
        banner = app.surface.offlineBanner,
    ) {
        VesperField(q, { q = it }, placeholder = "q")
        Spacer(Modifier.height(8.dp))
        VesperField(actor, { actor = it }, placeholder = "actor", mono = true)
        Spacer(Modifier.height(8.dp))
        VesperField(action, { action = it }, placeholder = "action")
        Spacer(Modifier.height(8.dp))
        VesperField(target, { target = it }, placeholder = "target")
        Spacer(Modifier.height(8.dp))
        VesperField(result, { result = it }, placeholder = "result")
        Spacer(Modifier.height(8.dp))
        GhostButton("APPLY") {
            vm.apply(AuditQuery(q = q, actor = actor, action = action, target = target, result = result, limit = 40, offset = 0))
        }
        ErrorLine(state.load.error)
        SectionLabel("RECORDS")
        if (state.rows.isEmpty() && !state.load.loading) {
            UnavailableState(title = "EMPTY", reason = "No audit rows for this filter.")
        }
        state.rows.forEach { obj ->
            AuditRow(obj)
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("PREV", compact = true, enabled = state.query.offset > 0) { vm.page(-state.query.limit) }
            GhostButton("NEXT", compact = true, enabled = state.query.offset + state.rows.size < state.total) {
                vm.page(state.query.limit)
            }
        }
    }
}

@Composable
private fun AuditRow(obj: JsonObject) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Hairline)
            .background(Elevated)
            .padding(12.dp),
    ) {
        Text(obj.str("action", "op", "event") ?: "—", style = LabelStyle.copy(color = Steel, fontSize = 10.sp))
        MonoId(obj.str("id", "audit_id") ?: "")
        KeyValue("actor", obj.str("actor", "operator", "operator_id") ?: "—")
        KeyValue("target", obj.str("target", "target_id") ?: "—")
        KeyValue("result", obj.str("result", "status") ?: "—")
        KeyValue("at", obj.str("at", "created_at", "time") ?: "—")
    }
}

@Composable
fun ScheduleScreen(
    vm: ScheduleViewModel,
    appVm: AppStateViewModel,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val app by appVm.state.collectAsStateWithLifecycle()
    val tabs = listOf("UPCOMING", "ACTIVE", "COMPLETED", "CANCELLED")
    VesperScaffold(
        title = "SCHEDULE",
        onBack = onBack,
        banner = app.surface.offlineBanner,
        actions = { Text("REFRESH", style = LabelStyle, modifier = Modifier.clickable { vm.refresh() }) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            tabs.forEach { t -> FilterChip(t, state.tab == t) { vm.tab(t) } }
        }
        ErrorLine(state.load.error)
        ErrorLine(state.notice)
        val filtered = state.items.filter { obj ->
            val st = (obj.str("state", "status", "bucket") ?: "").uppercase()
            st.contains(state.tab) || st.isBlank()
        }
        SectionLabel(state.tab)
        if (filtered.isEmpty() && !state.load.loading) {
            UnavailableState(title = "NONE", reason = "No schedule rows in ${state.tab} from the live worker payload.")
        }
        filtered.forEach { obj ->
            val id = obj.str("id", "schedule_id") ?: ""
            Panel {
                MonoId(id)
                KeyValue("state", obj.str("state", "status") ?: "—")
                KeyValue("operation", obj.str("operation", "op") ?: "—")
                KeyValue("run_at", obj.str("run_at", "at") ?: "—")
                if (id.isNotBlank() && (obj.str("state", "status")?.uppercase() in setOf("UPCOMING", "SCHEDULED", "ACTIVE", null))) {
                    Spacer(Modifier.height(8.dp))
                    GhostButton("CANCEL") { vm.cancel(id) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        SectionLabel("RAW")
        JsonPreview(state.raw ?: "—")
    }
}

@Composable
fun DiscoveryScreen(
    vm: DiscoveryViewModel,
    appVm: AppStateViewModel,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val app by appVm.state.collectAsStateWithLifecycle()
    VesperScaffold(
        title = "DISCOVERY",
        subtitle = "tease ≠ reveal",
        onBack = onBack,
        banner = app.surface.offlineBanner,
        actions = { Text("REFRESH", style = LabelStyle, modifier = Modifier.clickable { vm.refresh() }) },
    ) {
        Text(LegalCopy.TEASE_NE_REVEAL, style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
        Text(LegalCopy.TRIGGER_NE_REVEAL, style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
        ErrorLine(state.load.error)
        SectionLabel("FRAGMENTS")
        VesperField(state.q, vm::onQ, placeholder = "q")
        Spacer(Modifier.height(8.dp))
        GhostButton("SEARCH") { vm.refresh() }
        Spacer(Modifier.height(12.dp))
        JsonPreview(state.fragments ?: "unreadable")
        SectionLabel("DISCOVERY")
        JsonPreview(state.discovery ?: "unreadable")
    }
}

@Composable
fun DriveIntakeScreen(
    vm: DriveIntakeViewModel,
    appVm: AppStateViewModel,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val app by appVm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) vm.ingestUri(uri, context.contentResolver)
    }
    VesperScaffold(
        title = "DRIVE INTAKE",
        onBack = onBack,
        banner = app.surface.offlineBanner,
        actions = { Text("REFRESH", style = LabelStyle, modifier = Modifier.clickable { vm.refresh() }) },
    ) {
        if (!state.implemented) {
            UnavailableState(title = "NOT_IMPLEMENTED", reason = state.reason)
        } else {
            Text(state.reason, style = MonoStyle.copy(color = Steel, fontSize = 12.sp))
        }
        ErrorLine(state.load.error)
        ErrorLine(state.notice)
        SectionLabel("MANUAL PUSH")
        Text("/api/intake/push — local file, not Google Drive.", style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
        Spacer(Modifier.height(8.dp))
        GhostButton("PICK FILE") { picker.launch(arrayOf("*/*")) }
        Spacer(Modifier.height(8.dp))
        VesperField(state.filename, vm::onFilename, placeholder = "filename", mono = true)
        Spacer(Modifier.height(8.dp))
        VesperField(state.content, vm::onContent, placeholder = "content", singleLine = false, mono = true)
        Spacer(Modifier.height(8.dp))
        SteelButton("PUSH INTAKE", onClick = vm::push)
        SectionLabel("STATUS RAW")
        JsonPreview(state.statusRaw ?: "—")
    }
}

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
    onNotifications: () -> Unit,
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    var host by remember(s.mortisHost) { mutableStateOf(s.mortisHost) }
    var admin by remember(s.adminPathSeg) { mutableStateOf(s.adminPathSeg) }
    var operator by remember(s.operatorId) { mutableStateOf(s.operatorId) }
    var endpoint by remember(s.vesperEndpoint) { mutableStateOf(s.vesperEndpoint) }
    VesperScaffold(title = "SETTINGS", onBack = onBack) {
        SecureWindow()
        ErrorLine(notice)
        SectionLabel("MORTIS HOST")
        VesperField(host, { host = it }, placeholder = "https://…", mono = true)
        SectionLabel("ADMIN PATH SEGMENT")
        Text("Worker secret. Not shipped in the APK. Paste only on this device.", style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
        Spacer(Modifier.height(8.dp))
        VesperField(admin, { admin = it }, placeholder = "path segment", secret = true, mono = true)
        SectionLabel("OPERATOR ID")
        VesperField(operator, { operator = it }, placeholder = "operator_id", mono = true)
        SectionLabel("VESPER ENDPOINT")
        Text("Remote provider only. Leave blank if the PC core is the source of truth.", style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
        Spacer(Modifier.height(8.dp))
        VesperField(endpoint, { endpoint = it }, placeholder = "https://vesper-host (optional)", mono = true)
        Spacer(Modifier.height(16.dp))
        SteelButton("SAVE CONNECTION") {
            vm.update {
                it.copy(
                    mortisHost = host.trim().trimEnd('/'),
                    adminPathSeg = admin.trim().trim('/'),
                    operatorId = operator.trim(),
                    vesperEndpoint = endpoint.trim().trimEnd('/'),
                )
            }
        }
        SectionLabel("PRESENCE")
        Text(
            if (s.presenceEnabled) "Preference ON. VesperPresenceProvider is still UNAVAILABLE on Android."
            else "Preference OFF. Presence is not implemented on this client.",
            style = MonoStyle.copy(color = Muted, fontSize = 11.sp),
        )
        Spacer(Modifier.height(8.dp))
        GhostButton(if (s.presenceEnabled) "DISABLE PRESENCE PREFERENCE" else "ENABLE PRESENCE PREFERENCE") {
            vm.update { it.copy(presenceEnabled = !it.presenceEnabled) }
        }
        Spacer(Modifier.height(16.dp))
        SteelButton("NOTIFICATIONS", onClick = onNotifications)
    }
}

@Composable
fun NotificationsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
) {
    val s by vm.state.collectAsStateWithLifecycle()
    VesperScaffold(title = "NOTIFICATIONS", onBack = onBack) {
        Text("Channels: intake, review, release, signing, system.", style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
        Spacer(Modifier.height(16.dp))
        PrefToggle("INTAKE", s.notifyIntake) { vm.update { it.copy(notifyIntake = !it.notifyIntake) } }
        PrefToggle("REVIEW", s.notifyReview) { vm.update { it.copy(notifyReview = !it.notifyReview) } }
        PrefToggle("RELEASE", s.notifyRelease) { vm.update { it.copy(notifyRelease = !it.notifyRelease) } }
        PrefToggle("SIGNING", s.notifySigning) { vm.update { it.copy(notifySigning = !it.notifySigning) } }
        PrefToggle("SYSTEM", s.notifySystem) { vm.update { it.copy(notifySystem = !it.notifySystem) } }
        Spacer(Modifier.height(12.dp))
        PrefToggle("HEALTH POLL (15m)", s.healthPoll) { vm.update { it.copy(healthPoll = !it.healthPoll) } }
        Text(
            "Health poll uses WorkManager against GET /v1/health. Disabled unless you turn it on.",
            style = MonoStyle.copy(color = Muted, fontSize = 11.sp),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun PrefToggle(label: String, on: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, Hairline)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = LabelStyle.copy(color = Parchment))
        Text(if (on) "ON" else "OFF", style = LabelStyle.copy(color = if (on) Steel else Muted))
    }
}

@Composable
fun MoreScreen(
    appVm: AppStateViewModel,
    onBack: (() -> Unit)? = null,
    onSettings: () -> Unit,
    onNotifications: () -> Unit,
    onActivity: () -> Unit,
    onVoice: () -> Unit,
    onAudit: () -> Unit,
    onLogout: () -> Unit,
) {
    val app by appVm.state.collectAsStateWithLifecycle()
    val s = app.surface
    VesperScaffold(
        title = "MORE",
        subtitle = "device settings · not authority",
        onBack = onBack,
        banner = s.offlineBanner,
    ) {
        Text(
            text = "Personal settings stay on device. Mortis authority stays on Cloudflare.",
            style = MonoStyle.copy(color = Muted, fontSize = 11.sp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Link ${if (s.network) "up" else "down"} · Mortis ${s.mortis.label} · Operator ${s.operator.label}",
            style = MonoStyle.copy(color = Muted, fontSize = 11.sp),
        )
        SectionLabel("SURFACE")
        MoreRow("SETTINGS", "Host, admin path, presence") { onSettings() }
        MoreRow("NOTIFICATIONS", "Channels and preferences") { onNotifications() }
        MoreRow("ACTIVITY", "Recent local events") { onActivity() }
        MoreRow("VOICE", "Architecture only. UNAVAILABLE.") { onVoice() }
        MoreRow("AUDIT", "Mortis append-only trail") { onAudit() }
        if (s.operator.unlocked) {
            Spacer(Modifier.height(20.dp))
            DangerButton("END OPERATOR SESSION", onClick = onLogout)
        }
    }
}

@Composable
private fun MoreRow(label: String, note: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, Hairline)
            .background(Elevated)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(label, style = LabelStyle.copy(color = Parchment))
        Spacer(Modifier.height(4.dp))
        Text(note, style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
    }
}

@Composable
fun ActivityScreen(
    vm: ChatViewModel,
    appVm: AppStateViewModel,
    onBack: (() -> Unit)? = null,
    onChat: () -> Unit,
    onInbox: () -> Unit,
    onRelease: () -> Unit,
    onApps: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val app by appVm.state.collectAsStateWithLifecycle()
    val s = app.surface
    val recent = state.turns.filter { it.role != "system" }.takeLast(8).asReversed()
    VesperScaffold(
        title = "ACTIVITY",
        subtitle = "device-side log · not Mortis audit",
        onBack = onBack,
        banner = s.offlineBanner,
    ) {
        StatusRow("MORTIS", s.mortis.label, s.mortis.detail)
        StatusRow("OPERATOR", s.operator.label, s.operator.detail)
        SectionLabel("RECENT CONVERSATION")
        if (recent.isEmpty()) {
            UnavailableState(title = "NONE", reason = "No conversation on this device yet.")
        }
        recent.forEach { turn ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, Hairline)
                    .background(Elevated)
                    .clickable(onClick = onChat)
                    .padding(12.dp),
            ) {
                Text(
                    text = "${turn.role} · ${turn.atEpochMs}",
                    style = MonoStyle.copy(color = Muted, fontSize = 10.sp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = turn.text.ifBlank { turn.error ?: "—" }.take(160),
                    style = MonoStyle.copy(color = Parchment, fontSize = 12.sp),
                )
            }
        }
        SectionLabel("QUEUES")
        GhostButton("INBOX", enabled = s.operator.unlocked, onClick = onInbox)
        Spacer(Modifier.height(8.dp))
        GhostButton("RELEASE", enabled = s.operator.unlocked, onClick = onRelease)
        Spacer(Modifier.height(8.dp))
        GhostButton("APPLICATIONS", enabled = s.operator.unlocked, onClick = onApps)
        Spacer(Modifier.height(8.dp))
        GhostButton("TALK", onClick = onChat)
    }
}

@Composable
fun VoiceScreen(onBack: (() -> Unit)? = null) {
    VesperScaffold(title = "VOICE", subtitle = "architecture only", onBack = onBack) {
        UnavailableState(
            title = "VOICE UNAVAILABLE",
            reason = "Speech-to-text is not bound on this client. This screen does not invent a recognizer, transcript, or wake word. RECORD_AUDIO is not requested.",
        )
        Spacer(Modifier.height(16.dp))
        StatusRow("STT", "UNAVAILABLE", "No speech engine is packaged in this APK.")
        StatusRow("TTS", "UNAVAILABLE", "Speech output is a future device binding, not a PC-core claim.")
        StatusRow("MIC", "NOT REQUESTED", "Microphone permission is not declared until a real STT engine is bound.")
        StatusRow("PTT", "PREPARED", "Push-to-talk remains architecture. Chat MIC control stays disabled.")
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Native Android will use platform speech + notification channels when a real engine is bound. Until then this surface stays honest.",
            style = MonoStyle.copy(color = Muted, fontSize = 11.sp),
        )
    }
}

