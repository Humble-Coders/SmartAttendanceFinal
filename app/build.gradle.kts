plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.humblecoders.smartattendance"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.humblecoders.smartattendance"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // BLE Library - BLESSED-Kotlin
    implementation("com.github.weliem:blessed-android-coroutines:0.4.2")
    implementation("com.jakewharton.timber:timber:5.0.1")


    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")

    // WebView for Face.io
    implementation("androidx.webkit:webkit:1.9.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // DataStore for local storage
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    implementation ("androidx.compose.material:material-icons-extended:1.5.4")

    // Splash Screen API (for Android 12+ compatibility)
    implementation (libs.androidx.core.splashscreen)

    // Animation dependencies (if not already present)
    implementation (libs.androidx.animation)
    implementation (libs.androidx.animation.core)


    implementation("com.google.android.material:material:1.10.0")

    implementation ("androidx.constraintlayout:constraintlayout-compose:1.0.1")



}