package com.pning80.watchmycalories.ui.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pning80.watchmycalories.data.FoodEntry
import com.pning80.watchmycalories.data.MealType
import com.pning80.watchmycalories.ui.analysis.EditableEstimationItem
import com.pning80.watchmycalories.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMealGroupScreen(
    entries: List<FoodEntry>,
    onSave: (List<FoodEntry>) -> Unit,
    onCancel: () -> Unit
) {
    var mealName by remember { mutableStateOf(entries.firstOrNull()?.mealName ?: "") }
    // iOS EditMealGroupView's "Meal Info" section includes a meal-type segmented
    // picker (Components.swift:1053) so the whole meal can be re-bucketed; Android
    // was missing it (D-019). Track the group's meal type and write it to all items.
    var mealType by remember { mutableStateOf(MealType.fromRaw(entries.firstOrNull()?.mealTypeRaw)) }
    val editableItems = remember {
        mutableStateListOf<EditableEstimationItem>().apply {
            addAll(entries.map { entry ->
                EditableEstimationItem(com.pning80.watchmycalories.ai.EstimationItem(
                    name = entry.name,
                    quantity = entry.quantity,
                    calories = entry.calories,
                    protein = entry.protein,
                    carbs = entry.carbs,
                    fat = entry.fat,
                    confidence = 1.0
                ))
            })
        }
    }
    // Per-item "Nutrition (optional)" disclosure state — iOS shows the macros in a
    // collapsed disclosure per item (D-019); start collapsed, matching iOS + the
    // single-entry edit (ManualEntryScreen, iter-75).
    val nutritionExpanded = remember { mutableStateListOf(*Array(entries.size) { false }) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Meal Group", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val updatedEntries = entries.mapIndexed { index, original ->
                        val edited = editableItems[index].toEstimationItem()
                        original.copy(
                            name = edited.name,
                            quantity = edited.quantity,
                            calories = edited.calories,
                            protein = edited.protein,
                            carbs = edited.carbs,
                            fat = edited.fat,
                            mealName = mealName,
                            mealTypeRaw = mealType.displayName
                        )
                    }
                    onSave(updatedEntries)
                },
                modifier = Modifier.fillMaxWidth().padding(Spacing.l),
                shape = RoundedCornerShape(Spacing.m)
            ) {
                Text("Update All Items")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(Spacing.pageHorizontal),
            verticalArrangement = Arrangement.spacedBy(Spacing.l)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                    OutlinedTextField(
                        value = mealName,
                        onValueChange = { mealName = it },
                        // iOS EditMealGroupView labels this field "Meal Name"
                        // (Components.swift:926); "Meal Context" was an unclear,
                        // un-iOS Android-ism.
                        label = { Text("Meal Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Meal-type segmented — mirrors iOS Meal Info (D-019). No
                    // selected-checkmark, matching iOS + the other segmented controls.
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        MealType.displayOrder.forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = mealType == type,
                                onClick = { mealType = type },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = MealType.displayOrder.size
                                ),
                                icon = {},
                            ) {
                                Text(type.displayName, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            itemsIndexed(editableItems) { index, itemState ->
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
                        // iOS shows the per-item macros in a collapsed "Nutrition
                        // (optional)" disclosure (D-019). Match it: a constant label +
                        // rotating chevron that reveals the Protein/Carbs/Fat row.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { nutritionExpanded[index] = !nutritionExpanded[index] },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Nutrition (optional)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.rotate(if (nutritionExpanded[index]) 90f else 0f),
                            )
                        }
                        AnimatedVisibility(visible = nutritionExpanded[index]) {
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
            
            // Total Calories footer — mirrors iOS EditMealGroupView (D-019). Live
            // sum of the editable per-item calories; value in cwPrimary green.
            item {
                val totalCals = editableItems.sumOf { it.calories.toDoubleOrNull() ?: 0.0 }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.s),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Total Calories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${totalCals.toInt()} kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            item {
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
