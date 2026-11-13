plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

val releaseKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")

android {
    namespace = "com.tryo.dashboard"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.tryo.dashboard"
        minSdk = 23
        targetSdk = 35
        versionCode = 13
        versionName = "2.6.4"
    }
    signingConfigs {
        if (!releaseKeystorePath.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (!releaseKeystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:security"))
    implementation(project(":core:notifications"))
    implementation(project(":feature:pluginmanager"))
    implementation(project(":feature:tracker"))
    implementation(project(":feature:usage"))
    implementation(project(":feature:donghua"))
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
