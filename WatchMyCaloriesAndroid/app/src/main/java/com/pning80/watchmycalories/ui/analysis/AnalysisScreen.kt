package com.pning80.watchmycalories.ui.analysis

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pning80.watchmycalories.R
import com.pning80.watchmycalories.ads.NativeAdView
import com.pning80.watchmycalories.ai.EstimationItem
import com.pning80.watchmycalories.ai.EstimationResult
import com.pning80.watchmycalories.ai.GeminiRepository
import com.pning80.watchmycalories.data.MealType
import com.pning80.watchmycalories.ui.photolib.MealTypePicker
import com.pning80.watchmycalories.ui.theme.Spacing
import com.pning80.watchmycalories.utils.AccessibilityTags

@Composable
fun AnalysisScreen(
    images: List<Bitmap>,
    geminiRepository: GeminiRepository,
    isMetric: Boolean,
    initialMealType: MealType = MealType.fromTimestamp(System.currentTimeMillis()),
    onNavigateBack: () -> Unit,
    onSaveLog: (EstimationResult, MealType) -> Unit,
    onDoneAfterSave: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var rawResult by remember { mutableStateOf<EstimationResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // hasSaved switches the screen from Review & Edit to the post-save
    // confirmation view (D-008 — mirrors iOS "Logged Successfully!" + Done flow).
    var hasSaved by remember { mutableStateOf(false) }
    var savedTotalCalories by remember { mutableStateOf(0) }
    var savedItemCount by remember { mutableStateOf(0) }

    // Editable state mapping
    var editableMealName by remember { mutableStateOf("") }
    val editableItems = remember { mutableStateListOf<EditableEstimationItem>() }
    var selectedMealType by remember { mutableStateOf(initialMealType) }

    // Incrementing this re-fires the estimation LaunchedEffect — used by the
    // error view's Try Again button to retry without leaving the screen.
    var retryTrigger by remember { mutableStateOf(0) }
    // Reveals the raw error string in the error view (iOS "Show Details" toggle).
    var showErrorDetails by remember { mutableStateOf(false) }

    LaunchedEffect(images, retryTrigger) {
        isLoading = true
        errorMessage = null
        val response = geminiRepository.estimateCalories(images, isMetric)
        response.fold(
            onSuccess = { res ->
                rawResult = res
                editableMealName = res.mealName ?: "Meal"
                editableItems.clear()
                editableItems.addAll(res.items.map { EditableEstimationItem(it) })
                isLoading = false
            },
            onFailure = { err ->
                errorMessage = err.message
                isLoading = false
            }
        )
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Analysis", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            when {
                // Post-save confirmation: Done returns to dashboard.
                hasSaved -> {
                    Button(
                        onClick = onDoneAfterSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.l)
                            .testTag(AccessibilityTags.EstimationReview.DONE_BUTTON),
                        shape = RoundedCornerShape(Spacing.m)
                    ) {
                        Text("Done", modifier = Modifier.padding(vertical = Spacing.xs))
                    }
                }
                // Review & Edit: Save commits to DB and switches to confirmation.
                !isLoading && rawResult != null && editableItems.isNotEmpty() -> {
                    Button(
                        onClick = {
                            val finalResult = EstimationResult(
                                mealName = editableMealName,
                                items = editableItems.map { it.toEstimationItem() }
                            )
                            savedTotalCalories = finalResult.items.sumOf { it.calories }.toInt()
                            savedItemCount = finalResult.items.size
                            onSaveLog(finalResult, selectedMealType)
                            hasSaved = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .padding(Spacing.l)
                            .testTag(AccessibilityTags.EstimationReview.SAVE_BUTTON),
                        shape = RoundedCornerShape(Spacing.m)
                    ) {
                        Text("Save to Log", modifier = Modifier.padding(vertical = Spacing.xs))
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
            if (hasSaved) {
                // Post-save confirmation (D-008 — mirrors iOS "Logged Successfully!" + Total Added flow).
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(AccessibilityTags.EstimationReview.SUCCESS_VIEW),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.l),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Logged",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            "Logged Successfully!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Total Added",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "$savedTotalCalories kcal",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "$savedItemCount ${if (savedItemCount == 1) "item" else "items"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.l)
                        .testTag(AccessibilityTags.EstimationReview.LOADING_VIEW),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxl)
                ) {
                    // Branded loading header — mirrors iOS EstimationReviewView.swift:40-50
                    // (MiniAppIcon + serif "Watch My Calories" title above the spinner).
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                        modifier = Modifier.padding(top = Spacing.l)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_icon),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text(
                            "Watch My Calories",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("Analyzing food...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.weight(1f))
                    NativeAdView()
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(AccessibilityTags.EstimationReview.ERROR_VIEW),
                    contentAlignment = Alignment.Center,
                ) {
                    // Rate-limit (429) gets a distinct, softer treatment — clock
                    // icon + "Too Many Requests" + the server's message verbatim —
                    // mirroring iOS EstimationReviewView.swift:90-99.
                    val isRateLimited = errorMessage?.startsWith("Too many requests") == true
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.l),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            if (isRateLimited) Icons.Filled.Schedule else Icons.Filled.Warning,
                            contentDescription = "Error",
                            // Non-rate-limit error uses accent orange (iOS Color.orange),
                            // not the harsh Material error red; rate-limit uses primary mint.
                            tint = if (isRateLimited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            if (isRateLimited) "Too Many Requests" else "Analysis Failed",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            // iOS shows the server message for rate-limits, and a
                            // friendly generic line for everything else.
                            if (isRateLimited) (errorMessage ?: "") else "We couldn't analyze the image. Please try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Show Details toggle — reveals the raw error in a
                        // scrollable monospace box (iOS EstimationReviewView.swift:104-122).
                        if (!isRateLimited) {
                            if (showErrorDetails) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(Spacing.s),
                                    modifier = Modifier.heightIn(max = 120.dp)
                                ) {
                                    Text(
                                        errorMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(Spacing.m)
                                    )
                                }
                            }
                            TextButton(onClick = { showErrorDetails = !showErrorDetails }) {
                                Text(if (showErrorDetails) "Hide Details" else "Show Details")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                            // D-009: Cancel returns to the previous screen (camera or dashboard).
                            OutlinedButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag(AccessibilityTags.EstimationReview.CANCEL_BUTTON),
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = { retryTrigger++ },
                                modifier = Modifier.testTag(AccessibilityTags.EstimationReview.TRY_AGAIN_BUTTON),
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            } else if (rawResult != null) {
                if (editableItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(AccessibilityTags.EstimationReview.NO_FOOD_VIEW),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                            Icon(
                                // iOS uses fork.knife.circle for no-food — a soft
                                // "couldn't identify" cue, not a Warning triangle.
                                Icons.Filled.Restaurant,
                                contentDescription = "No Food",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Text("No Food Detected", style = MaterialTheme.typography.titleLarge)
                            Text(
                                // iOS: "...Try taking a clearer photo." (EstimationReviewView.swift:191)
                                "We couldn't identify any food items in this photo. Try taking a clearer photo.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                                // D-009: Cancel returns to the previous screen.
                                OutlinedButton(
                                    onClick = onNavigateBack,
                                    modifier = Modifier.testTag(AccessibilityTags.EstimationReview.CANCEL_BUTTON),
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = onNavigateBack,
                                    modifier = Modifier.testTag(AccessibilityTags.EstimationReview.TRY_AGAIN_BUTTON),
                                ) {
                                    Text("Try Again")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(Spacing.pageHorizontal),
                        verticalArrangement = Arrangement.spacedBy(Spacing.l),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(AccessibilityTags.EstimationReview.EDIT_VIEW),
                    ) {
                        item {
                            // Thumbnail header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
                            ) {
                                images.take(3).forEach { bitmap ->
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Captured Image",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(Spacing.s)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Success",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    "Review & Edit",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Text(
                                "Make sure the estimated quantities and macros are correct.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = Spacing.xs)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = editableMealName,
                                onValueChange = { editableMealName = it },
                                label = { Text("Meal Context") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                                Text(
                                    "Meal Type",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.padding(vertical = Spacing.xs),
                                ) {
                                    MealTypePicker(
                                        selection = selectedMealType,
                                        onSelect = { selectedMealType = it },
                                        // 6.dp is off-token and intentional — tighter MealTypePicker inner padding
                                        modifier = Modifier.padding(vertical = 6.dp),
                                    )
                                }
                            }
                        }

                        itemsIndexed(editableItems) { _, itemState ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(Spacing.l),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                                    OutlinedTextField(
                                        value = itemState.name,
                                        onValueChange = { itemState.name = it },
                                        label = { Text("Food Name") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                                        OutlinedTextField(
                                            value = itemState.quantity,
                                            onValueChange = { itemState.quantity = it },
                                            label = { Text("Quantity") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = itemState.calories,
                                            onValueChange = { itemState.calories = it },
                                            label = { Text("Calories") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                                        OutlinedTextField(
                                            value = itemState.protein,
                                            onValueChange = { itemState.protein = it },
                                            label = { Text("Protein (g)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = itemState.carbs,
                                            onValueChange = { itemState.carbs = it },
                                            label = { Text("Carbs (g)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = itemState.fat,
                                            onValueChange = { itemState.fat = it },
                                            label = { Text("Fat (g)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
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

class EditableEstimationItem(initial: EstimationItem) {
    var name by mutableStateOf(initial.name)
    var quantity by mutableStateOf(initial.quantity)
    var calories by mutableStateOf(initial.calories.toInt().toString())
    var protein by mutableStateOf(initial.protein?.toInt()?.toString() ?: "")
    var carbs by mutableStateOf(initial.carbs?.toInt()?.toString() ?: "")
    var fat by mutableStateOf(initial.fat?.toInt()?.toString() ?: "")
    val confidence = initial.confidence

    fun toEstimationItem(): EstimationItem {
        return EstimationItem(
            name = name.takeIf { it.isNotBlank() } ?: "Unknown",
            quantity = quantity.takeIf { it.isNotBlank() } ?: "1 serving",
            calories = calories.toDoubleOrNull() ?: 0.0,
            protein = protein.toDoubleOrNull(),
            carbs = carbs.toDoubleOrNull(),
            fat = fat.toDoubleOrNull(),
            confidence = confidence
        )
    }
}
