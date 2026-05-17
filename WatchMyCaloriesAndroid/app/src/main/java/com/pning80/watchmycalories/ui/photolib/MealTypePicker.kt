package com.pning80.watchmycalories.ui.photolib

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import com.pning80.watchmycalories.data.MealType
import com.pning80.watchmycalories.utils.AccessibilityTags

/**
 * Capsule-style meal-type picker overlaid on a dark background, mirroring
 * iOS `MealTypePicker` in `CameraView.swift`. Used by both the camera capture
 * flow and the photo-library review flow.
 */
@Composable
fun MealTypePicker(
    selection: MealType,
    onSelect: (MealType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .testTag(AccessibilityTags.MealTypePicker.PICKER)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (type in MealType.displayOrder) {
            val isSelected = type == selection
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .pointerInput(type) {
                        detectTapGestures(onTap = { onSelect(type) })
                    },
            ) {
                Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}
