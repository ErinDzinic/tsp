package com.maca.tsp.common.util

import androidx.compose.ui.geometry.Rect
import android.graphics.Rect as rect

data class PrintPositioningInfo(
    val sourceRect: rect,
    val destinationRect: Rect
)