package com.pning80.watchmycalories.ui.camera

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pning80.watchmycalories.data.MealType
import com.pning80.watchmycalories.ui.photolib.CalorieDisclaimerSheet
import com.pning80.watchmycalories.ui.photolib.MealTypePicker
import com.pning80.watchmycalories.ui.settings.SettingsDataStore
import com.pning80.watchmycalories.utils.AccessibilityTags
import kotlinx.coroutines.launch

/**
 * Camera capture review step — mirrors iOS `CameraView` post-capture review
 * and matches the [com.pning80.watchmycalories.ui.photolib.PhotoLibraryReviewScreen]
 * pattern. Inserted between [CameraScreen] and `AnalysisScreen` so the user
 * picks the meal type before the Gemini call starts (PORT_AUDIT.md C1).
 *
 * Default meal type is now (camera capture is just-now); no EXIF fallback like
 * the photo-library flow has.
 */
@Composable
fun CameraReviewScreen(
    bitmaps: List<Bitmap>,
    settingsDataStore: SettingsDataStore,
    onRetake: () -> Unit,
    onUse: (MealType) -> Unit,
    onCancel: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val hasSeenDisclaimer by settingsDataStore.hasSeenEstimateDisclaimerFlow.collectAsState(initial = true)

    var selectedMealType by remember {
        mutableStateOf(MealType.fromTimestamp(System.currentTimeMillis()))
    }
    var showDisclaimer by remember { mutableStateOf(false) }

    LaunchedEffect(hasSeenDisclaimer) {
        if (!hasSeenDisclaimer) showDisclaimer = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        bitmaps.firstOrNull()?.let { firstBitmap ->
            Image(
                bitmap = firstBitmap.asImageBitmap(),
                contentDescription = "Captured photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // Bottom scrim so the translucent-white capsules + Retake button stay
        // legible over a bright photo. Mirrors the iOS camera gradient
        // (CameraView.swift:95 — clear → black 0.8); iOS's review screen relies
        // on the same dark treatment, which Android was missing here.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                    ),
                ),
        )

        // Top scrim so the Cancel button stays legible over a bright photo
        // (mirrors the iOS camera preview top gradient, CameraView.swift:91).
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent),
                    ),
                ),
        )

        // Cancel — dismisses the whole capture flow. iOS shows this persistently
        // as the NavigationStack toolbar's leading "Cancel" (ContentView.swift:204)
        // throughout both the preview and this review state, tinted cwPrimary
        // (the root .tint, ContentView.swift:62). Android had no way out of the
        // review except Retake; this restores the parity exit.
        TextButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(horizontal = 8.dp)
                .testTag(AccessibilityTags.Camera.CANCEL_BUTTON),
        ) {
            Text(
                "Cancel",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (bitmaps.size > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.55f),
            ) {
                Text(
                    text = "${bitmaps.size} photos",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MealTypePicker(
                selection = selectedMealType,
                onSelect = { selectedMealType = it },
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                RetakeButton(onClick = onRetake)
                Spacer(Modifier.width(40.dp))
                UseButton(onClick = { onUse(selectedMealType) })
            }
        }
    }

    if (showDisclaimer) {
        CalorieDisclaimerSheet(
            onContinue = { dontShowAgain ->
                if (dontShowAgain) {
                    scope.launch { settingsDataStore.setSeenEstimateDisclaimer(true) }
                }
                showDisclaimer = false
            },
        )
    }
}

@Composable
private fun RetakeButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.testTag(AccessibilityTags.Camera.RETAKE_BUTTON),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.25f),
            contentColor = Color.White,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Icon(Icons.Filled.Refresh, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Retake", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun UseButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.testTag(AccessibilityTags.Camera.USE_PHOTO_BUTTON),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            // iOS Use button is cwAccent orange (CameraView.swift:78).
            // `tertiary` is the themed accent token.
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = Color.White,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Icon(Icons.Filled.Check, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Use", fontWeight = FontWeight.SemiBold)
    }
}
