plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.auriqo.music.wear"
    compileSdk = 36

    defaultConfig {
        // Google Play Services Data Layer requires the phone and Wear APKs to
        // use the same application package and signing certificate.
        applicationId = "com.auriqo.music"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("persistentDebug") {
            storeFile = rootProject.file("app/persistent-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            // Phone and Wear must share the protected production certificate
            // for Google Play Services Data Layer communication.
            storeFile = rootProject.file("app/keystore/release.keystore")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            signingConfig =
                signingConfigs.getByName("persistentDebug")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)
    implementation(libs.wear.tiles)
    implementation(libs.wear.tiles.material)
    implementation(libs.wear.tiles.renderer)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.material3)
    implementation(libs.activity)
    implementation(libs.play.services.wearable)
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.guava)

    debugImplementation(libs.wear.tooling.preview)
    coreLibraryDesugaring(libs.desugaring)

    testImplementation(libs.junit)
}
