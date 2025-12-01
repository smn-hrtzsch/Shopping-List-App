import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

// --- TEIL 1: Properties laden ---
// Wir erstellen ein Properties-Objekt und laden die Datei local.properties
val keystorePropertiesFile = rootProject.file("local.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.example.einkaufsliste"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.einkaufsliste"
        minSdk = 24
        targetSdk = 36
        versionCode = project.findProperty("versionCode")?.toString()?.toInt() ?: 2
        versionName = project.findProperty("versionName") as String? ?: "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // --- TEIL 2: Signing Config definieren ---
    signingConfigs {
        // Wir erstellen eine Konfiguration namens "release"
        create("release") {
            // Wir lesen die Werte sicher aus den Properties
            // Hinweis: Das "as String" ist in Kotlin notwendig
            keyAlias = keystoreProperties["key.alias"] as String? ?: "debug"
            keyPassword = keystoreProperties["key.password"] as String? ?: "android"
            storeFile = if (keystoreProperties["store.file"] != null) {
                file(keystoreProperties["store.file"] as String)
            } else {
                null
            }
            storePassword = keystoreProperties["store.password"] as String? ?: "android"
        }
    }

    buildTypes {
        release {
            // --- TEIL 3: Signing Config anwenden ---
            // Hier sagen wir dem Release-Build, dass er die Config von oben nutzen soll
            signingConfig = signingConfigs.getByName("release")

            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.window:window:1.5.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
}