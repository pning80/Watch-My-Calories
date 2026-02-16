package com.pning80.caloriewatcherandroid.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pning80.caloriewatcherandroid.R
import com.pning80.caloriewatcherandroid.data.model.ActivityLevel
import com.pning80.caloriewatcherandroid.data.model.Gender
import com.pning80.caloriewatcherandroid.ui.theme.*
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onSave: () -> Unit,
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val models by viewModel.models.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()

    var apiKeyText by remember(apiKey) { mutableStateOf(apiKey ?: "") }

    // State for imperial units
    var heightFeet by remember { mutableStateOf(5) }
    var heightInches by remember { mutableStateOf(9) }
    var weightLbs by remember { mutableStateOf(160f) }
    var age by remember { mutableStateOf(30f) }
    var gender by remember { mutableStateOf(Gender.MALE) }
    var activityLevel by remember { mutableStateOf(ActivityLevel.SEDENTARY) }
    var targetCalories by remember { mutableStateOf(2000) }
    var expanded by remember { mutableStateOf(false) }

    // When userProfile is loaded, update the local states
    LaunchedEffect(userProfile) {
        userProfile?.let {
            val totalInches = it.heightCm / 2.54
            heightFeet = (totalInches / 12).toInt()
            heightInches = (totalInches % 12).roundToInt()
            weightLbs = (it.weightKg * 2.20462).toFloat()
            age = it.age.toFloat()
            gender = it.gender
            activityLevel = it.activityLevel
            targetCalories = it.targetCalories.toInt()
        }
    }

    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CWBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo_modern),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = TitleFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = CWPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.saveApiKey(apiKeyText)
                        selectedModel?.let { viewModel.saveSelectedModel(it) }
                        val heightInCm = (heightFeet * 12 + heightInches) * 2.54
                        val weightInKg = weightLbs / 2.20462
                        viewModel.saveProfile(
                            heightInCm,
                            weightInKg,
                            age.toInt(),
                            gender,
                            activityLevel,
                            targetCalories
                        )
                        onSave()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            tint = CWPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CWBackground
                )
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // AI Configuration
            SettingsSection(title = "AI Configuration", icon = Icons.Filled.SmartToy) {
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    label = { Text("Gemini API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CWPrimary,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = CWPrimary,
                        focusedTextColor = CWTextPrimary,
                        unfocusedTextColor = CWTextPrimary,
                    )
                )
                if (models.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            value = selectedModel ?: "gemini-1.5-flash",
                            onValueChange = {},
                            label = { Text("Model") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CWPrimary,
                                unfocusedBorderColor = Color.Gray,
                                cursorColor = CWPrimary,
                                focusedTextColor = CWTextPrimary,
                                unfocusedTextColor = CWTextPrimary,
                                focusedLabelColor = CWPrimary,
                            ),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            models.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        viewModel.saveSelectedModel(selectionOption)
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }

                }
                TextButton(
                    onClick = { uriHandler.openUri("https://aistudio.google.com/app/apikey") },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Get API Key from Google AI Studio", color = CWPrimary)
                }
            }

            // User Profile
            SettingsSection(title = "User Profile", icon = Icons.Filled.AccountCircle) {
                Text(
                    "Height",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = CWTextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        LabeledSlider(
                            label = "",
                            value = heightFeet.toFloat(),
                            onValueChange = { heightFeet = it.roundToInt() },
                            valueRange = 3f..7f,
                            steps = 4,
                            valueText = "$heightFeet ft"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        LabeledSlider(
                            label = "",
                            value = heightInches.toFloat(),
                            onValueChange = { heightInches = it.roundToInt() },
                            valueRange = 0f..11f,
                            steps = 11,
                            valueText = "$heightInches in"
                        )
                    }
                }
                LabeledSlider(
                    label = "Weight",
                    value = weightLbs,
                    onValueChange = { weightLbs = it },
                    valueRange = 80f..400f,
                    steps = (400 - 80),
                    valueText = "${weightLbs.roundToInt()} lbs"
                )
                LabeledSlider(
                    label = "Age",
                    value = age,
                    onValueChange = { age = it },
                    valueRange = 13f..100f,
                    steps = (100 - 13) - 1,
                    valueText = "${age.roundToInt()} years"
                )

                Text(
                    "Gender",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = CWTextPrimary
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    Gender.entries.forEachIndexed { index, g ->
                        SegmentedButton(
                            modifier = Modifier.weight(1f),
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = Gender.entries.size
                            ),
                            onClick = { gender = g },
                            selected = gender == g,
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = CWPrimary,
                                activeContentColor = Color.White,
                                inactiveContainerColor = CWSecondary,
                                inactiveContentColor = CWPrimary,
                            ),
                        ) { Text(g.name.replaceFirstChar { it.titlecase(Locale.getDefault()) }) }
                    }
                }

                var activityLevelExpanded by remember { mutableStateOf(false) }
                val activityLevels = ActivityLevel.entries.toTypedArray()
                Text(
                    text = "Activity Level",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = CWTextPrimary
                )
                ExposedDropdownMenuBox(
                    expanded = activityLevelExpanded,
                    onExpandedChange = { activityLevelExpanded = !activityLevelExpanded },
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        readOnly = true,
                        value = activityLevel.name.replace("_", " ").lowercase()
                            .replaceFirstChar {
                                it.titlecase(
                                    Locale.getDefault()
                                )
                            },
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityLevelExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CWPrimary,
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = CWPrimary,
                            focusedTextColor = CWTextPrimary,
                            unfocusedTextColor = CWTextPrimary,
                            focusedLabelColor = CWPrimary,
                        ),
                    )
                    ExposedDropdownMenu(
                        expanded = activityLevelExpanded,
                        onDismissRequest = { activityLevelExpanded = false },
                    ) {
                        activityLevels.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = {
                                    Text(selectionOption.name.replace("_", " ").lowercase()
                                        .replaceFirstChar {
                                            it.titlecase(
                                                Locale.getDefault()
                                            )
                                        })
                                },
                                onClick = {
                                    activityLevel = selectionOption
                                    activityLevelExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // Calorie Target
            SettingsSection(title = "Calorie Target", icon = Icons.Filled.Flag) {
                LabeledSlider(
                    label = "Calorie Target",
                    value = targetCalories.toFloat(),
                    onValueChange = { targetCalories = it.roundToInt() },
                    valueRange = 1000f..3000f,
                    steps = (3000 - 1000) / 50 - 1,
                    valueText = "$targetCalories kcal"
                )

                Button(
                    onClick = {
                        val heightInCm = (heightFeet * 12 + heightInches) * 2.54
                        val weightInKg = weightLbs / 2.20462
                        val bmr = if (gender == Gender.MALE) {
                            (10 * weightInKg) + (6.25 * heightInCm) - (5 * age) + 5
                        } else {
                            (10 * weightInKg) + (6.25 * heightInCm) - (5 * age) - 161
                        }
                        val tdee = bmr * when (activityLevel) {
                            ActivityLevel.SEDENTARY -> 1.2
                            ActivityLevel.LIGHTLY_ACTIVE -> 1.375
                            ActivityLevel.MODERATELY_ACTIVE -> 1.55
                            ActivityLevel.VERY_ACTIVE -> 1.725
                            ActivityLevel.EXTRA_ACTIVE -> 1.9
                        }
                        targetCalories = tdee.roundToInt()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CWPrimary)
                ) {
                    Text("Auto-Calculate")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.PrivacyTip,
                    contentDescription = "Privacy",
                    tint = CWTextPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Calorie Watcher keeps your data on device.",
                    style = MaterialTheme.typography.labelMedium,
                    color = CWTextPrimary.copy(alpha = 0.8f)
                )
            }
        }
    }
}


@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = CWPrimary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = TitleFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = CWTextPrimary
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = CWTextPrimary
            )
            Text(valueText, style = MaterialTheme.typography.bodyLarge, color = CWTextPrimary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = CWPrimary,
                activeTrackColor = CWPrimary,
                inactiveTrackColor = CWSecondary
            )
        )
    }
}
