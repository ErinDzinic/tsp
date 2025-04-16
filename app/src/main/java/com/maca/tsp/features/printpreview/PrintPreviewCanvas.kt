package com.maca.tsp.features.printpreview

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.maca.tsp.R
import com.maca.tsp.common.util.PrintHelper
import com.maca.tsp.common.util.PrintHelper.calculatePrintPositioning
import com.maca.tsp.data.enums.PrintType
import com.maca.tsp.designsystem.SecondaryButton
import com.maca.tsp.features.printpreview.composables.ActionButtons
import com.maca.tsp.features.printpreview.composables.DimensionDisplay
import com.maca.tsp.features.printpreview.composables.PreviewArea
import com.maca.tsp.presentation.state.ImageContract
import com.maca.tsp.ui.theme.TspTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun PrintPreviewCanvas(
    modifier: Modifier = Modifier,
    viewState: ImageContract.ImageViewState,
    onEvent: (ImageContract.ImageEvent) -> Unit,
    onExitClick: () -> Unit
) {
    var currentScale by remember { mutableFloatStateOf(1f) }
    var currentOffset by remember { mutableStateOf(Offset.Zero) }
    val printType = viewState.selectedPrintType
    val isSleeve = printType == PrintType.SLEEVE
    val context = LocalContext.current
    var isPrinting by remember { mutableStateOf(false) } // Add state for loading indicator
    var previewAreaSize by remember { mutableStateOf(Size.Zero) }

    // Calculate target print dimensions (memoized)
    val targetPrintSize = remember(printType) {
        when (printType) {
            PrintType.SLEEVE -> IntSize(PrintHelper.A4_WIDTH_PX, PrintHelper.A4_HEIGHT_PX * 3)
            PrintType.BACK -> IntSize(PrintHelper.A4_WIDTH_PX * 3, PrintHelper.A4_HEIGHT_PX * 3)
            PrintType.SINGLE -> IntSize(PrintHelper.A4_WIDTH_PX, PrintHelper.A4_HEIGHT_PX)
        }
    }
    // Basic loading overlay (replace with a nicer one if needed)
    if (isPrinting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator() // Or your custom loading composable
        }
    }

    if (isSleeve) {
        // --- Sleeve Layout ---
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(TspTheme.colors.colorPurple)
                .padding(TspTheme.spacing.spacing0_5),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onSizeChanged { previewAreaSize = it.toSize() },
                contentAlignment = Alignment.Center
            ) {
                viewState.displayBitmap?.let { bitmap ->
                    PreviewArea(
                        bitmap = bitmap,
                        printType = printType,
                        onScaleChanged = { scale -> currentScale = scale },
                        onOffsetChanged = { offset -> currentOffset = offset },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Action Panel
            SleeveActionPanel(
                bitmap = viewState.displayBitmap,
                currentScale = currentScale,
                onPrintClick = {
                    if (isPrinting) return@SleeveActionPanel
                    isPrinting = true
                    CoroutineScope(Dispatchers.Default).launch {
                        val original = viewState.displayBitmap
                        if (original == null) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Bitmap not ready", Toast.LENGTH_SHORT)
                                    .show()
                                isPrinting = false
                            }
                            return@launch
                        }

                        val positioningInfo = calculatePrintPositioning(
                            originalBitmap = original,
                            previewCanvasSize = previewAreaSize,
                            currentScale = currentScale,
                            currentOffset = currentOffset,
                            targetPrintCanvasSize = targetPrintSize
                        )

                        if (positioningInfo == null) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Image outside printable area?", Toast.LENGTH_SHORT).show()
                                isPrinting = false
                            }
                            return@launch
                        }

                        try {
                            val fullSleeveBitmap = PrintHelper.renderFullAreaBitmap(
                                original = original,
                                positioningInfo = positioningInfo,
                                printType = PrintType.SLEEVE
                            )

                            val slices = PrintHelper.sliceBitmap(fullSleeveBitmap, PrintType.SLEEVE)

                            if (slices.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    PrintHelper.printMultipleBitmaps(
                                        bitmaps = slices,
                                        context = context,
                                        jobName = "Sleeve Print",
                                        onPrintError = { errorMsg ->
                                            Toast.makeText(
                                                context,
                                                "Print Error: $errorMsg",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            isPrinting = false
                                        }
                                    )
                                    isPrinting = false
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Failed to prepare slices",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    isPrinting = false
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Error during print prep: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                isPrinting = false
                            }
                        }
                    }
                },
                onSaveClick = { onEvent(ImageContract.ImageEvent.SaveImageClicked(context)) },
                onExitClick = onExitClick
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(TspTheme.colors.colorPurple),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                viewState.displayBitmap?.let { bitmap ->
                    PreviewArea(
                        bitmap = bitmap,
                        printType = printType,
                        onScaleChanged = { scale -> currentScale = scale },
                        onOffsetChanged = { offset -> currentOffset = offset },
                        modifier = Modifier.padding(TspTheme.spacing.spacing2)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(bottom = TspTheme.spacing.spacing3),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                viewState.displayBitmap?.let { bitmap ->
                    DimensionDisplay(bitmap = bitmap, currentScale = currentScale)
                    Divider(
                        color = TspTheme.colors.colorGrayishBlack,
                        thickness = TspTheme.spacing.extra_xxs
                    )
                }

                ActionButtons(
                    onPrintClick = {
                        if (isPrinting) return@ActionButtons
                        isPrinting = true
                        CoroutineScope(Dispatchers.Default).launch {
                            val original = viewState.displayBitmap
                            if (original == null) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Bitmap not ready", Toast.LENGTH_SHORT)
                                        .show()
                                    isPrinting = false
                                }
                                return@launch
                            }

                            val positioningInfo = calculatePrintPositioning(
                                originalBitmap = original,
                                previewCanvasSize = previewAreaSize, // Use measured size
                                currentScale = currentScale,
                                currentOffset = currentOffset,
                                targetPrintCanvasSize = targetPrintSize
                            )

                            if (positioningInfo == null) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Image outside printable area?", Toast.LENGTH_SHORT).show()
                                    isPrinting = false
                                }
                                return@launch
                            }

                            try {
                                val renderedBitmap = PrintHelper.renderFullAreaBitmap(
                                    original = original,
                                    positioningInfo = positioningInfo,
                                    printType = printType
                                )

                                val bitmapsToPrint =
                                    PrintHelper.sliceBitmap(renderedBitmap, printType)

                                if (bitmapsToPrint.isNotEmpty()) {
                                    withContext(Dispatchers.Main) {
                                        if (printType == PrintType.BACK) {
                                            PrintHelper.printMultipleBitmaps(
                                                bitmaps = bitmapsToPrint,
                                                context = context,
                                                jobName = "Back Print",
                                                onPrintError = { errorMsg ->
                                                    Toast.makeText(
                                                        context,
                                                        "Print Error: $errorMsg",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    isPrinting = false
                                                }
                                            )
                                        } else {
                                            PrintHelper.printBitmap(
                                                bitmapsToPrint.first(),
                                                context,
                                                "Single Print",
                                                onPrintError = { errorMsg ->
                                                    Toast.makeText(
                                                        context,
                                                        "Print Error: $errorMsg",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    isPrinting = false
                                                }
                                            )
                                        }
                                        isPrinting = false
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Failed to prepare image(s)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        isPrinting = false
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Error during print prep: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    isPrinting = false
                                }
                            }
                        }
                    },
                    onSaveClick = { onEvent(ImageContract.ImageEvent.SaveImageClicked(context)) },
                    onExitClick = onExitClick
                )
            }
        }
    }
}


