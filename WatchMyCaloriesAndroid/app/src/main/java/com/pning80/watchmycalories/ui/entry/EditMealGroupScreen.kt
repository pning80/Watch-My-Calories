package com.pning80.watchmycalories.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pning80.watchmycalories.data.FoodEntry
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
                            mealName = mealName
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
                OutlinedTextField(
                    value = mealName,
                    onValueChange = { mealName = it },
                    // iOS EditMealGroupView labels this field "Meal Name"
                    // (Components.swift:926); "Meal Context" was an unclear,
                    // un-iOS Android-ism.
                    label = { Text("Meal Name") },
                    modifier = Modifier.fillMaxWidth()
                )
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
            
            item {
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
