package com.pning80.watchmycalories.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.play.core.review.ReviewManagerFactory
import com.pning80.watchmycalories.security.PlayIntegrityManager
import com.pning80.watchmycalories.utils.AccessibilityTags
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isVerified by PlayIntegrityManager.isAttested.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Watch My Calories") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = "App Logo",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                "Watch My Calories",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Version 1.4.1",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(AccessibilityTags.About.VERSION_LABEL)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Rating
            ListItem(
                headlineContent = { Text("Rate on Play Store") },
                leadingContent = { Icon(Icons.Filled.Star, contentDescription = "Rate") },
                modifier = Modifier
                    .testTag(AccessibilityTags.About.RATE_ON_APP_STORE)
                    .clickable {
                    val manager = ReviewManagerFactory.create(context)
                    val request = manager.requestReviewFlow()
                    request.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val reviewInfo = task.result
                            val activity = context as? android.app.Activity
                            activity?.let { manager.launchReviewFlow(it, reviewInfo) }
                        }
                    }
                }
            )

            // Legal & Support Hub
            Text(
                "Support & Legal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            ListItem(
                headlineContent = { Text("Help & Support") },
                leadingContent = { Icon(Icons.Filled.Info, contentDescription = "Help") },
                modifier = Modifier
                    .testTag(AccessibilityTags.About.HELP_AND_SUPPORT)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gist.github.com/pning80/7dc8a85c83edcc03845d182386cab470"))
                        context.startActivity(intent)
                    }
            )

            ListItem(
                headlineContent = { Text("Privacy Policy") },
                leadingContent = { Icon(Icons.Filled.Info, contentDescription = "Privacy") },
                modifier = Modifier
                    .testTag(AccessibilityTags.About.PRIVACY_POLICY)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gist.github.com/pning80/fc4cc0aab367f96202371566241ec7cb"))
                        context.startActivity(intent)
                    }
            )

            // Device Attestation
            Text(
                "Device Attestation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            ListItem(
                headlineContent = { Text(if (isVerified) "Verified" else "Not Verified") },
                leadingContent = { 
                    Icon(
                        if (isVerified) Icons.Filled.CheckCircle else Icons.Filled.Warning, 
                        contentDescription = "Status",
                        tint = if (isVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    ) 
                }
            )

            Spacer(Modifier.weight(1f))

            Text(
                "Powered by Google Gemini",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
