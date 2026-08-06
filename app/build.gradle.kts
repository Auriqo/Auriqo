import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

/** Returns a BuildConfig-safe Java string literal for a value that may come from CI. */
fun buildConfigString(value: String): String = buildString {
    append('"')
    value.forEach { character ->
        append(
            when (character) {
                '\\' -> "\\\\"
                '"' -> "\\\""
                '\n' -> "\\n"
                '\r' -> "\\r"
                '\t' -> "\\t"
                else -> character
            }
        )
    }
    append('"')
}

fun projectValue(name: String): String =
    localProperties.getProperty(name)
        ?: providers.gradleProperty(name).orNull
        ?: System.getenv(name)
        ?: ""

val releaseKeystore = file("keystore/release.keystore")
val releaseSigningValues = listOf("STORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")
    .associateWith(::projectValue)
val releaseSigningConfigured =
    releaseKeystore.isFile && releaseSigningValues.values.all(String::isNotBlank)
plugins {
    id("com.android.application")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobufPlugin)
}

val hasGoogleServicesConfig = file("google-services.json").exists()

if (hasGoogleServicesConfig) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "com.auriqo.music"
    compileSdk = 36
    ndkVersion = "27.0.12077973"


    defaultConfig {
        applicationId = "com.auriqo.music"
        minSdk = 26
        targetSdk = 36
        versionCode = 527
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        // LastFM API keys from GitHub Secrets
        val lastFmKey = projectValue("LASTFM_API_KEY")
        val lastFmSecret = projectValue("LASTFM_SECRET")

        buildConfigField("String", "LASTFM_API_KEY", buildConfigString(lastFmKey))
        buildConfigField("String", "LASTFM_SECRET", buildConfigString(lastFmSecret))

        // Google Fonts Web Fonts Developer API key (optional).
        // Without it the app falls back to the public, key-free Google Fonts metadata endpoint.
        val googleFontsKey = projectValue("GOOGLE_FONTS_API_KEY")
        buildConfigField("String", "GOOGLE_FONTS_API_KEY", buildConfigString(googleFontsKey))

        // GitHub OAuth keys
        val githubClientId = projectValue("GH_CLIENT_ID")
        val githubClientSecret = projectValue("GH_CLIENT_SECRET")
        buildConfigField("String", "GH_CLIENT_ID", buildConfigString(githubClientId))
        buildConfigField("String", "GH_CLIENT_SECRET", buildConfigString(githubClientSecret))

        buildConfigField("String", "FLOW_NEURO_BASE_URL", buildConfigString(projectValue("FLOW_NEURO_BASE_URL").ifBlank { "https://api.flowneuroengine.com" }))
        buildConfigField("String", "FLOW_NEURO_API_KEY", buildConfigString(projectValue("FLOW_NEURO_API_KEY")))

        // Deployment-specific Listen Together endpoints. They are intentionally blank in
        // source builds and must be supplied by local.properties, -P, or CI environment.
        buildConfigField(
            "String",
            "LISTEN_TOGETHER_SERVER_URL",
            buildConfigString(projectValue("LISTEN_TOGETHER_SERVER_URL")),
        )
        buildConfigField(
            "String",
            "LISTEN_TOGETHER_SHARE_BASE_URL",
            buildConfigString(projectValue("LISTEN_TOGETHER_SHARE_BASE_URL")),
        )

//add nightly build label support
        val isNightly = project.hasProperty("nightly") && project.property("nightly") == "true"
        buildConfigField("Boolean", "IS_NIGHTLY", isNightly.toString())

        val discordApplicationId = "1518210534070292541"
        val discordApplicationIdLong = 1518210534070292541L
        val discordRedirectScheme = "discord-$discordApplicationId"

        buildConfigField("String", "DISCORD_APPLICATION_ID", "\"$discordApplicationId\"")
        buildConfigField("long", "DISCORD_APPLICATION_ID_LONG", "${discordApplicationIdLong}L")
        buildConfigField("String", "DISCORD_REDIRECT_SCHEME", "\"$discordRedirectScheme\"")
        manifestPlaceholders["discordRedirectScheme"] = discordRedirectScheme
    }


    flavorDimensions += listOf("abi", "variant")
    productFlavors {
        // FOSS variant (default) - F-Droid compatible, no Google Play Services
        create("foss") {
            dimension = "variant"
            isDefault = true
            buildConfigField("Boolean", "CAST_AVAILABLE", "false")
        }

        // GMS variant - with Google Cast support (requires Google Play Services)
        create("gms") {
            dimension = "variant"
            buildConfigField("Boolean", "CAST_AVAILABLE", "true")
        }

        create("universal") {
            dimension = "abi"
            buildConfigField("String", "ARCHITECTURE", "\"universal\"")
        }
        create("arm64") {
            dimension = "abi"
            buildConfigField("String", "ARCHITECTURE", "\"arm64\"")
            ndk { abiFilters.add("arm64-v8a") }
        }
        create("armeabi") {
            dimension = "abi"
            buildConfigField("String", "ARCHITECTURE", "\"armeabi\"")
            ndk { abiFilters.add("armeabi-v7a") }
        }
        create("x86") {
            dimension = "abi"
            buildConfigField("String", "ARCHITECTURE", "\"x86\"")
            ndk { abiFilters.add("x86") }
        }
        create("x86_64") {
            dimension = "abi"
            buildConfigField("String", "ARCHITECTURE", "\"x86_64\"")
            ndk { abiFilters.add("x86_64") }
        }
    }

    signingConfigs {
        create("persistentDebug") {
            storeFile = file("persistent-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            if (releaseSigningConfigured) {
                storeFile = releaseKeystore
                storePassword = releaseSigningValues.getValue("STORE_PASSWORD")
                keyAlias = releaseSigningValues.getValue("KEY_ALIAS")
                keyPassword = releaseSigningValues.getValue("KEY_PASSWORD")
            }
        }
        getByName("debug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storePassword = "android"
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
            isDebuggable = false
            // Local and F-Droid validation can build an unsigned release. CI sets the
            // three environment values and materializes app/keystore/release.keystore.
            signingConfig = signingConfigs.getByName("release").takeIf { releaseSigningConfigured }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        lintConfig = file("lint.xml")
        warningsAsErrors = false
        abortOnError = false
        checkDependencies = false
    }

    // Lightweight AOSP test image for the Compose instrumentation smoke suite in CI.
    // The task name is auriqoApi30UniversalFossDebugAndroidTest.
    testOptions {
        managedDevices {
            localDevices {
                create("auriqoApi30") {
                    device = "Pixel 2"
                    apiLevel = 30
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }

    androidResources {
        generateLocaleConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += listOf(
                "**/libandroidx.graphics.path.so",
                "**/libdatastore_shared_counter.so"
            )
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/CONTRIBUTORS.md"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

val releaseSigningGuardTaskName = "checkReleaseSigning"

tasks.register(releaseSigningGuardTaskName) {
    group = "verification"
    description = "Checks that the environment-only release signing inputs are available."
    doLast {
        check(releaseSigningConfigured) {
            "Release signing requires app/keystore/release.keystore and non-empty " +
                "STORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD environment variables."
        }
    }
}

if (providers.gradleProperty("requireReleaseSigning").orNull == "true") {
    val releaseSigningTaskPrefixes = listOf("validateSigning", "package", "bundle", "sign")
    tasks.configureEach {
        val isReleaseSigningTask =
            releaseSigningTaskPrefixes.any { prefix -> name.startsWith(prefix, ignoreCase = true) } &&
                name.contains("Release", ignoreCase = true)
        if (name != releaseSigningGuardTaskName && isReleaseSigningTask) {
            dependsOn(releaseSigningGuardTaskName)
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn"
        )
        suppressWarnings.set(false)
    }
}

dependencies {
    testImplementation(libs.junit)

    // Compose smoke tests exercise interaction semantics in a real instrumented
    // Compose runtime. They deliberately do not require app network providers.
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:${libs.versions.compose.get()}")
    debugImplementation("androidx.compose.ui:ui-test-manifest:${libs.versions.compose.get()}")

    // Firebase - GMS flavor only (excluded from F-Droid / FOSS builds)
    "gmsImplementation"(platform("com.google.firebase:firebase-bom:33.1.0"))
    "gmsImplementation"("com.google.firebase:firebase-analytics")
    "gmsImplementation"("com.google.firebase:firebase-crashlytics")

    // Google Drive Sync - GMS flavor only
    "gmsImplementation"(libs.play.services.auth)
    "gmsImplementation"(libs.google.api.client.android)
    "gmsImplementation"(libs.google.api.services.drive) {
        exclude(group = "org.apache.httpcomponents")
    }


    implementation(libs.haze)
    implementation(libs.guava)
    implementation(libs.coroutines.guava)
    implementation(libs.concurrent.futures)

    implementation(libs.activity)
    implementation(libs.hilt.navigation)
    implementation(libs.datastore)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.animation)
    implementation(libs.compose.reorderable)

    implementation(libs.viewmodel)
    implementation(libs.viewmodel.compose)
    implementation(libs.lifecycle.process)

    implementation(libs.material3)
    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.adaptive.layout)
    implementation(libs.androidx.adaptive.navigation)
    implementation(libs.palette)
    implementation(libs.materialKolor)
    implementation(libs.androidx.browser)

    implementation(libs.appcompat)

    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    implementation(libs.ucrop)

    implementation(libs.shimmer)

    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.hls)
    implementation(libs.media3.ui)
    implementation(libs.media3.okhttp)

    // Google Cast - only included in GMS flavor (not available in F-Droid/FOSS builds)
    "gmsImplementation"(libs.mediarouter)
    "gmsImplementation"(libs.cast.framework)

    implementation(libs.room.runtime)
    implementation(libs.kuromoji.ipadic)
    implementation(libs.tinypinyin)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    implementation(libs.apache.lang3)

    implementation(libs.hilt)
    implementation(libs.jsoup)
    ksp(libs.hilt.compiler)

    implementation(project(":innertube"))
    implementation(project(":kugou"))
    implementation(project(":lrclib"))
    implementation(project(":betterlyrics"))
    implementation(project(":simpmusic"))
    implementation(project(":youlyplus"))
    implementation(project(":canvas"))
    implementation(project(":shazamkit"))
    implementation(project(":artistvideo"))
    implementation(project(":applecanvas"))
    implementation(project(":paxsenixlyrics"))
    implementation(project(":unison"))


    implementation(libs.ktor.client.core)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)

    // Protobuf for message serialization (lite version for Android)
    implementation(libs.protobuf.javalite)
    implementation(libs.protobuf.kotlin.lite)

    coreLibraryDesugaring(libs.desugaring)
    implementation(libs.timber)
    implementation(libs.smoothCorner)
    implementation(libs.lottie.compose)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.work.runtime.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.ffmpeg.kit.audio)

}
