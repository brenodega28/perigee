pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "perigee"

include(
    ":core:model",
    ":core:scoring",
    ":core:rules",
    ":core:profile",
    ":core:protocol",
    ":probe:common",
    ":probe:android",
    ":probe:windows",
    ":ui:shared",
    ":ui:android",
    ":ui:desktop",
    ":app-android",
    ":app-desktop",
)
