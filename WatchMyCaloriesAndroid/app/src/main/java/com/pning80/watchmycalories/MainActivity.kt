package com.pning80.watchmycalories

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pning80.watchmycalories.ui.theme.WatchMyCaloriesTheme
import com.pning80.watchmycalories.ui.dashboard.DashboardScreen
import com.pning80.watchmycalories.ui.history.HistoryScreen
import com.pning80.watchmycalories.ui.camera.CameraScreen
import com.pning80.watchmycalories.ui.entry.ManualEntryScreen
import com.pning80.watchmycalories.ui.logfood.LogFoodSheet
import kotlinx.coroutines.launch
import com.pning80.watchmycalories.ui.onboarding.OnboardingScreen
import com.pning80.watchmycalories.ui.settings.SettingsScreen
import com.pning80.watchmycalories.ui.settings.SettingsDataStore
import com.pning80.watchmycalories.ui.analysis.AnalysisScreen
import com.pning80.watchmycalories.ui.menuscanner.ScannedMenusScreen
import com.pning80.watchmycalories.ui.menuscanner.MenuScanDetailScreen
import com.pning80.watchmycalories.ui.menuscanner.MenuAnalysisScreen
import com.pning80.watchmycalories.ui.about.AboutScreen
import com.pning80.watchmycalories.ads.AdManager
import com.pning80.watchmycalories.ai.GeminiRepository
import com.pning80.watchmycalories.security.PlayIntegrityManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.MediaStore
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.util.Log
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsDataStore = SettingsDataStore(this)
        
        AdManager.initialize(this)

        lifecycleScope.launch {
            PlayIntegrityManager.ensureAttested(this@MainActivity)
        }
        
        setContent {
            WatchMyCaloriesTheme {
                // Check onboarding state
                val hasCompletedOnboarding by settingsDataStore.hasCompletedOnboardingFlow.collectAsState(initial = true)

                if (!hasCompletedOnboarding) {
                    OnboardingScreen(
                        settingsDataStore = settingsDataStore,
                        onComplete = { profile ->
                            viewModel.saveProfile(profile)
                        }
                    )
                } else {
                    MainAppContent(
                        viewModel = viewModel,
                        settingsDataStore = settingsDataStore
                    )
                }
            }
        }
    }
}

