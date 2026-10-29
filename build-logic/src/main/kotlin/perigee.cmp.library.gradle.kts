plugins {
    id("perigee.kmp.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(versionCatalog.findLibrary("compose-runtime").get())
            implementation(versionCatalog.findLibrary("compose-foundation").get())
            implementation(versionCatalog.findLibrary("compose-material3").get())
        }
    }
}
