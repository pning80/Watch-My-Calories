package com.pning80.watchmycalories.ui.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pning80.watchmycalories.data.FoodEntry
import com.pning80.watchmycalories.data.MealType
import com.pning80.watchmycalories.utils.AccessibilityTags
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    initialEntry: FoodEntry? = null,
    onSave: (FoodEntry) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialEntry?.name ?: "") }
    var caloriesText by remember { mutableStateOf(initialEntry?.calories?.let { if (it > 0) it.toString() else "" } ?: "") }
    var quantity by remember { mutableStateOf(initialEntry?.quantity ?: "") }
    var mealType by remember { mutableStateOf(initialEntry?.mealTypeRaw?.let { MealType.fromRaw(it) } ?: MealType.fromTimestamp(System.currentTimeMillis())) }
    var showNutrition by remember { mutableStateOf(initialEntry?.protein != null || initialEntry?.carbs != null || initialEntry?.fat != null) }
    var proteinText by remember { mutableStateOf(initialEntry?.protein?.toString() ?: "") }
    var carbsText by remember { mutableStateOf(initialEntry?.carbs?.toString() ?: "") }
    var fatText by remember { mutableStateOf(initialEntry?.fat?.toString() ?: "") }

    val canSave = name.isNotBlank()
        && caloriesText.isNotBlank()
        && (caloriesText.toDoubleOrNull() ?: 0.0) > 0
        && quantity.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialEntry == null) "Log Food" else "Edit Food") },
                navigationIcon = {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag(AccessibilityTags.ManualEntry.CANCEL_BUTTON)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Food Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        label = { Text("Quantity (e.g. 1 cup, 200 g)") },
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { showNutrition = !showNutrition }) {
                        Text(
                            if (showNutrition) "Hide Nutrition Details" else "Add Nutrition Details (optional)",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    AnimatedVisibility(visible = showNutrition) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
