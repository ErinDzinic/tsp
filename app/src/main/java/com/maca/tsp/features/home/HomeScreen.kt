package com.maca.tsp.features.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.maca.tsp.R
import com.maca.tsp.common.util.rememberImagePicker
import com.maca.tsp.designsystem.AppBackground
import com.maca.tsp.designsystem.PrimaryButton
import com.maca.tsp.presentation.state.ImageContract
import com.maca.tsp.ui.theme.TspTheme

@Composable
fun HomeScreen(
    onEvent: (ImageContract.ImageEvent) -> Unit
) {

    val context = LocalContext.current

    val pickImage = rememberImagePicker(
        onImageSelected = { uri -> onEvent(ImageContract.ImageEvent.ImageSelected(uri,context)) },
        onError = { it.printStackTrace() }
    )

    AppBackground(imageRes = R.drawable.bg3x) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.tsp_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(TspTheme.spacing.spacing13_5)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(TspTheme.spacing.spacing10))

            PrimaryButton(
                text = stringResource(R.string.select_an_image),
                onClick = {
                    pickImage.invoke()
                }
            )
        }
    }
}