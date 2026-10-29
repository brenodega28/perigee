plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("perigee.ktlint")
}

android {
    namespace = perigeeNamespace()
    compileSdk = ANDROID_COMPILE_SDK
    defaultConfig {
        applicationId = "brenodega28.perigee"
        minSdk = ANDROID_MIN_SDK
        targetSdk = ANDROID_TARGET_SDK
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JAVA_TARGET
        targetCompatibility = JAVA_TARGET
    }
    buildFeatures {
        compose = true
    }
    lint {
        abortOnError = true
    }
}
