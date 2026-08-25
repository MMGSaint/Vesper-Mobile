package com.vesper.mobile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vesper.mobile.data.vesper.ChatTurn
import com.vesper.mobile.ui.AppStateViewModel
import com.vesper.mobile.ui.components.ErrorLine
import com.vesper.mobile.ui.components.GhostButton
import com.vesper.mobile.ui.components.Hairline
import com.vesper.mobile.ui.components.SectionLabel
import com.vesper.mobile.ui.components.SecureWindow
import com.vesper.mobile.ui.components.StatusRow
import com.vesper.mobile.ui.components.SteelButton
import com.vesper.mobile.ui.components.UnavailableState
import com.vesper.mobile.ui.components.VesperField
import com.vesper.mobile.ui.components.VesperFill
import com.vesper.mobile.ui.components.VesperScaffold
import com.vesper.mobile.ui.theme.Elevated
import com.vesper.mobile.ui.theme.Hairline as HairlineColor
import com.vesper.mobile.ui.theme.LabelStyle
import com.vesper.mobile.ui.theme.MonoStyle
import com.vesper.mobile.ui.theme.Muted
import com.vesper.mobile.ui.theme.NearBlack
import com.vesper.mobile.ui.theme.Parchment
import com.vesper.mobile.ui.theme.Steel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SplashScreen(appVm: AppStateViewModel, onDone: () -> Unit) {
    val state by appVm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.ready) {
        delay(700)
        if (state.ready) onDone()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "VESPER", style = LabelStyle.copy(color = Parchment, letterSpacing = 8.sp, fontSize = 14.sp))
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (state.probing) "PROBING" else "READY",
                style = MonoStyle.copy(color = Muted, fontSize = 11.sp),
            )
        }
    }
}

@Composable
fun HomeScreen(
    appVm: AppStateViewModel,
    onChat: () -> Unit,
    onOperator: () -> Unit,
    onUnlock: () -> Unit,
    onSettings: () -> Unit,
) {
    val state by appVm.state.collectAsStateWithLifecycle()
    val s = state.surface
    VesperScaffold(
        title = "VESPER",
        subtitle = "companion · control surface",
        banner = s.offlineBanner,
        actions = {
            Text(
                text = "REFRESH",
                style = LabelStyle,
                modifier = Modifier.clickable { appVm.refresh() },
            )
        },
    ) {
        SectionLabel("STATUS")
        StatusRow("VESPER", s.vesper.label, s.vesper.detail)
        Hairline()
        StatusRow("MORTIS", s.mortis.label, s.mortis.detail)
        Hairline()
        StatusRow("OPERATOR", s.operator.label, s.operator.detail)
        Hairline()
        StatusRow(
            "ATTENTION",
            s.attention.count?.toString() ?: "—",
            s.attention.detail,
        )
        Hairline()
        StatusRow(
            "LAST EVENT",
            s.lastEvent.summary ?: "—",
            s.lastEvent.at,
        )
        Hairline()
        StatusRow(
            "PC CORE",
            "UNAVAILABLE",
            "Local Vesper core runs on the PC host. It is not packaged in this Android client.",
        )
        Spacer(Modifier.height(28.dp))
        SteelButton(label = "TALK TO VESPER", onClick = onChat)
        Spacer(Modifier.height(12.dp))
        if (s.operator.unlocked) {
            SteelButton(label = "OPERATOR ROOM", onClick = onOperator)
        } else {
            SteelButton(label = "OPERATOR UNLOCK", onClick = onUnlock)
        }
        Spacer(Modifier.height(12.dp))
        GhostButton(label = "SETTINGS", onClick = onSettings)
        Spacer(Modifier.height(28.dp))
        Text(
            text = "This client does not invent connection. Unreachable surfaces stay unavailable.",
            style = MonoStyle.copy(color = Muted, fontSize = 11.sp),
        )
    }
}

