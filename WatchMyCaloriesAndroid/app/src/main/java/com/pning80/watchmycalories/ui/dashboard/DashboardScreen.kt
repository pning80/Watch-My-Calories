package com.pning80.watchmycalories.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pning80.watchmycalories.data.FoodEntry
import com.pning80.watchmycalories.data.MealType
import com.pning80.watchmycalories.ui.components.EmptyStateCard
import com.pning80.watchmycalories.ui.components.HeroSummaryCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    entries: List<FoodEntry>,
    targetCalories: Double,
    burnedCalories: Double,
    onLogFood: () -> Unit
) {
    val todayEntries = entries.filter { com.pning80.watchmycalories.utils.TimeUtils.isToday(it.timestamp) }
    val groupedMeals = todayEntries.groupBy { MealType.fromRaw(it.mealTypeRaw) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── App Header ──
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("🔥", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Watch My Calories",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        // ── Content ──
        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp),
            modifier = Modifier.fillMaxWidth().testTag("DashboardLazyColumn")
        ) {
            item {
                HeroSummaryCard(
                    targetCalories = targetCalories,
                    burnedCalories = burnedCalories,
                    entries = todayEntries
                )
            }

            if (todayEntries.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onLogFood,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        EmptyStateCard()
                    }

                    // "or log manually" link
                    TextButton(
                        onClick = onLogFood,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_manualEntryLink")
                    ) {
                        Text(
                            "✏️ or log manually",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                // Meal sections
                MealType.displayOrder.forEach { mealType ->
                    val mealEntries = groupedMeals[mealType]
                    if (!mealEntries.isNullOrEmpty()) {
                        item {
                            // Section header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 20.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mealType.displayName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing
                                )
                                Text(
                                    text = "${mealEntries.sumOf { it.calories }.toInt()} kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Group by imageId within the meal
                        val groupedByImage = groupEntriesByImage(mealEntries)

                        items(groupedByImage) { group ->
                            if (group.size > 1) {
                                MealGroupItem(entries = group)
                            } else {
                                FoodEntryCard(entry = group.first())
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun groupEntriesByImage(entries: List<FoodEntry>): List<List<FoodEntry>> {
    val results = mutableListOf<List<FoodEntry>>()
    var currentGroup = mutableListOf<FoodEntry>()
    var currentImageId: String? = null

    for (entry in entries) {
        if (entry.imageId == null) {
            if (currentGroup.isNotEmpty()) {
                results.add(currentGroup)
                currentGroup = mutableListOf()
            }
            results.add(listOf(entry))
            currentImageId = null
        } else if (entry.imageId == currentImageId) {
            currentGroup.add(entry)
        } else {
            if (currentGroup.isNotEmpty()) {
                results.add(currentGroup)
            }
            currentGroup = mutableListOf(entry)
            currentImageId = entry.imageId
        }
    }
    if (currentGroup.isNotEmpty()) {
        results.add(currentGroup)
    }
    return results
}

@Composable
private fun MealGroupItem(entries: List<FoodEntry>) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Meal Scan (${entries.size} items)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "${entries.sumOf { it.calories }.toInt()} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            entries.forEach { entry ->
                Text(
                    "• ${entry.name}: ${entry.calories.toInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FoodEntryCard(entry: FoodEntry) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Initial badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = entry.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (entry.quantity.isNotBlank()) {
                    Text(
                        "• ${entry.quantity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Text(
            "${entry.calories.toInt()}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
