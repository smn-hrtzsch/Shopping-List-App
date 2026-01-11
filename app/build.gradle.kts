import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

// --- TEIL 1: Properties laden ---
// Wir erstellen ein Properties-Objekt und laden die Datei local.properties
val keystorePropertiesFile = rootProject.file("local.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.CapyCode.ShoppingList"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.CapyCode.ShoppingList"
        minSdk = 24
        targetSdk = 36
        versionCode = project.findProperty("versionCode")?.toString()?.toInt() ?: 7
        versionName = project.findProperty("versionName") as String? ?: "2.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // --- TEIL 2: Signing Config definieren ---
    signingConfigs {
        create("configDebug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            // Wir nutzen 'getProperty' mit Standardwerten, das ist sicherer als der Array-Zugriff
            keyAlias = keystoreProperties.getProperty("key.alias", "debug")
            keyPassword = keystoreProperties.getProperty("key.password", "android")
            storePassword = keystoreProperties.getProperty("store.password", "android")

            // WICHTIG: storeFile nur setzen, wenn ein Pfad existiert UND nicht leer ist
            val keyStorePath = keystoreProperties.getProperty("store.file")
            if (!keyStorePath.isNullOrEmpty()) {
                storeFile = file(keyStorePath)
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("configDebug")
        }
        release {
            // --- TEIL 3: Signing Config anwenden ---
            // Hier sagen wir dem Release-Build, dass er die Config von oben nutzen soll
            signingConfig = signingConfigs.getByName("release")

            isMinifyEnabled = true
            isShrinkResources = true
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
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
}