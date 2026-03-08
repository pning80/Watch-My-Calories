package com.pning80.watchmycalories

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
import com.pning80.watchmycalories.ui.analysis.AnalysisScreen
import com.pning80.watchmycalories.ui.camera.CameraScreen
import com.pning80.watchmycalories.ui.today.TodayScreen
import com.pning80.watchmycalories.ui.history.HistoryScreen
import com.pning80.watchmycalories.ui.theme.WatchMyCaloriesTheme
import com.pning80.watchmycalories.ui.components.BottomNavigationBar
import com.pning80.watchmycalories.ui.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WatchMyCaloriesTheme {
                WatchMyCaloriesApp()
            }
        }
    }
}

@Composable
fun WatchMyCaloriesApp() {
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
                HistoryScreen(onEntryClick = {})
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
                SettingsScreen(onSave = { navController.navigate("dashboard") })
            }
        }
    }
}
