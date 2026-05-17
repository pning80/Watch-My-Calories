package com.pning80.watchmycalories.ui.photolib

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import com.pning80.watchmycalories.data.MealType
import com.pning80.watchmycalories.ui.settings.SettingsDataStore
import com.pning80.watchmycalories.utils.AccessibilityTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Photo-library review step — mirrors iOS `PhotoLibraryReviewView` (T1.1).
 *
 * Flow:
 *   1. User picks a photo from the OS picker (MainActivity wires the picker).
 *   2. This screen shows the chosen photo full-bleed with a MealTypePicker overlay
 *      defaulted from the photo's EXIF DateTimeOriginal (falls back to "now").
 *   3. First time the user reaches this screen with a chosen image, the one-time
 *      [CalorieDisclaimerSheet] appears.
 *   4. Reselect → caller's [onReselect] (typically re-launches the picker).
 *   5. Use → caller's [onUse] with the chosen MealType, advancing to analysis.
 *
 * Image and EXIF bytes come in pre-decoded from MainActivity (which already
 * handles the URI → Bitmap step for the picker callback).
 */
@Composable
fun PhotoLibraryReviewScreen(
    bitmap: Bitmap,
    rawBytesForExif: ByteArray?,
    settingsDataStore: SettingsDataStore,
    onReselect: () -> Unit,
    onUse: (MealType) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val hasSeenDisclaimer by settingsDataStore.hasSeenEstimateDisclaimerFlow.collectAsState(initial = true)

    var selectedMealType by remember {
        mutableStateOf(MealType.fromTimestamp(extractDateMillis(rawBytesForExif) ?: System.currentTimeMillis()))
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
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Selected photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

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
                ReselectButton(onClick = onReselect)
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
private fun ReselectButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.testTag(AccessibilityTags.PhotoLibrary.CHOOSE_AGAIN_BUTTON),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.25f),
            contentColor = Color.White,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Icon(Icons.Filled.Refresh, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Reselect", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun UseButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.testTag(AccessibilityTags.PhotoLibrary.USE_PHOTO_BUTTON),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Use", fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Extract the photo's DateTimeOriginal EXIF tag (in millis), or null if absent.
 * Mirrors iOS `extractCreationDate` in PhotoLibraryReviewView.swift.
 */
private fun extractDateMillis(bytes: ByteArray?): Long? {
    if (bytes == null || bytes.isEmpty()) return null
    return try {
        val exif = ExifInterface(bytes.inputStream() as InputStream)
        val tag = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: return null
        // EXIF format: "yyyy:MM:dd HH:mm:ss"
        val fmt = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
        fmt.parse(tag)?.time
    } catch (_: Exception) {
        null
    }
}

