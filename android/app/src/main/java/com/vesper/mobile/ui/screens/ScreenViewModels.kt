package com.vesper.mobile.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vesper.mobile.AppContainer
import com.vesper.mobile.data.mortis.AuditQuery
import com.vesper.mobile.data.mortis.ChannelAssignBody
import com.vesper.mobile.data.mortis.ChannelPromoteBody
import com.vesper.mobile.data.mortis.ChannelScheduleBody
import com.vesper.mobile.data.mortis.ConfirmNeed
import com.vesper.mobile.data.mortis.DashboardSnapshot
import com.vesper.mobile.data.mortis.EditBody
import com.vesper.mobile.data.mortis.InboxQuery
import com.vesper.mobile.data.mortis.IntakeFile
import com.vesper.mobile.data.mortis.MortisEnvelope
import com.vesper.mobile.data.mortis.MortisResult
import com.vesper.mobile.data.mortis.ScheduleSealBody
import com.vesper.mobile.data.mortis.StagingRow
import com.vesper.mobile.data.settings.VesperSettings
import com.vesper.mobile.data.vesper.ChatTurn
import com.vesper.mobile.data.vesper.CompletionRequest
import com.vesper.mobile.data.vesper.CompletionResult
import com.vesper.mobile.data.vesper.ProviderAvailability
import com.vesper.mobile.domain.ChannelKind
import com.vesper.mobile.domain.ChannelRules
import com.vesper.mobile.domain.LegalAction
import com.vesper.mobile.domain.ReleaseRules
import com.vesper.mobile.domain.Transitions
import com.vesper.mobile.notify.HealthCheckWorker
import com.vesper.mobile.notify.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import java.util.Base64

data class LoadState(
    val loading: Boolean = false,
    val error: String? = null,
)

data class ConfirmState(
    val need: ConfirmNeed? = null,
    val passphrase: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val retry: (suspend (String) -> MortisResult<*>)? = null,
)

private fun MortisResult<*>.asError(): String? = detailOrNull()

class ChatViewModel(private val c: AppContainer) : ViewModel() {
    data class State(
        val turns: List<ChatTurn> = emptyList(),
        val draft: String = "",
        val sending: Boolean = false,
        val availability: String = "Checking.",
        val available: Boolean = false,
        val error: String? = null,
        val voiceReason: String = "Voice engine is not on this device. RECORD_AUDIO is not requested until it is.",
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val turns = c.chat.load()
            _state.update { it.copy(turns = turns) }
            probe()
        }
    }

    fun onDraft(v: String) = _state.update { it.copy(draft = v) }

    fun probe() {
        viewModelScope.launch {
            val avail = c.vesper.firstAvailable()
            val line = c.vesper.statusLine()
            _state.update {
                it.copy(
                    available = avail != null,
                    availability = if (avail != null) "${avail.displayName} ready." else line.second,
                )
            }
        }
    }

    fun send() {
        val text = _state.value.draft.trim()
        if (text.isBlank() || _state.value.sending) return
        viewModelScope.launch {
            val user = ChatTurn(c.chat.newId(), "user", text, System.currentTimeMillis())
            val pending = ChatTurn(c.chat.newId(), "assistant", "", System.currentTimeMillis(), status = "pending")
            _state.update {
                it.copy(draft = "", sending = true, error = null, turns = it.turns + user + pending)
            }
            persist()
            complete(pending.id, text)
        }
    }

    fun retry(id: String) {
        val failed = _state.value.turns.firstOrNull { it.id == id } ?: return
        val prior = _state.value.turns.takeWhile { it.id != id }.lastOrNull { it.role == "user" } ?: return
        viewModelScope.launch {
            _state.update { s ->
                s.copy(
                    sending = true,
                    turns = s.turns.map {
                        if (it.id == id) it.copy(status = "pending", error = null, text = "") else it
                    },
                )
            }
            complete(failed.id, prior.text)
        }
    }

    fun clear() {
        viewModelScope.launch {
            c.chat.clear()
            _state.update { it.copy(turns = emptyList()) }
        }
    }

    private suspend fun complete(assistantId: String, userText: String) {
        val provider = c.vesper.firstAvailable()
        val result = if (provider == null) {
            CompletionResult.Failed(_state.value.availability)
        } else {
            provider.complete(CompletionRequest(userText, _state.value.turns))
        }
        _state.update { s ->
            s.copy(
                sending = false,
                turns = s.turns.map { t ->
                    if (t.id != assistantId) t else when (result) {
                        is CompletionResult.Text -> t.copy(text = result.text, status = "ok", error = null)
                        is CompletionResult.Failed -> t.copy(
                            text = "",
                            status = "error",
                            error = result.reason,
                        )
                    }
                },
            )
        }
        persist()
    }

    private suspend fun persist() {
        c.chat.save(_state.value.turns)
    }
}

