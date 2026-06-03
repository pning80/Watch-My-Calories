package com.pning80.watchmycalories.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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

/**
 * Card surface treatment — shadow, rounded clip, themed background, and
 * inner content padding. Callers are responsible for outer page padding
 * (typically via the surrounding LazyColumn's `contentPadding`) and for the
 * inter-card vertical gap (via `verticalArrangement.spacedBy`). PORT_AUDIT
 * Phase E (E2): padding-inside-only so vertical gaps don't double up.
 */
fun Modifier.cwCard(): Modifier = composed {
    // Use surfaceContainer so cards visibly lift off the background in dark
    // mode (where dropshadows on near-black are invisible). In light mode
    // surfaceContainer is close to surface so the visual change is minimal.
    val cardFill = MaterialTheme.colorScheme.surfaceContainer
    this
        .shadow(elevation = 4.dp, shape = RoundedCornerShape(Spacing.cardCorner))
        .clip(RoundedCornerShape(Spacing.cardCorner))
        .background(cardFill)
        .padding(Spacing.cardContent)
}

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
            .fillMaxWidth()
            .cwCard()
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
                    icon = Icons.Filled.Flag,
                    // outlineVariant instead of onSurface@0.4 — the transparent
                    // fill rendered as an ashy near-black circle on the dark
                    // surface; outlineVariant is a solid neutral that reads
                    // sharper (PR T dark-contrast pass). Its lightness flips
                    // between themes (dark #3A4540 / light #CAC4D0), so the icon
                    // tint must adapt too: onSurface is light-on-dark in dark
                    // mode and dark-on-pale in light mode — a fixed white icon
                    // would vanish on the pale light-theme fill.
                    badgeColor = MaterialTheme.colorScheme.outlineVariant,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    testTag = "dashboard_goalValue"
                )
                if (burnedCalories > 0) {
                    StatRow(
                        label = "Burned",
                        value = "${burnedCalories.toInt()}",
                        icon = Icons.Filled.LocalFireDepartment,
                        badgeColor = accentColor,
                        iconTint = Color.White
                    )
                }
                StatRow(
                    label = "Remaining",
                    value = "${remaining.toInt()}",
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    badgeColor = secondaryColor,
                    iconTint = primaryColor,
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
private fun StatRow(
    label: String,
    value: String,
    icon: ImageVector,
    badgeColor: Color,
    iconTint: Color,
    testTag: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
    ) {
        Surface(
            shape = CircleShape,
            color = badgeColor,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
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
        MacroLabel("Fat", fat, fatCals, totalMacroCals, CwMacroFat)
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
    // Fat segment = gray, mirroring iOS MacroProportionalBar (Components.swift:289
    // uses Color.secondary = system gray), not the brand sage.
    val fatColor = CwMacroFat

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
                    .background(fatColor)
            )
        }
    }
}

// ─── Empty State Card ────────────────────────────────────────────

@Composable
fun EmptyStateCard(
    title: String = "No meals tracked yet",
    subtitle: String = "Tap to scan your first meal.",
    icon: ImageVector = Icons.Filled.PhotoCamera,
) {
    // Dashed rounded border, transparent interior — mirrors iOS EmptyStateCard
    // (Components.swift:108-113: strokeBorder dash [5], no fill). Previously a
    // solid surface fill with no border.
    val dashColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val cardCornerPx = with(androidx.compose.ui.platform.LocalDensity.current) { Spacing.cardCorner.toPx() }
    val dashOnPx = with(androidx.compose.ui.platform.LocalDensity.current) { 5.dp.toPx() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = dashColor,
                    cornerRadius = CornerRadius(cardCornerPx, cardCornerPx),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashOnPx, dashOnPx)),
                    ),
                )
            }
            .padding(horizontal = Spacing.xxl, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp) // intra-card; not on the token grid
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    // iOS uses cwSecondary (light green) on the cwPrimary circle,
                    // not white — `secondary` is the themed match.
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