@Composable
private fun MainAppContent(
    viewModel: MainViewModel,
    settingsDataStore: SettingsDataStore
) {
    val navController = rememberNavController()
    var showLogFoodSheet by remember { mutableStateOf(false) }
    
    // Shared state for images to avoid Parcelable size limits in navigation
    var analysisImages by remember { mutableStateOf<List<Bitmap>?>(null) }
    val geminiRepository = remember { GeminiRepository("YOUR_API_KEY") } // Replace with real later
    val context = androidx.compose.ui.platform.LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                analysisImages = listOf(bitmap)
                navController.navigate("analysis")
            } catch (e: Exception) {
                Log.e("PhotoPicker", "Failed to load image", e)
            }
        }
    }

    // Shared state for Menu Analysis images
    var menuAnalysisImage by remember { mutableStateOf<Bitmap?>(null) }
    val menuPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                menuAnalysisImage = bitmap
                navController.navigate("menuAnalysis")
            } catch (e: Exception) {
                Log.e("MenuPicker", "Failed to load image", e)
            }
        }
    }

    // Collect live data from Room DB via ViewModel
    val allEntries by viewModel.allEntries.collectAsState()
    val todayEntries by viewModel.todayEntries.collectAsState()
    val targetCalories by viewModel.targetCalories.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val allMenuScans by viewModel.allMenuScans.collectAsState()
    val burnedCalories by viewModel.burnedCalories.collectAsState()
    val isMetric by settingsDataStore.isMetricFlow.collectAsState(initial = true)

    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        coroutineScope.launch {
            viewModel.healthConnectManager.checkPermissions()
        }
    }

    LaunchedEffect(Unit) {
        if (androidx.health.connect.client.HealthConnectClient.getSdkStatus(context) == androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE) {
            viewModel.healthConnectManager.checkPermissions()
            if (!viewModel.healthConnectManager.isAuthorized.value) {
                val permissions = setOf(
                    androidx.health.connect.client.permission.HealthPermission.getReadPermission(androidx.health.connect.client.records.ActiveCaloriesBurnedRecord::class)
                )
                permissionLauncher.launch(permissions)
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var topMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (currentRoute in listOf("dashboard", "history", "settings")) {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Watch My Calories") },
                    actions = {
                        IconButton(onClick = { topMenuExpanded = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = topMenuExpanded,
                            onDismissRequest = { topMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Scanned Menus") },
                                onClick = {
                                    topMenuExpanded = false
                                    navController.navigate("scannedMenus")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("About") },
                                onClick = {
                                    topMenuExpanded = false
                                    navController.navigate("about")
                                }
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentRoute in listOf("dashboard", "history", "settings")) {
                NavigationBar {
                    NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = currentRoute == "dashboard",
                    onClick = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.AddCircle, contentDescription = "Log Food") },
                    label = { Text("Log Food") },
                    selected = false,
                    onClick = { showLogFoodSheet = true }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History") },
                    label = { Text("History") },
                    selected = currentRoute == "history",
                    onClick = {
                        navController.navigate("history") {
                            popUpTo("dashboard")
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo("dashboard")
                        }
                    }
                )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    entries = todayEntries,
                    targetCalories = targetCalories,
                    burnedCalories = burnedCalories,
                    onLogFood = { showLogFoodSheet = true }
                )
            }
            composable("history") {
                HistoryScreen(
                    entries = allEntries,
                    onLogFood = { showLogFoodSheet = true },
                    onDeleteEntry = { id -> viewModel.deleteEntry(id) },
                    onEditEntry = { id -> navController.navigate("manualEntry?entryId=$id") },
                    onEditGroup = { imageId -> navController.navigate("editMealGroup/$imageId") }
                )
            }
            composable("scannedMenus") {
                ScannedMenusScreen(
                    scans = allMenuScans,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { id -> navController.navigate("scannedMenuDetail/$id") },
                    onScanNewMenu = {
                        menuPhotoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                )
            }
            composable(
                route = "scannedMenuDetail/{scanId}",
                arguments = listOf(androidx.navigation.navArgument("scanId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val scanId = backStackEntry.arguments?.getString("scanId")
                val scan = allMenuScans.find { it.id == scanId }
                if (scan != null) {
                    MenuScanDetailScreen(
                        scan = scan,
                        onNavigateBack = { navController.popBackStack() },
                        onDelete = { id ->
                            viewModel.deleteMenuScan(id)
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable("menuAnalysis") {
                val image = menuAnalysisImage
                if (image != null) {
                    MenuAnalysisScreen(
                        image = image,
                        geminiRepository = geminiRepository,
                        isMetric = isMetric,
                        onNavigateBack = { navController.popBackStack() },
                        onSaveScan = { scan ->
                            viewModel.addMenuScan(scan)
                            menuAnalysisImage = null
                            navController.navigate("scannedMenus") {
                                popUpTo("dashboard")
                            }
                        }
                    )
                }
            }
            composable("about") {
                AboutScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("camera") {
                CameraScreen(onPhotosCaptured = { bitmaps -> 
                    analysisImages = bitmaps
                    navController.navigate("analysis")
                })
            }
            composable("analysis") {
                val images = analysisImages
                if (images != null) {
                    AnalysisScreen(
                        images = images,
                        geminiRepository = geminiRepository,
                        isMetric = isMetric,
                        onNavigateBack = { navController.popBackStack() },
                        onSaveLog = { result ->
                            // Convert EstimationResult to FoodEntry entities
                            result.items.forEach { item ->
                                val entry = com.pning80.watchmycalories.data.FoodEntry(
                                    name = item.name,
                                    calories = item.calories,
                                    quantity = item.quantity,
                                    timestamp = System.currentTimeMillis(),
                                    protein = item.protein,
                                    carbs = item.carbs,
                                    fat = item.fat,
                                    imageId = null, // Future: save image locally
                                    mealName = result.mealName,
                                    mealTypeRaw = com.pning80.watchmycalories.data.MealType.fromTimestamp(System.currentTimeMillis()).displayName
                                )
                                viewModel.addEntry(entry)
                            }
                            analysisImages = null
                            navController.navigate("history") {
                                popUpTo("dashboard")
                            }
                        }
                    )
                }
            }
            composable(
                route = "manualEntry?entryId={entryId}",
                arguments = listOf(androidx.navigation.navArgument("entryId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val entryId = backStackEntry.arguments?.getString("entryId")
                val initialEntry = remember(entryId, allEntries) {
                    allEntries.find { it.id == entryId }
                }

                ManualEntryScreen(
                    initialEntry = initialEntry,
                    onSave = { entry ->
                        if (initialEntry != null) {
                            viewModel.updateEntry(entry)
                        } else {
                            viewModel.addEntry(entry)
                        }
                        AdManager.showInterstitialIfReady(context as android.app.Activity) {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(
                route = "editMealGroup/{imageId}",
                arguments = listOf(androidx.navigation.navArgument("imageId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val imageId = backStackEntry.arguments?.getString("imageId")
                val entriesInGroup = remember(imageId, allEntries) {
                    allEntries.filter { it.imageId == imageId }
                }

                if (entriesInGroup.isNotEmpty()) {
                    com.pning80.watchmycalories.ui.entry.EditMealGroupScreen(
                        entries = entriesInGroup,
                        onSave = { updated ->
                            viewModel.updateEntries(updated)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }
            }
            composable("settings") {
                SettingsScreen(
                    settingsDataStore = settingsDataStore,
                    currentProfile = userProfile,
                    onSaveProfile = { profile -> viewModel.saveProfile(profile) }
                )
            }
        }
    }

    // Log Food bottom sheet
    if (showLogFoodSheet) {
        LogFoodSheet(
            onScanFood = {
                showLogFoodSheet = false
                navController.navigate("camera")
            },
            onChooseFromLibrary = {
                showLogFoodSheet = false
                photoPickerLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            onLogManually = {
                showLogFoodSheet = false
                navController.navigate("manualEntry")
            },
            onDismiss = { showLogFoodSheet = false }
        )
    }
}
