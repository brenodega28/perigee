plugins {
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    version.set(versionCatalog.findVersion("ktlint").get().requiredVersion)
    filter {
        exclude { element -> element.file.path.contains("/build/generated/") }
    }
}
