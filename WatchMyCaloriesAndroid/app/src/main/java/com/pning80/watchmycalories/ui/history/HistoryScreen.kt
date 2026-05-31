package com.pning80.watchmycalories.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pning80.watchmycalories.data.FoodEntry
import com.pning80.watchmycalories.data.MealType
import com.pning80.watchmycalories.ui.components.EmptyStateCard
import com.pning80.watchmycalories.ui.theme.Spacing
import com.pning80.watchmycalories.utils.AccessibilityTags
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// onLogFood is wired by the caller but the empty-state log-food action that iOS
// HistoryView.swift exposes is not yet surfaced in the Compose tree. Suppressed
// only for that parameter, not the active onDelete/onEdit callbacks below.
@Composable
fun HistoryScreen(
    entries: List<FoodEntry>,
    @Suppress("UNUSED_PARAMETER") onLogFood: () -> Unit,
    onDeleteEntry: ((String) -> Unit)? = null,
    onEditEntry: ((String) -> Unit)? = null,
    onEditGroup: ((String) -> Unit)? = null
) {
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val groupedEntries = entries.groupBy {
        sdf.parse(sdf.format(Date(it.timestamp)))?.time ?: 0L
    }.toList().sortedByDescending { it.first }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (groupedEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.pageHorizontal, vertical = 40.dp)
                    .testTag(AccessibilityTags.History.EMPTY_STATE),
                contentAlignment = Alignment.Center,
            ) {
                EmptyStateCard(
                    title = "No history yet",
                    subtitle = "Your tracked meals will appear here.",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = Spacing.pageHorizontal, end = Spacing.pageHorizontal, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
                modifier = Modifier.testTag("HistoryLazyColumn")
            ) {
                items(groupedEntries) { (dateMillis, dayEntries) ->
                    HistoryDayCard(
                        dateMillis = dateMillis,
                        entries = dayEntries,
                        onDeleteEntry = onDeleteEntry,
                        onEditEntry = onEditEntry,
                        onEditGroup = onEditGroup
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryDayCard(
    dateMillis: Long,
    entries: List<FoodEntry>,
    onDeleteEntry: ((String) -> Unit)? = null,
    onEditEntry: ((String) -> Unit)? = null,
    onEditGroup: ((String) -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    val totalCalories = entries.sumOf { it.calories }

    // Macro totals
    val totalProtein = entries.mapNotNull { it.protein }.sum()
    val totalCarbs = entries.mapNotNull { it.carbs }.sum()
    val totalFat = entries.mapNotNull { it.fat }.sum()
    val hasMacros = totalProtein > 0 || totalCarbs > 0 || totalFat > 0

    // Group by meal type
    val mealGroups = entries.groupBy { MealType.fromRaw(it.mealTypeRaw) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            // Per-card tag with the day's calorie total is the unique handle used by
            // HistoryScreenTest (Android extra). The cross-platform AccessibilityTags.History.DAY_CARD
            // is also exposed via a sibling Modifier on the inner Row below so iOS-shared
            // UI tests can still locate any day card.
            .testTag("HistoryDayCard_${totalCalories.toInt()}")
            .clickable { isExpanded = !isExpanded }
            .animateContentSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .testTag(AccessibilityTags.History.DAY_CARD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date(dateMillis)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(dateMillis)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${totalCalories.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "kcal",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.rotate(if (isExpanded) 90f else 0f)
                )
            }
        }

        // Compact macro row in header
        if (hasMacros) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
                    .testTag(AccessibilityTags.History.DAY_CARD_MACROS),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MacroChip("P", totalProtein, MaterialTheme.colorScheme.primary)
                MacroChip("C", totalCarbs, MaterialTheme.colorScheme.tertiary)
                MacroChip("F", totalFat, MaterialTheme.colorScheme.secondary)
            }
        }

        // Expanded: meal-grouped entries
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider()

                MealType.displayOrder.forEach { mealType ->
                    val mealEntries = mealGroups[mealType]
                    if (!mealEntries.isNullOrEmpty()) {
                        // Meal section header
                        Text(
                            text = mealType.displayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )

                        // D-005: group by mealName (or imageID fallback).
                        val grouped = com.pning80.watchmycalories.ui.components.groupEntriesByMealOrImage(mealEntries)

                        grouped.forEach { group ->
                            if (group.size > 1) {
                                // Grouped card logic
                                MealGroupCard(
                                    entries = group,
                                    onEditGroup = { onEditGroup?.invoke(group.first().imageID ?: "") }
                                )
                            } else {
                                val entry = group.first()
                                SwipeToDeleteRow(
                                    onDelete = { onDeleteEntry?.invoke(entry.id) }
                                ) {
                                    FoodEntryItem(
                                        entry = entry,
                                        onEdit = { onEditEntry?.invoke(entry.id) }
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private fun groupEntriesByImage(entries: List<FoodEntry>): List<List<FoodEntry>> {
    val results = mutableListOf<List<FoodEntry>>()
    var currentGroup = mutableListOf<FoodEntry>()
    var currentImageId: String? = null

    for (entry in entries) {
        if (entry.imageID == null) {
            if (currentGroup.isNotEmpty()) {
                results.add(currentGroup)
                currentGroup = mutableListOf()
            }
            results.add(listOf(entry))
            currentImageId = null
        } else if (entry.imageID == currentImageId) {
            currentGroup.add(entry)
        } else {
            if (currentGroup.isNotEmpty()) {
                results.add(currentGroup)
            }
            currentGroup = mutableListOf(entry)
            currentImageId = entry.imageID
        }
    }
    if (currentGroup.isNotEmpty()) {
        results.add(currentGroup)
    }
    return results
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FoodEntryItem(entry: FoodEntry, onEdit: () -> Unit, onDelete: () -> Unit = {}) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onEdit() },
                onLongClick = { menuOpen = true },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (entry.quantity.isNotBlank()) {
                    Text(
                        "• ${entry.quantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            // D-007: proportional macro bar (no literal "P: 10g" text).
            // Replaces the per-entry MacroChip row to match iOS UX.
            val p = entry.protein ?: 0.0
            val c = entry.carbs ?: 0.0
            val f = entry.fat ?: 0.0
            if (p > 0 || c > 0 || f > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                com.pning80.watchmycalories.ui.components.MacroProportionalBar(
                    proteinCals = p * 4,
                    carbsCals = c * 4,
                    fatCals = f * 9,
                    height = 6
                )
            }
        }
        Text(
            "${entry.calories.toInt()}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; onEdit() })
        DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
    }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MealGroupCard(
    entries: List<FoodEntry>,
    onEditGroup: (List<FoodEntry>) -> Unit,
    onDeleteGroup: (List<FoodEntry>) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val title = entries.firstOrNull()?.mealName?.takeIf { it.isNotBlank() }
        ?: "Meal Scan (${entries.size} items)"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = { menuOpen = true },
            )
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "${entries.size} items",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${entries.sumOf { it.calories }.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Icon(
                    androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.rotate(if (expanded) 90f else 0f)
                )
            }
        }
        if (expanded) {
            entries.forEach { entry ->
                Text(
                    "• ${entry.name}: ${entry.calories.toInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; onEditGroup(entries) })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDeleteGroup(entries) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError,
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        content = { content() },
    )
}

@Composable
private fun MacroChip(label: String, grams: Double, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Surface(
            shape = RoundedCornerShape(50),
            color = color,
            modifier = Modifier.size(6.dp)
        ) {}
        Text(
            "$label: ${grams.toInt()}g",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
