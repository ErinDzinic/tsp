package com.maca.tsp.features.editimage

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.maca.tsp.common.util.ImageFilterUtils
import com.maca.tsp.common.util.ImageUtils.uriToBitmap

@Composable
fun EditableImageCanvas(
    imageUri: Uri?,
    filteredImage: Bitmap?,
    isBlackAndWhite: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var displayBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUri, isBlackAndWhite) {
        imageUri?.let { uri ->
            val bitmap = uriToBitmap(context, uri)
            displayBitmap = if (isBlackAndWhite && bitmap != null) {
                ImageFilterUtils.applyBlackAndWhiteFilter(bitmap)
            } else {
                bitmap
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight(1f)
            .border(2.dp, Color.Black)
            .background(Color.White)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        displayBitmap?.let { bitmap ->
            Image(
                painter = rememberImagePainter(
                    data = bitmap
                ),
                contentDescription = "Filtered Image",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}