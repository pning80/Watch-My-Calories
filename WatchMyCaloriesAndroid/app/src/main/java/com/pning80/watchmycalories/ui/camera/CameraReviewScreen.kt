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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
        Icon(Icons.Filled.CheckCircle, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Use", fontWeight = FontWeight.SemiBold)
    }
}