class UnlockViewModel(private val c: AppContainer) : ViewModel() {
    data class State(
        val passphrase: String = "",
        val operatorId: String = "",
        val busy: Boolean = false,
        val error: String? = null,
        val unlocked: Boolean = false,
        val host: String = "",
        val adminSet: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val s = c.settings.snapshot()
            _state.update {
                it.copy(
                    operatorId = s.operatorId,
                    host = s.mortisHost,
                    adminSet = s.adminPathSeg.isNotBlank(),
                    unlocked = c.session.isUnlocked(),
                )
            }
        }
    }

    fun onPass(v: String) = _state.update { it.copy(passphrase = v, error = null) }
    fun onOperator(v: String) = _state.update { it.copy(operatorId = v) }

    fun unlock() {
        viewModelScope.launch {
            val s = _state.value
            if (!s.adminSet) {
                _state.update { it.copy(error = "Set the admin path segment in Settings first.") }
                return@launch
            }
            _state.update { it.copy(busy = true, error = null) }
            if (s.operatorId.isNotBlank()) {
                c.settings.update { it.copy(operatorId = s.operatorId) }
            }
            when (val r = c.mortis.unlock(s.passphrase, s.operatorId)) {
                is MortisResult.Ok -> _state.update {
                    it.copy(busy = false, unlocked = true, passphrase = "", error = null)
                }
                else -> _state.update {
                    it.copy(busy = false, unlocked = false, passphrase = "", error = r.asError())
                }
            }
        }
    }
}

class OperatorHomeViewModel(private val c: AppContainer) : ViewModel() {
    data class State(
        val load: LoadState = LoadState(loading = true),
        val dash: DashboardSnapshot? = null,
        val statusRaw: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(load = LoadState(true)) }
            when (val d = c.mortis.dashboard()) {
                is MortisResult.Ok -> {
                    val st = c.mortis.status()
                    _state.update {
                        it.copy(
                            load = LoadState(false),
                            dash = d.value,
                            statusRaw = (st as? MortisResult.Ok)?.value?.raw?.take(4000),
                        )
                    }
                }
                else -> _state.update { it.copy(load = LoadState(false, d.asError())) }
            }
        }
    }
}

class InboxViewModel(private val c: AppContainer) : ViewModel() {
    data class State(
        val load: LoadState = LoadState(),
        val query: InboxQuery = InboxQuery(),
        val rows: List<StagingRow> = emptyList(),
        val total: Int = 0,
        val tab: String = "inbox",
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refresh()
    }

    fun onQuery(q: InboxQuery) = _state.update { it.copy(query = q) }
    fun onTab(tab: String) {
        _state.update { it.copy(tab = tab, query = it.query.copy(offset = 0)) }
        refresh()
    }

    fun applyFilters(q: InboxQuery) {
        _state.update { it.copy(query = q.copy(offset = 0)) }
        refresh()
    }

    fun page(delta: Int) {
        val q = _state.value.query
        val next = (q.offset + delta).coerceAtLeast(0)
        _state.update { it.copy(query = q.copy(offset = next)) }
        refresh()
    }

