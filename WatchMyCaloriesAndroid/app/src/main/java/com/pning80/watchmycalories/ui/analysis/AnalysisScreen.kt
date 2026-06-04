package com.pning80.watchmycalories.ui.analysis

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pning80.watchmycalories.R
import kotlin.math.roundToInt
import com.pning80.watchmycalories.ads.NativeAdView
import com.pning80.watchmycalories.ai.EstimationItem
import com.pning80.watchmycalories.ai.EstimationResult
import com.pning80.watchmycalories.ai.GeminiRepository
import com.pning80.watchmycalories.ai.totalCalories
import com.pning80.watchmycalories.ai.totalProtein
import com.pning80.watchmycalories.ai.totalCarbs
import com.pning80.watchmycalories.ai.totalFat
import com.pning80.watchmycalories.data.MealType
import com.pning80.watchmycalories.ui.components.MacroBreakdownRow
import com.pning80.watchmycalories.ui.components.MacroProportionalBar
import com.pning80.watchmycalories.ui.theme.CwAccent
import com.pning80.watchmycalories.ui.theme.CwMacroFat
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
    // Guards the auto-save so a successful estimation is committed exactly once
    // (mirrors iOS, which saves in estimate() before showing the summary).
    var savedOnce by remember { mutableStateOf(false) }

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
                // Auto-save on success, then show a read-only summary — mirrors
                // iOS EstimationReviewView (saveToHistory runs inside estimate(),
                // and the result screen is a confirmation, not an editor).
                if (res.items.isNotEmpty() && !savedOnce) {
                    onSaveLog(res, initialMealType)
                    savedOnce = true
                }
                isLoading = false
            },
            onFailure = { err ->
                // Never leave this null: the error view is gated on
                // `errorMessage != null`, and some failures (e.g. an
                // attestation/keystore exception) carry a null `message`,
                // which would otherwise fall through to a blank screen.
                errorMessage = err.message?.takeIf { it.isNotBlank() }
                    ?: "We couldn't analyze the image. Please try again."
                isLoading = false
            }
        )
    }

    Scaffold(
        // iOS hides the nav bar on this screen (EstimationReviewView.swift:312-313) —
        // there is no "Analysis" title; the branded header / checkmark is the topmost
        // element. The background carries through the status-bar inset region.
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // The success summary is auto-saved; a compact, centered Done with a
            // divider above mirrors iOS EstimationReviewView.swift:296-304. The
            // error / no-food states carry their own inline buttons.
            if (!isLoading && errorMessage == null && rawResult?.items?.isNotEmpty() == true) {
                Column(
                    // navigationBarsPadding keeps the button clear of the gesture /
                    // nav bar (the Scaffold doesn't inset a custom bottomBar) — iOS's
                    // Done sits above the safe area; Android was flush to the edge.
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Button(
                        onClick = onDoneAfterSave,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(Spacing.l)
                            .testTag(AccessibilityTags.EstimationReview.DONE_BUTTON),
                        shape = RoundedCornerShape(Spacing.m)
                    ) {
                        Text("Done", modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.xs))
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
                        .padding(horizontal = Spacing.l)
                        .testTag(AccessibilityTags.EstimationReview.LOADING_VIEW),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Layout mirrors iOS EstimationReviewView.swift:38-165 — branded
                    // header at the top, the native ad directly beneath it (upper-
                    // middle), then a Spacer pushing the spinner + status text to the
                    // bottom. Android previously centered the spinner and dropped the
                    // ad at the very bottom, inverting the iOS arrangement.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                        modifier = Modifier.padding(top = Spacing.xxl, bottom = Spacing.xl)
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

                    // Native ad sits in the upper-middle, just under the header.
                    NativeAdView()

                    Spacer(modifier = Modifier.weight(1f))

                    // Spinner + status pinned near the bottom (iOS bottom block).
                    // iOS uses a large ProgressView + .headline text (EstimationReviewView.swift:81-86).
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(Spacing.m))
                    Text(
                        "Analyzing food...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xxl))
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
                val result = rawResult!!
                if (result.items.isEmpty()) {
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
                    // Read-only, auto-saved summary — mirrors iOS EstimationReviewView
                    // success view (checkmark + "Logged Successfully!" + a card per item
                    // + a "Total Added" macro-breakdown card). The editable Review & Edit
                    // step was dropped in favor of iOS parity (was the D-008 extra).
                    LazyColumn(
                        // 24 between sections matches iOS VStack(spacing: 24); the cards
                        // get an extra horizontal inset (iOS .padding() + .padding(.horizontal))
                        // applied per-item below, so they sit narrower than the title.
                        contentPadding = PaddingValues(
                            horizontal = Spacing.l,
                            vertical = Spacing.l,
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xxl),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(AccessibilityTags.EstimationReview.SUCCESS_VIEW),
                    ) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Spacing.m),
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Logged",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(top = Spacing.s)
                                        .size(64.dp)
                                )
                                Text(
                                    "Logged Successfully!",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }

                        items(result.items) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.l)
                                    .clip(RoundedCornerShape(Spacing.m))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(Spacing.l),
                                verticalArrangement = Arrangement.spacedBy(Spacing.s),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        item.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(Spacing.s))
                                    Text(
                                        "${item.calories.toInt()} kcal",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                CompactMacroRow(item.protein, item.carbs, item.fat)
                            }
                        }

                        item {
                            // "Total Added" card — iOS uses `Color.cwSecondary`
                            // (EstimationReviewView.swift:288): pale green in light,
                            // forest in dark. colorScheme.secondary now matches that
                            // exactly (D-011). secondaryContainer was wrong — it fell
                            // back to M3's lavender in light mode. Title text uses
                            // onSurface (= iOS cwTextPrimary); total is accent orange.
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.l)
                                    .clip(RoundedCornerShape(Spacing.l))
                                    .background(MaterialTheme.colorScheme.secondary)
                                    .padding(Spacing.l),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Spacing.s),
                            ) {
                                Text(
                                    "Total Added",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "${result.totalCalories.toInt()} kcal",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                                if (result.totalProtein > 0 || result.totalCarbs > 0 || result.totalFat > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = Spacing.xs),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    )
                                    MacroBreakdownRow(
                                        result.totalProtein,
                                        result.totalCarbs,
                                        result.totalFat,
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

/**
 * Per-item macro row for the result summary — mirrors iOS `CompactMacroRow`
 * (Components.swift:300): a thin proportional bar beside compact P/C/F grams + %.
 * Renders nothing when the item has no macro data.
 */
@Composable
private fun CompactMacroRow(protein: Double?, carbs: Double?, fat: Double?) {
    val p = protein ?: 0.0
    val c = carbs ?: 0.0
    val f = fat ?: 0.0
    if (p <= 0.0 && c <= 0.0 && f <= 0.0) return

    val proteinCals = p * 4
    val carbsCals = c * 4
    val fatCals = f * 9
    val total = (proteinCals + carbsCals + fatCals).coerceAtLeast(1.0)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            MacroProportionalBar(proteinCals, carbsCals, fatCals, height = 6)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            // Round (not truncate) to match iOS (Components.swift:244) + History.
            CompactMacroLabel("P", p, ((proteinCals / total) * 100).roundToInt(), MaterialTheme.colorScheme.primary)
            CompactMacroLabel("C", c, ((carbsCals / total) * 100).roundToInt(), CwAccent)
            CompactMacroLabel("F", f, ((fatCals / total) * 100).roundToInt(), CwMacroFat)
        }
    }
}

@Composable
private fun CompactMacroLabel(
    label: String,
    grams: Double,
    pct: Int,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = color,
            modifier = Modifier.padding(top = 3.dp).size(6.dp),
        ) {}
        Column {
            Text(
                "$label: ${grams.toInt()}g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Text(
                "$pct%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        }
    }
}
