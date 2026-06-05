package com.pning80.watchmycalories.ui.menuscanner

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.pning80.watchmycalories.ads.NativeAdView
import com.pning80.watchmycalories.utils.AccessibilityTags
import com.pning80.watchmycalories.ai.GeminiRepository
import com.pning80.watchmycalories.location.LocationData
import com.pning80.watchmycalories.location.LocationManager
import com.pning80.watchmycalories.ai.MenuAnalysisResult
import com.pning80.watchmycalories.ui.theme.Spacing
import androidx.compose.ui.platform.LocalContext
import com.pning80.watchmycalories.data.MenuScan
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuAnalysisScreen(
    image: Bitmap,
    geminiRepository: GeminiRepository,
    isMetric: Boolean,
    onNavigateBack: () -> Unit,
    /** Persist-only — called automatically once analysis succeeds (D-12 fix). */
    onSaveScan: (MenuScan) -> Unit,
    /** Called when the user taps Done on the saved-confirmation screen. */
    onDoneAfterSave: () -> Unit = onNavigateBack,
) {
    var isLoading by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<MenuAnalysisResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // True once a successful, non-empty analysis has been persisted. Prevents
    // the LaunchedEffect from saving twice on recomposition.
    var hasSavedScan by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val locationManager = remember { LocationManager(context) }

    LaunchedEffect(image) {
        isLoading = true
        val locationData = locationManager.getCurrentLocation()
        val coordsString = locationData.location?.let { "${it.latitude},${it.longitude}" }
        val response = geminiRepository.analyzeMenu(
            image = image,
            locality = locationData.locality,
            coordinates = coordsString,
            isMetric = isMetric
        )
        response.fold(
            onSuccess = { res ->
                result = res
                isLoading = false
                // Auto-save when the analysis yields items — mirrors iOS, which
                // persists on success without requiring an explicit Save tap.
                val items = res.items
                if (!hasSavedScan && items != null && items.isNotEmpty()) {
                    val imageID = com.pning80.watchmycalories.data.ImageStorage.newImageID()
                    com.pning80.watchmycalories.data.ImageStorage.saveJpeg(context, image, imageID)
                    val scan = MenuScan(
                        id = UUID.randomUUID().toString(),
                        restaurantName = res.restaurantName ?: "Unknown Restaurant",
                        imageID = imageID,
                        timestamp = System.currentTimeMillis(),
                        itemsData = Gson().toJson(items),
                    )
                    onSaveScan(scan)
                    hasSavedScan = true
                }
            },
            onFailure = { err ->
                // Never null — the error views are gated on errorMessage, and a
                // null-message failure (e.g. attestation/keystore) would
                // otherwise render a blank screen. The "not_a_menu" sentinel is
                // thrown with an explicit message so its branch still matches.
                errorMessage = err.message?.takeIf { it.isNotBlank() }
                    ?: "We couldn't analyze the menu. Please try again."
                isLoading = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Menu Analysis") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            )
        },
        bottomBar = {
            // Post-save: Scan Again + Done. Auto-save happens in LaunchedEffect.
            if (hasSavedScan) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.l),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Spacing.m),
                    ) {
                        Text("Scan Again", modifier = Modifier.padding(vertical = Spacing.xs))
                    }
                    Button(
                        onClick = onDoneAfterSave,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Spacing.m),
                    ) {
                        Text("Done", modifier = Modifier.padding(vertical = Spacing.xs))
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.l)
                        .testTag(AccessibilityTags.EstimationReview.LOADING_VIEW),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxl)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("Analyzing menu...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.weight(1f))
                    NativeAdView()
                }
            } else if (errorMessage == "not_a_menu") {
                // Dedicated "Not a Menu" state — mirrors iOS doc.questionmark UI.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(AccessibilityTags.EstimationReview.ERROR_VIEW),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.l),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp),
                        )
                        Text("Not a Menu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "This doesn't appear to be a restaurant menu.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag(AccessibilityTags.EstimationReview.TRY_AGAIN_BUTTON),
                        ) { Text("Try Again") }
                    }
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(AccessibilityTags.EstimationReview.ERROR_VIEW),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.l),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = "Error",
                            // Accent orange (iOS Color.orange), not Material error red.
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Analysis Failed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(errorMessage ?: "An error occurred.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        // Cancel + Try Again — mirrors iOS MenuAnalysisView.swift:309-336
                        // (both buttons). The Food estimation screen already has this
                        // pair via D-009; this brings the menu path to the same shape.
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                            OutlinedButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag(AccessibilityTags.EstimationReview.CANCEL_BUTTON),
                            ) { Text("Cancel") }
                            Button(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag(AccessibilityTags.EstimationReview.TRY_AGAIN_BUTTON),
                            ) { Text("Try Again") }
                        }
                    }
                }
            } else if (result?.items != null) {
                val items = result!!.items!!
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(AccessibilityTags.EstimationReview.NO_FOOD_VIEW),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                            Icon(Icons.Filled.Warning, contentDescription = "None", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                            Text("No Dishes Found", style = MaterialTheme.typography.titleLarge)
                            Text("We couldn't identify any menu items.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag(AccessibilityTags.EstimationReview.TRY_AGAIN_BUTTON),
                            ) { Text("Try Again") }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(Spacing.pageHorizontal),
                        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // iOS's MenuAnalysisView success result (MenuAnalysisView.swift:132-162)
                        // shows NO scanned-photo thumbnail — it goes straight from the header to
                        // "Looks like {name}" to the item cards. Android previously led with a
                        // 200dp menu Image, an undocumented Android-only addition; dropped to match
                        // iOS (same call as the iter-6 ScannedMenus FAB removal).
                        item {
                            Text(
                                // iOS shows "Looks like {name}" (MenuAnalysisView.swift:149),
                                // not the bare restaurant name.
                                "Looks like ${result?.restaurantName ?: "Unknown Restaurant"}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = Spacing.l, bottom = Spacing.s)
                            )
                        }

                        items(items) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(Spacing.l)
                            ) {
                                Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Text("~${item.calories.toInt()} cal", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    }

                                    if (!item.description.isNullOrBlank()) {
                                        Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    // iOS menuItemCard shows macros as columns —
                                    // "{value}g" over the full Protein/Carbs/Fat label
                                    // (MenuAnalysisView.swift:227); Android's live result
                                    // used a compact "30g P" inline form, diverging from
                                    // iOS AND the saved MenuScanDetail. Reuse MacroStat.
                                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.l), modifier = Modifier.padding(top = Spacing.xs)) {
                                        item.protein?.let { MacroStat("Protein", it) }
                                        item.carbs?.let { MacroStat("Carbs", it) }
                                        item.fat?.let { MacroStat("Fat", it) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