@Composable
fun ChatScreen(
    vm: ChatViewModel,
    appVm: AppStateViewModel,
    onBack: (() -> Unit)? = null,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val surface by appVm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    LaunchedEffect(state.turns.size) {
        if (state.turns.isNotEmpty()) listState.animateScrollToItem(state.turns.lastIndex)
    }
    VesperFill(
        title = "CHAT",
        subtitle = if (state.available) "remote" else "unavailable",
        onBack = onBack,
        banner = surface.surface.offlineBanner,
        actions = {
            Text(
                text = "CLEAR",
                style = LabelStyle,
                modifier = Modifier.clickable { vm.clear() },
            )
        },
    ) {
        if (!state.available) {
            UnavailableState(
                title = "VESPER UNAVAILABLE",
                reason = state.availability,
                modifier = Modifier.padding(20.dp),
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.turns, key = { it.id }) { turn ->
                MessageRow(turn, onRetry = { vm.retry(turn.id) }, onCopy = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("vesper", turn.text.ifBlank { turn.error.orEmpty() }))
                })
            }
        }
        Hairline()
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ErrorLine(state.error)
            VesperField(
                value = state.draft,
                onValueChange = vm::onDraft,
                placeholder = "message",
                singleLine = false,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    SteelButton(
                        label = if (state.sending) "SENDING" else "SEND",
                        enabled = !state.sending && state.draft.isNotBlank(),
                        onClick = vm::send,
                    )
                }
                Box(Modifier.width(108.dp)) {
                    GhostButton(
                        label = "MIC",
                        enabled = false,
                        onClick = {},
                    )
                }
            }
            Text(text = state.voiceReason, style = MonoStyle.copy(color = Muted, fontSize = 10.sp))
        }
    }
}

@Composable
private fun MessageRow(turn: ChatTurn, onRetry: () -> Unit, onCopy: () -> Unit) {
    val label = when (turn.role) {
        "user" -> "YOU"
        "system" -> "SYSTEM"
        "tool" -> "TOOL"
        else -> "VESPER"
    }
    val ts = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(turn.atEpochMs))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HairlineColor)
            .background(Elevated)
            .padding(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = LabelStyle.copy(color = Steel, fontSize = 10.sp))
            Text(text = ts, style = MonoStyle.copy(color = Muted, fontSize = 10.sp))
        }
        Spacer(Modifier.height(6.dp))
        when {
            turn.role == "system" || turn.role == "tool" ->
                Text(text = turn.text.ifBlank { turn.error ?: "—" }, style = MonoStyle.copy(fontSize = 12.sp, color = Muted))
            turn.status == "pending" ->
                Text(text = "…", style = MonoStyle.copy(color = Muted))
            turn.status == "error" ->
                Text(text = turn.error ?: "failed", style = MonoStyle.copy(color = Steel, fontSize = 12.sp))
            else ->
                Text(text = turn.text, style = MonoStyle.copy(color = Parchment, fontSize = 13.sp))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton(label = "COPY", compact = true, onClick = onCopy)
            if (turn.status == "error") {
                GhostButton(label = "RETRY", compact = true, onClick = onRetry)
            }
        }
    }
}

@Composable
fun UnlockScreen(
    vm: UnlockViewModel,
    appVm: AppStateViewModel,
    onBack: (() -> Unit)? = null,
    onUnlocked: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.unlocked) {
        if (state.unlocked) {
            appVm.refresh()
            onUnlocked()
        }
    }
    VesperScaffold(title = "OPERATOR UNLOCK", onBack = onBack) {
        SecureWindow()
        Text(
            text = "Passphrase is POSTed to Mortis and is never stored on this device. Session token is held in EncryptedSharedPreferences. Idle 15 minutes. Absolute 60 minutes.",
            style = MonoStyle.copy(color = Muted, fontSize = 11.sp),
        )
        Spacer(Modifier.height(16.dp))
        StatusRow("HOST", state.host)
        StatusRow("ADMIN PATH", if (state.adminSet) "SET" else "MISSING")
        if (!state.adminSet) {
            Spacer(Modifier.height(12.dp))
            UnavailableState(
                title = "NOT CONFIGURED",
                reason = "Paste the admin path segment in Settings. It is not shipped in this APK.",
            )
        }
        SectionLabel("OPERATOR ID")
        VesperField(value = state.operatorId, onValueChange = vm::onOperator, placeholder = "operator_id", mono = true)
        SectionLabel("PASSPHRASE")
        VesperField(value = state.passphrase, onValueChange = vm::onPass, placeholder = "passphrase", secret = true)
        ErrorLine(state.error)
        Spacer(Modifier.height(16.dp))
        SteelButton(
            label = if (state.busy) "UNLOCKING" else "UNLOCK",
            enabled = !state.busy && state.adminSet && state.passphrase.isNotBlank(),
            onClick = vm::unlock,
        )
    }
}
