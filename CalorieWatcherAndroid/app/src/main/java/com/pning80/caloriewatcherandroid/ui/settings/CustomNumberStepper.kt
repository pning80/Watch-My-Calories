package com.pning80.caloriewatcherandroid.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pning80.caloriewatcherandroid.ui.theme.CWPrimary
import com.pning80.caloriewatcherandroid.ui.theme.CWSecondary

@Composable
fun CustomNumberStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
    range: IntRange? = null,
    label: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = if (label != null) "$value $label" else value.toString(),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = CWPrimary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )

        IconButton(
            onClick = {
                val newValue = value - step
                if (range == null || newValue in range) {
                    onValueChange(newValue)
                }
            },
            modifier = Modifier
                .size(36.dp)
                .background(CWSecondary, CircleShape)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease",
                tint = CWPrimary
            )
        }

        IconButton(
            onClick = {
                val newValue = value + step
                if (range == null || newValue in range) {
                    onValueChange(newValue)
                }
             },
            modifier = Modifier
                .size(36.dp)
                .background(CWSecondary, CircleShape)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase",
                tint = CWPrimary
            )
        }
    }
}