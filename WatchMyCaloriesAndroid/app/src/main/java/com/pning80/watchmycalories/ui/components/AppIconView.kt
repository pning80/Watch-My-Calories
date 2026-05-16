package com.pning80.watchmycalories.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppIconView(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 10
) {
    Surface(
        shape = RoundedCornerShape(cornerRadius.dp),
        color = Color.White,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text("🔥", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        }
    }
}
