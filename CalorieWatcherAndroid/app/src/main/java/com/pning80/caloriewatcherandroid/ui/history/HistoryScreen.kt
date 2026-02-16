package com.pning80.caloriewatcherandroid.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pning80.caloriewatcherandroid.R
import com.pning80.caloriewatcherandroid.ui.components.FoodEntryCard
import com.pning80.caloriewatcherandroid.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onEntryClick: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val history by viewModel.history.collectAsState()
    val expandedDates by viewModel.expandedDates.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CWBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
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
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = TitleFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = CWPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CWBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (history.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No entries yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                items(history) { summary ->
                    val isExpanded = expandedDates.contains(summary.date)
                    DailySummaryCard(
                        summary = summary,
                        isExpanded = isExpanded,
                        onToggle = { viewModel.toggleDate(summary.date) },
                        onEntryClick = onEntryClick
                    )
                }
            }
        }
    }
}

@Composable
fun DailySummaryCard(
    summary: DailySummary,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onEntryClick: (String) -> Unit
) {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val outputFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
    val date = inputFormat.parse(summary.date)
    val formattedDate = date?.let { outputFormat.format(it) } ?: summary.date

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CWTextPrimary
                )
                Text(
                    text = "${summary.totalCalories} kcal",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CWPrimary
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = CWTextPrimary,
                modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                summary.entries.forEach { entry ->
                    FoodEntryCard(
                        entry = entry,
                        onClick = { onEntryClick(entry.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
