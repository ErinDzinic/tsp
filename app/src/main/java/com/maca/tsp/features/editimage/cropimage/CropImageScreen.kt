package com.maca.tsp.features.editimage.cropimage

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.maca.tsp.ui.theme.TspTheme
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.UUID

@Composable
fun CropImageScreen(
    imageUri: Uri,
    onCropSuccess: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    // Create a temporary destination URI for the cropped image
    val destinationUri = Uri.fromFile(
        File(context.cacheDir, "cropped_${UUID.randomUUID()}.jpg")
    )

    val options = UCrop.Options().apply {
        setFreeStyleCropEnabled(true)
        setShowCropGrid(true)
        setHideBottomControls(true)
        setStatusBarColor(TspTheme.colors.colorPurple.toArgb())
        setToolbarColor(TspTheme.colors.colorPurple.toArgb())
        setToolbarWidgetColor(TspTheme.colors.background.toArgb())
    }

    // Prepare UCrop activity launcher
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                onCropSuccess(resultUri)
            } else {
                onCancel()
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            onCancel()
        } else {
            onCancel()
        }
    }

    // Launch UCrop when the composable is first created
    LaunchedEffect(imageUri) {
        val uCropIntent = UCrop.of(imageUri, destinationUri)
            .withOptions(options)
            .getIntent(context)
        cropLauncher.launch(uCropIntent)
    }

    // Optional loading UI (shown while UCrop activity is launching)
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator()
    }
}