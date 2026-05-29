package com.pning80.watchmycalories.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pning80.watchmycalories.data.CalorieCalculator
import com.pning80.watchmycalories.data.CalorieCalculator.Gender
import com.pning80.watchmycalories.data.CalorieCalculator.ActivityLevel
import com.pning80.watchmycalories.data.UserProfile
import com.pning80.watchmycalories.ads.AdManager
import com.pning80.watchmycalories.ads.BannerAdView
import com.pning80.watchmycalories.ui.components.AppIconView
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsDataStore: SettingsDataStore,
    currentProfile: UserProfile? = null,
    onSaveProfile: ((UserProfile) -> Unit)? = null
) {
    val isMetric by settingsDataStore.isMetricFlow.collectAsState(initial = true)
    val appTheme by settingsDataStore.appThemeFlow.collectAsState(initial = "System")
    val aiConsent by settingsDataStore.aiConsentFlow.collectAsState(initial = "notAsked")
    val coroutineScope = rememberCoroutineScope()

    // Profile state — initialized from currentProfile or defaults
    var heightCm by remember(currentProfile) { mutableIntStateOf(currentProfile?.height?.roundToInt() ?: 173) }
    var weightKg by remember(currentProfile) { mutableIntStateOf(currentProfile?.weight?.roundToInt() ?: 68) }
    var heightFeet by remember(currentProfile) {
        val totalInches = (currentProfile?.height ?: 173.0) / 2.54
        mutableIntStateOf(totalInches.toInt() / 12)
    }
    var heightInches by remember(currentProfile) {
        val totalInches = (currentProfile?.height ?: 173.0) / 2.54
        mutableIntStateOf(totalInches.toInt() % 12)
    }
    var weightLbs by remember(currentProfile) {
        mutableIntStateOf(((currentProfile?.weight ?: 68.0) * 2.20462).roundToInt())
    }
    var age by remember(currentProfile) { mutableIntStateOf(currentProfile?.age ?: 30) }
    var gender by remember(currentProfile) {
        mutableStateOf(Gender.fromRaw(currentProfile?.genderRaw))
    }
    var activityLevel by remember(currentProfile) {
        mutableStateOf(ActivityLevel.fromRaw(currentProfile?.activityLevelRaw))
    }
    var targetCaloriesText by remember(currentProfile) {
        mutableStateOf(
            if (currentProfile != null && currentProfile.targetCalories > 0)
                currentProfile.targetCalories.toInt().toString()
            else ""
        )
    }
    
    val isPrivacyOptionsRequired by AdManager.isPrivacyOptionsRequired.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Detected changes
    val hasUnsavedChanges = remember(
        heightCm, weightKg, heightFeet, heightInches, weightLbs, age, gender, activityLevel, targetCaloriesText, isMetric, appTheme
    ) {
        val originalTarget = if (currentProfile != null && currentProfile.targetCalories > 0)
            currentProfile.targetCalories.toInt().toString() else ""
            
        heightCm != (currentProfile?.height?.roundToInt() ?: 173) ||
        weightKg != (currentProfile?.weight?.roundToInt() ?: 68) ||
        age != (currentProfile?.age ?: 30) ||
        gender != Gender.fromRaw(currentProfile?.genderRaw) ||
        activityLevel != ActivityLevel.fromRaw(currentProfile?.activityLevelRaw) ||
        targetCaloriesText != originalTarget
    }

    BackHandler(enabled = hasUnsavedChanges) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(onClick = { 
                    showDiscardDialog = false
                    // In a real app, we might need a way to pass a "dismiss" higher up
                    // But for now, we just let them go back.
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep Editing") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title row removed — Scaffold TopAppBar in MainActivity provides "Settings" header.
        // App icon now sits inline as the leading element of App Appearance section.
        // BannerAdView is conditionally rendered inside BannerAdView() based on
        // whether a real (non-Google-test) ad unit ID is wired; in debug builds with
        // the placeholder test unit it returns Unit, avoiding the "Test Ad" banner.
        BannerAdView()

        // ── App Appearance ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("App Appearance", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                // Theme picker — labels match iOS AppTheme rawValues exactly
                Text("Theme", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Settings.THEME_PICKER)
                ) {
                    listOf("System", "Light", "Dark").forEachIndexed { index, theme ->
                        SegmentedButton(
                            selected = appTheme == theme,
                            onClick = { coroutineScope.launch { settingsDataStore.setAppTheme(theme) } },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                        ) { Text(theme) }
                    }
                }

                // Unit System picker — labels and options match iOS UnitSystem rawValues exactly
                Text("Unit System", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Settings.UNIT_PICKER)
                ) {
                    listOf("US Customary" to false, "Metric" to true).forEachIndexed { index, (label, metric) ->
                        SegmentedButton(
                            selected = isMetric == metric,
                            onClick = {
                                if (isMetric == metric) return@SegmentedButton
                                coroutineScope.launch { settingsDataStore.setMetric(metric) }
                                if (metric) {
                                    val totalInches = heightFeet * 12 + heightInches
                                    heightCm = (totalInches * 2.54).roundToInt()
                                    weightKg = (weightLbs / 2.20462).roundToInt()
                                } else {
                                    val totalInches = (heightCm / 2.54).toInt()
                                    heightFeet = totalInches / 12
                                    heightInches = totalInches % 12
                                    weightLbs = (weightKg * 2.20462).roundToInt()
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2)
                        ) { Text(label) }
                    }
                }
            }
        }

        // ── Profile ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Profile", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                if (isMetric) {
                    // Height (cm)
                    ProfileSliderRow(
                        label = "Height",
                        value = heightCm,
                        range = 100..250,
                        unit = "cm",
                        onValueChange = { heightCm = it }
                    )
                    // Weight (kg)
                    ProfileSliderRow(
                        label = "Weight",
                        value = weightKg,
                        range = 20..200,
                        unit = "kg",
                        onValueChange = { weightKg = it }
                    )
                } else {
                    // Height (ft/in)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Height", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileDropdown(
                                value = heightFeet,
                                options = (4..7).toList(),
                                suffix = "ft",
                                onValueChange = { heightFeet = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ProfileDropdown(
                                value = heightInches,
                                options = (0..11).toList(),
                                suffix = "in",
                                onValueChange = { heightInches = it }
                            )
                        }
                    }
                    // Weight (lbs)
                    ProfileSliderRow(
                        label = "Weight",
                        value = weightLbs,
                        range = 50..400,
                        unit = "lbs",
                        onValueChange = { weightLbs = it }
                    )
                }

                // Age
                ProfileSliderRow(
                    label = "Age",
                    value = age,
                    range = 1..100,
                    unit = "",
                    onValueChange = { age = it }
                )

                // Gender
                Text("Gender", style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Settings.GENDER_PICKER)
                ) {
                    Gender.entries.forEachIndexed { index, g ->
                        SegmentedButton(
                            selected = gender == g,
                            onClick = { gender = g },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = Gender.entries.size),
                            modifier = Modifier.testTag("settings_gender_${g.displayName}")
                        ) { Text(g.displayName) }
                    }
                }

                // Activity Level
                Text("Activity Level", style = MaterialTheme.typography.bodyMedium)
                var activityExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = activityExpanded,
                    onExpandedChange = { activityExpanded = it }
                ) {
                    OutlinedTextField(
                        value = activityLevel.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Settings.ACTIVITY_PICKER)
                    )
                    ExposedDropdownMenu(
                        expanded = activityExpanded,
                        onDismissRequest = { activityExpanded = false }
                    ) {
                        ActivityLevel.entries.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level.displayName) },
                                onClick = {
                                    activityLevel = level
                                    activityExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // ── Daily Goals ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Daily Goals", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = targetCaloriesText,
                    onValueChange = { targetCaloriesText = it },
                    label = { Text("Target Calories") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Settings.TARGET_CALORIES)
                )

                Button(
                    onClick = {
                        val hCm = if (isMetric) heightCm.toDouble() else (heightFeet * 12 + heightInches) * 2.54
                        val wKg = if (isMetric) weightKg.toDouble() else weightLbs / 2.20462
                        val recommended = CalorieCalculator.recommended(hCm, wKg, age, gender, activityLevel)
                        targetCaloriesText = recommended.toInt().toString()
                    },
                    modifier = Modifier.fillMaxWidth().testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Settings.CALCULATE_GOAL)
                ) {
                    Text("Calculate Recommended Goal")
                }
            }
        }

        // ── Privacy ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Privacy", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI Photo Analysis", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = aiConsent == "accepted",
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                settingsDataStore.setAiConsent(if (enabled) "accepted" else "declined")
                            }
                        },
                        modifier = Modifier.testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Settings.AI_CONSENT_TOGGLE)
                    )
                }
                Text(
                    "When enabled, food photos are sent to Google Gemini for calorie estimation. All other data stays on-device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isPrivacyOptionsRequired) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { AdManager.presentPrivacyOptionsForm(context as android.app.Activity) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("Manage Privacy Choices")
                    }
                }
            }
        }

        // ── Save Button ──
        Button(
            onClick = {
                val hCm = if (isMetric) heightCm.toDouble() else (heightFeet * 12 + heightInches) * 2.54
                val wKg = if (isMetric) weightKg.toDouble() else weightLbs / 2.20462
                val target = targetCaloriesText.toDoubleOrNull() ?: 2000.0

                val profile = UserProfile(
                    id = 1,
                    height = hCm,
                    weight = wKg,
                    age = age,
                    genderRaw = gender.displayName,
                    activityLevelRaw = activityLevel.displayName,
                    targetCalories = target
                )
                onSaveProfile?.invoke(profile)
            },
            modifier = Modifier.fillMaxWidth().testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Settings.SAVE_BUTTON)
        ) {
            Text("Save Settings")
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun ProfileSliderRow(
    label: String,
    value: Int,
    range: IntRange,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (unit.isNotEmpty()) "$value $unit" else "$value",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileDropdown(
    value: Int,
    options: List<Int>,
    suffix: String,
    onValueChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = "$value $suffix",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.width(80.dp).menuAnchor(),
            textStyle = MaterialTheme.typography.bodySmall
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text("$opt $suffix") },
                    onClick = {
                        onValueChange(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
