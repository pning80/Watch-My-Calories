package com.pning80.watchmycalories.ui.logfood

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pning80.watchmycalories.ads.BannerAdView
import com.pning80.watchmycalories.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogFoodSheet(
    onScanFood: () -> Unit,
    onChooseFromLibrary: () -> Unit,
    onLogManually: () -> Unit,
    onDismiss: () -> Unit
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
                text = "Log Food",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = Spacing.s)
            )

            // Icons mirror iOS LogFoodSheet.swift: camera.fill / photo.on.rectangle
            // / square.and.pencil → PhotoCamera / PhotoLibrary / Edit.
            LogOptionButton(
                title = "Scan Food",
                subtitle = "Take a photo of your meal",
                icon = Icons.Filled.PhotoCamera,
                onClick = onScanFood,
                testTag = "logFood_scanFood"
            )

            LogOptionButton(
                title = "Choose from Library",
                subtitle = "Select a photo from your library",
                icon = Icons.Filled.PhotoLibrary,
                onClick = onChooseFromLibrary,
                testTag = "logFood_chooseLibrary"
            )

            LogOptionButton(
                title = "Log Manually",
                subtitle = "Enter food details by hand",
                icon = Icons.Filled.Edit,
                onClick = onLogManually,
                testTag = "logFood_logManually"
            )

            // Banner ad — mirrors iOS LogFoodSheet.swift:56 (BannerAdView at the
            // bottom of the sheet). insetHorizontal=false: the sheet Column
            // already applies pageHorizontal padding.
            BannerAdView(insetHorizontal = false)
        }
    }
}

@Composable
private fun LogOptionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
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
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    // iOS LogFoodSheet.swift option title is .body .semibold — match it.
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
