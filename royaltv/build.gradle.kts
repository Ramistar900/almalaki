plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.almalaki.royaltv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.almalaki.royaltv"
        minSdk = 23
        targetSdk = 35
        versionCode = 17
versionName = "1.16"
    }

    signingConfigs {
        create("release") {
            storeFile = file(
                System.getenv("ROYALTV_RELEASE_STORE_FILE")
                    ?: "royaltv-release.jks"
            )
            storePassword =
                System.getenv("ROYALTV_STORE_PASSWORD") ?: ""
            keyAlias =
                System.getenv("ROYALTV_KEY_ALIAS") ?: ""
            keyPassword =
                System.getenv("ROYALTV_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
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
    implementation("androidx.lifecycle:lifecycle-runtime-android:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation(platform("io.github.jan-tennert.supabase:bom:3.2.1"))
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.ktor:ktor-client-android:3.2.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
