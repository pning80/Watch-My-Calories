package com.pning80.watchmycalories.ui.menuscanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import com.pning80.watchmycalories.ui.components.EmptyStateCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pning80.watchmycalories.ads.BannerAdView
import com.pning80.watchmycalories.data.MenuScan
import com.pning80.watchmycalories.ui.theme.Spacing
import com.pning80.watchmycalories.utils.AccessibilityTags
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteScanRow(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(); true
            } else false
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannedMenusScreen(
    scans: List<MenuScan>,
    @Suppress("UNUSED_PARAMETER") onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onScanNewMenu: () -> Unit,
    onDeleteScan: (String) -> Unit = {},
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onScanNewMenu,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "Scan New Menu")
            }
        }
    ) { padding ->
        if (scans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(Spacing.pageHorizontal)
                    .testTag(AccessibilityTags.ScannedMenus.EMPTY_STATE),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = "No scanned menus yet",
                    subtitle = "Tap below to scan a menu and see options.",
                    icon = Icons.AutoMirrored.Filled.MenuBook
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(Spacing.pageHorizontal),
                verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                item {
                    BannerAdView()
                }

                items(scans, key = { it.id }) { scan ->
                    SwipeToDeleteScanRow(onDelete = { onDeleteScan(scan.id) }) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val thumbnail = remember(scan.imageID) {
                            scan.imageID?.let { com.pning80.watchmycalories.data.ImageStorage.getImageFile(context, it) }
                        }
                        val itemCount = remember(scan.itemsData) {
                            try {
                                val type = object : com.google.gson.reflect.TypeToken<List<com.pning80.watchmycalories.data.MenuItemResult>>() {}.type
                                (com.google.gson.Gson().fromJson<List<com.pning80.watchmycalories.data.MenuItemResult>>(scan.itemsData, type) ?: emptyList()).size
                            } catch (_: Exception) { 0 }
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onNavigateToDetail(scan.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.l),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.m)
                            ) {
                                if (thumbnail != null && thumbnail.exists()) {
                                    coil.compose.AsyncImage(
                                        model = thumbnail,
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = scan.restaurantName ?: "Unknown Restaurant",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault()).format(Date(scan.timestamp)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (itemCount > 0) {
                                        Text(
                                            text = "$itemCount ${if (itemCount == 1) "item" else "items"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
