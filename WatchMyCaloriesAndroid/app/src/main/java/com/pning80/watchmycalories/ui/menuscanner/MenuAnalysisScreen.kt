package com.pning80.watchmycalories.ui.menuscanner

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.pning80.watchmycalories.ai.GeminiRepository
import com.pning80.watchmycalories.location.LocationData
import com.pning80.watchmycalories.location.LocationManager
import com.pning80.watchmycalories.ai.MenuAnalysisResult
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
    onSaveScan: (MenuScan) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<MenuAnalysisResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
            },
            onFailure = { err ->
                errorMessage = err.message
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
            if (!isLoading && result?.items != null && result?.items!!.isNotEmpty()) {
                Button(
                    onClick = {
                        val scan = MenuScan(
                            id = UUID.randomUUID().toString(),
                            restaurantName = result?.restaurantName ?: "Unknown Restaurant",
                            imageId = null,
                            timestamp = System.currentTimeMillis(),
                            itemsJson = Gson().toJson(result?.items)
                        )
                        onSaveScan(scan)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save to Stored Menus", modifier = Modifier.padding(vertical = 4.dp))
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
                        Text("Analyzing menu...", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("Analysis Failed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        val text = if (errorMessage == "not_a_menu") "This doesn't look like a menu." else errorMessage ?: "An error occurred."
                        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onNavigateBack) { Text("Try Again") }
                    }
                }
            } else if (result?.items != null) {
                val items = result!!.items!!
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Filled.Warning, contentDescription = "None", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                            Text("No Dishes Found", style = MaterialTheme.typography.titleLarge)
                            Text("We couldn't identify any menu items.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = onNavigateBack) { Text("Try Again") }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Image(
                                bitmap = image.asImageBitmap(),
                                contentDescription = "Menu Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        item {
                            Text(
                                result?.restaurantName ?: "Unknown Restaurant",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }

                        items(items) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Text("~${item.calories.toInt()} cal", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    }

                                    if (!item.description.isNullOrBlank()) {
                                        Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 4.dp)) {
                                        item.protein?.let { Text("${it.toInt()}g P", style = MaterialTheme.typography.labelMedium) }
                                        item.carbs?.let { Text("${it.toInt()}g C", style = MaterialTheme.typography.labelMedium) }
                                        item.fat?.let { Text("${it.toInt()}g F", style = MaterialTheme.typography.labelMedium) }
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
