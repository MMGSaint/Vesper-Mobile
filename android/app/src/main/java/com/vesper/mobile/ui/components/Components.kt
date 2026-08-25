package com.vesper.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.mobile.ui.theme.BodyStyle
import com.vesper.mobile.ui.theme.Crimson
import com.vesper.mobile.ui.theme.CrimsonSoft
import com.vesper.mobile.ui.theme.Elevated
import com.vesper.mobile.ui.theme.Hairline as HairlineColor
import com.vesper.mobile.ui.theme.LabelStyle
import com.vesper.mobile.ui.theme.MonoStyle
import com.vesper.mobile.ui.theme.Muted
import com.vesper.mobile.ui.theme.NearBlack
import com.vesper.mobile.ui.theme.Parchment
import com.vesper.mobile.ui.theme.Steel

@Composable
fun VesperScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    banner: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Text(
                    text = "BACK",
                    style = LabelStyle,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(end = 16.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = LabelStyle.copy(color = Parchment, letterSpacing = 3.sp))
                if (!subtitle.isNullOrBlank()) {
                    Text(text = subtitle, style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
        Hairline()
        if (!banner.isNullOrBlank()) {
            OfflineBanner(banner)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            content = content,
        )
    }
}

@Composable
fun VesperFill(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    banner: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.(PaddingValues) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Text(
                    text = "BACK",
                    style = LabelStyle,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(end = 16.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = LabelStyle.copy(color = Parchment, letterSpacing = 3.sp))
                if (!subtitle.isNullOrBlank()) {
                    Text(text = subtitle, style = MonoStyle.copy(color = Muted, fontSize = 11.sp))
                }
            }
            Row(content = actions)
        }
        Hairline()
        if (!banner.isNullOrBlank()) OfflineBanner(banner)
        Column(modifier = Modifier.fillMaxSize()) {
            content(PaddingValues(0.dp))
        }
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(HairlineColor),
    )
}

@Composable
fun OfflineBanner(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Elevated)
            .border(1.dp, HairlineColor)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(text = text, style = LabelStyle.copy(color = Steel, letterSpacing = 2.8.sp))
    }
}

@Composable
fun UnavailableState(
    title: String,
    reason: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, HairlineColor)
            .background(Elevated)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = title, style = LabelStyle.copy(color = Steel))
        Text(text = reason, style = BodyStyle.copy(color = Muted, fontSize = 13.sp))
    }
}

@Composable
fun StatusRow(label: String, value: String, detail: String? = null) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = LabelStyle)
            Text(text = value, style = LabelStyle.copy(color = Parchment, letterSpacing = 1.6.sp))
        }
        if (!detail.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(text = detail, style = BodyStyle.copy(color = Muted, fontSize = 12.sp))
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = LabelStyle,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun SteelButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val color = if (enabled) Steel else Muted.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = LabelStyle.copy(color = color, letterSpacing = 2.4.sp))
    }
}

@Composable
fun DangerButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val border = if (enabled) Crimson else Crimson.copy(alpha = 0.35f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (enabled) CrimsonSoft else Color.Transparent)
            .border(1.dp, border)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = LabelStyle.copy(color = if (enabled) Parchment else Muted, letterSpacing = 2.4.sp))
    }
}

@Composable
fun GhostButton(label: String, enabled: Boolean = true, compact: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .then(if (compact) Modifier else Modifier.fillMaxWidth())
            .border(1.dp, HairlineColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = LabelStyle.copy(
                color = if (enabled) Steel else Muted.copy(alpha = 0.5f),
                letterSpacing = 1.8.sp,
                fontSize = if (compact) 10.sp else 11.sp,
            ),
        )
    }
}

@Composable
fun VesperField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    secret: Boolean = false,
    mono: Boolean = false,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, HairlineColor, RoundedCornerShape(0.dp))
            .background(Elevated)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(text = placeholder, style = BodyStyle.copy(color = Muted.copy(alpha = 0.55f), fontSize = 13.sp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            textStyle = if (mono) MonoStyle.copy(color = Parchment, fontSize = 13.sp)
            else BodyStyle.copy(fontSize = 14.sp),
            cursorBrush = SolidColor(Steel),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun MonoId(text: String) {
    Text(text = text, style = MonoStyle)
}

@Composable
fun KeyValue(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = LabelStyle.copy(fontSize = 10.sp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MonoStyle.copy(color = Parchment, fontSize = 11.sp),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HairlineColor)
            .background(Elevated)
            .padding(14.dp),
        content = content,
    )
}

@Composable
fun ErrorLine(text: String?) {
    if (text.isNullOrBlank()) return
    Text(
        text = text,
        style = BodyStyle.copy(color = Steel, fontSize = 13.sp),
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
fun ConfirmSheet(
    op: String,
    detail: String?,
    passphrase: String,
    onPassphrase: (String) -> Unit,
    busy: Boolean,
    error: String?,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NearBlack)
            .border(1.dp, Crimson)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = "STEP-UP REQUIRED", style = LabelStyle.copy(color = Parchment))
        Text(text = op, style = MonoStyle.copy(color = Steel))
        if (!detail.isNullOrBlank()) {
            Text(text = detail, style = BodyStyle.copy(color = Muted, fontSize = 13.sp))
        }
        Text(
            text = "Passphrase is sent to Mortis and is not stored on this device.",
            style = BodyStyle.copy(color = Muted, fontSize = 12.sp),
        )
        VesperField(value = passphrase, onValueChange = onPassphrase, placeholder = "passphrase", secret = true)
        ErrorLine(error)
        DangerButton(label = if (busy) "CONFIRMING" else "CONFIRM $op", enabled = !busy && passphrase.isNotBlank(), onClick = onConfirm)
        GhostButton(label = "CANCEL", enabled = !busy, onClick = onCancel)
    }
}

@Composable
fun StateGrid(rows: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HairlineColor),
    ) {
        rows.forEach { (k, v) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = k, style = LabelStyle.copy(fontSize = 10.sp))
                Text(text = v, style = MonoStyle.copy(color = Parchment, fontSize = 11.sp))
            }
            Hairline()
        }
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(1.dp, if (selected) Steel else HairlineColor)
            .background(if (selected) Elevated else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = LabelStyle.copy(
                color = if (selected) Parchment else Muted,
                letterSpacing = 1.4.sp,
                fontSize = 10.sp,
            ),
        )
    }
}

@Composable
fun JsonPreview(text: String) {
    Text(
        text = text,
        style = MonoStyle.copy(fontSize = 11.sp, color = Muted, fontFamily = FontFamily.Monospace),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HairlineColor)
            .background(Elevated)
            .padding(12.dp),
    )
}