    fun sync() {
        viewModelScope.launch {
            _state.update { it.copy(load = LoadState(true)) }
            val r = c.mortis.inboxSync()
            if (r is MortisResult.Ok) refresh() else _state.update { it.copy(load = LoadState(false, r.asError())) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(load = LoadState(true)) }
            val r = if (_state.value.tab == "staging") {
                c.mortis.staging(_state.value.query)
            } else {
                c.mortis.inbox(_state.value.query)
            }
            when (r) {
                is MortisResult.Ok -> _state.update {
                    it.copy(
                        load = LoadState(false),
                        rows = r.value.items.map { o -> StagingRow.from(o) },
                        total = r.value.total,
                    )
                }
                else -> _state.update { it.copy(load = LoadState(false, r.asError())) }
            }
        }
    }
}

class ProposalViewModel(private val c: AppContainer) : ViewModel() {
    data class State(
        val id: String = "",
        val load: LoadState = LoadState(),
        val envelope: MortisEnvelope? = null,
        val diff: String? = null,
        val row: StagingRow? = null,
        val confirm: ConfirmState = ConfirmState(),
        val editContent: String = "",
        val editTitle: String = "",
        val scheduleAt: String = "",
        val notice: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun load(id: String) {
        if (id.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(id = id, load = LoadState(true), notice = null) }
            when (val r = c.mortis.stagingDetail(id)) {
                is MortisResult.Ok -> {
                    val row = r.value.obj?.let { StagingRow.from(it) }
                    val content = r.value.obj?.get("content")?.toString()?.trim('"') ?: ""
                    val title = row?.title.orEmpty()
                    val diff = when (val d = c.mortis.stagingDiff(id)) {
                        is MortisResult.Ok -> d.value.raw
                        else -> d.asError()
                    }
                    _state.update {
                        it.copy(
                            load = LoadState(false),
                            envelope = r.value,
                            row = row,
                            diff = diff,
                            editContent = content,
                            editTitle = title,
                        )
                    }
                }
                else -> _state.update { it.copy(load = LoadState(false, r.asError())) }
            }
        }
    }

    fun onEditContent(v: String) = _state.update { it.copy(editContent = v) }
    fun onEditTitle(v: String) = _state.update { it.copy(editTitle = v) }
    fun onScheduleAt(v: String) = _state.update { it.copy(scheduleAt = v) }
    fun onConfirmPass(v: String) = _state.update { it.copy(confirm = it.confirm.copy(passphrase = v, error = null)) }
    fun cancelConfirm() = _state.update { it.copy(confirm = ConfirmState()) }

    fun run(action: LegalAction) {
        viewModelScope.launch {
            val id = _state.value.id
            val confirmOp = Transitions.confirmOp(action)
            val body = when (action) {
                LegalAction.EDIT -> c.json.encodeToString(
                    EditBody(_state.value.editContent, _state.value.editTitle.ifBlank { null }),
                )
                LegalAction.SCHEDULE -> {
                    val at = _state.value.scheduleAt.trim()
                    if (at.isBlank()) {
                        _state.update { it.copy(notice = "Schedule requires run_at ISO-8601. Operation is SEAL only.") }
                        return@launch
                    }
                    c.json.encodeToString(ScheduleSealBody(run_at = at, operation = "SEAL"))
                }
                else -> "{}"
            }
            val send: suspend (String?) -> MortisResult<MortisEnvelope> = { token ->
                c.mortis.stagingAction(id, action.path, body, token)
            }
            dispatch(confirmOp, send)
        }
    }

    fun submitConfirm() {
        val confirm = _state.value.confirm
        val need = confirm.need ?: return
        val retry = confirm.retry ?: return
        viewModelScope.launch {
            _state.update { it.copy(confirm = it.confirm.copy(busy = true, error = null)) }
            when (val step = c.mortis.stepUp(confirm.passphrase, need.op)) {
                is MortisResult.Ok -> {
                    when (val r = retry(step.value)) {
                        is MortisResult.Ok -> {
                            _state.update { it.copy(confirm = ConfirmState(), notice = "Accepted.") }
                            load(_state.value.id)
                        }
                        else -> _state.update {
                            it.copy(confirm = it.confirm.copy(busy = false, passphrase = "", error = r.asError()))
                        }
                    }
                }
                else -> _state.update {
                    it.copy(confirm = it.confirm.copy(busy = false, passphrase = "", error = step.asError()))
                }
            }
        }
    }

