import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

// local.properties values (dev-only secrets; never committed)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(FileInputStream(f))
}
val appBackendApiKey: String = localProps.getProperty("APP_BACKEND_API_KEY", "")

// AdMob — production IDs live in the committed `../Ads/AdMob-Android.properties`
// (mirror of `Ads/AdMob-iOS.xcconfig`). `local.properties` takes precedence so
// individual devs can override locally without touching the committed file.
// Absent both, the values fall back to Google's published test IDs so
// debug/CI/fresh-checkout builds keep working. iOS uses the same shape (DEBUG
// → hardcoded test IDs; RELEASE → `Bundle.main.infoDictionary` reads of
// `AdMob*AdUnitID` env vars sourced from `Ads/AdMob-iOS.xcconfig`).
//   Test app ID:        ca-app-pub-3940256099942544~3347511713
//   Test banner:        ca-app-pub-3940256099942544/6300978111
//   Test native:        ca-app-pub-3940256099942544/2247696110
//   Test interstitial:  ca-app-pub-3940256099942544/1033173712
val admobProps = Properties().apply {
    val f = rootProject.layout.projectDirectory
        .dir("../Ads")
        .file("AdMob-Android.properties")
        .asFile
    if (f.exists()) load(FileInputStream(f))
}
// `Properties.getProperty("KEY=")` returns "" (not null), so a dev who comments
// out their override by writing `ADMOB_BANNER_ID=` would otherwise short-circuit
// the chain to an empty BuildConfig string and AdMob would reject at runtime.
// `takeIf { it.isNotBlank() }` makes empty values defer to the next source.
fun admobIdFor(key: String, fallback: String): String =
    localProps.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: admobProps.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: fallback
val admobAppId: String = admobIdFor("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
val admobBannerId: String = admobIdFor("ADMOB_BANNER_ID", "ca-app-pub-3940256099942544/6300978111")
val admobNativeId: String = admobIdFor("ADMOB_NATIVE_ID", "ca-app-pub-3940256099942544/2247696110")
val admobInterstitialId: String = admobIdFor("ADMOB_INTERSTITIAL_ID", "ca-app-pub-3940256099942544/1033173712")

// Upload-key signing config for release builds. Absent on CI / fresh checkouts —
// we fall back to no signingConfig on release in that case, which lets ./gradlew
// :app:assembleDebug / lintDebug keep working without the keystore. A real
// ./gradlew :app:bundleRelease will fail clearly if these are missing or wrong.
val releaseStoreFile: String? = localProps.getProperty("RELEASE_STORE_FILE")
val releaseKeyAlias: String? = localProps.getProperty("RELEASE_KEY_ALIAS")
// Passwords prefer env vars (scripts/build-release.sh prompts and exports them)
// so they never persist to disk. local.properties is a fallback for CI.
val releaseStorePassword: String? =
    System.getenv("RELEASE_STORE_PASSWORD") ?: localProps.getProperty("RELEASE_STORE_PASSWORD")
val releaseKeyPassword: String? =
    System.getenv("RELEASE_KEY_PASSWORD") ?: localProps.getProperty("RELEASE_KEY_PASSWORD")
val hasReleaseSigning =
    releaseStoreFile != null && releaseKeyAlias != null &&
    releaseStorePassword != null && releaseKeyPassword != null &&
    file(releaseStoreFile).exists()

android {
    namespace = "com.pning80.watchmycalories"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pning80.watchmycalories"
        minSdk = 26
        targetSdk = 35
        versionCode = 142
        versionName = "1.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Dev legacy-key fallback for emulators without Play Services. Sourced from
        // local.properties; absent in release builds anyway (BackendConfig.devLegacyKey
        // gates by BuildConfig.DEBUG). See PORTING_CRITERIA.md T1.8.
        buildConfigField("String", "APP_BACKEND_API_KEY", "\"$appBackendApiKey\"")

        // AdMob unit IDs — read by `ads/AdManager.kt`. defaultConfig pins
        // Google's published test IDs so debug builds + parity tests never
        // fire real AdMob impressions (mirrors iOS `#if DEBUG` in
        // AdManager.swift). The `release { }` block below overrides with
        // production IDs from `Ads/AdMob-Android.properties`.
        buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
        buildConfigField("String", "ADMOB_NATIVE_ID", "\"ca-app-pub-3940256099942544/2247696110\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")

        // App ID injected into AndroidManifest.xml via `${ADMOB_APP_ID}`.
        // defaultConfig pins the test app ID for the same reason as above.
        manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    // Expose cross-platform fixtures (shared-fixtures/) on the JVM test classpath.
    // Tests load them via ClassLoader.getResource — same JSON the iOS suite reads.
    // gate-script source trees (schema-diff/, accessibility-diff/) also get bundled
    // into test resources; that's a few KB of bloat with no behavioral effect.
    sourceSets {
        getByName("test") {
            resources.srcDir(
                rootProject.layout.projectDirectory.dir("../shared-fixtures")
            )
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // Override defaultConfig's test IDs with the production unit IDs
            // resolved from local.properties / Ads/AdMob-Android.properties at
            // configuration time. Mirror of iOS `#else` branch in
            // AdManager.swift (RELEASE → Info.plist `AdMob*AdUnitID` env vars).
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerId\"")
            buildConfigField("String", "ADMOB_NATIVE_ID", "\"$admobNativeId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobInterstitialId\"")
            manifestPlaceholders["ADMOB_APP_ID"] = admobAppId

            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test:rules:1.6.1")  // GrantPermissionRule for CAMERA in CameraCapture mirrors
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("androidx.compose.ui:ui-test-manifest")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    
    // 1.4.x is the first CameraX line shipping 16KB-aligned native libs
    // (Android 15+ requirement). VISUAL_PARITY_AUDIT V-6.
    val cameraxVersion = "1.4.2"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.google.code.gson:gson:2.10.1")
    // OkHttp — used by GeminiRepository to talk to the Cloud Run backend
    // (PORTING_CRITERIA.md T1.5; replaces the Google AI SDK we removed).
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.health.connect:connect-client:1.1.0-alpha07")
    implementation("com.google.guava:guava:32.1.3-android")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    implementation("com.google.android.ump:user-messaging-platform:2.2.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // EncryptedSharedPreferences for the per-key Android assertion secret
    // (PORTING_CRITERIA.md T1.8). AES256.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // EXIF read for photo-library review screen (PhotoLibraryReviewScreen.kt) —
    // auto-detects meal type from the photo's DateTimeOriginal tag, mirroring
    // iOS PhotoLibraryReviewView.swift extractCreationDate().
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Play Integrity & Review
    implementation("com.google.android.play:integrity:1.3.0")
    implementation("com.google.android.play:review:2.0.1")
    implementation("com.google.android.play:review-ktx:2.0.1")
    
    // Location Context
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
    }
}

configurations.all {
    resolutionStrategy {
        force("androidx.test.espresso:espresso-core:3.7.0")
        force("androidx.test.espresso:espresso-idling-resource:3.7.0")
        force("androidx.test.ext:junit:1.2.1")
    }
}
