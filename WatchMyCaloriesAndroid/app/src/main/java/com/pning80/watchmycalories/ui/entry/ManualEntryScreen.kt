package com.pning80.watchmycalories.ui.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pning80.watchmycalories.data.FoodEntry
import com.pning80.watchmycalories.data.MealType
import com.pning80.watchmycalories.ui.theme.Spacing
import com.pning80.watchmycalories.utils.AccessibilityTags
import com.pning80.watchmycalories.ads.BannerAdView
import java.util.UUID

// Mirrors iOS's `String(format: "%g", value)` used to pre-populate the edit
// fields (Components.swift:893-898): drops the trailing ".0" on whole numbers
// so calories/macros show "300" not "300.0".
private fun Double.formatClean(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    initialEntry: FoodEntry? = null,
    isMetric: Boolean,
    onSave: (FoodEntry) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialEntry?.name ?: "") }
    var caloriesText by remember { mutableStateOf(initialEntry?.calories?.let { if (it > 0) it.formatClean() else "" } ?: "") }
    var quantity by remember { mutableStateOf(initialEntry?.quantity ?: "") }
    var mealType by remember { mutableStateOf(initialEntry?.mealTypeRaw?.let { MealType.fromRaw(it) } ?: MealType.fromTimestamp(System.currentTimeMillis())) }
    var showNutrition by remember { mutableStateOf(initialEntry?.protein != null || initialEntry?.carbs != null || initialEntry?.fat != null) }
    var proteinText by remember { mutableStateOf(initialEntry?.protein?.formatClean() ?: "") }
    var carbsText by remember { mutableStateOf(initialEntry?.carbs?.formatClean() ?: "") }
    var fatText by remember { mutableStateOf(initialEntry?.fat?.formatClean() ?: "") }

    val canSave = name.isNotBlank()
        && caloriesText.isNotBlank()
        && (caloriesText.toDoubleOrNull() ?: 0.0) > 0
        && quantity.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialEntry == null) "Log Food" else "Edit Food") },
                navigationIcon = {
                    // iOS uses a "Cancel" text button (DashboardView.swift:272-274),
                    // matching the Settings screen's Cancel. Replaced the Material
                    // close-"X" icon to mirror iOS and stay consistent within the app.
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag(AccessibilityTags.ManualEntry.CANCEL_BUTTON)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.titleSmall)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val entry = initialEntry?.copy(
                                name = name.trim(),
                                calories = caloriesText.trim().toDoubleOrNull() ?: 0.0,
                                quantity = quantity.trim(),
                                protein = proteinText.trim().toDoubleOrNull(),
                                carbs = carbsText.trim().toDoubleOrNull(),
                                fat = fatText.trim().toDoubleOrNull(),
                                mealTypeRaw = mealType.displayName
                            ) ?: FoodEntry(
                                id = UUID.randomUUID().toString(),
                                name = name.trim(),
                                calories = caloriesText.trim().toDoubleOrNull() ?: 0.0,
                                quantity = quantity.trim(),
                                timestamp = System.currentTimeMillis(),
                                protein = proteinText.trim().toDoubleOrNull(),
                                carbs = carbsText.trim().toDoubleOrNull(),
                                fat = fatText.trim().toDoubleOrNull(),
                                imageID = null,
                                mealName = null,
                                mealTypeRaw = mealType.displayName
                            )
                            onSave(entry)
                        },
                        enabled = canSave,
                        modifier = Modifier.testTag(AccessibilityTags.ManualEntry.SAVE_BUTTON)
                    ) {
                        Text("Save", style = MaterialTheme.typography.titleSmall)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(Spacing.pageHorizontal),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            BannerAdView(insetHorizontal = false)

            // Food Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                    Text("Food Details", style = MaterialTheme.typography.titleSmall)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Food name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag(AccessibilityTags.ManualEntry.FOOD_NAME)
                    )

                    OutlinedTextField(
                        value = caloriesText,
                        onValueChange = { caloriesText = it },
                        label = { Text("Calories") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag(AccessibilityTags.ManualEntry.CALORIES)
                    )

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = {
                            Text(
                                if (isMetric) "Quantity (e.g. 200 g, 250 ml)"
                                else "Quantity (e.g. 1 cup, 6 oz)"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag(AccessibilityTags.ManualEntry.QUANTITY)
                    )
                }
            }

            // Meal Type Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                    Text("Meal", style = MaterialTheme.typography.titleSmall)

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().testTag(AccessibilityTags.ManualEntry.MEAL_PICKER)) {
                        MealType.displayOrder.forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = mealType == type,
                                onClick = { mealType = type },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = MealType.displayOrder.size
                                ),
                                // iOS's segmented Picker shows no leading checkmark on
                                // the selected segment; suppress Material's default check.
                                icon = {},
                                modifier = Modifier.testTag("mealType_${type.displayName}")
                            ) {
                                Text(type.displayName, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Optional Nutrition Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                    // iOS uses DisclosureGroup("Nutrition Details (optional)")
                    // (DashboardView.swift:250): a constant label + a chevron that
                    // rotates on expand — not an "Add"/"Hide" text-button toggle.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNutrition = !showNutrition },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Nutrition Details (optional)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.rotate(if (showNutrition) 90f else 0f),
                        )
                    }

                    AnimatedVisibility(visible = showNutrition) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.s)
                        ) {
                            OutlinedTextField(
                                value = proteinText,
                                onValueChange = { proteinText = it },
                                label = { Text("Protein (g)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = carbsText,
                                onValueChange = { carbsText = it },
                                label = { Text("Carbs (g)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = fatText,
                                onValueChange = { fatText = it },
                                label = { Text("Fat (g)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