    private suspend fun dispatch(
        confirmOp: String?,
        send: suspend (String?) -> MortisResult<MortisEnvelope>,
    ) {
        _state.update { it.copy(load = LoadState(true), notice = null) }
        when (val r = send(null)) {
            is MortisResult.Ok -> {
                _state.update { it.copy(load = LoadState(false), notice = "Accepted.") }
                load(_state.value.id)
            }
            is MortisResult.ConfirmRequired -> {
                _state.update {
                    it.copy(
                        load = LoadState(false),
                        confirm = ConfirmState(
                            need = r.need.copy(op = confirmOp ?: r.need.op),
                            retry = send,
                        ),
                    )
                }
            }
            else -> _state.update { it.copy(load = LoadState(false, r.asError())) }
        }
    }
}

class ReleaseViewModel(private val c: AppContainer) : ViewModel() {
    data class State(
        val load: LoadState = LoadState(),
        val candidate: String? = null,
        val diff: String? = null,
        val releases: String? = null,
        val notice: String? = null,
        val confirm: ConfirmState = ConfirmState(),
        val importBody: String = "",
        val targetChannel: String = "PREVIEW",
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refresh()
    }

    fun onImport(v: String) = _state.update { it.copy(importBody = v) }
    fun onChannel(v: String) = _state.update { it.copy(targetChannel = v) }
    fun onConfirmPass(v: String) = _state.update { it.copy(confirm = it.confirm.copy(passphrase = v)) }
    fun cancelConfirm() = _state.update { it.copy(confirm = ConfirmState()) }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(load = LoadState(true), notice = null) }
            val cand = c.mortis.releaseCandidate()
            val diff = c.mortis.releaseDiff()
            val rel = c.mortis.releases()
            _state.update {
                it.copy(
                    load = LoadState(false, listOf(cand, diff, rel).firstOrNull { r -> r !is MortisResult.Ok }?.asError()),
                    candidate = (cand as? MortisResult.Ok)?.value?.raw,
                    diff = (diff as? MortisResult.Ok)?.value?.raw,
                    releases = (rel as? MortisResult.Ok)?.value?.raw,
                )
            }
        }
    }

    fun generate() = act { c.mortis.releaseGenerate() }
    fun leakscan() = act { c.mortis.releaseLeakscan() }
    fun exportUnsigned() = act {
        val r = c.mortis.releaseExportUnsigned()
        if (r is MortisResult.Ok && c.settings.snapshot().notifySigning) {
            c.notifications.notify(
                NotificationHelper.Channel.SIGNING,
                7201,
                "UNSIGNED EXPORT",
                "Sign on PC. Signing key is not on this device.",
            )
        }
        r
    }

    fun importSigned() {
        val body = _state.value.importBody.ifBlank { "{}" }
        confirmThen(ReleaseRules.IMPORT_CONFIRM) { token -> c.mortis.releaseImportSigned(body, token) }
    }

    fun publish() {
        val ch = _state.value.targetChannel
        confirmThen(ReleaseRules.PUBLISH_CONFIRM) { token -> c.mortis.releasePublish(ch, token) }
    }

    fun submitConfirm() {
        val confirm = _state.value.confirm
        val need = confirm.need ?: return
        val retry = confirm.retry ?: return
        viewModelScope.launch {
            _state.update { it.copy(confirm = it.confirm.copy(busy = true, error = null)) }
            when (val step = c.mortis.stepUp(confirm.passphrase, need.op)) {
                is MortisResult.Ok -> when (val r = retry(step.value)) {
                    is MortisResult.Ok -> {
                        _state.update { it.copy(confirm = ConfirmState(), notice = "Accepted.") }
                        refresh()
                    }
                    else -> _state.update {
                        it.copy(confirm = it.confirm.copy(busy = false, passphrase = "", error = r.asError()))
                    }
                }
                else -> _state.update {
                    it.copy(confirm = it.confirm.copy(busy = false, passphrase = "", error = step.asError()))
                }
            }
        }
    }

    private fun act(block: suspend () -> MortisResult<MortisEnvelope>) {
        viewModelScope.launch {
            _state.update { it.copy(load = LoadState(true), notice = null) }
            when (val r = block()) {
                is MortisResult.Ok -> {
                    _state.update { it.copy(load = LoadState(false), notice = "Accepted.") }
                    refresh()
                }
                is MortisResult.ConfirmRequired -> _state.update {
                    it.copy(load = LoadState(false), confirm = ConfirmState(need = r.need))
                }
                else -> _state.update { it.copy(load = LoadState(false, r.asError())) }
            }
        }
    }

    private fun confirmThen(op: String, send: suspend (String) -> MortisResult<MortisEnvelope>) {
        viewModelScope.launch {
            when (val r = send("")) {
                is MortisResult.Ok -> {
                    _state.update { it.copy(notice = "Accepted.") }
                    refresh()
                }
                is MortisResult.ConfirmRequired -> _state.update {
                    it.copy(confirm = ConfirmState(need = r.need.copy(op = op), retry = { t -> send(t) }))
                }
                is MortisResult.HttpError, is MortisResult.Unauthorized, is MortisResult.NetworkError,
                is MortisResult.Misconfigured, is MortisResult.Locked,
                -> {
                    _state.update {
                        it.copy(confirm = ConfirmState(need = ConfirmNeed(op, r.asError()), retry = { t -> send(t) }))
                    }
                }
                else -> _state.update { it.copy(load = LoadState(false, r.asError())) }
            }
        }
    }
}

