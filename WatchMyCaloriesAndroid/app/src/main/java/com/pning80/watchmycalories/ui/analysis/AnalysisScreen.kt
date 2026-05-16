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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pning80.watchmycalories.ai.EstimationItem
import com.pning80.watchmycalories.ai.EstimationResult
import com.pning80.watchmycalories.ai.GeminiRepository

@Composable
fun AnalysisScreen(
    images: List<Bitmap>,
    geminiRepository: GeminiRepository,
    isMetric: Boolean,
    onNavigateBack: () -> Unit,
    onSaveLog: (EstimationResult) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var rawResult by remember { mutableStateOf<EstimationResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Editable state mapping
    var editableMealName by remember { mutableStateOf("") }
    val editableItems = remember { mutableStateListOf<EditableEstimationItem>() }

    LaunchedEffect(images) {
        isLoading = true
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
                title = { Text("Menu Analysis", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (!isLoading && rawResult != null && editableItems.isNotEmpty()) {
                Button(
                    onClick = {
                        val finalResult = EstimationResult(
                            mealName = editableMealName,
                            items = editableItems.map { it.toEstimationItem() }
                        )
                        onSaveLog(finalResult)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save to Log", modifier = Modifier.padding(vertical = 4.dp))
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("Analyzing food...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Analysis Failed",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$errorMessage",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onNavigateBack) {
                            Text("Try Again")
                        }
                    }
                }
            } else if (rawResult != null) {
                if (editableItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = "No Food",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Text("No Food Detected", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "We couldn't identify any food items.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onNavigateBack) {
                                Text("Try Again")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            // Thumbnail header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                images.take(3).forEach { bitmap ->
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Captured Image",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                modifier = Modifier.padding(top = 4.dp)
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

                        itemsIndexed(editableItems) { _, itemState ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = itemState.name,
                                        onValueChange = { itemState.name = it },
                                        label = { Text("Food Name") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
