package com.pning80.watchmycalories.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pning80.watchmycalories.data.CalorieCalculator
import com.pning80.watchmycalories.data.CalorieCalculator.Gender
import com.pning80.watchmycalories.data.CalorieCalculator.ActivityLevel
import com.pning80.watchmycalories.data.UserProfile
import com.pning80.watchmycalories.ui.settings.SettingsDataStore
import com.pning80.watchmycalories.ui.theme.Spacing
import com.pning80.watchmycalories.utils.AccessibilityTags
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(
    settingsDataStore: SettingsDataStore,
    onComplete: (UserProfile) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Profile state
    var heightCm by remember { mutableIntStateOf(173) }
    var weightKg by remember { mutableIntStateOf(68) }
    var age by remember { mutableIntStateOf(30) }
    var gender by remember { mutableStateOf(Gender.MALE) }
    var activityLevel by remember { mutableStateOf(ActivityLevel.SEDENTARY) }
    var targetCaloriesText by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(targetState = currentStep, label = "onboarding") { step ->
            when (step) {
                0 -> WelcomeStep(onNext = { currentStep = 1 })
                1 -> PrivacyStep(
                    settingsDataStore = settingsDataStore,
                    onNext = { currentStep = 2 }
                )
                2 -> GoalStep(
                    heightCm = heightCm,
                    weightKg = weightKg,
                    age = age,
                    gender = gender,
                    activityLevel = activityLevel,
                    targetCaloriesText = targetCaloriesText,
                    onHeightChanged = { heightCm = it },
                    onWeightChanged = { weightKg = it },
                    onAgeChanged = { age = it },
                    onGenderChanged = { gender = it },
                    onActivityChanged = { activityLevel = it },
                    onCaloriesTextChanged = { targetCaloriesText = it },
                    onFinish = {
                        val target = targetCaloriesText.toDoubleOrNull() ?: 2000.0
                        val profile = UserProfile(
                            id = 1,
                            height = heightCm.toDouble(),
                            weight = weightKg.toDouble(),
                            age = age,
                            genderRaw = gender.displayName,
                            activityLevelRaw = activityLevel.displayName,
                            targetCalories = target
                        )
                        coroutineScope.launch {
                            settingsDataStore.setOnboardingCompleted(true)
                        }
                        onComplete(profile)
                    }
                )
            }
        }

        // Skip button
        TextButton(
            onClick = {
                coroutineScope.launch { settingsDataStore.setOnboardingCompleted(true) }
                val defaultProfile = UserProfile(
                    id = 1, height = 173.0, weight = 68.0, age = 30,
                    genderRaw = "Male", activityLevelRaw = "Sedentary",
                    targetCalories = 2000.0
                )
                onComplete(defaultProfile)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(Spacing.l)
                .testTag(AccessibilityTags.Onboarding.SKIP_BUTTON)
        ) {
            Text("Skip", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 10.dp,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = "Watch My Calories logo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(64.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xxl))

        Text(
            "Watch My Calories",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("onboarding_title")
        )

        Spacer(modifier = Modifier.height(Spacing.m))

        Text(
            "Track your meals with AI-powered calorie estimation",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                "Your data is never stored outside this device",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.l))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag(AccessibilityTags.Onboarding.GET_STARTED_BUTTON),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Get Started", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PrivacyStep(
    settingsDataStore: SettingsDataStore,
    onNext: () -> Unit
) {
    val aiConsent by settingsDataStore.aiConsentFlow.collectAsState(initial = "notAsked")
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            "Your Privacy",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI Photo Analysis", fontWeight = FontWeight.Medium)
                    Switch(
                        checked = aiConsent == "accepted",
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                settingsDataStore.setAiConsent(if (enabled) "accepted" else "declined")
                            }
                        },
                        modifier = Modifier.testTag(AccessibilityTags.Onboarding.AI_CONSENT_TOGGLE)
                    )
                }
                Text(
                    "When enabled, food photos are sent to Google Gemini for calorie estimation. All other data stays on-device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Mirror of iOS Privacy step "Connect Health" — request once, then
        // swap to a green checkmark + disabled state so the user knows it
        // registered. iOS's `heart.fill → checkmark.circle.fill` swap.
        val context = androidx.compose.ui.platform.LocalContext.current
        var healthRequested by remember { mutableStateOf(false) }
        OutlinedButton(
            enabled = !healthRequested,
            onClick = {
                healthRequested = true
                try {
                    val intent = android.content.Intent("androidx.health.connect.action.HEALTH_CONNECT_SETTINGS")
                    context.startActivity(intent)
                } catch (_: Exception) { /* Health Connect not installed; flag still flips */ }
            },
            colors = if (healthRequested) {
                androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.primary,
                )
            } else androidx.compose.material3.ButtonDefaults.outlinedButtonColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AccessibilityTags.Onboarding.CONNECT_HEALTH_BUTTON),
        ) {
            if (healthRequested) {
                Icon(
                    androidx.compose.material.icons.Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Health Connect Requested")
            } else {
                Text("Connect Health (active calories)")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        ProgressDots(current = 1, total = 2)

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag(AccessibilityTags.Onboarding.NEXT_BUTTON),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Next", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalStep(
    heightCm: Int,
    weightKg: Int,
    age: Int,
    gender: Gender,
    activityLevel: ActivityLevel,
    targetCaloriesText: String,
    onHeightChanged: (Int) -> Unit,
    onWeightChanged: (Int) -> Unit,
    onAgeChanged: (Int) -> Unit,
    onGenderChanged: (Gender) -> Unit,
    onActivityChanged: (ActivityLevel) -> Unit,
    onCaloriesTextChanged: (String) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.l)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            "Your Goal",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // About You
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                Text("About You", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                // Unit System toggle — defaults to metric. Internal storage stays cm/kg;
                // display labels convert to ft/in or lbs when US Customary is selected.
                var isMetric by remember { mutableStateOf(true) }
                Text("Unit System", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("US Customary" to false, "Metric" to true).forEachIndexed { index, (label, metric) ->
                        SegmentedButton(
                            selected = isMetric == metric,
                            onClick = { isMetric = metric },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                    }
                }

                // Height slider — cm internally; display "5 ft 8 in" when US.
                val heightDisplay = if (isMetric) {
                    "$heightCm cm"
                } else {
                    val totalIn = (heightCm / 2.54).toInt()
                    "${totalIn / 12} ft ${totalIn % 12} in"
                }
                SliderRow("Height", heightCm, 100..250, heightDisplay, onHeightChanged)

                // Weight slider — kg internally; display lbs when US.
                val weightDisplay = if (isMetric) {
                    "$weightKg kg"
                } else {
                    "${(weightKg * 2.20462).toInt()} lbs"
                }
                SliderRow("Weight", weightKg, 20..200, weightDisplay, onWeightChanged)

                // Age slider (unitless)
                SliderRow("Age", age, 1..100, "$age", onAgeChanged)

                // Gender
                Text("Gender", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    Gender.entries.forEachIndexed { index, g ->
                        SegmentedButton(
                            selected = gender == g,
                            onClick = { onGenderChanged(g) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = Gender.entries.size)
                        ) { Text(g.displayName, style = MaterialTheme.typography.labelSmall) }
                    }
                }

                // Activity
                Text("Activity Level", style = MaterialTheme.typography.bodyMedium)
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = activityLevel.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        ActivityLevel.entries.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level.displayName) },
                                onClick = { onActivityChanged(level); expanded = false }
                            )
                        }
                    }
                }
            }
        }

        // Daily Goal
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                Text("Daily Goal", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = targetCaloriesText,
                    onValueChange = onCaloriesTextChanged,
                    label = { Text("Target Calories") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag(AccessibilityTags.Onboarding.TARGET_CALORIES_FIELD)
                )

                Button(
                    onClick = {
                        val recommended = CalorieCalculator.recommended(
                            heightCm.toDouble(), weightKg.toDouble(), age, gender, activityLevel
                        )
                        onCaloriesTextChanged(recommended.toInt().toString())
                    },
                    modifier = Modifier.fillMaxWidth().testTag(AccessibilityTags.Onboarding.CALCULATE_GOAL_BUTTON)
                ) {
                    Text("Calculate Recommended Goal")
                }
            }
        }

        ProgressDots(current = 2, total = 2, modifier = Modifier.align(Alignment.CenterHorizontally))

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag(AccessibilityTags.Onboarding.FINISH_BUTTON),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Start Tracking", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

/**
 * `displayValue` is the pre-formatted right-aligned label (e.g. "175 cm" /
 * "5 ft 9 in"); the slider operates on `value` in the canonical unit. Callers
 * compose the display string so unit conversion logic stays at the call site.
 */
@Composable
private fun SliderRow(label: String, value: Int, range: IntRange, displayValue: String, onChange: (Int) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                displayValue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat()
        )
    }
}

@Composable
private fun ProgressDots(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        repeat(total) { step ->
            Surface(
                shape = CircleShape,
                color = if (step + 1 == current) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                modifier = Modifier.size(8.dp)
            ) {}
        }
    }
}
