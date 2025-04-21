package com.maca.tsp.common.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap
import com.maca.tsp.R
import com.maca.tsp.data.enums.PrintType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import android.graphics.Rect as AndroidRect
import androidx.compose.ui.geometry.Rect as ComposeRect

object PrintHelper {

    // --- Constants ---
    const val A4_WIDTH_PX = 2480 // A4 at 300 DPI
    const val A4_HEIGHT_PX = 3508 // A4 at 300 DPI
    private const val CROP_MARK_LENGTH = 75f // Length of crop marks in pixels
    private const val CROP_MARK_WIDTH = 5f  // Thickness of crop marks

    // --- Existing printBitmap (unchanged, but ensure context/jobName are handled) ---
    fun printBitmap(bitmap: Bitmap?, context: Context, jobNameRoot: String, onPrintError: (String) -> Unit) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager

        printManager?.let {
            val jobName = context.getString(R.string.app_name) + " $jobNameRoot"
            val printAdapter = BitmapPrintAdapter(context, jobName)
            printAdapter.setBitmap(bitmap) // Pass the single bitmap
            it.print(jobName, printAdapter, null)
        } ?: run {
            onPrintError("Printing service not available.")
        }
    }

    // --- Existing BitmapPrintAdapter (modified slightly for clarity) ---
    private class BitmapPrintAdapter(
        val context: Context,
        val jobName: String
    ) : PrintDocumentAdapter() {
        private var bitmapsToPrint: List<Bitmap> = emptyList()
        private var printJob: Job? = null // To manage the background coroutine

        // Use a single-threaded context for writing to avoid potential issues
        // Using Dispatchers.IO might be okay too, but this ensures serial access
        private val printDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()


        fun setBitmap(bitmap: Bitmap?) {
            this.bitmapsToPrint = if (bitmap != null) listOf(bitmap) else emptyList()
        }
        fun setBitmaps(bitmaps: List<Bitmap>) {
            this.bitmapsToPrint = bitmaps
        }

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            options: Bundle?
        ) {
            // Cancel any previous print job if layout changes
            printJob?.cancel()

            cancellationSignal?.setOnCancelListener { printJob?.cancel() }

            if (bitmapsToPrint.isEmpty()) {
                callback.onLayoutFailed("No bitmaps to print")
                return
            }

            val pdi = PrintDocumentInfo.Builder(jobName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(bitmapsToPrint.size)
                .build()

            callback.onLayoutFinished(pdi, newAttributes != oldAttributes)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor?,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            if (destination == null) {
                callback.onWriteFailed("Destination is null")
                return
            }

            // Launch the work on the background dispatcher
            printJob = CoroutineScope(printDispatcher).launch {
                var pdfDocument: PrintedPdfDocument? = null // Declare outside try
                try {
                    // Check for cancellation initially
                    if (cancellationSignal?.isCanceled == true) {
                        withContext(Dispatchers.Main) { callback.onWriteCancelled() }
                        return@launch
                    }

                    // --- PDF Generation on Background Thread ---
                    val printAttributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("pdf", "PDF resolution", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()

                    pdfDocument = PrintedPdfDocument(context, printAttributes)

                    val pagesToWrite = computeWrittenPages(pages, bitmapsToPrint.size)

                    pagesToWrite.forEach { pageIndex ->
                        // Check for cancellation *inside* the loop
                        if (cancellationSignal?.isCanceled == true) {
                            Timber.d("Print job cancelled during page processing.")
                            // Clean up handled in finally block
                            throw CancellationException("Print cancelled")
                        }
                        if (pageIndex >= bitmapsToPrint.size) return@forEach // Safety check

                        val bitmap = bitmapsToPrint[pageIndex]
                        val page = pdfDocument.startPage(pageIndex)
                        val canvas = page.canvas

                        // --- Fit bitmap onto A4 page logic ---
                        val scale = minOf(
                            canvas.width.toFloat() / bitmap.width,
                            canvas.height.toFloat() / bitmap.height
                        )
                        val left = (canvas.width - bitmap.width * scale) / 2f
                        val top = (canvas.height - bitmap.height * scale) / 2f
                        val matrix = Matrix().apply {
                            postScale(scale, scale)
                            postTranslate(left, top)
                        }
                        // ------------------------------------

                        canvas.drawBitmap(bitmap, matrix, null) // This drawing happens on background thread
                        pdfDocument.finishPage(page)
                        Timber.v("Processed page $pageIndex")
                    }
                    // --- End Page Loop ---

                    // --- Write PDF to file descriptor ---
                    Timber.d("Writing PDF document...")
                    FileOutputStream(destination.fileDescriptor).use { output ->
                        pdfDocument.writeTo(output)
                    }
                    Timber.d("PDF document written successfully.")
                    // --- End Write PDF ---

                    // --- Report Success (on Main Thread) ---
                    withContext(Dispatchers.Main) {
                        callback.onWriteFinished(pagesToWrite.map { PageRange(it, it) }.toTypedArray())
                        Timber.d("onWriteFinished called.")
                    }

                } catch (e: CancellationException) { // Catch cancellation specifically
                    withContext(Dispatchers.Main) {
                        callback.onWriteCancelled()
                        Timber.d("onWriteCancelled called.")
                    }
                } catch (e: Exception) { // Catch other errors
                    Timber.e(e, "Failed to write PDF")
                    withContext(Dispatchers.Main) {
                        callback.onWriteFailed("Failed to write PDF: ${e.message}")
                        Timber.d("onWriteFailed called.")
                    }
                } finally {
                    // --- Cleanup (always executed) ---
                    pdfDocument?.close()
                    try {
                        destination.close() // Close the file descriptor
                    } catch(ioe: java.io.IOException) {
                        Timber.e(ioe, "Error closing ParcelFileDescriptor")
                    }
                    Timber.d("PDF Document and destination closed.")
                }
            } // End CoroutineScope Launch

            // Set up cancellation listener for the signal from the print framework
            cancellationSignal?.setOnCancelListener {
                printJob?.cancel() // Cancel the coroutine if system requests cancellation
                Timber.d("CancellationSignal received, cancelling print job.")
            }
        } // End onWrite

        override fun onFinish() {
            // Cancel any ongoing job when the print process finishes (or is cancelled externally)
            printJob?.cancel()
            printDispatcher.close() // Shut down the dedicated dispatcher
            Timber.d("Print job finished, dispatcher closed.")
            super.onFinish()
        }

        // Helper to figure out which pages to actually write (unchanged)
        private fun computeWrittenPages(requestedPages: Array<out PageRange>?, totalPages: Int): List<Int> {
            // ... (implementation remains the same) ...
            val writtenPages = mutableListOf<Int>()
            requestedPages?.forEach { range ->
                for (i in range.start..range.end) {
                    if (i < totalPages) { // Ensure index is valid
                        writtenPages.add(i)
                    }
                }
            } ?: run { // If null, write all pages (common case)
                for (i in 0 until totalPages) {
                    writtenPages.add(i)
                }
            }
            return writtenPages.distinct().sorted() // Ensure unique and ordered
        }
    } // End BitmapPrintAdapter


    // --- Existing printMultipleBitmaps (Updated to use the improved Adapter) ---
    fun printMultipleBitmaps(
        bitmaps: List<Bitmap>,
        context: Context,
        jobName: String,
        onPrintError: (String) -> Unit
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            onPrintError("Print service unavailable.")
            return
        }

        val printAdapter = BitmapPrintAdapter(context, jobName)
        printAdapter.setBitmaps(bitmaps) // Pass the list of bitmaps
        printManager.print(jobName, printAdapter, null) // Use the same adapter
    }


    // --- NEW Function to render onto the full target area (Sleeve or Back) ---
    fun renderFullAreaBitmap(
        original: Bitmap,
        positioningInfo: PrintPositioningInfo?, // Use the calculated info
        printType: PrintType,
        targetWidthPx: Int = 0,
        targetHeightPx: Int = 0
    ): Bitmap {
        val finalTargetWidth: Int
        val finalTargetHeight: Int

        // Determine target canvas size
        when (printType) {
            PrintType.SLEEVE -> {
                finalTargetWidth = if (targetWidthPx > 0) targetWidthPx else A4_WIDTH_PX
                finalTargetHeight = if (targetHeightPx > 0) targetHeightPx else A4_HEIGHT_PX * 3
            }
            PrintType.BACK -> {
                finalTargetWidth = if (targetWidthPx > 0) targetWidthPx else A4_WIDTH_PX * 3
                finalTargetHeight = if (targetHeightPx > 0) targetHeightPx else A4_HEIGHT_PX * 3
            }
            PrintType.SINGLE -> {
                finalTargetWidth = if (targetWidthPx > 0) targetWidthPx else A4_WIDTH_PX
                finalTargetHeight = if (targetHeightPx > 0) targetHeightPx else A4_HEIGHT_PX
            }
        }

        val resultBitmap = createBitmap(finalTargetWidth, finalTargetHeight)
        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawColor(Color.WHITE)

        if (positioningInfo != null) {
            // --- FIX: Manually create Android Rect from Compose Rect ---
            val destinationAndroidRect = Rect(
                positioningInfo.destinationRect.left.roundToInt(),
                positioningInfo.destinationRect.top.roundToInt(),
                positioningInfo.destinationRect.right.roundToInt(),
                positioningInfo.destinationRect.bottom.roundToInt()
            )
            // ---------------------------------------------------------

            canvas.drawBitmap(
                original,
                positioningInfo.sourceRect,    // Source Android Rect
                destinationAndroidRect,        // Destination Android Rect
                paint
            )
        } else {
            // Fallback: Center the whole image (old logic)
            val scaleFactor = min(
                finalTargetWidth.toFloat() / original.width,
                finalTargetHeight.toFloat() / original.height
            )
            val centerX = finalTargetWidth / 2f
            val centerY = finalTargetHeight / 2f
            val matrix = Matrix()
            matrix.postScale(scaleFactor, scaleFactor)
            matrix.postTranslate(
                centerX - (original.width * scaleFactor) / 2f,
                centerY - (original.height * scaleFactor) / 2f
            )
            canvas.drawBitmap(original, matrix, paint)
        }

        return resultBitmap
    }


    // --- Slice Bitmap (Updated for clarity and Back print) ---
    fun sliceBitmap(bitmap: Bitmap, printType: PrintType): List<Bitmap> {
        val slices = mutableListOf<Bitmap>()
        if (bitmap.width <= 0 || bitmap.height <= 0) return slices // Avoid division by zero

        when (printType) {
            PrintType.SINGLE -> {
                slices.add(addCropMarks(bitmap)) // Add crop marks to single image
            }
            PrintType.SLEEVE -> {
                // Ensure height is divisible by 3, handle remainder if necessary
                val baseSliceHeight = bitmap.height / 3
                val remainderHeight = bitmap.height % 3
                var currentY = 0
                for (i in 0 until 3) {
                    // Add remainder to the last slice
                    val sliceHeight = if (i == 2) baseSliceHeight + remainderHeight else baseSliceHeight
                    if (sliceHeight > 0 && currentY + sliceHeight <= bitmap.height) {
                        try {
                            val slice = Bitmap.createBitmap(bitmap, 0, currentY, bitmap.width, sliceHeight)
                            slices.add(addCropMarks(slice)) // Add crop marks to each slice
                            currentY += sliceHeight
                        } catch (e: IllegalArgumentException) {
                            // Handle potential errors if coordinates are invalid (shouldn't happen with checks)
                            System.err.println("Error slicing sleeve: ${e.message}")
                        }
                    }
                }
            }
            PrintType.BACK -> {
                // Ensure dimensions are divisible by 3
                val baseSliceWidth = bitmap.width / 3
                val baseSliceHeight = bitmap.height / 3
                val remainderWidth = bitmap.width % 3
                val remainderHeight = bitmap.height % 3
                var currentY = 0

                for (row in 0 until 3) {
                    val sliceHeight = if (row == 2) baseSliceHeight + remainderHeight else baseSliceHeight
                    if (sliceHeight <= 0) continue // Skip if height is zero

                    var currentX = 0
                    for (col in 0 until 3) {
                        val sliceWidth = if (col == 2) baseSliceWidth + remainderWidth else baseSliceWidth
                        if (sliceWidth <= 0) continue // Skip if width is zero

                        if (currentY + sliceHeight <= bitmap.height && currentX + sliceWidth <= bitmap.width) {
                            try {
                                val slice = Bitmap.createBitmap(bitmap, currentX, currentY, sliceWidth, sliceHeight)
                                slices.add(addCropMarks(slice)) // Add crop marks to each slice
                                currentX += sliceWidth
                            } catch (e: IllegalArgumentException) {
                                System.err.println("Error slicing back: ${e.message}")
                            }
                        }
                    }
                    currentY += sliceHeight
                }
            }
        }
        return slices
    }

    // --- NEW Helper Function to Add Crop Marks ---
    private fun addCropMarks(bitmap: Bitmap): Bitmap {
        // Create a slightly larger bitmap to accommodate marks if needed,
        // or draw directly if margins are acceptable. Let's draw directly for simplicity.
        val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true) // Ensure mutable copy
        val canvas = Canvas(resultBitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = CROP_MARK_WIDTH
            style = Paint.Style.STROKE
        }

        val w = resultBitmap.width.toFloat()
        val h = resultBitmap.height.toFloat()

        // Top-Left
        canvas.drawLine(0f, 0f, CROP_MARK_LENGTH, 0f, paint)
        canvas.drawLine(0f, 0f, 0f, CROP_MARK_LENGTH, paint)

        // Top-Right
        canvas.drawLine(w - CROP_MARK_LENGTH, 0f, w, 0f, paint)
        canvas.drawLine(w, 0f, w, CROP_MARK_LENGTH, paint)

        // Bottom-Left
        canvas.drawLine(0f, h - CROP_MARK_LENGTH, 0f, h, paint)
        canvas.drawLine(0f, h, CROP_MARK_LENGTH, h, paint)

        // Bottom-Right
        canvas.drawLine(w - CROP_MARK_LENGTH, h, w, h, paint)
        canvas.drawLine(w, h - CROP_MARK_LENGTH, w, h, paint)

        return resultBitmap
    }

    fun calculatePrintPositioning(
        originalBitmap: Bitmap,
        previewCanvasSize: Size, // Size of the interactive canvas
        currentScale: Float,
        currentOffset: Offset,
        targetPrintCanvasSize: IntSize // Size of the final output bitmap (e.g., A4, 3xA4)
    ): PrintPositioningInfo? {

        if (previewCanvasSize.width <= 0 || previewCanvasSize.height <= 0 || originalBitmap.width <= 0 || originalBitmap.height <= 0) {
            Timber.w("Invalid input dimensions for calculatePrintPositioning.")
            return null
        }

        // 1. Determine the initial scale to fit the bitmap within the preview canvas (like in PrintableImageCanvas)
        //    This represents the scale when zoom is 1.0f.
        //    We assume the initial display fits the image centered within the preview.
        val fitScale = min(
            previewCanvasSize.width / originalBitmap.width,
            previewCanvasSize.height / originalBitmap.height
        )

        // 2. Calculate the total effective scale including user zoom.
        val totalScale = fitScale * currentScale

        // 3. Calculate the bounds of the fully scaled/panned bitmap *relative to the preview canvas*.
        val scaledWidth = originalBitmap.width * totalScale
        val scaledHeight = originalBitmap.height * totalScale
        // Top-left corner calculation considers the center pivot and the pan offset.
        val scaledTopLeftX = previewCanvasSize.width / 2f + currentOffset.x - scaledWidth / 2f
        val scaledTopLeftY = previewCanvasSize.height / 2f + currentOffset.y - scaledHeight / 2f
        val scaledBitmapBounds = ComposeRect(
            offset = Offset(scaledTopLeftX, scaledTopLeftY),
            size = Size(scaledWidth, scaledHeight)
        )

        // 4. Define the bounds of the preview canvas itself.
        val previewBounds = ComposeRect(Offset.Zero, previewCanvasSize)

        // 5. Find the intersection rectangle (the visible portion of the bitmap in preview coordinates).
        //    Compose Rect's intersect function requires API level that might not be met,
        //    so we implement intersection manually.
        val intersectLeft = max(scaledBitmapBounds.left, previewBounds.left)
        val intersectTop = max(scaledBitmapBounds.top, previewBounds.top)
        val intersectRight = min(scaledBitmapBounds.right, previewBounds.right)
        val intersectBottom = min(scaledBitmapBounds.bottom, previewBounds.bottom)

        // Check if there is a valid intersection
        if (intersectLeft >= intersectRight || intersectTop >= intersectBottom) {
            Timber.d("No intersection between scaled bitmap and preview bounds.")
            return null // Image is entirely outside the preview area
        }

        val intersectionRectCompose = ComposeRect(intersectLeft, intersectTop, intersectRight, intersectBottom)

        // 6. Map the intersection rectangle (in preview canvas coordinates) back to the *original bitmap's* coordinates.
        //    This determines the sourceRect.
        //    Reverse the transformations: subtract offset, account for centering, divide by total scale.

        // Calculate intersection relative to the scaled bitmap's top-left corner
        val intersectRelativeX = intersectionRectCompose.left - scaledBitmapBounds.left
        val intersectRelativeY = intersectionRectCompose.top - scaledBitmapBounds.top

        // Scale these relative coordinates back to the original bitmap's pixel space
        val srcLeft = (intersectRelativeX / totalScale).coerceIn(0f, originalBitmap.width.toFloat())
        val srcTop = (intersectRelativeY / totalScale).coerceIn(0f, originalBitmap.height.toFloat())
        val srcWidth = (intersectionRectCompose.width / totalScale).coerceIn(0f, originalBitmap.width - srcLeft)
        val srcHeight = (intersectionRectCompose.height / totalScale).coerceIn(0f, originalBitmap.height - srcTop)

        val sourceRect = AndroidRect(
            srcLeft.roundToInt(),
            srcTop.roundToInt(),
            (srcLeft + srcWidth).roundToInt(),
            (srcTop + srcHeight).roundToInt()
        )

        // Sanity check for sourceRect dimensions
        if (sourceRect.width() <= 0 || sourceRect.height() <= 0) {
            Timber.w("Calculated sourceRect has zero or negative dimensions.")
            return null
        }

        // 7. Map the intersection rectangle proportionally onto the targetPrintCanvasSize.
        //    This determines the destinationRect.
        //    Calculate the "loss" percentages from the preview to map position and size.

        // Percentage position of the intersection's top-left within the preview canvas
        val relativeXInPreview = intersectionRectCompose.left / previewCanvasSize.width
        val relativeYInPreview = intersectionRectCompose.top / previewCanvasSize.height

        // Percentage size of the intersection relative to the preview canvas
        val relativeWidthInPreview = intersectionRectCompose.width / previewCanvasSize.width
        val relativeHeightInPreview = intersectionRectCompose.height / previewCanvasSize.height

        // Apply these percentages to the target print canvas size
        val dstLeft = relativeXInPreview * targetPrintCanvasSize.width
        val dstTop = relativeYInPreview * targetPrintCanvasSize.height
        val dstWidth = relativeWidthInPreview * targetPrintCanvasSize.width
        val dstHeight = relativeHeightInPreview * targetPrintCanvasSize.height

        val destinationRect = ComposeRect(
            left = dstLeft,
            top = dstTop,
            right = dstLeft + dstWidth,
            bottom = dstTop + dstHeight
        )

        Timber.d("""
            Calculated Positioning:
            - Preview Size: $previewCanvasSize
            - Scale: $currentScale, Offset: $currentOffset
            - Scaled Bitmap Bounds (Preview): $scaledBitmapBounds
            - Intersection (Preview): $intersectionRectCompose
            - Source Rect (Original Bitmap): $sourceRect
            - Target Print Size: $targetPrintCanvasSize
            - Destination Rect (Print Canvas): $destinationRect
        """.trimIndent())

        return PrintPositioningInfo(sourceRect = sourceRect, destinationRect = destinationRect)
    }
}