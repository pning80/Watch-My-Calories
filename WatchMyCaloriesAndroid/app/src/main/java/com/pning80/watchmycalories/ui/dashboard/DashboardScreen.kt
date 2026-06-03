package com.pning80.watchmycalories.ui.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.res.painterResource
import com.pning80.watchmycalories.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pning80.watchmycalories.data.FoodEntry
import com.pning80.watchmycalories.data.MealType
import com.pning80.watchmycalories.ads.BannerAdView
import com.pning80.watchmycalories.ui.components.EmptyStateCard
import com.pning80.watchmycalories.ui.components.HeroSummaryCard
import com.pning80.watchmycalories.ui.components.MacroProportionalBar
import com.pning80.watchmycalories.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    entries: List<FoodEntry>,
    targetCalories: Double,
    burnedCalories: Double,
    onLogFood: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onEditEntry: (String) -> Unit = {},
    onDeleteEntry: (String) -> Unit = {},
    onEditGroup: (String) -> Unit = {},
) {
    var appMenuOpen by remember { mutableStateOf(false) }
    val todayEntries = entries.filter { com.pning80.watchmycalories.utils.TimeUtils.isToday(it.timestamp) }
    val groupedMeals = todayEntries.groupBy { MealType.fromRaw(it.mealTypeRaw) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // ── App Header ──
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand mark — mirrors iOS Dashboard header
            // (DashboardView.swift:54-59 → `Image("MiniAppIcon")`). Shares the
            // same 1024.png source as the launcher icon.
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = "Watch My Calories logo",
                modifier = Modifier
                    .size(38.dp)
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Watch My Calories",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    // Mirror iOS `.dateTime.weekday(.wide).day().month()` — a
                    // locale-driven skeleton so en-US renders "SATURDAY, MAY 31"
                    // (month-before-day, abbreviated month) instead of the
                    // previous hardcoded `EEEE, d MMMM` ("SATURDAY, 31 MAY").
                    text = SimpleDateFormat(
                        android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEMMMd"),
                        Locale.getDefault()
                    ).format(Date()).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    // 0.7 (was 0.5) — on the dark `#0F1411` background, 0.5 alpha
                    // labelSmall sits at borderline WCAG contrast. Bumping to 0.7
                    // keeps the date legible in dark mode (PR T dark-contrast pass).
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            // App menu overflow — Settings + About (mirrors iOS AppMenuToolbar).
            Box {
            IconButton(
                onClick = { appMenuOpen = true },
                modifier = Modifier.testTag(com.pning80.watchmycalories.utils.AccessibilityTags.AppMenu.MENU_BUTTON)
            ) {
                // App-menu overflow — iOS uses `ellipsis.circle` (horizontal
                // dots in a circle); MoreVert is the Material equivalent and
                // is the conventional Android overflow affordance. A gear here
                // would mislead users into "Settings only" when the menu also
                // has About.
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "App menu",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            DropdownMenu(expanded = appMenuOpen, onDismissRequest = { appMenuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = { appMenuOpen = false; onNavigateToSettings() },
                )
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = { appMenuOpen = false; onNavigateToAbout() },
                )
            }
            }
        }

        // ── Content ──
        LazyColumn(
            contentPadding = PaddingValues(start = Spacing.pageHorizontal, end = Spacing.pageHorizontal, bottom = 100.dp),
            modifier = Modifier.fillMaxWidth().testTag("DashboardLazyColumn")
        ) {
            item {
                HeroSummaryCard(
                    targetCalories = targetCalories,
                    burnedCalories = burnedCalories,
                    entries = todayEntries
                )
            }

            // Banner ad — mirrors iOS Dashboard placement (between the
            // HeroSummaryCard and the meal sections / empty state). 24dp top
            // spacer matches iOS `VStack(spacing: 24)` (DashboardView.swift:52)
            // so the banner doesn't crowd the hero card.
            item {
                Spacer(modifier = Modifier.height(Spacing.xl))
                BannerAdView(insetHorizontal = false)
            }

            if (todayEntries.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onLogFood,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Dashboard.EMPTY_STATE_CARD)
                    ) {
                        EmptyStateCard()
                    }

                    // "or log manually" link
                    TextButton(
                        onClick = onLogFood,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Dashboard.MANUAL_ENTRY_LINK)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "or log manually",
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
                                    .testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Dashboard.MEAL_SECTION)
                                    .padding(top = Spacing.xl, bottom = Spacing.s),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    // iOS MealSection header (Components.swift:816-819):
                                    // title-case displayName, .title3 bold, cwTextPrimary.
                                    // Prominent heading — not the former tiny uppercase
                                    // green label (D-006 resolved 2026-06-02).
                                    text = mealType.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                // kcal pill — mirrors iOS MealSection (Components.swift:823-830):
                                // a light-green (cwSecondary) capsule with primary text,
                                // not plain low-alpha gray text.
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.secondary,
                                ) {
                                    Text(
                                        text = "${mealEntries.sumOf { it.calories }.toInt()} kcal",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs)
                                    )
                                }
                            }
                        }

                        // D-005: group by mealName (or imageID fallback) so multi-item
                        // meals collapse under a single card titled with the meal name.
                        val grouped = com.pning80.watchmycalories.ui.components.groupEntriesByMealOrImage(mealEntries)

                        items(grouped) { group ->
                            if (group.size > 1) {
                                MealGroupItem(
                                    entries = group,
                                    onEdit = {
                                        // Edit group by imageID when present, else by mealName via the first entry's id.
                                        val groupKey = group.first().imageID
                                        if (groupKey != null) onEditGroup(groupKey)
                                        else onEditEntry(group.first().id)
                                    },
                                    onDelete = { group.forEach { e -> onDeleteEntry(e.id) } },
                                )
                            } else {
                                val entry = group.first()
                                FoodEntryCard(
                                    entry = entry,
                                    onEdit = { onEditEntry(entry.id) },
                                    onDelete = { onDeleteEntry(entry.id) },
                                )
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
private fun MealGroupItem(
    entries: List<FoodEntry>,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val title = entries.firstOrNull()?.mealName?.takeIf { it.isNotBlank() }
        ?: "Meal Scan (${entries.size} items)"
    Card(
        modifier = Modifier
            .padding(vertical = Spacing.xs)
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = { menuOpen = true },
            )
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        // iOS group-card title is cwTextPrimary (Components.swift:606),
                        // not the secondary accent — onSurface matches and stays
                        // legible now that secondary is forest (D-011).
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${entries.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${entries.sumOf { it.calories }.toInt()} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.rotate(if (expanded) 90f else 0f)
                    )
                }
            }
            // Group macro bar — mirrors iOS grouped summaryRow (Components.swift:614):
            // the collapsed group summary shows the group-total macro proportions.
            val gpCals = entries.sumOf { it.protein ?: 0.0 } * 4
            val gcCals = entries.sumOf { it.carbs ?: 0.0 } * 4
            val gfCals = entries.sumOf { it.fat ?: 0.0 } * 9
            if (gpCals + gcCals + gfCals > 0) {
                MacroProportionalBar(proteinCals = gpCals, carbsCals = gcCals, fatCals = gfCals, height = 4)
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
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { menuOpen = false; onEdit() },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FoodEntryCard(
    entry: FoodEntry,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
    Row(
        modifier = Modifier
            .padding(vertical = 3.dp) // tight inter-entry gap; not on token grid
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .combinedClickable(
                onClick = { onEdit() },
                onLongClick = { menuOpen = true },
            )
            .padding(Spacing.m)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Photo thumbnail when available; otherwise initial badge with
        // a cwPrimary letter on the cwSecondary tile, mirroring iOS.
        val context = androidx.compose.ui.platform.LocalContext.current
        val imageFile = remember(entry.imageID) {
            entry.imageID?.let { com.pning80.watchmycalories.data.ImageStorage.getImageFile(context, it) }
        }
        // iOS draws a 48dp cwSecondary rounded-rect tile (corner 12) and, when a
        // photo exists, a 40dp Circle-clipped image centered on it; otherwise the
        // initial letter (Components.swift:457-477/580-599). Mirror that: the tile
        // is always the sage rounded-rect, the photo is a circle inside it (F2).
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            if (imageFile != null && imageFile.exists()) {
                coil.compose.AsyncImage(
                    model = imageFile,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = entry.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    // iOS avatar letter is cwPrimary on the cwSecondary tile
                    // (Components.swift:475/598) — mint on forest in dark,
                    // dark-green on pale in light, legible in both (F5/D-011).
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
            Text(
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            // Macro proportional bar — mirrors iOS FoodEntryGroupCard.summaryRow
            // (Components.swift:614): shown under name/time when the entry has
            // macro data. iOS shows no inline quantity on this card (it lives in
            // the Edit screen), so the "• quantity" text was dropped for parity.
            val pCals = (entry.protein ?: 0.0) * 4
            val cCals = (entry.carbs ?: 0.0) * 4
            val fCals = (entry.fat ?: 0.0) * 9
            if (pCals + cCals + fCals > 0) {
                Spacer(Modifier.height(4.dp))
                MacroProportionalBar(proteinCals = pCals, carbsCals = cCals, fatCals = fCals, height = 4)
            }
        }

        Text(
            "${entry.calories.toInt()}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
    DropdownMenu(
        expanded = menuOpen,
        onDismissRequest = { menuOpen = false }
    ) {
        DropdownMenuItem(
            text = { Text("Edit") },
            onClick = { menuOpen = false; onEdit() },
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = { menuOpen = false; onDelete() },
        )
    }
    }
}
