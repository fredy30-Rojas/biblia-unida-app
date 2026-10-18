plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.fredy.bibliaunida"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fredy.bibliaunida"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            // el keystore lo genera GitHub Actions con un secreto
            storeFile = file(System.getenv("BIBLIA_KEYSTORE") ?: "biblia.jks")
            storePassword = System.getenv("BIBLIA_STORE_PASS")
            keyAlias = System.getenv("BIBLIA_KEY_ALIAS")
            keyPassword = System.getenv("BIBLIA_KEY_PASS")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.11.0")
}
