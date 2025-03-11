package com.maca.tsp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowInsets.Type
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.maca.tsp.common.appstate.GlobalState
import com.maca.tsp.features.navigation.MainScreenNavHost
import com.maca.tsp.presentation.viewmodel.MainViewModel
import com.maca.tsp.ui.theme.TspTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var globalState: GlobalState

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        installSplashScreen().setKeepOnScreenCondition {
            mainViewModel.keepSplashScreen
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.keepSplashScreen = false

                mainScreen()
            }
        }
    }

    private fun mainScreen() {
        setContent {
            //globalState.ErrorView()
            val navController = rememberNavController()
            val context = LocalContext.current as Activity
            SideEffect {
                WindowInsetsControllerCompat(context.window, context.window.decorView).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                    hide(Type.statusBars())
                    hide(Type.navigationBars())
                }
            }
            TspTheme {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentColor = Color.Transparent,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        MainScreenNavHost(navController = navController, paddingValue = it)
                    }
                    //globalState.LoadingView()
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, MainActivity::class.java))
        }
    }
}
