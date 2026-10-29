plugins {
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    version.set(versionCatalog.findVersion("ktlint").get().requiredVersion)
}
