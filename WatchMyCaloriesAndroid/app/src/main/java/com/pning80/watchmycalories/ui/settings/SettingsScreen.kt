package com.pning80.watchmycalories.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Info
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
import com.pning80.watchmycalories.ui.theme.Spacing
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsDataStore: SettingsDataStore,
    currentProfile: UserProfile? = null,
    onSaveProfile: ((UserProfile) -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") onNavigateToAbout: () -> Unit = {},
    onCancel: () -> Unit
) {
    val isMetricNullable by settingsDataStore.isMetricFlow.collectAsState(initial = null)
    val appThemeNullable by settingsDataStore.appThemeFlow.collectAsState(initial = null)
    val aiConsentNullable by settingsDataStore.aiConsentFlow.collectAsState(initial = null)

    if (isMetricNullable == null || appThemeNullable == null || aiConsentNullable == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val isMetric = isMetricNullable!!
    val appTheme = appThemeNullable!!
    val aiConsent = aiConsentNullable!!

    val initialMetric = rememberSaveable { isMetric }
    val initialTheme = rememberSaveable { appTheme }
    val initialConsent = rememberSaveable { aiConsent }

    // Instant local states for theme, metric, and consent to avoid Datastore write delay/flicker and reset bugs
    var localIsMetric by rememberSaveable { mutableStateOf(initialMetric) }
    var localAppTheme by rememberSaveable { mutableStateOf(initialTheme) }
    var localAiConsent by rememberSaveable { mutableStateOf(initialConsent) }

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

    val originalHeightCm = remember(currentProfile) { currentProfile?.height?.roundToInt() ?: 173 }
    val originalWeightKg = remember(currentProfile) { currentProfile?.weight?.roundToInt() ?: 68 }
    val originalHeightFeet = remember(currentProfile) {
        val totalInches = (currentProfile?.height ?: 173.0) / 2.54
        totalInches.toInt() / 12
    }
    val originalHeightInches = remember(currentProfile) {
        val totalInches = (currentProfile?.height ?: 173.0) / 2.54
        totalInches.toInt() % 12
    }
    val originalWeightLbs = remember(currentProfile) {
        ((currentProfile?.weight ?: 68.0) * 2.20462).roundToInt()
    }

    // Detected changes
    val hasUnsavedChanges = remember(
        heightCm, weightKg, heightFeet, heightInches, weightLbs, age, gender, activityLevel, targetCaloriesText, localIsMetric, localAppTheme, localAiConsent,
        initialMetric, initialTheme, initialConsent
    ) {
        val originalTarget = if (currentProfile != null && currentProfile.targetCalories > 0)
            currentProfile.targetCalories.toInt().toString() else ""

        val heightChanged = if (localIsMetric) {
            heightCm != originalHeightCm
        } else {
            heightFeet != originalHeightFeet || heightInches != originalHeightInches
        }

        val weightChanged = if (localIsMetric) {
            weightKg != originalWeightKg
        } else {
            weightLbs != originalWeightLbs
        }

        val consentChanged = localAiConsent != initialConsent

        heightChanged ||
        weightChanged ||
        age != (currentProfile?.age ?: 30) ||
        gender != Gender.fromRaw(currentProfile?.genderRaw) ||
        activityLevel != ActivityLevel.fromRaw(currentProfile?.activityLevelRaw) ||
        targetCaloriesText != originalTarget ||
        localIsMetric != initialMetric ||
        localAppTheme != initialTheme ||
        consentChanged
    }

    BackHandler(enabled = hasUnsavedChanges) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            // iOS confirmationDialog states the fact ("You have unsaved changes.")
            // with a "Discard Changes" action — no question-form body.
            title = { Text("You have unsaved changes.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    coroutineScope.launch {
                        settingsDataStore.setMetric(initialMetric)
                        settingsDataStore.setAppTheme(initialTheme)
                        settingsDataStore.setAiConsent(initialConsent)
                    }
                    onCancel()
                }) { Text("Discard Changes") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep Editing") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", modifier = Modifier.testTag("SettingsTitle")) },
                windowInsets = TopAppBarDefaults.windowInsets,
                navigationIcon = {
                    TextButton(
                        onClick = {
                            if (hasUnsavedChanges) {
                                showDiscardDialog = true
                            } else {
                                onCancel()
                            }
                        },
                        modifier = Modifier.testTag("settings_cancel_button")
                    ) {
                        Text("Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val hCm = if (localIsMetric) heightCm.toDouble() else (heightFeet * 12 + heightInches) * 2.54
                            val wKg = if (localIsMetric) weightKg.toDouble() else weightLbs / 2.20462
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
                        enabled = hasUnsavedChanges,
                        modifier = Modifier.testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Settings.SAVE_BUTTON)
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.pageHorizontal),
            verticalArrangement = Arrangement.spacedBy(Spacing.l)
        ) {
            // App icon now sits inline as the leading element of App Appearance section.
            // BannerAdView is conditionally rendered inside BannerAdView() based on
            // whether a real (non-Google-test) ad unit ID is wired; in debug builds with
            // the placeholder test unit it returns Unit, avoiding the "Test Ad" banner.
            BannerAdView(insetHorizontal = false)

            // ── App Appearance ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                    Text("App Appearance", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                    // Theme picker — inline trailing-value menu (mirror of iOS .pickerStyle(.menu))
                    InlineMenuPickerRow(
                        label = "Theme",
                        options = listOf("System", "Light", "Dark"),
                        selectedLabel = localAppTheme,
                        onSelect = { theme ->
                            localAppTheme = theme
                            coroutineScope.launch { settingsDataStore.setAppTheme(theme) }
                        },
                        testTag = com.pning80.watchmycalories.utils.AccessibilityTags.Settings.THEME_PICKER,
                    )

                    // Unit System picker — inline trailing-value menu
                    InlineMenuPickerRow(
                        label = "Unit System",
                        options = listOf("US Customary", "Metric"),
                        selectedLabel = if (localIsMetric) "Metric" else "US Customary",
                        onSelect = { picked ->
                            val metric = picked == "Metric"
                            if (localIsMetric == metric) return@InlineMenuPickerRow
                            localIsMetric = metric
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
                        testTag = com.pning80.watchmycalories.utils.AccessibilityTags.Settings.UNIT_PICKER,
                    )
                }
            }

            // ── Profile ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                    Text("Profile", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                    if (localIsMetric) {
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
                                Spacer(modifier = Modifier.width(Spacing.s))
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
                            // Default M3 outline (`outline`) is near-invisible on
                            // the dark `#1A211C` surface; pin the border + value
                            // text to primary mint so the picker reads clearly
                            // and echoes iOS's mint picker value (PR T).
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedTextColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = MaterialTheme.colorScheme.primary,
                            ),
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
                Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
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
                            val hCm = if (localIsMetric) heightCm.toDouble() else (heightFeet * 12 + heightInches) * 2.54
                            val wKg = if (localIsMetric) weightKg.toDouble() else weightLbs / 2.20462
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
                Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Text("Privacy", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AI Photo Analysis", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = localAiConsent == "accepted",
                            onCheckedChange = { enabled ->
                                val newConsent = if (enabled) "accepted" else "declined"
                                localAiConsent = newConsent
                                coroutineScope.launch {
                                    settingsDataStore.setAiConsent(newConsent)
                                }
                            },
                            modifier = Modifier.testTag(com.pning80.watchmycalories.utils.AccessibilityTags.Settings.AI_CONSENT_TOGGLE)
                        )
                    }
                    Text(
                        "When enabled, food photos are sent to Google Gemini, a third-party AI service by Google, for calorie estimation. All other data stays on-device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isPrivacyOptionsRequired) {
                        Spacer(modifier = Modifier.height(Spacing.s))
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

            // About is reached via the Dashboard overflow menu (gear → About),
            // matching iOS's AppMenuToolbar pattern. The previous in-Settings
            // "About this app" row was removed for parity (PR C).

            Spacer(modifier = Modifier.height(Spacing.l))
        }
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

/**
 * Settings row with a static `label` on the left and an inline trailing
 * menu picker on the right that displays the current selection. Tap the
 * row to open a DropdownMenu of options. Mirrors iOS's `.pickerStyle(.menu)`
 * affordance closely — closes the "Settings is too visually heavy" gap from
 * the cross-screen audit.
 */
@Composable
private fun InlineMenuPickerRow(
    label: String,
    options: List<String>,
    selectedLabel: String,
    onSelect: (String) -> Unit,
    testTag: String,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { menuOpen = true }
                .padding(vertical = Spacing.s)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    selectedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    androidx.compose.material.icons.Icons.Filled.UnfoldMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { menuOpen = false; onSelect(opt) },
                )
            }
        }
    }
}
