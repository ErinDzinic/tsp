package com.maca.tsp.features.editimage.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.maca.tsp.common.util.ImageUtils.uriToBitmap
import com.maca.tsp.features.editimage.EditableImageCanvas
import com.maca.tsp.presentation.state.ImageContract

@Composable
fun ImageCanvasContainer(
    viewState: ImageContract.ImageViewState,
    heightFraction: Float
) {
    val context = LocalContext.current
    val displayBitmap = remember(viewState.rawImage, viewState.displayBitmap) {
        viewState.displayBitmap ?: viewState.rawImage?.let { uriToBitmap(context, it) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(heightFraction)
    ) {
        if (displayBitmap != null) {
            EditableImageCanvas(displayBitmap = displayBitmap)
        }
    }
}
