plugins {
    id("com.android.library")
    id("perigee.ktlint")
}

android {
    namespace = perigeeNamespace()
    compileSdk = ANDROID_COMPILE_SDK
    defaultConfig {
        minSdk = ANDROID_MIN_SDK
    }
    compileOptions {
        sourceCompatibility = JAVA_TARGET
        targetCompatibility = JAVA_TARGET
    }
}
