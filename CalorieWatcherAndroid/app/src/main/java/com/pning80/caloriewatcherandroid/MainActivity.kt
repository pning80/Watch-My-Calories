package com.pning80.caloriewatcherandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pning80.caloriewatcherandroid.ui.analysis.AnalysisScreen
import com.pning80.caloriewatcherandroid.ui.camera.CameraScreen
import com.pning80.caloriewatcherandroid.ui.today.TodayScreen
import com.pning80.caloriewatcherandroid.ui.history.HistoryScreen
import com.pning80.caloriewatcherandroid.ui.theme.CalorieWatcherAndroidTheme
import com.pning80.caloriewatcherandroid.ui.components.BottomNavigationBar
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalorieWatcherAndroidTheme {
                CalorieWatcherApp()
            }
        }
    }
}

@Composable
fun CalorieWatcherApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                TodayScreen(
                    onEntryClick = { /* Navigate to details */ },
                    onScanClick = { navController.navigate("camera") }
                )
            }
            composable("history") {
                HistoryScreen()
            }
            composable("camera") {
                CameraScreen(
                    onImagesCaptured = { paths ->
                        val encodedPaths = paths.joinToString(",") { 
                             URLEncoder.encode(it, StandardCharsets.UTF_8.toString())
                        }
                        navController.navigate("analysis/$encodedPaths")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("analysis/{imagePaths}") { backStackEntry ->
                val pathsString = backStackEntry.arguments?.getString("imagePaths") ?: ""
                val paths = pathsString.split(",").filter { it.isNotEmpty() }
                AnalysisScreen(
                    imagePaths = paths,
                    onAnalysisComplete = {
                        navController.popBackStack("dashboard", inclusive = false)
                    }
                )
            }
            composable("settings") {
                com.pning80.caloriewatcherandroid.ui.settings.SettingsScreen()
            }
        }
    }
}