class ApplicationsViewModel(private val c: AppContainer) : ViewModel() {
    data class State(
        val load: LoadState = LoadState(),
        val raw: String? = null,
        val items: List<JsonObject> = emptyList(),
        val applicationId: String = "",
        val artifactId: String = "",
        val channel: String = "DEV",
        val fromChannel: String = "DEV",
        val toChannel: String = "TEST",
        val runAt: String = "",
        val notice: String? = null,
        val confirm: ConfirmState = ConfirmState(),
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refresh()
    }

    fun onApp(v: String) = _state.update { it.copy(applicationId = v) }
    fun onArtifact(v: String) = _state.update { it.copy(artifactId = v) }
    fun onChannel(v: String) = _state.update { it.copy(channel = v) }
    fun onFrom(v: String) = _state.update { it.copy(fromChannel = v) }
    fun onTo(v: String) = _state.update { it.copy(toChannel = v) }
    fun onRunAt(v: String) = _state.update { it.copy(runAt = v) }
    fun onConfirmPass(v: String) = _state.update { it.copy(confirm = it.confirm.copy(passphrase = v)) }
    fun cancelConfirm() = _state.update { it.copy(confirm = ConfirmState()) }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(load = LoadState(true)) }
            when (val r = c.mortis.applications()) {
                is MortisResult.Ok -> _state.update {
                    it.copy(load = LoadState(false), raw = r.value.raw, items = r.value.items())
                }
                else -> _state.update { it.copy(load = LoadState(false, r.asError())) }
            }
        }
    }

    fun assign() {
        val kind = ChannelKind.parse(_state.value.channel)
        if (kind == null) {
            _state.update { it.copy(notice = "Unknown channel.") }
            return
        }
        val body = ChannelAssignBody(
            application_id = _state.value.applicationId,
            channel = kind.name,
            artifact_id = _state.value.artifactId.ifBlank { null },
        )
        val needs = ChannelRules.assignRequiresConfirm(kind)
        if (needs) {
            confirmThen(ChannelRules.ASSIGN_CONFIRM) { t -> c.mortis.channelsAssign(body, t) }
        } else {
            act { c.mortis.channelsAssign(body, null) }
        }
    }

    fun promote() {
        val body = ChannelPromoteBody(
            application_id = _state.value.applicationId,
            from_channel = _state.value.fromChannel,
            to_channel = _state.value.toChannel,
        )
        confirmThen(ChannelRules.PROMOTE_CONFIRM) { t -> c.mortis.channelsPromote(body, t) }
    }

    fun schedule() {
        val kind = ChannelKind.parse(_state.value.channel) ?: ChannelKind.DEV
        if (!ChannelRules.canSchedule(kind)) {
            _state.update { it.copy(notice = "Cannot schedule STABLE. DEV | TEST | PREVIEW only.") }
            return
        }
        val at = _state.value.runAt.trim()
        if (at.isBlank()) {
            _state.update { it.copy(notice = "run_at ISO-8601 required.") }
            return
        }
        act {
            c.mortis.channelsSchedule(
                ChannelScheduleBody(
                    application_id = _state.value.applicationId,
                    channel = kind.name,
                    run_at = at,
                    artifact_id = _state.value.artifactId.ifBlank { null },
                ),
            )
        }
    }

    fun submitConfirm() {
        val confirm = _state.value.confirm
        val need = confirm.need ?: return
        val retry = confirm.retry ?: return
        viewModelScope.launch {
            _state.update { it.copy(confirm = it.confirm.copy(busy = true, error = null)) }
            when (val step = c.mortis.stepUp(confirm.passphrase, need.op)) {
                is MortisResult.Ok -> when (val r = retry(step.value)) {
                    is MortisResult.Ok -> {
                        _state.update { it.copy(confirm = ConfirmState(), notice = "Accepted.") }
                        refresh()
                    }
                    else -> _state.update {
                        it.copy(confirm = it.confirm.copy(busy = false, passphrase = "", error = r.asError()))
                    }
                }
                else -> _state.update {
                    it.copy(confirm = it.confirm.copy(busy = false, passphrase = "", error = step.asError()))
                }
            }
        }
    }

    private fun act(block: suspend () -> MortisResult<MortisEnvelope>) {
        viewModelScope.launch {
            when (val r = block()) {
                is MortisResult.Ok -> {
                    _state.update { it.copy(notice = "Accepted.") }
                    refresh()
                }
                is MortisResult.ConfirmRequired -> _state.update { it.copy(confirm = ConfirmState(need = r.need)) }
                else -> _state.update { it.copy(notice = r.asError()) }
            }
        }
    }

    private fun confirmThen(op: String, send: suspend (String) -> MortisResult<MortisEnvelope>) {
        viewModelScope.launch {
            _state.update {
                it.copy(confirm = ConfirmState(need = ConfirmNeed(op, "Step-up required for $op."), retry = { t -> send(t) }))
            }
        }
    }
}

