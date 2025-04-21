package com.maca.tsp.features.navigation

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.maca.tsp.features.editimage.EditImageScreen
import com.maca.tsp.features.home.HomeScreen
import com.maca.tsp.features.printpreview.PrintPreviewCanvas
import com.maca.tsp.presentation.state.ImageContract
import com.maca.tsp.presentation.state.SIDE_EFFECTS_KEY
import com.maca.tsp.presentation.viewmodel.ImageViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

enum class MainScreens {
    HOME,
    EDIT_IMAGE,
    PRINT_PREVIEW
}

sealed class MainNavigationItem(
    val route: String,
) {
    data object Home : MainNavigationItem(MainScreens.HOME.name)
    data object EditImage : MainNavigationItem(MainScreens.EDIT_IMAGE.name)
    data object PrintPreview : MainNavigationItem(MainScreens.PRINT_PREVIEW.name)
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun MainScreenNavHost(
    navController: NavHostController,
    paddingValue: PaddingValues,
    startDestination: String = MainNavigationItem.Home.route,
) {

    val imageViewModel: ImageViewModel = hiltViewModel()
    val viewState = imageViewModel.viewState.collectAsState().value
    val context = LocalContext.current

    LaunchedEffect(SIDE_EFFECTS_KEY) {
        imageViewModel.effect.onEach { effect ->
            when (effect) {
                ImageContract.ImageEffect.Navigation.ToImageDetails -> navController.navigate(
                    MainNavigationItem.EditImage.route
                )

                ImageContract.ImageEffect.Navigation.ToPrintPreview -> navController.navigate(
                    MainNavigationItem.PrintPreview.route
                )
                is ImageContract.ImageEffect.Navigation.SaveImageToGallery -> {imageViewModel.saveBitmapToGallery(effect.bitmap, context)}
                is ImageContract.ImageEffect.Navigation.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }.collect()
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(MainNavigationItem.Home.route) { HomeScreen(imageViewModel::setEvent, viewState.isImageLoading) }
        composable(MainNavigationItem.EditImage.route) {
            EditImageScreen(
                viewState = viewState,
                onEvent = imageViewModel::setEvent
            )
        }
        composable(MainNavigationItem.PrintPreview.route) {
            PrintPreviewCanvas(
                viewState = viewState,
                onEvent = imageViewModel::setEvent,
                onExitClick = {
                    navController.popBackStack()
                })
        }
    }
}