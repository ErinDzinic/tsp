package com.maca.tsp.data.enums

import com.maca.tsp.R

enum class ImageFilterType(
    val iconRes: Int,
    val displayName: String,
    val valueRange: ClosedFloatingPointRange<Float>,
    val defaultValue: Float
) {
    BRIGHTNESS(R.drawable.ic_brightness, "Brightness", -100f..100f, 0f),
    CONTRAST(R.drawable.ic_contrast, "Contrast", 0.0f..3f, 1f),
    EXPOSURE(R.drawable.ic_lightbulb_otln, "Exposure", -1f..1f, 0f),
    GAMMA(R.drawable.ic_gamma, "Gamma", 0.1f..3f, 1f),
    SHARPNESS(R.drawable.ic_sharpness, "Sharpness", 0f..10f, 0f)
}

val filterOptions = listOf(
    ImageFilterType.BRIGHTNESS,
    ImageFilterType.EXPOSURE,
    ImageFilterType.CONTRAST,
    ImageFilterType.GAMMA,
    ImageFilterType.SHARPNESS
)
