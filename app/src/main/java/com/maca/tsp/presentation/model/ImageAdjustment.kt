package com.maca.tsp.presentation.model

enum class ImageTool {
    NONE, CROP, BRIGHTNESS, CONTRAST, EXPOSURE, GAMMA, SHARPNESS
}

data class ImageAdjustments(
    val brightness: Float = 0f,  // -1f to 1f
    val contrast: Float = 1f,    // 0.5f to 2f
    val exposure: Float = 0f,    // -1f to 1f
    val gamma: Float = 1f,       // 0.1f to 3f
    val sharpness: Float = 0f     // 0f to 1f
)

sealed class AdjustmentType(val displayName: String, val range: ClosedFloatingPointRange<Float>) {
    object Brightness : AdjustmentType("Brightness", -1f..1f)
    object Contrast : AdjustmentType("Contrast", 0.5f..2f)
    object Exposure : AdjustmentType("Exposure", -1f..1f)
    object Gamma : AdjustmentType("Gamma", 0.1f..3f)
    object Sharpness : AdjustmentType("Sharpness", 0f..1f)

    companion object {
        fun fromTool(tool: ImageTool): AdjustmentType? = when (tool) {
            ImageTool.BRIGHTNESS -> Brightness
            ImageTool.CONTRAST -> Contrast
            ImageTool.EXPOSURE -> Exposure
            ImageTool.GAMMA -> Gamma
            ImageTool.SHARPNESS -> Sharpness
            else -> null
        }
    }
}