class AuditViewModel(private val c: AppContainer) : ViewModel() {
    data class State(
        val load: LoadState = LoadState(),
        val query: AuditQuery = AuditQuery(),
        val rows: List<JsonObject> = emptyList(),
        val total: Int = 0,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refresh()
    }

    fun apply(q: AuditQuery) {
        _state.update { it.copy(query = q.copy(offset = 0)) }
        refresh()
    }

    fun page(delta: Int) {
        val q = _state.value.query
        _state.update { it.copy(query = q.copy(offset = (q.offset + delta).coerceAtLeast(0))) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(load = LoadState(true)) }
            when (val r = c.mortis.audit(_state.value.query)) {
                is MortisResult.Ok -> _state.update {
                    it.copy(load = LoadState(false), rows = r.value.items, total = r.value.total)
                }
                else -> _state.update { it.copy(load = LoadState(false, r.asError())) }
            }
        }
    }
}

class ScheduleViewModel(private val c: AppContainer) : ViewModel() {
    data class State(
        val load: LoadState = LoadState(),
        val tab: String = "UPCOMING",
        val items: List<JsonObject> = emptyList(),
        val raw: String? = null,
        val notice: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refresh()
    }

    fun tab(t: String) = _state.update { it.copy(tab = t) }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(load = LoadState(true)) }
            when (val r = c.mortis.schedule()) {
                is MortisResult.Ok -> _state.update {
                    it.copy(load = LoadState(false), items = r.value.items(), raw = r.value.raw)
                }
                else -> _state.update { it.copy(load = LoadState(false, r.asError())) }
            }
        }
    }

    fun cancel(id: String) {
        viewModelScope.launch {
            when (val r = c.mortis.cancelSchedule(id)) {
                is MortisResult.Ok -> {
                    _state.update { it.copy(notice = "Cancelled $id") }
                    refresh()
                }
                else -> _state.update { it.copy(notice = r.asError()) }
            }
        }
    }
}

class DiscoveryViewModel(private val c: AppContainer) : ViewModel() {
    data class State(
        val load: LoadState = LoadState(),
        val discovery: String? = null,
        val fragments: String? = null,
        val q: String = "",
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refresh()
    }

