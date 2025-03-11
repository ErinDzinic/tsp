package com.maca.tsp.ui.theme.util

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.Dp
import com.maca.tsp.ui.theme.DarkColorScheme
import com.maca.tsp.ui.theme.LightColorScheme
import com.maca.tsp.ui.theme.TspColorScheme
import com.maca.tsp.ui.theme.TspTheme
import com.maca.tsp.ui.theme.TspTypography
import kotlinx.coroutines.flow.StateFlow


@Composable
internal fun rememberTypography(): Typography {
    return remember { TspTypography }
}

@Composable
internal fun TspColorScheme.remember(darkTheme: Boolean = isSystemInDarkTheme()) = remember {
    if (darkTheme)
        DarkColorScheme
    else
        LightColorScheme
}

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() }) {
        onClick()
    }
}

@Composable
fun <UiState> StateFlow<UiState>.asState(): UiState {
    return this.collectAsState().value
}

@Composable
fun Modifier.tspSection(
    outsidePadding: Dp = TspTheme.spacing.spacing2,
    outsideTopPadding: Dp = TspTheme.spacing.spacing1_5,
    outsideBottomPadding: Dp = TspTheme.spacing.unit,
    internalTopPadding: Dp = TspTheme.spacing.unit,
    internalBottomPadding: Dp = TspTheme.spacing.spacing2,
    internalStartPadding: Dp = TspTheme.spacing.spacing2,
    internalEndPadding: Dp = TspTheme.spacing.spacing2,
    ) =
    this
        .padding(horizontal = outsidePadding)
        .padding(top = outsideTopPadding, bottom = outsideBottomPadding)
        .background(
            color = TspTheme.colors.dark9.copy(alpha = 0.1f),
            shape = RoundedCornerShape(TspTheme.spacing.spacing1_5)
        )
        .padding(
            bottom = internalBottomPadding,
            top = internalTopPadding,
            start = internalStartPadding,
            end = internalEndPadding
        )