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
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    
    val cameraxVersion = "1.3.1"
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
