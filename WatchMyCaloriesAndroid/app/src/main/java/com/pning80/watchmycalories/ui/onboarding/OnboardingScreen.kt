package com.pning80.watchmycalories.ui.onboarding

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pning80.watchmycalories.R
import com.pning80.watchmycalories.data.CalorieCalculator
import com.pning80.watchmycalories.data.CalorieCalculator.ActivityLevel
import com.pning80.watchmycalories.data.CalorieCalculator.Gender
import com.pning80.watchmycalories.ui.settings.ProfileWheelRow
import com.pning80.watchmycalories.ui.settings.ProfileDropdown
import com.pning80.watchmycalories.data.UserProfile
import com.pning80.watchmycalories.ui.settings.SettingsDataStore
import com.pning80.watchmycalories.ui.theme.Spacing
import com.pning80.watchmycalories.utils.AccessibilityTags
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    settingsDataStore: SettingsDataStore,
    onComplete: (UserProfile) -> Unit
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Unit system from the app-wide preference. Onboarding reads and writes
    // through the same DataStore the Settings screen uses, so the choice
    // persists and stays in sync app-wide.
    val isMetric by settingsDataStore.isMetricFlow.collectAsState(initial = true)

    // Profile state. Metric and US inputs are stored independently — mirrors
    // iOS, which avoids lossy round-trips between cm/kg sliders and ft/in/lbs
    // pickers by giving each unit system its own backing state.
    var heightCmUI by rememberSaveable { mutableIntStateOf(173) }
    var weightKgUI by rememberSaveable { mutableIntStateOf(68) }
    var heightFeet by rememberSaveable { mutableIntStateOf(5) }
    var heightInchesPart by rememberSaveable { mutableIntStateOf(8) }
    var weightLbs by rememberSaveable { mutableIntStateOf(150) }
    var age by rememberSaveable { mutableIntStateOf(30) }
    var genderRaw by rememberSaveable { mutableStateOf(Gender.MALE.displayName) }
    var activityLevelRaw by rememberSaveable { mutableStateOf(ActivityLevel.SEDENTARY.displayName) }
    var targetCaloriesText by rememberSaveable { mutableStateOf("") }

    val gender = Gender.fromRaw(genderRaw)
    val activityLevel = ActivityLevel.fromRaw(activityLevelRaw)

    // Canonical metric values used by the save + recommend paths.
    fun computedHeightCm(): Double =
        if (isMetric) heightCmUI.toDouble()
        else (heightFeet * 12 + heightInchesPart) * 2.54
    fun computedWeightKg(): Double =
        if (isMetric) weightKgUI.toDouble()
        else weightLbs / 2.20462

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(targetState = currentStep, label = "onboarding") { step ->
            when (step) {
                0 -> WelcomeStep(onNext = { currentStep = 1 })
                1 -> PrivacyStep(
                    settingsDataStore = settingsDataStore,
                    onNext = { currentStep = 2 }
                )
                2 -> GoalStep(
                    isMetric = isMetric,
                    onMetricChanged = { metric ->
                        coroutineScope.launch { settingsDataStore.setMetric(metric) }
                    },
                    heightCmUI = heightCmUI,
                    onHeightCmChanged = { heightCmUI = it },
                    weightKgUI = weightKgUI,
                    onWeightKgChanged = { weightKgUI = it },
                    heightFeet = heightFeet,
                    onHeightFeetChanged = { heightFeet = it },
                    heightInchesPart = heightInchesPart,
                    onHeightInchesChanged = { heightInchesPart = it },
                    weightLbs = weightLbs,
                    onWeightLbsChanged = { weightLbs = it },
                    age = age,
                    onAgeChanged = { age = it },
                    gender = gender,
                    onGenderChanged = { genderRaw = it.displayName },
                    activityLevel = activityLevel,
                    onActivityChanged = { activityLevelRaw = it.displayName },
                    targetCaloriesText = targetCaloriesText,
                    onCaloriesTextChanged = { targetCaloriesText = it },
                    onCalculateRecommended = {
                        val recommended = CalorieCalculator.recommended(
                            computedHeightCm(), computedWeightKg(), age, gender, activityLevel
                        )
                        targetCaloriesText = recommended.toInt().toString()
                    },
                    onFinish = {
                        val target = targetCaloriesText.toDoubleOrNull() ?: 2000.0
                        val profile = UserProfile(
                            id = 1,
                            height = computedHeightCm(),
                            weight = computedWeightKg(),
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

        // Brand mark — mirrors iOS Onboarding welcome
        // (OnboardingView.swift:60-63 → `AppIconView()`). Shares the same
        // 1024.png source as the launcher icon.
        Image(
            painter = painterResource(id = R.drawable.app_icon),
            contentDescription = "Watch My Calories logo",
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(28.dp))
        )

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
            color = MaterialTheme.colorScheme.primary,
            // iOS centers step titles (OnboardingView VStack default alignment);
            // the Privacy/Goal Columns here default to Start. Center to match iOS
            // and the Welcome step (which already centers).
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Scrollable content region (weight 1f) so the pinned ProgressDots +
        // Next button below never get clipped when content (e.g. the health
        // caption) makes the step taller than the viewport.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
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
        // `rememberSaveable` so the requested state survives rotation.
        val context = LocalContext.current
        var healthRequested by rememberSaveable { mutableStateOf(false) }
        OutlinedButton(
            enabled = !healthRequested,
            onClick = {
                healthRequested = true
                try {
                    val intent = Intent("androidx.health.connect.action.HEALTH_CONNECT_SETTINGS")
                    context.startActivity(intent)
                } catch (_: Exception) { /* Health Connect not installed; flag still flips */ }
            },
            colors = if (healthRequested) {
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.primary,
                )
            } else ButtonDefaults.outlinedButtonColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AccessibilityTags.Onboarding.CONNECT_HEALTH_BUTTON),
        ) {
            if (healthRequested) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Health Connect Requested")
            } else {
                // iOS shows a green heart.fill before the request
                // (OnboardingView.swift:249-250); Android was missing the leading
                // icon in this initial state (it only had the post-request check).
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Connect Health (active calories)")
            }
        }

            // Mirror of iOS permissionsStep health caption (OnboardingView.swift:258).
            // "Apple Health" → "Health Connect" for the Android platform.
            Text(
                "Syncs active calories burned from Health Connect to adjust your daily goal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        ProgressDots(current = 1, total = 2, modifier = Modifier.align(Alignment.CenterHorizontally))

        // iOS permissionsStep repeats the privacy reassurance label on this step
        // (OnboardingView.swift:266-268), between the progress dots and the Next
        // button — Android only rendered it on the Welcome step. Match iOS, reusing
        // the Welcome step's Lock + labelMedium treatment.
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
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
    isMetric: Boolean,
    onMetricChanged: (Boolean) -> Unit,
    heightCmUI: Int,
    onHeightCmChanged: (Int) -> Unit,
    weightKgUI: Int,
    onWeightKgChanged: (Int) -> Unit,
    heightFeet: Int,
    onHeightFeetChanged: (Int) -> Unit,
    heightInchesPart: Int,
    onHeightInchesChanged: (Int) -> Unit,
    weightLbs: Int,
    onWeightLbsChanged: (Int) -> Unit,
    age: Int,
    onAgeChanged: (Int) -> Unit,
    gender: Gender,
    onGenderChanged: (Gender) -> Unit,
    activityLevel: ActivityLevel,
    onActivityChanged: (ActivityLevel) -> Unit,
    targetCaloriesText: String,
    onCaloriesTextChanged: (String) -> Unit,
    onCalculateRecommended: () -> Unit,
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
            color = MaterialTheme.colorScheme.primary,
            // Centered to match iOS (OnboardingView VStack) + the Welcome step.
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // About You
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                Text("About You".uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Unit System toggle — wired to the app-wide DataStore so it
                // persists and stays in sync with the Settings screen.
                Text("Unit System", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("US Customary" to false, "Metric" to true).forEachIndexed { index, (label, metric) ->
                        SegmentedButton(
                            selected = isMetric == metric,
                            onClick = { onMetricChanged(metric) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                    }
                }

                // Profile inputs reuse the Settings profile controls (D-004):
                // tumbler wheels for Height(metric)/Weight/Age + ft-in dropdowns for
                // US Height + a Gender dropdown. Replaces the prior Sliders +
                // segmented Gender so onboarding matches the Settings profile
                // (D-004 rationale: a Slider can't reliably hit an exact value).
                // One wheel open at a time, like Settings.
                var expandedProfileField by remember { mutableStateOf<String?>(null) }

                if (isMetric) {
                    ProfileWheelRow(
                        label = "Height", value = heightCmUI, range = 100..250, unit = "cm",
                        expanded = expandedProfileField == "height",
                        onToggle = { expandedProfileField = if (expandedProfileField == "height") null else "height" },
                        onValueChange = onHeightCmChanged
                    )
                    ProfileWheelRow(
                        label = "Weight", value = weightKgUI, range = 20..200, unit = "kg",
                        expanded = expandedProfileField == "weight",
                        onToggle = { expandedProfileField = if (expandedProfileField == "weight") null else "weight" },
                        onValueChange = onWeightKgChanged
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Height", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileDropdown(value = heightFeet, options = (4..7).toList(), suffix = "ft", onValueChange = onHeightFeetChanged)
                            Spacer(modifier = Modifier.width(Spacing.s))
                            ProfileDropdown(value = heightInchesPart, options = (0..11).toList(), suffix = "in", onValueChange = onHeightInchesChanged)
                        }
                    }
                    ProfileWheelRow(
                        label = "Weight", value = weightLbs, range = 50..400, unit = "lbs",
                        expanded = expandedProfileField == "weight",
                        onToggle = { expandedProfileField = if (expandedProfileField == "weight") null else "weight" },
                        onValueChange = onWeightLbsChanged
                    )
                }

                // Age
                ProfileWheelRow(
                    label = "Age", value = age, range = 1..100, unit = "",
                    expanded = expandedProfileField == "age",
                    onToggle = { expandedProfileField = if (expandedProfileField == "age") null else "age" },
                    onValueChange = onAgeChanged
                )

                // Gender — dropdown menu picker (mirrors Settings + iOS .menu Picker).
                Text("Gender", style = MaterialTheme.typography.bodyMedium)
                var genderExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = it }) {
                    OutlinedTextField(
                        value = gender.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                        Gender.entries.forEach { g ->
                            DropdownMenuItem(text = { Text(g.displayName) }, onClick = { onGenderChanged(g); genderExpanded = false })
                        }
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
                Text("Daily Goal".uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = targetCaloriesText,
                    onValueChange = onCaloriesTextChanged,
                    label = { Text("Target Calories") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag(AccessibilityTags.Onboarding.TARGET_CALORIES_FIELD)
                )

                Button(
                    onClick = onCalculateRecommended,
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
