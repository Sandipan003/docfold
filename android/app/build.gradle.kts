import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release secrets (AdMob IDs, signing) live in android/keys.properties, which is git-ignored.
// Debug builds always use Google's official AdMob test IDs, so no real ad is ever clicked in testing.
val keys = Properties().apply {
    val f = rootProject.file("keys.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testBanner = "ca-app-pub-3940256099942544/9214589741"
val testInterstitial = "ca-app-pub-3940256099942544/1033173712"
val hasReleaseAds = listOf("ADMOB_APP_ID", "ADMOB_BANNER_ID", "ADMOB_INTERSTITIAL_ID").all { !keys.getProperty(it).isNullOrBlank() }
val hasSigning = listOf("STORE_FILE", "STORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD").all { !keys.getProperty(it).isNullOrBlank() }

android {
    namespace = "com.docfold.scanner"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.docfold.scanner"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.0"
    }

    androidResources {
        // Ship only the languages the app is translated into; trims library strings from the APK.
        localeFilters += listOf("en")
    }

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = rootProject.file(keys.getProperty("STORE_FILE"))
                storePassword = keys.getProperty("STORE_PASSWORD")
                keyAlias = keys.getProperty("KEY_ALIAS")
                keyPassword = keys.getProperty("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["admobAppId"] = testAppId
            buildConfigField("String", "AD_BANNER_ID", "\"$testBanner\"")
            buildConfigField("String", "AD_INTERSTITIAL_ID", "\"$testInterstitial\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["admobAppId"] = keys.getProperty("ADMOB_APP_ID") ?: testAppId
            buildConfigField("String", "AD_BANNER_ID", "\"${keys.getProperty("ADMOB_BANNER_ID") ?: testBanner}\"")
            buildConfigField("String", "AD_INTERSTITIAL_ID", "\"${keys.getProperty("ADMOB_INTERSTITIAL_ID") ?: testInterstitial}\"")
            if (hasSigning) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources.excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin", "kotlin-tooling-metadata.json")
    }
}

// Never let a Play Store build go out with test ads or an unsigned bundle.
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    val ready = hasReleaseAds && hasSigning
    doFirst {
        if (!ready) throw GradleException(
            "Release build blocked: fill android/keys.properties (AdMob IDs + signing). See PUBLISHING.md."
        )
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // Scanner + OCR run on-device and are delivered by Google Play services, so they add almost nothing to the APK.
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.2")

    // Ads + consent (GDPR / US state privacy) + in-app review.
    implementation("com.google.android.gms:play-services-ads:25.5.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation("com.google.android.play:review-ktx:2.0.2")
    // The Ads SDK depends on WorkManager 2.7 (2021); a current version fixes Android 14+ background-work issues.
    implementation("androidx.work:work-runtime:2.11.2")

    testImplementation("junit:junit:4.13.2")
}
