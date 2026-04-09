import java.util.Properties
import java.io.FileInputStream


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    // Hilt plugin removed as per instruction
    id("com.google.devtools.ksp") version "2.3.4"
}

android {
    namespace = "com.calorieko.app"
    compileSdk = 36


    defaultConfig {
        applicationId = "com.calorieko.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Read Mapbox public access token from local.properties (git-ignored)
        val localPropertiesFile = rootProject.file("local.properties")
        val localProperties = Properties().apply {
            if (localPropertiesFile.exists()) {
                load(FileInputStream(localPropertiesFile))
            }
        }

        val mapboxAccessToken = localProperties.getProperty("MAPBOX_ACCESS_TOKEN")
            ?: throw GradleException(
                "MAPBOX_ACCESS_TOKEN not found in local.properties. " +
                "Please add: MAPBOX_ACCESS_TOKEN=pk.your_token_here"
            )
        resValue("string", "mapbox_access_token", mapboxAccessToken)

        val apiUrl = localProperties.getProperty("CALORIEKO_API_BASE_URL") ?: "http://10.0.2.2:8000/"
        buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
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
    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
            vendor.set(JvmVendorSpec.JETBRAINS)
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
        resValues = true
    }

    androidResources {
        noCompress += "tflite"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.material.icons.extended)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Real GPS Tracking
    implementation(libs.play.services.location)

    // Mapbox Maps SDK + Compose extension
    implementation(libs.mapbox.maps)
    implementation(libs.mapbox.maps.compose)

    //URI COMPOSE IMAGE
    implementation("io.coil-kt:coil-compose:2.5.0")

    // uCrop (for 1:1 profile image cropping)
    implementation("com.github.yalantis:ucrop:2.2.8")

    // Room (KSP)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // LiteRT (TensorFlow Lite successor)
    implementation(libs.litert)

    // Retrofit + OkHttp (REST API sync to Laravel backend)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // WorkManager (background auto-sync after Room writes)
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    // CameraX
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
