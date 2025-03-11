package com.maca.tsp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.maca.tsp.R

// Set of Material typography styles to start with
internal val Roboto = FontFamily(
    Font(R.font.roboto_bold, FontWeight.Bold),
    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_regular, FontWeight.Normal),
)
val roboto: TextStyle = TextStyle(
    fontFamily = Roboto
)

val robotoBold: TextStyle = roboto.copy(
    fontWeight = FontWeight.Bold
)
val robotoMedium: TextStyle = roboto.copy(
    fontWeight = FontWeight.Medium
)
val robotoRegular: TextStyle = roboto.copy(
    fontWeight = FontWeight.Normal
)

val TspTypography = Typography(

    displayLarge = robotoRegular.copy(
        fontSize = 57.sp,
        lineHeight = 64.sp
    ),
    displayMedium = robotoRegular.copy(
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = robotoRegular.copy(
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),

    headlineLarge = robotoRegular.copy(
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = robotoRegular.copy(
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = robotoRegular.copy(
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),

    titleLarge = robotoRegular.copy(
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = robotoRegular.copy(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium
    ),
    titleSmall = robotoRegular.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium
    ),

    labelLarge = robotoRegular.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = robotoRegular.copy(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = robotoRegular.copy(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    ),

    bodyLarge = robotoRegular.copy(
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = robotoRegular.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = robotoRegular.copy(
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
)

val title3 = robotoRegular.copy(
    fontSize = 20.sp,
    lineHeight = 25.sp
)

val LocalTspTypography = staticCompositionLocalOf { TspTypography }