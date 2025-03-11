package com.maca.tsp.common.util

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberImagePicker(
    onImageSelected: (Uri) -> Unit,
    onError: ((Exception) -> Unit)? = null
): () -> Unit {
    val coroutineScope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    onImageSelected(it)
                } catch (e: Exception) {
                    onError?.invoke(e)
                }
            }
        }
    }

    return { imagePickerLauncher.launch("image/*") }
}