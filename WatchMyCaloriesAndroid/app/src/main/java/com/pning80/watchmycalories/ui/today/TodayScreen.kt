package com.pning80.watchmycalories.ui.today

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pning80.watchmycalories.R
import com.pning80.watchmycalories.data.model.MealType
import com.pning80.watchmycalories.ui.components.FoodEntryCard
import com.pning80.watchmycalories.ui.components.HeroSummaryCard
import com.pning80.watchmycalories.ui.theme.CWBackground
import com.pning80.watchmycalories.ui.theme.CWPrimary
import com.pning80.watchmycalories.ui.theme.CWSecondary
import com.pning80.watchmycalories.ui.theme.CWTextPrimary
import com.pning80.watchmycalories.ui.theme.TitleFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodayScreen(
    onEntryClick: (String) -> Unit,
    onScanClick: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val groupedEntries = uiState.todayEntries.groupBy { it.mealType }
    val mealOrder = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK)

    // Refresh burned calories when the screen is shown
    LaunchedEffect(Unit) {
        viewModel.refreshBurnedCalories()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CWBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo_modern),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date()).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp
                            ),
                            color = Color.Gray
                        )
                        Text(
                            text = "Calorie Watcher",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = TitleFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = CWPrimary
                        )
                    }
                }
            }

            // Hero Card
            item {
                HeroSummaryCard(
                    consumed = uiState.totalConsumed,
                    target = uiState.targetCalories,
                    burned = uiState.burnedCalories,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            // Content
            if (uiState.todayEntries.isEmpty()) {
                item {
                    EmptyStateCard(onScanClick = onScanClick)
                }
            } else {
                mealOrder.forEach { mealType ->
                    val entries = groupedEntries[mealType]
                    if (!entries.isNullOrEmpty()) {
                        item {
                            MealSectionHeader(
                                title = mealType.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                                calories = entries.sumOf { it.calories }.toInt()
                            )
                        }
                        
                        items(entries) { entry ->
                            FoodEntryCard(
                                entry = entry,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onClick = { onEntryClick(entry.id) }
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MealSectionHeader(title: String, calories: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = CWTextPrimary
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Surface(
            color = CWSecondary,
            shape = CircleShape
        ) {
            Text(
                text = "$calories kcal",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = CWPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun EmptyStateCard(onScanClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(CWPrimary, CircleShape)
                .clickable { onScanClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Scan a new meal",
                tint = CWSecondary,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Text(
            text = "No meals tracked yet",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = CWTextPrimary
        )
        
        Text(
            text = "Tap the camera icon to scan your first meal.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}
