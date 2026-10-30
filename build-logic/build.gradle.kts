plugins {
    `kotlin-dsl`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(libs.plugin.kotlin)
    implementation(libs.plugin.kotlinSerialization)
    implementation(libs.plugin.android)
    implementation(libs.plugin.compose)
    implementation(libs.plugin.composeCompiler)
    implementation(libs.plugin.ktlint)
}
