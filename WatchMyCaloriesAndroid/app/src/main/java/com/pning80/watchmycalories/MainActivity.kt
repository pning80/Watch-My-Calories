package com.pning80.watchmycalories

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Phase 3 of PARITY_FIX_PLAN.md: intent-extra-driven test mode. When the
        // launching Intent contains EXTRA_UI_TESTING=true, TestSeed wipes Room
        // and applies the requested seed/consent state synchronously before the
        // first composition. Production launches don't carry the extra, so this
        // is a no-op. Mirrors iOS `--uitesting`+`--seed-*` launch args.
        TestSeed.applyIfTesting(this, intent)

        val settingsDataStore = SettingsDataStore(this)

        AdManager.initialize(this)

        lifecycleScope.launch {
            PlayIntegrityManager.ensureAttested(this@MainActivity)
        }
        
        setContent {
            val appTheme by settingsDataStore.appThemeFlow.collectAsState(initial = "System")
            val darkTheme = when (appTheme) {
                "Light" -> false
                "Dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            WatchMyCaloriesTheme(darkTheme = darkTheme) {
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val activityIntent = (context as? android.app.Activity)?.intent

    // Shared state for images to avoid Parcelable size limits in navigation
    var analysisImages by remember {
        // Test-only fast path: when EXTRA_START_AT_ANALYSIS is set, seed a stub
        // bitmap so the analysis route renders immediately (camera-bypass for
        // EstimationReviewParityTest). Production launches return null here.
        val initial: List<Bitmap>? = if (TestSeed.shouldStartAtAnalysis(activityIntent)) {
            listOf(TestSeed.stubBitmap())
        } else null
        mutableStateOf(initial)
    }
    // Raw bytes of the picked photo (kept around for EXIF extraction on the
    // review screen — Bitmap loses EXIF on decode).
    var photoLibraryReviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoLibraryReviewBytes by remember { mutableStateOf<ByteArray?>(null) }
    // User-chosen meal type from the review screen, flowed into Save (otherwise
    // null → falls back to MealType.fromTimestamp at save time).
    var chosenMealType by remember { mutableStateOf<com.pning80.watchmycalories.data.MealType?>(null) }
    val geminiRepository = remember {
        // Returns a Mock repo if the launching intent has the test-mode mock
        // extras; otherwise a real GeminiRepository. Production launches never
        // pass EXTRA_UI_TESTING so the test branch is unreachable in release.
        TestSeed.geminiRepositoryFor(context, activityIntent)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                // Read the raw bytes once; we need them for both Bitmap decode and
                // EXIF extraction (Bitmap.decode strips EXIF metadata).
                val rawBytes = context.contentResolver.openInputStream(it)?.use { stream ->
                    stream.readBytes()
                } ?: throw Exception("Could not open picked image")
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                photoLibraryReviewBitmap = bitmap
                photoLibraryReviewBytes = rawBytes
                // Reset any prior chosen meal type so the review screen recomputes
                // from EXIF for this new image.
                chosenMealType = null
                navController.navigate("photoLibraryReview")
            } catch (e: Exception) {
                Log.e("PhotoPicker", "Failed to load image", e)
            }
        }
    }

    // Shared state for Menu Analysis images
    var menuAnalysisImage by remember {
        // Mirror of analysisImages: pre-seed a stub Bitmap when EXTRA_START_AT_MENU_ANALYSIS
        // is set so the menuAnalysis route renders immediately (camera-bypass for
        // upcoming menu-analysis parity tests). Production: returns null.
        val initial: Bitmap? = if (TestSeed.shouldStartAtMenuAnalysis(activityIntent)) {
            TestSeed.stubBitmap()
        } else null
        mutableStateOf(initial)
    }
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

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            if (currentRoute in listOf("history", "scannedMenus")) {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        val titleText = when (currentRoute) {
                            "history" -> "History"
                            "scannedMenus" -> "Scanned Menus"
                            else -> ""
                        }
                        val titleTag = when (currentRoute) {
                            "history" -> "HistoryTitle"
                            "scannedMenus" -> "ScannedMenusTitle"
                            else -> null
                        }
                        Text(
                            text = titleText,
                            modifier = titleTag?.let {
                                androidx.compose.ui.Modifier.testTag(it)
                            } ?: androidx.compose.ui.Modifier
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                navController.navigate("settings") {
                                    popUpTo("dashboard")
                                }
                            },
                            modifier = androidx.compose.ui.Modifier.testTag(com.pning80.watchmycalories.utils.AccessibilityTags.AppMenu.MENU_BUTTON)
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentRoute in listOf("dashboard", "history", "settings", "scannedMenus")) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                NavigationBar {
                    NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = currentRoute == "dashboard",
                    onClick = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    },
                    colors = navItemColors,
                    modifier = androidx.compose.ui.Modifier.testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Tab.DASHBOARD),
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.AddCircle, contentDescription = "Log Food") },
                    label = { Text("Log Food") },
                    selected = false,
                    onClick = { showLogFoodSheet = true },
                    colors = navItemColors,
                    modifier = androidx.compose.ui.Modifier.testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Tab.CAMERA),
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Scan Menu") },
                    label = { Text("Scan Menu") },
                    selected = currentRoute == "scannedMenus",
                    onClick = {
                        navController.navigate("scannedMenus") {
                            popUpTo("dashboard")
                        }
                    },
                    colors = navItemColors,
                    modifier = androidx.compose.ui.Modifier.testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Tab.SCAN_MENU),
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History") },
                    label = { Text("History") },
                    selected = currentRoute == "history",
                    onClick = {
                        navController.navigate("history") {
                            popUpTo("dashboard")
                        }
                    },
                    colors = navItemColors,
                    modifier = androidx.compose.ui.Modifier.testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Tab.HISTORY),
                )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = when {
                TestSeed.shouldStartAtAnalysis(activityIntent) -> "analysis"
                TestSeed.shouldStartAtMenuAnalysis(activityIntent) -> "menuAnalysis"
                else -> "dashboard"
            },
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    entries = todayEntries,
                    targetCalories = targetCalories,
                    burnedCalories = burnedCalories,
                    onLogFood = { showLogFoodSheet = true },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onEditEntry = { id -> navController.navigate("manualEntry?entryId=$id") },
                    onDeleteEntry = { id -> viewModel.deleteEntry(id) },
                    onEditGroup = { imageID -> navController.navigate("editMealGroup/$imageID") },
                )
            }
            composable("history") {
                HistoryScreen(
                    entries = allEntries,
                    onLogFood = { showLogFoodSheet = true },
                    onDeleteEntry = { id -> viewModel.deleteEntry(id) },
                    onEditEntry = { id -> navController.navigate("manualEntry?entryId=$id") },
                    onEditGroup = { imageID -> navController.navigate("editMealGroup/$imageID") }
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
                    // Reset any prior meal type so the review screen defaults to now.
                    chosenMealType = null
                    navController.navigate("cameraReview")
                })
            }
            composable("cameraReview") {
                val images = analysisImages
                if (images != null) {
                    com.pning80.watchmycalories.ui.camera.CameraReviewScreen(
                        bitmaps = images,
                        settingsDataStore = settingsDataStore,
                        onRetake = {
                            // Drop captures + return to a fresh camera (popUpTo
                            // inclusive forces CameraScreen state to reset).
                            analysisImages = null
                            navController.navigate("camera") {
                                popUpTo("camera") { inclusive = true }
                            }
                        },
                        onUse = { mealType ->
                            chosenMealType = mealType
                            navController.navigate("analysis") {
                                popUpTo("dashboard")
                            }
                        },
                    )
                }
            }
            composable("photoLibraryReview") {
                val bmp = photoLibraryReviewBitmap
                if (bmp != null) {
                    com.pning80.watchmycalories.ui.photolib.PhotoLibraryReviewScreen(
                        bitmap = bmp,
                        rawBytesForExif = photoLibraryReviewBytes,
                        settingsDataStore = settingsDataStore,
                        onReselect = {
                            // Pop this screen, re-launch the picker.
                            navController.popBackStack()
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        onUse = { mealType ->
                            chosenMealType = mealType
                            analysisImages = listOf(bmp)
                            // Free the raw bytes now that we've forwarded.
                            photoLibraryReviewBytes = null
                            navController.navigate("analysis") {
                                popUpTo("dashboard")
                            }
                        },
                    )
                }
            }
            composable("analysis") {
                val images = analysisImages
                if (images != null) {
                    AnalysisScreen(
                        images = images,
                        geminiRepository = geminiRepository,
                        isMetric = isMetric,
                        initialMealType = chosenMealType
                            ?: com.pning80.watchmycalories.data.MealType.fromTimestamp(System.currentTimeMillis()),
                        onNavigateBack = { navController.popBackStack() },
                        onSaveLog = { result, pickedMealType ->
                            // Persist the first captured image (mirrors iOS one-JPEG-per-session).
                            // All entries from this capture share the same imageID — used for
                            // grouping in History/Dashboard and as the on-disk filename. T1.4.
                            val sharedImageID = images.firstOrNull()?.let { firstBitmap ->
                                val id = com.pning80.watchmycalories.data.ImageStorage.newImageID()
                                com.pning80.watchmycalories.data.ImageStorage.saveJpeg(context, firstBitmap, id)
                                id
                            }
                            // Meal type comes from the picker in AnalysisScreen, which itself
                            // defaults to chosenMealType (EXIF-derived from the photo-library flow)
                            // or now-based bucketing for direct camera capture.
                            val mealTypeRaw = pickedMealType.displayName
                            result.items.forEach { item ->
                                val entry = com.pning80.watchmycalories.data.FoodEntry(
                                    name = item.name,
                                    calories = item.calories,
                                    quantity = item.quantity,
                                    timestamp = System.currentTimeMillis(),
                                    protein = item.protein,
                                    carbs = item.carbs,
                                    fat = item.fat,
                                    imageID = sharedImageID,
                                    mealName = result.mealName,
                                    mealTypeRaw = mealTypeRaw,
                                )
                                viewModel.addEntry(entry)
                            }
                            // D-008: do not navigate here — AnalysisScreen shows the
                            // post-save confirmation. Navigation happens on the Done tap
                            // via onDoneAfterSave below.
                        },
                        onDoneAfterSave = {
                            analysisImages = null
                            photoLibraryReviewBitmap = null
                            chosenMealType = null
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = true }
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
                    isMetric = isMetric,
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
                route = "editMealGroup/{imageID}",
                arguments = listOf(androidx.navigation.navArgument("imageID") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val imageID = backStackEntry.arguments?.getString("imageID")
                val entriesInGroup = remember(imageID, allEntries) {
                    allEntries.filter { it.imageID == imageID }
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
                    onSaveProfile = { profile -> 
                        viewModel.saveProfile(profile)
                        navController.popBackStack()
                    },
                    onNavigateToAbout = { navController.navigate("about") },
                    onCancel = { navController.popBackStack() }
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
