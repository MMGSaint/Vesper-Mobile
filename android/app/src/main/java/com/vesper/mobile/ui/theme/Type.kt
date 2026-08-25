package com.vesper.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val LabelStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 2.2.sp,
    color = Muted,
)

val BodyStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    letterSpacing = 0.15.sp,
    lineHeight = 22.sp,
    color = Parchment,
)

val MonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    letterSpacing = 0.2.sp,
    color = Steel,
)

val VesperTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 36.sp,
        letterSpacing = 6.sp,
        color = Parchment,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        letterSpacing = 1.6.sp,
        color = Parchment,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 1.2.sp,
        color = Parchment,
    ),
    bodyLarge = BodyStyle,
    bodyMedium = BodyStyle.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = BodyStyle.copy(fontSize = 12.sp, lineHeight = 18.sp, color = Muted),
    labelLarge = LabelStyle.copy(fontSize = 12.sp),
    labelMedium = LabelStyle,
    labelSmall = LabelStyle.copy(fontSize = 10.sp, letterSpacing = 2.4.sp),
)
