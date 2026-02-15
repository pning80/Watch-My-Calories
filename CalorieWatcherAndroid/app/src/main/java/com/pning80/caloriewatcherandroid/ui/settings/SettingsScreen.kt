package com.pning80.caloriewatcherandroid.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.pning80.caloriewatcherandroid.ui.theme.OrganicGreen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    // Initialize text field with data from ViewModel (which includes BuildConfig fallback)
    var apiKeyText by remember(apiKey) { mutableStateOf(apiKey ?: "") }

    // Track local changes to profile fields
    var heightText by remember(userProfile) { mutableStateOf(userProfile?.heightCm?.toString() ?: "") }
    var weightText by remember(userProfile) { mutableStateOf(userProfile?.weightKg?.toString() ?: "") }
    var ageText by remember(userProfile) { mutableStateOf(userProfile?.age?.toString() ?: "") }
    var gender by remember(userProfile) { mutableStateOf(userProfile?.gender ?: Gender.MALE) }
    var activityLevel by remember(userProfile) { mutableStateOf(userProfile?.activityLevel ?: ActivityLevel.SEDENTARY) }

    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo_modern),
                contentDescription = "App Logo",
                modifier = Modifier.size(40.dp).padding(end = 12.dp)
            )
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "AI Configuration", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    label = { Text("Gemini API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { uriHandler.openUri("https://aistudio.google.com/app/apikey") },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Get API Key from Google AI Studio")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "User Profile", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                   OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it },
                        label = { Text("Height (cm)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Weight (kg)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it },
                    label = { Text("Age") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Gender", style = MaterialTheme.typography.bodyMedium)
                Row {
                    Gender.entries.forEach { g ->
                         FilterChip(
                            selected = (gender == g),
                            onClick = { gender = g },
                            label = { Text(g.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Activity Level", style = MaterialTheme.typography.bodyMedium)
                Column {
                     ActivityLevel.entries.forEach { level ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activityLevel = level }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (activityLevel == level),
                                onClick = { activityLevel = level }
                            )
                            Text(
                                text = level.name.replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                viewModel.saveApiKey(apiKeyText)

                val height = heightText.toDoubleOrNull() ?: 0.0
                val weight = weightText.toDoubleOrNull() ?: 0.0
                val age = ageText.toIntOrNull() ?: 0

                viewModel.saveProfile(height, weight, age, gender, activityLevel)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }

        if (userProfile != null) {
            Text(
                text = "Current Target: ${userProfile!!.targetCalories.toInt()} kcal",
                style = MaterialTheme.typography.headlineSmall,
                color = OrganicGreen,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