    fun onQ(v: String) = _state.update { it.copy(q = v) }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(load = LoadState(true)) }
            val d = c.mortis.discovery()
            val f = c.mortis.fragments(_state.value.q)
            _state.update {
                it.copy(
                    load = LoadState(
                        false,
                        listOf(d, f).firstOrNull { r -> r !is MortisResult.Ok }?.asError(),
                    ),
                    discovery = (d as? MortisResult.Ok)?.value?.raw,
                    fragments = (f as? MortisResult.Ok)?.value?.raw,
                )
            }
        }
    }
}

class DriveIntakeViewModel(private val c: AppContainer) : ViewModel() {
    data class State(
        val load: LoadState = LoadState(),
        val implemented: Boolean = false,
        val reason: String = "Drive intake is NOT_IMPLEMENTED unless the worker reports Google credentials.",
        val statusRaw: String? = null,
        val filename: String = "",
        val content: String = "",
        val sourceType: String = "manual",
        val sourceName: String = "android",
        val notice: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refresh()
    }

    fun onFilename(v: String) = _state.update { it.copy(filename = v) }
    fun onContent(v: String) = _state.update { it.copy(content = v) }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(load = LoadState(true)) }
            when (val st = c.mortis.status()) {
                is MortisResult.Ok -> {
                    val raw = st.value.raw.lowercase()
                    val obj = st.value.obj
                    val google = obj?.get("google") ?: obj?.get("drive") ?: obj?.get("gdrive")
                    val hasCreds = google != null && google.toString().contains("true", true) ||
                        raw.contains("\"google\"") && !raw.contains("not_configured") &&
                        !raw.contains("not_implemented")
                    val implemented = hasCreds && !raw.contains("not_implemented")
                    _state.update {
                        it.copy(
                            load = LoadState(false),
                            implemented = implemented,
                            statusRaw = st.value.raw.take(4000),
                            reason = if (implemented) {
                                "Worker reported a Google/Drive surface."
                            } else {
                                "Drive intake is NOT_IMPLEMENTED. Worker has no Google credentials visible on /api/status."
                            },
                        )
                    }
                }
                else -> _state.update {
                    it.copy(
                        load = LoadState(false, st.asError()),
                        implemented = false,
                        reason = "Drive intake is NOT_IMPLEMENTED. ${st.asError() ?: "Status unread."}",
                    )
                }
            }
        }
    }

    fun push() {
        viewModelScope.launch {
            val s = _state.value
            if (s.filename.isBlank() || s.content.isBlank()) {
                _state.update { it.copy(notice = "Filename and content required.") }
                return@launch
            }
            val files = listOf(
                IntakeFile(s.filename, s.content, s.sourceType, s.sourceName, c.session.operatorId ?: "android"),
            )
            when (val r = c.mortis.intakePush(files)) {
                is MortisResult.Ok -> _state.update { it.copy(notice = "Pushed.") }
                else -> _state.update { it.copy(notice = r.asError()) }
            }
        }
    }

    fun ingestUri(uri: Uri, resolver: android.content.ContentResolver) {
        viewModelScope.launch {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "intake.bin"
            val bytes = withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes == null) {
                _state.update { it.copy(notice = "Could not read file.") }
                return@launch
            }
            val encoded = Base64.getEncoder().encodeToString(bytes)
            _state.update { it.copy(filename = name, content = encoded, notice = "Loaded ${bytes.size} bytes as base64.") }
        }
    }
}

class SettingsViewModel(
    private val app: Application,
    private val c: AppContainer,
) : ViewModel() {
    private val _state = MutableStateFlow(VesperSettings())
    val state: StateFlow<VesperSettings> = _state.asStateFlow()
    val notice = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            c.settings.flow.collect { _state.value = it }
        }
    }

    fun update(transform: (VesperSettings) -> VesperSettings) {
        viewModelScope.launch {
            c.settings.update(transform)
            val next = c.settings.snapshot()
            HealthCheckWorker.reconcile(app, next.healthPoll && next.notifySystem)
            notice.value = "Saved."
        }
    }
}
