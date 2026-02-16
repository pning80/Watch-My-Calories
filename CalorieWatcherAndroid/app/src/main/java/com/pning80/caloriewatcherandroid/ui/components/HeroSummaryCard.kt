package com.pning80.caloriewatcherandroid.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pning80.caloriewatcherandroid.ui.theme.*
import kotlin.math.max

@Composable
fun HeroSummaryCard(
    consumed: Int,
    target: Int,
    burned: Int = 0,
    modifier: Modifier = Modifier
) {
    val total = target + burned
    val progress = if (total > 0) (consumed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress = remember { Animatable(0f) }

    val remaining = max(0, total - consumed)

    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress,
            animationSpec = tween(durationMillis = 1500, delayMillis = 200)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = CWSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Circular Progress
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                val strokeWidthVal = 15.dp
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = strokeWidthVal.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeftOffset = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)
                    
                    // Background Ring
                    drawArc(
                        color = CWSecondary,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                    
                    // Foreground Ring
                    drawArc(
                        color = CWPrimary,
                        startAngle = -90f,
                        sweepAngle = animatedProgress.value * 360f,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$consumed",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = CWPrimary
                    )
                    Text(
                        text = "kcal",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Gray
                    )
                }
            }
            
            // Right: Stats Stack
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Goal
                StatRow(
                    label = "Goal",
                    value = "$target",
                    icon = Icons.Default.Flag,
                    bgColor = Color.Gray.copy(alpha=0.1f), // Gray tint
                    iconTint = Color.Gray
                )
                
                // Burned (Always show even if 0)
                StatRow(
                    label = "Burned",
                    value = "$burned",
                    icon = Icons.Default.LocalFireDepartment,
                    bgColor = CWAccent.copy(alpha=0.15f), // Accent tint
                    iconTint = CWAccent
                )
                
                // Remaining
                StatRow(
                    label = "Remaining",
                    value = "$remaining",
                    icon = Icons.Default.BarChart,
                    bgColor = CWSecondary,
                    iconTint = CWPrimary
                )
            }
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    icon: ImageVector,
    bgColor: Color,
    iconTint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = CWTextPrimary.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CWTextPrimary
            )
        }
    }
}
