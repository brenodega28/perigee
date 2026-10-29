plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("perigee.ktlint")
}

kotlin {
    jvmToolchain(JVM_TOOLCHAIN)
    jvm()
    android {
        namespace = perigeeNamespace()
        compileSdk = ANDROID_COMPILE_SDK
        minSdk = ANDROID_MIN_SDK
        withHostTest {}
    }
    sourceSets {
        commonTest.dependencies {
            implementation(versionCatalog.findLibrary("kotlin-test").get())
        }
    }
}
