package com.maca.tsp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class TspSpacing(
    val default: Dp = 0.dp,
    val unit: Dp = 1.dp,
    val extra_xxs: Dp = 2.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val s: Dp = 12.dp,
    val m: Dp = 16.dp,
    val l: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,

    val primaryPadding: Dp = 18.dp,
    val rowPadding: Dp = 20.dp,
    val spacer: Dp = 15.dp,
    val spacerMini: Dp = 10.dp,
    val cardPadding: Dp = s,

    val minWidthCard: Dp = 110.dp,
    val chipsHeight: Dp = 40.dp,

    val spacing0_5: Dp = 4.dp,
    val spacing0_75 : Dp = 6.dp,
    val spacing1: Dp = 8.dp,
    val spacing1_25 : Dp = 10.dp,
    val spacing1_5: Dp = 12.dp,
    val spacing1_75 : Dp = 14.dp,
    val spacing2: Dp = 16.dp,
    val spacing2_5 : Dp = 20.dp,
    val spacing3: Dp = 24.dp,
    val spacing3_5 : Dp = 28.dp,
    val spacing4: Dp = 32.dp,
    val spacing4_5 : Dp = 36.dp,
    val spacing5: Dp = 40.dp,
    val spacing5_5: Dp = 44.dp,
    val spacing6: Dp = 48.dp,
    val spacing7 : Dp = 56.dp,
    val spacing7_5 : Dp = 60.dp,
    val spacing8: Dp = 64.dp,
    val spacing8_5: Dp = 68.dp,
    val spacing9 : Dp = 72.dp,
    val spacing10: Dp = 80.dp,
    val spacing10_5 : Dp = 84.dp,
    val spacing11: Dp = 88.dp,
    val spacing11_25: Dp = 92.dp,
    val spacing12 : Dp = 96.dp,
    val spacing13 : Dp = 102.dp,
    val spacing13_5 : Dp = 104.dp,
    val spacing14 : Dp = 110.dp,
    val spacing14_5 : Dp = 114.dp,
    val spacing15 : Dp = 120.dp,
    val spacing16 : Dp = 128.dp,
    val spacing19 : Dp = 152.dp,
    val spacing20 : Dp = 160.dp,
    val spacing22_5 : Dp = 180.dp,
    val spacing25 : Dp = 200.dp,
    val spacing30 : Dp = 240.dp,
    val spacing40 : Dp = 320.dp,
    val spacing45 : Dp = 360.dp,
)

val LocalTspSpacing = staticCompositionLocalOf { TspSpacing() }