package com.pning80.watchmycalories.ui.components

import com.pning80.watchmycalories.data.FoodEntry

/**
 * Group consecutive entries that share a meal identity, mirroring the iOS
 * dashboard/history grouping (D-005). A "meal identity" is:
 *   - shared non-null `mealName`, OR (when mealName is null)
 *   - shared non-null `imageID`
 *
 * Two consecutive entries with the same non-null `mealName` always group
 * together regardless of their imageID. This matches the iOS data semantics
 * where `mealName` is the canonical group key — `imageID` is just the
 * persistence convention for camera-captured meals.
 *
 * Single-entry groups (the common case for manual entries) are returned as
 * `List<FoodEntry>` of size 1 so callers can render a normal entry card.
 */
fun groupEntriesByMealOrImage(entries: List<FoodEntry>): List<List<FoodEntry>> {
    if (entries.isEmpty()) return emptyList()
    val results = mutableListOf<MutableList<FoodEntry>>()
    var currentKey: String? = null

    for (entry in entries) {
        val entryKey = entry.mealName?.takeIf { it.isNotBlank() } ?: entry.imageID
        if (entryKey != null && entryKey == currentKey && results.isNotEmpty()) {
            results.last().add(entry)
        } else {
            results.add(mutableListOf(entry))
            currentKey = entryKey
        }
    }
    return results
}
