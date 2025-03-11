package com.maca.tsp.ui.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.maca.tsp.ui.theme.util.remember
import com.maca.tsp.ui.theme.util.rememberTypography


object TspTheme {
    val colors
        @Composable
        @ReadOnlyComposable
        get() = LocalTspColors(isSystemInDarkTheme()).current

    val typography
        @Composable
        @ReadOnlyComposable
        get() = LocalTspTypography.current

    val spacing
        @Composable
        @ReadOnlyComposable
        get() = LocalTspSpacing.current
}

@Composable
fun TspTheme(
    colorScheme: TspColorScheme = TspTheme.colors,
    content: @Composable () -> Unit
) {
    val typography = rememberTypography()
    val rememberNewColorScheme = colorScheme.remember()
    val spacing = remember { TspSpacing() }

    setStatusBarColor(colorScheme)

    CompositionLocalProvider(
        LocalTspTypography provides typography,
        LocalTspColors(isSystemInDarkTheme()) provides rememberNewColorScheme,
        LocalTspSpacing provides spacing,
        content = content,
    )

}


@SuppressLint("NewApi")
@Composable
private fun setStatusBarColor(
    colorScheme: TspColorScheme,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.orbBlack.toArgb()
            window.navigationBarColor = colorScheme.orbBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
}