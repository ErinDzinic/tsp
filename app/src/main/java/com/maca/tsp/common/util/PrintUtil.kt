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
import java.io.FileOutputStream
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

        // Accept single or multiple bitmaps
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
            if (cancellationSignal?.isCanceled == true || bitmapsToPrint.isEmpty()) {
                callback.onLayoutFailed("Cancelled or no bitmaps")
                return
            }

            val pdi = PrintDocumentInfo.Builder(jobName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(bitmapsToPrint.size) // Set page count based on list size
                .build()

            // Check if layout actually changed - optimisation
            val layoutChanged = newAttributes != oldAttributes
            callback.onLayoutFinished(pdi, layoutChanged)
        }

        override fun onWrite(
            pages: Array<out PageRange>?, // Android system provides the specific pages to write
            destination: ParcelFileDescriptor?,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            if (cancellationSignal?.isCanceled == true || destination == null) {
                callback.onWriteFailed("Cancelled or destination is null")
                return
            }

            // Define standard A4 attributes for PDF generation
            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("pdf", "PDF resolution", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            val pdfDocument = PrintedPdfDocument(context, printAttributes)

            // Determine which pages to write based on the 'pages' array
            val pagesToWrite = computeWrittenPages(pages, bitmapsToPrint.size)

            try {
                pagesToWrite.forEach { pageIndex ->
                    if (cancellationSignal?.isCanceled == true) {
                        pdfDocument.close() // Clean up if cancelled mid-process
                        callback.onWriteCancelled()
                        return@forEach
                    }
                    if (pageIndex >= bitmapsToPrint.size) return@forEach // Safety check

                    val bitmap = bitmapsToPrint[pageIndex]
                    val page = pdfDocument.startPage(pageIndex) // Use pageIndex
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

                    canvas.drawBitmap(bitmap, matrix, null)
                    pdfDocument.finishPage(page)
                }

                FileOutputStream(destination.fileDescriptor).use { output ->
                    pdfDocument.writeTo(output)
                }
                // Inform system which pages were actually written
                callback.onWriteFinished(pagesToWrite.map { PageRange(it, it) }.toTypedArray())

            } catch (e: Exception) {
                callback.onWriteFailed("Failed to write PDF: ${e.message}")
            } finally {
                pdfDocument.close()
            }
        }
        // Helper to figure out which pages to actually write based on Print framework request
        private fun computeWrittenPages(requestedPages: Array<out PageRange>?, totalPages: Int): List<Int> {
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
    }

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
        previewCanvasSize: Size, // Size of the interactive canvas from PrintableImageCanvas
        currentScale: Float,
        currentOffset: Offset,
        targetPrintCanvasSize: IntSize, // Size of the final output bitmap
        // printType: PrintType // Might not be needed if targetPrintCanvasSize is always provided
    ): PrintPositioningInfo? {

        // --- Placeholder Implementation ---
        // This placeholder just fits the whole image centered, ignoring precise preview bounds.
        // You need to replace this with accurate geometric calculations.

        // 1. Calculate the effective scale to fit the original bitmap into the previewCanvasSize
        //    (This is similar to the initial scaling done in PrintableImageCanvas/render functions)
        val fitScale = min(
            previewCanvasSize.width / originalBitmap.width,
            previewCanvasSize.height / originalBitmap.height
        )
        val totalScale = fitScale * currentScale // Combine initial fit with user zoom

        // 2. Calculate the bounds of the scaled bitmap *relative to the preview canvas center*
        val scaledWidth = originalBitmap.width * totalScale
        val scaledHeight = originalBitmap.height * totalScale
        val scaledTopLeft = Offset(
            x = previewCanvasSize.width / 2f + currentOffset.x - scaledWidth / 2f,
            y = previewCanvasSize.height / 2f + currentOffset.y - scaledHeight / 2f
        )
        val scaledBitmapBounds = ComposeRect(offset = scaledTopLeft, size = Size(scaledWidth, scaledHeight))

        // 3. *** CRITICAL STEP (Placeholder Needs Replacing) ***
        //    Calculate the intersection between scaledBitmapBounds and the previewCanvas bounds (ComposeRect(Offset.Zero, previewCanvasSize)).
        //    If there's no intersection, return null.
        val previewBounds = ComposeRect(Offset.Zero, previewCanvasSize)
        val intersectionRectCompose = scaledBitmapBounds.intersect(previewBounds) // This function might not exist, need library or manual calc

        if (intersectionRectCompose.isEmpty) {
            // return null // Image is entirely outside preview
            // For now, let's pretend it's always visible to avoid breaking flow
        }

        // 4. *** CRITICAL STEP (Placeholder Needs Replacing) ***
        //    Map the intersectionRectCompose (which is in preview canvas coordinates) back to the *original bitmap's* coordinates
        //    to get the sourceRect (AndroidRect). This involves reversing the scale and offset transformations.
        //    This is complex geometry.
        val srcLeft = 0 // TODO: Replace with actual calculated source left coordinate
        val srcTop = 0  // TODO: Replace with actual calculated source top coordinate
        val srcRight = originalBitmap.width // TODO: Replace with actual calculated source right coordinate
        val srcBottom = originalBitmap.height // TODO: Replace with actual calculated source bottom coordinate

        val sourceRect = AndroidRect(srcLeft.coerceIn(0, originalBitmap.width),
            srcTop.coerceIn(0, originalBitmap.height),
            srcRight.coerceIn(0, originalBitmap.width),
            srcBottom.coerceIn(0, originalBitmap.height))

        if (sourceRect.width() <= 0 || sourceRect.height() <= 0) return null // Nothing visible

        // 5. *** CRITICAL STEP (Placeholder Needs Replacing) ***
        //    Calculate the destinationRect (ComposeRect) on the *targetPrintCanvas*. This should reflect the
        //    positioning seen in the preview. You might use the "loss" percentages derived from the
        //    intersection calculation or map the intersectionRectCompose proportionally to the targetPrintCanvasSize.
        //    For this placeholder, we'll just center the sourceRect within the target canvas.
        val printScale = min(
            targetPrintCanvasSize.width.toFloat() / sourceRect.width(),
            targetPrintCanvasSize.height.toFloat() / sourceRect.height()
        )
        val dstWidth = sourceRect.width() * printScale
        val dstHeight = sourceRect.height() * printScale
        val dstLeft = (targetPrintCanvasSize.width - dstWidth) / 2f
        val dstTop = (targetPrintCanvasSize.height - dstHeight) / 2f
        val destinationRect = ComposeRect(dstLeft, dstTop, dstLeft + dstWidth, dstTop + dstHeight)


        // --- End Placeholder ---

        return PrintPositioningInfo(sourceRect = sourceRect, destinationRect = destinationRect)
    }
}