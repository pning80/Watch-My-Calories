package com.pning80.watchmycalories.ui.analysis

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.pning80.watchmycalories.R
import com.pning80.watchmycalories.data.model.FoodEntry
import com.pning80.watchmycalories.data.model.MealType
import com.pning80.watchmycalories.ui.theme.CWPrimary
import com.pning80.watchmycalories.ui.theme.CWSurface
import com.pning80.watchmycalories.ui.theme.CWTextPrimary
import java.io.File
import java.util.*

@Composable
fun AnalysisScreen(
    imagePaths: List<String>,
    onAnalysisComplete: () -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDetails by remember { mutableStateOf(false) }

    LaunchedEffect(imagePaths) {
        if (uiState is AnalysisUiState.Idle && imagePaths.isNotEmpty()) {
            viewModel.analyzeImages(imagePaths)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is AnalysisUiState.Idle -> {
                Text("Ready to analyze...", color = Color.Gray)
            }
            is AnalysisUiState.Loading -> {
                LoadingView()
            }
            is AnalysisUiState.Error -> {
                ErrorView(
                    message = state.message,
                    apiKey = state.apiKeySuffix,
                    showDetails = showDetails,
                    onToggleDetails = { showDetails = !showDetails },
                    onRetry = { viewModel.analyzeImages(imagePaths) },
                    onCancel = onAnalysisComplete
                )
            }
            is AnalysisUiState.Success -> {
                SuccessView(
                    result = state.result,
                    imagePaths = imagePaths,
                    onDone = {
                        state.result.items.forEach { item ->
                            val imagePath = imagePaths.getOrNull(item.imageIndex) ?: imagePaths.firstOrNull()
                            val entry = FoodEntry(
                                name = item.name,
                                calories = item.calories,
                                quantity = item.quantity,
                                timestamp = System.currentTimeMillis(),
                                imagePath = imagePath,
                                protein = item.protein,
                                carbs = item.carbs,
                                fat = item.fat,
                                mealType = getMealTypeByTime()
                            )
                            viewModel.saveEntry(entry)
                        }
                        onAnalysisComplete()
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingView() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.ill_loading_analysis),
            contentDescription = "Analyzing",
            modifier = Modifier
                .size(200.dp)
                .scale(scale)
        )
        Spacer(modifier = Modifier.height(20.dp))
        CircularProgressIndicator(color = CWPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Analyzing Food...",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun ErrorView(
    message: String,
    apiKey: String?,
    showDetails: Boolean,
    onToggleDetails: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            modifier = Modifier.size(60.dp),
            tint = Color(0xFFFFA500)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Analysis Failed",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = CWTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We couldn't analyze the image. Please try again.",
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (showDetails) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Red.copy(alpha = 0.1f))
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    apiKey?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "API Key Used:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
        
        TextButton(onClick = onToggleDetails) {
            Text(if (showDetails) "Hide Details" else "Show Details", color = Color.Blue, fontSize = 12.sp)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = CWPrimary),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Try Again")
        }
        
        TextButton(onClick = onCancel) {
            Text("Cancel", color = Color.Gray)
        }
    }
}

@Composable
private fun SuccessView(
    result: com.pning80.watchmycalories.data.model.FoodAnalysisResult,
    imagePaths: List<String>,
    onDone: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            modifier = Modifier.size(64.dp),
            tint = CWPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Logged Successfully!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = CWTextPrimary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val totalCalories = result.items.sumOf { it.calories }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(result.items) { item ->
                val imagePath = imagePaths.getOrNull(item.imageIndex) ?: imagePaths.firstOrNull()
                FoodItemResultCard(item, imagePath)
            }
            
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Added", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${totalCalories.toInt()} kcal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE91E63) // CWAccent equivalent
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = CWPrimary),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Done")
        }
    }
}

@Composable
fun FoodItemResultCard(item: com.pning80.watchmycalories.data.model.IdentifiedFoodItem, imagePath: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CWSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imagePath != null) {
                Image(
                    painter = rememberAsyncImagePainter(File(imagePath)),
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                color = CWTextPrimary,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = "${item.calories.toInt()} kcal",
                fontWeight = FontWeight.Bold,
                color = CWPrimary
            )
        }
    }
}

fun getMealTypeByTime(): MealType {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 7..9 -> MealType.BREAKFAST
        in 11..14 -> MealType.LUNCH
        in 17..20 -> MealType.DINNER
        else -> MealType.SNACK
    }
}
