package com.maca.tsp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val orbNavy = Color(0xFF0E294B)
val orbBlue = Color(0xFF00FFFF)
val orbGreen = Color(0xFF4FFF57)
val orbBlack = Color(0xFF03202F)
val orbStale = Color(0xFF3E536E)
val orbGray = Color(0xFF49454F)
val orbCoral = Color(0xFFFF4A69)
val orbPurple = Color(0xFF831F82)
val darkYellow = Color(0xFFFFD60A)
val darkPurple = Color(0xFFAF52DE)
val blue = Color(0xFF0A84FF)
val orange = Color(0xFFFF9F0A)
val green = Color(0xFF30D158)
val dark9 = Color(0xFF3E536E)
val white = Color(0xFFFFFFFF)
val turquoise = Color(0xFF00C7BE)
val gradientTransparent = Color(0x03202F00)
val onSurface50 = Color(0x50E6E0E9)
val darkBlue = Color(0xFF0A84FF)
val lightBlue = Color(0xFF5BBBE4)
val lightTurquoise = Color(0xFF5ACDCB)
val darkCyan = Color(0xFF64D2FF)
val colorGrayishBlack = Color(0xFF3D3B41)
val colorDarkPurple = Color(0xFF120D19)
val colorPurple = Color(0xFF5E33AC)
val colorDarkGray = Color(0xFF232225)
val colorMediumGray = Color(0xFF989898)
val colorVividPurple = Color(0xFF8530E3)
val colorLightLavender = Color(0xFFF4ECF9)


val LightColorScheme =
    TspColorScheme(
        primary = Color(0xFF6750A4),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005D),
        inversePrimary = Color(0xFFD0BCFF),
        secondary = Color(0xFF625B71),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE8DEF8),
        onSecondaryContainer = Color(0xFF1D192B),
        tertiary = Color(0xFF7D5260),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD8E4),
        onTertiaryContainer = Color(0xFF31111D),
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFFFEF7FF),
        onSurface = Color(0xFF1D1B20),
        surfaceVariant = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFF49454F),
        surfaceTint = Color(0xFFFFFFFF),
        inverseSurface = Color(0xFF322F35),
        inverseOnSurface = Color(0xFFF5EFF7),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFFEF7FF),
        surfaceDim = Color(0xFFFFFFFF),
        surfaceContainer = Color(0xFFF3EDF7),
        surfaceContainerHigh = Color(0xFFECE6F0),
        surfaceContainerHighest = Color(0xFFE6E0E9),
        surfaceContainerLow = Color(0xFFF7F2FA),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        orbNavy = orbNavy,
        orbBlue = orbBlue,
        orbGreen = orbGreen,
        orbBlack = orbBlack,
        orbStale = orbStale,
        orbGray = orbGray,
        orbCoral = orbCoral,
        orbPurple = orbPurple,
        darkYellow = darkYellow,
        darkPurple = darkPurple,
        blue = blue,
        orange = orange,
        green = green,
        dark9 = dark9,
        white = white,
        gradientTransparent = gradientTransparent,
        onSurface50 = onSurface50,
        turquoise = turquoise,
        darkBlue = darkBlue,
        lightBlue = lightBlue,
        lightTurquoise = lightTurquoise,
        darkCyan = darkCyan,
        colorGrayishBlack = colorGrayishBlack,
        colorDarkPurple = colorDarkPurple,
        colorPurple = colorPurple,
        colorDarkGray = colorDarkGray,
        colorMediumGray = colorMediumGray,
        colorVividPurple = colorVividPurple,
        colorLightLavender = colorLightLavender,
    )

val DarkColorScheme =
    TspColorScheme(
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        inversePrimary = Color(0xFF6750A4),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8),
        tertiary = Color(0xFFEFB8C8),
        onTertiary = Color(0xFF492532),
        tertiaryContainer = Color(0xFF633B48),
        onTertiaryContainer = Color(0xFFFFD8E4),
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF141218),
        onSurface = Color(0xFFE6E0E9),
        surfaceVariant = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFFCAC4D0),
        surfaceTint = Color(0xFFFFFFFF),
        inverseSurface = Color(0xFFE6E0E9),
        inverseOnSurface = Color(0xFF322F35),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF3B383E),
        surfaceDim = Color(0xFFFFFFFF),
        surfaceContainer = Color(0xFF211F26),
        surfaceContainerHigh = Color(0xFF2B2930),
        surfaceContainerHighest = Color(0xFF36343B),
        surfaceContainerLow = Color(0xFF1D1B20),
        surfaceContainerLowest = Color(0xFF0F0D13),
        orbNavy = orbNavy,
        orbBlue = orbBlue,
        orbGreen = orbGreen,
        orbBlack = orbBlack,
        orbStale = orbStale,
        orbGray = orbGray,
        orbCoral = orbCoral,
        orbPurple = orbPurple,
        darkYellow = darkYellow,
        darkPurple = darkPurple,
        blue = blue,
        orange = orange,
        green = green,
        dark9 = dark9,
        white = white,
        gradientTransparent = gradientTransparent,
        onSurface50 = onSurface50,
        turquoise = turquoise,
        darkBlue = darkBlue,
        lightBlue = lightBlue,
        lightTurquoise = lightTurquoise,
        darkCyan = darkCyan,
        colorGrayishBlack = colorGrayishBlack,
        colorDarkPurple = colorDarkPurple,
        colorPurple = colorPurple,
        colorDarkGray = colorDarkGray,
        colorMediumGray = colorMediumGray,
        colorVividPurple = colorVividPurple,
        colorLightLavender = colorLightLavender,
    )

@Suppress("LongParameterList", "LongMethod")
class TspColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceTint: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val surfaceBright: Color,
    val surfaceDim: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerLowest: Color,
    val orbNavy: Color,
    val orbBlue: Color,
    val orbGreen: Color,
    val orbBlack: Color,
    val orbStale: Color,
    val orbGray: Color,
    val orbCoral: Color,
    val orbPurple: Color,
    val darkYellow: Color,
    val darkPurple: Color,
    val blue: Color,
    val orange: Color,
    val green: Color,
    val dark9: Color,
    val white: Color,
    val gradientTransparent: Color,
    val onSurface50: Color,
    val turquoise: Color,
    val darkBlue: Color,
    val lightBlue: Color,
    val lightTurquoise: Color,
    val darkCyan: Color,
    val colorGrayishBlack: Color,
    val colorDarkPurple: Color,
    val colorPurple: Color,
    val colorDarkGray: Color,
    val colorMediumGray: Color,
    val colorVividPurple: Color,
    val colorLightLavender: Color,
)


@ReadOnlyComposable
@Composable
fun LocalTspColors(darkTheme: Boolean = isSystemInDarkTheme()) = staticCompositionLocalOf {
    if (darkTheme)
        DarkColorScheme
    else {
        // LightColorScheme uncomment when we support light theme
        DarkColorScheme
    }
}