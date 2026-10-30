plugins {
    id("perigee.kmp.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(versionCatalog.findLibrary("kotlinx-serialization-json").get())
        }
    }
}