@Composable
fun SleeveActionPanel(
    bitmap: Bitmap?,
    currentScale: Float,
    onPrintClick: () -> Unit,
    onSaveClick: () -> Unit,
    onExitClick: () -> Unit
) {
    Column(
        modifier = Modifier
            // .fillMaxWidth(0.3f) // Example: 30% of available width
            .width(200.dp) // Keep fixed width if preferred
            .height(TspTheme.spacing.spacing55) // Fill height of the Row
            .padding(start = TspTheme.spacing.spacing1) // Add padding between preview and panel
            .clip(RoundedCornerShape(TspTheme.spacing.spacing3))
            .background(Color.Black), // Keep original background
        verticalArrangement = Arrangement.Top, // Center items vertically
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (bitmap != null) {
            DimensionDisplay(bitmap = bitmap, currentScale = currentScale)
            Divider(
                color = TspTheme.colors.colorGrayishBlack,
                thickness = TspTheme.spacing.extra_xxs,
            )
        }
        // Use spacing within the column for buttons
        Column(
            modifier = Modifier
                .padding(TspTheme.spacing.spacing2),
            verticalArrangement = Arrangement.spacedBy(TspTheme.spacing.spacing2)
        ) {
            SecondaryButton(
                text = stringResource(R.string.print),
                onClick = onPrintClick,
                icon = R.drawable.ic_print,
                iconSize = TspTheme.spacing.spacing2_75
            )
            SecondaryButton(
                text = stringResource(R.string.save),
                onClick = onSaveClick,
                icon = R.drawable.ic_save,
                iconSize = TspTheme.spacing.spacing2_75
            )
            SecondaryButton(
                text = stringResource(R.string.exit),
                onClick = onExitClick,
                icon = R.drawable.ic_exit,
                iconSize = TspTheme.spacing.spacing2_75
            )
        }
    }
}