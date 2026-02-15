package com.pning80.caloriewatcherandroid.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pning80.caloriewatcherandroid.R
import com.pning80.caloriewatcherandroid.data.model.MealType
import com.pning80.caloriewatcherandroid.ui.components.FoodEntryCard
import com.pning80.caloriewatcherandroid.ui.components.HeroSummaryCard
import com.pning80.caloriewatcherandroid.ui.theme.MintSage
import com.pning80.caloriewatcherandroid.ui.theme.OrganicGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onCameraClick: () -> Unit,
    onEntryClick: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val groupedEntries = uiState.todayEntries.groupBy { it.mealType }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.headlineMedium, // Playfair
                            color = OrganicGreen
                        )
                        Text(
                            text = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.pning80.caloriewatcherandroid.ui.theme.Green10.copy(alpha=0.5f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCameraClick,
                containerColor = OrganicGreen,
                contentColor = MintSage,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Food", modifier = Modifier.size(28.dp))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            com.pning80.caloriewatcherandroid.ui.theme.Green10,
                            com.pning80.caloriewatcherandroid.ui.theme.Green20,
                            androidx.compose.ui.graphics.Color.White
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HeroSummaryCard(
                        consumed = uiState.totalConsumed,
                        target = uiState.targetCalories,
                        burned = uiState.burnedCalories
                    )
                }

                MealType.values().forEach { mealType ->
                    val entries = groupedEntries[mealType]
                    if (!entries.isNullOrEmpty()) {
                        item {
                            Text(
                                text = mealType.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                                style = MaterialTheme.typography.headlineSmall,
                                color = OrganicGreen,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }
                        items(entries) { entry ->
                            FoodEntryCard(
                                entry = entry,
                                onClick = { onEntryClick(entry.id) }
                            )
                        }
                    }
                }
                
                if (uiState.todayEntries.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ill_empty_dashboard),
                                contentDescription = "No food logged",
                                modifier = Modifier.size(200.dp).padding(bottom = 16.dp)
                            )
                            Text(
                                text = "Start your day fresh!",
                                style = MaterialTheme.typography.titleMedium,
                                color = com.pning80.caloriewatcherandroid.ui.theme.OrganicGreen
                            )
                            Text(
                                text = "Log your first meal to track calories.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(100.dp)) // Extra clearance
                }
            }
        }
    }
}
