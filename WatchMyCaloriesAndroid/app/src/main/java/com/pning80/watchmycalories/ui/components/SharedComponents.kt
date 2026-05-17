package com.pning80.watchmycalories.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pning80.watchmycalories.data.FoodEntry
import com.pning80.watchmycalories.ui.theme.*
import kotlin.math.min

fun Modifier.cwCard() = this
    .padding(horizontal = 16.dp, vertical = 6.dp)
    .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
    .clip(RoundedCornerShape(24.dp))
    .background(Color.White)
    .padding(16.dp)

// ─── Hero Summary Card ───────────────────────────────────────────

@Composable
fun HeroSummaryCard(targetCalories: Double, burnedCalories: Double, entries: List<FoodEntry>) {
    val consumed = entries.sumOf { it.calories }
    val effectiveTarget = targetCalories + burnedCalories
    val progress = if (effectiveTarget > 0) min(consumed / effectiveTarget, 1.0) else 0.0
    val remaining = maxOf(0.0, effectiveTarget - consumed)
    val burnedProgress = if (effectiveTarget > 0) min(burnedCalories / effectiveTarget, 1.0) else 0.0

    // Macro totals
    val totalProtein = entries.mapNotNull { it.protein }.sum()
    val totalCarbs = entries.mapNotNull { it.carbs }.sum()
    val totalFat = entries.mapNotNull { it.fat }.sum()
    val hasMacros = totalProtein > 0 || totalCarbs > 0 || totalFat > 0

    val animatedProgress by animateFloatAsState(
        targetValue = progress.toFloat(),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
        label = "progress"
    )
    val animatedBurnedProgress by animateFloatAsState(
        targetValue = (progress + burnedProgress).toFloat().coerceAtMost(1f),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
        label = "burnedProgress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val accentColor = CwAccent

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp)
            .fillMaxWidth()
            .testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Dashboard.HERO_CARD)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Calorie Ring ──
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    val strokeWidth = 15.dp.toPx()
                    val diameter = size.minDimension
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter - strokeWidth, diameter - strokeWidth)

                    // Background ring
                    drawArc(
                        color = secondaryColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )

                    // Burned (orange) ring
                    if (burnedCalories > 0) {
                        drawArc(
                            color = accentColor,
                            startAngle = -90f,
                            sweepAngle = animatedBurnedProgress * 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Consumed (green) ring
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Center text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${consumed.toInt()}",
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        color = primaryColor,
                        modifier = Modifier.testTag("dashboard_consumedCalories")
                    )
                    Text(
                        text = "kcal",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // ── Stat Rows ──
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatRow(
                    label = "Goal",
                    value = "${targetCalories.toInt()}",
                    dotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    testTag = "dashboard_goalValue"
                )
                if (burnedCalories > 0) {
                    StatRow(
                        label = "Burned",
                        value = "${burnedCalories.toInt()}",
                        dotColor = accentColor
                    )
                }
                StatRow(
                    label = "Remaining",
                    value = "${remaining.toInt()}",
                    dotColor = secondaryColor,
                    testTag = "dashboard_remainingValue"
                )
            }
        }

        // ── Macro Breakdown ──
        if (hasMacros) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            MacroBreakdownRow(totalProtein, totalCarbs, totalFat)
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, dotColor: Color, testTag: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
    ) {
        Surface(
            shape = CircleShape,
            color = dotColor,
            modifier = Modifier.size(8.dp)
        ) {}
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─── Macro Components ────────────────────────────────────────────

@Composable
fun MacroBreakdownRow(protein: Double, carbs: Double, fat: Double) {
    val proteinCals = protein * 4
    val carbsCals = carbs * 4
    val fatCals = fat * 9
    val totalMacroCals = proteinCals + carbsCals + fatCals

    // Labels
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MacroLabel("Protein", protein, proteinCals, totalMacroCals, MaterialTheme.colorScheme.primary)
        MacroLabel("Carbs", carbs, carbsCals, totalMacroCals, CwAccent)
        MacroLabel("Fat", fat, fatCals, totalMacroCals, MaterialTheme.colorScheme.secondary)
    }

    // Proportional bar
    Spacer(modifier = Modifier.height(6.dp))
    MacroProportionalBar(proteinCals, carbsCals, fatCals)
}

@Composable
private fun MacroLabel(
    label: String,
    grams: Double,
    cals: Double,
    totalCals: Double,
    color: Color
) {
    val pct = if (totalCals > 0) ((cals / totalCals) * 100).toInt() else 0
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(shape = CircleShape, color = color, modifier = Modifier.size(8.dp)) {}
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Text(
            "${grams.toInt()}g",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "$pct%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun MacroProportionalBar(
    proteinCals: Double,
    carbsCals: Double,
    fatCals: Double,
    height: Int = 8
) {
    val total = maxOf(proteinCals + carbsCals + fatCals, 1.0)
    val pFraction = (proteinCals / total).toFloat()
    val cFraction = (carbsCals / total).toFloat()
    val fFraction = (fatCals / total).toFloat()

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Row(
        modifier = Modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape(4.dp)),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp)
    ) {
        if (proteinCals > 0) {
            Box(
                modifier = Modifier
                    .weight(pFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(primaryColor)
            )
        }
        if (carbsCals > 0) {
            Box(
                modifier = Modifier
                    .weight(cFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(CwAccent)
            )
        }
        if (fatCals > 0) {
            Box(
                modifier = Modifier
                    .weight(fFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(secondaryColor)
            )
        }
    }
}

// ─── Empty State Card ────────────────────────────────────────────

@Composable
fun EmptyStateCard() {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("📷", fontSize = 28.sp)
            }
        }

        Text(
            "No meals tracked yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Tap to scan your first meal.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
