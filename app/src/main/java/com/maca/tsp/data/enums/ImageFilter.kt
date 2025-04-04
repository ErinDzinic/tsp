package com.maca.tsp.data.enums

import com.maca.tsp.R

enum class ImageFilterType(
    val iconRes: Int,
    val displayName: String,
    val valueRange: ClosedFloatingPointRange<Float>,
    val defaultValue: Float
)  {
    BRIGHTNESS(R.drawable.ic_brightness, "Brightness", -100f..100f, 0f),
    CONTRAST(R.drawable.ic_contrast, "Contrast", 0.5f..1.5f, 1f),
    EXPOSURE(R.drawable.ic_lightbulb_otln, "Exposure", 0.5f..3f, 1f),
    GAMMA(R.drawable.ic_gamma, "Gamma", 0.1f..3f, 1f),
    SHARPNESS(R.drawable.ic_sharpness, "Sharpness", 0f..5f, 0f),
    GAUSSIAN_BLUR(R.drawable.ic_edit, "Gaussian Blur", 1f..25f, 1f)
}

val filterOptions = listOf(
    ImageFilterType.GAMMA,
    ImageFilterType.CONTRAST,
    ImageFilterType.BRIGHTNESS,
    ImageFilterType.SHARPNESS,
    ImageFilterType.EXPOSURE,
)
