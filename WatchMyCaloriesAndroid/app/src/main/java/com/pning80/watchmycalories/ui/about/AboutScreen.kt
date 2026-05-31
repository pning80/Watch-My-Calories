package com.pning80.watchmycalories.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.play.core.review.ReviewManagerFactory
import com.pning80.watchmycalories.ads.BannerAdView
import com.pning80.watchmycalories.security.PlayIntegrityManager
import com.pning80.watchmycalories.ui.theme.Spacing
import com.pning80.watchmycalories.utils.AccessibilityTags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val isVerified by PlayIntegrityManager.isAttested.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Watch My Calories") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.l)
        ) {
            // Brand mark — consistent with Dashboard header + Onboarding welcome.
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp,
                modifier = Modifier.size(80.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = "Watch My Calories logo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }

            Text(
                "Watch My Calories",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
            val versionLabel = remember {
                val name = com.pning80.watchmycalories.BuildConfig.VERSION_NAME
                val code = com.pning80.watchmycalories.BuildConfig.VERSION_CODE
                "Version $name ($code)"
            }
            var versionCopied by remember { mutableStateOf(false) }
            Text(
                if (versionCopied) "$versionLabel — copied!" else versionLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .testTag(AccessibilityTags.About.VERSION_LABEL)
                    .clickable {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(versionLabel))
                        versionCopied = true
                    }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.l))

            // Banner ad — mirrors iOS About screen layout (BannerAdView between
            // the header section and the Rate row).
            BannerAdView()

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
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.s)
            )

            ListItem(
                headlineContent = { Text("Help & Support") },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help") },
                modifier = Modifier
                    .testTag(AccessibilityTags.About.HELP_AND_SUPPORT)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gist.github.com/pning80/7dc8a85c83edcc03845d182386cab470"))
                        context.startActivity(intent)
                    }
            )

            ListItem(
                headlineContent = { Text("Privacy Policy") },
                leadingContent = { Icon(Icons.Filled.PrivacyTip, contentDescription = "Privacy") },
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
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.s)
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

            // AI disclaimer — exact mirror of iOS AboutView footer copy.
            Text(
                "Calorie estimates are generated by AI and may not be accurate. Not intended as medical or nutritional advice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Text(
                "Powered by Google Gemini",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
            )

            // Copyright footer — matches iOS "© 2026 Watch My Calories".
            Text(
                "© 2026 Watch My Calories",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}
