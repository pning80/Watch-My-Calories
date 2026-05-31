package com.pning80.watchmycalories.ui.menuscanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pning80.watchmycalories.ui.theme.Spacing
import com.pning80.watchmycalories.utils.AccessibilityTags

/**
 * Modal sheet for the Scan Menu tab — mirrors iOS `ScanMenuSheet` (closes D-002).
 * Three actions: Scan with Camera, Choose from Library, Stored Menus.
 *
 * Sibling of `LogFoodSheet` (which is the food-side equivalent on the Log Food tab).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanMenuSheet(
    onScanCamera: () -> Unit,
    onChooseFromLibrary: () -> Unit,
    onStoredMenus: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = Spacing.cardCorner, topEnd = Spacing.cardCorner),
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.pageHorizontal)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)
        ) {
            Text(
                text = "Scan Menu",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = Spacing.s)
            )

            SheetOption(
                title = "Scan with Camera",
                subtitle = "Take a photo of the menu",
                icon = Icons.Default.Search,
                onClick = onScanCamera,
                testTag = AccessibilityTags.ScanMenuSheet.SCAN_BUTTON,
            )

            SheetOption(
                title = "Choose from Library",
                subtitle = "Select a menu photo from your library",
                icon = Icons.Default.Add,
                onClick = onChooseFromLibrary,
                testTag = AccessibilityTags.ScanMenuSheet.CHOOSE_FROM_LIBRARY_BUTTON,
            )

            SheetOption(
                title = "Stored Menus",
                subtitle = "View previously scanned menus",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                onClick = onStoredMenus,
                testTag = AccessibilityTags.ScanMenuSheet.STORED_MENUS_BUTTON,
            )
        }
    }
}

@Composable
private fun SheetOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(Spacing.l)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.l)
        ) {
            Surface(
                shape = RoundedCornerShape(Spacing.m),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
